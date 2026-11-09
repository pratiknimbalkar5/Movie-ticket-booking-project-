package com.moviebooking.dto.response.internationalshowtimes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IsCountryDto {
    private String id;
    private String code;       // e.g. "IN", "US", "GB"
    private String name;       // e.g. "India"
    private String currency;   // e.g. "INR"
    private String timezone;   // e.g. "Asia/Kolkata"
}

