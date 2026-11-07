package com.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a single booking transaction by a user for a show.
 * Maps to the "bookings" table.
 *
 * One booking can include multiple seats (e.g., book 3 seats at once).
 * The actual seats are stored in the booking_seats table (see BookingSeat).
 *
 * Booking lifecycle:
 *   CONFIRMED → CANCELLED (if show is > 1 hour away and booking is not already cancelled)
 */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who made this booking.
     * Many bookings can belong to one user.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The show for which this booking was made.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    /**
     * Total price paid = sum of prices of all booked seats.
     * Stored here so it's immutable even if show prices change later.
     */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.CONFIRMED;

    @CreationTimestamp
    @Column(name = "booking_time", nullable = false, updatable = false)
    private LocalDateTime bookingTime;

    /**
     * Set when the booking is cancelled. Null if not cancelled.
     */
    @Column(name = "cancellation_time")
    private LocalDateTime cancellationTime;

    /**
     * Preserved snapshot of booked seat numbers (e.g., "A1, A2").
     * Stored so that even if booking_seats rows are deleted upon cancellation,
     * the booking history retains record of which seats were originally reserved.
     */
    @Column(name = "seat_summary", length = 255)
    private String seatSummary;

    /**
     * The individual seats included in this booking.
     * CascadeType.ALL: when booking is deleted, its seat records are deleted.
     * orphanRemoval: if a BookingSeat is removed from this list, delete it from DB.
     *
     * This is central to concurrency: when we cancel, we delete these rows,
     * freeing the unique constraint so seats become bookable again.
     */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    private List<BookingSeat> bookingSeats;

    public enum BookingStatus {
        CONFIRMED, CANCELLED
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Show getShow() { return show; }
    public void setShow(Show show) { this.show = show; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public LocalDateTime getBookingTime() { return bookingTime; }
    public void setBookingTime(LocalDateTime bookingTime) { this.bookingTime = bookingTime; }

    public LocalDateTime getCancellationTime() { return cancellationTime; }
    public void setCancellationTime(LocalDateTime cancellationTime) { this.cancellationTime = cancellationTime; }

    public List<BookingSeat> getBookingSeats() { return bookingSeats; }
    public void setBookingSeats(List<BookingSeat> bookingSeats) { this.bookingSeats = bookingSeats; }

    public void addBookingSeat(BookingSeat bookingSeat) {
        if (this.bookingSeats == null) {
            this.bookingSeats = new java.util.ArrayList<>();
        }
        this.bookingSeats.add(bookingSeat);
        bookingSeat.setBooking(this);
    }
}
