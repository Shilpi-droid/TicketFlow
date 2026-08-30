package com.ticketflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** The JSON body of POST /auth/login. */
public record LoginRequest(

        @NotBlank
        @Email
        String email,

        @NotBlank
        String password
) {
}
