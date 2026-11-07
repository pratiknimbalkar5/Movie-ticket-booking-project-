package com.moviebooking.controller;

import com.moviebooking.dto.response.TmdbMovieDto;
import com.moviebooking.dto.response.TmdbPageResponse;
import com.moviebooking.exception.TmdbAuthException;
import com.moviebooking.service.MovieService;
import com.moviebooking.service.TmdbMovieService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TmdbMovieService tmdbMovieService;

    @MockitoBean
    private MovieService movieService;

    private TmdbPageResponse<TmdbMovieDto> sampleTmdbPage() {
        TmdbMovieDto movie = TmdbMovieDto.builder()
                .id(823464L)
                .title("Godzilla x Kong")
                .overview("The epic battle continues.")
                .posterUrl("https://image.tmdb.org/t/p/w500/sample.jpg")
                .rating(7.2)
                .language("English")
                .releaseDate("2024-03-27")
                .build();

        return TmdbPageResponse.<TmdbMovieDto>builder()
                .page(1)
                .totalPages(10)
                .totalResults(200)
                .results(List.of(movie))
                .build();
    }

    @Test
    @DisplayName("GET /api/movies/now-playing returns 200 with TMDB data")
    void shouldReturnNowPlayingMovies() throws Exception {
        when(tmdbMovieService.getNowPlaying(anyString(), anyInt())).thenReturn(sampleTmdbPage());

        mockMvc.perform(get("/api/movies/now-playing")
                        .param("region", "IN")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results[0].title").value("Godzilla x Kong"))
                .andExpect(jsonPath("$.data.results[0].rating").value(7.2));
    }

    @Test
    @DisplayName("GET /api/movies/upcoming returns 200 with upcoming releases")
    void shouldReturnUpcomingMovies() throws Exception {
        when(tmdbMovieService.getUpcoming(anyString(), anyInt())).thenReturn(sampleTmdbPage());

        mockMvc.perform(get("/api/movies/upcoming")
                        .param("region", "IN")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results[0].id").value(823464));
    }

    @Test
    @DisplayName("GET /api/movies/popular returns 200 with popular movies")
    void shouldReturnPopularMovies() throws Exception {
        when(tmdbMovieService.getPopular(anyInt())).thenReturn(sampleTmdbPage());

        mockMvc.perform(get("/api/movies/popular")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results[0].title").value("Godzilla x Kong"));
    }

    @Test
    @DisplayName("GET /api/movies/trending returns 200 with trending movies")
    void shouldReturnTrendingMovies() throws Exception {
        when(tmdbMovieService.getTrending(anyString(), anyInt())).thenReturn(sampleTmdbPage());

        mockMvc.perform(get("/api/movies/trending")
                        .param("window", "day")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results[0].title").value("Godzilla x Kong"));
    }

    @Test
    @DisplayName("GET /api/movies/search with query returns TMDB search results")
    void shouldReturnSearchResults() throws Exception {
        when(tmdbMovieService.searchMovies("Godzilla", 1)).thenReturn(sampleTmdbPage());

        mockMvc.perform(get("/api/movies/search")
                        .param("query", "Godzilla")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results[0].title").value("Godzilla x Kong"));
    }

    @Test
    @DisplayName("When TMDB credentials are not configured, endpoint returns 503 SERVICE_UNAVAILABLE with clear error")
    void shouldReturn503WhenTmdbNotConfigured() throws Exception {
        when(tmdbMovieService.getNowPlaying(anyString(), anyInt()))
                .thenThrow(new TmdbAuthException("TMDB credentials not configured. Please set TMDB_ACCESS_TOKEN."));

        mockMvc.perform(get("/api/movies/now-playing"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("TMDB credentials not configured. Please set TMDB_ACCESS_TOKEN."));
    }
}
