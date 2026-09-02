package com.ticketflow.repository;

import com.ticketflow.domain.WebhookEvent;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    Optional<WebhookEvent> findByProviderEventId(String providerEventId);

    /**
     * Insert a webhook event, or do nothing if provider_event_id already exists.
     * Returns 1 if a new row was written, 0 if it was a duplicate.
     *
     * Why a native "ON CONFLICT DO NOTHING" instead of catching the unique
     * violation in Java: once a constraint violation aborts a statement inside a
     * PostgreSQL transaction, the whole transaction is poisoned ("current
     * transaction is aborted"). ON CONFLICT lets the insert succeed-or-skip
     * without ever raising an error, so the surrounding transaction stays alive
     * and can go on to confirm the booking.
     */
    @Modifying
    @Query(value = """
            INSERT INTO webhook_events (provider_event_id, payload)
            VALUES (:providerEventId, CAST(:payload AS jsonb))
            ON CONFLICT (provider_event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfNew(@Param("providerEventId") String providerEventId,
                    @Param("payload") String payload);

    @Modifying
    @Query("update WebhookEvent w set w.processedAt = :now where w.providerEventId = :providerEventId")
    void markProcessed(@Param("providerEventId") String providerEventId, @Param("now") Instant now);
}
