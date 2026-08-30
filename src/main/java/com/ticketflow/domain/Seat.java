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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One physical seat belonging to one event.
 *
 * @ManyToOne — many seats point to one event. This maps the "event_id" foreign
 *   key column to an actual Event object.
 *
 * fetch = LAZY — do NOT automatically run a second query to load the Event
 *   every time we load a Seat. We ask for the event explicitly when we need it.
 *   (The JPA default for @ManyToOne is EAGER, which quietly causes a storm of
 *   extra queries. Setting LAZY here is deliberate.)
 *
 * optional = false — the relationship is mandatory (matches NOT NULL on the
 *   column).
 */
@Entity
@Table(name = "seats")
@Getter
@Setter
@NoArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private String section;

    @Column(name = "row_label", nullable = false)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;
}