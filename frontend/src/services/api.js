import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Automatically inject JWT token into Authorization header if stored
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle auth expiration
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('user_details');
      localStorage.removeItem('user');
    }
    return Promise.reject(error);
  }
);

// Pan-India Dynamic Location & Cinema API Methods
export const searchLocationSuggestions = async (query, limit = 6) => {
  if (!query || !query.trim()) return [];
  try {
    const res = await api.get(`/location/search?query=${encodeURIComponent(query.trim())}&limit=${limit}`);
    return res.data?.data || [];
  } catch (err) {
    return [];
  }
};

export const reverseGeocodeCoordinates = async (lat, lon) => {
  try {
    const res = await api.get(`/location/reverse?lat=${lat}&lon=${lon}`);
    return res.data?.data || null;
  } catch (err) {
    return null;
  }
};

export const fetchNearbyCinemas = async ({ query = '', city = '', latitude = null, longitude = null, radiusKm = 15, date = '', movieId = null }) => {
  const params = new URLSearchParams();
  if (query && query.trim()) params.append('query', query.trim());
  if (city && city.trim()) params.append('city', city.trim());
  if (latitude != null) params.append('latitude', latitude);
  if (longitude != null) params.append('longitude', longitude);
  if (radiusKm != null) params.append('radiusKm', radiusKm);
  if (date) params.append('date', date);
  if (movieId != null) params.append('movieId', movieId);

  const res = await api.get(`/cinemas/nearby?${params.toString()}`);
  return res.data?.data || null;
};

export default api;
