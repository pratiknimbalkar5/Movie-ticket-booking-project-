package com.moviebooking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.dto.response.TmdbMovieDto;
import com.moviebooking.dto.response.TmdbPageResponse;
import com.moviebooking.entity.Movie;
import com.moviebooking.entity.Screen;
import com.moviebooking.entity.Show;
import com.moviebooking.entity.Theatre;
import com.moviebooking.exception.ResourceNotFoundException;
import com.moviebooking.exception.TmdbApiException;
import com.moviebooking.exception.TmdbAuthException;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.ScreenRepository;
import com.moviebooking.repository.ShowRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TmdbMovieService {

    static {
        // Prevent CloudFront TLS 1.3 handshake resets during session ticket resumption on Java 25
        System.setProperty("jdk.tls.client.enableSessionTicketExtension", "false");
    }

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final ScreenRepository screenRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.tmdb.base-url:https://api.themoviedb.org/3}")
    private String baseUrl;

    @Value("${app.tmdb.access-token:}")
    private String accessToken;

    @Value("${app.tmdb.api-key:}")
    private String apiKey;

    @Value("${app.tmdb.default-region:IN}")
    private String defaultRegion;

    @Value("${app.tmdb.cache-ttl-minutes:15}")
    private int cacheTtlMinutes;

    // Thread-safe in-memory cache to prevent repeated external TMDB requests and respect rate limits
    private final Map<String, CacheEntry<TmdbPageResponse<TmdbMovieDto>>> pageCache = new ConcurrentHashMap<>();
    private final Map<Long, CacheEntry<TmdbMovieDto>> movieDetailCache = new ConcurrentHashMap<>();

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("jdk.tls.client.enableSessionTicketExtension", "false");
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");
    }

    @org.springframework.beans.factory.annotation.Autowired
    public TmdbMovieService(
            MovieRepository movieRepository,
            ShowRepository showRepository,
            ScreenRepository screenRepository,
            @Value("${app.tmdb.base-url:https://api.themoviedb.org/3}") String baseUrl,
            @Value("${app.tmdb.connect-timeout-seconds:8}") int connectTimeout,
            @Value("${app.tmdb.read-timeout-seconds:10}") int readTimeout) {

        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("jdk.tls.client.enableSessionTicketExtension", "false");
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");

        this.movieRepository = movieRepository;
        this.showRepository = showRepository;
        this.screenRepository = screenRepository;
        this.objectMapper = new ObjectMapper();

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

    @jakarta.annotation.PostConstruct
    public void postConstruct() {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("jdk.tls.client.enableSessionTicketExtension", "false");
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");

        if (!isConfigured()) {
            loadFromDotenv();
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
                            if ("TMDB_ACCESS_TOKEN".equalsIgnoreCase(k) && (accessToken == null || accessToken.isBlank())) {
                                this.accessToken = v;
                            }
                            if ("TMDB_API_KEY".equalsIgnoreCase(k) && (apiKey == null || apiKey.isBlank())) {
                                this.apiKey = v;
                            }
                        }
                    }
                    if (isConfigured()) {
                        log.info("TMDB credentials dynamically populated from {}", p);
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    // Testing constructor allowing injected RestClient
    TmdbMovieService(
            MovieRepository movieRepository,
            ShowRepository showRepository,
            ScreenRepository screenRepository,
            RestClient restClient) {
        this.movieRepository = movieRepository;
        this.showRepository = showRepository;
        this.screenRepository = screenRepository;
        this.restClient = restClient;
        this.objectMapper = new ObjectMapper();
        this.defaultRegion = "IN";
        this.cacheTtlMinutes = 15;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setDefaultRegion(String defaultRegion) {
        this.defaultRegion = defaultRegion;
    }

    /**
     * Checks if a valid TMDB Read Access Token or API Key is configured.
     */
    public boolean isConfigured() {
        return (accessToken != null && !accessToken.isBlank()) ||
               (apiKey != null && !apiKey.isBlank());
    }

    private void validateConfiguration() {
        if (!isConfigured()) {
            throw new TmdbAuthException(
                    "TMDB credentials not configured. Please set the TMDB_ACCESS_TOKEN environment variable in your terminal/environment " +
                    "(e.g. in PowerShell: $env:TMDB_ACCESS_TOKEN='your_token')."
            );
        }
    }

    /**
     * Now Playing Movies (defaults to India region: region=IN)
     * Strictly filters to movies currently running in theatres; purges old releases.
     */
    public TmdbPageResponse<TmdbMovieDto> getNowPlaying(String region, int page) {
        validateConfiguration();
        String activeRegion = (region != null && !region.isBlank()) ? region.toUpperCase() : defaultRegion;
        int safePage = Math.max(1, page);
        String cacheKey = "now_playing:" + activeRegion + ":" + safePage;

        CacheEntry<TmdbPageResponse<TmdbMovieDto>> cached = pageCache.get(cacheKey);
        if (cached != null && !cached.isExpired(cacheTtlMinutes)) {
            return cached.getData(true);
        }

        String uri = String.format("/movie/now_playing?region=%s&page=%d&language=en-US", activeRegion, safePage);
        try {
            TmdbPageResponse<TmdbMovieDto> response = executePageRequest(uri, safePage);
            LocalDate today = LocalDate.now();

            // Strict filter: Only movies currently in theatres (released in last 30 days OR with active bookable shows)
            List<TmdbMovieDto> currentMovies = response.getResults().stream()
                    .filter(m -> {
                        if (Boolean.TRUE.equals(m.getIsUpcoming())) return false;
                        if (Boolean.TRUE.equals(m.getHasBookableShows())) return true;
                        if (m.getReleaseDate() != null && !m.getReleaseDate().isBlank()) {
                            try {
                                LocalDate rd = LocalDate.parse(m.getReleaseDate().trim());
                                return !rd.isAfter(today) && rd.isAfter(today.minusDays(30));
                            } catch (Exception e) {
                                return false;
                            }
                        }
                        return false;
                    })
                    .sorted((a, b) -> {
                        boolean aShows = Boolean.TRUE.equals(a.getHasBookableShows());
                        boolean bShows = Boolean.TRUE.equals(b.getHasBookableShows());
                        if (aShows != bShows) {
                            return aShows ? -1 : 1;
                        }
                        double rA = a.getRating() != null ? a.getRating() : 0.0;
                        double rB = b.getRating() != null ? b.getRating() : 0.0;
                        return Double.compare(rB, rA);
                    })
                    .collect(Collectors.toCollection(ArrayList::new));

            // Enrich with verified live scheduled movies from local database so now-playing is rich and bookable
            List<Movie> localLive = movieRepository.findByIsActiveTrueAndIsUpcomingFalseOrderByRatingDesc();
            for (Movie lm : localLive) {
                boolean alreadyInList = currentMovies.stream().anyMatch(dto ->
                        dto.getTitle().equalsIgnoreCase(lm.getTitle()) ||
                        (dto.getId() != null && dto.getId().equals(lm.getTmdbId())));
                if (!alreadyInList) {
                    currentMovies.add(0, TmdbMovieDto.builder()
                            .id(lm.getTmdbId() != null ? lm.getTmdbId() : lm.getId())
                            .localMovieId(lm.getId())
                            .title(lm.getTitle())
                            .overview(lm.getDescription())
                            .releaseDate(lm.getReleaseDate() != null ? lm.getReleaseDate().toString() : today.toString())
                            .posterUrl(lm.getPosterUrl())
                            .rating(lm.getRating() != null ? lm.getRating() : 7.8)
                            .language(lm.getLanguage() != null ? lm.getLanguage() : "Hindi / English")
                            .genre(lm.getGenre() != null ? lm.getGenre() : "Action / Drama")
                            .isUpcoming(false)
                            .hasBookableShows(true)
                            .releaseStatus("NOW_SHOWING")
                            .build());
                }
            }

            response.setResults(currentMovies);
            pageCache.put(cacheKey, new CacheEntry<>(response));
            return response;
        } catch (TmdbAuthException authEx) {
            throw authEx;
        } catch (TmdbApiException apiEx) {
            if (cached != null) {
                log.warn("TMDB now_playing request failed ({}), returning stale cached data", apiEx.getMessage());
                return cached.getData(true);
            }
            throw apiEx;
        } catch (Exception e) {
            if (cached != null) {
                log.warn("TMDB now_playing request failed ({}), returning stale cached data", e.getMessage());
                return cached.getData(true);
            }
            throw new TmdbApiException("Failed to fetch now playing movies from TMDB: " + e.getMessage(), 502);
        }
    }

    /**
     * Upcoming Movies (defaults to India region: region=IN)
     * Strictly filters to movies releasing in the future; never shows past released movies.
     */
    public TmdbPageResponse<TmdbMovieDto> getUpcoming(String region, int page) {
        validateConfiguration();
        String activeRegion = (region != null && !region.isBlank()) ? region.toUpperCase() : defaultRegion;
        int safePage = Math.max(1, page);
        String cacheKey = "upcoming:" + activeRegion + ":" + safePage;

        CacheEntry<TmdbPageResponse<TmdbMovieDto>> cached = pageCache.get(cacheKey);
        if (cached != null && !cached.isExpired(cacheTtlMinutes)) {
            return cached.getData(true);
        }

        String uri = String.format("/movie/upcoming?region=%s&page=%d&language=en-US", activeRegion, safePage);
        try {
            TmdbPageResponse<TmdbMovieDto> response = executePageRequest(uri, safePage);
            LocalDate today = LocalDate.now();

            // Strict upcoming filter: release date MUST strictly be in the future!
            List<TmdbMovieDto> strictlyUpcoming = response.getResults().stream()
                    .filter(m -> {
                        if (m.getReleaseDate() != null && !m.getReleaseDate().isBlank()) {
                            try {
                                LocalDate rd = LocalDate.parse(m.getReleaseDate().trim());
                                return rd.isAfter(today);
                            } catch (Exception e) {
                                return false;
                            }
                        }
                        return Boolean.TRUE.equals(m.getIsUpcoming());
                    })
                    .map(m -> {
                        m.setIsUpcoming(true);
                        m.setReleaseStatus("UPCOMING");
                        return m;
                    })
                    .collect(Collectors.toCollection(ArrayList::new));

            // Enrich with database upcoming blockbusters
            List<Movie> localUpcoming = movieRepository.findByIsActiveTrueAndIsUpcomingTrueOrderByReleaseDateAsc();
            for (Movie lum : localUpcoming) {
                if (lum.getReleaseDate() != null && lum.getReleaseDate().isAfter(today)) {
                    boolean alreadyInList = strictlyUpcoming.stream().anyMatch(dto ->
                            dto.getTitle().equalsIgnoreCase(lum.getTitle()) ||
                            (dto.getId() != null && dto.getId().equals(lum.getTmdbId())));
                    if (!alreadyInList) {
                        strictlyUpcoming.add(TmdbMovieDto.builder()
                                .id(lum.getTmdbId() != null ? lum.getTmdbId() : lum.getId())
                                .localMovieId(lum.getId())
                                .title(lum.getTitle())
                                .overview(lum.getDescription())
                                .releaseDate(lum.getReleaseDate().toString())
                                .posterUrl(lum.getPosterUrl())
                                .rating(lum.getRating() != null ? lum.getRating() : 8.5)
                                .language(lum.getLanguage() != null ? lum.getLanguage() : "Hindi / English")
                                .genre(lum.getGenre() != null ? lum.getGenre() : "Action / Epic")
                                .isUpcoming(true)
                                .hasBookableShows(false)
                                .releaseStatus("UPCOMING")
                                .build());
                    }
                }
            }

            strictlyUpcoming.sort(Comparator.comparing(TmdbMovieDto::getReleaseDate, Comparator.nullsLast(String::compareTo)));
            response.setResults(strictlyUpcoming);
            response.setTotalResults(strictlyUpcoming.size());
            pageCache.put(cacheKey, new CacheEntry<>(response));
            return response;
        } catch (TmdbAuthException authEx) {
            throw authEx;
        } catch (TmdbApiException apiEx) {
            if (cached != null) {
                log.warn("TMDB upcoming request failed ({}), returning stale cached data", apiEx.getMessage());
                return cached.getData(true);
            }
            throw apiEx;
        } catch (Exception e) {
            if (cached != null) {
                log.warn("TMDB upcoming request failed ({}), returning stale cached data", e.getMessage());
                return cached.getData(true);
            }
            throw new TmdbApiException("Failed to fetch upcoming movies from TMDB: " + e.getMessage(), 502);
        }
    }

    /**
     * Latest Releases (strictly movies released theatrically within the recency window, default 30 days)
     * Sorted newest release date first.
     */
    public TmdbPageResponse<TmdbMovieDto> getLatestReleases(String region, int recencyDays, int page) {
        validateConfiguration();
        String activeRegion = (region != null && !region.isBlank()) ? region.toUpperCase() : defaultRegion;
        int safePage = Math.max(1, page);
        int safeDays = (recencyDays > 0) ? recencyDays : 30;
        String cacheKey = "latest_releases:" + activeRegion + ":" + safeDays + ":" + safePage;

        CacheEntry<TmdbPageResponse<TmdbMovieDto>> cached = pageCache.get(cacheKey);
        if (cached != null && !cached.isExpired(cacheTtlMinutes)) {
            return cached.getData(true);
        }

        String uri = String.format("/movie/now_playing?region=%s&page=%d&language=en-US", activeRegion, safePage);
        try {
            TmdbPageResponse<TmdbMovieDto> response = executePageRequest(uri, safePage);
            LocalDate today = LocalDate.now();
            LocalDate cutoff = today.minusDays(safeDays);

            List<TmdbMovieDto> latest = response.getResults().stream()
                    .filter(m -> {
                        if (Boolean.TRUE.equals(m.getIsUpcoming())) return false;
                        if (m.getReleaseDate() != null && !m.getReleaseDate().isBlank()) {
                            try {
                                LocalDate rd = LocalDate.parse(m.getReleaseDate().trim());
                                return !rd.isAfter(today) && !rd.isBefore(cutoff);
                            } catch (Exception e) {
                                return false;
                            }
                        }
                        return false;
                    })
                    .sorted((a, b) -> {
                        try {
                            LocalDate da = LocalDate.parse(a.getReleaseDate().trim());
                            LocalDate db = LocalDate.parse(b.getReleaseDate().trim());
                            return db.compareTo(da); // Newest first
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .collect(Collectors.toCollection(ArrayList::new));

            // Enrich with local recent releases
            List<Movie> localRecent = movieRepository.findByIsActiveTrueAndIsUpcomingFalseOrderByReleaseDateDesc();
            for (Movie lm : localRecent) {
                if (lm.getReleaseDate() != null && !lm.getReleaseDate().isAfter(today) && !lm.getReleaseDate().isBefore(cutoff)) {
                    boolean alreadyInList = latest.stream().anyMatch(dto ->
                            dto.getTitle().equalsIgnoreCase(lm.getTitle()) ||
                            (dto.getId() != null && dto.getId().equals(lm.getTmdbId())));
                    if (!alreadyInList) {
                        latest.add(0, TmdbMovieDto.builder()
                                .id(lm.getTmdbId() != null ? lm.getTmdbId() : lm.getId())
                                .localMovieId(lm.getId())
                                .title(lm.getTitle())
                                .overview(lm.getDescription())
                                .releaseDate(lm.getReleaseDate().toString())
                                .posterUrl(lm.getPosterUrl())
                                .rating(lm.getRating() != null ? lm.getRating() : 7.8)
                                .language(lm.getLanguage() != null ? lm.getLanguage() : "Hindi / English")
                                .genre(lm.getGenre() != null ? lm.getGenre() : "Action / Drama")
                                .isUpcoming(false)
                                .hasBookableShows(true)
                                .releaseStatus("NOW_SHOWING")
                                .build());
                    }
                }
            }

            // Sort again so newest is first
            latest.sort((a, b) -> {
                try {
                    LocalDate da = LocalDate.parse(a.getReleaseDate().trim());
                    LocalDate db = LocalDate.parse(b.getReleaseDate().trim());
                    return db.compareTo(da);
                } catch (Exception e) {
                    return 0;
                }
            });

            response.setResults(latest);
            response.setTotalResults(latest.size());
            pageCache.put(cacheKey, new CacheEntry<>(response));
            return response;
        } catch (Exception e) {
            return fallbackLatestReleasesFromLocal(safeDays);
        }
    }

    private TmdbPageResponse<TmdbMovieDto> fallbackLatestReleasesFromLocal(int safeDays) {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.minusDays(safeDays);
        List<Movie> localRecent = movieRepository.findByIsActiveTrueAndIsUpcomingFalseOrderByReleaseDateDesc();
        List<TmdbMovieDto> list = localRecent.stream()
                .filter(lm -> lm.getReleaseDate() != null && !lm.getReleaseDate().isAfter(today) && !lm.getReleaseDate().isBefore(cutoff))
                .map(lm -> TmdbMovieDto.builder()
                        .id(lm.getTmdbId() != null ? lm.getTmdbId() : lm.getId())
                        .localMovieId(lm.getId())
                        .title(lm.getTitle())
                        .overview(lm.getDescription())
                        .releaseDate(lm.getReleaseDate().toString())
                        .posterUrl(lm.getPosterUrl())
                        .rating(lm.getRating() != null ? lm.getRating() : 7.8)
                        .language(lm.getLanguage() != null ? lm.getLanguage() : "Hindi / English")
                        .genre(lm.getGenre() != null ? lm.getGenre() : "Action / Drama")
                        .isUpcoming(false)
                        .hasBookableShows(true)
                        .releaseStatus("NOW_SHOWING")
                        .build())
                .toList();

        return TmdbPageResponse.<TmdbMovieDto>builder()
                .page(1)
                .totalPages(1)
                .totalResults(list.size())
                .results(list)
                .cached(true)
                .build();
    }

    /**
     * Popular Movies
     */
    public TmdbPageResponse<TmdbMovieDto> getPopular(int page) {
        validateConfiguration();
        int safePage = Math.max(1, page);
        String cacheKey = "popular:" + safePage;

        CacheEntry<TmdbPageResponse<TmdbMovieDto>> cached = pageCache.get(cacheKey);
        if (cached != null && !cached.isExpired(cacheTtlMinutes)) {
            return cached.getData(true);
        }

        String uri = String.format("/movie/popular?page=%d&language=en-US&region=%s", safePage, defaultRegion);
        try {
            TmdbPageResponse<TmdbMovieDto> response = executePageRequest(uri, safePage);
            List<TmdbMovieDto> currentOrUpcoming = response.getResults() != null
                    ? response.getResults().stream().filter(this::isCurrentOrUpcoming).toList()
                    : Collections.emptyList();
            response.setResults(currentOrUpcoming);
            response.setTotalResults(currentOrUpcoming.size());
            pageCache.put(cacheKey, new CacheEntry<>(response));
            return response;
        } catch (TmdbAuthException authEx) {
            throw authEx;
        } catch (TmdbApiException apiEx) {
            if (cached != null) {
                log.warn("TMDB popular request failed ({}), returning stale cached data", apiEx.getMessage());
                return cached.getData(true);
            }
            throw apiEx;
        } catch (Exception e) {
            if (cached != null) {
                log.warn("TMDB popular request failed ({}), returning stale cached data", e.getMessage());
                return cached.getData(true);
            }
            throw new TmdbApiException("Failed to fetch popular movies from TMDB: " + e.getMessage(), 502);
        }
    }

    /**
     * Trending Movies (window: "day" or "week")
     */
    public TmdbPageResponse<TmdbMovieDto> getTrending(String window, int page) {
        validateConfiguration();
        String activeWindow = ("week".equalsIgnoreCase(window)) ? "week" : "day";
        int safePage = Math.max(1, page);
        String cacheKey = "trending:" + activeWindow + ":" + safePage;

        CacheEntry<TmdbPageResponse<TmdbMovieDto>> cached = pageCache.get(cacheKey);
        if (cached != null && !cached.isExpired(cacheTtlMinutes)) {
            return cached.getData(true);
        }

        String uri = String.format("/trending/movie/%s?page=%d&language=en-US", activeWindow, safePage);
        try {
            TmdbPageResponse<TmdbMovieDto> response = executePageRequest(uri, safePage);
            List<TmdbMovieDto> currentOrUpcoming = response.getResults() != null
                    ? response.getResults().stream().filter(this::isCurrentOrUpcoming).toList()
                    : Collections.emptyList();
            response.setResults(currentOrUpcoming);
            response.setTotalResults(currentOrUpcoming.size());
            pageCache.put(cacheKey, new CacheEntry<>(response));
            return response;
        } catch (TmdbAuthException authEx) {
            throw authEx;
        } catch (TmdbApiException apiEx) {
            if (cached != null) {
                log.warn("TMDB trending request failed ({}), returning stale cached data", apiEx.getMessage());
                return cached.getData(true);
            }
            throw apiEx;
        } catch (Exception e) {
            if (cached != null) {
                log.warn("TMDB trending request failed ({}), returning stale cached data", e.getMessage());
                return cached.getData(true);
            }
            throw new TmdbApiException("Failed to fetch trending movies from TMDB: " + e.getMessage(), 502);
        }
    }

    /**
     * Search Movies by title or keyword
     */
    public TmdbPageResponse<TmdbMovieDto> searchMovies(String query, int page) {
        validateConfiguration();
        if (query == null || query.isBlank()) {
            return TmdbPageResponse.<TmdbMovieDto>builder()
                    .page(1)
                    .totalPages(0)
                    .totalResults(0)
                    .results(Collections.emptyList())
                    .cached(false)
                    .build();
        }

        int safePage = Math.max(1, page);
        String encodedQuery = java.net.URLEncoder.encode(query.trim(), java.nio.charset.StandardCharsets.UTF_8);
        String cacheKey = "search:" + encodedQuery.toLowerCase() + ":" + safePage;

        CacheEntry<TmdbPageResponse<TmdbMovieDto>> cached = pageCache.get(cacheKey);
        if (cached != null && !cached.isExpired(cacheTtlMinutes)) {
            return cached.getData(true);
        }

        String uri = String.format("/search/movie?query=%s&page=%d&language=en-US&include_adult=false", encodedQuery, safePage);
        try {
            TmdbPageResponse<TmdbMovieDto> response = executePageRequest(uri, safePage);

            // Filter to ONLY current releases and upcoming movies — exclude old catalog movies
            List<TmdbMovieDto> filtered = response.getResults() == null ? Collections.emptyList() :
                    response.getResults().stream()
                            .filter(this::isCurrentOrUpcoming)
                            .toList();

            // If TMDB returns no current/upcoming movies, search the curated local database
            if (filtered.isEmpty()) {
                TmdbPageResponse<TmdbMovieDto> localFallback = fallbackSearchFromLocal(query, safePage);
                if (localFallback.getResults() != null && !localFallback.getResults().isEmpty()) {
                    pageCache.put(cacheKey, new CacheEntry<>(localFallback));
                    return localFallback;
                }
            }

            response.setResults(filtered);
            response.setTotalResults(filtered.size());
            response.setTotalPages(Math.max(1, (int) Math.ceil(filtered.size() / 20.0)));
            pageCache.put(cacheKey, new CacheEntry<>(response));
            return response;
        } catch (TmdbAuthException authEx) {
            throw authEx;
        } catch (TmdbApiException apiEx) {
            if (apiEx.getStatusCode() >= 400 && apiEx.getStatusCode() < 500) {
                throw apiEx;
            }
            if (cached != null) {
                log.warn("TMDB search request failed ({}), returning stale cached data", apiEx.getMessage());
                return cached.getData(true);
            }
            log.warn("TMDB search request failed ({}), falling back to local database search", apiEx.getMessage());
            return fallbackSearchFromLocal(query, safePage);
        } catch (Exception e) {
            if (cached != null) {
                log.warn("TMDB search request failed ({}), returning stale cached data", e.getMessage());
                return cached.getData(true);
            }
            log.warn("TMDB search request failed ({}), falling back to local database search", e.getMessage());
            return fallbackSearchFromLocal(query, safePage);
        }
    }

    /**
     * Get Movie details by TMDB ID
     */
    public TmdbMovieDto getMovieDetails(Long tmdbId) {
        validateConfiguration();
        if (tmdbId == null) {
            throw new ResourceNotFoundException("Movie ID cannot be null");
        }

        CacheEntry<TmdbMovieDto> cached = movieDetailCache.get(tmdbId);
        if (cached != null && !cached.isExpired(cacheTtlMinutes)) {
            return cached.getData();
        }

        String uri = "/movie/" + tmdbId + "?language=en-US&append_to_response=credits";
        try {
            String jsonResponse = executeRawGet(uri);
            JsonNode root = objectMapper.readTree(jsonResponse);
            TmdbMovieDto dto = mapJsonToDto(root);

            // Cross-reference with local database
            movieRepository.findByTmdbId(tmdbId).ifPresent(local -> dto.setLocalMovieId(local.getId()));

            movieDetailCache.put(tmdbId, new CacheEntry<>(dto));
            return dto;
        } catch (Exception e) {
            if (cached != null) {
                return cached.getData();
            }
            throw new TmdbApiException("Failed to fetch movie details for TMDB ID " + tmdbId + ": " + e.getMessage());
        }
    }

    /**
     * Safe synchronization: Link or create local database Movie entity from TMDB ID.
     * Guarantees zero duplicate rows and preserves existing booking entity relationships.
     */
    @Transactional
    public Movie getOrCreateLocalMovie(Long tmdbId) {
        if (tmdbId == null) {
            throw new IllegalArgumentException("TMDB ID cannot be null");
        }

        // 1. Check if already linked by tmdbId
        Optional<Movie> existingByTmdb = movieRepository.findByTmdbId(tmdbId);
        if (existingByTmdb.isPresent()) {
            return existingByTmdb.get();
        }

        // 2. Fetch live metadata from TMDB
        TmdbMovieDto tmdbDto = getMovieDetails(tmdbId);

        // 3. Check if matching by title exists
        Optional<Movie> existingByTitle = movieRepository.findFirstByTitleIgnoreCase(tmdbDto.getTitle().trim());
        if (existingByTitle.isPresent()) {
            Movie movie = existingByTitle.get();
            movie.setTmdbId(tmdbId);
            if (tmdbDto.getPosterUrl() != null) movie.setPosterUrl(tmdbDto.getPosterUrl());
            if (tmdbDto.getRating() != null && tmdbDto.getRating() > 0) movie.setRating(tmdbDto.getRating());
            if (tmdbDto.getOverview() != null && !tmdbDto.getOverview().isBlank()) movie.setDescription(tmdbDto.getOverview());
            return movieRepository.save(movie);
        }

        // 4. Create new local Movie row
        LocalDate releaseDate = null;
        if (tmdbDto.getReleaseDate() != null && !tmdbDto.getReleaseDate().isBlank()) {
            try {
                releaseDate = LocalDate.parse(tmdbDto.getReleaseDate());
            } catch (Exception ignored) {}
        }

        Movie newMovie = Movie.builder()
                .title(tmdbDto.getTitle())
                .tmdbId(tmdbId)
                .description((tmdbDto.getOverview() != null && !tmdbDto.getOverview().isBlank())
                        ? tmdbDto.getOverview()
                        : "Live screening in premier Indian residential cinemas.")
                .genre(tmdbDto.getGenre() != null ? tmdbDto.getGenre() : "Action / Drama")
                .durationMinutes(125)
                .language(tmdbDto.getLanguage() != null ? tmdbDto.getLanguage() : "Hindi / English")
                .releaseDate(releaseDate != null ? releaseDate : LocalDate.now())
                .posterUrl(tmdbDto.getPosterUrl())
                .rating(tmdbDto.getRating() != null ? tmdbDto.getRating() : 7.5)
                .isUpcoming(Boolean.TRUE.equals(tmdbDto.getIsUpcoming()))
                .isActive(true)
                .build();

        Movie savedMovie = movieRepository.save(newMovie);

        // Schedule shows across existing screens so user can book immediately
        scheduleShowsForMovie(savedMovie);

        return savedMovie;
    }

    private void scheduleShowsForMovie(Movie movie) {
        List<Screen> allScreens = screenRepository.findAll();
        if (allScreens.isEmpty()) return;
        List<Screen> targetScreens = allScreens.stream().limit(30).toList();

        LocalDate today = LocalDate.now();
        List<Show> newShows = new ArrayList<>();

        for (int dayOffset = 0; dayOffset < 4; dayOffset++) {
            LocalDate date = today.plusDays(dayOffset);
            for (Screen screen : targetScreens) {
                Theatre theatre = screen.getTheatre();
                for (int slot = 0; slot < 2; slot++) {
                    int slotIndex = (slot == 0) ? 1 : 3; // matinee & prime evening
                    LocalDateTime showTime = com.moviebooking.util.ShowPricingAndScheduleHelper.calculateShowTime(date, dayOffset, slotIndex, theatre, screen);
                    var pricing = com.moviebooking.util.ShowPricingAndScheduleHelper.calculatePricing(theatre, screen, slotIndex);

                    newShows.add(Show.builder()
                            .movie(movie)
                            .screen(screen)
                            .showTime(showTime)
                            .basePrice(pricing.basePrice())
                            .premiumPrice(pricing.premiumPrice())
                            .vipPrice(pricing.vipPrice())
                            .status(Show.ShowStatus.SCHEDULED)
                            .build());
                }
            }
        }
        showRepository.saveAll(newShows);
    }

    /**
     * Executes GET request to TMDB and builds paginated response DTO.
     */
    private TmdbPageResponse<TmdbMovieDto> executePageRequest(String uri, int requestedPage) {
        String jsonResponse = executeRawGet(uri);
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            int page = root.path("page").asInt(requestedPage);
            int totalPages = root.path("total_pages").asInt(1);
            int totalResults = root.path("total_results").asInt(0);

            List<TmdbMovieDto> dtoList = new ArrayList<>();
            JsonNode results = root.path("results");
            if (results.isArray()) {
                for (JsonNode m : results) {
                    TmdbMovieDto dto = mapJsonToDto(m);
                    // Match with local database if already saved
                    movieRepository.findByTmdbId(dto.getId()).ifPresent(loc -> dto.setLocalMovieId(loc.getId()));
                    dtoList.add(dto);
                }
            }

            return TmdbPageResponse.<TmdbMovieDto>builder()
                    .page(page)
                    .totalPages(totalPages)
                    .totalResults(totalResults)
                    .results(dtoList)
                    .cached(false)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse TMDB JSON response: {}", e.getMessage());
            throw new TmdbApiException("Failed to parse movie data from TMDB: " + e.getMessage());
        }
    }

    /**
     * Low-level HTTP call with authentication header (Bearer token or api_key param).
     */
    private String executeRawGet(String pathAndQuery) {
        String finalUri = pathAndQuery;
        boolean hasBearer = (accessToken != null && !accessToken.isBlank());
        boolean hasApiKey = (apiKey != null && !apiKey.isBlank());

        if (hasApiKey) {
            String separator = finalUri.contains("?") ? "&" : "?";
            finalUri = finalUri + separator + "api_key=" + apiKey.trim();
        }

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                RestClient.RequestHeadersSpec<?> requestSpec = restClient.get().uri(finalUri);

                if (hasBearer) {
                    requestSpec = requestSpec.header("Authorization", "Bearer " + accessToken.trim());
                }

                return requestSpec
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                            int code = resp.getStatusCode().value();
                            if (code == 401 || code == 403) {
                                throw new TmdbAuthException("Invalid TMDB credentials (HTTP " + code + "). Please verify your TMDB_ACCESS_TOKEN.");
                            } else if (code == 429) {
                                throw new TmdbApiException("TMDB API rate limit exceeded (HTTP 429). Please wait a moment.", 429);
                            } else {
                                throw new TmdbApiException("TMDB API client error (HTTP " + code + ")", code);
                            }
                        })
                        .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                            int code = resp.getStatusCode().value();
                            throw new TmdbApiException("TMDB API server error (HTTP " + code + ")", code);
                        })
                        .body(String.class);

            } catch (TmdbAuthException authEx) {
                // Do not retry authorization failures
                throw authEx;
            } catch (TmdbApiException apiEx) {
                if (apiEx.getStatusCode() >= 400 && apiEx.getStatusCode() < 500) {
                    throw apiEx;
                }
                if (attempt == maxRetries) {
                    throw apiEx;
                }
                log.warn("TMDB request attempt {}/{} failed with API error: {}. Retrying...", attempt, maxRetries, apiEx.getMessage());
            } catch (Exception e) {
                // Fallback attempt: If Bearer token header caused CloudFront to request TLS renegotiation and reset,
                // and apiKey is present, execute via standard query parameter which succeeds cleanly.
                boolean isTlsOrReset = false;
                Throwable cause = e;
                while (cause != null) {
                    if (cause instanceof javax.net.ssl.SSLHandshakeException ||
                        cause instanceof java.net.SocketException ||
                        (cause.getMessage() != null && cause.getMessage().contains("handshake"))) {
                        isTlsOrReset = true;
                        break;
                    }
                    cause = cause.getCause();
                }

                if (isTlsOrReset && hasApiKey) {
                    try {
                        log.warn("CloudFront renegotiation/reset on Bearer header. Executing with api_key query param for TMDB...");
                        String fallbackUri = pathAndQuery;
                        String sep = fallbackUri.contains("?") ? "&" : "?";
                        fallbackUri = fallbackUri + sep + "api_key=" + apiKey.trim();

                        return restClient.get()
                                .uri(fallbackUri)
                                .retrieve()
                                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                                    int code = resp.getStatusCode().value();
                                    throw new TmdbApiException("TMDB API client error (HTTP " + code + ")", code);
                                })
                                .body(String.class);
                    } catch (Exception fallbackEx) {
                        log.warn("Query param fallback attempt also failed: {}", fallbackEx.getMessage());
                    }
                }

                if (attempt == maxRetries) {
                    if (isTlsOrReset) {
                        log.error("TMDB TLS/SSL handshake failure: {}", e.getMessage());
                        throw new TmdbApiException("TMDB TLS handshake failure: " + e.getMessage() + ". Handshake with api.themoviedb.org failed.", 502);
                    }
                    log.error("Network error communicating with TMDB API after {} attempts: {}", maxRetries, e.getMessage());
                    throw new TmdbApiException("Network error connecting to TMDB API: " + e.getMessage(), 504);
                }
                log.warn("TMDB request attempt {}/{} network glitch: {}. Retrying...", attempt, maxRetries, e.getMessage());
            }

            try {
                Thread.sleep(150L * attempt);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new TmdbApiException("Request interrupted while retrying TMDB", 500);
            }
        }
        throw new TmdbApiException("Failed to communicate with TMDB API after retries", 504);
    }

    private TmdbPageResponse<TmdbMovieDto> fallbackSearchFromLocal(String query, int page) {
        Page<Movie> localPage = movieRepository.searchMovies(query, null, PageRequest.of(Math.max(0, page - 1), 20));
        List<Movie> localMovies = (localPage != null && localPage.getContent() != null) ? localPage.getContent() : Collections.emptyList();
        List<TmdbMovieDto> list = localMovies.stream()
                .map(m -> TmdbMovieDto.builder()
                        .id(m.getTmdbId() != null ? m.getTmdbId() : m.getId())
                        .localMovieId(m.getId())
                        .title(m.getTitle())
                        .overview(m.getDescription())
                        .releaseDate(m.getReleaseDate() != null ? m.getReleaseDate().toString() : "")
                        .posterUrl(m.getPosterUrl())
                        .rating(m.getRating() != null ? m.getRating() : 7.5)
                        .language(m.getLanguage() != null ? m.getLanguage() : "Hindi / English")
                        .genre(m.getGenre() != null ? m.getGenre() : "Action / Drama")
                        .isUpcoming(Boolean.TRUE.equals(m.getIsUpcoming()))
                        .build())
                .filter(this::isCurrentOrUpcoming)
                .toList();

        return TmdbPageResponse.<TmdbMovieDto>builder()
                .page(page)
                .totalPages(Math.max(1, (int) Math.ceil(list.size() / 20.0)))
                .totalResults(list.size())
                .results(list)
                .cached(true)
                .build();
    }

    public boolean isCurrentOrUpcoming(TmdbMovieDto dto) {
        if (Boolean.TRUE.equals(dto.getIsUpcoming())) {
            return true;
        }
        if (Boolean.TRUE.equals(dto.getHasBookableShows())) {
            return true;
        }
        if (dto.getReleaseDate() != null && !dto.getReleaseDate().isBlank()) {
            try {
                LocalDate releaseDate = LocalDate.parse(dto.getReleaseDate().trim());
                LocalDate today = LocalDate.now();
                if (releaseDate.isAfter(today)) {
                    dto.setIsUpcoming(true);
                    return true;
                }
                // Strictly movies currently running in theatres (released in last 30 days)
                return releaseDate.isAfter(today.minusDays(30));
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private TmdbPageResponse<TmdbMovieDto> fallbackLocalCatalog(int page, boolean upcomingOnly) {
        List<Movie> localMovies = upcomingOnly
                ? movieRepository.findByIsActiveTrueAndIsUpcomingTrueOrderByReleaseDateAsc()
                : movieRepository.findByIsActiveTrueAndIsUpcomingFalseOrderByRatingDesc();

        List<TmdbMovieDto> list = localMovies.stream()
                .map(m -> TmdbMovieDto.builder()
                        .id(m.getTmdbId() != null ? m.getTmdbId() : m.getId())
                        .localMovieId(m.getId())
                        .title(m.getTitle())
                        .overview(m.getDescription())
                        .releaseDate(m.getReleaseDate() != null ? m.getReleaseDate().toString() : "")
                        .posterUrl(m.getPosterUrl())
                        .rating(m.getRating() != null ? m.getRating() : 7.5)
                        .language(m.getLanguage() != null ? m.getLanguage() : "Hindi / English")
                        .genre(m.getGenre() != null ? m.getGenre() : "Action / Drama")
                        .isUpcoming(Boolean.TRUE.equals(m.getIsUpcoming()))
                        .build())
                .toList();

        return TmdbPageResponse.<TmdbMovieDto>builder()
                .page(1)
                .totalPages(1)
                .totalResults(list.size())
                .results(list)
                .cached(true)
                .build();
    }

    private TmdbMovieDto mapJsonToDto(JsonNode m) {
        Long id = m.path("id").asLong();
        String title = m.path("title").asText("");
        String overview = m.path("overview").asText("");
        String releaseDateStr = m.path("release_date").asText("");

        String posterPath = m.path("poster_path").asText(null);
        String posterUrl = (posterPath != null && !posterPath.isBlank() && !posterPath.equals("null"))
                ? "https://image.tmdb.org/t/p/w500" + posterPath
                : null;

        String backdropPath = m.path("backdrop_path").asText(null);
        String backdropUrl = (backdropPath != null && !backdropPath.isBlank() && !backdropPath.equals("null"))
                ? "https://image.tmdb.org/t/p/w500" + backdropPath
                : null;

        double voteAvg = m.path("vote_average").asDouble(0.0);
        double rating = Math.round(voteAvg * 10.0) / 10.0;
        int voteCount = m.path("vote_count").asInt(0);

        String langCode = m.path("original_language").asText("en");
        String language = formatLanguage(langCode);

        String genre = "Action / Drama";
        if (m.has("genres") && m.path("genres").isArray() && m.path("genres").size() > 0) {
            List<String> genreNames = new ArrayList<>();
            for (JsonNode g : m.path("genres")) {
                genreNames.add(g.path("name").asText());
            }
            genre = String.join(" / ", genreNames);
        } else if (m.has("genre_ids") && m.path("genre_ids").isArray() && m.path("genre_ids").size() > 0) {
            List<Integer> ids = new ArrayList<>();
            for (JsonNode gid : m.path("genre_ids")) {
                ids.add(gid.asInt());
            }
            genre = determineGenre(ids);
        }

        LocalDate releaseDate = null;
        if (!releaseDateStr.isBlank()) {
            try {
                releaseDate = LocalDate.parse(releaseDateStr);
            } catch (Exception ignored) {}
        }
        boolean isUpcoming = (releaseDate != null) && releaseDate.isAfter(LocalDate.now());

        boolean hasShows = false;
        try {
            if (showRepository != null) {
                hasShows = showRepository.hasActiveBookableShowsByMovieOrTitle(id, title, LocalDateTime.now().minusMinutes(30));
            }
        } catch (Exception ignored) {}

        String releaseStatus = isUpcoming ? "UPCOMING" : (hasShows ? "NOW_SHOWING" : "RELEASED");

        List<com.moviebooking.dto.response.CastMemberDto> castList = new ArrayList<>();
        JsonNode creditsNode = m.has("credits") ? m.path("credits") : null;
        if (creditsNode != null && creditsNode.has("cast") && creditsNode.path("cast").isArray()) {
            JsonNode castArray = creditsNode.path("cast");
            int limit = Math.min(12, castArray.size());
            for (int i = 0; i < limit; i++) {
                JsonNode c = castArray.get(i);
                String profilePath = c.path("profile_path").asText(null);
                String profileUrl = (profilePath != null && !profilePath.isBlank() && !profilePath.equals("null"))
                        ? "https://image.tmdb.org/t/p/w185" + profilePath
                        : null;
                castList.add(com.moviebooking.dto.response.CastMemberDto.builder()
                        .id(c.path("id").asLong())
                        .name(c.path("name").asText(""))
                        .character(c.path("character").asText(""))
                        .profileUrl(profileUrl)
                        .order(c.path("order").asInt(i))
                        .build());
            }
        }

        return TmdbMovieDto.builder()
                .id(id)
                .title(title)
                .overview(overview)
                .releaseDate(releaseDateStr)
                .posterPath(posterPath)
                .posterUrl(posterUrl)
                .backdropPath(backdropPath)
                .backdropUrl(backdropUrl)
                .rating(rating)
                .voteCount(voteCount)
                .language(language)
                .genre(genre)
                .isUpcoming(isUpcoming)
                .hasBookableShows(hasShows)
                .releaseStatus(releaseStatus)
                .cast(castList)
                .build();
    }

    /**
     * Get Movie Cast with actor names, character roles, and profile photo URLs
     */
    public List<com.moviebooking.dto.response.CastMemberDto> getMovieCast(Long tmdbId) {
        if (tmdbId == null) return Collections.emptyList();
        try {
            TmdbMovieDto details = getMovieDetails(tmdbId);
            return (details != null && details.getCast() != null) ? details.getCast() : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch cast for movie TMDB ID {}: {}", tmdbId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String formatLanguage(String code) {
        if (code == null || code.isBlank()) return "Hindi / English";
        return switch (code.toLowerCase()) {
            case "hi" -> "Hindi";
            case "en" -> "English";
            case "te" -> "Telugu";
            case "ta" -> "Tamil";
            case "ml" -> "Malayalam";
            case "kn" -> "Kannada";
            case "mr" -> "Marathi";
            case "bn" -> "Bengali";
            case "gu" -> "Gujarati";
            case "pa" -> "Punjabi";
            case "ko" -> "Korean";
            case "ja" -> "Japanese";
            case "es" -> "Spanish";
            case "fr" -> "French";
            default -> code.toUpperCase();
        };
    }

    private String determineGenre(List<Integer> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) return "Action / Drama";
        int first = genreIds.get(0);
        return switch (first) {
            case 28 -> "Action";
            case 12 -> "Adventure";
            case 16 -> "Animation";
            case 35 -> "Comedy";
            case 80 -> "Crime";
            case 18 -> "Drama";
            case 14 -> "Fantasy";
            case 27 -> "Horror";
            case 9648 -> "Mystery";
            case 878 -> "Sci-Fi";
            case 53 -> "Thriller";
            default -> "Drama / Thriller";
        };
    }

    /**
     * Backward-compatible live sync: pulls now playing & upcoming from TMDB and ensures local records
     */
    public Map<String, Object> syncLiveMoviesFromImdb() {
        if (!isConfigured()) {
            return Map.of("status", "skipped", "message", "TMDB_ACCESS_TOKEN not configured");
        }
        int synced = 0;
        try {
            var nowPlaying = getNowPlaying("IN", 1);
            if (nowPlaying != null && nowPlaying.getResults() != null) {
                for (var item : nowPlaying.getResults()) {
                    getOrCreateLocalMovie(item.getId());
                    synced++;
                }
            }
            var upcoming = getUpcoming("IN", 1);
            if (upcoming != null && upcoming.getResults() != null) {
                for (var item : upcoming.getResults()) {
                    getOrCreateLocalMovie(item.getId());
                    synced++;
                }
            }
        } catch (Exception e) {
            log.warn("syncLiveMovies encountered notice: {}", e.getMessage());
        }
        return Map.of("status", "success", "totalSynced", synced);
    }

    /**
     * Backward-compatible realtime search
     */
    public List<Map<String, Object>> searchRealtimeMovies(String query) {
        if (!isConfigured() || query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        try {
            var page = searchMovies(query, 1);
            List<Map<String, Object>> list = new ArrayList<>();
            if (page != null && page.getResults() != null) {
                for (var item : page.getResults()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", item.getId());
                    m.put("title", item.getTitle());
                    m.put("posterUrl", item.getPosterUrl());
                    m.put("rating", item.getRating());
                    m.put("releaseDate", item.getReleaseDate());
                    m.put("overview", item.getOverview());
                    m.put("language", item.getLanguage());
                    list.add(m);
                }
            }
            return list;
        } catch (Exception e) {
            log.warn("searchRealtimeMovies failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Cache Entry helper with TTL
     */
    private static class CacheEntry<T> {
        private final T data;
        private final long createdAt;

        public CacheEntry(T data) {
            this.data = data;
            this.createdAt = System.currentTimeMillis();
        }

        public boolean isExpired(int ttlMinutes) {
            long maxAgeMs = ttlMinutes * 60L * 1000L;
            return (System.currentTimeMillis() - createdAt) > maxAgeMs;
        }

        public T getData() {
            return data;
        }

        @SuppressWarnings("unchecked")
        public T getData(boolean markCached) {
            if (markCached && data instanceof TmdbPageResponse<?> resp) {
                ((TmdbPageResponse<Object>) resp).setCached(true);
            }
            return data;
        }
    }
}
