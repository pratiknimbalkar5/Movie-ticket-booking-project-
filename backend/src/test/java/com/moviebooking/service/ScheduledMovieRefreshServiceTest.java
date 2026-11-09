package com.moviebooking.service;

import com.moviebooking.entity.Movie;
import com.moviebooking.entity.Show;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.ShowRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledMovieRefreshServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private ShowRepository showRepository;

    @Mock
    private TmdbMovieService tmdbMovieService;

    @Mock
    private InternationalShowtimesService internationalShowtimesService;

    @InjectMocks
    private ScheduledMovieRefreshService refreshService;

    @Test
    @DisplayName("Transitions upcoming movies to released once their release date arrives")
    void testTransitionUpcomingToReleased() {
        LocalDate yesterday = LocalDate.now(ScheduledMovieRefreshService.IST_ZONE).minusDays(1);
        Movie m1 = Movie.builder()
                .id(1L)
                .title("Released Yesterday")
                .releaseDate(yesterday)
                .isUpcoming(true)
                .build();

        LocalDate future = LocalDate.now(ScheduledMovieRefreshService.IST_ZONE).plusDays(10);
        Movie m2 = Movie.builder()
                .id(2L)
                .title("Still Future")
                .releaseDate(future)
                .isUpcoming(true)
                .build();

        when(movieRepository.findByIsActiveTrueAndIsUpcomingTrueOrderByReleaseDateAsc())
                .thenReturn(List.of(m1, m2));

        refreshService.executeDailyIndianTheatricalSync();

        assertThat(m1.getIsUpcoming()).isFalse();
        assertThat(m2.getIsUpcoming()).isTrue();
        verify(movieRepository, atLeastOnce()).saveAll(any());
    }

    @Test
    @DisplayName("Marks stale showtimes older than 2 hours as COMPLETED")
    void testExpirePastShowtimes() {
        Show pastShow = new Show();
        pastShow.setStatus(Show.ShowStatus.SCHEDULED);
        pastShow.setShowTime(LocalDateTime.now(ScheduledMovieRefreshService.IST_ZONE).minusHours(3));

        Show futureShow = new Show();
        futureShow.setStatus(Show.ShowStatus.SCHEDULED);
        futureShow.setShowTime(LocalDateTime.now(ScheduledMovieRefreshService.IST_ZONE).plusHours(2));

        when(showRepository.findAll()).thenReturn(List.of(pastShow, futureShow));

        refreshService.executePeriodicRefresh();

        assertThat(pastShow.getStatus()).isEqualTo(Show.ShowStatus.COMPLETED);
        assertThat(futureShow.getStatus()).isEqualTo(Show.ShowStatus.SCHEDULED);
    }
}

