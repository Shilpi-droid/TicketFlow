# TicketFlow

An event ticketing backend that sells seat-level tickets without ever
double-selling a seat.

Users browse events, pick seats, and get an 8-minute hold. Pay within the window
and the seats are sold; otherwise the hold expires and the seats return to
inventory. Two users racing for the same seat resolve to exactly one winner.
Payment webhooks that arrive twice, out of order, or late never cause a double
charge or a double sale.

## Stack

| Layer      | Choice                              |
|------------|-------------------------------------|
| Language   | Java 21                             |
| Framework  | Spring Boot 4.1                     |
| Data       | Spring Data JPA + PostgreSQL 16     |
| Migrations | Flyway                              |
| Cache      | Redis 7                             |
| Auth       | Spring Security + JWT               |
| Tests      | JUnit 5 + Testcontainers            |
| Local infra| Docker Compose                      |

> The build plan targets Spring Boot 3.x, but start.spring.io no longer generates
> 3.x projects, so this uses 4.1.

## Getting started

Requires Java 21, Docker, and Docker Compose.

```bash
docker compose up -d                        # Postgres 16 + Redis 7
./mvnw spring-boot:run                       # app on :8080
curl http://localhost:8080/actuator/health  # expect {"status":"UP"}
```

The app runs the JVM in UTC (`-Duser.timezone=UTC`, set in `pom.xml`); without
it, a machine on a non-UTC locale can fail to connect to Postgres 16.

## Design

Correctness is enforced by database constraints, not application logic:

- `booking_seats UNIQUE (seat_id)` — a seat is sold at most once, ever.
- `seat_holds UNIQUE (seat_id) WHERE released_at IS NULL` — one active hold per seat.
- `bookings UNIQUE (idempotency_key)` — a retried checkout never creates a second booking.
- `webhook_events UNIQUE (provider_event_id)` — a replayed webhook is processed once.

Seat status (AVAILABLE / HELD / SOLD) is derived from these tables, never stored.

## Progress

- [x] Phase 0 — scaffolding: app boots, connects to Postgres + Redis, Flyway wired
- [ ] Phase 1 — domain model, migrations, seed data
- [ ] Phase 2 — auth
- [ ] Phase 3 — read APIs
- [ ] Phase 4 — seat holds (pessimistic locking)
- [ ] Phase 5 — hold expiry
- [ ] Phase 6 — checkout + idempotent webhooks
- [ ] Phase 7 — Kafka events (optional)
- [ ] Phase 8 — load test
- [ ] Phase 9 — README + deploy
