package com.moviebooking.controller;

import com.moviebooking.dto.response.LocationSuggestionDto;
import com.moviebooking.service.GeocodingService;
import com.moviebooking.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
@Tag(name = "Pan-India Location Discovery", description = "Dynamic location geocoding, autocomplete and reverse lookup for any Indian locality, PIN code, landmark or city")
public class LocationController {

    private final GeocodingService geocodingService;

    @GetMapping("/search")
    @Operation(summary = "Search any Indian city, area, locality, landmark, or PIN code with live autocomplete suggestions")
    public ResponseEntity<ApiResponse<List<LocationSuggestionDto>>> searchLocation(
            @RequestParam String query,
            @RequestParam(defaultValue = "6") int limit) {

        List<LocationSuggestionDto> suggestions = geocodingService.searchLocations(query, limit);
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    @GetMapping("/reverse")
    @Operation(summary = "Reverse geocode GPS coordinates to an exact Indian locality and city name")
    public ResponseEntity<ApiResponse<LocationSuggestionDto>> reverseGeocode(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {

        if (latitude == null || longitude == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Latitude and longitude must be provided"));
        }

        LocationSuggestionDto location = geocodingService.reverseGeocode(latitude, longitude);
        return ResponseEntity.ok(ApiResponse.success(location));
    }
}
