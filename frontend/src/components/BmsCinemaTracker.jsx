import React, { useState, useEffect, useMemo, useRef } from 'react';
import api from '../services/api';
import { detectCurrentLocation } from '../services/bmsLocationService';

const SUGGESTED_CITIES = [
  'Mumbai', 'Delhi NCR', 'Bengaluru', 'Hyderabad', 'Indore', 'Bhopal',
  'Pune', 'Ahmedabad', 'Chennai', 'Kolkata', 'Jaipur', 'Lucknow',
  'Chandigarh', 'Kochi', 'Surat', 'Nagpur', 'Goa (Panaji)', 'Patna'
];

export default function BmsCinemaTracker({
  selectedCity = '',
  onCityChange,
  onSelectShow,
  userLocation = null,
  onLocationDetected,
}) {
  const [cityInput, setCityInput] = useState(selectedCity || '');
  const [theatres, setTheatres] = useState([]);
  const [loading, setLoading] = useState(false);
  const [locating, setLocating] = useState(false);
  const [locationStatus, setLocationStatus] = useState('');
  const [showDropdown, setShowDropdown] = useState(false);
  const [movieFilter, setMovieFilter] = useState('');
  const [formatFilter, setFormatFilter] = useState('ALL'); // 'ALL' | 'IMAX' | 'ATMOS'
  const [searchRadius, setSearchRadius] = useState(10); // 10 km default, expandable to 20 or 30 km
  const [selectedDate, setSelectedDate] = useState(() => {
    return new Date().toISOString().split('T')[0];
  });

  const dropdownRef = useRef(null);

  // Sync cityInput when selectedCity changes
  useEffect(() => {
    setCityInput(selectedCity);
  }, [selectedCity]);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Upcoming 4 dates
  const dateOptions = Array.from({ length: 4 }).map((_, idx) => {
    const d = new Date();
    d.setDate(d.getDate() + idx);
    const iso = d.toISOString().split('T')[0];
    const label =
      idx === 0
        ? 'Today'
        : idx === 1
        ? 'Tomorrow'
        : d.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
    return { iso, label };
  });

  // Fetch real shows in the selected city directly
  const fetchCityShows = async (city, date, coords = null, radius = searchRadius) => {
    if (!city && !coords) return;
    try {
      setLoading(true);
      let theatreList = [];

      let queryParams = `date=${date}&radiusKm=${radius}`;
      if (city) queryParams += `&city=${encodeURIComponent(city)}`;
      if (coords?.latitude && coords?.longitude) {
        queryParams += `&latitude=${coords.latitude}&longitude=${coords.longitude}`;
      }

      // 1. Primary: fetch guaranteed bookable shows matching city and/or GPS coordinates
      try {
        const res = await api.get(`/shows/nearby?${queryParams}`);
        theatreList = res.data?.data || [];
      } catch (e) {}

      // 2. Fallback: If empty and coords exist, scan nearby coordinates directly
      if (theatreList.length === 0 && coords?.latitude && coords?.longitude) {
        try {
          const res = await api.get(
            `/shows/scan-nearby?latitude=${coords.latitude}&longitude=${coords.longitude}&radiusKm=${radius}&date=${date}`
          );
          theatreList = res.data?.data || [];
        } catch (e) {}
      }

      // 3. Fallback: If still empty and city exists, fetch without strict radius restriction
      if (theatreList.length === 0 && city) {
        try {
          const res = await api.get(`/shows/nearby?city=${encodeURIComponent(city)}`);
          theatreList = res.data?.data || [];
        } catch (e) {}
      }

      // 4. Normalize theatre objects & strictly calculate accurate distance
      const normalized = theatreList.map((t) => {
        let dist = t.distanceKm;
        if (coords?.latitude && coords?.longitude && t.latitude && t.longitude) {
          // Calculate true Haversine distance from user's current GPS coordinates
          dist = calculateDistanceKm(coords.latitude, coords.longitude, t.latitude, t.longitude);
        } else if (dist != null) {
          dist = Math.round(Number(dist) * 10) / 10;
        } else {
          dist = 2.5;
        }

        return {
          theatreId: t.theatreId || t.id,
          theatreName: t.theatreName || t.name,
          address: t.address || t.theatreAddress || `${city || t.city}`,
          city: t.city || city,
          latitude: t.latitude,
          longitude: t.longitude,
          distanceKm: dist,
          shows: (t.shows || []).map((s) => ({
            ...s,
            showId: s.showId || s.id,
            id: s.showId || s.id,
            theatreName: t.theatreName || t.name,
            theatreAddress: t.address || t.theatreAddress,
            theatreCity: t.city || city,
          })),
        };
      });

      // Filter by radius if GPS was used
      const radiusFiltered = (coords?.latitude && coords?.longitude)
        ? normalized.filter((t) => t.distanceKm <= radius)
        : normalized;

      const finalTheatres = radiusFiltered.length > 0 ? radiusFiltered : normalized;

      // Sort ascending so closest cinema is always displayed first
      finalTheatres.sort((a, b) => (a.distanceKm ?? 999) - (b.distanceKm ?? 999));

      setTheatres(finalTheatres);
    } catch (err) {
      setTheatres([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCityShows(selectedCity, selectedDate, userLocation, searchRadius);
  }, [selectedCity, selectedDate, userLocation, searchRadius]);

  // Handle manual city search submission - clear old GPS coords to avoid stale location mixing
  const handleCitySubmit = (e) => {
    e?.preventDefault();
    const clean = cityInput.trim();
    if (!clean) return;
    setShowDropdown(false);
    if (onLocationDetected) onLocationDetected(null);
    if (onCityChange) onCityChange(clean);
    fetchCityShows(clean, selectedDate, null, searchRadius);
  };

  const handleSelectCitySuggestion = (city) => {
    setCityInput(city);
    setShowDropdown(false);
    if (onLocationDetected) onLocationDetected(null);
    if (onCityChange) onCityChange(city);
    fetchCityShows(city, selectedDate, null, searchRadius);
  };

  // GPS Locate
  const handleGpsDetect = async () => {
    setLocating(true);
    setLocationStatus('📡 Detecting GPS location...');
    try {
      const res = await detectCurrentLocation();
      if (res.success) {
        setLocationStatus(`📍 Detected: ${res.city}`);
        setCityInput(res.city);
        if (onCityChange) onCityChange(res.city);
        if (onLocationDetected) {
          onLocationDetected({
            latitude: res.latitude,
            longitude: res.longitude,
            neighborhood: res.neighborhood,
          });
        }
        fetchCityShows(res.city, selectedDate, { latitude: res.latitude, longitude: res.longitude });
        setTimeout(() => setLocationStatus(''), 4000);
      } else {
        setLocationStatus(`⚠️ ${res.message}. Please enter city manually.`);
        setTimeout(() => setLocationStatus(''), 4000);
      }
    } catch (e) {
      setLocationStatus('⚠️ Location unavailable. Please enter your city.');
      setTimeout(() => setLocationStatus(''), 4000);
    } finally {
      setLocating(false);
    }
  };

  // Filtered suggestions as user types
  const filteredSuggestions = useMemo(() => {
    const q = cityInput.trim().toLowerCase();
    if (!q) return SUGGESTED_CITIES.slice(0, 8);
    return SUGGESTED_CITIES.filter((c) => c.toLowerCase().includes(q));
  }, [cityInput]);

  // Filter theatres/shows by movie title or format
  const displayTheatres = useMemo(() => {
    return theatres
      .map((t) => {
        let matchingShows = t.shows || [];

        if (movieFilter.trim()) {
          const q = movieFilter.trim().toLowerCase();
          matchingShows = matchingShows.filter((s) =>
            (s.movieTitle || '').toLowerCase().includes(q)
          );
        }

        if (formatFilter === 'IMAX') {
          matchingShows = matchingShows.filter((s) =>
            (s.screenName || '').toLowerCase().includes('imax')
          );
        } else if (formatFilter === 'ATMOS') {
          matchingShows = matchingShows.filter((s) =>
            (s.screenName || '').toLowerCase().includes('atmos')
          );
        }

        return {
          ...t,
          shows: matchingShows,
        };
      })
      .filter((t) => (movieFilter.trim() ? t.shows.length > 0 : true));
  }, [theatres, movieFilter, formatFilter]);

  const totalShowsCount = useMemo(() => {
    return displayTheatres.reduce((acc, t) => acc + (t.shows?.length || 0), 0);
  }, [displayTheatres]);

  return (
    <div style={{ marginTop: '16px' }}>
      {/* =========================================================================
          SIMPLIFIED CITY SEARCH & LOCATION STRIP
          Clean, elegant, no cluttered city pills!
          ========================================================================= */}
      <div
        style={{
          background: 'linear-gradient(180deg, rgba(22, 31, 48, 0.98) 0%, rgba(15, 23, 42, 0.95) 100%)',
          border: '1px solid rgba(56, 189, 248, 0.25)',
          borderRadius: '14px',
          padding: '20px 24px',
          marginBottom: '24px',
          boxShadow: '0 8px 28px rgba(0, 0, 0, 0.4)',
        }}
      >
        <div style={{ display: 'flex', gap: '14px', flexWrap: 'wrap', alignItems: 'center' }}>
          {/* City Input & Autocomplete Form */}
          <div ref={dropdownRef} style={{ flex: '1 1 320px', position: 'relative' }}>
            <form onSubmit={handleCitySubmit} style={{ display: 'flex', width: '100%' }}>
              <span
                style={{
                  position: 'absolute',
                  left: '16px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  fontSize: '18px',
                  color: '#38bdf8',
                  pointerEvents: 'none',
                }}
              >
                🏙️
              </span>
              <input
                type="text"
                value={cityInput}
                onChange={(e) => {
                  setCityInput(e.target.value);
                  setShowDropdown(true);
                }}
                onFocus={() => setShowDropdown(true)}
                placeholder="Search city (e.g., Indore, Bhopal, Mumbai, Delhi, Bengaluru, Pune)..."
                style={{
                  width: '100%',
                  padding: '13px 44px 13px 48px',
                  fontSize: '15px',
                  fontWeight: 600,
                  background: 'rgba(10, 14, 23, 0.9)',
                  border: '1px solid rgba(56, 189, 248, 0.35)',
                  borderRadius: '10px',
                  color: '#fff',
                  outline: 'none',
                  boxShadow: 'inset 0 2px 4px rgba(0,0,0,0.4)',
                }}
              />
              {cityInput && (
                <button
                  type="button"
                  onClick={() => {
                    setCityInput('');
                    setShowDropdown(true);
                  }}
                  style={{
                    position: 'absolute',
                    right: '14px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'transparent',
                    border: 'none',
                    color: '#94a3b8',
                    cursor: 'pointer',
                    fontSize: '16px',
                  }}
                  title="Clear"
                >
                  ✕
                </button>
              )}
            </form>

            {/* Smart Suggestions Dropdown */}
            {showDropdown && filteredSuggestions.length > 0 && (
              <div
                style={{
                  position: 'absolute',
                  top: 'calc(100% + 6px)',
                  left: 0,
                  right: 0,
                  background: '#0f172a',
                  border: '1px solid rgba(56, 189, 248, 0.3)',
                  borderRadius: '10px',
                  zIndex: 200,
                  maxHeight: '260px',
                  overflowY: 'auto',
                  boxShadow: '0 12px 30px rgba(0,0,0,0.7)',
                }}
              >
                <div style={{ padding: '8px 14px', fontSize: '11px', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                  Select or type city name:
                </div>
                {filteredSuggestions.map((c) => (
                  <div
                    key={c}
                    onClick={() => handleSelectCitySuggestion(c)}
                    style={{
                      padding: '10px 16px',
                      cursor: 'pointer',
                      fontSize: '14px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      borderBottom: '1px solid rgba(255,255,255,0.04)',
                      background: selectedCity === c ? 'rgba(56, 189, 248, 0.15)' : 'transparent',
                      color: selectedCity === c ? '#38bdf8' : '#e2e8f0',
                      transition: 'background 0.15s ease',
                    }}
                    onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(56, 189, 248, 0.12)')}
                    onMouseLeave={(e) =>
                      (e.currentTarget.style.background = selectedCity === c ? 'rgba(56, 189, 248, 0.15)' : 'transparent')
                    }
                  >
                    <span>📍 {c}</span>
                    <span style={{ fontSize: '11px', color: '#38bdf8' }}>View Shows →</span>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Action buttons */}
          <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
            <button
              className="btn"
              onClick={handleCitySubmit}
              style={{ padding: '12px 22px', fontSize: '14px', fontWeight: 700 }}
            >
              🔍 Find Shows
            </button>
            <button
              className="btn btn-radar"
              onClick={handleGpsDetect}
              disabled={locating}
              style={{ padding: '12px 18px', fontSize: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}
              title="Detect device GPS location"
            >
              <span>{locating ? '📡' : '📍'}</span>
              <span>{locating ? 'Detecting...' : 'My Location'}</span>
            </button>
          </div>
        </div>

        {/* Location alert */}
        {locationStatus && (
          <div
            style={{
              marginTop: '12px',
              padding: '8px 14px',
              background: 'rgba(56, 189, 248, 0.1)',
              border: '1px solid rgba(56, 189, 248, 0.25)',
              borderRadius: '8px',
              color: '#38bdf8',
              fontSize: '13px',
            }}
          >
            {locationStatus}
          </div>
        )}

        {/* Selected City Indicator */}
        <div
          style={{
            marginTop: '14px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '10px',
            fontSize: '13px',
            color: '#94a3b8',
            borderTop: '1px solid rgba(255, 255, 255, 0.06)',
            paddingTop: '12px',
          }}
        >
          <div>
            {selectedCity ? (
              <>
                Showing live cinema schedules in{' '}
                <strong style={{ color: '#38bdf8', fontSize: '15px' }}>📍 {selectedCity}</strong>
                <span style={{ marginLeft: '10px', color: '#10b981', fontWeight: 600 }}>
                  • {theatres.length} Theatres • {totalShowsCount} Shows Available
                </span>
              </>
            ) : (
              <span style={{ color: '#fbbf24', fontWeight: 600 }}>
                📍 Please search or select a city above, or click Detect GPS to discover nearby cinema shows
              </span>
            )}
          </div>

          {/* Quick filter within the city */}
          {selectedCity && (
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              <input
                type="text"
                placeholder="Filter movie title in this city..."
                value={movieFilter}
                onChange={(e) => setMovieFilter(e.target.value)}
                style={{
                  padding: '6px 12px',
                  fontSize: '12px',
                  background: 'rgba(0,0,0,0.4)',
                  border: '1px solid rgba(255,255,255,0.12)',
                  borderRadius: '6px',
                  color: '#fff',
                  width: '180px',
                }}
              />
              {['ALL', 'IMAX', 'ATMOS'].map((fmt) => (
                <button
                  key={fmt}
                  className={`pill ${formatFilter === fmt ? 'active' : ''}`}
                  style={{ fontSize: '11px', padding: '4px 8px' }}
                  onClick={() => setFormatFilter(fmt)}
                >
                  {fmt === 'ALL' ? 'All' : fmt}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Date & Search Radius Controls */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '12px',
          marginBottom: '20px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          <div className="pill-container">
            {dateOptions.map((opt) => (
              <button
                key={opt.iso}
                className={`pill ${selectedDate === opt.iso ? 'active' : ''}`}
                style={{ padding: '8px 16px', fontSize: '13px', fontWeight: selectedDate === opt.iso ? 700 : 500 }}
                onClick={() => setSelectedDate(opt.iso)}
              >
                📅 {opt.label}
              </button>
            ))}
          </div>

          {/* User-configurable search radius */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
              background: 'rgba(15, 23, 42, 0.8)',
              padding: '4px 8px',
              borderRadius: '8px',
              border: '1px solid rgba(56, 189, 248, 0.25)',
            }}
          >
            <span style={{ fontSize: '11px', color: '#94a3b8', textTransform: 'uppercase', marginRight: '4px' }}>
              📍 Radius:
            </span>
            {[10, 20, 30].map((rad) => (
              <button
                key={rad}
                className={`pill ${searchRadius === rad ? 'active' : ''}`}
                style={{
                  padding: '4px 10px',
                  fontSize: '12px',
                  fontWeight: searchRadius === rad ? 700 : 500,
                  background: searchRadius === rad ? '#38bdf8' : 'transparent',
                  color: searchRadius === rad ? '#000' : '#cbd5e1',
                  border: 'none',
                }}
                onClick={() => setSearchRadius(rad)}
                title={`Filter cinemas strictly within ${rad} km`}
              >
                {rad} km{rad === 10 ? ' (Default)' : ''}
              </button>
            ))}
          </div>
        </div>

        <div style={{ color: '#64748b', fontSize: '12px' }}>
          💡 Sorted strictly by distance • Click showtime to book
        </div>
      </div>

      {/* =========================================================================
          THEATRES & REAL SHOWTIMES LISTING
          ========================================================================= */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: '#94a3b8' }}>
          <div style={{ fontSize: '42px', marginBottom: '12px', animation: 'spin 2s linear infinite' }}>🍿</div>
          <p style={{ fontSize: '16px', fontWeight: 600 }}>Loading nearby theatres and shows within {searchRadius} km...</p>
        </div>
      ) : displayTheatres.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '48px 24px', margin: '20px 0' }}>
          <div style={{ fontSize: '48px', marginBottom: '14px' }}>🏛️</div>
          {!selectedCity ? (
            <>
              <h3 style={{ fontSize: '1.35rem', fontWeight: 700, color: '#f8fafc' }}>
                📍 Choose Your City to View Live Cinema Shows
              </h3>
              <p style={{ color: '#94a3b8', margin: '10px auto 22px', maxWidth: '520px', fontSize: '14px', lineHeight: '1.6' }}>
                Search any Indian city above (e.g. <strong>Indore, Bhopal, Delhi NCR, Pune, Bengaluru</strong>) or click Detect GPS to locate cinema screens near you.
              </p>
              <div style={{ display: 'flex', gap: '10px', justifyContent: 'center', flexWrap: 'wrap', maxWidth: '600px', margin: '0 auto 20px' }}>
                {['Indore', 'Bhopal', 'Delhi NCR', 'Bengaluru', 'Pune', 'Hyderabad', 'Mumbai', 'Ahmedabad', 'Jaipur', 'Lucknow'].map((c) => (
                  <button
                    key={c}
                    className="pill"
                    style={{ padding: '6px 14px', fontSize: '13px' }}
                    onClick={() => handleSelectCitySuggestion(c)}
                  >
                    📍 {c}
                  </button>
                ))}
              </div>
              <button
                className="btn"
                style={{ padding: '10px 20px', fontSize: '14px' }}
                onClick={handleGpsDetect}
                disabled={locating}
              >
                {locating ? '🛰️ Detecting Device Location...' : '📍 Detect My Current Location'}
              </button>
            </>
          ) : (
            <>
              <h3 style={{ fontSize: '1.25rem', color: '#f87171' }}>
                No cinema screens found within {searchRadius} km for "{selectedCity}"
              </h3>
              <p style={{ color: '#94a3b8', margin: '10px auto 20px', maxWidth: '480px', fontSize: '14px', lineHeight: '1.6' }}>
                There are no active shows found within your {searchRadius} km radius limit on this date.
                Expand your search radius to 20 km or 30 km to view theatres in surrounding areas.
              </p>
              <div style={{ display: 'flex', gap: '10px', justifyContent: 'center', flexWrap: 'wrap' }}>
                {searchRadius < 20 && (
                  <button
                    className="btn"
                    onClick={() => setSearchRadius(20)}
                  >
                    📍 Expand to 20 km
                  </button>
                )}
                {searchRadius < 30 && (
                  <button
                    className="btn"
                    onClick={() => setSearchRadius(30)}
                  >
                    📍 Expand to 30 km
                  </button>
                )}
                <button
                  className="btn btn-secondary"
                  onClick={() => {
                    setMovieFilter('');
                    setFormatFilter('ALL');
                    setSelectedDate(dateOptions[1].iso); // Tomorrow
                  }}
                >
                  📅 Check Tomorrow
                </button>
                <button
                  className="btn btn-secondary"
                  onClick={() => {
                    setCityInput('');
                    if (onCityChange) onCityChange('');
                  }}
                >
                  🔍 Change City
                </button>
              </div>
            </>
          )}
        </div>
      ) : (
        <div>
          {displayTheatres.map((theatre, index) => {
            const isClosest = index === 0;
            const shows = theatre.shows || [];

            return (
              <div
                key={theatre.theatreId}
                className={`theatre-card ${isClosest ? 'closest' : ''}`}
                style={{ padding: '22px 24px', marginBottom: '20px' }}
              >
                {/* Theatre Header */}
                <div className="theatre-header">
                  <div>
                    <div className="theatre-name" style={{ fontSize: '1.2rem', display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                      <span>🏛️ {theatre.theatreName}</span>
                      {isClosest && (
                        <span className="closest-badge">🌟 CLOSEST TO YOU</span>
                      )}
                      <span
                        style={{
                          fontSize: '11px',
                          fontWeight: 700,
                          padding: '2px 8px',
                          borderRadius: '4px',
                          background: 'rgba(16, 185, 129, 0.15)',
                          color: '#34d399',
                          border: '1px solid rgba(16, 185, 129, 0.3)',
                        }}
                      >
                        🟢 Online Booking Active
                      </span>
                    </div>
                    <div className="theatre-address" style={{ margin: '4px 0 8px', color: '#94a3b8' }}>
                      📍 {theatre.address}
                    </div>

                    {/* Facility Tags */}
                    <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                      <span className="bms-facility-badge">📱 M-Ticket</span>
                      <span className="bms-facility-badge">🍿 F&amp;B Available</span>
                      <span className="bms-facility-badge" style={{ color: '#38bdf8' }}>
                        🔊 Dolby Atmos
                      </span>
                      <span className="bms-facility-badge" style={{ color: '#fbbf24' }}>
                        📽️ IMAX Laser
                      </span>
                    </div>
                  </div>

                  <div style={{ textAlign: 'right' }}>
                    <div className="theatre-distance" style={{ fontSize: '13px', color: '#38bdf8', fontWeight: 700 }}>
                      📍 {theatre.distanceKm ? `${theatre.distanceKm} km away` : 'Near you'}
                    </div>
                    {theatre.latitude && theatre.longitude && (
                      <a
                        href={`https://www.google.com/maps/dir/?api=1&destination=${theatre.latitude},${theatre.longitude}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '4px',
                          fontSize: '11px',
                          color: '#94a3b8',
                          textDecoration: 'none',
                          marginTop: '6px',
                          background: 'rgba(255,255,255,0.06)',
                          padding: '3px 8px',
                          borderRadius: '4px',
                        }}
                      >
                        🧭 Directions ↗
                      </a>
                    )}
                  </div>
                </div>

                {/* Shows Grid */}
                {shows.length === 0 ? (
                  <p style={{ color: '#94a3b8', fontSize: '13px', margin: '14px 0' }}>
                    No shows matching filters for this theatre today.
                  </p>
                ) : (
                  <div className="shows-grid" style={{ marginTop: '16px' }}>
                    {shows.map((show) => {
                      const showId = show.showId || show.id;
                      const showDate = new Date(show.showTime);
                      const timeStr = showDate.toLocaleTimeString('en-US', {
                        hour: '2-digit',
                        minute: '2-digit',
                      });

                      const availCount = show.availableSeats != null ? show.availableSeats : 86;
                      const isFillingFast = availCount > 0 && availCount <= 12;
                      const isAlmostFull = availCount > 0 && availCount <= 4;

                      return (
                        <button
                          key={showId}
                          className="bms-showtime-btn"
                          onClick={() =>
                            onSelectShow({
                              ...show,
                              id: showId,
                              theatreName: theatre.theatreName,
                              theatreAddress: theatre.address,
                              theatreCity: theatre.city,
                            })
                          }
                          title={`Click to select seats for ${show.movieTitle} at ${timeStr}`}
                        >
                          <div style={{ fontSize: '16px', fontWeight: 800, color: '#fff' }}>
                            {timeStr}
                          </div>
                          <div
                            style={{
                              fontSize: '13px',
                              color: '#38bdf8',
                              fontWeight: 700,
                              margin: '3px 0 2px',
                              maxWidth: '140px',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              whiteSpace: 'nowrap',
                            }}
                            title={show.movieTitle}
                          >
                            {show.movieTitle}
                          </div>
                          <div style={{ fontSize: '11px', color: '#64748b', marginBottom: '6px' }}>
                            {show.screenName}
                          </div>

                          <div
                            style={{
                              display: 'flex',
                              justifyContent: 'space-between',
                              width: '100%',
                              borderTop: '1px solid rgba(255,255,255,0.08)',
                              paddingTop: '6px',
                              fontSize: '11px',
                            }}
                          >
                            {isAlmostFull ? (
                              <span className="bms-status-full">🔴 Almost Full</span>
                            ) : isFillingFast ? (
                              <span className="bms-status-fast">🟡 Fast ({availCount})</span>
                            ) : (
                              <span className="bms-status-avail">🟢 {availCount} seats</span>
                            )}
                            <span style={{ color: '#fbbf24', fontWeight: 700 }}>₹{show.basePrice}</span>
                          </div>
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
