package com.ticketflow.auth;

import com.ticketflow.auth.dto.AuthResponse;
import com.ticketflow.auth.dto.LoginRequest;
import com.ticketflow.auth.dto.RegisterRequest;
import com.ticketflow.domain.User;
import com.ticketflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The business logic for registration and login.
 *
 * @RequiredArgsConstructor (Lombok) generates a constructor taking every final
 * field. Spring sees that single constructor and injects the three beans —
 * this is "constructor injection", the recommended style (fields can be final,
 * and the class is easy to unit-test with plain `new`).
 *
 * @Transactional on register(): everything inside runs in one database
 * transaction. If save() fails, nothing is half-written. (Note: put
 * @Transactional on the SERVICE, never the controller — CLAUDE.md §4.)
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (users.existsByEmail(request.email())) {
            throw new EmailAlreadyUsedException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        // Store a BCrypt hash, never the raw password. BCrypt is deliberately
        // slow and salts each hash, so two users with the same password get
        // different hashes and brute-forcing is expensive.
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        users.save(user);

        return issueFor(user);
    }

    public AuthResponse login(LoginRequest request) {
        // Same generic error whether the email is unknown or the password is
        // wrong — never tell an attacker which emails are registered.
        User user = users.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return issueFor(user);
    }

    private AuthResponse issueFor(User user) {
        String token = jwtService.issueToken(user.getEmail(), user.getId());
        return new AuthResponse(token, "Bearer", jwtService.expiresInSeconds());
    }
}
