# Flyway migrations

Every `.sql` file here is applied **once**, in order, and then recorded in a
table Flyway creates called `flyway_schema_history`. Once applied, a file is
frozen — you never edit it; you add a new one.

## Naming (Flyway is strict about this)

```
V<version>__<description>.sql
```

- `V` — capital V, for a versioned migration.
- `<version>` — `1`, `2`, `3` ... (or `1.1`, `2026.08.29`). Must sort correctly.
- `__` — **two** underscores between version and description.
- `<description>` — words separated by single underscores.

Examples: `V1__create_events_and_seats.sql`, `V2__add_seat_holds.sql`.

## Phase 0

No migration files exist yet. On startup Flyway still connects, creates
`flyway_schema_history`, finds nothing to do, and reports success — that is
the proof that Flyway is wired correctly. The real schema arrives in Phase 1.
