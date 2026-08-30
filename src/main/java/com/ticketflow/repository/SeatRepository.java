package com.ticketflow.repository;

import com.ticketflow.domain.Seat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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

    /** Every seat for one event, in id order (Phase 4 will lock seats in this order). */
    List<Seat> findByEventIdOrderById(Long eventId);

    long countByEventId(Long eventId);
}