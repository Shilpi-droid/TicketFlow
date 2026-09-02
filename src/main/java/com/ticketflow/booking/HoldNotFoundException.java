package com.ticketflow.booking;

import java.util.UUID;

/** No hold group with that id. Maps to HTTP 404. */
public class HoldNotFoundException extends RuntimeException {

    public HoldNotFoundException(UUID holdGroupId) {
        super("No hold group " + holdGroupId);
    }
}
