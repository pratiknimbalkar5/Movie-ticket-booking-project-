package com.moviebooking.service;

import com.moviebooking.dto.response.internationalshowtimes.IsApiResponse;
import com.moviebooking.dto.response.internationalshowtimes.IsCinemaDto;
import com.moviebooking.dto.response.internationalshowtimes.IsCountryDto;
import com.moviebooking.dto.response.internationalshowtimes.IsMovieDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class InternationalShowtimesServiceTest {

    @Test
    @DisplayName("Returns unconfigured status when INTERNATIONAL_SHOWTIMES_API_KEY is missing")
    void testUnconfiguredStatus() {
        InternationalShowtimesService service = new InternationalShowtimesService(RestClient.create(), null);
        assertThat(service.isConfigured()).isFalse();

        Map<String, Object> status = service.verifyAccountAccess();
        assertThat(status.get("configured")).isEqualTo(false);
        assertThat(status.get("authenticated")).isEqualTo(false);
        assertThat(status.get("indiaSupported")).isEqualTo(false);
        assertThat((String) status.get("message")).contains("INTERNATIONAL_SHOWTIMES_API_KEY is not set");
    }

    @Test
    @DisplayName("Verifies V5 authentication and detects India coverage when supported")
    void testVerifyAccountAccessWithIndiaSupport() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.internationalshowtimes.com/v5");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        InternationalShowtimesService service = new InternationalShowtimesService(restClient, "test_api_key_v5");
        assertThat(service.isConfigured()).isTrue();

        // 1. Mock /v5/countries/
        String countriesJson = """
                {
                  "countries": [
                    { "id": "1", "code": "IN", "name": "India", "currency": "INR", "timezone": "Asia/Kolkata" },
                    { "id": "2", "code": "US", "name": "United States", "currency": "USD", "timezone": "America/New_York" }
                  ]
                }
                """;
        mockServer.expect(requestTo("https://api.internationalshowtimes.com/v5/countries/"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Api-Key", "test_api_key_v5"))
                .andRespond(withSuccess(countriesJson, MediaType.APPLICATION_JSON));

        // 2. Mock /v5/cinemas/?countries=IN
        String cinemasJson = """
                {
                  "cinemas": [
                    {
                      "id": "is_cin_1",
                      "name": "PVR ICON Infinity Mall",
                      "chain": "PVR",
                      "location": { "lat": 19.1415, "lon": 72.8315 },
                      "address": { "city": "Mumbai", "country_code": "IN" }
                    }
                  ]
                }
                """;
        mockServer.expect(requestTo("https://api.internationalshowtimes.com/v5/cinemas/?countries=IN"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Api-Key", "test_api_key_v5"))
                .andRespond(withSuccess(cinemasJson, MediaType.APPLICATION_JSON));

        Map<String, Object> status = service.verifyAccountAccess();
        mockServer.verify();

        assertThat(status.get("configured")).isEqualTo(true);
        assertThat(status.get("authenticated")).isEqualTo(true);
        assertThat(status.get("indiaSupported")).isEqualTo(true);
        assertThat(status.get("indianCinemasCount")).isEqualTo(1);
    }

    @Test
    @DisplayName("Handles 401 Unauthorized gracefully from V5 API without throwing unexpected exceptions")
    void testAuthFailure() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.internationalshowtimes.com/v5");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        InternationalShowtimesService service = new InternationalShowtimesService(restClient, "invalid_key");

        mockServer.expect(requestTo("https://api.internationalshowtimes.com/v5/countries/"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withUnauthorizedRequest());

        Map<String, Object> status = service.verifyAccountAccess();
        mockServer.verify();

        assertThat(status.get("authenticated")).isEqualTo(false);
        assertThat(status.get("indiaSupported")).isEqualTo(false);
        assertThat((String) status.get("message")).contains("HTTP 401 Unauthorized");
    }

    @Test
    @DisplayName("Finds movie by TMDB ID via /v5/movies/?tmdb_id={id}")
    void testFindMovieByTmdbId() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.internationalshowtimes.com/v5");
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        InternationalShowtimesService service = new InternationalShowtimesService(restClient, "valid_key");

        String movieJson = """
                {
                  "movies": [
                    {
                      "id": "is_mov_100",
                      "title": "Hope",
                      "tmdb_id": 1058424,
                      "imdb_id": "tt27369017"
                    }
                  ]
                }
                """;
        mockServer.expect(requestTo("https://api.internationalshowtimes.com/v5/movies/?tmdb_id=1058424"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Api-Key", "valid_key"))
                .andRespond(withSuccess(movieJson, MediaType.APPLICATION_JSON));

        var movieOpt = service.findMovieByTmdbId(1058424L);
        mockServer.verify();

        assertThat(movieOpt).isPresent();
        assertThat(movieOpt.get().getTitle()).isEqualTo("Hope");
        assertThat(movieOpt.get().getTmdbId()).isEqualTo(1058424L);
        assertThat(movieOpt.get().getImdbId()).isEqualTo("tt27369017");
    }
}

