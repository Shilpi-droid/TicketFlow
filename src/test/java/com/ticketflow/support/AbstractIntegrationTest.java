package com.ticketflow.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for every test that needs the real application wired to real
 * infrastructure.
 *
 * @SpringBootTest boots the full Spring context (all beans, real transaction
 * manager, real repositories).
 *
 * @ActiveProfiles("test") makes the "!test" profile false, so the background
 * HoldExpirySweeper timer does not run during tests — expiry logic is exercised
 * by calling HoldExpiryService directly, which is deterministic.
 *
 * Both containers are SINGLETONS: created and started once in the static block,
 * shared by every test class that extends this, never explicitly stopped. The
 * JVM exits at the end of the test run and Testcontainers' "Ryuk" helper removes
 * them. One set of containers for the whole suite is the standard perf pattern.
 *
 * Flyway still runs V1 + V2 into the fresh Postgres on context startup, so every
 * test begins with the schema in place and the demo event + 500 seats seeded.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16");

    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    /**
     * Point Spring at the containers. These lambdas run after the containers are
     * up, so getJdbcUrl() / getMappedPort() return real values.
     */
    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.url",
                () -> "redis://%s:%d".formatted(REDIS.getHost(), REDIS.getMappedPort(6379)));
    }
}
