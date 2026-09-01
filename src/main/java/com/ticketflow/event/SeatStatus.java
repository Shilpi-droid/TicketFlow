package com.ticketflow.event;

/**
 * A seat's availability, as shown on the seat map.
 *
 * This is a READ-MODEL concept — it is computed on the fly from the seat_holds
 * and booking_seats tables every time the map is requested. It is deliberately
 * NOT a column on the seats table: a stored status can drift out of sync with
 * the tables that actually decide the truth, and preventing that class of bug
 * is the whole point of this project's schema (CLAUDE.md §3).
 *
 * Precedence when building the map: SOLD wins over HELD wins over AVAILABLE.
 */
public enum SeatStatus {
    AVAILABLE,
    HELD,
    SOLD
}
