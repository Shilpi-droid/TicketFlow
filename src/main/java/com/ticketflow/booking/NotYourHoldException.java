package com.ticketflow.booking;

/** The hold group belongs to a different user. Maps to HTTP 403. */
public class NotYourHoldException extends RuntimeException {

    public NotYourHoldException() {
        super("That hold group belongs to another user");
    }
}
