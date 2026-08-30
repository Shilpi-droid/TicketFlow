package com.ticketflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A raw record of one webhook call from the payment provider.
 *
 * The flow in Phase 6: on every webhook, INSERT here FIRST. If provider_event_id
 * is a duplicate, the INSERT fails the unique constraint — we catch that, treat
 * it as "already handled", and return 200 without doing the work again.
 *
 * @JdbcTypeCode(SqlTypes.JSON) — tells Hibernate this String field is backed by
 *   a real Postgres "jsonb" column, so it is stored and validated as JSON
 *   rather than as opaque text.
 *
 * processedAt == null  means "logged but not yet acted on".
 */
@Entity
@Table(name = "webhook_events")
@Getter
@Setter
@NoArgsConstructor
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_event_id", nullable = false)
    private String providerEventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}