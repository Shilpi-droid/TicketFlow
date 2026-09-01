package com.ticketflow.event;

/** Thrown when an event id in the URL does not exist. Maps to HTTP 404. */
public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(Long eventId) {
        super("No event with id " + eventId);
    }
}
