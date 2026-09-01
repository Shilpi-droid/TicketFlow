package com.ticketflow.hold;

import java.util.List;

/**
 * Thrown when at least one requested seat cannot be held — it is already sold,
 * already held by someone else, does not exist, or belongs to a different event.
 *
 * Maps to HTTP 409 Conflict. The hold is all-or-nothing: if this is thrown, NO
 * seats from the request were held.
 */
public class SeatUnavailableException extends RuntimeException {

    private final List<Long> seatIds;

    public SeatUnavailableException(String message, List<Long> seatIds) {
        super(message);
        this.seatIds = List.copyOf(seatIds);
    }

    public List<Long> getSeatIds() {
        return seatIds;
    }
}
