package com.ticketflow.hold.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Body of POST /events/{eventId}/holds.
 *
 * @NotEmpty — the list must have at least one element.
 * List&lt;@NotNull Long&gt; — no null entries inside the list.
 */
public record HoldRequest(
        @NotEmpty List<@NotNull Long> seatIds
) {
}
