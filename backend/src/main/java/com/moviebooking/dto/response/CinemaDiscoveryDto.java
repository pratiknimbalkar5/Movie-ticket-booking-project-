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
    private String sourceProvider;     // "OPEN_CINEMA_PLATFORM" | "GOOGLE_PLACES" | "OPENSTREETMAP" | "LOCAL_DATABASE"
    private String externalSourceUri;  // Link to official source / cinema portal

    // Data Transparency & Honesty Fields
    private String cinemaChain;         // Cinema chain (e.g., "PVR", "INOX", "Cinepolis", "Carnival", "Miraj")
    private String dataSource;          // "VERIFIED_LIVE_PROVIDER" | "PARTNER_DEMO_SCHEDULE" | "EXTERNAL_DIRECTORY"
    private String lastUpdated;         // ISO timestamp of last data fetch
    private String officialBookingUrl;  // Direct booking link or Google Maps reservation portal
    private String priceNote;           // "Real-time pricing available" or "Not provided by cinema data source"
    private String seatAvailabilityNote;// "Real-time seat map available" or "Not provided by cinema data source"

    // Real showtimes if bookable
    private List<NearbyTheatreShowsResponse.NearbyShowItem> shows;
}
