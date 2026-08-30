package com.ticketflow.repository;

import com.ticketflow.domain.BookingSeat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    /** Which of these seat ids are already sold. Used by the availability map (Phase 3). */
    List<BookingSeat> findBySeatIdIn(List<Long> seatIds);
}