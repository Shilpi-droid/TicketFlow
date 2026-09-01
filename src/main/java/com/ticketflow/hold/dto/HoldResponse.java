package com.ticketflow.hold.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response of a successful hold.
 *
 * holdGroupId ties this batch of seats together — the client passes it back at
 * checkout (Phase 6) to turn the hold into a booking.
 * expiresAt is 8 minutes out; after that the hold is worthless (Phase 5 releases it).
 */
public record HoldResponse(
        UUID holdGroupId,
        Instant expiresAt,
        List<Long> seatIds
) {
}
