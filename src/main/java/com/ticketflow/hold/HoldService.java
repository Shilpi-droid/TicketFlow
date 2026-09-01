package com.ticketflow.hold;

import com.ticketflow.domain.Seat;
import com.ticketflow.domain.SeatHold;
import com.ticketflow.hold.dto.HoldResponse;
import com.ticketflow.repository.BookingSeatRepository;
import com.ticketflow.repository.SeatHoldRepository;
import com.ticketflow.repository.SeatRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Places an 8-minute hold on a set of seats. This is the core of the project.
 *
 * The race we defend against: two requests both read "seat 14 is free", both
 * decide to hold it, both insert a hold row. Result: two holds, and downstream,
 * two sales of one seat.
 *
 * Two layers of defence:
 *   1. Pessimistic lock (this class). We SELECT ... FOR UPDATE the seat rows,
 *      sorted by id, BEFORE checking availability. A second request for the same
 *      seat blocks on that lock until we commit, then sees our hold and fails
 *      cleanly with 409.
 *   2. The partial unique index on seat_holds (added in V1). Even if a bug let
 *      two inserts through, the database rejects the second one. We catch that
 *      and turn it into the same 409.
 *
 * Notes on the CLAUDE.md "traps":
 *   - @Transactional is on this SERVICE method, not the controller.
 *   - hold() is called from HoldController (a different class), so Spring's
 *     proxy is in play and @Transactional actually takes effect.
 *   - No `synchronized` / ReentrantLock anywhere — those only work inside one
 *     JVM and defeat the point.
 *   - Seats are always locked in ascending id order (deadlock avoidance).
 */
@Service
@RequiredArgsConstructor
public class HoldService {

    private final SeatRepository seatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final HoldCache holdCache;

    /** How long a hold lasts. From app.hold.duration (default PT8M). */
    @Value("${app.hold.duration}")
    private Duration holdDuration;

    @Transactional
    public HoldResponse hold(Long eventId, Long userId, List<Long> requestedSeatIds) {
        // Normalise: drop nulls, de-duplicate, sort ascending. Sorting here is
        // what guarantees every caller locks seats in the same order.
        List<Long> seatIds = requestedSeatIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        if (seatIds.isEmpty()) {
            throw new SeatUnavailableException("No seats requested", List.of());
        }

        // 1. LOCK the seat rows. Concurrent requests for an overlapping seat
        //    queue up on this line.
        List<Seat> seats = seatRepository.lockSeatsById(seatIds);

        if (seats.size() != seatIds.size()) {
            throw new SeatUnavailableException("One or more seats do not exist", seatIds);
        }
        List<Long> wrongEvent = seats.stream()
                .filter(seat -> !seat.getEvent().getId().equals(eventId))
                .map(Seat::getId)
                .toList();
        if (!wrongEvent.isEmpty()) {
            throw new SeatUnavailableException(
                    "Seats do not belong to event " + eventId, wrongEvent);
        }

        Instant now = Instant.now();

        // 2. Self-heal: release any hold on these seats that has already expired
        //    but hasn't been swept yet. We hold the seat locks, so this is safe,
        //    and it means a stale hold can't block a fresh one in the gap before
        //    the background sweeper runs. Also clears the way past the partial
        //    unique index (which only ignores rows where released_at IS NOT NULL).
        seatHoldRepository.releaseExpiredHoldsForSeats(seatIds, now);

        // 3. Now check availability. Any hold or sale that another transaction
        //    committed before us is visible here.
        List<Long> taken = new ArrayList<>();
        taken.addAll(bookingSeatRepository.findSoldSeatIdsIn(seatIds));
        taken.addAll(seatHoldRepository.findActivelyHeldSeatIdsIn(seatIds, now));
        if (!taken.isEmpty()) {
            throw new SeatUnavailableException("Seats already taken: " + taken, taken);
        }

        // 4. Insert one hold row per seat, all sharing a hold_group_id.
        UUID holdGroupId = UUID.randomUUID();
        Instant expiresAt = now.plus(holdDuration);
        List<SeatHold> holds = seats.stream().map(seat -> {
            SeatHold hold = new SeatHold();
            hold.setSeat(seat);
            hold.setHoldGroupId(holdGroupId);
            hold.setUserId(userId);
            hold.setExpiresAt(expiresAt);
            return hold;
        }).toList();

        try {
            // saveAllAndFlush forces the INSERTs to hit the database now, so a
            // unique-index violation is thrown here where we can handle it —
            // not later during an opaque commit.
            seatHoldRepository.saveAllAndFlush(holds);
        } catch (DataIntegrityViolationException ex) {
            throw new SeatUnavailableException("A seat was taken concurrently", seatIds);
        }

        // 5. Index the hold in Redis with a TTL = the remaining hold time. Best
        //    effort: if Redis is down this does nothing and the hold is still
        //    valid (Postgres has it). Written before commit — a rolled-back
        //    transaction leaves an orphan key, but its TTL cleans that up.
        holdCache.register(holdGroupId, holdDuration);

        return new HoldResponse(holdGroupId, expiresAt, seatIds);
    }
}
