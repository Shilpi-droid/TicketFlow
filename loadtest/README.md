# Load test

`seat-rush.js` fires many virtual users at a small pool of seats to prove the
locking + constraints hold up under contention, and to produce the latency /
throughput / conflict numbers for the project README.

## Prerequisites

- k6 installed (`k6 version`)
- the app running (`./mvnw spring-boot:run`) with Postgres + Redis up
  (`docker compose up -d`)

## Run

```bash
# 1. clear transient data so all 50 pool seats start AVAILABLE
docker exec -i ticketflow-postgres psql -U ticketflow -d ticketflow < loadtest/reset.sql

# 2. 200 VUs racing for seats 1..50 of event 1 for 45s
k6 run loadtest/seat-rush.js
#   tunables:  -e VUS=200  -e DURATION=60s  -e SEAT_MIN=1 -e SEAT_MAX=50  -e BASE_URL=http://localhost:8080

# 3. correctness check — the first query MUST return zero rows
docker exec -i ticketflow-postgres psql -U ticketflow -d ticketflow < loadtest/verify.sql
```

## What to read from the k6 output

| Metric | Meaning |
|---|---|
| `hold_latency` p(95) | 95th-percentile latency of the hold endpoint under contention |
| `http_reqs` / `iterations` per second | throughput |
| `hold_conflict` rate | fraction of hold attempts that correctly got 409 (seat already taken) |
| `hold_won`, `booking_confirmed` counters | seats actually held / sold |
| `server_error` count | **must be 0** — this is a threshold, k6 exits non-zero if not |

## Expected shape

With 200 users and 50 seats (4x oversubscription), the first ~50 hold attempts
win and the rest 409 — so a high `hold_conflict` rate is correct behaviour. The
point being proven: no request 5xxs, and `verify.sql` shows every seat sold at
most once.
