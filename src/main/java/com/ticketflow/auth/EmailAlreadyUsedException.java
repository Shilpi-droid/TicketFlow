package com.ticketflow.auth;

/**
 * Thrown by AuthService.register when the email is already registered.
 * GlobalExceptionHandler turns this into an HTTP 409 Conflict.
 */
public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("Email already registered: " + email);
    }
}
