package com.ticketflow.hold;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The background job. Every `app.hold.sweep-interval-ms` (default 30s) it asks
 * HoldExpiryService to release whatever has expired.
 *
 * It is a safety net, not the primary mechanism:
 *   - availability reads already treat expired holds as free (Phase 3), and
 *   - the hold flow self-heals stale holds on the seats it touches (Phase 4/5),
 * so the system is correct between sweeps. The sweeper just keeps the table
 * tidy and guarantees seats free up even if nobody tries to re-hold them.
 *
 * @Profile("!test") keeps this timer from firing during the test suite, where we
 * call HoldExpiryService directly for determinism.
 *
 * fixedDelayString (not fixedRateString): wait N ms AFTER the previous run
 * finishes, so a slow run can't stack up overlapping executions.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class HoldExpirySweeper {

    private final HoldExpiryService holdExpiryService;

    @Scheduled(fixedDelayString = "${app.hold.sweep-interval-ms}")
    public void sweep() {
        holdExpiryService.releaseExpired();
    }
}
