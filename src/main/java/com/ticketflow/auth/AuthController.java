package com.ticketflow.auth;

import com.ticketflow.auth.dto.AuthResponse;
import com.ticketflow.auth.dto.LoginRequest;
import com.ticketflow.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry points for auth.
 *
 * @RestController = @Controller + @ResponseBody: return values are serialized
 *   straight to JSON (via Jackson), not treated as view names.
 * @RequestMapping("/auth") prefixes every method's path.
 * @RequestBody binds the JSON request body to the parameter.
 * @Valid triggers the Bean Validation constraints on the DTO.
 *
 * Controllers stay thin: parse the request, call the service, shape the
 * response. No business logic, no @Transactional here.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)   // 201, not the default 200
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
