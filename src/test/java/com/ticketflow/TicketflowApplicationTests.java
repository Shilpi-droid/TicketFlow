package com.ticketflow;

import com.ticketflow.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Smoke test: does the whole Spring context start? Extends AbstractIntegrationTest
 * so it runs against Testcontainers Postgres, not a database it assumes is
 * already running on localhost.
 */
class TicketflowApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}

}
