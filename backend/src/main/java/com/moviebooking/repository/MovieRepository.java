package com.moviebooking.repository;

import com.moviebooking.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    Page<Movie> findByIsActiveTrue(Pageable pageable);

    List<Movie> findByIsActiveTrueOrderByRatingDesc();

    List<Movie> findByIsActiveTrueAndIsUpcomingFalseOrderByRatingDesc();

    List<Movie> findByIsActiveTrueAndIsUpcomingFalseOrderByReleaseDateDesc();

    List<Movie> findByIsActiveTrueAndIsUpcomingTrueOrderByReleaseDateAsc();

    List<Movie> findByIdInAndIsActiveTrueOrderByRatingDesc(List<Long> ids);

    @Query("SELECT DISTINCT m.genre FROM Movie m WHERE m.isActive = true AND m.genre IS NOT NULL ORDER BY m.genre ASC")
    List<String> findDistinctGenres();

    @Query("SELECT m FROM Movie m WHERE m.isActive = true " +
           "AND (:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND (:genre IS NULL OR LOWER(m.genre) = LOWER(:genre))")
    Page<Movie> searchMovies(@Param("title") String title,
                             @Param("genre") String genre,
                             Pageable pageable);

    Optional<Movie> findByIdAndIsActiveTrue(Long id);

    Optional<Movie> findFirstByTitleIgnoreCase(String title);

    Optional<Movie> findByTmdbId(Long tmdbId);

    Optional<Movie> findByImdbId(String imdbId);
}
