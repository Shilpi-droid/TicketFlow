package com.ticketflow.user;

import com.ticketflow.domain.User;
import com.ticketflow.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * GET /me — returns the currently authenticated user.
 *
 * This is our first PROTECTED endpoint. It exists mainly to prove the auth
 * chain works: no token (or a bad one) => 401 before this method ever runs;
 * a valid token => Spring injects the Authentication and getName() is the
 * email we put in the token's subject claim.
 */
@RestController
@RequiredArgsConstructor
public class MeController {

    private final UserRepository users;

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        User user = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        return new MeResponse(user.getId(), user.getEmail(), user.getCreatedAt());
    }

    public record MeResponse(Long id, String email, Instant createdAt) {
    }
}
