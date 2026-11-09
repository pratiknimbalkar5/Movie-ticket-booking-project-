package com.moviebooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmdbMovieDto {
    private Long id;              // TMDB external ID
    private String imdbId;        // IMDb external ID (ttXXXXXXX)
    private Long localMovieId;    // Local database ID (for booking showtimes)
    private String title;
    private String overview;
    private String releaseDate;
    private String posterPath;
    private String posterUrl;
    private String backdropPath;
    private String backdropUrl;
    private Double rating;
    private Integer voteCount;
    private String language;
    private String genre;
    private Boolean isUpcoming;
    private Boolean hasBookableShows;
    private String releaseStatus; // "NOW_SHOWING" | "UPCOMING" | "RELEASED"
    private java.util.List<CastMemberDto> cast;
}
