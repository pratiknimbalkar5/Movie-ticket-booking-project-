package com.moviebooking.service;

import com.moviebooking.dto.request.ShowRequest;
import com.moviebooking.dto.response.NearbyTheatreShowsResponse;
import com.moviebooking.dto.response.SeatResponse;
import com.moviebooking.dto.response.ShowResponse;
import com.moviebooking.entity.Movie;
import com.moviebooking.entity.Screen;
import com.moviebooking.entity.Seat;
import com.moviebooking.entity.Show;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.repository.BookingSeatRepository;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.ScreenRepository;
import com.moviebooking.repository.SeatRepository;
import com.moviebooking.repository.ShowRepository;
import com.moviebooking.repository.TheatreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final SeatRepository seatRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final TheatreRepository theatreRepository;
    private final TheatreService theatreService;

    @Transactional
    public Page<ShowResponse> searchShows(Long movieId, String city, Pageable pageable) {
        if (city != null && !city.isBlank()) {
            theatreService.ensureCityTheatres(city.trim());
        }
        LocalDateTime now = LocalDateTime.now();
        return showRepository.searchShows(movieId, city, now, pageable).map(this::mapToShowResponse);
    }

    @Transactional
    public List<NearbyTheatreShowsResponse> getNearbyShows(String city, Long movieId, LocalDate date) {
        return getNearbyShows(city, movieId, null, null, null, date);
    }

    @Transactional
    public List<NearbyTheatreShowsResponse> getNearbyShows(String city, Long movieId, Double userLat, Double userLon, Double radiusKm, LocalDate date) {
        String cleanCity = (city != null && !city.isBlank()) ? city.trim() : null;
        if (cleanCity == null && movieId == null && (userLat == null || userLon == null)) {
            return Collections.emptyList();
        }
        if (cleanCity != null) {
            theatreService.ensureCityTheatres(cleanCity);
        }
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        LocalDateTime start = targetDate.isEqual(LocalDate.now()) ? LocalDateTime.now().minusHours(1) : targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(23, 59, 59);

        List<Show> shows = showRepository.findNearbyShows(movieId, cleanCity, start, end);

        // Fallback 1: If today's shows are done or none on this date, widen to upcoming 3 days
        if (shows.isEmpty()) {
            shows = showRepository.findNearbyShows(movieId, cleanCity, LocalDateTime.now(), LocalDateTime.now().plusDays(3));
        }

        // Fallback 2: If movieId was passed but yielded no shows (e.g. unmapped TMDB id), fetch all shows in this city
        if (shows.isEmpty() && cleanCity != null) {
            shows = showRepository.findNearbyShows(null, cleanCity, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(3));
        }

        // Fallback 3: If still empty and cleanCity was provided, fetch any shows for theatres matching city
        if (shows.isEmpty() && cleanCity != null) {
            List<com.moviebooking.entity.Theatre> matchingTheatres = theatreRepository.searchByCityLikeIgnoreCase(cleanCity);
            if (!matchingTheatres.isEmpty()) {
                List<Long> theatreIds = matchingTheatres.stream().map(com.moviebooking.entity.Theatre::getId).toList();
                shows = showRepository.findAll().stream()
                        .filter(s -> s.getStatus() == Show.ShowStatus.SCHEDULED)
                        .filter(s -> theatreIds.contains(s.getScreen().getTheatre().getId()))
                        .filter(s -> s.getShowTime().isAfter(LocalDateTime.now().minusHours(2)))
                        .sorted(Comparator.comparing(Show::getShowTime))
                        .limit(30)
                        .toList();
            }
        }

        Map<Long, List<Show>> showsByTheatre = shows.stream()
                .collect(Collectors.groupingBy(s -> s.getScreen().getTheatre().getId(), LinkedHashMap::new, Collectors.toList()));

        double refLat;
        double refLon;
        if (userLat != null && userLon != null) {
            refLat = userLat;
            refLon = userLon;
        } else {
            var cityCoords = com.moviebooking.util.CityCoordinatesHelper.resolveCityCoords(cleanCity);
            refLat = cityCoords.latitude();
            refLon = cityCoords.longitude();
        }

        List<NearbyTheatreShowsResponse> result = new ArrayList<>();
        double maxRadius = (radiusKm != null && radiusKm > 0) ? radiusKm : 35.0;

        for (Map.Entry<Long, List<Show>> entry : showsByTheatre.entrySet()) {
            List<Show> theatreShows = entry.getValue();
            if (theatreShows.isEmpty()) continue;
            var theatre = theatreShows.get(0).getScreen().getTheatre();

            double distance = (theatre.getLatitude() != null && theatre.getLongitude() != null)
                    ? com.moviebooking.util.CityCoordinatesHelper.calculateHaversineKm(refLat, refLon, theatre.getLatitude(), theatre.getLongitude())
                    : 2.5;

            // When searching by GPS with explicit radius, enforce maximum distance
            if (userLat != null && userLon != null && cleanCity == null && distance > maxRadius) {
                continue;
            }

            List<NearbyTheatreShowsResponse.NearbyShowItem> items = theatreShows.stream().map(s -> {
                int totalSeats = (s.getScreen().getTotalSeats() != null) ? s.getScreen().getTotalSeats() : 86;
                int bookedSeats = bookingSeatRepository.findBookedSeatIdsByShowId(s.getId()).size();
                int availableSeats = Math.max(0, totalSeats - bookedSeats);
                String format = s.getScreen().getName().contains("IMAX") ? "IMAX Laser 3D"
                        : s.getScreen().getName().contains("Atmos") ? "Dolby Atmos 7.1" : "2D Digital";

                return NearbyTheatreShowsResponse.NearbyShowItem.builder()
                        .showId(s.getId())
                        .movieId(s.getMovie().getId())
                        .movieTitle(s.getMovie().getTitle())
                        .moviePosterUrl(s.getMovie().getPosterUrl())
                        .movieLanguage(s.getMovie().getLanguage())
                        .screenName(s.getScreen().getName())
                        .screenFormat(format)
                        .showTime(s.getShowTime())
                        .basePrice(s.getBasePrice())
                        .premiumPrice(s.getPremiumPrice())
                        .vipPrice(s.getVipPrice())
                        .totalSeats(totalSeats)
                        .availableSeats(availableSeats)
                        .dataSource("PARTNER_DEMO_SCHEDULE")
                        .priceNote("₹" + s.getBasePrice() + " - ₹" + s.getVipPrice())
                        .seatAvailabilityNote(availableSeats + " seats left")
                        .officialBookingUrl("/seat-selection/" + s.getId())
                        .build();
            }).toList();

            result.add(NearbyTheatreShowsResponse.builder()
                    .theatreId(theatre.getId())
                    .theatreName(theatre.getName())
                    .city(theatre.getCity())
                    .address(theatre.getAddress())
                    .latitude(theatre.getLatitude())
                    .longitude(theatre.getLongitude())
                    .distanceKm(distance)
                    .shows(items)
                    .build());
        }

        // Sort ascending by true geographical distance (closest theatres first)
        result.sort(Comparator.comparingDouble(r -> r.getDistanceKm() != null ? r.getDistanceKm() : 999.0));
        return result;
    }

    @Transactional
    public List<NearbyTheatreShowsResponse> scanNearbyShowsByGps(Double userLat, Double userLon, Double maxRadiusKm, LocalDate date) {
        if (userLat == null || userLon == null) {
            return Collections.emptyList();
        }
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        LocalDateTime start = targetDate.isEqual(LocalDate.now()) ? LocalDateTime.now() : targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(23, 59, 59);

        // Fetch all upcoming shows for date across all cities
        List<Show> shows = showRepository.findNearbyShows(null, null, start, end);
        if (shows.isEmpty() && targetDate.isEqual(LocalDate.now())) {
            shows = showRepository.findNearbyShows(null, null, LocalDateTime.now(), LocalDateTime.now().plusDays(3));
        }

        Map<Long, List<Show>> showsByTheatre = shows.stream()
                .collect(Collectors.groupingBy(s -> s.getScreen().getTheatre().getId(), LinkedHashMap::new, Collectors.toList()));

        record TheatreCandidate(com.moviebooking.entity.Theatre theatre, List<Show> shows, double distanceKm) {}

        List<TheatreCandidate> candidates = new ArrayList<>();
        double radius = (maxRadiusKm != null && maxRadiusKm > 0) ? maxRadiusKm : 10.0;

        for (Map.Entry<Long, List<Show>> entry : showsByTheatre.entrySet()) {
            List<Show> theatreShows = entry.getValue();
            if (theatreShows.isEmpty()) continue;
            var theatre = theatreShows.get(0).getScreen().getTheatre();

            double distance;
            if (userLat != null && userLon != null && theatre.getLatitude() != null && theatre.getLongitude() != null) {
                distance = calculateHaversineKm(userLat, userLon, theatre.getLatitude(), theatre.getLongitude());
            } else {
                distance = Math.round((1.2 + (theatre.getId() % 5) * 1.5) * 10.0) / 10.0;
            }
            candidates.add(new TheatreCandidate(theatre, theatreShows, Math.round(distance * 10.0) / 10.0));
        }

        // Sort candidates ascending by distance
        candidates.sort(Comparator.comparingDouble(TheatreCandidate::distanceKm));

        // Filter strictly to in-radius candidates (Default: 10km, or user-selected 20km/30km)
        List<TheatreCandidate> targetCandidates;
        if (userLat != null && userLon != null) {
            targetCandidates = candidates.stream()
                    .filter(c -> c.distanceKm() <= radius)
                    .toList();
        } else {
            targetCandidates = candidates.stream().limit(15).toList();
        }

        // Now map shows ONLY for the target candidate cinemas
        List<NearbyTheatreShowsResponse> result = new ArrayList<>();
        for (TheatreCandidate candidate : targetCandidates) {
            List<NearbyTheatreShowsResponse.NearbyShowItem> items = candidate.shows().stream().map(s -> {
                int totalSeats = (s.getScreen().getTotalSeats() != null) ? s.getScreen().getTotalSeats() : 86;
                int bookedSeats = bookingSeatRepository.findBookedSeatIdsByShowId(s.getId()).size();
                int availableSeats = Math.max(0, totalSeats - bookedSeats);
                String format = s.getScreen().getName().contains("IMAX") ? "IMAX Laser 3D"
                        : s.getScreen().getName().contains("Atmos") ? "Dolby Atmos 7.1" : "2D Digital";

                return NearbyTheatreShowsResponse.NearbyShowItem.builder()
                        .showId(s.getId())
                        .movieId(s.getMovie().getId())
                        .movieTitle(s.getMovie().getTitle())
                        .moviePosterUrl(s.getMovie().getPosterUrl())
                        .movieLanguage(s.getMovie().getLanguage())
                        .screenName(s.getScreen().getName())
                        .screenFormat(format)
                        .showTime(s.getShowTime())
                        .basePrice(s.getBasePrice())
                        .premiumPrice(s.getPremiumPrice())
                        .vipPrice(s.getVipPrice())
                        .totalSeats(totalSeats)
                        .availableSeats(availableSeats)
                        .dataSource("PARTNER_DEMO_SCHEDULE")
                        .priceNote("₹" + s.getBasePrice() + " - ₹" + s.getVipPrice())
                        .seatAvailabilityNote(availableSeats + " seats left")
                        .officialBookingUrl("/seat-selection/" + s.getId())
                        .build();
            }).toList();

            result.add(NearbyTheatreShowsResponse.builder()
                    .theatreId(candidate.theatre().getId())
                    .theatreName(candidate.theatre().getName())
                    .city(candidate.theatre().getCity())
                    .address(candidate.theatre().getAddress())
                    .latitude(candidate.theatre().getLatitude())
                    .longitude(candidate.theatre().getLongitude())
                    .distanceKm(candidate.distanceKm())
                    .shows(items)
                    .build());
        }

        return result;
    }

    private double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Transactional(readOnly = true)
    public ShowResponse getShowById(Long id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found with id: " + id));
        return mapToShowResponse(show);
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatAvailabilityForShow(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found with id: " + showId));

        List<Seat> allSeats = new ArrayList<>(seatRepository.findByScreenIdOrderBySeatNumberAsc(show.getScreen().getId()));
        allSeats.sort((s1, s2) -> {
            String n1 = s1.getSeatNumber();
            String n2 = s2.getSeatNumber();
            String row1 = n1.replaceAll("\\d", "");
            String row2 = n2.replaceAll("\\d", "");
            int rowCmp = row1.compareTo(row2);
            if (rowCmp != 0) return rowCmp;
            int num1 = Integer.parseInt(n1.replaceAll("\\D", "0"));
            int num2 = Integer.parseInt(n2.replaceAll("\\D", "0"));
            return Integer.compare(num1, num2);
        });
        Set<Long> bookedSeatIds = new HashSet<>(bookingSeatRepository.findBookedSeatIdsByShowId(showId));

        return allSeats.stream()
                .map(seat -> {
                    boolean isBooked = bookedSeatIds.contains(seat.getId());
                    BigDecimal price = switch (seat.getSeatType()) {
                        case REGULAR -> show.getBasePrice();
                        case PREMIUM -> show.getPremiumPrice();
                        case VIP -> show.getVipPrice();
                    };

                    return SeatResponse.builder()
                            .id(seat.getId())
                            .seatNumber(seat.getSeatNumber())
                            .seatType(seat.getSeatType())
                            .isAvailable(!isBooked)
                            .price(price)
                            .build();
                })
                .toList();
    }

    @Transactional
    public ShowResponse createShow(ShowRequest request) {
        if (request.getShowTime() == null || request.getShowTime().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new IllegalArgumentException("Show time cannot be in the past");
        }

        Movie movie = movieRepository.findByIdAndIsActiveTrue(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie not found with id: " + request.getMovieId()));

        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new ResourceNotFoundException("Screen not found with id: " + request.getScreenId()));

        boolean conflicting = showRepository.existsConflictingShow(screen.getId(), request.getShowTime(), null);
        if (conflicting) {
            throw new IllegalArgumentException("A conflicting show is already scheduled for this screen at " + request.getShowTime());
        }

        Show show = Show.builder()
                .movie(movie)
                .screen(screen)
                .showTime(request.getShowTime())
                .basePrice(request.getBasePrice())
                .premiumPrice(request.getPremiumPrice())
                .vipPrice(request.getVipPrice())
                .status(Show.ShowStatus.SCHEDULED)
                .build();

        return mapToShowResponse(showRepository.save(show));
    }

    @Transactional
    public ShowResponse updateShowStatus(Long id, Show.ShowStatus status) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show not found with id: " + id));

        show.setStatus(status);
        return mapToShowResponse(showRepository.save(show));
    }

    public ShowResponse mapToShowResponse(Show show) {
        return ShowResponse.builder()
                .id(show.getId())
                .movieId(show.getMovie().getId())
                .movieTitle(show.getMovie().getTitle())
                .moviePosterUrl(show.getMovie().getPosterUrl())
                .movieLanguage(show.getMovie().getLanguage())
                .movieDurationMinutes(show.getMovie().getDurationMinutes())
                .screenId(show.getScreen().getId())
                .screenName(show.getScreen().getName())
                .theatreId(show.getScreen().getTheatre().getId())
                .theatreName(show.getScreen().getTheatre().getName())
                .theatreCity(show.getScreen().getTheatre().getCity())
                .showTime(show.getShowTime())
                .basePrice(show.getBasePrice())
                .premiumPrice(show.getPremiumPrice())
                .vipPrice(show.getVipPrice())
                .status(show.getStatus())
                .build();
    }
}
