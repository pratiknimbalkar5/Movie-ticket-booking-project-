package com.moviebooking.repository;

import com.moviebooking.entity.Theatre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TheatreRepository extends JpaRepository<Theatre, Long> {

    List<Theatre> findByIsActiveTrue();

    Page<Theatre> findByIsActiveTrue(Pageable pageable);

    List<Theatre> findByCityIgnoreCaseAndIsActiveTrue(String city);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM Theatre t WHERE t.isActive = true AND (LOWER(t.city) LIKE LOWER(CONCAT('%', :city, '%')) OR LOWER(:city) LIKE LOWER(CONCAT('%', t.city, '%')))")
    List<Theatre> searchByCityLikeIgnoreCase(@org.springframework.data.repository.query.Param("city") String city);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT t.city FROM Theatre t WHERE t.isActive = true ORDER BY t.city ASC")
    List<String> findDistinctCities();

    Optional<Theatre> findByIdAndIsActiveTrue(Long id);

    boolean existsByNameAndCity(String name, String city);
}
