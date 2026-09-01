package com.ticketflow.repository;

import com.ticketflow.domain.Seat;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * A Spring Data repository.
 *
 * You write an INTERFACE; Spring generates the implementation at startup.
 * Extending JpaRepository<Seat, Long> gives you save/findById/findAll/delete/...
 * for free (Seat is the entity, Long is the type of its @Id).
 *
 * Spring Data also builds queries from METHOD NAMES. "findByEventIdOrderById"
 * parses to: WHERE event_id = ? ORDER BY id. No SQL required.
 */
public interface SeatRepository extends JpaRepository<Seat, Long> {

    /** Every seat for one event, in id order. */
    List<Seat> findByEventIdOrderById(Long eventId);

    long countByEventId(Long eventId);

    /**
     * Load these seats AND take a write lock on their rows.
     *
     * @Lock(PESSIMISTIC_WRITE) makes Hibernate append "FOR UPDATE" to the SQL:
     * the rows are locked until the surrounding transaction commits or rolls
     * back. A second transaction that runs this same query for an overlapping
     * seat blocks until the first one finishes — that is how two people clicking
     * the same seat get serialised into a clear winner and loser.
     *
     * "ORDER BY s.id" is not cosmetic: if request A locks seats (5, 9) and
     * request B locks (9, 5), they can deadlock waiting on each other. Everyone
     * locking in ascending-id order makes that impossible.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.id in :ids order by s.id")
    List<Seat> lockSeatsById(@Param("ids") Collection<Long> ids);
}
