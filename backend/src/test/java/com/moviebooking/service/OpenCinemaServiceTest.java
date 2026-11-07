package com.moviebooking.service;

import com.moviebooking.dto.response.CinemaDiscoveryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenCinemaServiceTest {

    private MockRestServiceServer mockServer;
    private OpenCinemaService openCinemaService;

    private static final String SAMPLE_OPEN_CINEMA_JSON = """
            {
              "cinemas": [
                {
                  "id": "oc-mumbai-01",
                  "name": "Matterden Carnival Cinemas (Deepak Talkies)",
                  "address": "38 NM Joshi Marg, Lower Parel, Mumbai, Maharashtra 400013",
                  "city": "Mumbai",
                  "latitude": 18.9950,
                  "longitude": 72.8300,
                  "purchase_url": "https://matterdencinemas.com/tickets",
                  "rating": 4.6,
                  "review_count": 480
                },
                {
                  "id": "oc-delhi-02",
                  "theater_name": "Habitat Film Club",
                  "theater_address": "India Habitat Centre, Lodhi Road, New Delhi",
                  "city": "Delhi",
                  "lat": 28.5890,
                  "lon": 77.2250,
                  "website": "https://indiahabitat.org/film-club",
                  "rating": 4.8,
                  "review_count": 320
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.opencinemaproject.com/v1");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        openCinemaService = new OpenCinemaService(restClient, "test_open_cinema_secret_key", "https://api.opencinemaproject.com/v1");
    }

    @Test
    @DisplayName("Should detect configuration status correctly")
    void shouldDetectConfigurationStatus() {
        assertThat(openCinemaService.isConfigured()).isTrue();

        openCinemaService.setApiKey(null);
        assertThat(openCinemaService.isConfigured()).isFalse();

        openCinemaService.setApiKey("   ");
        assertThat(openCinemaService.isConfigured()).isFalse();
    }

    @Test
    @DisplayName("Should discover nearby cinemas and map fields according to Open Cinema Platform standard")
    void shouldDiscoverNearbyCinemasSuccessfully() {
        mockServer.expect(requestTo("https://api.opencinemaproject.com/v1/cinemas/nearby?lat=18.9950&lon=72.8300&radius_km=15.0"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test_open_cinema_secret_key"))
                .andExpect(header("X-API-Key", "test_open_cinema_secret_key"))
                .andRespond(withSuccess(SAMPLE_OPEN_CINEMA_JSON, MediaType.APPLICATION_JSON));

        List<CinemaDiscoveryDto> cinemas = openCinemaService.discoverCinemas(18.9950, 72.8300, 15.0);

        assertThat(cinemas).hasSize(2);

        CinemaDiscoveryDto c1 = cinemas.get(0);
        assertThat(c1.getId()).isEqualTo("opencinema-oc-mumbai-01");
        assertThat(c1.getName()).isEqualTo("Matterden Carnival Cinemas (Deepak Talkies)");
        assertThat(c1.getAddress()).contains("Lower Parel");
        assertThat(c1.getSourceProvider()).isEqualTo("OPEN_CINEMA_PLATFORM");
        assertThat(c1.getExternalSourceUri()).isEqualTo("https://matterdencinemas.com/tickets");
        assertThat(c1.isRegistered()).isFalse();
        assertThat(c1.isHasBookableShows()).isFalse();
        assertThat(c1.getCinemaType()).isEqualTo("EXTERNAL_DISCOVERY");
        assertThat(c1.getDistanceKm()).isLessThan(1.0); // essentially at center

        CinemaDiscoveryDto c2 = cinemas.get(1);
        assertThat(c2.getId()).isEqualTo("opencinema-oc-delhi-02");
        assertThat(c2.getName()).isEqualTo("Habitat Film Club");
        assertThat(c2.getAddress()).contains("India Habitat Centre");
        assertThat(c2.getSourceProvider()).isEqualTo("OPEN_CINEMA_PLATFORM");
        assertThat(c2.getExternalSourceUri()).isEqualTo("https://indiahabitat.org/film-club");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should query cinemas by city when specified")
    void shouldDiscoverCinemasByCity() {
        mockServer.expect(requestTo("https://api.opencinemaproject.com/v1/cinemas?city=Mumbai"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test_open_cinema_secret_key"))
                .andRespond(withSuccess(SAMPLE_OPEN_CINEMA_JSON, MediaType.APPLICATION_JSON));

        List<CinemaDiscoveryDto> cinemas = openCinemaService.discoverCinemasByCity("Mumbai", 18.9950, 72.8300);

        assertThat(cinemas).hasSize(2);
        assertThat(cinemas.get(0).getName()).isEqualTo("Matterden Carnival Cinemas (Deepak Talkies)");
        mockServer.verify();
    }

    @Test
    @DisplayName("Should gracefully handle server errors without throwing uncaught exceptions")
    void shouldHandleServerErrorsGracefully() {
        mockServer.expect(requestTo("https://api.opencinemaproject.com/v1/cinemas/nearby?lat=18.9950&lon=72.8300&radius_km=15.0"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        List<CinemaDiscoveryDto> cinemas = openCinemaService.discoverCinemas(18.9950, 72.8300, 15.0);

        assertThat(cinemas).isNotNull();
        assertThat(cinemas).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("Should return empty list immediately if unconfigured")
    void shouldReturnEmptyWhenUnconfigured() {
        openCinemaService.setApiKey(null);
        List<CinemaDiscoveryDto> cinemas = openCinemaService.discoverCinemas(18.9950, 72.8300, 15.0);
        assertThat(cinemas).isEmpty();
    }
}
