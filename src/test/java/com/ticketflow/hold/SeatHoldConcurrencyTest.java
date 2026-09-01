package com.ticketflow.hold;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketflow.domain.Seat;
import com.ticketflow.domain.User;
import com.ticketflow.hold.dto.HoldResponse;
import com.ticketflow.repository.SeatHoldRepository;
import com.ticketflow.repository.SeatRepository;
import com.ticketflow.repository.UserRepository;
import com.ticketflow.support.AbstractIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The most important test in the repo. It turns "the hold logic is correct"
 * from a claim into evidence.
 *
 * It runs against a REAL PostgreSQL (Testcontainers) because the whole point is
 * row-level locking behaviour, which an in-memory database like H2 does not
 * reproduce.
 */
class SeatHoldConcurrencyTest extends AbstractIntegrationTest {

    private static final long EVENT_ID = 1L;   // seeded by V2__seed_demo_event.sql

    @Autowired
    private HoldService holdService;
    @Autowired
    private SeatHoldRepository seatHoldRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private UserRepository userRepository;

    private Long userId;
    private List<Long> seatIds;

    @BeforeEach
    void setUp() {
        seatHoldRepository.deleteAllInBatch();
        User user = new User();
        user.setEmail("racer-" + UUID.randomUUID() + "@test.local");
        user.setPasswordHash("not-a-real-hash");
        userId = userRepository.save(user).getId();

        seatIds = seatRepository.findByEventIdOrderById(EVENT_ID).stream()
                .map(Seat::getId)
                .toList();
    }

    @AfterEach
    void tearDown() {
        seatHoldRepository.deleteAllInBatch();
        userRepository.deleteById(userId);
    }

    @Test
    void exactly_one_of_50_concurrent_requests_wins_the_same_seat() throws Exception {
        long contestedSeat = seatIds.get(0);
        int threadCount = 50;

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch fire = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(threadCount);
        AtomicInteger held = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Throwable> unexpected = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    fire.await();                                  // block until released
                    holdService.hold(EVENT_ID, userId, List.of(contestedSeat));
                    held.incrementAndGet();
                } catch (SeatUnavailableException expected) {
                    rejected.incrementAndGet();
                } catch (Throwable t) {
                    unexpected.add(t);
                } finally {
                    finished.countDown();
                }
            });
        }

        ready.await();                                             // all 50 threads parked
        fire.countDown();                                          // GO — all at once
        assertThat(finished.await(60, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        assertThat(unexpected).as("no unexpected exceptions").isEmpty();
        assertThat(held.get()).as("exactly one hold succeeded").isEqualTo(1);
        assertThat(rejected.get()).as("everyone else got 409").isEqualTo(threadCount - 1);
        assertThat(seatHoldRepository.countActiveHoldsForSeat(contestedSeat))
                .as("exactly one row in seat_holds")
                .isEqualTo(1);
    }

    @Test
    void a_batch_containing_one_taken_seat_holds_nothing() {
        long free1 = seatIds.get(10);
        long taken = seatIds.get(11);
        long free2 = seatIds.get(12);

        holdService.hold(EVENT_ID, userId, List.of(taken));        // someone already holds it

        assertThatThrownBy(() -> holdService.hold(EVENT_ID, userId, List.of(free1, taken, free2)))
                .isInstanceOf(SeatUnavailableException.class);

        assertThat(seatHoldRepository.countActiveHoldsForSeat(free1)).isZero();
        assertThat(seatHoldRepository.countActiveHoldsForSeat(free2)).isZero();
        assertThat(seatHoldRepository.countActiveHoldsForSeat(taken)).isEqualTo(1);
    }

    @Test
    void holding_free_seats_succeeds_with_an_8_minute_expiry() {
        List<Long> requested = List.of(seatIds.get(30), seatIds.get(31), seatIds.get(32));
        Instant before = Instant.now();

        HoldResponse response = holdService.hold(EVENT_ID, userId, requested);

        assertThat(response.holdGroupId()).isNotNull();
        assertThat(response.seatIds()).containsExactlyElementsOf(requested.stream().sorted().toList());
        assertThat(response.expiresAt())
                .isBetween(before.plus(Duration.ofMinutes(7)),
                           Instant.now().plus(Duration.ofMinutes(9)));
        requested.forEach(id ->
                assertThat(seatHoldRepository.countActiveHoldsForSeat(id)).isEqualTo(1));
    }
}
