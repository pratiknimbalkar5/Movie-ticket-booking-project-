package com.moviebooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Represents a single screening hall inside a Theatre.
 * Maps to the "screens" table.
 *
 * Example: Theatre "PVR Cinemas" → Screen "Audi 1", "Audi 2", "Audi 3"
 * Each screen has a fixed number of seats (defined once when the screen is created).
 */
@Entity
@Table(name = "screens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Screen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    /**
     * Many-to-one: many screens belong to one theatre.
     * @JoinColumn: in the "screens" table, the FK column is named "theatre_id".
     * FetchType.LAZY: don't load the whole Theatre object when we load a Screen.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theatre_id", nullable = false)
    private Theatre theatre;

    /**
     * One screen → many seats.
     * CascadeType.ALL: seats are created/deleted with the screen.
     */
    @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Seat> seats;

    /**
     * One screen → many shows (different movies at different times).
     */
    @OneToMany(mappedBy = "screen", fetch = FetchType.LAZY)
    private List<Show> shows;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }

    public Theatre getTheatre() { return theatre; }
    public void setTheatre(Theatre theatre) { this.theatre = theatre; }

    public List<Seat> getSeats() { return seats; }
    public void setSeats(List<Seat> seats) { this.seats = seats; }

    public List<Show> getShows() { return shows; }
    public void setShows(List<Show> shows) { this.shows = shows; }
}
