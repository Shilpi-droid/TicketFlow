package com.ticketflow.repository;

import com.ticketflow.domain.Booking;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Used at checkout to detect a retried request (Phase 6). */
    Optional<Booking> findByIdempotencyKey(String idempotencyKey);
}