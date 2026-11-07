package com.moviebooking.repository;

import com.moviebooking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.user u " +
           "JOIN FETCH b.show s " +
           "JOIN FETCH s.movie m " +
           "JOIN FETCH s.screen sc " +
           "JOIN FETCH sc.theatre t " +
           "LEFT JOIN FETCH b.bookingSeats bs " +
           "LEFT JOIN FETCH bs.seat " +
           "WHERE b.id = :id")
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);

    @Query(value = "SELECT b FROM Booking b " +
                   "JOIN FETCH b.user u " +
                   "JOIN FETCH b.show s " +
                   "JOIN FETCH s.movie m " +
                   "JOIN FETCH s.screen sc " +
                   "JOIN FETCH sc.theatre t " +
                   "WHERE b.user.id = :userId " +
                   "ORDER BY b.bookingTime DESC",
           countQuery = "SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId")
    Page<Booking> findByUserId(@Param("userId") Long userId, Pageable pageable);

    List<Booking> findByShowId(Long showId);
}
