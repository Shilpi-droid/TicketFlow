-- Correctness check to run AFTER a load-test run.
--
--   docker exec -i ticketflow-postgres psql -U ticketflow -d ticketflow < loadtest/verify.sql

\echo '--- seats sold more than once (MUST be zero rows) ---'
SELECT seat_id, count(*) AS times_sold
FROM booking_seats
GROUP BY seat_id
HAVING count(*) > 1;

\echo '--- summary ---'
SELECT
  (SELECT count(*)                 FROM booking_seats)                       AS booking_seat_rows,
  (SELECT count(DISTINCT seat_id)  FROM booking_seats)                       AS distinct_seats_sold,
  (SELECT count(*)                 FROM bookings WHERE status = 'CONFIRMED') AS confirmed_bookings,
  (SELECT count(*)                 FROM bookings WHERE status = 'CANCELLED') AS cancelled_bookings,
  (SELECT count(*)                 FROM seat_holds WHERE released_at IS NULL) AS active_holds,
  (SELECT count(*)                 FROM webhook_events)                      AS webhook_events;
