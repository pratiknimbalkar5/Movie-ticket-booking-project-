package com.moviebooking.repository;

import com.moviebooking.entity.Show;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findByMovieIdAndStatus(Long movieId, Show.ShowStatus status);

    List<Show> findByScreenIdAndStatus(Long screenId, Show.ShowStatus status);

    @Query("SELECT s FROM Show s " +
           "JOIN FETCH s.movie m " +
           "JOIN FETCH s.screen sc " +
           "JOIN FETCH sc.theatre t " +
           "WHERE s.status = 'SCHEDULED' " +
           "AND s.showTime >= :startTime " +
           "AND (:movieId IS NULL OR m.id = :movieId OR m.tmdbId = :movieId) " +
           "AND (:city IS NULL OR LOWER(t.city) LIKE LOWER(CONCAT('%', :city, '%')) OR LOWER(:city) LIKE LOWER(CONCAT('%', t.city, '%'))) " +
           "ORDER BY s.showTime ASC")
    Page<Show> searchShows(@Param("movieId") Long movieId,
                           @Param("city") String city,
                           @Param("startTime") LocalDateTime startTime,
                           Pageable pageable);

    @Query("SELECT s FROM Show s " +
           "JOIN FETCH s.movie m " +
           "JOIN FETCH s.screen sc " +
           "JOIN FETCH sc.theatre t " +
           "WHERE s.status = 'SCHEDULED' " +
           "AND s.showTime >= :startTime AND s.showTime <= :endTime " +
           "AND (:movieId IS NULL OR m.id = :movieId OR m.tmdbId = :movieId) " +
           "AND (:city IS NULL OR LOWER(t.city) LIKE LOWER(CONCAT('%', :city, '%')) OR LOWER(:city) LIKE LOWER(CONCAT('%', t.city, '%'))) " +
           "ORDER BY t.name ASC, s.showTime ASC")
    List<Show> findNearbyShows(@Param("movieId") Long movieId,
                               @Param("city") String city,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(s) > 0 FROM Show s WHERE (s.movie.id = :movieId OR s.movie.tmdbId = :movieId) AND s.status = 'SCHEDULED' AND s.showTime >= :startTime")
    boolean hasActiveBookableShows(@Param("movieId") Long movieId, @Param("startTime") LocalDateTime startTime);

    @Query("SELECT COUNT(s) > 0 FROM Show s WHERE (s.movie.id = :movieId OR s.movie.tmdbId = :movieId OR LOWER(s.movie.title) = LOWER(:title)) AND s.status = 'SCHEDULED' AND s.showTime >= :startTime")
    boolean hasActiveBookableShowsByMovieOrTitle(@Param("movieId") Long movieId, @Param("title") String title, @Param("startTime") LocalDateTime startTime);

    @Query("SELECT DISTINCT s.movie.id FROM Show s WHERE s.status = 'SCHEDULED' AND s.showTime >= :startTime")
    List<Long> findMovieIdsWithActiveShows(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT COUNT(s) > 0 FROM Show s WHERE s.screen.id = :screenId AND s.status = 'SCHEDULED' AND (:excludeShowId IS NULL OR s.id <> :excludeShowId) AND s.showTime = :showTime")
    boolean existsConflictingShow(@Param("screenId") Long screenId, @Param("showTime") LocalDateTime showTime, @Param("excludeShowId") Long excludeShowId);
}
