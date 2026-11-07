package com.moviebooking.service;

import com.moviebooking.dto.response.CinemaSearchResponse;
import com.moviebooking.entity.Movie;
import com.moviebooking.entity.Screen;
import com.moviebooking.entity.Theatre;
import com.moviebooking.repository.BookingSeatRepository;
import com.moviebooking.repository.ShowRepository;
import com.moviebooking.repository.TheatreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class GooglePlacesServiceTest {

    @Mock
    private TheatreRepository theatreRepository;
    @Mock
    private ShowRepository showRepository;
    @Mock
    private BookingSeatRepository bookingSeatRepository;
    @Mock
    private TheatreService theatreService;
    @Mock
    private OpenCinemaService openCinemaService;

    private MockRestServiceServer mockServer;
    private GooglePlacesService googlePlacesService;

    private static final String SAMPLE_PLACES_JSON = """
            {
              "places": [
                {
                  "id": "ChIJN1t_tDeuEmsRUsoyG83frY4",
                  "displayName": {
                    "text": "PVR INOX Central Mall",
                    "languageCode": "en"
                  },
                  "formattedAddress": "Central Mall, Indore, MP 452001",
                  "location": {
                    "latitude": 22.7196,
                    "longitude": 75.8577
                  },
                  "rating": 4.5,
                  "userRatingCount": 1200,
                  "googleMapsUri": "https://maps.google.com/?cid=1001"
                },
                {
                  "id": "ChIJ-wYZj324wjsR9Y4aB9V1Z-k",
                  "displayName": {
                    "text": "Carnival Cinemas Treasure Island",
                    "languageCode": "en"
                  },
                  "formattedAddress": "Treasure Island, MG Road, Indore",
                  "location": {
                    "latitude": 22.7210,
                    "longitude": 75.8600
                  },
                  "rating": 3.9,
                  "userRatingCount": 850,
                  "googleMapsUri": "https://maps.google.com/?cid=1002"
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://places.googleapis.com/v1");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        googlePlacesService = new GooglePlacesService(theatreRepository, showRepository, bookingSeatRepository, theatreService, openCinemaService, restClient);
    }

    @Test
    @DisplayName("Should report not configured when API key is missing and fallback safely to local database")
    void shouldFallbackWhenNotConfigured() {
        googlePlacesService.setApiKey(null);
        assertThat(googlePlacesService.isConfigured()).isFalse();

        Theatre localTheatre = Theatre.builder()
                .id(1L)
                .name("PVR INOX Indore")
                .city("Indore")
                .address("RNT Marg, Indore")
                .latitude(22.7196)
                .longitude(75.8577)
                .isActive(true)
                .build();

        when(theatreRepository.findByIsActiveTrue()).thenReturn(List.of(localTheatre));
        when(showRepository.findNearbyShows(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        CinemaSearchResponse response = googlePlacesService.discoverNearbyCinemas(
                "Indore", 22.7196, 75.8577, 15.0, null, LocalDate.now()
        );

        assertThat(response.isProviderConfigured()).isFalse();
        assertThat(response.getProvider()).isEqualTo("LOCAL_DATABASE");
        assertThat(response.getCinemas()).isNotEmpty();
        assertThat(response.getCinemas().get(0).getName()).isEqualTo("PVR INOX Indore");
        assertThat(response.getCinemas().get(0).isRegistered()).isTrue();
    }

    @Test
    @DisplayName("Should query Google Places API and distinguish external from registered cinemas")
    void shouldDiscoverCinemasViaGooglePlaces() {
        googlePlacesService.setApiKey("test_google_places_api_key_123");
        assertThat(googlePlacesService.isConfigured()).isTrue();

        mockServer.expect(requestTo("https://places.googleapis.com/v1/places:searchNearby"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", "test_google_places_api_key_123"))
                .andRespond(withSuccess(SAMPLE_PLACES_JSON, MediaType.APPLICATION_JSON));

        Theatre registeredTheatre = Theatre.builder()
                .id(10L)
                .name("PVR INOX Central Mall")
                .city("Indore")
                .address("Central Mall, Indore")
                .latitude(22.7196)
                .longitude(75.8577)
                .isActive(true)
                .build();

        when(theatreRepository.findByIsActiveTrue()).thenReturn(List.of(registeredTheatre));
        when(showRepository.findNearbyShows(any(), any(), any(), any())).thenReturn(Collections.emptyList());

        CinemaSearchResponse response = googlePlacesService.discoverNearbyCinemas(
                "Indore", 22.7196, 75.8577, 15.0, null, LocalDate.now()
        );

        assertThat(response.isProviderConfigured()).isTrue();
        assertThat(response.getProvider()).isEqualTo("GOOGLE_PLACES");
        assertThat(response.getCinemas()).hasSize(2);

        // First cinema matches local database theatre
        var cinema1 = response.getCinemas().get(0);
        assertThat(cinema1.getName()).contains("PVR INOX Central Mall");
        assertThat(cinema1.isRegistered()).isTrue();
        assertThat(cinema1.getTheatreId()).isEqualTo(10L);

        // Second cinema is externally discovered
        var cinema2 = response.getCinemas().get(1);
        assertThat(cinema2.getName()).contains("Carnival Cinemas");
        assertThat(cinema2.isRegistered()).isFalse();
        assertThat(cinema2.getTheatreId()).isNull();
        assertThat(cinema2.getCinemaType()).isEqualTo("EXTERNAL_DISCOVERY");
        assertThat(cinema2.getGoogleMapsUri()).contains("maps.google.com");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should resolve verified coordinates for Indian cities")
    void shouldResolveCityCoordinates() {
        var indoreCoords = googlePlacesService.resolveCityCoordinates("Indore");
        assertThat(indoreCoords.latitude()).isEqualTo(22.7196);
        assertThat(indoreCoords.longitude()).isEqualTo(75.8577);

        var bhopalCoords = googlePlacesService.resolveCityCoordinates("Bhopal");
        assertThat(bhopalCoords.latitude()).isEqualTo(23.2599);
        assertThat(bhopalCoords.longitude()).isEqualTo(77.4126);

        var mumbaiCoords = googlePlacesService.resolveCityCoordinates("Mumbai");
        assertThat(mumbaiCoords.latitude()).isEqualTo(19.0760);
        assertThat(mumbaiCoords.longitude()).isEqualTo(72.8777);
    }

    @Test
    @DisplayName("Should integrate Open Cinema Platform provider and mark cinemas as external discovery")
    void shouldDiscoverCinemasViaOpenCinemaPlatform() {
        when(openCinemaService.isConfigured()).thenReturn(true);

        com.moviebooking.dto.response.CinemaDiscoveryDto openCinema = com.moviebooking.dto.response.CinemaDiscoveryDto.builder()
                .id("opencinema-indore-1")
                .name("Matterden Independent Cinema")
                .address("AB Road, Indore")
                .latitude(22.7200)
                .longitude(75.8580)
                .distanceKm(0.3)
                .sourceProvider("OPEN_CINEMA_PLATFORM")
                .externalSourceUri("https://opencinemaproject.com/c/indore-1")
                .isRegistered(false)
                .cinemaType("EXTERNAL_DISCOVERY")
                .hasBookableShows(false)
                .build();

        when(openCinemaService.discoverCinemas(22.7196, 75.8577, 15.0)).thenReturn(List.of(openCinema));
        when(theatreRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());

        CinemaSearchResponse response = googlePlacesService.discoverNearbyCinemas(
                "Indore", 22.7196, 75.8577, 15.0, null, LocalDate.now()
        );

        assertThat(response.isOpenCinemaConfigured()).isTrue();
        assertThat(response.getProvider()).contains("OPEN_CINEMA_PLATFORM");
        assertThat(response.getCinemas()).hasSize(1);
        assertThat(response.getCinemas().get(0).getName()).isEqualTo("Matterden Independent Cinema");
        assertThat(response.getCinemas().get(0).getSourceProvider()).isEqualTo("OPEN_CINEMA_PLATFORM");
        assertThat(response.getCinemas().get(0).isHasBookableShows()).isFalse();
    }
}
