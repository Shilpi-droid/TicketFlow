package com.ticketflow.event.dto;

import com.ticketflow.event.SeatStatus;
import java.util.List;

/**
 * The response body of GET /events/{id}/seats.
 *
 * Nested records keep related shapes together without a file each.
 */
public record SeatMapResponse(
        Long eventId,
        String eventName,
        Summary summary,
        List<SeatView> seats
) {

    /** Aggregate counts, handy for a frontend header ("498 of 500 available"). */
    public record Summary(int total, int available, int held, int sold) {
    }

    /** One seat and its computed status. */
    public record SeatView(
            Long id,
            String section,
            String rowLabel,
            int seatNumber,
            long priceCents,
            SeatStatus status
    ) {
    }
}
