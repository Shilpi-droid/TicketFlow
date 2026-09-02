-- Reset transient data before a load-test run. Seats themselves are never
-- deleted; this just clears every hold, booking, sale and webhook record so the
-- seat pool is fully AVAILABLE again.
--
--   docker exec -i ticketflow-postgres psql -U ticketflow -d ticketflow < loadtest/reset.sql

DELETE FROM booking_seats;
DELETE FROM bookings;
DELETE FROM seat_holds;
DELETE FROM webhook_events;
DELETE FROM users WHERE email LIKE 'load-%@test.local';

SELECT
  (SELECT count(*) FROM seats)          AS seats_total,
  (SELECT count(*) FROM booking_seats)  AS sold,
  (SELECT count(*) FROM seat_holds)     AS holds,
  (SELECT count(*) FROM bookings)       AS bookings;
