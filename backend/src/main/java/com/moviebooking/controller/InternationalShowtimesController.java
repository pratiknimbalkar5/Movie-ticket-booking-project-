package com.moviebooking.controller;

import com.moviebooking.dto.response.internationalshowtimes.IsCinemaDto;
import com.moviebooking.dto.response.internationalshowtimes.IsMovieDto;
import com.moviebooking.service.InternationalShowtimesService;
import com.moviebooking.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/international-showtimes")
@RequiredArgsConstructor
@Tag(name = "International Showtimes V5", description = "Endpoints integrating International Showtimes API V5 for verified Indian cinema and showtime feeds")
public class InternationalShowtimesController {

    private final InternationalShowtimesService showtimesService;

    @GetMapping("/status")
    @Operation(summary = "Get current status and verification report of International Showtimes API V5")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        return ResponseEntity.ok(ApiResponse.success(showtimesService.verifyAccountAccess()));
    }

    @GetMapping("/cinemas")
    @Operation(summary = "Get verified Indian cinemas from International Showtimes API V5")
    public ResponseEntity<ApiResponse<List<IsCinemaDto>>> getIndianCinemas(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false, defaultValue = "20.0") Double radiusKm) {

        List<IsCinemaDto> cinemas;
        if (latitude != null && longitude != null) {
            cinemas = showtimesService.fetchNearbyCinemasFromApi(latitude, longitude, radiusKm);
        } else {
            cinemas = showtimesService.fetchIndianCinemasFromApi();
        }
        return ResponseEntity.ok(ApiResponse.success(cinemas));
    }

    @GetMapping("/movies")
    @Operation(summary = "Get Indian theatrical movie screenings reported by International Showtimes V5")
    public ResponseEntity<ApiResponse<List<IsMovieDto>>> getIndianMovies() {
        return ResponseEntity.ok(ApiResponse.success(showtimesService.fetchIndianMoviesFromApi()));
    }
}

