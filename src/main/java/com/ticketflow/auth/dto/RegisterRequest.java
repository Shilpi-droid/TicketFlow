package com.ticketflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The JSON body of POST /auth/register.
 *
 * A "record" is a compact immutable data carrier: this one line generates the
 * constructor, accessors (email(), password()), equals, hashCode, toString.
 *
 * The annotations are Bean Validation constraints. Because the controller method
 * marks the parameter @Valid, Spring checks them before our code runs and
 * returns 400 with the violation details if any fail.
 */
public record RegisterRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8, max = 100, message = "password must be 8-100 characters")
        String password
) {
}
