package com.moviebooking.controller;

import com.moviebooking.dto.response.SeatResponse;
import com.moviebooking.dto.response.ShowResponse;
import com.moviebooking.service.ShowService;
import com.moviebooking.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
@Tag(name = "Shows", description = "Public endpoints to discover shows, pricing, and live seat availability")
public class ShowController {

    private final ShowService showService;

    @GetMapping
    @Operation(summary = "Search scheduled shows by movie ID and/or city")
    public ResponseEntity<ApiResponse<Page<ShowResponse>>> searchShows(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<ShowResponse> shows = showService.searchShows(movieId, city, pageable);
        return ResponseEntity.ok(ApiResponse.success(shows));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Get real-time nearby shows grouped by theatre with live seat availability")
    public ResponseEntity<ApiResponse<List<com.moviebooking.dto.response.NearbyTheatreShowsResponse>>> getNearbyShows(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double radiusKm,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {

        List<com.moviebooking.dto.response.NearbyTheatreShowsResponse> nearby = showService.getNearbyShows(city, movieId, latitude, longitude, radiusKm, date);
        return ResponseEntity.ok(ApiResponse.success(nearby));
    }

    @GetMapping("/scan-nearby")
    @Operation(summary = "Scan cinemas near GPS coordinates (latitude, longitude) sorted by exact distance")
    public ResponseEntity<ApiResponse<List<com.moviebooking.dto.response.NearbyTheatreShowsResponse>>> scanNearbyShows(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false, defaultValue = "50.0") Double radiusKm,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {

        List<com.moviebooking.dto.response.NearbyTheatreShowsResponse> nearby = showService.scanNearbyShowsByGps(latitude, longitude, radiusKm, date);
        return ResponseEntity.ok(ApiResponse.success(nearby));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get show details and pricing tiers")
    public ResponseEntity<ApiResponse<ShowResponse>> getShowById(@PathVariable Long id) {
        ShowResponse show = showService.getShowById(id);
        return ResponseEntity.ok(ApiResponse.success(show));
    }

    @GetMapping("/{id}/seats")
    @Operation(summary = "Get seat map and live seat availability for a show")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getSeatsForShow(@PathVariable Long id) {
        List<SeatResponse> seats = showService.getSeatAvailabilityForShow(id);
        return ResponseEntity.ok(ApiResponse.success(seats));
    }
}
