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
        private String moviePosterUrl;
        private String movieLanguage;
        private String screenName;
        private String screenFormat;    // "2D", "3D", "IMAX Laser", "Dolby Atmos 7.1", etc.
        private LocalDateTime showTime;
        private BigDecimal basePrice;
        private BigDecimal premiumPrice;
        private BigDecimal vipPrice;
        private int totalSeats;
        private int availableSeats;
        private String dataSource;      // "VERIFIED_LIVE_PROVIDER" | "PARTNER_DEMO_SCHEDULE"
        private String priceNote;
        private String seatAvailabilityNote;
        private String officialBookingUrl;

        public Long getId() {
            return showId;
        }
    }
}
