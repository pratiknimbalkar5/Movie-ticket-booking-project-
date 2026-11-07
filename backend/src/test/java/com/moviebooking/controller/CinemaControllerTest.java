package com.moviebooking.controller;

import com.moviebooking.dto.response.CinemaDiscoveryDto;
import com.moviebooking.dto.response.CinemaSearchResponse;
import com.moviebooking.service.GooglePlacesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CinemaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GooglePlacesService googlePlacesService;

    @MockBean
    private com.moviebooking.service.OpenCinemaService openCinemaService;

    @Test
    @DisplayName("Should return discovered nearby cinemas with provider status and booking distinctions")
    void shouldReturnNearbyCinemas() throws Exception {
        CinemaSearchResponse mockResponse = CinemaSearchResponse.builder()
                .searchCity("Indore")
                .searchLatitude(22.7196)
                .searchLongitude(75.8577)
                .searchRadiusKm(15.0)
                .provider("GOOGLE_PLACES")
                .providerConfigured(true)
                .providerStatusMessage("Google Places API active")
                .cinemas(List.of(
                        CinemaDiscoveryDto.builder()
                                .id("place-123")
                                .name("PVR INOX Indore Central")
                                .address("RNT Marg, Indore")
                                .city("Indore")
                                .latitude(22.7190)
                                .longitude(75.8570)
                                .distanceKm(1.2)
                                .googleMapsUri("https://maps.google.com/?cid=123")
                                .rating(4.4)
                                .isRegistered(true)
                                .theatreId(10L)
                                .hasBookableShows(true)
                                .cinemaType("BOOKABLE_PARTNER")
                                .build(),
                        CinemaDiscoveryDto.builder()
                                .id("place-456")
                                .name("Velocity III Multiplex")
                                .address("Ring Road, Indore")
                                .city("Indore")
                                .latitude(22.7300)
                                .longitude(75.8700)
                                .distanceKm(3.5)
                                .googleMapsUri("https://maps.google.com/?cid=456")
                                .rating(4.0)
                                .isRegistered(false)
                                .theatreId(null)
                                .hasBookableShows(false)
                                .cinemaType("EXTERNAL_DISCOVERY")
                                .build()
                ))
                .build();

        when(googlePlacesService.discoverNearbyCinemas(eq("Indore"), any(), any(), any(), any(), any()))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/api/cinemas/nearby?city=Indore")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.searchCity").value("Indore"))
                .andExpect(jsonPath("$.data.cinemas[0].name").value("PVR INOX Indore Central"))
                .andExpect(jsonPath("$.data.cinemas[0].cinemaType").value("BOOKABLE_PARTNER"))
                .andExpect(jsonPath("$.data.cinemas[0].hasBookableShows").value(true))
                .andExpect(jsonPath("$.data.cinemas[1].name").value("Velocity III Multiplex"))
                .andExpect(jsonPath("$.data.cinemas[1].cinemaType").value("EXTERNAL_DISCOVERY"))
                .andExpect(jsonPath("$.data.cinemas[1].hasBookableShows").value(false));
    }

    @Test
    @DisplayName("Should resolve verified coordinates for city")
    void shouldResolveCityCoordinates() throws Exception {
        when(googlePlacesService.resolveCityCoordinates("Bhopal"))
                .thenReturn(new GooglePlacesService.Coordinates(23.2599, 77.4126, "Bhopal"));

        mockMvc.perform(get("/api/cinemas/coordinates?city=Bhopal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.city").value("Bhopal"))
                .andExpect(jsonPath("$.data.latitude").value(23.2599))
                .andExpect(jsonPath("$.data.longitude").value(77.4126));
    }

    @Test
    @DisplayName("Should report provider configuration status accurately")
    void shouldReportProviderStatus() throws Exception {
        when(googlePlacesService.isConfigured()).thenReturn(false);
        when(openCinemaService.isConfigured()).thenReturn(false);

        mockMvc.perform(get("/api/cinemas/provider-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.googlePlacesConfigured").value(false))
                .andExpect(jsonPath("$.data.openCinemaConfigured").value(false))
                .andExpect(jsonPath("$.data.provider").value("LOCAL_DATABASE"));
    }
}
