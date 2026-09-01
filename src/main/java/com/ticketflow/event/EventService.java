package com.ticketflow.event;

import com.ticketflow.domain.Event;
import com.ticketflow.domain.Seat;
import com.ticketflow.event.dto.EventResponse;
import com.ticketflow.event.dto.SeatMapResponse;
import com.ticketflow.event.dto.SeatMapResponse.SeatView;
import com.ticketflow.event.dto.SeatMapResponse.Summary;
import com.ticketflow.repository.BookingSeatRepository;
import com.ticketflow.repository.EventRepository;
import com.ticketflow.repository.SeatHoldRepository;
import com.ticketflow.repository.SeatRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-side logic for the catalog.
 *
 * @Transactional(readOnly = true): all the work here is SELECTs. Marking it
 * read-only lets Hibernate skip dirty-checking (it won't scan loaded entities
 * for changes to flush) and lets the driver/DB optimise. It also documents
 * intent: nothing in here writes.
 */
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository events;
    private final SeatRepository seats;
    private final SeatHoldRepository seatHolds;
    private final BookingSeatRepository bookingSeats;

    @Transactional(readOnly = true)
    public List<EventResponse> listEvents() {
        return events.findAllByOrderByStartsAtAsc()
                .stream()
                .map(EventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SeatMapResponse seatMap(Long eventId) {
        Event event = events.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        List<Seat> allSeats = seats.findByEventIdOrderById(eventId);

        // Two small lookups instead of one query per seat. HashSet gives O(1)
        // "is this seat id in the set?" checks while we build the map.
        Set<Long> soldSeatIds = new HashSet<>(bookingSeats.findSoldSeatIds(eventId));
        Set<Long> heldSeatIds = new HashSet<>(
                seatHolds.findActivelyHeldSeatIds(eventId, Instant.now()));

        int available = 0;
        int held = 0;
        int sold = 0;
        List<SeatView> views = new ArrayList<>(allSeats.size());

        for (Seat seat : allSeats) {
            SeatStatus status;
            if (soldSeatIds.contains(seat.getId())) {
                status = SeatStatus.SOLD;
                sold++;
            } else if (heldSeatIds.contains(seat.getId())) {
                status = SeatStatus.HELD;
                held++;
            } else {
                status = SeatStatus.AVAILABLE;
                available++;
            }
            views.add(new SeatView(
                    seat.getId(), seat.getSection(), seat.getRowLabel(),
                    seat.getSeatNumber(), seat.getPriceCents(), status));
        }

        return new SeatMapResponse(
                event.getId(), event.getName(),
                new Summary(allSeats.size(), available, held, sold),
                views);
    }
}
