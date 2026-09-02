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

**PostgreSQL is the source of truth. Redis is an index.** Redis holds a key per
hold group with an 8-minute TTL for fast "is this hold still alive?" lookups, but
if Redis and Postgres ever disagree, Postgres wins — every Redis call is
best-effort and falls back to Postgres on failure.

### Hold expiry

A hold has `expires_at`. Three things enforce it, in order of importance:

1. **Availability reads** treat `expires_at < now()` as free — so the seat map is
   correct the instant a hold expires, with no job having run.
2. **The hold flow self-heals** — before taking a seat it releases any expired
   hold on that seat (while holding the row lock), so a stale hold never blocks a
   fresh one.
3. **A `@Scheduled` sweeper** every 30s sets `released_at` on expired holds — a
   safety net that also runs once on startup, cleaning up anything that expired
   while the app was down.

### Checkout & payment webhooks

`POST /checkout` (client sends an `Idempotency-Key` header) creates a `PENDING`
booking from a hold group. No money moves — a fake payment provider then POSTs to
`POST /webhooks/payment` to confirm it.

- **Retried checkout** → same booking. `bookings UNIQUE (idempotency_key)` is the
  guarantee; the "already exists?" check is the fast path.
- **Duplicate webhook** → `webhook_events` is written first via
  `INSERT ... ON CONFLICT (provider_event_id) DO NOTHING`; a return of 0 rows
  means "seen before, skip". The provider can deliver the same event 5× and get
  `200` every time with exactly one booking confirmed.
- **Confirming** turns holds into `booking_seats` rows in one transaction;
  `booking_seats UNIQUE (seat_id)` is the final backstop against a double-sell.
- **Late webhook** (hold already expired) → booking is `CANCELLED`, no seats
  sold, `200` returned, and a "would refund here" line is logged. The refund
  itself is out of scope (no real provider).

## Concurrency: preventing a double-sell of one seat

Two requests read "seat 14 is free", both insert a hold, both proceed to sell.
Three ways to stop that:

| Approach | How | Trade-off |
|---|---|---|
| **Pessimistic lock** *(chosen)* | `SELECT … FOR UPDATE` on the seat rows (ordered by id) before checking availability; the loser blocks, then sees the winner's hold and gets 409 | One extra round trip and a held lock per request; serialises access per seat. Predictable under high contention for the same seat — which is exactly this workload (everyone wants the front row). |
| Optimistic lock | `@Version` column; both proceed, second commit throws `OptimisticLockException`, retry | Great under low contention, wasteful when many requests target the same seat (lots of retries). |
| Insert-and-catch | Just insert the hold; the partial unique index rejects the loser; catch `DataIntegrityViolationException` | Fewest moving parts, but you lose control over lock ordering for multi-seat requests and error messages are less precise. |

We use the **pessimistic lock as the primary path and the partial unique index
as a backstop** — if a bug ever lets two inserts through, the database still
refuses the second. Seats are always locked in ascending id order so two
multi-seat requests can't deadlock.

Evidence: `SeatHoldConcurrencyTest` fires 50 threads at one seat against a real
Postgres (Testcontainers) and asserts exactly one hold succeeds and exactly one
row lands in `seat_holds`.

## Progress

- [x] Phase 0 — scaffolding: app boots, connects to Postgres + Redis, Flyway wired
- [x] Phase 1 — domain model, migrations, seed data (1 event, 500 seats)
- [x] Phase 2 — auth: register/login, stateless JWT, `/me` protected, 401 on missing token
- [x] Phase 3 — read APIs: `GET /events`, `GET /events/{id}/seats` with derived AVAILABLE/HELD/SOLD
- [x] Phase 4 — seat holds: `POST /events/{id}/holds`, pessimistic lock, all-or-nothing; 50-thread concurrency test
- [x] Phase 5 — hold expiry: read-time check + self-heal + `@Scheduled` sweeper + Redis TTL index
- [x] Phase 6 — checkout + idempotent webhooks: `Idempotency-Key`, `ON CONFLICT` webhook log, late-webhook cancel
- [ ] Phase 7 — Kafka events (optional)
- [ ] Phase 8 — load test
- [ ] Phase 9 — README + deploy
