package com.ticketflow.event.dto;

import com.ticketflow.domain.Event;
import java.time.Instant;

/**
 * One event in the GET /events list.
 *
 * A DTO (data transfer object) is the shape we expose over HTTP. We map the
 * Event ENTITY into this on the way out instead of serializing the entity
 * directly — that keeps JPA/Hibernate concerns (lazy proxies, internal fields)
 * out of the API, and lets the API shape evolve independently of the table.
 */
public record EventResponse(
        Long id,
        String name,
        String venue,
        Instant startsAt,
        Instant salesOpenAt,
        Instant salesCloseAt
) {
    public static EventResponse from(Event e) {
        return new EventResponse(
                e.getId(), e.getName(), e.getVenue(),
                e.getStartsAt(), e.getSalesOpenAt(), e.getSalesCloseAt());
    }
}
