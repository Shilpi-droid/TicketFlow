package com.ticketflow.booking;

import com.ticketflow.booking.dto.BookingResponse;
import com.ticketflow.domain.Booking;
import com.ticketflow.domain.BookingStatus;
import com.ticketflow.domain.SeatHold;
import com.ticketflow.repository.BookingRepository;
import com.ticketflow.repository.SeatHoldRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns a live hold group into a PENDING booking. No money moves here — a later
 * payment webhook confirms it (BookingService.confirm).
 *
 * Idempotency: the client sends an "Idempotency-Key" header. The same key always
 * maps to the same booking:
 *   - if a booking with that key already exists, return it (no second booking);
 *   - the bookings.idempotency_key UNIQUE constraint is the real guarantee — if
 *     two retries race past the "already exists?" check, one INSERT wins and the
 *     other is caught here and resolved to the winner.
 */
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final SeatHoldRepository seatHoldRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public BookingResponse checkout(Long userId, UUID holdGroupId, String idempotencyKey) {

        // 1. Retried request? Return the booking that key already made.
        Booking existing = bookingRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return BookingResponse.of(existing, seatIdsFor(existing.getHoldGroupId()));
        }

        // 2. Load and validate the hold group.
        List<SeatHold> holds = seatHoldRepository.findByHoldGroupId(holdGroupId);
        if (holds.isEmpty()) {
            throw new HoldNotFoundException(holdGroupId);
        }
        if (!holds.getFirst().getUserId().equals(userId)) {
            throw new NotYourHoldException();
        }
        Instant now = Instant.now();
        boolean allActive = holds.stream()
                .allMatch(h -> h.getReleasedAt() == null && h.getExpiresAt().isAfter(now));
        if (!allActive) {
            throw new HoldNotActiveException(holdGroupId);
        }

        // 3. Create the PENDING booking. total = sum of seat prices, snapshotted now.
        long totalCents = holds.stream().mapToLong(h -> h.getSeat().getPriceCents()).sum();
        Long eventId = holds.getFirst().getSeat().getEvent().getId();

        Booking booking = new Booking();
        booking.setHoldGroupId(holdGroupId);
        booking.setUserId(userId);
        booking.setEventId(eventId);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalCents(totalCents);
        booking.setIdempotencyKey(idempotencyKey);

        try {
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException raceOnKey) {
            // Another concurrent checkout with the same key beat us to it.
            Booking winner = bookingRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> raceOnKey);
            return BookingResponse.of(winner, seatIdsFor(winner.getHoldGroupId()));
        }

        return BookingResponse.of(booking, sortedSeatIds(holds));
    }

    private List<Long> seatIdsFor(UUID holdGroupId) {
        return sortedSeatIds(seatHoldRepository.findByHoldGroupId(holdGroupId));
    }

    private List<Long> sortedSeatIds(List<SeatHold> holds) {
        return holds.stream().map(h -> h.getSeat().getId()).sorted().toList();
    }
}
