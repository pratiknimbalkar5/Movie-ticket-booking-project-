package com.moviebooking.repository;

import com.moviebooking.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByScreenIdOrderBySeatNumberAsc(Long screenId);

    List<Seat> findByIdIn(List<Long> seatIds);

    /**
     * Pessimistic Write Lock (SELECT ... FOR UPDATE).
     * Locks the requested seats during the booking transaction to prevent concurrent
     * booking requests from proceeding at the exact same moment.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id IN :seatIds")
    List<Seat> findByIdInWithPessimisticLock(@Param("seatIds") List<Long> seatIds);

    boolean existsByScreenIdAndSeatNumber(Long screenId, String seatNumber);
}
