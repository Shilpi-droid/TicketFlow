package com.ticketflow.booking;

import java.util.UUID;

/**
 * The hold group exists but is no longer usable — every hold in it has been
 * released or has expired. Maps to HTTP 409.
 *
 * At checkout this means "your 8 minutes ran out, start again".
 */
public class HoldNotActiveException extends RuntimeException {

    public HoldNotActiveException(UUID holdGroupId) {
        super("Hold group " + holdGroupId + " has expired or been released");
    }
}
