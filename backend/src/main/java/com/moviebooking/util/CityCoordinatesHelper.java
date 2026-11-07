package com.moviebooking.util;

import java.util.HashMap;
import java.util.Map;

public final class CityCoordinatesHelper {

    private CityCoordinatesHelper() {}

    public record CityCoords(double latitude, double longitude, String canonicalCity) {}

    private static final Map<String, CityCoords> CITY_COORDS = new HashMap<>();

    static {
        CITY_COORDS.put("mumbai", new CityCoords(19.0760, 72.8777, "Mumbai"));
        CITY_COORDS.put("delhi", new CityCoords(28.6139, 77.2090, "Delhi NCR"));
        CITY_COORDS.put("delhi ncr", new CityCoords(28.6139, 77.2090, "Delhi NCR"));
        CITY_COORDS.put("noida", new CityCoords(28.5355, 77.3910, "Noida"));
        CITY_COORDS.put("gurugram", new CityCoords(28.4595, 77.0266, "Gurugram"));
        CITY_COORDS.put("bengaluru", new CityCoords(12.9716, 77.5946, "Bengaluru"));
        CITY_COORDS.put("bangalore", new CityCoords(12.9716, 77.5946, "Bengaluru"));
        CITY_COORDS.put("hyderabad", new CityCoords(17.3850, 78.4867, "Hyderabad"));
        CITY_COORDS.put("pune", new CityCoords(18.5204, 73.8567, "Pune"));
        CITY_COORDS.put("chennai", new CityCoords(13.0827, 80.2707, "Chennai"));
        CITY_COORDS.put("kolkata", new CityCoords(22.5726, 88.3639, "Kolkata"));
        CITY_COORDS.put("ahmedabad", new CityCoords(23.0225, 72.5714, "Ahmedabad"));
        CITY_COORDS.put("jaipur", new CityCoords(26.9124, 75.7873, "Jaipur"));
        CITY_COORDS.put("lucknow", new CityCoords(26.8467, 80.9462, "Lucknow"));
        CITY_COORDS.put("chandigarh", new CityCoords(30.7333, 76.7794, "Chandigarh"));
        CITY_COORDS.put("kochi", new CityCoords(9.9312, 76.2673, "Kochi"));
        CITY_COORDS.put("indore", new CityCoords(22.7196, 75.8577, "Indore"));
        CITY_COORDS.put("bhopal", new CityCoords(23.2599, 77.4126, "Bhopal"));
        CITY_COORDS.put("surat", new CityCoords(21.1702, 72.8311, "Surat"));
        CITY_COORDS.put("patna", new CityCoords(25.5941, 85.1376, "Patna"));
        CITY_COORDS.put("nagpur", new CityCoords(21.1458, 79.0882, "Nagpur"));
        CITY_COORDS.put("visakhapatnam", new CityCoords(17.6868, 83.2185, "Visakhapatnam"));
        CITY_COORDS.put("vadodara", new CityCoords(22.3072, 73.1812, "Vadodara"));
        CITY_COORDS.put("coimbatore", new CityCoords(11.0168, 76.9558, "Coimbatore"));
        CITY_COORDS.put("dehradun", new CityCoords(30.3165, 78.0322, "Dehradun"));
        CITY_COORDS.put("guwahati", new CityCoords(26.1445, 91.7362, "Guwahati"));
        CITY_COORDS.put("mysuru", new CityCoords(12.2958, 76.6394, "Mysuru"));
        CITY_COORDS.put("amritsar", new CityCoords(31.6340, 74.8723, "Amritsar"));
        CITY_COORDS.put("varanasi", new CityCoords(25.3176, 82.9739, "Varanasi"));
        CITY_COORDS.put("agra", new CityCoords(27.1767, 78.0081, "Agra"));
        CITY_COORDS.put("panaji", new CityCoords(15.4909, 73.8278, "Goa (Panaji)"));
        CITY_COORDS.put("goa", new CityCoords(15.4909, 73.8278, "Goa (Panaji)"));
        CITY_COORDS.put("thiruvananthapuram", new CityCoords(8.5241, 76.9366, "Thiruvananthapuram"));
        CITY_COORDS.put("bhubaneswar", new CityCoords(20.2961, 85.8245, "Bhubaneswar"));
        CITY_COORDS.put("raipur", new CityCoords(21.2514, 81.6296, "Raipur"));
        CITY_COORDS.put("mangalore", new CityCoords(12.9141, 74.8560, "Mangalore"));
        CITY_COORDS.put("ranchi", new CityCoords(23.3441, 85.3096, "Ranchi"));
        CITY_COORDS.put("jodhpur", new CityCoords(26.2389, 73.0243, "Jodhpur"));
        CITY_COORDS.put("madurai", new CityCoords(9.9252, 78.1198, "Madurai"));
        CITY_COORDS.put("nashik", new CityCoords(19.9975, 73.7898, "Nashik"));
        CITY_COORDS.put("kanpur", new CityCoords(26.4499, 80.3319, "Kanpur"));
        CITY_COORDS.put("thane", new CityCoords(19.2183, 72.9781, "Thane"));
    }

    public static CityCoords resolveCityCoords(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return new CityCoords(19.0760, 72.8777, "Mumbai");
        }
        String clean = cityName.trim().toLowerCase();
        if (CITY_COORDS.containsKey(clean)) {
            return CITY_COORDS.get(clean);
        }
        for (Map.Entry<String, CityCoords> entry : CITY_COORDS.entrySet()) {
            if (clean.contains(entry.getKey()) || entry.getKey().contains(clean)) {
                return entry.getValue();
            }
        }
        return new CityCoords(19.0760, 72.8777, cityName.trim());
    }

    public static double calculateHaversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round((R * c) * 10.0) / 10.0;
    }
}

