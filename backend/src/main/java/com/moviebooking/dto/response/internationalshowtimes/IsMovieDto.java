package com.moviebooking.dto.response.internationalshowtimes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IsMovieDto {

    private String id;
    private String slug;
    private String title;

    @JsonProperty("original_title")
    private String originalTitle;

    private String synopsis;
    private Integer runtime;

    @JsonProperty("release_date")
    private String releaseDate;

    private String certification;

    @JsonProperty("imdb_id")
    private String imdbId;

    @JsonProperty("tmdb_id")
    private Long tmdbId;

    @JsonProperty("poster_image_thumbnail")
    private String posterImageThumbnail;

    @JsonProperty("backdrop_image")
    private String backdropImage;

    private List<Genre> genres;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Genre {
        private String id;
        private String name;
    }
}

