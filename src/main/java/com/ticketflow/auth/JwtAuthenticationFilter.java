package com.ticketflow.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs on EVERY request (OncePerRequestFilter guarantees exactly once, even if
 * the request is forwarded internally).
 *
 * Job: if there is a valid "Authorization: Bearer &lt;jwt&gt;" header, tell
 * Spring Security who the user is by putting an Authentication object into the
 * SecurityContext. If the header is missing or the token is bad, do nothing —
 * the request continues as anonymous, and the authorization rules in
 * SecurityConfig decide whether that is allowed (usually: 401).
 *
 * This filter never sends a response itself. It only populates context.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HEADER);

        if (header != null && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = header.substring(PREFIX.length());
            try {
                Claims claims = jwtService.parse(token);   // throws if invalid/expired

                // principal = email (claims.getSubject()). No authorities/roles —
                // this app has no role system, every authenticated user is equal.
                var authentication = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, List.of());
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                // Bad token: leave the context empty. Do NOT throw — an endpoint
                // that permits anonymous access should still work.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
