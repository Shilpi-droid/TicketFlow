package com.ticketflow.hold;

import com.ticketflow.repository.SeatHoldRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Releases expired holds: sets released_at on any seat_holds row whose
 * expires_at has passed and which is still marked active.
 *
 * Why this matters even though availability reads already ignore expired holds:
 * the partial unique index `UNIQUE (seat_id) WHERE released_at IS NULL` still
 * sees an expired-but-unreleased row, so without this the seat could never be
 * re-held. Setting released_at is what truly frees the seat.
 *
 * Split from the scheduler (HoldExpirySweeper) on purpose: @Transactional only
 * works when the method is called from another bean through Spring's proxy.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HoldExpiryService {

    private final SeatHoldRepository seatHoldRepository;

    @Transactional
    public int releaseExpired() {
        int released = seatHoldRepository.releaseAllExpiredHolds(Instant.now());
        if (released > 0) {
            log.info("Released {} expired seat hold(s)", released);
        }
        return released;
    }
}
