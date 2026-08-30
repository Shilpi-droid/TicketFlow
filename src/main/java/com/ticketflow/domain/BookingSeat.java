package com.ticketflow.domain;

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
 * The permanent record that a specific seat was sold on a specific booking.
 *
 * The UNIQUE (seat_id) constraint on this table is THE guarantee of the whole
 * project: a seat can be written here at most once, ever. Everything else —
 * holds, locks, expiry — is there to make this constraint rarely fire. When it
 * does fire, it means two code paths both thought they could sell the seat, and
 * the database stopped the second one.
 *
 * This is a join table, but we give it its own surrogate `id` so Spring Data
 * repositories stay simple (composite keys need extra ceremony).
 */
@Entity
@Table(name = "booking_seats")
@Getter
@Setter
@NoArgsConstructor
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;
}