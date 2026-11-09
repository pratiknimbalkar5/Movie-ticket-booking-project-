package com.moviebooking.dto.response.internationalshowtimes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Generic response wrapper for International Showtimes API V5.
 * Endpoints return either { "cinemas": [...] }, { "movies": [...] }, { "showtimes": [...] },
 * or { "countries": [...] } along with optional "meta_info".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IsApiResponse {

    @JsonProperty("meta_info")
    private Map<String, Object> metaInfo;

    private List<IsCinemaDto> cinemas;

    private List<IsMovieDto> movies;

    private List<IsShowtimeDto> showtimes;

    private List<IsCountryDto> countries;
}
