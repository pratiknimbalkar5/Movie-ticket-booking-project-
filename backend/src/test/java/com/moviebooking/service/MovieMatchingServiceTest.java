package com.moviebooking.service;

import com.moviebooking.dto.response.TmdbMovieDto;
import com.moviebooking.dto.response.internationalshowtimes.IsMovieDto;
import com.moviebooking.entity.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MovieMatchingServiceTest {

    private MovieMatchingService matchingService;

    @BeforeEach
    void setUp() {
        matchingService = new MovieMatchingService();
    }

    @Test
    @DisplayName("Matches movies with identical TMDB ID with 1.0 confidence")
    void testMatchByTmdbId() {
        IsMovieDto isMovie = IsMovieDto.builder()
                .id("is_123")
                .title("Hope")
                .tmdbId(1058424L)
                .build();

        TmdbMovieDto tmdbMovie = TmdbMovieDto.builder()
                .id(1058424L)
                .title("Hope")
                .build();

        var result = matchingService.matchIsMovieWithTmdb(isMovie, tmdbMovie);
        assertThat(result.matched()).isTrue();
        assertThat(result.confidenceScore()).isEqualTo(1.0);
        assertThat(result.matchReason()).isEqualTo("EXACT_TMDB_ID");
    }

    @Test
    @DisplayName("Matches movies with identical IMDb ID with 0.95 confidence")
    void testMatchByImdbId() {
        IsMovieDto isMovie = IsMovieDto.builder()
                .id("is_456")
                .title("Hope")
                .imdbId("tt27369017")
                .build();

        TmdbMovieDto tmdbMovie = TmdbMovieDto.builder()
                .id(999999L)
                .imdbId("tt27369017")
                .title("Hope")
                .build();

        var result = matchingService.matchIsMovieWithTmdb(isMovie, tmdbMovie);
        assertThat(result.matched()).isTrue();
        assertThat(result.confidenceScore()).isEqualTo(0.95);
        assertThat(result.matchReason()).isEqualTo("EXACT_IMDB_ID");
    }

    @Test
    @DisplayName("Matches movies by title and identical release year")
    void testMatchByTitleAndYear() {
        IsMovieDto isMovie = IsMovieDto.builder()
                .id("is_789")
                .title("Clayface")
                .releaseDate("2026-10-23")
                .build();

        TmdbMovieDto tmdbMovie = TmdbMovieDto.builder()
                .id(1400940L)
                .title("Clayface")
                .releaseDate("2026-10-23")
                .build();

        var result = matchingService.matchIsMovieWithTmdb(isMovie, tmdbMovie);
        assertThat(result.matched()).isTrue();
        assertThat(result.confidenceScore()).isGreaterThanOrEqualTo(0.90);
        assertThat(result.matchReason()).isEqualTo("STRICT_TITLE_AND_YEAR");
    }

    @Test
    @DisplayName("Rejects movies with same title but different release years (e.g. 2011 vs 2024)")
    void testRejectTitleMatchWithYearDiscrepancy() {
        IsMovieDto isMovie = IsMovieDto.builder()
                .id("is_old")
                .title("Fighter")
                .releaseDate("2011-04-15")
                .build();

        TmdbMovieDto tmdbMovie = TmdbMovieDto.builder()
                .id(12345L)
                .title("Fighter")
                .releaseDate("2024-01-25")
                .build();

        var result = matchingService.matchIsMovieWithTmdb(isMovie, tmdbMovie);
        assertThat(result.matched()).isFalse();
        assertThat(result.matchReason()).isEqualTo("YEAR_DISCREPANCY");
    }

    @Test
    @DisplayName("Matches local database Movie entity with TMDB movie by TMDB ID")
    void testMatchLocalMovieWithTmdb() {
        Movie localMovie = Movie.builder()
                .id(1L)
                .tmdbId(1058424L)
                .title("Hope")
                .releaseDate(LocalDate.of(2026, 10, 23))
                .build();

        TmdbMovieDto tmdbMovie = TmdbMovieDto.builder()
                .id(1058424L)
                .title("Hope")
                .build();

        var result = matchingService.matchLocalMovieWithTmdb(localMovie, tmdbMovie);
        assertThat(result.matched()).isTrue();
        assertThat(result.confidenceScore()).isEqualTo(1.0);
    }
}

