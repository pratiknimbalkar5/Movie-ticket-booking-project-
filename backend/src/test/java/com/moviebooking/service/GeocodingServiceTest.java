package com.moviebooking.service;

import com.moviebooking.dto.response.LocationSuggestionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeocodingServiceTest {

    private final GeocodingService geocodingService = new GeocodingService(4, 6);

    @Test
    @DisplayName("Should return empty list for null or very short query")
    void shouldHandleEmptyQuerySafely() {
        List<LocationSuggestionDto> result1 = geocodingService.searchLocations(null, 5);
        List<LocationSuggestionDto> result2 = geocodingService.searchLocations("", 5);
        List<LocationSuggestionDto> result3 = geocodingService.searchLocations("a", 5);

        assertThat(result1).isEmpty();
        assertThat(result2).isEmpty();
        assertThat(result3).isEmpty();
    }

    @Test
    @DisplayName("Should return coordinates and location for reverse geocode")
    void shouldReturnCoordinatesReverseGeocode() {
        LocationSuggestionDto result = geocodingService.reverseGeocode(22.7196, 75.8577);
        assertThat(result).isNotNull();
        assertThat(result.getLatitude()).isBetween(22.5, 23.0);
        assertThat(result.getLongitude()).isBetween(75.5, 76.0);
    }
}
