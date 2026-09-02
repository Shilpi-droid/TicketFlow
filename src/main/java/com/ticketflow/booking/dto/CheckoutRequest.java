package com.ticketflow.booking.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Body of POST /checkout.
 *
 * The hold group already ties together the seats and the user, so that is all
 * we need. The Idempotency-Key comes in as an HTTP header, not in this body.
 */
public record CheckoutRequest(
        @NotNull UUID holdGroupId
) {
}
