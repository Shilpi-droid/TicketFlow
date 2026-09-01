package com.ticketflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * exclude = UserDetailsServiceAutoConfiguration:
 *   By default Spring Boot creates an in-memory user with a random password
 *   ("Using generated security password: ..." in the logs). We do stateless JWT
 *   auth and have no use for it, so we switch that auto-configuration off.
 *
 * @EnableScheduling turns on @Scheduled methods (the Phase 5 hold-expiry sweeper).
 */
@EnableScheduling
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class TicketflowApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketflowApplication.class, args);
	}

}
