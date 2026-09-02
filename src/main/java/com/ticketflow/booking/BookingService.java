package com.ticketflow.booking;

import com.ticketflow.domain.Booking;
import com.ticketflow.domain.BookingSeat;
import com.ticketflow.domain.BookingStatus;
import com.ticketflow.domain.Seat;
import com.ticketflow.domain.SeatHold;
import com.ticketflow.hold.HoldCache;
import com.ticketflow.repository.BookingRepository;
import com.ticketflow.repository.BookingSeatRepository;
import com.ticketflow.repository.SeatHoldRepository;
import com.ticketflow.repository.SeatRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirms or cancels a booking. Called by the payment webhook handler.
 *
 * confirm() is the moment holds become a permanent sale. It is idempotent: a
 * booking that is already CONFIRMED just returns ALREADY_DONE, so a webhook
 * delivered five times still produces exactly one set of booking_seats rows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    /** Result of trying to confirm a booking, for logging / the webhook ack. */
    public enum ConfirmOutcome {
        CONFIRMED,        // holds turned into a sale just now
        ALREADY_DONE,     // booking was already CONFIRMED
        HOLD_EXPIRED,     // late payment — hold gone; booking CANCELLED (would refund)
        SEATS_TAKEN,      // seats sold elsewhere — booking CANCELLED (would refund)
        NOT_PENDING,      // booking was CANCELLED already; nothing to do
        NO_SUCH_BOOKING
    }

    private final BookingRepository bookingRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final SeatRepository seatRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final HoldCache holdCache;

    @Transactional
    public ConfirmOutcome confirm(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            log.warn("Payment webhook for unknown booking {}", bookingId);
            return ConfirmOutcome.NO_SUCH_BOOKING;
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            return ConfirmOutcome.ALREADY_DONE;          // idempotent replay
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            return ConfirmOutcome.NOT_PENDING;
        }

        List<SeatHold> holds = seatHoldRepository.findByHoldGroupId(booking.getHoldGroupId());
        List<Long> seatIds = holds.stream().map(h -> h.getSeat().getId()).sorted().toList();

        // Lock the seat rows before the final write (same discipline as Phase 4).
        seatRepository.lockSeatsById(seatIds);

        Instant now = Instant.now();

        // Late payment: the 8-minute hold lapsed before the money arrived.
        boolean holdsStillValid = !holds.isEmpty() && holds.stream()
                .allMatch(h -> h.getReleasedAt() == null && h.getExpiresAt().isAfter(now));
        if (!holdsStillValid) {
            booking.setStatus(BookingStatus.CANCELLED);
            log.warn("Late payment for booking {}: hold group {} no longer valid. "
                            + "A real system would refund the charge here.",
                    bookingId, booking.getHoldGroupId());
            return ConfirmOutcome.HOLD_EXPIRED;
        }

        // Defence in depth: are any of these seats already in booking_seats?
        List<Long> alreadySold = bookingSeatRepository.findSoldSeatIdsIn(seatIds);
        if (!alreadySold.isEmpty()) {
            booking.setStatus(BookingStatus.CANCELLED);
            log.error("Cannot confirm booking {}: seats {} already sold. Would refund.",
                    bookingId, alreadySold);
            return ConfirmOutcome.SEATS_TAKEN;
        }

        // Turn each held seat into a permanent booking_seats row.
        List<BookingSeat> sold = holds.stream().map(hold -> {
            Seat seat = hold.getSeat();
            BookingSeat bs = new BookingSeat();
            bs.setBooking(booking);
            bs.setSeat(seat);
            return bs;
        }).toList();

        try {
            bookingSeatRepository.saveAllAndFlush(sold);   // UNIQUE(seat_id) is the last backstop
        } catch (DataIntegrityViolationException seatTaken) {
            booking.setStatus(BookingStatus.CANCELLED);
            log.error("Cannot confirm booking {}: a seat was sold concurrently. Would refund.", bookingId);
            return ConfirmOutcome.SEATS_TAKEN;
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        holds.forEach(h -> h.setReleasedAt(now));          // holds have served their purpose
        seatHoldRepository.saveAll(holds);
        holdCache.evict(booking.getHoldGroupId());

        log.info("Booking {} CONFIRMED ({} seats, {} cents)", bookingId, sold.size(), booking.getTotalCents());
        return ConfirmOutcome.CONFIRMED;
    }

    @Transactional
    public ConfirmOutcome cancel(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return ConfirmOutcome.NO_SUCH_BOOKING;
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            // A "payment_failed" after we already sold the seats — needs a human.
            log.error("payment_failed for already-CONFIRMED booking {}. Manual review.", bookingId);
            return ConfirmOutcome.ALREADY_DONE;
        }
        if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.CANCELLED);
            log.info("Booking {} CANCELLED (payment failed)", bookingId);
        }
        return ConfirmOutcome.NOT_PENDING;
    }
}
