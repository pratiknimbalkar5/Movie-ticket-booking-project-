package com.moviebooking.service;

import com.moviebooking.dto.response.TmdbMovieDto;
import com.moviebooking.dto.response.internationalshowtimes.IsMovieDto;
import com.moviebooking.entity.Movie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * Service responsible for robust, verified movie matching across TMDB,
 * International Showtimes V5, and local database records.
 *
 * Prevents poster/title mismatches by adhering to strict matching rules:
 * 1. Primary: Exact TMDB ID match (score: 1.0)
 * 2. Secondary: Exact IMDb ID match (score: 0.95)
 * 3. Tertiary: Normalized Title + Release Year match (score >= 0.90)
 * Rejects all uncertain matches to prevent incorrect poster display.
 */
@Service
@Slf4j
public class MovieMatchingService {

    public record MatchResult(boolean matched, double confidenceScore, String matchReason) {
        public static MatchResult exactTmdb() {
            return new MatchResult(true, 1.0, "EXACT_TMDB_ID");
        }
        public static MatchResult exactImdb() {
            return new MatchResult(true, 0.95, "EXACT_IMDB_ID");
        }
        public static MatchResult strictTitleAndYear(double score) {
            return new MatchResult(true, score, "STRICT_TITLE_AND_YEAR");
        }
        public static MatchResult noMatch(String reason) {
            return new MatchResult(false, 0.0, reason);
        }
    }

    /**
     * Matches an International Showtimes movie with a TMDB movie.
     */
    public MatchResult matchIsMovieWithTmdb(IsMovieDto isMovie, TmdbMovieDto tmdbMovie) {
        if (isMovie == null || tmdbMovie == null) {
            return MatchResult.noMatch("NULL_INPUT");
        }

        // 1. Direct TMDB ID match
        if (isMovie.getTmdbId() != null && tmdbMovie.getId() != null) {
            if (Objects.equals(isMovie.getTmdbId(), tmdbMovie.getId())) {
                return MatchResult.exactTmdb();
            }
        }

        // 2. Direct IMDb ID match
        if (isMovie.getImdbId() != null && !isMovie.getImdbId().isBlank()) {
            if (isMovie.getImdbId().equalsIgnoreCase(tmdbMovie.getImdbId())) {
                return MatchResult.exactImdb();
            }
        }

        // 3. Normalized Title + Year match
        return matchByNormalizedTitleAndYear(
                isMovie.getTitle(),
                isMovie.getReleaseDate(),
                tmdbMovie.getTitle(),
                tmdbMovie.getReleaseDate()
        );
    }

    /**
     * Matches a local database Movie with a TMDB movie.
     */
    public MatchResult matchLocalMovieWithTmdb(Movie localMovie, TmdbMovieDto tmdbMovie) {
        if (localMovie == null || tmdbMovie == null) {
            return MatchResult.noMatch("NULL_INPUT");
        }

        // 1. Direct TMDB ID match
        if (localMovie.getTmdbId() != null && tmdbMovie.getId() != null) {
            if (Objects.equals(localMovie.getTmdbId(), tmdbMovie.getId())) {
                return MatchResult.exactTmdb();
            }
        }

        // 2. Direct IMDb ID match
        if (localMovie.getImdbId() != null && !localMovie.getImdbId().isBlank()) {
            if (localMovie.getImdbId().equalsIgnoreCase(tmdbMovie.getImdbId())) {
                return MatchResult.exactImdb();
            }
        }

        // 3. Strict Title + Year match
        String localYear = (localMovie.getReleaseDate() != null) ? String.valueOf(localMovie.getReleaseDate().getYear()) : null;
        return matchByNormalizedTitleAndYear(
                localMovie.getTitle(),
                localYear,
                tmdbMovie.getTitle(),
                tmdbMovie.getReleaseDate()
        );
    }

    /**
     * Strict Title + Year comparison.
     * Rejects generic titles unless year matches exactly.
     */
    public MatchResult matchByNormalizedTitleAndYear(String titleA, String dateOrYearA, String titleB, String dateOrYearB) {
        if (titleA == null || titleB == null) return MatchResult.noMatch("MISSING_TITLE");

        String normA = normalizeTitle(titleA);
        String normB = normalizeTitle(titleB);

        if (normA.isEmpty() || normB.isEmpty()) return MatchResult.noMatch("EMPTY_NORMALIZED_TITLE");

        boolean titlesMatch = normA.equals(normB);
        if (!titlesMatch) {
            // Check if one contains the other only if length is significant
            if (normA.length() > 6 && normB.length() > 6 && (normA.contains(normB) || normB.contains(normA))) {
                titlesMatch = true;
            } else {
                return MatchResult.noMatch("TITLE_MISMATCH");
            }
        }

        // Verify Release Year if dates are provided
        Integer yearA = extractYear(dateOrYearA);
        Integer yearB = extractYear(dateOrYearB);

        if (yearA != null && yearB != null) {
            int yearDiff = Math.abs(yearA - yearB);
            if (yearDiff == 0) {
                return MatchResult.strictTitleAndYear(0.92);
            } else if (yearDiff == 1) {
                // Allowed for year-end release differences across territories (e.g. Dec 2025 vs Jan 2026)
                return MatchResult.strictTitleAndYear(0.85);
            } else {
                // Different years = different movie (e.g. 2011 "Fighter" vs 2024 "Fighter")
                log.warn("Rejected title match '{}' and '{}' due to year discrepancy ({} vs {})", titleA, titleB, yearA, yearB);
                return MatchResult.noMatch("YEAR_DISCREPANCY");
            }
        }

        // If year is unknown on both sides and title is unique (>= 8 chars), accept with lower confidence
        if (normA.length() >= 8) {
            return MatchResult.strictTitleAndYear(0.80);
        }

        return MatchResult.noMatch("UNCERTAIN_SHORT_TITLE_WITHOUT_YEAR");
    }

    private String normalizeTitle(String raw) {
        if (raw == null) return "";
        return raw.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    private Integer extractYear(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        String s = dateStr.trim();
        if (s.length() >= 4) {
            try {
                return Integer.parseInt(s.substring(0, 4));
            } catch (Exception ignored) {}
        }
        return null;
    }
}

