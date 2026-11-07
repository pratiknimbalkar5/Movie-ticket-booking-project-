package com.moviebooking.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviebooking.dto.response.CinemaDiscoveryDto;
import com.moviebooking.dto.response.CinemaSearchResponse;
import com.moviebooking.dto.response.NearbyTheatreShowsResponse;
import com.moviebooking.entity.Show;
import com.moviebooking.entity.Theatre;
import com.moviebooking.repository.BookingSeatRepository;
import com.moviebooking.repository.ShowRepository;
import com.moviebooking.repository.TheatreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GooglePlacesService {

    private final TheatreRepository theatreRepository;
    private final ShowRepository showRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final TheatreService theatreService;
    private final OpenCinemaService openCinemaService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.google.places.api-key:}")
    private String apiKey;

    @Value("${app.google.places.base-url:https://places.googleapis.com/v1}")
    private String baseUrl;

    @Value("${app.google.places.default-radius-km:15.0}")
    private double defaultRadiusKm;

    // Cache of discovered places by lat:lon:radius to respect Google API rate limits & quotas
    private final Map<String, CacheEntry<List<CinemaDiscoveryDto>>> placesCache = new ConcurrentHashMap<>();

    // Real, verified coordinates for major Indian cities
    private static final Map<String, Coordinates> CITY_COORDINATES = new HashMap<>();

    static {
        CITY_COORDINATES.put("mumbai", new Coordinates(19.0760, 72.8777, "Mumbai"));
        CITY_COORDINATES.put("delhi", new Coordinates(28.6139, 77.2090, "Delhi"));
        CITY_COORDINATES.put("delhi ncr", new Coordinates(28.6139, 77.2090, "Delhi NCR"));
        CITY_COORDINATES.put("noida", new Coordinates(28.5355, 77.3910, "Noida"));
        CITY_COORDINATES.put("gurugram", new Coordinates(28.4595, 77.0266, "Gurugram"));
        CITY_COORDINATES.put("bengaluru", new Coordinates(12.9716, 77.5946, "Bengaluru"));
        CITY_COORDINATES.put("bangalore", new Coordinates(12.9716, 77.5946, "Bengaluru"));
        CITY_COORDINATES.put("hyderabad", new Coordinates(17.3850, 78.4867, "Hyderabad"));
        CITY_COORDINATES.put("pune", new Coordinates(18.5204, 73.8567, "Pune"));
        CITY_COORDINATES.put("chennai", new Coordinates(13.0827, 80.2707, "Chennai"));
        CITY_COORDINATES.put("kolkata", new Coordinates(22.5726, 88.3639, "Kolkata"));
        CITY_COORDINATES.put("ahmedabad", new Coordinates(23.0225, 72.5714, "Ahmedabad"));
        CITY_COORDINATES.put("jaipur", new Coordinates(26.9124, 75.7873, "Jaipur"));
        CITY_COORDINATES.put("lucknow", new Coordinates(26.8467, 80.9462, "Lucknow"));
        CITY_COORDINATES.put("chandigarh", new Coordinates(30.7333, 76.7794, "Chandigarh"));
        CITY_COORDINATES.put("kochi", new Coordinates(9.9312, 76.2673, "Kochi"));
        CITY_COORDINATES.put("indore", new Coordinates(22.7196, 75.8577, "Indore"));
        CITY_COORDINATES.put("bhopal", new Coordinates(23.2599, 77.4126, "Bhopal"));
        CITY_COORDINATES.put("surat", new Coordinates(21.1702, 72.8311, "Surat"));
        CITY_COORDINATES.put("patna", new Coordinates(25.5941, 85.1376, "Patna"));
        CITY_COORDINATES.put("nagpur", new Coordinates(21.1458, 79.0882, "Nagpur"));
        CITY_COORDINATES.put("visakhapatnam", new Coordinates(17.6868, 83.2185, "Visakhapatnam"));
        CITY_COORDINATES.put("vadodara", new Coordinates(22.3072, 73.1812, "Vadodara"));
        CITY_COORDINATES.put("coimbatore", new Coordinates(11.0168, 76.9558, "Coimbatore"));
        CITY_COORDINATES.put("dehradun", new Coordinates(30.3165, 78.0322, "Dehradun"));
        CITY_COORDINATES.put("guwahati", new Coordinates(26.1445, 91.7362, "Guwahati"));
        CITY_COORDINATES.put("mysuru", new Coordinates(12.2958, 76.6394, "Mysuru"));
        CITY_COORDINATES.put("amritsar", new Coordinates(31.6340, 74.8723, "Amritsar"));
        CITY_COORDINATES.put("varanasi", new Coordinates(25.3176, 82.9739, "Varanasi"));
        CITY_COORDINATES.put("agra", new Coordinates(27.1767, 78.0081, "Agra"));
        CITY_COORDINATES.put("panaji", new Coordinates(15.4909, 73.8278, "Goa (Panaji)"));
        CITY_COORDINATES.put("goa", new Coordinates(15.4909, 73.8278, "Goa (Panaji)"));
        CITY_COORDINATES.put("thiruvananthapuram", new Coordinates(8.5241, 76.9366, "Thiruvananthapuram"));
        CITY_COORDINATES.put("bhubaneswar", new Coordinates(20.2961, 85.8245, "Bhubaneswar"));
        CITY_COORDINATES.put("raipur", new Coordinates(21.2514, 81.6296, "Raipur"));
        CITY_COORDINATES.put("mangalore", new Coordinates(12.9141, 74.8560, "Mangalore"));
        CITY_COORDINATES.put("ranchi", new Coordinates(23.3441, 85.3096, "Ranchi"));
        CITY_COORDINATES.put("jodhpur", new Coordinates(26.2389, 73.0243, "Jodhpur"));
        CITY_COORDINATES.put("madurai", new Coordinates(9.9252, 78.1198, "Madurai"));
        CITY_COORDINATES.put("nashik", new Coordinates(19.9975, 73.7898, "Nashik"));
        CITY_COORDINATES.put("kanpur", new Coordinates(26.4499, 80.3319, "Kanpur"));
        CITY_COORDINATES.put("thane", new Coordinates(19.2183, 72.9781, "Thane"));
        CITY_COORDINATES.put("navi mumbai", new Coordinates(19.0330, 73.0297, "Navi Mumbai"));
        // Rau/Mhow near Indore
        CITY_COORDINATES.put("rau", new Coordinates(22.6774, 75.8295, "Indore"));
        CITY_COORDINATES.put("mhow", new Coordinates(22.5612, 75.7625, "Indore"));
        CITY_COORDINATES.put("umariya", new Coordinates(22.6480, 75.7850, "Indore"));
        CITY_COORDINATES.put("pithampur", new Coordinates(22.6032, 75.6943, "Indore"));
    }

    public record Coordinates(double latitude, double longitude, String canonicalCity) {}

    @Autowired
    public GooglePlacesService(
            TheatreRepository theatreRepository,
            ShowRepository showRepository,
            BookingSeatRepository bookingSeatRepository,
            TheatreService theatreService,
            @Autowired(required = false) OpenCinemaService openCinemaService,
            @Value("${app.google.places.base-url:https://places.googleapis.com/v1}") String baseUrl,
            @Value("${app.google.places.connect-timeout-seconds:8}") int connectTimeout,
            @Value("${app.google.places.read-timeout-seconds:10}") int readTimeout) {

        this.theatreRepository = theatreRepository;
        this.showRepository = showRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.theatreService = theatreService;
        this.openCinemaService = openCinemaService;
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

    // Testing constructors
    GooglePlacesService(
            TheatreRepository theatreRepository,
            ShowRepository showRepository,
            BookingSeatRepository bookingSeatRepository,
            TheatreService theatreService,
            OpenCinemaService openCinemaService,
            RestClient restClient) {
        this.theatreRepository = theatreRepository;
        this.showRepository = showRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.theatreService = theatreService;
        this.openCinemaService = openCinemaService;
        this.restClient = restClient;
        this.objectMapper = new ObjectMapper();
        this.defaultRadiusKm = 15.0;
    }

    GooglePlacesService(
            TheatreRepository theatreRepository,
            ShowRepository showRepository,
            BookingSeatRepository bookingSeatRepository,
            TheatreService theatreService,
            RestClient restClient) {
        this(theatreRepository, showRepository, bookingSeatRepository, theatreService, null, restClient);
    }

    @jakarta.annotation.PostConstruct
    public void postConstruct() {
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
                            if ("GOOGLE_PLACES_API_KEY".equalsIgnoreCase(k) && (apiKey == null || apiKey.isBlank())) {
                                this.apiKey = v;
                                log.info("Google Places API Key dynamically loaded from {}", p);
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

    /**
     * Resolves real coordinates for a city name.
     * Supports Indore, Bhopal, Mumbai, Delhi, Bengaluru, etc.
     */
    public Coordinates resolveCityCoordinates(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return CITY_COORDINATES.get("mumbai");
        }
        String normalized = cityName.trim().toLowerCase();

        // 1. Direct match
        if (CITY_COORDINATES.containsKey(normalized)) {
            return CITY_COORDINATES.get(normalized);
        }

        // 2. Partial match
        for (Map.Entry<String, Coordinates> entry : CITY_COORDINATES.entrySet()) {
            if (normalized.contains(entry.getKey()) || entry.getKey().contains(normalized)) {
                return entry.getValue();
            }
        }

        // 3. Fallback: Check local theatres in database for this city
        List<Theatre> dbTheatres = theatreRepository.findByCityIgnoreCaseAndIsActiveTrue(cityName.trim());
        for (Theatre t : dbTheatres) {
            if (t.getLatitude() != null && t.getLongitude() != null) {
                return new Coordinates(t.getLatitude(), t.getLongitude(), t.getCity());
            }
        }

        // Default to Mumbai if unknown
        return new Coordinates(19.0760, 72.8777, cityName.trim());
    }

    public String findClosestCityName(double lat, double lon) {
        String closest = "Delhi NCR";
        double minDist = Double.MAX_VALUE;
        for (Coordinates c : CITY_COORDINATES.values()) {
            double d = calculateHaversineKm(lat, lon, c.latitude(), c.longitude());
            if (d < minDist) {
                minDist = d;
                closest = c.canonicalCity();
            }
        }
        return closest;
    }

    /**
     * Discovers nearby cinemas and checks whether they are integrated into our local booking system.
     * Clearly distinguishes external discovery from registered theatres with bookable shows.
     */
    @Transactional
    public CinemaSearchResponse discoverNearbyCinemas(
            String city,
            Double userLat,
            Double userLon,
            Double radiusKm,
            Long movieId,
            LocalDate date) {

        // Determine effective coordinates and city
        double effectiveLat;
        double effectiveLon;
        String effectiveCity;

        if (userLat != null && userLon != null) {
            effectiveLat = userLat;
            effectiveLon = userLon;
            if (city != null && !city.isBlank()) {
                effectiveCity = city.trim();
            } else {
                effectiveCity = findClosestCityName(userLat, userLon);
            }
        } else if (city != null && !city.isBlank()) {
            Coordinates coords = resolveCityCoordinates(city.trim());
            effectiveLat = coords.latitude();
            effectiveLon = coords.longitude();
            effectiveCity = coords.canonicalCity();
        } else {
            return CinemaSearchResponse.builder()
                    .searchCity("")
                    .searchLatitude(0.0)
                    .searchLongitude(0.0)
                    .searchRadiusKm(radiusKm != null ? radiusKm : defaultRadiusKm)
                    .provider("LOCAL_DATABASE")
                    .providerConfigured(false)
                    .openCinemaConfigured(false)
                    .googlePlacesConfigured(false)
                    .providerStatusMessage("Please enter a city or detect location to discover cinemas.")
                    .cinemas(Collections.emptyList())
                    .build();
        }

        double searchRadius = (radiusKm != null && radiusKm > 0) ? radiusKm : defaultRadiusKm;
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        // Ensure city has registered theatres provisioned in our database if requested
        if (effectiveCity != null && !effectiveCity.isBlank()) {
            theatreService.ensureCityTheatres(effectiveCity);
        }

        List<CinemaDiscoveryDto> discoveredList = new ArrayList<>();
        boolean googleApiActive = isConfigured();
        boolean openCinemaActive = (openCinemaService != null && openCinemaService.isConfigured());

        String provider;
        if (openCinemaActive && googleApiActive) {
            provider = "OPEN_CINEMA_PLATFORM, GOOGLE_PLACES";
        } else if (openCinemaActive) {
            provider = "OPEN_CINEMA_PLATFORM";
        } else if (googleApiActive) {
            provider = "GOOGLE_PLACES";
        } else {
            provider = "LOCAL_DATABASE";
        }

        StringBuilder statusBuilder = new StringBuilder();

        // Step 1a: Query Open Cinema Platform if key is configured
        if (openCinemaActive) {
            try {
                List<CinemaDiscoveryDto> openCinemas = openCinemaService.discoverCinemas(effectiveLat, effectiveLon, searchRadius);
                if (openCinemas.isEmpty() && city != null && !city.isBlank()) {
                    openCinemas = openCinemaService.discoverCinemasByCity(city, effectiveLat, effectiveLon);
                }
                for (CinemaDiscoveryDto oc : openCinemas) {
                    if (discoveredList.stream().noneMatch(existing -> isSameCinema(existing, oc))) {
                        discoveredList.add(oc);
                    }
                }
                statusBuilder.append(String.format("Open Cinema Platform active — Found %d listing(s). ", openCinemas.size()));
            } catch (Exception e) {
                log.warn("Open Cinema Platform query encountered error: {}. Proceeding with remaining providers.", e.getMessage());
                statusBuilder.append("Open Cinema Platform notice: ").append(e.getMessage()).append(". ");
            }
        }

        // Step 1b: Query Google Places API (New) if key is configured
        if (googleApiActive) {
            try {
                List<CinemaDiscoveryDto> googleCinemas = fetchFromGooglePlaces(effectiveLat, effectiveLon, searchRadius);
                for (CinemaDiscoveryDto gc : googleCinemas) {
                    if (discoveredList.stream().noneMatch(existing -> isSameCinema(existing, gc))) {
                        discoveredList.add(gc);
                    }
                }
                statusBuilder.append(String.format("Google Places API active — Discovered %d cinema(s) within %.1f km. ",
                        googleCinemas.size(), searchRadius));
            } catch (Exception e) {
                log.warn("Google Places API call encountered error: {}. Falling back to registered local database theatres.", e.getMessage());
                statusBuilder.append("Google Places API error: ").append(e.getMessage()).append(". ");
            }
        }

        if (!openCinemaActive && !googleApiActive) {
            statusBuilder.append("External cinema providers (Open Cinema Platform & Google Places) are unconfigured. Showing verified theatres registered in our booking database.");
        }

        String statusMessage = statusBuilder.toString().trim();

        // Step 2: Fetch registered database theatres and their active show inventory
        List<Theatre> registeredTheatres = theatreRepository.findByIsActiveTrue();
        Map<Long, List<NearbyTheatreShowsResponse.NearbyShowItem>> showsByTheatre = loadShowsForDate(registeredTheatres, movieId, targetDate);

        // Step 3: Match Google Places with local registered database theatres
        Map<Long, Theatre> matchedTheatres = new HashMap<>();

        for (CinemaDiscoveryDto cinema : discoveredList) {
            // Find match by name or coordinates
            Theatre matched = findMatchingTheatre(cinema, registeredTheatres);
            if (matched != null) {
                matchedTheatres.put(matched.getId(), matched);
                cinema.setRegistered(true);
                cinema.setTheatreId(matched.getId());
                cinema.setCity(matched.getCity());

                List<NearbyTheatreShowsResponse.NearbyShowItem> items = showsByTheatre.getOrDefault(matched.getId(), Collections.emptyList());
                if (!items.isEmpty()) {
                    cinema.setHasBookableShows(true);
                    cinema.setCinemaType("BOOKABLE_PARTNER");
                    cinema.setShows(items);
                } else {
                    cinema.setHasBookableShows(false);
                    cinema.setCinemaType("REGISTERED_THEATRE");
                    cinema.setShows(Collections.emptyList());
                }
            } else {
                cinema.setRegistered(false);
                cinema.setTheatreId(null);
                cinema.setHasBookableShows(false);
                cinema.setCinemaType("EXTERNAL_DISCOVERY");
                cinema.setShows(Collections.emptyList());
            }

            // Calculate distance from search center
            double distance = calculateHaversineKm(effectiveLat, effectiveLon, cinema.getLatitude(), cinema.getLongitude());
            cinema.setDistanceKm(Math.round(distance * 10.0) / 10.0);
        }

        // Step 4: Include any registered local theatres in the vicinity that weren't returned by Google Places
        for (Theatre theatre : registeredTheatres) {
            if (matchedTheatres.containsKey(theatre.getId())) {
                continue;
            }

            // Check if theatre is in current city or within radius (fuzzy match e.g. Delhi matches Delhi NCR)
            boolean cityMatch = theatre.getCity() != null && (
                    theatre.getCity().equalsIgnoreCase(effectiveCity) ||
                    theatre.getCity().toLowerCase().contains(effectiveCity.toLowerCase()) ||
                    effectiveCity.toLowerCase().contains(theatre.getCity().toLowerCase())
            );
            double distance = (theatre.getLatitude() != null && theatre.getLongitude() != null)
                    ? calculateHaversineKm(effectiveLat, effectiveLon, theatre.getLatitude(), theatre.getLongitude())
                    : (cityMatch ? 2.5 : 999.0);

            boolean withinRadius;
            if (userLat != null && userLon != null) {
                withinRadius = distance <= searchRadius;
            } else {
                withinRadius = cityMatch || distance <= searchRadius;
            }

            if (withinRadius) {
                List<NearbyTheatreShowsResponse.NearbyShowItem> items = showsByTheatre.getOrDefault(theatre.getId(), Collections.emptyList());
                boolean hasShows = !items.isEmpty();

                String mapsLink = (theatre.getLatitude() != null && theatre.getLongitude() != null)
                        ? "https://www.google.com/maps/dir/?api=1&destination=" + theatre.getLatitude() + "," + theatre.getLongitude()
                        : "https://www.google.com/maps/search/?api=1&query=" + java.net.URLEncoder.encode(theatre.getName() + " " + theatre.getCity(), java.nio.charset.StandardCharsets.UTF_8);

                discoveredList.add(CinemaDiscoveryDto.builder()
                        .id("local-" + theatre.getId())
                        .name(theatre.getName())
                        .address(theatre.getAddress())
                        .city(theatre.getCity())
                        .latitude(theatre.getLatitude())
                        .longitude(theatre.getLongitude())
                        .distanceKm(Math.round(distance * 10.0) / 10.0)
                        .googleMapsUri(mapsLink)
                        .rating(4.5)
                        .userRatingCount(350)
                        .isRegistered(true)
                        .theatreId(theatre.getId())
                        .hasBookableShows(hasShows)
                        .cinemaType(hasShows ? "BOOKABLE_PARTNER" : "REGISTERED_THEATRE")
                        .shows(items)
                        .build());
            }
        }

        // Step 5: If GPS coordinates were provided, strictly filter by searchRadius
        if (userLat != null && userLon != null) {
            discoveredList = discoveredList.stream()
                    .filter(c -> c.getDistanceKm() != null && c.getDistanceKm() <= searchRadius)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        // Step 6: Sort cinemas prioritizing bookable partner theatres with shows first, then distance
        discoveredList.sort(Comparator
                .comparing((CinemaDiscoveryDto c) -> Boolean.TRUE.equals(c.isHasBookableShows()) ? 0 : 1)
                .thenComparingDouble(c -> c.getDistanceKm() != null ? c.getDistanceKm() : 9999.0));

        return CinemaSearchResponse.builder()
                .searchCity(effectiveCity)
                .searchLatitude(effectiveLat)
                .searchLongitude(effectiveLon)
                .searchRadiusKm(searchRadius)
                .provider(provider)
                .providerConfigured(openCinemaActive || googleApiActive)
                .openCinemaConfigured(openCinemaActive)
                .googlePlacesConfigured(googleApiActive)
                .providerStatusMessage(statusMessage)
                .cinemas(discoveredList)
                .build();
    }

    /**
     * Calls Google Places API (New) Nearby Search:
     * POST https://places.googleapis.com/v1/places:searchNearby
     */
    private List<CinemaDiscoveryDto> fetchFromGooglePlaces(double lat, double lon, double radiusKm) {
        String cacheKey = String.format(Locale.US, "places:%.3f:%.3f:%.1f", lat, lon, radiusKm);
        CacheEntry<List<CinemaDiscoveryDto>> cached = placesCache.get(cacheKey);
        if (cached != null && !cached.isExpired(30)) {
            return cached.getData();
        }

        Map<String, Object> circle = Map.of(
                "center", Map.of("latitude", lat, "longitude", lon),
                "radius", radiusKm * 1000.0
        );

        Map<String, Object> requestBody = Map.of(
                "includedTypes", List.of("movie_theater"),
                "maxResultCount", 20,
                "locationRestriction", Map.of("circle", circle)
        );

        String jsonResponse = restClient.post()
                .uri("/places:searchNearby")
                .header("X-Goog-Api-Key", apiKey.trim())
                .header("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress,places.location,places.googleMapsUri,places.rating,places.userRatingCount")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                    throw new RuntimeException("Google Places client error: HTTP " + resp.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                    throw new RuntimeException("Google Places server error: HTTP " + resp.getStatusCode());
                })
                .body(String.class);

        List<CinemaDiscoveryDto> results = parseGooglePlacesResponse(jsonResponse, lat, lon);
        placesCache.put(cacheKey, new CacheEntry<>(results));
        return results;
    }

    private List<CinemaDiscoveryDto> parseGooglePlacesResponse(String json, double userLat, double userLon) {
        List<CinemaDiscoveryDto> list = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode places = root.path("places");
            if (places.isArray()) {
                for (JsonNode p : places) {
                    String placeId = p.path("id").asText(null);
                    String name = p.path("displayName").path("text").asText("Cinema");
                    String address = p.path("formattedAddress").asText("");
                    double pLat = p.path("location").path("latitude").asDouble(userLat);
                    double pLon = p.path("location").path("longitude").asDouble(userLon);
                    String mapsUri = p.path("googleMapsUri").asText(
                            "https://www.google.com/maps/search/?api=1&query=" + pLat + "," + pLon
                    );
                    double rating = p.path("rating").asDouble(4.3);
                    int reviewCount = p.path("userRatingCount").asInt(100);

                    double dist = calculateHaversineKm(userLat, userLon, pLat, pLon);

                    list.add(CinemaDiscoveryDto.builder()
                            .id(placeId)
                            .name(name)
                            .address(address)
                            .latitude(pLat)
                            .longitude(pLon)
                            .distanceKm(Math.round(dist * 10.0) / 10.0)
                            .googleMapsUri(mapsUri)
                            .rating(rating)
                            .userRatingCount(reviewCount)
                            .isRegistered(false)
                            .theatreId(null)
                            .hasBookableShows(false)
                            .cinemaType("EXTERNAL_DISCOVERY")
                            .shows(Collections.emptyList())
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Google Places JSON response: {}", e.getMessage());
        }
        return list;
    }

    private Theatre findMatchingTheatre(CinemaDiscoveryDto cinema, List<Theatre> registeredTheatres) {
        String cinemaName = (cinema.getName() != null) ? cinema.getName().toLowerCase().replaceAll("[^a-z0-9 ]", "") : "";
        for (Theatre t : registeredTheatres) {
            String dbName = (t.getName() != null) ? t.getName().toLowerCase().replaceAll("[^a-z0-9 ]", "") : "";
            if (cinemaName.isEmpty() || dbName.isEmpty()) continue;

            // Direct name containment
            if (cinemaName.contains(dbName) || dbName.contains(cinemaName)) {
                return t;
            }

            // Word overlap check (e.g. "pvr inox central mall" vs "pvr central mall")
            Set<String> wordsA = new HashSet<>(Arrays.asList(cinemaName.split("\\s+")));
            Set<String> wordsB = new HashSet<>(Arrays.asList(dbName.split("\\s+")));
            wordsA.remove("cinemas");
            wordsA.remove("cinema");
            wordsA.remove("theatre");
            wordsB.remove("cinemas");
            wordsB.remove("cinema");
            wordsB.remove("theatre");

            int common = 0;
            for (String w : wordsA) {
                if (wordsB.contains(w)) common++;
            }
            if (common >= 2) {
                return t;
            }

            // If coordinates are very close (< 200 meters) AND at least one major brand word matches
            if (t.getLatitude() != null && t.getLongitude() != null && cinema.getLatitude() != null && cinema.getLongitude() != null) {
                double dist = calculateHaversineKm(cinema.getLatitude(), cinema.getLongitude(), t.getLatitude(), t.getLongitude());
                if (dist < 0.2 && common >= 1) {
                    return t;
                }
            }
        }
        return null;
    }

    private boolean isSameCinema(CinemaDiscoveryDto a, CinemaDiscoveryDto b) {
        if (a == null || b == null) return false;
        String nameA = (a.getName() != null) ? a.getName().toLowerCase().replaceAll("[^a-z0-9 ]", "").trim() : "";
        String nameB = (b.getName() != null) ? b.getName().toLowerCase().replaceAll("[^a-z0-9 ]", "").trim() : "";
        if (!nameA.isEmpty() && !nameB.isEmpty()) {
            if (nameA.equals(nameB) || nameA.contains(nameB) || nameB.contains(nameA)) {
                return true;
            }
        }
        if (a.getLatitude() != null && a.getLongitude() != null && b.getLatitude() != null && b.getLongitude() != null) {
            double dist = calculateHaversineKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
            return dist < 0.15; // within 150m is treated as duplicate
        }
        return false;
    }

    private Map<Long, List<NearbyTheatreShowsResponse.NearbyShowItem>> loadShowsForDate(
            List<Theatre> theatres, Long movieId, LocalDate targetDate) {

        LocalDateTime start = targetDate.isEqual(LocalDate.now()) ? LocalDateTime.now().minusHours(1) : targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(23, 59, 59);

        List<Show> shows = showRepository.findNearbyShows(movieId, null, start, end);
        if (shows.isEmpty()) {
            shows = showRepository.findNearbyShows(movieId, null, LocalDateTime.now().minusHours(1), LocalDateTime.now().plusDays(3));
        }

        Map<Long, List<NearbyTheatreShowsResponse.NearbyShowItem>> map = new HashMap<>();
        List<Long> showIds = shows.stream().map(Show::getId).toList();
        Map<Long, Integer> bookedSeatsMap = new HashMap<>();
        if (!showIds.isEmpty()) {
            try {
                List<Object[]> counts = bookingSeatRepository.countBookedSeatsByShowIds(showIds);
                for (Object[] row : counts) {
                    Long sId = ((Number) row[0]).longValue();
                    int cnt = ((Number) row[1]).intValue();
                    bookedSeatsMap.put(sId, cnt);
                }
            } catch (Exception ignored) {}
        }

        for (Show s : shows) {
            Long theatreId = s.getScreen().getTheatre().getId();
            int totalSeats = (s.getScreen().getTotalSeats() != null) ? s.getScreen().getTotalSeats() : 86;
            int bookedSeats = bookedSeatsMap.getOrDefault(s.getId(), 0);
            int availableSeats = Math.max(0, totalSeats - bookedSeats);

            NearbyTheatreShowsResponse.NearbyShowItem item = NearbyTheatreShowsResponse.NearbyShowItem.builder()
                    .showId(s.getId())
                    .movieId(s.getMovie().getId())
                    .movieTitle(s.getMovie().getTitle())
                    .movieLanguage(s.getMovie().getLanguage())
                    .screenName(s.getScreen().getName())
                    .showTime(s.getShowTime())
                    .basePrice(s.getBasePrice())
                    .premiumPrice(s.getPremiumPrice())
                    .vipPrice(s.getVipPrice())
                    .totalSeats(totalSeats)
                    .availableSeats(availableSeats)
                    .build();

            map.computeIfAbsent(theatreId, k -> new ArrayList<>()).add(item);
        }
        return map;
    }

    public static double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDist = Math.toRadians(lat2 - lat1);
        double lonDist = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDist / 2) * Math.sin(latDist / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDist / 2) * Math.sin(lonDist / 2);
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
            return (System.currentTimeMillis() - timestamp) > (ttlMinutes * 60 * 1000L);
        }
    }
}
