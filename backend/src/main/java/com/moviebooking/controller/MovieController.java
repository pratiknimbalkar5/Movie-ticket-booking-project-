package com.moviebooking.controller;

import com.moviebooking.dto.response.MovieResponse;
import com.moviebooking.dto.response.TmdbMovieDto;
import com.moviebooking.dto.response.TmdbPageResponse;
import com.moviebooking.service.MovieService;
import com.moviebooking.service.TmdbMovieService;
import com.moviebooking.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Tag(name = "Movies", description = "Public endpoints to browse, search and discover live movies via TMDB and local database")
public class MovieController {

    private final MovieService movieService;
    private final TmdbMovieService tmdbMovieService;

    @GetMapping
    @Operation(summary = "Get paginated list of all active local movies")
    public ResponseEntity<ApiResponse<Page<MovieResponse>>> getMovies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "releaseDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<MovieResponse> movies = movieService.getAllActiveMovies(pageable);
        return ResponseEntity.ok(ApiResponse.success(movies));
    }

    @GetMapping("/now-playing")
    @Operation(summary = "Get now playing movies from TMDB")
    public ResponseEntity<ApiResponse<TmdbPageResponse<TmdbMovieDto>>> getNowPlaying(
            @RequestParam(defaultValue = "IN") String region,
            @RequestParam(defaultValue = "1") int page) {
        try {
            return ResponseEntity.ok(ApiResponse.success(tmdbMovieService.getNowPlaying(region, page)));
        } catch (com.moviebooking.exception.TmdbAuthException e) {
            throw e;
        } catch (Exception e) {
            // Graceful fallback to verified local scheduled movies with valid bookable shows
            List<MovieResponse> localMovies = movieService.getNowShowingMovies();
            List<TmdbMovieDto> dtoList = localMovies.stream().map(m -> TmdbMovieDto.builder()
                    .id(m.getId())
                    .localMovieId(m.getId())
                    .title(m.getTitle())
                    .overview(m.getDescription())
                    .genre(m.getGenre())
                    .rating(m.getRating())
                    .language(m.getLanguage())
                    .posterUrl(m.getPosterUrl())
                    .releaseDate(m.getReleaseDate() != null ? m.getReleaseDate().toString() : null)
                    .isUpcoming(false)
                    .build()).toList();

            TmdbPageResponse<TmdbMovieDto> fallbackResponse = TmdbPageResponse.<TmdbMovieDto>builder()
                    .page(1)
                    .totalPages(1)
                    .totalResults(dtoList.size())
                    .results(dtoList)
                    .cached(true)
                    .build();
            return ResponseEntity.ok(ApiResponse.success(fallbackResponse));
        }
    }

    @GetMapping("/latest-releases")
    @Operation(summary = "Get latest theatrical releases in India within recency window (default 30 days) ordered newest first")
    public ResponseEntity<ApiResponse<TmdbPageResponse<TmdbMovieDto>>> getLatestReleases(
            @RequestParam(defaultValue = "IN") String region,
            @RequestParam(defaultValue = "30") int recencyDays,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(ApiResponse.success(tmdbMovieService.getLatestReleases(region, recencyDays, page)));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming movies from TMDB")
    public ResponseEntity<ApiResponse<TmdbPageResponse<TmdbMovieDto>>> getUpcoming(
            @RequestParam(defaultValue = "IN") String region,
            @RequestParam(defaultValue = "1") int page) {
        try {
            return ResponseEntity.ok(ApiResponse.success(tmdbMovieService.getUpcoming(region, page)));
        } catch (com.moviebooking.exception.TmdbAuthException e) {
            throw e;
        } catch (Exception e) {
            Page<MovieResponse> localPage = movieService.getAllActiveMovies(PageRequest.of(0, 20));
            List<TmdbMovieDto> dtoList = localPage.getContent().stream()
                    .filter(m -> Boolean.TRUE.equals(m.getIsUpcoming()))
                    .map(m -> TmdbMovieDto.builder()
                            .id(m.getId())
                            .localMovieId(m.getId())
                            .title(m.getTitle())
                            .overview(m.getDescription())
                            .genre(m.getGenre())
                            .rating(m.getRating())
                            .language(m.getLanguage())
                            .posterUrl(m.getPosterUrl())
                            .releaseDate(m.getReleaseDate() != null ? m.getReleaseDate().toString() : null)
                            .isUpcoming(true)
                            .build()).toList();

            TmdbPageResponse<TmdbMovieDto> fallbackResponse = TmdbPageResponse.<TmdbMovieDto>builder()
                    .page(1)
                    .totalPages(1)
                    .totalResults(dtoList.size())
                    .results(dtoList)
                    .cached(true)
                    .build();
            return ResponseEntity.ok(ApiResponse.success(fallbackResponse));
        }
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular movies from TMDB")
    public ResponseEntity<ApiResponse<TmdbPageResponse<TmdbMovieDto>>> getPopular(
            @RequestParam(defaultValue = "1") int page) {
        try {
            return ResponseEntity.ok(ApiResponse.success(tmdbMovieService.getPopular(page)));
        } catch (com.moviebooking.exception.TmdbAuthException e) {
            throw e;
        } catch (Exception e) {
            List<MovieResponse> localMovies = movieService.getTrendingMovies();
            List<TmdbMovieDto> dtoList = localMovies.stream().map(m -> TmdbMovieDto.builder()
                    .id(m.getId())
                    .localMovieId(m.getId())
                    .title(m.getTitle())
                    .overview(m.getDescription())
                    .genre(m.getGenre())
                    .rating(m.getRating())
                    .language(m.getLanguage())
                    .posterUrl(m.getPosterUrl())
                    .releaseDate(m.getReleaseDate() != null ? m.getReleaseDate().toString() : null)
                    .isUpcoming(false)
                    .build()).toList();

            TmdbPageResponse<TmdbMovieDto> fallbackResponse = TmdbPageResponse.<TmdbMovieDto>builder()
                    .page(1)
                    .totalPages(1)
                    .totalResults(dtoList.size())
                    .results(dtoList)
                    .cached(true)
                    .build();
            return ResponseEntity.ok(ApiResponse.success(fallbackResponse));
        }
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending movies from TMDB")
    public ResponseEntity<ApiResponse<TmdbPageResponse<TmdbMovieDto>>> getTrending(
            @RequestParam(defaultValue = "day") String window,
            @RequestParam(defaultValue = "1") int page) {
        try {
            return ResponseEntity.ok(ApiResponse.success(tmdbMovieService.getTrending(window, page)));
        } catch (com.moviebooking.exception.TmdbAuthException e) {
            throw e;
        } catch (Exception e) {
            List<MovieResponse> localMovies = movieService.getTrendingMovies();
            List<TmdbMovieDto> dtoList = localMovies.stream().map(m -> TmdbMovieDto.builder()
                    .id(m.getId())
                    .localMovieId(m.getId())
                    .title(m.getTitle())
                    .overview(m.getDescription())
                    .genre(m.getGenre())
                    .rating(m.getRating())
                    .language(m.getLanguage())
                    .posterUrl(m.getPosterUrl())
                    .releaseDate(m.getReleaseDate() != null ? m.getReleaseDate().toString() : null)
                    .isUpcoming(false)
                    .build()).toList();

            TmdbPageResponse<TmdbMovieDto> fallbackResponse = TmdbPageResponse.<TmdbMovieDto>builder()
                    .page(1)
                    .totalPages(1)
                    .totalResults(dtoList.size())
                    .results(dtoList)
                    .cached(true)
                    .build();
            return ResponseEntity.ok(ApiResponse.success(fallbackResponse));
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search movies via TMDB query or local catalog")
    public ResponseEntity<ApiResponse<?>> searchMovies(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        String effectiveQuery = (query != null && !query.isBlank()) ? query.trim() : (title != null ? title.trim() : null);
        if (effectiveQuery != null && !effectiveQuery.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(tmdbMovieService.searchMovies(effectiveQuery, page)));
        }

        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size);
        Page<MovieResponse> movies = movieService.searchMovies(null, genre, pageable);
        return ResponseEntity.ok(ApiResponse.success(movies));
    }

    @GetMapping("/tmdb/{tmdbId}")
    @Operation(summary = "Get TMDB movie details by TMDB ID")
    public ResponseEntity<ApiResponse<TmdbMovieDto>> getTmdbMovieById(@PathVariable Long tmdbId) {
        return ResponseEntity.ok(ApiResponse.success(tmdbMovieService.getMovieDetails(tmdbId)));
    }

    @PostMapping("/tmdb/{tmdbId}/sync-local")
    @Operation(summary = "Synchronize a TMDB movie to local database for show scheduling and seat booking")
    public ResponseEntity<ApiResponse<MovieResponse>> syncLocalMovie(@PathVariable Long tmdbId) {
        var localMovie = tmdbMovieService.getOrCreateLocalMovie(tmdbId);
        return ResponseEntity.ok(ApiResponse.success(movieService.getMovieById(localMovie.getId())));
    }

    @GetMapping("/now-showing")
    @Operation(summary = "Get current movies playing in cinemas (falls back to local database if TMDB is not configured)")
    public ResponseEntity<ApiResponse<?>> getNowShowingMovies(
            @RequestParam(defaultValue = "IN") String region,
            @RequestParam(defaultValue = "1") int page) {
        if (tmdbMovieService.isConfigured()) {
            return ResponseEntity.ok(ApiResponse.success(tmdbMovieService.getNowPlaying(region, page)));
        }
        return ResponseEntity.ok(ApiResponse.success(movieService.getNowShowingMovies()));
    }

    @GetMapping("/local")
    @Operation(summary = "Get local database active movies")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getLocalMovies() {
        return ResponseEntity.ok(ApiResponse.success(movieService.getNowShowingMovies()));
    }

    @GetMapping("/config-status")
    @Operation(summary = "Check TMDB configuration status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfigStatus() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "tmdbConfigured", tmdbMovieService.isConfigured()
        )));
    }

    @PostMapping("/sync-live")
    @Operation(summary = "Trigger live IMDb / TMDB sync for latest now playing and upcoming movies")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncLiveMovies() {
        return ResponseEntity.ok(ApiResponse.success("IMDb/TMDb live sync completed", tmdbMovieService.syncLiveMoviesFromImdb()));
    }

    @GetMapping("/live-search")
    @Operation(summary = "Search real-time movies (supports online TMDB and local database)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchLiveMovies(
            @RequestParam(defaultValue = "") String query) {
        return ResponseEntity.ok(ApiResponse.success(tmdbMovieService.searchRealtimeMovies(query)));
    }

    @GetMapping("/genres")
    @Operation(summary = "Get list of all active movie genres")
    public ResponseEntity<ApiResponse<List<String>>> getGenres() {
        return ResponseEntity.ok(ApiResponse.success(movieService.getAllGenres()));
    }

    @GetMapping("/{id:[0-9]+}/cast")
    @Operation(summary = "Get movie cast members with character roles and profile photos")
    public ResponseEntity<ApiResponse<List<com.moviebooking.dto.response.CastMemberDto>>> getMovieCast(@PathVariable Long id) {
        Long tmdbId = id;
        try {
            MovieResponse local = movieService.getMovieById(id);
            if (local != null && local.getTmdbId() != null) {
                tmdbId = local.getTmdbId();
            }
        } catch (Exception ignored) {}

        List<com.moviebooking.dto.response.CastMemberDto> cast = tmdbMovieService.getMovieCast(tmdbId);
        return ResponseEntity.ok(ApiResponse.success(cast));
    }

    @GetMapping("/{id:[0-9]+}")
    @Operation(summary = "Get movie details by internal ID or TMDB ID")
    public ResponseEntity<ApiResponse<MovieResponse>> getMovieById(@PathVariable Long id) {
        try {
            MovieResponse movie = movieService.getMovieById(id);
            if (movie != null && tmdbMovieService.isConfigured()) {
                Long lookupId = (movie.getTmdbId() != null) ? movie.getTmdbId() : id;
                try {
                    movie.setCast(tmdbMovieService.getMovieCast(lookupId));
                } catch (Exception ignored) {}
            }
            return ResponseEntity.ok(ApiResponse.success(movie));
        } catch (com.moviebooking.exception.ResourceNotFoundException ex) {
            if (tmdbMovieService.isConfigured()) {
                try {
                    var localMovie = tmdbMovieService.getOrCreateLocalMovie(id);
                    MovieResponse res = movieService.getMovieById(localMovie.getId());
                    try {
                        res.setCast(tmdbMovieService.getMovieCast(id));
                    } catch (Exception ignored) {}
                    return ResponseEntity.ok(ApiResponse.success(res));
                } catch (Exception ignored) {
                }
            }
            throw ex;
        }
    }
}
