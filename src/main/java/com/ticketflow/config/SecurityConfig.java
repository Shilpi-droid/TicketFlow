package com.ticketflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TEMPORARY Phase 0 security setup.
 *
 * Why this file exists at all: the moment `spring-boot-starter-security` is on
 * the classpath, Spring Boot locks down EVERY endpoint with HTTP Basic auth and
 * prints a random password in the console. That would make /actuator/health
 * return 401 and break our Phase 0 acceptance check.
 *
 * So here we tell Spring: "for now, allow every request." Phase 2 replaces this
 * whole class with real JWT authentication and locks the protected endpoints
 * back down.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF protection. CSRF defends browser form/cookie sessions;
            // this service is a stateless JSON API, so it does not apply.
            .csrf(csrf -> csrf.disable())
            // Authorization rules: permit everything for now.
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
