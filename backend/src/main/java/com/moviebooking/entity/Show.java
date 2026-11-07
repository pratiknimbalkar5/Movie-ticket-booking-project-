package com.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a scheduled screening of a Movie in a Screen.
 * Maps to the "shows" table.
 *
 * A show links a Movie + Screen at a specific date/time.
 * It stores pricing for each seat type (REGULAR, PREMIUM, VIP).
 *
 * Example: "Inception" showing in "Audi 1" on "2025-11-01 18:30" at Rs.200/300/500
 */
@Entity
@Table(name = "shows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many shows can be for the same movie (same movie, different times/theatres).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    /**
     * Many shows can play in the same screen (different movies, different days).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    @Column(name = "show_time", nullable = false)
    private LocalDateTime showTime;

    /**
     * Tiered pricing based on seat type.
     * BigDecimal is used for money — never use float/double for currency
     * because of floating-point precision issues.
     */
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;       // for REGULAR seats

    @Column(name = "premium_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal premiumPrice;    // for PREMIUM seats

    @Column(name = "vip_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal vipPrice;        // for VIP seats

    /**
     * Lifecycle of a show:
     * SCHEDULED → COMPLETED (automatically after showTime passes)
     * SCHEDULED → CANCELLED (admin cancels it)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShowStatus status = ShowStatus.SCHEDULED;

    /**
     * A show has many bookings (different users book different seats).
     */
    @OneToMany(mappedBy = "show", fetch = FetchType.LAZY)
    private List<Booking> bookings;

    /**
     * A show has many booking-seat records (the actual booked seat entries).
     */
    @OneToMany(mappedBy = "show", fetch = FetchType.LAZY)
    private List<BookingSeat> bookedSeats;

    public enum ShowStatus {
        SCHEDULED, CANCELLED, COMPLETED
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }

    public Screen getScreen() { return screen; }
    public void setScreen(Screen screen) { this.screen = screen; }

    public LocalDateTime getShowTime() { return showTime; }
    public void setShowTime(LocalDateTime showTime) { this.showTime = showTime; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public BigDecimal getPremiumPrice() { return premiumPrice; }
    public void setPremiumPrice(BigDecimal premiumPrice) { this.premiumPrice = premiumPrice; }

    public BigDecimal getVipPrice() { return vipPrice; }
    public void setVipPrice(BigDecimal vipPrice) { this.vipPrice = vipPrice; }

    public ShowStatus getStatus() { return status; }
    public void setStatus(ShowStatus status) { this.status = status; }

    public List<Booking> getBookings() { return bookings; }
    public void setBookings(List<Booking> bookings) { this.bookings = bookings; }

    public List<BookingSeat> getBookedSeats() { return bookedSeats; }
    public void setBookedSeats(List<BookingSeat> bookedSeats) { this.bookedSeats = bookedSeats; }
}
