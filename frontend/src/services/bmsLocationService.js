/**
 * BookMyShow-style Location and Geolocation Tracking Service for India
 * Detects user coordinates via GPS, reverse-geocodes to exact Indian locality/city,
 * and tracks residential cinema theatres across all Indian states and metro hubs.
 */

export const POPULAR_CITIES = [
  'Mumbai',
  'Delhi NCR',
  'Bengaluru',
  'Hyderabad',
  'Ahmedabad',
  'Chennai',
  'Kolkata',
  'Pune',
  'Jaipur',
  'Lucknow',
  'Chandigarh',
  'Kochi'
];

export const INDIAN_REGIONS = {
  'Popular Metros': [
    'Mumbai', 'Delhi NCR', 'Bengaluru', 'Hyderabad', 'Ahmedabad', 'Chennai', 'Kolkata', 'Pune'
  ],
  'North India': [
    'Delhi NCR', 'Noida', 'Gurugram', 'Faridabad', 'Ghaziabad', 'Chandigarh', 'Lucknow',
    'Kanpur', 'Jaipur', 'Agra', 'Varanasi', 'Prayagraj', 'Meerut', 'Bareilly', 'Aligarh',
    'Gorakhpur', 'Moradabad', 'Dehradun', 'Amritsar', 'Ludhiana', 'Jalandhar', 'Jammu',
    'Srinagar', 'Shimla', 'Haridwar'
  ],
  'West India': [
    'Mumbai', 'Pune', 'Ahmedabad', 'Surat', 'Vadodara', 'Rajkot', 'Nagpur', 'Nashik',
    'Aurangabad', 'Thane', 'Navi Mumbai', 'Kalyan', 'Solapur', 'Kolhapur', 'Bhavnagar',
    'Jamnagar', 'Jodhpur', 'Udaipur', 'Kota', 'Bikaner', 'Ajmer', 'Goa (Panaji)'
  ],
  'South India': [
    'Bengaluru', 'Hyderabad', 'Chennai', 'Kochi', 'Coimbatore', 'Mysuru', 'Visakhapatnam',
    'Vijayawada', 'Guntur', 'Nellore', 'Madurai', 'Tiruchirappalli', 'Salem', 'Tirunelveli',
    'Erode', 'Mangalore', 'Hubli-Dharwad', 'Belgaum', 'Warangal', 'Thiruvananthapuram',
    'Kozhikode', 'Thrissur'
  ],
  'East & Central India': [
    'Kolkata', 'Howrah', 'Durgapur', 'Asansol', 'Siliguri', 'Patna', 'Gaya', 'Bhagalpur',
    'Muzaffarpur', 'Ranchi', 'Jamshedpur', 'Dhanbad', 'Bhubaneswar', 'Cuttack', 'Rourkela',
    'Guwahati', 'Bhopal', 'Indore', 'Jabalpur', 'Gwalior', 'Ujjain', 'Raipur', 'Bhilai'
  ]
};

// Flatten and deduplicate all Indian cities
export const ALL_INDIAN_CITIES = Array.from(
  new Set(Object.values(INDIAN_REGIONS).flat())
).sort((a, b) => a.localeCompare(b));

export const CITY_COORDINATES = {
  'Mumbai': { lat: 19.0760, lon: 72.8777 },
  'Delhi NCR': { lat: 28.6139, lon: 77.2090 },
  'Noida': { lat: 28.5355, lon: 77.3910 },
  'Gurugram': { lat: 28.4595, lon: 77.0266 },
  'Ghaziabad': { lat: 28.6692, lon: 77.4538 },
  'Faridabad': { lat: 28.4089, lon: 77.3178 },
  'Bengaluru': { lat: 12.9716, lon: 77.5946 },
  'Hyderabad': { lat: 17.3850, lon: 78.4867 },
  'Pune': { lat: 18.5204, lon: 73.8567 },
  'Chennai': { lat: 13.0827, lon: 80.2707 },
  'Kolkata': { lat: 22.5726, lon: 88.3639 },
  'Ahmedabad': { lat: 23.0225, lon: 72.5714 },
  'Jaipur': { lat: 26.9124, lon: 75.7873 },
  'Lucknow': { lat: 26.8467, lon: 80.9462 },
  'Chandigarh': { lat: 30.7333, lon: 76.7794 },
  'Kochi': { lat: 9.9312, lon: 76.2673 },
  'Indore': { lat: 22.7196, lon: 75.8577 },
  'Surat': { lat: 21.1702, lon: 72.8311 },
  'Bhopal': { lat: 23.2599, lon: 77.4126 },
  'Patna': { lat: 25.5941, lon: 85.1376 },
  'Nagpur': { lat: 21.1458, lon: 79.0882 },
  'Visakhapatnam': { lat: 17.6868, lon: 83.2185 },
  'Vadodara': { lat: 22.3072, lon: 73.1812 },
  'Coimbatore': { lat: 11.0168, lon: 76.9558 },
  'Dehradun': { lat: 30.3165, lon: 78.0322 },
  'Guwahati': { lat: 26.1445, lon: 91.7362 },
  'Mysuru': { lat: 12.2958, lon: 76.6394 },
  'Amritsar': { lat: 31.6340, lon: 74.8723 },
  'Varanasi': { lat: 25.3176, lon: 82.9739 },
  'Agra': { lat: 27.1767, lon: 78.0081 },
  'Goa (Panaji)': { lat: 15.4909, lon: 73.8278 },
  'Thiruvananthapuram': { lat: 8.5241, lon: 76.9366 },
  'Bhubaneswar': { lat: 20.2961, lon: 85.8245 },
  'Raipur': { lat: 21.2514, lon: 81.6296 },
  'Mangalore': { lat: 12.9141, lon: 74.8560 },
  'Ranchi': { lat: 23.3441, lon: 85.3096 },
  'Jodhpur': { lat: 26.2389, lon: 73.0243 },
  'Madurai': { lat: 9.9252, lon: 78.1198 },
  'Nashik': { lat: 19.9975, lon: 73.7898 },
  'Kanpur': { lat: 26.4499, lon: 80.3319 },
  'Thane': { lat: 19.2183, lon: 72.9781 },
  'Navi Mumbai': { lat: 19.0330, lon: 73.0297 }
};

