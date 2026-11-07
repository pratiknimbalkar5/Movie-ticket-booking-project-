package com.moviebooking.repository;

import com.moviebooking.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, Long> {

    List<Screen> findByTheatreId(Long theatreId);

    Optional<Screen> findByIdAndTheatreId(Long id, Long theatreId);

    @Query("SELECT s FROM Screen s JOIN FETCH s.theatre")
    List<Screen> findAllWithTheatre();
}
