package com.moviebooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a cinema discovered either via Google Places API (New) or local database.
 * Explicitly distinguishes external discovery from registered/bookable theatres.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinemaDiscoveryDto {

    private String id;                 // Google Place ID or local ID string
    private String name;               // Cinema name (e.g. "PVR INOX Palladium")
    private String address;            // Full physical address
    private String city;               // City name
    private Double latitude;           // Latitude coordinate
    private Double longitude;          // Longitude coordinate
    private Double distanceKm;         // Exact Haversine distance in km from search coordinates
    private String googleMapsUri;      // Real Google Maps directions / view link
    private Double rating;             // Rating (e.g. 4.4 / 5.0)
    private Integer userRatingCount;   // Number of reviews/ratings

    // Status distinctions
    private boolean isRegistered;      // True if registered in our booking system
    private Long theatreId;            // Local database Theatre ID if registered
    private boolean hasBookableShows;  // True if active scheduled shows exist
    private String cinemaType;         // "BOOKABLE_PARTNER" | "REGISTERED_THEATRE" | "EXTERNAL_DISCOVERY"
    private String sourceProvider;     // "OPEN_CINEMA_PLATFORM" | "GOOGLE_PLACES" | "LOCAL_DATABASE"
    private String externalSourceUri;  // Link to official source / cinema portal

    // Real showtimes if bookable
    private List<NearbyTheatreShowsResponse.NearbyShowItem> shows;
}
