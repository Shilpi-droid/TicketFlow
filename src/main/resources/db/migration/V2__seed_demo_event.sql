-- V2__seed_demo_event.sql
-- ===========================================================================
-- Demo data: one event with 500 seats across 3 sections.
--
-- This is a plain versioned migration, so it runs exactly once per database
-- (Flyway records it in flyway_schema_history and never repeats it). In a
-- production-grade setup you would gate demo data behind a "dev" profile so it
-- never runs in prod; we are keeping it simple for now.
--
-- Seat layout:
--   Orchestra : rows A-J (10 rows) x 20 seats = 200 seats @ $120.00
--   Mezzanine : rows A-H  (8 rows) x 20 seats = 160 seats @  $85.00
--   Balcony   : rows A-G  (7 rows) x 20 seats = 140 seats @  $55.00
--                                     total   = 500 seats
-- ===========================================================================

INSERT INTO events (name, venue, starts_at, sales_open_at, sales_close_at)
VALUES (
    'Midnight Symphony — Opening Night',
    'The Grand Theater',
    now() + INTERVAL '30 days',   -- show is a month out
    now() - INTERVAL '1 day',     -- sales already open
    now() + INTERVAL '29 days'    -- sales close the day before the show
);

-- Generate the 500 seat rows in one statement.
--
--   generate_series(1, n)  produces the numbers 1..n as rows.
--   CROSS JOIN             combines every section with every row number and
--                          every seat number — a Cartesian product.
--   chr(64 + r)            turns 1 -> 'A', 2 -> 'B', ...  (ASCII 'A' is 65)
WITH e AS (
    SELECT id FROM events WHERE name = 'Midnight Symphony — Opening Night'
),
layout (section, row_count, price_cents) AS (
    VALUES
        ('Orchestra', 10, 12000),
        ('Mezzanine',  8,  8500),
        ('Balcony',    7,  5500)
)
INSERT INTO seats (event_id, section, row_label, seat_number, price_cents)
SELECT
    e.id,
    layout.section,
    chr(64 + row_num)      AS row_label,
    seat_num               AS seat_number,
    layout.price_cents
FROM e
CROSS JOIN layout
CROSS JOIN generate_series(1, layout.row_count) AS row_num
CROSS JOIN generate_series(1, 20)               AS seat_num
-- An INSERT ... SELECT assigns identity ids in the order rows arrive, which is
-- otherwise undefined. Order it so seat ids run Orchestra A1..A20, B1..., then
-- Mezzanine, then Balcony — matching how a person reads a seating chart.
ORDER BY layout.price_cents DESC, row_num, seat_num;