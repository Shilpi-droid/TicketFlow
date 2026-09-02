package com.ticketflow.booking.dto;

/**
 * The tiny body we send back to the payment provider. We almost always answer
 * 200 (see WebhookController) — this just records what we decided to do, which
 * is handy in logs and tests.
 */
public record WebhookAck(String result) {
}
