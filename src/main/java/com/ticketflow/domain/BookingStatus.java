package com.ticketflow.domain;

/**
 * Lifecycle of a booking.
 *
 *   PENDING   — created at checkout, waiting for the payment webhook.
 *   CONFIRMED — payment succeeded; seats are now sold (booking_seats rows exist).
 *   CANCELLED — checkout abandoned, payment failed, or the hold expired first.
 *
 * These names must exactly match the CHECK constraint in V1__initial_schema.sql:
 *   CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED'))
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}