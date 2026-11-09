package com.moviebooking.service;

import com.moviebooking.dto.request.MovieRequest;
import com.moviebooking.dto.response.MovieResponse;
import com.moviebooking.entity.Movie;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;
    private final com.moviebooking.repository.ShowRepository showRepository;

    @Transactional(readOnly = true)
    public Page<MovieResponse> getAllActiveMovies(Pageable pageable) {
        return movieRepository.findByIsActiveTrue(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<MovieResponse> searchMovies(String title, String genre, Pageable pageable) {
        return movieRepository.searchMovies(title, genre, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public MovieResponse getMovieById(Long id) {
        Movie movie = movieRepository.findByIdAndIsActiveTrue(id)
                .or(() -> movieRepository.findByTmdbId(id))
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
        return mapToResponse(movie);
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> getTrendingMovies() {
        return movieRepository.findByIsActiveTrueOrderByRatingDesc()
                .stream()
                .limit(10)
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getAllGenres() {
        return movieRepository.findDistinctGenres();
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> getNowShowingMovies() {
        List<Long> activeMovieIds = showRepository.findMovieIdsWithActiveShows(java.time.LocalDateTime.now().minusMinutes(30));
        if (activeMovieIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return movieRepository.findByIdInAndIsActiveTrueOrderByRatingDesc(activeMovieIds)
                .stream()
                .filter(m -> m.getReleaseDate() == null || !m.getReleaseDate().isAfter(java.time.LocalDate.now()))
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovieResponse> getUpcomingMovies() {
        return movieRepository.findByIsActiveTrueOrderByRatingDesc()
                .stream()
                .filter(m -> (m.getReleaseDate() != null && m.getReleaseDate().isAfter(java.time.LocalDate.now())) || Boolean.TRUE.equals(m.getIsUpcoming()))
                .filter(m -> m.getReleaseDate() == null || m.getReleaseDate().isAfter(java.time.LocalDate.now()))
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public MovieResponse createMovie(MovieRequest request) {
        Movie movie = Movie.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .genre(request.getGenre())
                .durationMinutes(request.getDurationMinutes())
                .language(request.getLanguage())
                .releaseDate(request.getReleaseDate())
                .posterUrl(request.getPosterUrl())
                .rating(request.getRating())
                .isUpcoming(Boolean.TRUE.equals(request.getIsUpcoming()))
                .isActive(true)
                .build();

        return mapToResponse(movieRepository.save(movie));
    }

    @Transactional
    public MovieResponse updateMovie(Long id, MovieRequest request) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));

        movie.setTitle(request.getTitle());
        movie.setDescription(request.getDescription());
        movie.setGenre(request.getGenre());
        movie.setDurationMinutes(request.getDurationMinutes());
        movie.setLanguage(request.getLanguage());
        movie.setReleaseDate(request.getReleaseDate());
        if (request.getPosterUrl() != null) movie.setPosterUrl(request.getPosterUrl());
        if (request.getRating() != null) movie.setRating(request.getRating());
        if (request.getIsUpcoming() != null) movie.setIsUpcoming(request.getIsUpcoming());

        return mapToResponse(movieRepository.save(movie));
    }

    @Transactional
    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + id));
        // Soft delete
        movie.setIsActive(false);
        movieRepository.save(movie);
    }

    public MovieResponse mapToResponse(Movie movie) {
        boolean isUpcoming = movie.getReleaseDate() != null
                ? movie.getReleaseDate().isAfter(java.time.LocalDate.now())
                : Boolean.TRUE.equals(movie.getIsUpcoming());

        boolean hasShows = false;
        try {
            if (showRepository != null) {
                hasShows = showRepository.hasActiveBookableShows(movie.getId(), java.time.LocalDateTime.now().minusMinutes(30));
            }
        } catch (Exception ignored) {}

        String releaseStatus = isUpcoming ? "UPCOMING" : (hasShows ? "NOW_SHOWING" : "RELEASED");

        return MovieResponse.builder()
                .id(movie.getId())
                .tmdbId(movie.getTmdbId())
                .imdbId(movie.getImdbId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .genre(movie.getGenre())
                .durationMinutes(movie.getDurationMinutes())
                .language(movie.getLanguage())
                .releaseDate(movie.getReleaseDate())
                .posterUrl(movie.getPosterUrl())
                .rating(movie.getRating())
                .isUpcoming(isUpcoming)
                .hasBookableShows(hasShows)
                .releaseStatus(releaseStatus)
                .isActive(movie.getIsActive())
                .createdAt(movie.getCreatedAt())
                .build();
    }
}
