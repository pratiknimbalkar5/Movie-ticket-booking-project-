package com.moviebooking.dto.response;

import com.moviebooking.entity.Show;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowResponse {

    private Long id;
    private Long movieId;
    private String movieTitle;
    private String moviePosterUrl;
    private String movieLanguage;
    private Integer movieDurationMinutes;
    private Long screenId;
    private String screenName;
    private Long theatreId;
    private String theatreName;
    private String theatreCity;
    private LocalDateTime showTime;
    private BigDecimal basePrice;
    private BigDecimal premiumPrice;
    private BigDecimal vipPrice;
    private Show.ShowStatus status;
}
