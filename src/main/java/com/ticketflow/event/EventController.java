package com.ticketflow.event;

import com.ticketflow.event.dto.EventResponse;
import com.ticketflow.event.dto.SeatMapResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public read APIs for browsing events and seat maps.
 *
 * These are GET-only and are allow-listed in SecurityConfig, so no token is
 * needed — a visitor can look before they sign up. Holding and buying seats
 * (Phase 4+) will require authentication.
 */
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public List<EventResponse> listEvents() {
        return eventService.listEvents();
    }

    /**
     * @PathVariable binds the {id} segment of the URL to the method parameter.
     * If it is not a number, Spring returns 400 before this runs.
     */
    @GetMapping("/{id}/seats")
    public SeatMapResponse seatMap(@PathVariable Long id) {
        return eventService.seatMap(id);
    }
}
