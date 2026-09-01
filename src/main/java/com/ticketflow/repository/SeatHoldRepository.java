package com.ticketflow.repository;

import com.ticketflow.domain.SeatHold;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    /** All seat holds belonging to one checkout group. */
    List<SeatHold> findByHoldGroupId(UUID holdGroupId);

    /**
     * Seat ids that currently have a LIVE hold for this event: not released, and
     * not yet expired.
     *
     * The "expiresAt > :now" clause matters: a hold whose time has passed but
     * which the Phase 5 sweeper has not released yet must still read as free.
     * Correctness never depends on the sweeper having run (CLAUDE.md §5).
     *
     * This is JPQL (queries entities, not tables): "h.seat.id" walks the
     * SeatHold -> Seat -> id path; Hibernate turns it into a SQL join.
     */
    @Query("""
            select h.seat.id
            from SeatHold h
            where h.seat.event.id = :eventId
              and h.releasedAt is null
              and h.expiresAt > :now
            """)
    List<Long> findActivelyHeldSeatIds(@Param("eventId") Long eventId, @Param("now") Instant now);

    /**
     * Of these specific seat ids, which currently have a live hold (not released,
     * not expired). Used by the hold flow to reject seats someone else holds.
     */
    @Query("""
            select h.seat.id
            from SeatHold h
            where h.seat.id in :seatIds
              and h.releasedAt is null
              and h.expiresAt > :now
            """)
    List<Long> findActivelyHeldSeatIdsIn(@Param("seatIds") Collection<Long> seatIds,
                                         @Param("now") Instant now);

    /** Count the live holds on one seat — used by the concurrency test to assert "exactly one". */
    @Query("""
            select count(h)
            from SeatHold h
            where h.seat.id = :seatId
              and h.releasedAt is null
            """)
    long countActiveHoldsForSeat(@Param("seatId") Long seatId);
}
