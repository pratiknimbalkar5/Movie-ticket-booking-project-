package com.moviebooking.dto.response.internationalshowtimes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IsShowtimeDto {

    private String id;

    @JsonProperty("cinema_id")
    private String cinemaId;

    @JsonProperty("movie_id")
    private String movieId;

    @JsonProperty("start_at")
    private String startAt;   // ISO timestamp, e.g. "2026-11-09T18:30:00+05:30"

    @JsonProperty("is_3d")
    private Boolean is3d;

    @JsonProperty("is_imax")
    private Boolean isImax;

    @JsonProperty("booking_link")
    private String bookingLink;

    private String auditorium;
    private String subtitle_language;
    private String spoken_language;
}

