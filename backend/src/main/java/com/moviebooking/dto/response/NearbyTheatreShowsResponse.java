package com.moviebooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyTheatreShowsResponse {
    private Long theatreId;
    private String theatreName;
    private String city;
    private String address;
    private Double distanceKm;
    private Double latitude;
    private Double longitude;
    private List<NearbyShowItem> shows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NearbyShowItem {
        private Long showId;
        private Long movieId;
        private String movieTitle;
        private String movieLanguage;
        private String screenName;
        private LocalDateTime showTime;
        private BigDecimal basePrice;
        private BigDecimal premiumPrice;
        private BigDecimal vipPrice;
        private int totalSeats;
        private int availableSeats;

        public Long getId() {
            return showId;
        }
    }
}
