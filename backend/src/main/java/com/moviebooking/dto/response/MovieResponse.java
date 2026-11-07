package com.moviebooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {

    private Long id;
    private Long tmdbId;
    private String title;
    private String description;
    private String genre;
    private Integer durationMinutes;
    private String language;
    private LocalDate releaseDate;
    private String posterUrl;
    private Double rating;
    private Boolean isUpcoming;
    private Boolean isActive;
    private Boolean hasBookableShows;
    private String releaseStatus; // "NOW_SHOWING" | "UPCOMING" | "RELEASED"
    private LocalDateTime createdAt;
    private java.util.List<CastMemberDto> cast;
}
