package com.moviebooking.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@Tag(name = "System", description = "Root and system health endpoints")
public class HomeController {

    @GetMapping("/")
    @Operation(summary = "Root welcoming and health status endpoint")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "ONLINE");
        status.put("application", "CinemaPulse Movie Ticket Booking System");
        status.put("version", "1.0.0");
        status.put("apiDocumentation", "/swagger-ui/index.html");
        status.put("frontendUrl", "http://localhost:3000");
        status.put("nowPlayingEndpoint", "/api/movies/now-playing?region=IN&page=1");
        status.put("upcomingEndpoint", "/api/movies/upcoming?region=IN&page=1");
        status.put("popularEndpoint", "/api/movies/popular?page=1");
        status.put("trendingEndpoint", "/api/movies/trending?window=day");
        status.put("cinemasEndpoint", "/api/theatres/cities");
        return ResponseEntity.ok(status);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check probe")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
