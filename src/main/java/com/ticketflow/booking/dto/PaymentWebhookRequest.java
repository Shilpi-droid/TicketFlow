package com.ticketflow.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * The JSON a payment provider POSTs to /webhooks/payment.
 *
 * This is a FAKE provider — no real Stripe account. A real integration would
 * also carry a signature header that we verify before trusting the body; that
 * verification is where authentication for this endpoint would live.
 *
 * providerEventId — the provider's own unique id for THIS delivery. The same
 *   logical event redelivered carries the same providerEventId, which is how we
 *   detect and ignore duplicates (webhook_events UNIQUE constraint).
 * type — "payment_succeeded" or "payment_failed".
 * bookingId — which of our bookings this payment is for (a real provider would
 *   carry this in metadata we set when creating the charge).
 */
public record PaymentWebhookRequest(
        @NotBlank String providerEventId,
        @NotBlank String type,
        @NotNull Long bookingId
) {
    public static final String PAYMENT_SUCCEEDED = "payment_succeeded";
    public static final String PAYMENT_FAILED = "payment_failed";
}
