package com.moviebooking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.controller.AdminController;
import com.moviebooking.dto.request.MovieRequest;
import com.moviebooking.dto.response.MovieResponse;
import com.moviebooking.config.SecurityConfig;
import com.moviebooking.security.JwtAuthFilter;
import com.moviebooking.security.JwtUtil;
import com.moviebooking.security.UserDetailsServiceImpl;
import com.moviebooking.service.BookingService;
import com.moviebooking.service.MovieService;
import com.moviebooking.service.ShowService;
import com.moviebooking.service.TheatreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class AdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MovieService movieService;
    @MockBean
    private TheatreService theatreService;
    @MockBean
    private ShowService showService;
    @MockBean
    private BookingService bookingService;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @WithMockUser(username = "regular_user", roles = {"USER"})
    @DisplayName("USER role accessing /api/admin/movies should be rejected with 403 Forbidden")
    void testAdminEndpoint_ForbiddenForRegularUser() throws Exception {
        MovieRequest request = MovieRequest.builder()
                .title("Test Movie")
                .durationMinutes(120)
                .build();

        mockMvc.perform(post("/api/admin/movies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("ADMIN role accessing /api/admin/movies should be allowed and return 201 Created")
    void testAdminEndpoint_AllowedForAdmin() throws Exception {
        MovieRequest request = MovieRequest.builder()
                .title("Test Movie")
                .durationMinutes(120)
                .releaseDate(LocalDate.now())
                .build();

        MovieResponse response = MovieResponse.builder()
                .id(1L)
                .title("Test Movie")
                .durationMinutes(120)
                .isActive(true)
                .build();

        when(movieService.createMovie(any(MovieRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/movies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Unauthenticated request to /api/admin/movies should be rejected")
    void testAdminEndpoint_UnauthorizedWithoutAuth() throws Exception {
        MovieRequest request = MovieRequest.builder()
                .title("Test Movie")
                .durationMinutes(120)
                .build();

        mockMvc.perform(post("/api/admin/movies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
