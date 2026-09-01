package com.ticketflow.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for every test that needs the real application wired to a real
 * database.
 *
 * @SpringBootTest boots the full Spring context (all beans, real transaction
 * manager, real repositories).
 *
 * The container is a SINGLETON: created and started once in the static block,
 * shared by every test class that extends this, never explicitly stopped. The
 * JVM exits at the end of the test run and Testcontainers' "Ryuk" helper removes
 * it. Starting one Postgres for the whole suite instead of one per class is the
 * standard performance pattern.
 *
 * Flyway still runs V1 + V2 into this fresh container on context startup, so
 * every test begins with the schema in place and the demo event + 500 seats
 * already seeded.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    static {
        POSTGRES.start();
    }

    /**
     * Point Spring's datasource at the container. These lambdas are evaluated
     * after the container is running, so getJdbcUrl() etc. return real values.
     */
    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
