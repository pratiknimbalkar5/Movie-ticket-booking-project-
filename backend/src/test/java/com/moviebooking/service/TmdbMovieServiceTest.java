package com.moviebooking.service;

import com.moviebooking.dto.response.TmdbMovieDto;
import com.moviebooking.dto.response.TmdbPageResponse;
import com.moviebooking.exception.TmdbApiException;
import com.moviebooking.exception.TmdbAuthException;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.ScreenRepository;
import com.moviebooking.repository.ShowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class TmdbMovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private ShowRepository showRepository;

    @Mock
    private ScreenRepository screenRepository;

    private MockRestServiceServer mockServer;
    private TmdbMovieService tmdbMovieService;

    private static final String SAMPLE_TMDB_PAGE_JSON = """
            {
              "page": 1,
              "total_pages": 42,
              "total_results": 834,
              "results": [
                {
                  "id": 939243,
                  "title": "Sonic the Hedgehog 3",
                  "overview": "Sonic, Knuckles, and Tails reunite against a powerful new adversary, Shadow.",
                  "release_date": "2026-10-25",
                  "vote_average": 7.8,
                  "original_language": "en",
                  "poster_path": "/d8Ryb8AunYAuycPtDp66kI1fB9A.jpg",
                  "genre_ids": [28, 12, 16]
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.themoviedb.org/3");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        tmdbMovieService = new TmdbMovieService(movieRepository, showRepository, screenRepository, restClient);
    }

    @Test
    @DisplayName("Should throw TmdbAuthException when TMDB_ACCESS_TOKEN is not configured")
    void shouldThrowAuthExceptionWhenCredentialsMissing() {
        tmdbMovieService.setAccessToken(null);
        tmdbMovieService.setApiKey(null);

        assertThat(tmdbMovieService.isConfigured()).isFalse();

        assertThatThrownBy(() -> tmdbMovieService.getNowPlaying("IN", 1))
                .isInstanceOf(TmdbAuthException.class)
                .hasMessageContaining("TMDB credentials not configured");
    }

    @Test
    @DisplayName("Should successfully map TMDB now playing response to DTOs with w500 poster path")
    void shouldMapNowPlayingResponseCorrectly() {
        tmdbMovieService.setAccessToken("test_mock_token_12345");
        when(movieRepository.findByTmdbId(anyLong())).thenReturn(Optional.empty());

        mockServer.expect(requestTo("https://api.themoviedb.org/3/movie/now_playing?region=IN&page=1&language=en-US"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test_mock_token_12345"))
                .andRespond(withSuccess(SAMPLE_TMDB_PAGE_JSON, MediaType.APPLICATION_JSON));

        TmdbPageResponse<TmdbMovieDto> response = tmdbMovieService.getNowPlaying("IN", 1);

        assertThat(response).isNotNull();
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(42);
        assertThat(response.getTotalResults()).isEqualTo(834);
        assertThat(response.getResults()).hasSize(1);

        TmdbMovieDto movie = response.getResults().get(0);
        assertThat(movie.getId()).isEqualTo(939243L);
        assertThat(movie.getTitle()).isEqualTo("Sonic the Hedgehog 3");
        assertThat(movie.getOverview()).contains("Sonic, Knuckles, and Tails reunite");
        assertThat(movie.getPosterUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/d8Ryb8AunYAuycPtDp66kI1fB9A.jpg");
        assertThat(movie.getRating()).isEqualTo(7.8);
        assertThat(movie.getLanguage()).isEqualTo("English");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should use cache on second call and not repeat external HTTP request")
    void shouldUseCacheOnSubsequentCalls() {
        tmdbMovieService.setAccessToken("test_mock_token_12345");
        when(movieRepository.findByTmdbId(anyLong())).thenReturn(Optional.empty());

        // Expect exactly 1 HTTP call
        mockServer.expect(requestTo("https://api.themoviedb.org/3/movie/now_playing?region=IN&page=1&language=en-US"))
                .andRespond(withSuccess(SAMPLE_TMDB_PAGE_JSON, MediaType.APPLICATION_JSON));

        TmdbPageResponse<TmdbMovieDto> first = tmdbMovieService.getNowPlaying("IN", 1);
        assertThat(first.isCached()).isFalse();

        // Second call should hit in-memory cache
        TmdbPageResponse<TmdbMovieDto> second = tmdbMovieService.getNowPlaying("IN", 1);
        assertThat(second.isCached()).isTrue();
        assertThat(second.getResults().get(0).getTitle()).isEqualTo("Sonic the Hedgehog 3");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw TmdbAuthException when TMDB returns HTTP 401 Unauthorized")
    void shouldHandleTmdbHttp401Unauthorized() {
        tmdbMovieService.setAccessToken("invalid_expired_token");

        mockServer.expect(requestTo("https://api.themoviedb.org/3/movie/now_playing?region=IN&page=1&language=en-US"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> tmdbMovieService.getNowPlaying("IN", 1))
                .isInstanceOf(TmdbAuthException.class)
                .hasMessageContaining("Invalid TMDB credentials (HTTP 401)");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should throw TmdbApiException with status 429 when TMDB returns rate limit exceeded")
    void shouldHandleTmdbRateLimitHttp429() {
        tmdbMovieService.setAccessToken("valid_token");

        mockServer.expect(requestTo("https://api.themoviedb.org/3/movie/popular?page=1&language=en-US&region=IN"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> tmdbMovieService.getPopular(1))
                .isInstanceOf(TmdbApiException.class)
                .hasMessageContaining("rate limit exceeded (HTTP 429)");

        mockServer.verify();
    }

    @Test
    @DisplayName("Should search movies with URL encoding and return matching results")
    void shouldSearchMoviesSuccessfully() {
        tmdbMovieService.setAccessToken("valid_token");
        when(movieRepository.findByTmdbId(anyLong())).thenReturn(Optional.empty());

        mockServer.expect(requestTo("https://api.themoviedb.org/3/search/movie?query=Avatar+Fire&page=1&language=en-US&include_adult=false"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(SAMPLE_TMDB_PAGE_JSON, MediaType.APPLICATION_JSON));

        TmdbPageResponse<TmdbMovieDto> searchRes = tmdbMovieService.searchMovies("Avatar Fire", 1);

        assertThat(searchRes).isNotNull();
        assertThat(searchRes.getResults()).isNotEmpty();
        mockServer.verify();
    }
}
