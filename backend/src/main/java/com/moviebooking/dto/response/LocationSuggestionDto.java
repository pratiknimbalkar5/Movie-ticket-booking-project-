package com.moviebooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationSuggestionDto {
    private String displayName;     // Full description (e.g., "Vijay Nagar, Indore, Madhya Pradesh, 452010, India")
    private String name;            // Short name (e.g., "Vijay Nagar")
    private String locality;        // Suburb or neighborhood
    private String city;            // City (e.g., "Indore", "Mumbai", "Chennai")
    private String state;           // State (e.g., "Madhya Pradesh", "Maharashtra")
    private String postcode;        // PIN code (e.g., "452010", "600040")
    private double latitude;        // Latitude coordinate
    private double longitude;       // Longitude coordinate
    private String type;            // "suburb", "city", "postcode", "landmark", etc.
}
