// seat-rush.js — k6 load test
// ---------------------------------------------------------------------------
// Simulates an on-sale rush: many virtual users racing for a small pool of
// seats. Each iteration:
//   1. POST a hold for one random seat from the pool
//   2. if the hold wins (201): POST /checkout, then POST the payment webhook
//
// Most hold attempts are expected to 409 (someone else got the seat) — that is
// the system working, not an error. The hard requirement is ZERO 5xx and,
// checked afterwards in SQL, ZERO seats sold more than once.
//
// Run:
//   k6 run loadtest/seat-rush.js
//   k6 run -e VUS=200 -e DURATION=60s -e SEAT_MAX=50 loadtest/seat-rush.js
// ---------------------------------------------------------------------------

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const EVENT_ID = __ENV.EVENT_ID || '1';
const SEAT_MIN = Number(__ENV.SEAT_MIN || 1);
const SEAT_MAX = Number(__ENV.SEAT_MAX || 50);

const holdConflict = new Rate('hold_conflict');          // fraction of holds that got 409
const holdWon = new Counter('hold_won');                 // holds that got 201
const bookingConfirmed = new Counter('booking_confirmed');
const serverError = new Counter('server_error');         // any 5xx, anywhere
const holdLatency = new Trend('hold_latency', true);

export const options = {
  scenarios: {
    seat_rush: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 200),
      duration: __ENV.DURATION || '45s',
    },
  },
  thresholds: {
    server_error: ['count==0'],          // no 5xx is a pass/fail requirement
    hold_latency: ['p(95)<2000'],
  },
};

export function setup() {
  const res = http.post(
    `${BASE}/auth/register`,
    JSON.stringify({ email: `load-${Date.now()}@test.local`, password: 'loadtest-pw-1' }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(res, { 'setup: registered a user': (r) => r.status === 201 });
  return { token: res.json('token') };
}

export default function (data) {
  const seat = Math.floor(Math.random() * (SEAT_MAX - SEAT_MIN + 1)) + SEAT_MIN;
  const authJson = { 'Content-Type': 'application/json', Authorization: `Bearer ${data.token}` };

  const hold = http.post(
    `${BASE}/events/${EVENT_ID}/holds`,
    JSON.stringify({ seatIds: [seat] }),
    { headers: authJson, tags: { name: 'hold' } },
  );
  holdLatency.add(hold.timings.duration);
  holdConflict.add(hold.status === 409);
  if (hold.status >= 500) serverError.add(1);

  if (hold.status === 201) {
    holdWon.add(1);
    const holdGroupId = hold.json('holdGroupId');
    const idem = `${__VU}-${__ITER}-${Date.now()}`;

    const checkout = http.post(
      `${BASE}/checkout`,
      JSON.stringify({ holdGroupId }),
      { headers: { ...authJson, 'Idempotency-Key': idem }, tags: { name: 'checkout' } },
    );
    if (checkout.status >= 500) serverError.add(1);

    if (checkout.status === 201) {
      const bookingId = checkout.json('id');
      const webhook = http.post(
        `${BASE}/webhooks/payment`,
        JSON.stringify({ providerEventId: `evt-${idem}`, type: 'payment_succeeded', bookingId }),
        { headers: { 'Content-Type': 'application/json' }, tags: { name: 'webhook' } },
      );
      if (webhook.status >= 500) serverError.add(1);
      if (webhook.status === 200) bookingConfirmed.add(1);
    }
  }

  check(hold, { 'hold is not 5xx': (r) => r.status < 500 });
  sleep(0.05);
}
