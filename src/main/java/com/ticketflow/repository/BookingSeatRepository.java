package com.ticketflow.repository;

import com.ticketflow.domain.BookingSeat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    /** Which of these seat ids are already sold. */
    List<BookingSeat> findBySeatIdIn(List<Long> seatIds);

    /** Seat ids sold for one event. A seat in booking_seats is SOLD, permanently. */
    @Query("select bs.seat.id from BookingSeat bs where bs.seat.event.id = :eventId")
    List<Long> findSoldSeatIds(@Param("eventId") Long eventId);
}
