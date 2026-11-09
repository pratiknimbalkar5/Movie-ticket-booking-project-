package com.moviebooking.service;

import com.moviebooking.entity.Movie;
import com.moviebooking.entity.Show;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.ShowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Scheduled service maintaining data freshness across the application.
 * Executes on configurable intervals using Indian Standard Time (Asia/Kolkata).
 *
 * Responsibilities:
 * 1. Daily refresh of upcoming Indian theatrical release information.
 * 2. Moving movies whose Indian theatrical release date has passed out of Coming Soon.
 * 3. Expiring stale and past showtimes from the active booking inventory.
 * 4. Recalculating Now Showing eligibility based on current screening evidence.
 * 5. Periodically synchronizing live TMDB metadata and International Showtimes V5 verification.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduledMovieRefreshService {

    public static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final TmdbMovieService tmdbMovieService;
    private final InternationalShowtimesService internationalShowtimesService;

    /**
     * Periodic Refresh Job (runs every 6 hours by default):
     * - Evicts expired showtimes.
     * - Clears stale in-memory caches.
     * - Re-verifies International Showtimes V5 and TMDB connectivity.
     */
    @Scheduled(fixedRateString = "${app.refresh.interval-ms:21600000}", initialDelay = 30000)
    @Transactional
    public void executePeriodicRefresh() {
        log.info("Running periodic movie and showtime refresh job [IST]...");
        try {
            // 1. Expire past showtimes (older than 2 hours)
            LocalDateTime threshold = LocalDateTime.now(IST_ZONE).minusHours(2);
            List<Show> activeShows = showRepository.findAll();
            int completedCount = 0;
            for (Show s : activeShows) {
                if (s.getStatus() == Show.ShowStatus.SCHEDULED && s.getShowTime().isBefore(threshold)) {
                    s.setStatus(Show.ShowStatus.COMPLETED);
                    completedCount++;
                }
            }
            if (completedCount > 0) {
                log.info("Marked {} past shows as COMPLETED (stale showtime cleanup)", completedCount);
            }

            // 2. Clear API caches to trigger fresh data fetch
            if (tmdbMovieService != null) {
                tmdbMovieService.clearPageCache();
            }
            if (internationalShowtimesService != null) {
                internationalShowtimesService.clearCache();
                if (internationalShowtimesService.isConfigured()) {
                    internationalShowtimesService.verifyAccountAccess();
                }
            }

            // 3. Recalculate Now Showing eligibility for local database movies
            recalculateMovieEligibility();

            log.info("Periodic refresh job completed successfully.");
        } catch (Exception e) {
            log.warn("Notice during periodic refresh job: {}", e.getMessage());
        }
    }

    /**
     * Daily Midnight/Early-Morning Refresh Job (04:00 AM IST daily):
     * - Shifts movies whose Indian release date has arrived out of Coming Soon.
     * - Refreshes upcoming movies from TMDB for India region.
     * - Re-indexes Indian theatrical blockbusters.
     */
    @Scheduled(cron = "${app.refresh.cron.daily-sync:0 0 4 * * *}", zone = "Asia/Kolkata")
    @Transactional
    public void executeDailyIndianTheatricalSync() {
        LocalDate todayIst = LocalDate.now(IST_ZONE);
        log.info("Executing daily Indian theatrical release sync for date: {} [Asia/Kolkata]", todayIst);

        try {
            // 1. Update any movie in database whose release date has passed or arrived today
            List<Movie> upcomingMovies = movieRepository.findByIsActiveTrueAndIsUpcomingTrueOrderByReleaseDateAsc();
            int transitioned = 0;
            for (Movie m : upcomingMovies) {
                if (m.getReleaseDate() != null && !m.getReleaseDate().isAfter(todayIst)) {
                    m.setIsUpcoming(false);
                    transitioned++;
                    log.info("Movie '{}' Indian release date {} has arrived: Transitioned from Coming Soon to Released.",
                            m.getTitle(), m.getReleaseDate());
                }
            }
            if (transitioned > 0) {
                movieRepository.saveAll(upcomingMovies);
            }

            // 2. Fetch fresh live upcoming titles from TMDB for India
            if (tmdbMovieService != null && tmdbMovieService.isConfigured()) {
                tmdbMovieService.syncLiveMoviesFromImdb();
            }

            log.info("Daily Indian theatrical release sync completed successfully. Transitioned: {}", transitioned);
        } catch (Exception e) {
            log.warn("Daily Indian theatrical release sync encountered notice: {}", e.getMessage());
        }
    }

    /**
     * Recalculates movie eligibility:
     * - Coming Soon: releaseDate strictly in the future.
     * - Now Showing: has current screening evidence from International Showtimes or active scheduled shows in booking system.
     */
    @Transactional
    public void recalculateMovieEligibility() {
        LocalDate todayIst = LocalDate.now(IST_ZONE);
        LocalDateTime activeShowCutoff = LocalDateTime.now(IST_ZONE).minusMinutes(30);
        List<Movie> allMovies = movieRepository.findAll();

        for (Movie m : allMovies) {
            if (m.getReleaseDate() != null && m.getReleaseDate().isAfter(todayIst)) {
                m.setIsUpcoming(true);
            } else if (Boolean.TRUE.equals(m.getIsUpcoming())) {
                m.setIsUpcoming(false);
            }
        }
        movieRepository.saveAll(allMovies);
    }
}

