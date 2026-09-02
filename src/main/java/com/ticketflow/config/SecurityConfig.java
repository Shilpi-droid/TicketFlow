package com.ticketflow.config;

import com.ticketflow.auth.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Real security, from Phase 2 on.
 *
 * The model: STATELESS JWT. There is no server-side session and no login form.
 * A client registers or logs in, gets a token, and sends it on every request.
 * JwtAuthenticationFilter (added below) reads that token and populates the
 * security context; the rules here decide what an authenticated (or anonymous)
 * caller may reach.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF protects cookie/session browser flows. We have no sessions and
            // no cookies — auth is a header — so it does not apply. Disable it.
            .csrf(csrf -> csrf.disable())

            // Never create an HttpSession. Every request must carry its own token.
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Turn off the built-in login mechanisms we are replacing.
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())

            // Who can reach what.
            .authorizeHttpRequests(auth -> auth
                    // Spring's AuthorizationFilter also guards internal ERROR /
                    // ASYNC dispatches. When validation fails, MVC forwards to
                    // /error internally; without this line that forward is itself
                    // "unauthenticated" and the real 400 gets replaced by a 401.
                    .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()
                    .requestMatchers("/auth/**").permitAll()          // register + login
                    .requestMatchers("/actuator/health").permitAll()  // liveness probe
                    .requestMatchers(HttpMethod.GET, "/events/**").permitAll()  // public browsing
                    .requestMatchers("/webhooks/**").permitAll()      // payment provider callbacks (signature-verified in real life)
                    .anyRequest().authenticated())                    // everything else: token required

            // When an unauthenticated caller hits a protected endpoint, return a
            // plain 401 instead of redirecting to a login page (there is none).
            .exceptionHandling(ex ->
                    ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

            // Run our JWT filter before Spring's username/password filter, so the
            // context is already populated by the time authorization is checked.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * The hashing algorithm for passwords. AuthService uses this to encode on
     * register and to verify on login. BCrypt is the sensible default: salted,
     * adaptive, and slow on purpose.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
