package com.moviebooking.dto.response.internationalshowtimes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IsCinemaDto {

    private String id;
    private String slug;
    private String name;
    private String chain;
    private String telephone;
    private String website;

    @JsonProperty("booking_type")
    private String bookingType;

    private Location location;
    private Address address;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private Double lat;
        private Double lon;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Address {
        private String display_text;
        private String street;
        private String house;
        private String zipcode;
        private String city;
        private String state;
        private String country;
        private String country_code;
    }
}

