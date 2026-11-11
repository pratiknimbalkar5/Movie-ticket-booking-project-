import React, { useState } from 'react';
import { Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import Navbar from './components/Navbar';
import HomePage from './pages/HomePage';
import SearchPage from './pages/SearchPage';
import MoviesPage from './pages/MoviesPage';
import MovieDetailPage from './pages/MovieDetailPage';
import SeatSelectionPage from './pages/SeatSelectionPage';
import BookingConfirmationPage from './pages/BookingConfirmationPage';
import MyBookingsPage from './pages/MyBookingsPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import NotFoundPage from './pages/NotFoundPage';
import { normalizeIndianCity } from './services/bmsLocationService';

export default function App() {
  const [selectedCity, setSelectedCity] = useState(() => {
    const saved = localStorage.getItem('selected_city');
    return saved ? normalizeIndianCity(saved) : '';
  });
  const [neighborhood, setNeighborhood] = useState('');
  const [userCoords, setUserCoords] = useState(null);

  const handleLocationDetected = ({ latitude, longitude, neighborhood: n, city }) => {
    setUserCoords({ latitude, longitude });
    if (n) setNeighborhood(n);
    if (city) {
      const clean = normalizeIndianCity(city, latitude, longitude);
      setSelectedCity(clean);
      localStorage.setItem('selected_city', clean);
    }
  };

  const handleCitySelect = (city) => {
    const clean = normalizeIndianCity(city);
    setSelectedCity(clean);
    setUserCoords(null); // Purge previous coordinates so old coordinates do not distort discovery
    if (clean) {
      localStorage.setItem('selected_city', clean);
    } else {
      localStorage.removeItem('selected_city');
    }
    setNeighborhood('');
  };

  return (
    <AuthProvider>
      <Navbar
        selectedCity={selectedCity}
        onCityChange={handleCitySelect}
        neighborhood={neighborhood}
        onLocationDetected={handleLocationDetected}
        userCoords={userCoords}
      />

      <main>
        <Routes>
          {/* Root URL strictly renders clean Homepage */}
          <Route
            path="/"
            element={
              <HomePage
                selectedCity={selectedCity}
                onCityChange={handleCitySelect}
                userCoords={userCoords}
                onLocationDetected={handleLocationDetected}
              />
            }
          />

          {/* Dedicated Search Panel & View */}
          <Route path="/search" element={<SearchPage />} />

          {/* Dedicated City Theatres & Shows View */}
          <Route
            path="/theatres"
            element={
              <MoviesPage
                initialTab="cinemas"
                selectedCity={selectedCity}
                onCityChange={handleCitySelect}
                userCoords={userCoords}
                onLocationDetected={handleLocationDetected}
              />
            }
          />

          {/* Now In Theatres Catalog */}
          <Route
            path="/now-showing"
            element={
              <MoviesPage
                initialTab="now-playing"
                selectedCity={selectedCity}
                onCityChange={handleCitySelect}
                userCoords={userCoords}
                onLocationDetected={handleLocationDetected}
              />
            }
          />

          {/* Upcoming Movies Catalog */}
          <Route
            path="/upcoming"
            element={
              <MoviesPage
                initialTab="upcoming"
                selectedCity={selectedCity}
                onCityChange={handleCitySelect}
                userCoords={userCoords}
                onLocationDetected={handleLocationDetected}
              />
            }
          />

          {/* Popular Catalog */}
          <Route
            path="/popular"
            element={
              <MoviesPage
                initialTab="popular"
                selectedCity={selectedCity}
                onCityChange={handleCitySelect}
                userCoords={userCoords}
                onLocationDetected={handleLocationDetected}
              />
            }
          />

          {/* Trending Catalog */}
          <Route
            path="/trending"
            element={
              <MoviesPage
                initialTab="trending"
                selectedCity={selectedCity}
                onCityChange={handleCitySelect}
                userCoords={userCoords}
                onLocationDetected={handleLocationDetected}
              />
            }
          />

          {/* Dynamic Movie Detail with Showtimes */}
          <Route
            path="/movie/:id"
            element={
              <MovieDetailPage
                selectedCity={selectedCity}
                onCityChange={handleCitySelect}
              />
            }
          />
          <Route
            path="/movies/:id"
            element={
              <MovieDetailPage
                selectedCity={selectedCity}
                onCityChange={handleCitySelect}
              />
            }
          />

          {/* Seat Layout Selection */}
          <Route path="/seat-selection/:showId" element={<SeatSelectionPage />} />

          {/* Booking Confirmation & Instant Cancellation */}
          <Route path="/booking-confirmation/:bookingId" element={<BookingConfirmationPage />} />

          {/* User Bookings Wallet */}
          <Route path="/bookings" element={<MyBookingsPage />} />

          {/* Auth Routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* 404 Route */}
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
    </AuthProvider>
  );
}
