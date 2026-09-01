package com.ticketflow.repository;

import com.ticketflow.domain.Event;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

    /** Events, soonest first. */
    List<Event> findAllByOrderByStartsAtAsc();
}
