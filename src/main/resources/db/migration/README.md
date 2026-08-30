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

## Rules

- **Never edit a migration that has run on a shared database.** Flyway stores a
  checksum; a changed file fails startup with a checksum mismatch. Add a new
  `V<n>` instead. (Editing one that has only ever run on your local dev DB is
  fine — wipe and recreate with `docker compose down -v && docker compose up`.)
- The schema is owned entirely by these files. Hibernate runs with
  `ddl-auto=validate` and never changes tables.

## Current migrations

| File | What it does |
|------|--------------|
| `V1__initial_schema.sql` | All 7 tables + the unique constraints that guarantee no double-sell |
| `V2__seed_demo_event.sql` | Demo data: 1 event, 500 seats across 3 sections |
