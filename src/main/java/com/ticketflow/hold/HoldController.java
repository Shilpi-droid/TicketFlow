package com.ticketflow.hold;

import com.ticketflow.hold.dto.HoldRequest;
import com.ticketflow.hold.dto.HoldResponse;
import com.ticketflow.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * POST /events/{eventId}/holds  — requires a valid JWT (it is not in the public
 * GET allow-list, so SecurityConfig's "anyRequest().authenticated()" applies).
 */
@RestController
@RequestMapping("/events/{eventId}/holds")
@RequiredArgsConstructor
public class HoldController {

    private final HoldService holdService;
    private final UserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HoldResponse create(
            @PathVariable Long eventId,
            @Valid @RequestBody HoldRequest request,
            Authentication authentication) {

        Long userId = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"))
                .getId();

        return holdService.hold(eventId, userId, request.seatIds());
    }
}
