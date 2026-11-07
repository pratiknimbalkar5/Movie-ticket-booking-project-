package com.moviebooking.controller;

import com.moviebooking.dto.request.MovieRequest;
import com.moviebooking.dto.request.ScreenRequest;
import com.moviebooking.dto.request.SeatRequest;
import com.moviebooking.dto.request.ShowRequest;
import com.moviebooking.dto.request.TheatreRequest;
import com.moviebooking.dto.response.BookingResponse;
import com.moviebooking.dto.response.MovieResponse;
import com.moviebooking.dto.response.ScreenResponse;
import com.moviebooking.dto.response.SeatResponse;
import com.moviebooking.dto.response.ShowResponse;
import com.moviebooking.dto.response.TheatreResponse;
import com.moviebooking.entity.Show;
import com.moviebooking.service.BookingService;
import com.moviebooking.service.MovieService;
import com.moviebooking.service.ShowService;
import com.moviebooking.service.TheatreService;
import com.moviebooking.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Operations", description = "Admin-only endpoints to manage movies, theatres, screens, shows, and view all bookings")
public class AdminController {

    private final MovieService movieService;
    private final TheatreService theatreService;
    private final ShowService showService;
    private final BookingService bookingService;

    // --- Movie Management ---

    @PostMapping("/movies")
    @Operation(summary = "Add a new movie (Admin only)")
    public ResponseEntity<ApiResponse<MovieResponse>> createMovie(@Valid @RequestBody MovieRequest request) {
        MovieResponse movie = movieService.createMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Movie created successfully", movie));
    }

    @PutMapping("/movies/{id}")
    @Operation(summary = "Update an existing movie (Admin only)")
    public ResponseEntity<ApiResponse<MovieResponse>> updateMovie(
            @PathVariable Long id,
            @Valid @RequestBody MovieRequest request) {
        MovieResponse movie = movieService.updateMovie(id, request);
        return ResponseEntity.ok(ApiResponse.success("Movie updated successfully", movie));
    }

    @DeleteMapping("/movies/{id}")
    @Operation(summary = "Soft-delete a movie (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.ok(ApiResponse.success("Movie deleted successfully", null));
    }

    // --- Theatre, Screen & Seat Management ---

    @PostMapping("/theatres")
    @Operation(summary = "Create a new theatre venue (Admin only)")
    public ResponseEntity<ApiResponse<TheatreResponse>> createTheatre(@Valid @RequestBody TheatreRequest request) {
        TheatreResponse theatre = theatreService.createTheatre(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Theatre created successfully", theatre));
    }

    @PostMapping("/theatres/{theatreId}/screens")
    @Operation(summary = "Add a screen to a theatre (Admin only)")
    public ResponseEntity<ApiResponse<ScreenResponse>> addScreen(
            @PathVariable Long theatreId,
            @Valid @RequestBody ScreenRequest request) {
        ScreenResponse screen = theatreService.addScreenToTheatre(theatreId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Screen added successfully", screen));
    }

    @PostMapping("/screens/{screenId}/seats")
    @Operation(summary = "Add seats to a screen (Admin only)")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> addSeats(
            @PathVariable Long screenId,
            @Valid @RequestBody List<SeatRequest> requests) {
        List<SeatResponse> seats = theatreService.addSeatsToScreen(screenId, requests);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Seats configured successfully", seats));
    }

    // --- Show Management ---

    @PostMapping("/shows")
    @Operation(summary = "Schedule a new show (Admin only)")
    public ResponseEntity<ApiResponse<ShowResponse>> createShow(@Valid @RequestBody ShowRequest request) {
        ShowResponse show = showService.createShow(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Show scheduled successfully", show));
    }

    @PatchMapping("/shows/{id}/status")
    @Operation(summary = "Update show status (SCHEDULED, CANCELLED, COMPLETED) (Admin only)")
    public ResponseEntity<ApiResponse<ShowResponse>> updateShowStatus(
            @PathVariable Long id,
            @RequestParam Show.ShowStatus status) {
        ShowResponse show = showService.updateShowStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Show status updated successfully", show));
    }

    // --- View All Bookings ---

    @GetMapping("/bookings")
    @Operation(summary = "View all bookings in the system (Admin only)")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<BookingResponse> bookings = bookingService.getAllBookings(pageable);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }
}
