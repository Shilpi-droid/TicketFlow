package com.ticketflow.repository;

import com.ticketflow.domain.SeatHold;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    /** All seat holds belonging to one checkout group. */
    List<SeatHold> findByHoldGroupId(UUID holdGroupId);
}