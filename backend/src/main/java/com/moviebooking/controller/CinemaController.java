package com.moviebooking.controller;

import com.moviebooking.dto.response.CinemaSearchResponse;
import com.moviebooking.service.GooglePlacesService;
import com.moviebooking.service.OpenCinemaService;
import com.moviebooking.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/cinemas")
@RequiredArgsConstructor
@Tag(name = "Cinemas Discovery", description = "Endpoints for discovering nearby cinemas via Google Places API (New) and local booking system")
public class CinemaController {

    private final GooglePlacesService googlePlacesService;
    private final OpenCinemaService openCinemaService;

    @GetMapping("/nearby")
    @Operation(summary = "Discover nearby cinemas and shows by city or GPS coordinates (Open Cinema Platform + Google Places API + Local Inventory)")
    public ResponseEntity<ApiResponse<CinemaSearchResponse>> getNearbyCinemas(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false, defaultValue = "15.0") Double radiusKm,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        CinemaSearchResponse response = googlePlacesService.discoverNearbyCinemas(
                city, latitude, longitude, radiusKm, movieId, date
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/coordinates")
    @Operation(summary = "Resolve verified geographical coordinates for a city name in India")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resolveCityCoordinates(
            @RequestParam String city) {

        GooglePlacesService.Coordinates coords = googlePlacesService.resolveCityCoordinates(city);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "city", coords.canonicalCity(),
                "latitude", coords.latitude(),
                "longitude", coords.longitude()
        )));
    }

    @GetMapping("/provider-status")
    @Operation(summary = "Check status of external cinema providers (Open Cinema Platform & Google Places API)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProviderStatus() {
        boolean openCinemaConfigured = (openCinemaService != null && openCinemaService.isConfigured());
        boolean googlePlacesConfigured = googlePlacesService.isConfigured();

        String provider;
        if (openCinemaConfigured && googlePlacesConfigured) {
            provider = "OPEN_CINEMA_PLATFORM, GOOGLE_PLACES";
        } else if (openCinemaConfigured) {
            provider = "OPEN_CINEMA_PLATFORM";
        } else if (googlePlacesConfigured) {
            provider = "GOOGLE_PLACES";
        } else {
            provider = "LOCAL_DATABASE";
        }

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "openCinemaConfigured", openCinemaConfigured,
                "googlePlacesConfigured", googlePlacesConfigured,
                "provider", provider,
                "openCinemaBaseUrl", (openCinemaService != null && openCinemaService.getBaseUrl() != null)
                        ? openCinemaService.getBaseUrl()
                        : "https://api.opencinemaproject.com/v1",
                "message", (openCinemaConfigured || googlePlacesConfigured)
                        ? "External cinema discovery provider(s) active on backend."
                        : "No external API keys set (OPEN_CINEMA_API_KEY / GOOGLE_PLACES_API_KEY). System safely falls back to local database registered cinemas."
        )));
    }
}
