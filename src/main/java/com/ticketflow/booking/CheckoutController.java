package com.ticketflow.booking;

import com.ticketflow.booking.dto.BookingResponse;
import com.ticketflow.booking.dto.CheckoutRequest;
import com.ticketflow.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * POST /checkout — needs a valid JWT.
 *
 * The client generates a fresh random "Idempotency-Key" per checkout attempt and
 * resends the SAME value on every retry. @RequestHeader with required=true (the
 * default) makes a missing header a 400 before this method runs.
 */
@RestController
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final UserRepository userRepository;

    @PostMapping("/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse checkout(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CheckoutRequest request,
            Authentication authentication) {

        if (idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key must not be blank");
        }
        Long userId = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"))
                .getId();

        return checkoutService.checkout(userId, request.holdGroupId(), idempotencyKey);
    }
}
