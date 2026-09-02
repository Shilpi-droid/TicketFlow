package com.ticketflow.booking.dto;

import com.ticketflow.domain.Booking;
import com.ticketflow.domain.BookingStatus;
import java.util.List;
import java.util.UUID;

/**
 * What POST /checkout returns, and what the tests assert on.
 *
 * status starts as PENDING; a later payment webhook flips it to CONFIRMED
 * (or CANCELLED if the hold lapsed first).
 */
public record BookingResponse(
        Long id,
        BookingStatus status,
        long totalCents,
        UUID holdGroupId,
        List<Long> seatIds
) {
    public static BookingResponse of(Booking booking, List<Long> seatIds) {
        return new BookingResponse(
                booking.getId(),
                booking.getStatus(),
                booking.getTotalCents(),
                booking.getHoldGroupId(),
                seatIds);
    }
}
