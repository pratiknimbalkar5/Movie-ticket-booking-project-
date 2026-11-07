package com.moviebooking.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.dto.request.BookingRequest;
import com.moviebooking.dto.request.LoginRequest;
import com.moviebooking.entity.Show;
import com.moviebooking.repository.ShowRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BookingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShowRepository showRepository;

    @Test
    @DisplayName("End-to-end: Login, Fetch Seats, Book Seats, View Bookings, Cancel Booking")
    void testEndToEndBookingAndCancellation() throws Exception {
        // 1. Login as john_doe
        LoginRequest loginRequest = LoginRequest.builder()
                .username("john_doe")
                .password("User@123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginJson.path("data").path("token").asText();
        assertNotNull(token);
        assertFalse(token.isBlank());

        // 2. Find an upcoming scheduled show
        List<Show> shows = showRepository.findAll();
        assertFalse(shows.isEmpty(), "Shows should have been seeded");
        Show testShow = shows.stream()
                .filter(s -> s.getStatus() == Show.ShowStatus.SCHEDULED && s.getShowTime().isAfter(java.time.LocalDateTime.now().plusHours(2)))
                .findFirst()
                .orElse(shows.get(0));

        // 3. Get available seats for this show
        MvcResult seatsResult = mockMvc.perform(get("/api/shows/" + testShow.getId() + "/seats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode seatsJson = objectMapper.readTree(seatsResult.getResponse().getContentAsString());
        JsonNode seatsArray = seatsJson.path("data");
        assertTrue(seatsArray.isArray() && seatsArray.size() > 0, "Seats should exist for show");

        List<Long> selectedSeatIds = new ArrayList<>();
        for (JsonNode seat : seatsArray) {
            if (seat.path("isAvailable").asBoolean()) {
                selectedSeatIds.add(seat.path("id").asLong());
                if (selectedSeatIds.size() == 2) break;
            }
        }
        assertEquals(2, selectedSeatIds.size(), "Should find 2 available seats");

        // 4. Book seats
        BookingRequest bookingRequest = BookingRequest.builder()
                .showId(testShow.getId())
                .seatIds(selectedSeatIds)
                .build();

        MvcResult bookingResult = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode bookingJson = objectMapper.readTree(bookingResult.getResponse().getContentAsString());
        long bookingId = bookingJson.path("data").path("id").asLong();
        assertTrue(bookingId > 0);
        assertEquals("CONFIRMED", bookingJson.path("data").path("status").asText());

        // 5. Attempt to double-book the same seats (MUST be rejected with 409 Conflict)
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isConflict());

        // 6. Verify seats are marked unavailable for this show
        MvcResult seatsAfterBookResult = mockMvc.perform(get("/api/shows/" + testShow.getId() + "/seats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode seatsAfterBookArray = objectMapper.readTree(seatsAfterBookResult.getResponse().getContentAsString()).path("data");
        for (JsonNode seat : seatsAfterBookArray) {
            long seatId = seat.path("id").asLong();
            if (selectedSeatIds.contains(seatId)) {
                assertFalse(seat.path("isAvailable").asBoolean(), "Booked seat must be unavailable");
            }
        }

        // 7. Get My Bookings - verified in user's history
        MvcResult myBookingsResult = mockMvc.perform(get("/api/bookings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode myBookingsJson = objectMapper.readTree(myBookingsResult.getResponse().getContentAsString());
        assertTrue(myBookingsJson.path("data").path("content").size() > 0);

        // 8. Cancel Booking
        MvcResult cancelResult = mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode cancelJson = objectMapper.readTree(cancelResult.getResponse().getContentAsString());
        assertEquals("CANCELLED", cancelJson.path("data").path("status").asText());

        // 9. Attempt duplicate cancellation (MUST be rejected with 400 Bad Request)
        mockMvc.perform(post("/api/bookings/" + bookingId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        // 10. Verify seats are immediately RELEASED and bookable again after cancellation
        MvcResult seatsAfterCancelResult = mockMvc.perform(get("/api/shows/" + testShow.getId() + "/seats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode seatsAfterCancelArray = objectMapper.readTree(seatsAfterCancelResult.getResponse().getContentAsString()).path("data");
        for (JsonNode seat : seatsAfterCancelArray) {
            long seatId = seat.path("id").asLong();
            if (selectedSeatIds.contains(seatId)) {
                assertTrue(seat.path("isAvailable").asBoolean(), "Cancelled seat must be available again");
            }
        }

        // 11. Re-book the same released seats successfully
        MvcResult rebookResult = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode rebookJson = objectMapper.readTree(rebookResult.getResponse().getContentAsString());
        long rebookId = rebookJson.path("data").path("id").asLong();
        assertTrue(rebookId > bookingId, "New booking reference created");
        assertEquals("CONFIRMED", rebookJson.path("data").path("status").asText());
    }
}
