package com.moviebooking.controller;

import com.moviebooking.dto.response.ScreenResponse;
import com.moviebooking.dto.response.TheatreResponse;
import com.moviebooking.service.TheatreService;
import com.moviebooking.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/theatres")
@RequiredArgsConstructor
@Tag(name = "Theatres", description = "Public endpoints to browse theatres and screens")
public class TheatreController {

    private final TheatreService theatreService;

    @GetMapping(value = {"", "/by-city"})
    @Operation(summary = "Get all active theatres, optionally filtered by city")
    public ResponseEntity<ApiResponse<List<TheatreResponse>>> getAllTheatres(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String city) {
        List<TheatreResponse> theatres = (city != null && !city.isBlank())
                ? theatreService.getTheatresByCity(city)
                : theatreService.getAllActiveTheatres();
        return ResponseEntity.ok(ApiResponse.success(theatres));
    }

    @GetMapping("/cities")
    @Operation(summary = "Get all cities with active theatres")
    public ResponseEntity<ApiResponse<List<String>>> getCities() {
        return ResponseEntity.ok(ApiResponse.success(theatreService.getAllCities()));
    }

    @GetMapping("/{id:[0-9]+}")
    @Operation(summary = "Get theatre details by ID")
    public ResponseEntity<ApiResponse<TheatreResponse>> getTheatreById(@PathVariable Long id) {
        TheatreResponse theatre = theatreService.getTheatreById(id);
        return ResponseEntity.ok(ApiResponse.success(theatre));
    }

    @GetMapping("/{id:[0-9]+}/screens")
    @Operation(summary = "Get all screens for a specific theatre")
    public ResponseEntity<ApiResponse<List<ScreenResponse>>> getScreensByTheatre(@PathVariable Long id) {
        List<ScreenResponse> screens = theatreService.getScreensByTheatre(id);
        return ResponseEntity.ok(ApiResponse.success(screens));
    }
}
