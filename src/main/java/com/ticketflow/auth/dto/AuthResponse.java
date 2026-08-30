package com.ticketflow.auth.dto;

/**
 * What POST /auth/register and POST /auth/login return.
 *
 * The client stores `token` and sends it on every subsequent request as the
 * header:  Authorization: Bearer &lt;token&gt;
 */
public record AuthResponse(
        String token,
        String tokenType,       // always "Bearer"
        long expiresInSeconds
) {
}
