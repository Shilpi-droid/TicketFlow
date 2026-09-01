package com.ticketflow.hold;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * A thin index over hold groups, backed by Redis.
 *
 * ***  Postgres is the source of truth. Redis is an index.  ***
 * If the two ever disagree, Postgres wins. That is why every method here
 * swallows Redis failures: a Redis outage must never break holding or checkout,
 * it just makes those paths fall back to querying Postgres.
 *
 * What it stores: one key per hold group, "hold:{uuid}" -> "1", with a TTL
 * equal to the remaining hold time. Redis deletes the key on its own when the
 * TTL runs out, giving a near-instant "is this hold still alive?" check without
 * a database round trip (used at checkout, Phase 6).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HoldCache {

    private static final String KEY_PREFIX = "hold:";

    private final StringRedisTemplate redis;

    /** Record a live hold group with a time-to-live. Best effort. */
    public void register(UUID holdGroupId, Duration ttl) {
        try {
            redis.opsForValue().set(key(holdGroupId), "1", ttl);
        } catch (RuntimeException ex) {
            log.warn("Redis register failed for hold {} — index only, continuing", holdGroupId, ex);
        }
    }

    /**
     * Fast check: does Redis still have a key for this hold group?
     * Fails OPEN — if Redis is unreachable we return true so the caller goes on
     * to confirm against Postgres rather than wrongly rejecting a valid hold.
     */
    public boolean isRegistered(UUID holdGroupId) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(key(holdGroupId)));
        } catch (RuntimeException ex) {
            log.warn("Redis lookup failed for hold {} — assuming present", holdGroupId, ex);
            return true;
        }
    }

    /** Drop the key (hold was confirmed or released). Best effort. */
    public void evict(UUID holdGroupId) {
        try {
            redis.delete(key(holdGroupId));
        } catch (RuntimeException ex) {
            log.warn("Redis evict failed for hold {} — TTL will clean it up", holdGroupId, ex);
        }
    }

    private String key(UUID holdGroupId) {
        return KEY_PREFIX + holdGroupId;
    }
}
