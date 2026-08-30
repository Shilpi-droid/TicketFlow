package com.ticketflow.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Creates and verifies JSON Web Tokens (JWTs).
 *
 * A JWT is three base64 chunks joined by dots: header.payload.signature.
 * The payload ("claims") holds who the user is and when the token expires.
 * The signature is an HMAC of header+payload using our secret key — anyone can
 * READ a JWT, but only someone with the secret can FORGE one or tamper with it
 * undetected.
 *
 * We use HS256 (HMAC-SHA-256), a symmetric algorithm: the same secret signs and
 * verifies. Simple and correct for a single service like this one.
 *
 * @Service registers this class as a Spring-managed singleton so it can be
 * injected elsewhere.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    /**
     * Spring calls this constructor and fills the parameters from application.yml
     * via @Value. "${app.jwt.secret}" is the path into the YAML tree.
     */
    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // Keys.hmacShaKeyFor throws if the secret is shorter than 32 bytes,
        // which is a good thing — a short key is a weak key.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Issue a signed token for a user. The email goes in the standard "subject"
     * claim; the numeric id goes in a custom "uid" claim so we don't have to
     * look the user up by email on every request.
     */
    public String issueToken(String email, Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("uid", userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    /**
     * Verify a token's signature and expiry, and return its claims. Throws
     * io.jsonwebtoken.JwtException (a RuntimeException) if the token is invalid,
     * expired, or tampered with — the caller decides what to do with that.
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Token lifetime in seconds, for the "expiresIn" field of the login response. */
    public long expiresInSeconds() {
        return expirationMs / 1000;
    }
}
