package com.moviebooking.repository;

import com.moviebooking.entity.BookingSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    @Query("SELECT bs.seat.id FROM BookingSeat bs WHERE bs.show.id = :showId")
    List<Long> findBookedSeatIdsByShowId(@Param("showId") Long showId);

    @Query("SELECT COUNT(bs) > 0 FROM BookingSeat bs WHERE bs.show.id = :showId AND bs.seat.id IN :seatIds")
    boolean existsByShowIdAndSeatIdIn(@Param("showId") Long showId, @Param("seatIds") List<Long> seatIds);

    @Query("SELECT bs.show.id, COUNT(bs) FROM BookingSeat bs WHERE bs.show.id IN :showIds GROUP BY bs.show.id")
    List<Object[]> countBookedSeatsByShowIds(@Param("showIds") List<Long> showIds);

    void deleteByBookingId(Long bookingId);
}
