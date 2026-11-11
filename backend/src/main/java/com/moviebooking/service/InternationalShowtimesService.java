package com.moviebooking.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.dto.response.internationalshowtimes.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service integrating the official International Showtimes API V5.
 * Documentation: https://assemble-api.internationalshowtimes.com/documentation/v5/
 * Base URL: https://api.internationalshowtimes.com/v5
 * Authentication: Header "X-Api-Key" with INTERNATIONAL_SHOWTIMES_API_KEY
 */
@Service
@Slf4j
public class InternationalShowtimesService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.internationalshowtimes.base-url:https://api.internationalshowtimes.com/v5}")
    private String baseUrl;

    @Value("${app.internationalshowtimes.api-key:}")
    private String apiKey;

    // Status tracking for reporting
    private volatile boolean lastAuthSuccessful = false;
    private volatile boolean indiaTerritorySupported = false;
    private volatile int lastDiscoveredIndianCinemas = 0;
    private volatile int lastDiscoveredIndianMovies = 0;
    private volatile int lastDiscoveredIndianShowtimes = 0;
    private volatile String lastStatusMessage = "Uninitialized";

    // In-memory cache
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(Object data, LocalDateTime expiresAt) {
        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    @Autowired
    public InternationalShowtimesService(
            @Value("${app.internationalshowtimes.base-url:https://api.internationalshowtimes.com/v5}") String baseUrl,
            @Value("${app.internationalshowtimes.connect-timeout-seconds:8}") int connectTimeout,
            @Value("${app.internationalshowtimes.read-timeout-seconds:10}") int readTimeout) {

        this.objectMapper = new ObjectMapper();
        this.baseUrl = baseUrl;

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(connectTimeout))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeout));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "CinemaPulse-App/2.0")
                .build();
    }

    // Testing constructor
    public InternationalShowtimesService(RestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.baseUrl = "https://api.internationalshowtimes.com/v5";
    }

    @jakarta.annotation.PostConstruct
    public void postConstruct() {
        if (!isConfigured()) {
            loadFromDotenv();
        }
        if (isConfigured()) {
            verifyAccountAccess();
        } else {
            this.lastStatusMessage = "INTERNATIONAL_SHOWTIMES_API_KEY environment variable is not configured. External live showtime queries disabled.";
            log.info("International Showtimes API V5 is not configured (INTERNATIONAL_SHOWTIMES_API_KEY missing).");
        }
    }

    private void loadFromDotenv() {
        java.nio.file.Path[] paths = new java.nio.file.Path[]{
                java.nio.file.Paths.get(".env"),
                java.nio.file.Paths.get("backend/.env"),
                java.nio.file.Paths.get("../.env"),
                java.nio.file.Paths.get("../../.env")
        };
        for (java.nio.file.Path p : paths) {
            if (java.nio.file.Files.isRegularFile(p)) {
                try (var reader = java.nio.file.Files.newBufferedReader(p)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            String k = line.substring(0, eq).trim();
                            String v = line.substring(eq + 1).trim();
                            if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                                if (v.length() >= 2) v = v.substring(1, v.length() - 1);
                            }
                            if ("INTERNATIONAL_SHOWTIMES_API_KEY".equalsIgnoreCase(k) && (apiKey == null || apiKey.isBlank())) {
                                this.apiKey = v;
                                log.info("INTERNATIONAL_SHOWTIMES_API_KEY dynamically loaded from {}", p);
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Probes V5 API to verify authentication, country coverage (India/IN), and permissions.
     */
    public synchronized Map<String, Object> verifyAccountAccess() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("provider", "International Showtimes API V5");
        status.put("configured", isConfigured());
        status.put("paidServiceDisabled", !isConfigured());
        status.put("policyNotice", "Operating under Strict Free API Policy. Paid external subscriptions are permanently decoupled from active runtime.");

        if (!isConfigured()) {
            this.lastAuthSuccessful = false;
            this.indiaTerritorySupported = false;
            this.lastStatusMessage = "INTERNATIONAL_SHOWTIMES_API_KEY is not set. Deactivated under Strict Free API Policy (commercial service requires €150/mo paid plan). Live multiplex showtimes served via internal directory schedule.";
            status.put("authenticated", false);
            status.put("indiaSupported", false);
            status.put("message", this.lastStatusMessage);
            return status;
        }

        try {
            // 1. Check supported countries via GET /v5/countries/
            IsApiResponse countriesRes = restClient.get()
                    .uri("/countries/")
                    .header("X-Api-Key", apiKey)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        int code = resp.getStatusCode().value();
                        if (code == 401 || code == 403) {
                            throw new RuntimeException("HTTP " + code + " Unauthorized / Forbidden: Invalid INTERNATIONAL_SHOWTIMES_API_KEY or plan inactive");
                        }
                        throw new RuntimeException("HTTP " + code + " from International Showtimes V5");
                    })
                    .body(IsApiResponse.class);

            this.lastAuthSuccessful = true;
            status.put("authenticated", true);

            boolean hasIndia = false;
            if (countriesRes != null && countriesRes.getCountries() != null) {
                hasIndia = countriesRes.getCountries().stream()
                        .anyMatch(c -> "IN".equalsIgnoreCase(c.getCode()) || "India".equalsIgnoreCase(c.getName()));
            }

            // 2. Also check cinemas with countries=IN
            List<IsCinemaDto> indianCinemas = fetchIndianCinemasFromApi();
            this.lastDiscoveredIndianCinemas = indianCinemas.size();
            if (lastDiscoveredIndianCinemas > 0) {
                hasIndia = true;
            }

            this.indiaTerritorySupported = hasIndia;
            status.put("indiaSupported", hasIndia);
            status.put("indianCinemasCount", lastDiscoveredIndianCinemas);

            if (hasIndia) {
                this.lastStatusMessage = "International Showtimes V5 active with verified Indian coverage (" + lastDiscoveredIndianCinemas + " cinemas).";
            } else {
                this.lastStatusMessage = "Authentication successful (HTTP 200), but India (IN) is not active on this account tier.";
            }
            status.put("message", this.lastStatusMessage);

        } catch (Exception e) {
            this.lastAuthSuccessful = false;
            this.indiaTerritorySupported = false;
            this.lastStatusMessage = "International Showtimes V5 verification failed: " + e.getMessage();
            log.warn("International Showtimes V5 check: {}", e.getMessage());
            status.put("authenticated", false);
            status.put("indiaSupported", false);
            status.put("message", this.lastStatusMessage);
        }

        return status;
    }

    /**
     * Fetch cinemas in India via /v5/cinemas/?countries=IN
     */
    public List<IsCinemaDto> fetchIndianCinemasFromApi() {
        if (!isConfigured()) return Collections.emptyList();

        String cacheKey = "is_cinemas_in";
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return (List<IsCinemaDto>) cached.data();
        }

        try {
            IsApiResponse resp = restClient.get()
                    .uri("/cinemas/?countries=IN")
                    .header("X-Api-Key", apiKey)
                    .retrieve()
                    .body(IsApiResponse.class);

            List<IsCinemaDto> list = (resp != null && resp.getCinemas() != null) ? resp.getCinemas() : Collections.emptyList();
            this.lastDiscoveredIndianCinemas = list.size();
            cache.put(cacheKey, new CacheEntry(list, LocalDateTime.now().plusHours(4)));
            return list;
        } catch (Exception e) {
            log.warn("Failed to fetch Indian cinemas from International Showtimes V5: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetch cinemas near coordinates via /v5/cinemas/?location={lat},{lon}&distance={km}
     */
    public List<IsCinemaDto> fetchNearbyCinemasFromApi(double lat, double lon, double radiusKm) {
        if (!isConfigured()) return Collections.emptyList();

        String cacheKey = String.format("is_cinemas_geo_%.2f_%.2f_%.0f", lat, lon, radiusKm);
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return (List<IsCinemaDto>) cached.data();
        }

        try {
            String uri = String.format("/cinemas/?location=%.4f,%.4f&distance=%.0f", lat, lon, radiusKm);
            IsApiResponse resp = restClient.get()
                    .uri(uri)
                    .header("X-Api-Key", apiKey)
                    .retrieve()
                    .body(IsApiResponse.class);

            List<IsCinemaDto> list = (resp != null && resp.getCinemas() != null) ? resp.getCinemas() : Collections.emptyList();
            cache.put(cacheKey, new CacheEntry(list, LocalDateTime.now().plusHours(2)));
            return list;
        } catch (Exception e) {
            log.warn("Failed to fetch nearby cinemas from International Showtimes V5: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetch movies playing in India via /v5/movies/?countries=IN
     */
    public List<IsMovieDto> fetchIndianMoviesFromApi() {
        if (!isConfigured()) return Collections.emptyList();

        String cacheKey = "is_movies_in";
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return (List<IsMovieDto>) cached.data();
        }

        try {
            IsApiResponse resp = restClient.get()
                    .uri("/movies/?countries=IN")
                    .header("X-Api-Key", apiKey)
                    .retrieve()
                    .body(IsApiResponse.class);

            List<IsMovieDto> list = (resp != null && resp.getMovies() != null) ? resp.getMovies() : Collections.emptyList();
            this.lastDiscoveredIndianMovies = list.size();
            cache.put(cacheKey, new CacheEntry(list, LocalDateTime.now().plusHours(2)));
            return list;
        } catch (Exception e) {
            log.warn("Failed to fetch Indian movies from International Showtimes V5: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Look up a movie by TMDB ID via /v5/movies/?tmdb_id={id}
     */
    public Optional<IsMovieDto> findMovieByTmdbId(Long tmdbId) {
        if (!isConfigured() || tmdbId == null) return Optional.empty();

        String cacheKey = "is_movie_tmdb_" + tmdbId;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return Optional.ofNullable((IsMovieDto) cached.data());
        }

        try {
            IsApiResponse resp = restClient.get()
                    .uri("/movies/?tmdb_id=" + tmdbId)
                    .header("X-Api-Key", apiKey)
                    .retrieve()
                    .body(IsApiResponse.class);

            if (resp != null && resp.getMovies() != null && !resp.getMovies().isEmpty()) {
                IsMovieDto m = resp.getMovies().get(0);
                cache.put(cacheKey, new CacheEntry(m, LocalDateTime.now().plusHours(6)));
                return Optional.of(m);
            }
        } catch (Exception e) {
            log.debug("No movie found on International Showtimes V5 for tmdb_id {}: {}", tmdbId, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Look up a movie by IMDb ID via /v5/movies/?imdb_id={imdbId}
     */
    public Optional<IsMovieDto> findMovieByImdbId(String imdbId) {
        if (!isConfigured() || imdbId == null || imdbId.isBlank()) return Optional.empty();

        String cacheKey = "is_movie_imdb_" + imdbId.trim();
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return Optional.ofNullable((IsMovieDto) cached.data());
        }

        try {
            IsApiResponse resp = restClient.get()
                    .uri("/movies/?imdb_id=" + imdbId.trim())
                    .header("X-Api-Key", apiKey)
                    .retrieve()
                    .body(IsApiResponse.class);

            if (resp != null && resp.getMovies() != null && !resp.getMovies().isEmpty()) {
                IsMovieDto m = resp.getMovies().get(0);
                cache.put(cacheKey, new CacheEntry(m, LocalDateTime.now().plusHours(6)));
                return Optional.of(m);
            }
        } catch (Exception e) {
            log.debug("No movie found on International Showtimes V5 for imdb_id {}: {}", imdbId, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Retrieve live showtimes for a cinema via /v5/showtimes/?cinema_id={id}&date={date}
     */
    public List<IsShowtimeDto> fetchShowtimesForCinema(String cinemaId, LocalDate date) {
        if (!isConfigured() || cinemaId == null || cinemaId.isBlank()) return Collections.emptyList();

        String targetDate = (date != null) ? date.toString() : LocalDate.now().toString();
        String cacheKey = "is_st_c_" + cinemaId + "_" + targetDate;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return (List<IsShowtimeDto>) cached.data();
        }

        try {
            String uri = String.format("/showtimes/?cinema_id=%s&date=%s", cinemaId, targetDate);
            IsApiResponse resp = restClient.get()
                    .uri(uri)
                    .header("X-Api-Key", apiKey)
                    .retrieve()
                    .body(IsApiResponse.class);

            List<IsShowtimeDto> list = (resp != null && resp.getShowtimes() != null) ? resp.getShowtimes() : Collections.emptyList();
            cache.put(cacheKey, new CacheEntry(list, LocalDateTime.now().plusMinutes(30)));
            return list;
        } catch (Exception e) {
            log.warn("Failed to fetch showtimes for cinema {} from V5: {}", cinemaId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Retrieve live showtimes for a movie via /v5/showtimes/?movie_id={id}&date={date}
     */
    public List<IsShowtimeDto> fetchShowtimesForMovie(String movieId, LocalDate date) {
        if (!isConfigured() || movieId == null || movieId.isBlank()) return Collections.emptyList();

        String targetDate = (date != null) ? date.toString() : LocalDate.now().toString();
        String cacheKey = "is_st_m_" + movieId + "_" + targetDate;
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return (List<IsShowtimeDto>) cached.data();
        }

        try {
            String uri = String.format("/showtimes/?movie_id=%s&date=%s", movieId, targetDate);
            IsApiResponse resp = restClient.get()
                    .uri(uri)
                    .header("X-Api-Key", apiKey)
                    .retrieve()
                    .body(IsApiResponse.class);

            List<IsShowtimeDto> list = (resp != null && resp.getShowtimes() != null) ? resp.getShowtimes() : Collections.emptyList();
            this.lastDiscoveredIndianShowtimes += list.size();
            cache.put(cacheKey, new CacheEntry(list, LocalDateTime.now().plusMinutes(30)));
            return list;
        } catch (Exception e) {
            log.warn("Failed to fetch showtimes for movie {} from V5: {}", movieId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Check if a movie has verified current theatrical screening evidence in India from V5.
     */
    public boolean hasCurrentIndianScreeningEvidence(Long tmdbId, String imdbId, String title) {
        if (!isConfigured() || !indiaTerritorySupported) {
            return false;
        }

        // Try lookup by TMDB ID
        Optional<IsMovieDto> byTmdb = findMovieByTmdbId(tmdbId);
        if (byTmdb.isPresent()) {
            List<IsShowtimeDto> st = fetchShowtimesForMovie(byTmdb.get().getId(), LocalDate.now());
            if (!st.isEmpty()) return true;
        }

        // Try lookup by IMDb ID
        if (imdbId != null && !imdbId.isBlank()) {
            Optional<IsMovieDto> byImdb = findMovieByImdbId(imdbId);
            if (byImdb.isPresent()) {
                List<IsShowtimeDto> st = fetchShowtimesForMovie(byImdb.get().getId(), LocalDate.now());
                if (!st.isEmpty()) return true;
            }
        }

        return false;
    }

    // Diagnostics Getters
    public boolean isLastAuthSuccessful() { return lastAuthSuccessful; }
    public boolean isIndiaTerritorySupported() { return indiaTerritorySupported; }
    public int getLastDiscoveredIndianCinemas() { return lastDiscoveredIndianCinemas; }
    public int getLastDiscoveredIndianMovies() { return lastDiscoveredIndianMovies; }
    public int getLastDiscoveredIndianShowtimes() { return lastDiscoveredIndianShowtimes; }
    public String getLastStatusMessage() { return lastStatusMessage; }

    public void clearCache() {
        cache.clear();
    }
}