export function calculateDistanceKm(lat1, lon1, lat2, lon2) {
  const R = 6371; // Earth's radius in km
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * (Math.PI / 180)) *
      Math.cos(lat2 * (Math.PI / 180)) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Math.round(R * c * 10) / 10;
}

export function findClosestCity(lat, lon) {
  let closest = 'Mumbai';
  let minDistance = Infinity;

  for (const [city, coords] of Object.entries(CITY_COORDINATES)) {
    const dist = calculateDistanceKm(lat, lon, coords.lat, coords.lon);
    if (dist < minDistance) {
      minDistance = dist;
      closest = city;
    }
  }

  return { city: closest, distanceKm: minDistance };
}

export const LOCALITY_PRESETS = [
  // Mumbai
  { name: 'Andheri West (Infinity Mall)', city: 'Mumbai', lat: 19.1415, lon: 72.8315, pincode: '400053' },
  { name: 'Bandra West (Linking Road)', city: 'Mumbai', lat: 19.0596, lon: 72.8295, pincode: '400050' },
  { name: 'Malad West (Inorbit Mall)', city: 'Mumbai', lat: 19.1764, lon: 72.8358, pincode: '400064' },
  { name: 'Thane West (Viviana Mall)', city: 'Mumbai', lat: 19.2088, lon: 72.9715, pincode: '400606' },
  { name: 'Lower Parel (High Street Phoenix)', city: 'Mumbai', lat: 18.9950, lon: 72.8240, pincode: '400013' },
  // Delhi NCR
  { name: 'Vasant Kunj (Ambience Mall)', city: 'Delhi NCR', lat: 28.5402, lon: 77.1554, pincode: '110070' },
  { name: 'Nehru Place (Insignia Epicuria)', city: 'Delhi NCR', lat: 28.5501, lon: 77.2515, pincode: '110019' },
  { name: 'Noida Sector 18 (Mall of India)', city: 'Delhi NCR', lat: 28.5672, lon: 77.3209, pincode: '201301' },
  { name: 'Gurugram Cyber City (Ambience Mall)', city: 'Delhi NCR', lat: 28.5042, lon: 77.0970, pincode: '122002' },
  { name: 'Connaught Place', city: 'Delhi NCR', lat: 28.6315, lon: 77.2167, pincode: '110001' },
  // Bengaluru
  { name: 'Koramangala (Forum Mall)', city: 'Bengaluru', lat: 12.9352, lon: 77.6111, pincode: '560095' },
  { name: 'Ashok Nagar (Garuda Mall)', city: 'Bengaluru', lat: 12.9704, lon: 77.6094, pincode: '560025' },
  { name: 'Whitefield (Nexus Shantiniketan)', city: 'Bengaluru', lat: 12.9892, lon: 77.7289, pincode: '560067' },
  { name: 'Indiranagar 100ft Road', city: 'Bengaluru', lat: 12.9784, lon: 77.6408, pincode: '560038' },
  // Hyderabad
  { name: 'Gachibowli / Kothaguda (AMB Cinemas)', city: 'Hyderabad', lat: 17.4577, lon: 78.3644, pincode: '500084' },
  { name: 'Panjagutta (Next Galleria Mall)', city: 'Hyderabad', lat: 17.4262, lon: 78.4522, pincode: '500082' },
  { name: 'Necklace Road (Prasads Multiplex)', city: 'Hyderabad', lat: 17.4125, lon: 78.4682, pincode: '500063' },
  // Pune
  { name: 'Viman Nagar (Phoenix Marketcity)', city: 'Pune', lat: 18.5621, lon: 73.9167, pincode: '411014' },
  { name: 'Magarpatta City (Seasons Mall)', city: 'Pune', lat: 18.5197, lon: 73.9312, pincode: '411028' },
  { name: 'Kothrud / Deccan', city: 'Pune', lat: 18.5074, lon: 73.8077, pincode: '411038' },
  // Chennai
  { name: 'Royapettah (Sathyam Cinemas)', city: 'Chennai', lat: 13.0569, lon: 80.2586, pincode: '600014' },
  { name: 'Anna Nagar (VR Mall)', city: 'Chennai', lat: 13.0844, lon: 80.1947, pincode: '600040' },
  // Kolkata
  { name: 'Park Circus (Quest Mall)', city: 'Kolkata', lat: 22.5392, lon: 88.3653, pincode: '700017' },
  { name: 'EM Bypass (Mani Square)', city: 'Kolkata', lat: 22.5765, lon: 88.4014, pincode: '700054' },
  // Ahmedabad
  { name: 'Vastrapur (Alpha One Mall)', city: 'Ahmedabad', lat: 23.0373, lon: 72.5312, pincode: '380015' },
  { name: 'SG Highway (Acropolis Mall)', city: 'Ahmedabad', lat: 23.0514, lon: 72.5186, pincode: '380054' }
];

