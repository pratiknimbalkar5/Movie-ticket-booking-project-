package com.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Represents a single seat within a booking.
 * Maps to the "booking_seats" table.
 *
 * This is the MOST IMPORTANT table for booking correctness.
 *
 * The UNIQUE constraint on (seat_id, show_id) is the database-level
 * guarantee that the SAME SEAT cannot be booked TWICE for the SAME SHOW.
 *
 * Even if two transactions try to insert the same (seat_id, show_id) pair
 * at exactly the same millisecond, MySQL will reject the second one
 * with a constraint violation — one of them MUST fail.
 *
 * This is our "last line of defence" against double-booking.
 * The service layer adds pessimistic locking as the first line of defence.
 */
@Entity
@Table(
    name = "booking_seats",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_seat_show",       // constraint name for clear error messages
            columnNames = {"seat_id", "show_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The booking this seat entry belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    /**
     * The specific seat being booked.
     * Combined with show_id, this is UNIQUE (no double booking).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    /**
     * The show for which this seat is booked.
     * Redundant with booking.show but needed for the UNIQUE constraint
     * (we can't put a unique constraint across table relationships directly).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    /**
     * Price locked at booking time for this specific seat.
     * If the show price changes later, this historical price is preserved.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public Seat getSeat() { return seat; }
    public void setSeat(Seat seat) { this.seat = seat; }

    public Show getShow() { return show; }
    public void setShow(Show show) { this.show = show; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
