package com.moviebooking.service;

import com.moviebooking.dto.request.ScreenRequest;
import com.moviebooking.dto.request.SeatRequest;
import com.moviebooking.dto.request.TheatreRequest;
import com.moviebooking.dto.response.ScreenResponse;
import com.moviebooking.dto.response.SeatResponse;
import com.moviebooking.dto.response.TheatreResponse;
import com.moviebooking.entity.*;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TheatreService {

    private final TheatreRepository theatreRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;

    @Transactional(readOnly = true)
    public List<TheatreResponse> getAllActiveTheatres() {
        return theatreRepository.findByIsActiveTrue().stream()
                .map(this::mapToTheatreResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getAllCities() {
        return theatreRepository.findDistinctCities();
    }

    @Transactional
    public List<TheatreResponse> getTheatresByCity(String city) {
        if (city == null || city.isBlank()) {
            return getAllActiveTheatres();
        }
        List<Theatre> theatres = theatreRepository.searchByCityLikeIgnoreCase(city.trim());
        if (theatres.isEmpty()) {
            theatres = theatreRepository.findByCityIgnoreCaseAndIsActiveTrue(city.trim());
        }
        return theatres.stream()
                .map(this::mapToTheatreResponse)
                .toList();
    }

    @Transactional
    public List<Theatre> ensureCityTheatres(String city) {
        if (city == null || city.isBlank()) return Collections.emptyList();
        String normalizedCity = city.trim();
        return theatreRepository.searchByCityLikeIgnoreCase(normalizedCity);
    }

    @Transactional(readOnly = true)
    public TheatreResponse getTheatreById(Long id) {
        Theatre theatre = theatreRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found with id: " + id));
        return mapToTheatreResponse(theatre);
    }

    @Transactional
    public TheatreResponse createTheatre(TheatreRequest request) {
        Theatre theatre = Theatre.builder()
                .name(request.getName())
                .city(request.getCity())
                .address(request.getAddress())
                .isActive(true)
                .build();

        return mapToTheatreResponse(theatreRepository.save(theatre));
    }

    @Transactional
    public ScreenResponse addScreenToTheatre(Long theatreId, ScreenRequest request) {
        Theatre theatre = theatreRepository.findByIdAndIsActiveTrue(theatreId)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre not found with id: " + theatreId));

        Screen screen = Screen.builder()
                .name(request.getName())
                .totalSeats(request.getTotalSeats())
                .theatre(theatre)
                .build();

        Screen savedScreen = screenRepository.save(screen);

        return ScreenResponse.builder()
                .id(savedScreen.getId())
                .name(savedScreen.getName())
                .totalSeats(savedScreen.getTotalSeats())
                .theatreId(theatre.getId())
                .theatreName(theatre.getName())
                .build();
    }

    @Transactional
    public List<SeatResponse> addSeatsToScreen(Long screenId, List<SeatRequest> seatRequests) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + screenId));

        List<Seat> seatsToSave = new ArrayList<>();
        for (SeatRequest sr : seatRequests) {
            if (seatRepository.existsByScreenIdAndSeatNumber(screenId, sr.getSeatNumber())) {
                throw new IllegalArgumentException("Seat " + sr.getSeatNumber() + " already exists in screen " + screenId);
            }
            seatsToSave.add(Seat.builder()
                    .screen(screen)
                    .seatNumber(sr.getSeatNumber())
                    .seatType(sr.getSeatType())
                    .build());
        }

        List<Seat> savedSeats = seatRepository.saveAll(seatsToSave);

        return savedSeats.stream()
                .map(s -> SeatResponse.builder()
                        .id(s.getId())
                        .seatNumber(s.getSeatNumber())
                        .seatType(s.getSeatType())
                        .isAvailable(true)
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScreenResponse> getScreensByTheatre(Long theatreId) {
        return screenRepository.findByTheatreId(theatreId).stream()
                .map(s -> ScreenResponse.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .totalSeats(s.getTotalSeats())
                        .theatreId(theatreId)
                        .theatreName(s.getTheatre().getName())
                        .build())
                .toList();
    }

    private TheatreResponse mapToTheatreResponse(Theatre theatre) {
        return TheatreResponse.builder()
                .id(theatre.getId())
                .name(theatre.getName())
                .city(theatre.getCity())
                .address(theatre.getAddress())
                .isActive(theatre.getIsActive())
                .build();
    }
}