export function locateByPincodeOrArea(query) {
  if (!query || !query.trim()) return null;
  const q = query.trim().toLowerCase();

  // 1. Direct match with preset localities / pincodes
  const match = LOCALITY_PRESETS.find(
    (loc) => loc.pincode === q || loc.name.toLowerCase().includes(q) || q.includes(loc.name.toLowerCase())
  );
  if (match) {
    return {
      success: true,
      city: match.city,
      neighborhood: match.name,
      latitude: match.lat,
      longitude: match.lon,
      message: `Located near ${match.name}, ${match.city}`
    };
  }

  // 2. City coordinate match
  for (const [cityName, coords] of Object.entries(CITY_COORDINATES)) {
    if (cityName.toLowerCase() === q || cityName.toLowerCase().includes(q) || q.includes(cityName.toLowerCase())) {
      return {
        success: true,
        city: cityName,
        neighborhood: '',
        latitude: coords.lat,
        longitude: coords.lon,
        message: `Located: ${cityName} center`
      };
    }
  }

  return null;
}

export async function detectCurrentLocation() {
  return new Promise((resolve) => {
    if (!navigator.geolocation) {
      resolve({
        success: false,
        city: '',
        message: 'Geolocation is not supported by your browser. Please search your city or area.'
      });
      return;
    }

    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const { latitude, longitude } = pos.coords;
        let detectedCity = '';
        let neighborhood = '';

        try {
          const controller = new AbortController();
          const timeoutId = setTimeout(() => controller.abort(), 4000);

          const res = await fetch(
            `https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}&zoom=14&addressdetails=1`,
            { headers: { 'Accept': 'application/json' }, signal: controller.signal }
          );
          clearTimeout(timeoutId);

          if (res.ok) {
            const data = await res.json();
            const addr = data.address || {};
            neighborhood = addr.suburb || addr.neighbourhood || addr.residential || addr.city_district || addr.quarter || '';
            const rawCity = addr.city || addr.town || addr.municipality || addr.state_district || addr.county || '';

            if (rawCity) {
              const matched = ALL_INDIAN_CITIES.find(
                (c) => c.toLowerCase() === rawCity.toLowerCase() || rawCity.toLowerCase().includes(c.toLowerCase())
              );
              detectedCity = matched || rawCity;
            }
          }
        } catch (err) {
          // Geometric distance fallback
        }

        if (!detectedCity) {
          const match = findClosestCity(latitude, longitude);
          detectedCity = match.city;
        }

        resolve({
          success: true,
          city: detectedCity,
          neighborhood,
          latitude,
          longitude,
          message: `GPS Active: Located at ${neighborhood ? neighborhood + ', ' : ''}${detectedCity} (${latitude.toFixed(2)}°N, ${longitude.toFixed(2)}°E)`
        });
      },
      (err) => {
        // Permission denied or timeout
        resolve({
          success: false,
          city: '',
          message: 'GPS access denied or unavailable. Please type your city name or locality.'
        });
      },
      { timeout: 8000, enableHighAccuracy: true, maximumAge: 60000 }
    );
  });
}
