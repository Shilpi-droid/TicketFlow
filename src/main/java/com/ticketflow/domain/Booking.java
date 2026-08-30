package com.ticketflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A purchase attempt for a group of seats.
 *
 * @Enumerated(EnumType.STRING) — store the enum as its NAME ("PENDING") in a
 *   text column, not as its ordinal position (0, 1, 2). Ordinals are a trap:
 *   reordering the enum later silently corrupts every existing row.
 *
 * idempotencyKey — supplied by the client in the Idempotency-Key header at
 *   checkout. The UNIQUE constraint on this column means a retried request
 *   cannot create a second booking (Phase 6).
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hold_group_id", nullable = false)
    private UUID holdGroupId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "total_cents", nullable = false)
    private long totalCents;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}