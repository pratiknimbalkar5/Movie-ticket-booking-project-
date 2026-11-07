package com.moviebooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Top-level response for cinema discovery.
 * Reports provider configuration status, search metadata, and discovered cinemas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinemaSearchResponse {

    private String searchCity;
    private Double searchLatitude;
    private Double searchLongitude;
    private Double searchRadiusKm;
    private String provider;           // Primary active provider: "OPEN_CINEMA_PLATFORM" | "GOOGLE_PLACES" | "LOCAL_DATABASE"
    private boolean providerConfigured;// True if an external cinema discovery provider key is active
    private boolean openCinemaConfigured; // True if OPEN_CINEMA_API_KEY is configured
    private boolean googlePlacesConfigured; // True if GOOGLE_PLACES_API_KEY is configured
    private String providerStatusMessage; // Human-readable status / dependency guidance
    private List<CinemaDiscoveryDto> cinemas;
}
