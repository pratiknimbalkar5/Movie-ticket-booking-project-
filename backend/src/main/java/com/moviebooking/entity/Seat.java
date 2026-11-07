package com.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a physical seat in a Screen.
 * Maps to the "seats" table.
 *
 * Seats are created once when a screen is set up and never change.
 * Example: Screen "Audi 1" has seats A1, A2, ... B1, B2, ... etc.
 *
 * The UNIQUE constraint on (screen_id, seat_number) prevents
 * duplicate seat numbers within the same screen.
 */
@Entity
@Table(
    name = "seats",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_seat_number_per_screen",
            columnNames = {"screen_id", "seat_number"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable seat identifier.
     * Convention: row letter + column number, e.g. "A1", "B5", "VIP1"
     */
    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    /**
     * Seat category determines the price for this seat.
     * REGULAR < PREMIUM < VIP in terms of price.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    private SeatType seatType;

    /**
     * Many seats belong to one screen.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false)
    private Screen screen;

    public enum SeatType {
        REGULAR, PREMIUM, VIP
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public SeatType getSeatType() { return seatType; }
    public void setSeatType(SeatType seatType) { this.seatType = seatType; }

    public Screen getScreen() { return screen; }
    public void setScreen(Screen screen) { this.screen = screen; }
}
