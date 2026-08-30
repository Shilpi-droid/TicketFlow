package com.ticketflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A temporary claim on a single seat.
 *
 * The database has a partial unique index that allows only ONE row per seat
 * where released_at IS NULL. That is what makes "hold this seat" safe under a
 * race: two concurrent inserts, one wins, the other gets a unique violation.
 *
 * Design notes:
 *   - userId is stored as a plain Long, not a @ManyToOne User. We only ever
 *     need the id here; mapping the whole User would just cause extra loads.
 *     The foreign key still exists and is enforced by the database.
 *   - releasedAt == null  means the hold is active.
 *   - @CreationTimestamp — Hibernate fills this field in with "now" when the
 *     row is first persisted, so we never set it by hand.
 */
@Entity
@Table(name = "seat_holds")
@Getter
@Setter
@NoArgsConstructor
public class SeatHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(name = "hold_group_id", nullable = false)
    private UUID holdGroupId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "released_at")
    private Instant releasedAt;
}