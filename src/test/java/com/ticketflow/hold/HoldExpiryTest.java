package com.ticketflow.hold;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.ticketflow.domain.Seat;
import com.ticketflow.domain.SeatHold;
import com.ticketflow.domain.User;
import com.ticketflow.hold.dto.HoldResponse;
import com.ticketflow.repository.SeatHoldRepository;
import com.ticketflow.repository.SeatRepository;
import com.ticketflow.repository.UserRepository;
import com.ticketflow.support.AbstractIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Phase 5 — expiry.
 *
 * These tests use the seeded event 1 + 500 seats and call HoldExpiryService
 * directly (the background sweeper is off under the "test" profile).
 */
class HoldExpiryTest extends AbstractIntegrationTest {

    private static final long EVENT_ID = 1L;

    @Autowired
    private HoldService holdService;
    @Autowired
    private HoldExpiryService holdExpiryService;
    @Autowired
    private SeatHoldRepository seatHoldRepository;
    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StringRedisTemplate redis;

    private Long userId;
    private List<Long> seatIds;

    @BeforeEach
    void setUp() {
        seatHoldRepository.deleteAllInBatch();
        User user = new User();
        user.setEmail("expiry-" + UUID.randomUUID() + "@test.local");
        user.setPasswordHash("x");
        userId = userRepository.save(user).getId();
        seatIds = seatRepository.findByEventIdOrderById(EVENT_ID).stream().map(Seat::getId).toList();
    }

    @AfterEach
    void tearDown() {
        seatHoldRepository.deleteAllInBatch();
        userRepository.deleteById(userId);
    }

    @Test
    void sweeper_sets_released_at_on_holds_past_their_expiry() {
        Seat seat = seatRepository.findById(seatIds.get(0)).orElseThrow();
        SeatHold stale = new SeatHold();
        stale.setSeat(seat);
        stale.setHoldGroupId(UUID.randomUUID());
        stale.setUserId(userId);
        stale.setExpiresAt(Instant.now().minusSeconds(120));   // already expired
        seatHoldRepository.save(stale);

        int released = holdExpiryService.releaseExpired();

        assertThat(released).isEqualTo(1);
        assertThat(seatHoldRepository.findById(stale.getId()).orElseThrow().getReleasedAt())
                .as("released_at is now set")
                .isNotNull();
        assertThat(seatHoldRepository.countActiveHoldsForSeat(seat.getId())).isZero();
    }

    @Test
    void an_expired_hold_no_longer_blocks_re_holding_the_seat() {
        long seatId = seatIds.get(5);

        // hold the seat, then age the hold so it is expired
        HoldResponse first = holdService.hold(EVENT_ID, userId, List.of(seatId));
        seatHoldRepository.findByHoldGroupId(first.holdGroupId()).forEach(h -> {
            h.setExpiresAt(Instant.now().minusSeconds(60));
            seatHoldRepository.save(h);
        });

        // a fresh hold on the same seat must now succeed (self-heal + then insert)
        assertThatCode(() -> holdService.hold(EVENT_ID, userId, List.of(seatId)))
                .doesNotThrowAnyException();

        assertThat(seatHoldRepository.countActiveHoldsForSeat(seatId))
                .as("exactly one active hold — the new one")
                .isEqualTo(1);
    }

    @Test
    void a_new_hold_writes_a_redis_key_with_a_ttl() {
        HoldResponse response = holdService.hold(EVENT_ID, userId, List.of(seatIds.get(20)));

        String key = "hold:" + response.holdGroupId();
        assertThat(redis.hasKey(key)).isTrue();

        Long ttlSeconds = redis.getExpire(key);
        assertThat(ttlSeconds)
                .as("key has a positive TTL no longer than the hold duration")
                .isBetween(1L, Duration.ofMinutes(8).toSeconds());
    }
}
