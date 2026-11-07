package com.moviebooking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.dto.response.CinemaDiscoveryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service integrating the Open Cinema Platform provider.
 * <p>
 * Provider Architecture Analysis:
 * - The Open Cinema Platform is an open standard and aggregator for independent, repertory, and arthouse cinemas.
 * - Authentication: Authenticates via OPEN_CINEMA_API_KEY environment variable. Supports standard HTTP Bearer token
 *   or X-API-Key header.
 * - Regional Coverage Analysis: Independent/repertory community cinemas in North America and Europe with emerging
 *   listings in India. When queries yield no external listings or credentials are unset, the system safely falls back
 *   to local verified cinema records.
 * - Showtimes & Pricing Analysis: The provider aggregates show dates, film titles, and ticketing deep links;
 *   seat layout reservation and atomic booking transactions are managed by our local application's verified inventory.
 */
@Service
@Slf4j
public class OpenCinemaService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.opencinema.api-key:${OPEN_CINEMA_API_KEY:}}")
    private String apiKey;

    @Value("${app.opencinema.base-url:https://api.opencinemaproject.com/v1}")
    private String baseUrl;

    @Value("${app.opencinema.connect-timeout-seconds:8}")
    private int connectTimeoutSeconds;

    @Value("${app.opencinema.read-timeout-seconds:10}")
    private int readTimeoutSeconds;

    // Cache to respect provider rate limits (key: "lat:lon:radius" or "city")
    private final Map<String, CacheEntry<List<CinemaDiscoveryDto>>> cinemaCache = new ConcurrentHashMap<>();

    @Autowired
    public OpenCinemaService(
            @Value("${app.opencinema.base-url:https://api.opencinemaproject.com/v1}") String baseUrl,
            @Value("${app.opencinema.connect-timeout-seconds:8}") int connectTimeout,
            @Value("${app.opencinema.read-timeout-seconds:10}") int readTimeout) {

        this.objectMapper = new ObjectMapper();
        this.baseUrl = baseUrl;
        this.connectTimeoutSeconds = connectTimeout;
        this.readTimeoutSeconds = readTimeout;

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
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "CinemaPulse-App/2.0 (OpenCinemaIntegration)")
                .build();
    }

    // Constructor for testing / mock injection
    OpenCinemaService(RestClient restClient, String apiKey, String baseUrl) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.objectMapper = new ObjectMapper();
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        if (!isConfigured()) {
            loadFromDotenv();
        }
        if (isConfigured()) {
            log.info("Open Cinema Platform provider successfully configured via OPEN_CINEMA_API_KEY (Base URL: {})", baseUrl);
        } else {
            log.info("OPEN_CINEMA_API_KEY is not set. Open Cinema Platform provider is inactive; falling back to local database / places provider.");
        }
    }

    private void loadFromDotenv() {
        java.nio.file.Path[] paths = new java.nio.file.Path[]{
                java.nio.file.Paths.get(".env"),
                java.nio.file.Paths.get("backend/.env"),
                java.nio.file.Paths.get("../.env")
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
                            if ("OPEN_CINEMA_API_KEY".equalsIgnoreCase(k) && !v.isBlank()) {
                                this.apiKey = v;
                                log.info("Loaded OPEN_CINEMA_API_KEY from {}", p);
                                return;
                            }
                        }
                    }
                } catch (Exception ignored) {}
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
     * Discover cinemas from Open Cinema Platform by coordinates and radius.
     */
    public List<CinemaDiscoveryDto> discoverCinemas(double latitude, double longitude, double radiusKm) {
        if (!isConfigured()) {
            return Collections.emptyList();
        }

        String cacheKey = String.format(Locale.US, "coord:%.3f:%.3f:%.1f", latitude, longitude, radiusKm);
        CacheEntry<List<CinemaDiscoveryDto>> cached = cinemaCache.get(cacheKey);
        if (cached != null && !cached.isExpired(30)) {
            return cached.getData();
        }

        String endpoint = String.format(Locale.US, "/cinemas/nearby?lat=%.4f&lon=%.4f&radius_km=%.1f", latitude, longitude, radiusKm);
        List<CinemaDiscoveryDto> result = executeCinemaQuery(endpoint, latitude, longitude);
        cinemaCache.put(cacheKey, new CacheEntry<>(result));
        return result;
    }

    /**
     * Discover cinemas from Open Cinema Platform by city name.
     */
    public List<CinemaDiscoveryDto> discoverCinemasByCity(String city, double fallbackLat, double fallbackLon) {
        if (!isConfigured() || city == null || city.isBlank()) {
            return Collections.emptyList();
        }

        String cacheKey = "city:" + city.trim().toLowerCase();
        CacheEntry<List<CinemaDiscoveryDto>> cached = cinemaCache.get(cacheKey);
        if (cached != null && !cached.isExpired(30)) {
            return cached.getData();
        }

        String endpoint = "/cinemas?city=" + java.net.URLEncoder.encode(city.trim(), java.nio.charset.StandardCharsets.UTF_8);
        List<CinemaDiscoveryDto> result = executeCinemaQuery(endpoint, fallbackLat, fallbackLon);
        cinemaCache.put(cacheKey, new CacheEntry<>(result));
        return result;
    }

    private List<CinemaDiscoveryDto> executeCinemaQuery(String pathAndQuery, double centerLat, double centerLon) {
        try {
            log.debug("Calling Open Cinema Platform endpoint: {}", pathAndQuery);

            String json = restClient.get()
                    .uri(pathAndQuery)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
                    .header("X-API-Key", apiKey.trim())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                        int code = resp.getStatusCode().value();
                        log.warn("Open Cinema Platform returned client status {}: {}", code, resp.getStatusText());
                        throw new RuntimeException("Open Cinema client error HTTP " + code);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        int code = resp.getStatusCode().value();
                        log.warn("Open Cinema Platform server error HTTP {}", code);
                        throw new RuntimeException("Open Cinema server error HTTP " + code);
                    })
                    .body(String.class);

            return parseCinemaList(json, centerLat, centerLon);
        } catch (Exception e) {
            log.warn("Open Cinema query failed ({})", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Parses standard Open Cinema Platform JSON payload.
     * Maps id, name, address, coordinates, website/source url, and formats.
     */
    public List<CinemaDiscoveryDto> parseCinemaList(String json, double centerLat, double centerLon) {
        if (json == null || json.isBlank()) return Collections.emptyList();

        List<CinemaDiscoveryDto> list = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode cinemasNode = root.has("cinemas") ? root.get("cinemas")
                    : root.has("data") ? root.get("data")
                    : root.isArray() ? root : null;

            if (cinemasNode != null && cinemasNode.isArray()) {
                for (JsonNode c : cinemasNode) {
                    String id = c.path("id").asText(c.path("cinema_id").asText(UUID.randomUUID().toString()));
                    String name = c.path("name").asText(c.path("theater_name").asText("Independent Cinema"));
                    String address = c.path("address").asText(c.path("theater_address").asText(""));
                    String city = c.path("city").asText("");
                    double lat = c.path("latitude").asDouble(c.path("lat").asDouble(centerLat));
                    double lon = c.path("longitude").asDouble(c.path("lon").asDouble(centerLon));
                    String purchaseUrl = c.path("purchase_url").asText(c.path("website").asText(c.path("url").asText("")));

                    String mapsUri = (lat != 0.0 && lon != 0.0)
                            ? "https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lon
                            : purchaseUrl;

                    double dist = calculateHaversineKm(centerLat, centerLon, lat, lon);

                    list.add(CinemaDiscoveryDto.builder()
                            .id("opencinema-" + id)
                            .name(name)
                            .address(address)
                            .city(city)
                            .latitude(lat)
                            .longitude(lon)
                            .distanceKm(Math.round(dist * 10.0) / 10.0)
                            .googleMapsUri(mapsUri)
                            .externalSourceUri(purchaseUrl.isBlank() ? "https://opencinemaproject.com" : purchaseUrl)
                            .sourceProvider("OPEN_CINEMA_PLATFORM")
                            .rating(c.path("rating").asDouble(4.3))
                            .userRatingCount(c.path("review_count").asInt(120))
                            .isRegistered(false)
                            .theatreId(null)
                            .hasBookableShows(false)
                            .cinemaType("EXTERNAL_DISCOVERY")
                            .shows(Collections.emptyList())
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Open Cinema JSON: {}", e.getMessage());
        }
        return list;
    }

    private double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        if (lat1 == 0.0 || lon1 == 0.0 || lat2 == 0.0 || lon2 == 0.0) return 0.0;
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private static class CacheEntry<T> {
        private final T data;
        private final long timestamp;

        public CacheEntry(T data) {
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public T getData() {
            return data;
        }

        public boolean isExpired(int ttlMinutes) {
            return System.currentTimeMillis() - timestamp > (ttlMinutes * 60L * 1000L);
        }
    }
}
