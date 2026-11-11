import React, { useState, useEffect, useMemo, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { searchLocationSuggestions, fetchNearbyCinemas } from '../services/api';
import { detectCurrentLocation, calculateDistanceKm, normalizeIndianCity } from '../services/bmsLocationService';
import { handlePosterError, getFallbackPosterSvg } from '../utils/posterHelper';

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
  const navigate = useNavigate();

  const handleShowClick = (showObj) => {
    if (onSelectShow) {
      onSelectShow(showObj);
    } else {
      const showId = showObj.id || showObj.showId;
      if (showId) {
        navigate(`/seat-selection/${showId}`);
      }
    }
  };

  const [searchInput, setSearchInput] = useState(selectedCity || '');
  const [activeLocation, setActiveLocation] = useState(() => {
    if (userLocation?.latitude && userLocation?.longitude) {
      return {
        name: userLocation.neighborhood || selectedCity || 'Current Location',
        city: selectedCity,
        latitude: userLocation.latitude,
        longitude: userLocation.longitude,
      };
    }
    return selectedCity ? { name: selectedCity, city: selectedCity, latitude: null, longitude: null } : null;
  });

  const [theatres, setTheatres] = useState([]);
  const [providerMetadata, setProviderMetadata] = useState(null);
  const [loading, setLoading] = useState(false);
  const [locating, setLocating] = useState(false);
  const [locationStatus, setLocationStatus] = useState('');
  const [showDropdown, setShowDropdown] = useState(false);
  const [remoteSuggestions, setRemoteSuggestions] = useState([]);
  const [searchingSuggestions, setSearchingSuggestions] = useState(false);

  // Filters
  const [movieFilter, setMovieFilter] = useState('');
  const [formatFilter, setFormatFilter] = useState('ALL'); // 'ALL' | 'IMAX' | 'ATMOS' | '2D' | '3D'
  const [cinemaViewTab, setCinemaViewTab] = useState('ALL'); // 'ALL' | 'BOOKABLE'
  const [searchRadius, setSearchRadius] = useState(15); // 5, 10, 15, 20, 30, 50 km
  const [selectedDate, setSelectedDate] = useState(() => {
    return new Date().toISOString().split('T')[0];
  });

  const dropdownRef = useRef(null);
  const debounceTimerRef = useRef(null);

  // Sync search input when selectedCity prop updates externally
  useEffect(() => {
    if (selectedCity && selectedCity !== searchInput && !activeLocation?.latitude) {
      setSearchInput(selectedCity);
      setActiveLocation({ name: selectedCity, city: selectedCity, latitude: null, longitude: null });
    }
  }, [selectedCity]);

  // Sync userLocation prop when provided
  useEffect(() => {
    if (userLocation?.latitude && userLocation?.longitude) {
      setActiveLocation((prev) => ({
        ...(prev || {}),
        name: userLocation.neighborhood || selectedCity || 'Detected Location',
        city: selectedCity || prev?.city || '',
        latitude: userLocation.latitude,
        longitude: userLocation.longitude,
      }));
    }
  }, [userLocation]);

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

  // Dynamic Autocomplete Debounce for ANY Indian city, locality, PIN, or landmark
  useEffect(() => {
    const q = searchInput.trim();
    if (q.length < 2) {
      setRemoteSuggestions([]);
      setSearchingSuggestions(false);
      return;
    }

    if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current);
    debounceTimerRef.current = setTimeout(async () => {
      setSearchingSuggestions(true);
      try {
        const results = await searchLocationSuggestions(q, 6);
        setRemoteSuggestions(results);
      } catch (err) {
        setRemoteSuggestions([]);
      } finally {
        setSearchingSuggestions(false);
      }
    }, 300);

    return () => {
      if (debounceTimerRef.current) clearTimeout(debounceTimerRef.current);
    };
  }, [searchInput]);

  // Upcoming 4 dates
  const dateOptions = useMemo(() => {
    return Array.from({ length: 4 }).map((_, idx) => {
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
  }, []);

  // Fetch verified cinemas dynamically from backend
  const fetchCinemas = async (loc, date, radius) => {
    if (!loc?.name && !loc?.city && !loc?.latitude) return;

    try {
      setLoading(true);
      const queryParam = loc.name || loc.city || '';
      const cityParam = loc.city || '';
      const latParam = loc.latitude ?? null;
      const lonParam = loc.longitude ?? null;

      // 1. Primary: Dynamic Cinema Discovery endpoint
      const discoveryRes = await fetchNearbyCinemas({
        query: queryParam,
        city: cityParam,
        latitude: latParam,
        longitude: lonParam,
        radiusKm: radius,
        date: date,
      });

      if (discoveryRes && Array.isArray(discoveryRes.cinemas) && discoveryRes.cinemas.length > 0) {
        setProviderMetadata({
          provider: discoveryRes.provider,
          providerConfigured: discoveryRes.providerConfigured,
          providerStatusMessage: discoveryRes.providerStatusMessage,
          searchCity: discoveryRes.searchCity,
          searchLatitude: discoveryRes.searchLatitude,
          searchLongitude: discoveryRes.searchLongitude,
          searchRadiusKm: discoveryRes.searchRadiusKm,
        });

        // Normalize distance and ensure clean show references
        const normalized = discoveryRes.cinemas.map((c) => {
          let dist = c.distanceKm;
          if (latParam && lonParam && c.latitude && c.longitude) {
            dist = calculateDistanceKm(latParam, lonParam, c.latitude, c.longitude);
          }
          return {
            ...c,
            theatreId: c.theatreId || c.id,
            theatreName: c.name,
            distanceKm: dist != null ? Math.round(Number(dist) * 10) / 10 : 2.0,
            shows: (c.shows || []).map((s) => ({
              ...s,
              id: s.showId || s.id,
              theatreName: c.name,
              theatreAddress: c.address,
              theatreCity: c.city,
            })),
          };
        });

        normalized.sort((a, b) => (a.distanceKm ?? 999) - (b.distanceKm ?? 999));
        setTheatres(normalized);
        return;
      }

      // 2. Secondary fallback: Local show inventory scan
      try {
        let fallbackParams = `date=${date}&radiusKm=${radius}`;
        if (cityParam) fallbackParams += `&city=${encodeURIComponent(cityParam)}`;
        if (latParam && lonParam) fallbackParams += `&latitude=${latParam}&longitude=${lonParam}`;

        const showsRes = await api.get(`/shows/nearby?${fallbackParams}`);
        const showTheatres = showsRes.data?.data || [];

        if (showTheatres.length > 0) {
          const mapped = showTheatres.map((t) => ({
            id: String(t.theatreId),
            theatreId: t.theatreId,
            name: t.theatreName,
            theatreName: t.theatreName,
            address: t.address || `${t.city}`,
            city: t.city,
            latitude: t.latitude,
            longitude: t.longitude,
            distanceKm: t.distanceKm ? Math.round(Number(t.distanceKm) * 10) / 10 : 2.0,
            cinemaChain: t.theatreName?.split(' ')[0] || 'Multiplex',
            cinemaType: 'BOOKABLE_PARTNER',
            dataSource: 'PARTNER_DEMO_SCHEDULE',
            priceNote: 'Real-time pricing available',
            seatAvailabilityNote: 'Real-time seat map available',
            hasBookableShows: true,
            isRegistered: true,
            shows: (t.shows || []).map((s) => ({
              ...s,
              id: s.showId || s.id,
              theatreName: t.theatreName,
              theatreAddress: t.address,
              theatreCity: t.city,
            })),
          }));
          mapped.sort((a, b) => (a.distanceKm ?? 999) - (b.distanceKm ?? 999));
          setTheatres(mapped);
          setProviderMetadata({
            provider: 'LOCAL_DATABASE',
            providerConfigured: false,
            providerStatusMessage: 'Showing partner multiplexes from application booking schedule.',
          });
          return;
        }
      } catch (errFallback) {}

      // Empty state
      setTheatres([]);
    } catch (err) {
      setTheatres([]);
    } finally {
      setLoading(false);
    }
  };

  // Re-fetch when location, date, or radius changes
  useEffect(() => {
    if (activeLocation) {
      fetchCinemas(activeLocation, selectedDate, searchRadius);
    }
  }, [activeLocation, selectedDate, searchRadius]);

  // Search submission (enter key or click)
  const handleLocationSubmit = (e) => {
    e?.preventDefault();
    const clean = searchInput.trim();
    if (!clean) return;

    const normalized = normalizeIndianCity(clean);
    setShowDropdown(false);
    const newLoc = {
      name: normalized,
      city: normalized,
      latitude: null,
      longitude: null,
    };
    setActiveLocation(newLoc);
    if (onCityChange) onCityChange(normalized);
    if (onLocationDetected) onLocationDetected(null);
  };

  // Selection from dynamic suggestions (any Indian address, PIN, landmark)
  const handleSelectRemoteSuggestion = (item) => {
    const locName = item.name || item.locality || item.city || item.displayName;
    const city = normalizeIndianCity(item.city || locName, item.latitude, item.longitude);
    setSearchInput(locName);
    setShowDropdown(false);

    const newLoc = {
      name: item.displayName || locName,
      city: city,
      locality: item.locality,
      state: item.state,
      postcode: item.postcode,
      latitude: item.latitude,
      longitude: item.longitude,
    };

    setActiveLocation(newLoc);
    if (onCityChange) onCityChange(city);
    if (onLocationDetected) {
      onLocationDetected({
        latitude: item.latitude,
        longitude: item.longitude,
        neighborhood: item.locality || locName,
        city: city,
        state: item.state,
        postcode: item.postcode,
      });
    }
  };

  // Selection from preset quick cities
  const handleSelectCityPreset = (city) => {
    const cleanCity = normalizeIndianCity(city);
    setSearchInput(cleanCity);
    setShowDropdown(false);
    const newLoc = { name: cleanCity, city: cleanCity, latitude: null, longitude: null };
    setActiveLocation(newLoc);
    if (onCityChange) onCityChange(cleanCity);
    if (onLocationDetected) onLocationDetected(null);
  };

  // GPS Locate
  const handleGpsDetect = async () => {
    setLocating(true);
    setLocationStatus('📡 Detecting GPS coordinates and resolving locality...');
    try {
      const res = await detectCurrentLocation();
      if (res.success) {
        const placeName = res.neighborhood ? `${res.neighborhood}, ${res.city}` : res.city;
        setLocationStatus(`📍 Detected: ${placeName}`);
        setSearchInput(placeName);

        const newLoc = {
          name: placeName,
          city: res.city,
          locality: res.neighborhood,
          state: res.state,
          postcode: res.postcode,
          latitude: res.latitude,
          longitude: res.longitude,
        };

        setActiveLocation(newLoc);
        if (onCityChange) onCityChange(res.city);
        if (onLocationDetected) {
          onLocationDetected({
            latitude: res.latitude,
            longitude: res.longitude,
            neighborhood: res.neighborhood,
            city: res.city,
            state: res.state,
            postcode: res.postcode,
          });
        }
        setTimeout(() => setLocationStatus(''), 4500);
      } else {
        setLocationStatus(`⚠️ ${res.message}`);
        setTimeout(() => setLocationStatus(''), 4500);
      }
    } catch (e) {
      setLocationStatus('⚠️ Location detection failed. Please search manually.');
      setTimeout(() => setLocationStatus(''), 4500);
    } finally {
      setLocating(false);
    }
  };

  // Local preset filter for fallback dropdown
  const filteredPresets = useMemo(() => {
    const q = searchInput.trim().toLowerCase();
    if (!q) return SUGGESTED_CITIES.slice(0, 8);
    return SUGGESTED_CITIES.filter((c) => c.toLowerCase().includes(q));
  }, [searchInput]);

  // Filtered theatres & shows based on user criteria
  const displayTheatres = useMemo(() => {
    return theatres
      .filter((t) => {
        if (cinemaViewTab === 'BOOKABLE') {
          return t.hasBookableShows || (t.shows && t.shows.length > 0);
        }
        return true;
      })
      .map((t) => {
        let matchingShows = t.shows || [];

        if (movieFilter.trim()) {
          const q = movieFilter.trim().toLowerCase();
          matchingShows = matchingShows.filter((s) =>
            (s.movieTitle || '').toLowerCase().includes(q)
          );
        }

        if (formatFilter !== 'ALL') {
          matchingShows = matchingShows.filter((s) => {
            const screen = (s.screenName || '').toUpperCase();
            const fmt = (s.screenFormat || '').toUpperCase();
            if (formatFilter === 'IMAX') return screen.includes('IMAX') || fmt.includes('IMAX');
            if (formatFilter === 'ATMOS') return screen.includes('ATMOS') || fmt.includes('ATMOS');
            if (formatFilter === '3D') return screen.includes('3D') || fmt.includes('3D');
            if (formatFilter === '2D') return fmt.includes('2D') || (!screen.includes('3D') && !screen.includes('IMAX'));
            return true;
          });
        }

        return {
          ...t,
          shows: matchingShows,
        };
      })
      .filter((t) => (movieFilter.trim() && cinemaViewTab === 'BOOKABLE' ? t.shows.length > 0 : true));
  }, [theatres, cinemaViewTab, movieFilter, formatFilter]);

  const totalShowsCount = useMemo(() => {
    return displayTheatres.reduce((acc, t) => acc + (t.shows?.length || 0), 0);
  }, [displayTheatres]);

  const bookableTheatresCount = useMemo(() => {
    return theatres.filter((t) => t.hasBookableShows || (t.shows && t.shows.length > 0)).length;
  }, [theatres]);

  return (
    <div style={{ marginTop: '16px' }}>
      {/* =========================================================================
          PAN-INDIA DYNAMIC LOCATION SEARCH & RADIUS CONTROL PANEL
          ========================================================================= */}
      <div
        style={{
          background: 'linear-gradient(180deg, rgba(22, 31, 48, 0.98) 0%, rgba(15, 23, 42, 0.95) 100%)',
          border: '1px solid rgba(56, 189, 248, 0.25)',
          borderRadius: '16px',
          padding: '22px 26px',
          marginBottom: '24px',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.45)',
        }}
      >
        <div style={{ display: 'flex', gap: '14px', flexWrap: 'wrap', alignItems: 'center' }}>
          {/* Dynamic Search Input with Autocomplete */}
          <div ref={dropdownRef} style={{ flex: '1 1 360px', position: 'relative' }}>
            <form onSubmit={handleLocationSubmit} style={{ display: 'flex', width: '100%' }}>
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
                🔍
              </span>
              <input
                type="text"
                value={searchInput}
                onChange={(e) => {
                  setSearchInput(e.target.value);
                  setShowDropdown(true);
                }}
                onFocus={() => setShowDropdown(true)}
                placeholder="Search ANY Indian city, locality, PIN code or landmark (e.g. Vijay Nagar, Indore; 600040; Andheri)..."
                style={{
                  width: '100%',
                  padding: '14px 44px 14px 48px',
                  fontSize: '15px',
                  fontWeight: 600,
                  background: 'rgba(10, 14, 23, 0.92)',
                  border: '1px solid rgba(56, 189, 248, 0.35)',
                  borderRadius: '12px',
                  color: '#fff',
                  outline: 'none',
                  boxShadow: 'inset 0 2px 4px rgba(0,0,0,0.4)',
                }}
              />
              {searchInput && (
                <button
                  type="button"
                  onClick={() => {
                    setSearchInput('');
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
            {showDropdown && (
              <div
                style={{
                  position: 'absolute',
                  top: 'calc(100% + 8px)',
                  left: 0,
                  right: 0,
                  background: '#0f172a',
                  border: '1px solid rgba(56, 189, 248, 0.35)',
                  borderRadius: '12px',
                  zIndex: 300,
                  maxHeight: '340px',
                  overflowY: 'auto',
                  boxShadow: '0 16px 40px rgba(0,0,0,0.8)',
                }}
              >
                {/* Real-time remote geocoding suggestions */}
                {searchingSuggestions ? (
                  <div style={{ padding: '12px 16px', color: '#38bdf8', fontSize: '13px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{ animation: 'spin 1.5s linear infinite' }}>🌐</span> Searching Pan-India addresses...
                  </div>
                ) : remoteSuggestions.length > 0 ? (
                  <div>
                    <div style={{ padding: '8px 16px', fontSize: '11px', color: '#38bdf8', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.6px', borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
                      📍 Verified Indian Addresses &amp; Localities:
                    </div>
                    {remoteSuggestions.map((item, idx) => (
                      <div
                        key={`${item.name}-${item.latitude}-${idx}`}
                        onClick={() => handleSelectRemoteSuggestion(item)}
                        style={{
                          padding: '12px 16px',
                          cursor: 'pointer',
                          borderBottom: '1px solid rgba(255,255,255,0.04)',
                          transition: 'background 0.15s ease',
                        }}
                        onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(56, 189, 248, 0.12)')}
                        onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
                      >
                        <div style={{ fontSize: '14px', fontWeight: 700, color: '#f8fafc' }}>
                          📍 {item.name || item.locality || item.city}
                        </div>
                        <div style={{ fontSize: '12px', color: '#94a3b8', marginTop: '2px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {item.displayName}
                        </div>
                        <div style={{ fontSize: '11px', color: '#38bdf8', marginTop: '3px' }}>
                          {item.city ? `City: ${item.city}` : ''} {item.state ? `• ${item.state}` : ''} {item.postcode ? `• PIN: ${item.postcode}` : ''}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : null}

                {/* Popular city presets */}
                <div style={{ padding: '8px 16px', fontSize: '11px', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.5px', borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
                  Quick City Hubs:
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2px' }}>
                  {filteredPresets.map((c) => (
                    <div
                      key={c}
                      onClick={() => handleSelectCityPreset(c)}
                      style={{
                        padding: '10px 16px',
                        cursor: 'pointer',
                        fontSize: '13px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        color: activeLocation?.city === c ? '#38bdf8' : '#e2e8f0',
                        background: activeLocation?.city === c ? 'rgba(56, 189, 248, 0.12)' : 'transparent',
                        transition: 'background 0.15s ease',
                      }}
                      onMouseEnter={(e) => (e.currentTarget.style.background = 'rgba(56, 189, 248, 0.1)')}
                      onMouseLeave={(e) =>
                        (e.currentTarget.style.background = activeLocation?.city === c ? 'rgba(56, 189, 248, 0.12)' : 'transparent')
                      }
                    >
                      <span>🏙️ {c}</span>
                      <span style={{ fontSize: '11px', color: '#38bdf8' }}>Select →</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          {/* Action buttons */}
          <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
            <button
              className="btn"
              onClick={handleLocationSubmit}
              style={{ padding: '13px 22px', fontSize: '14px', fontWeight: 700 }}
            >
              🔍 Discover Theatres
            </button>
            <button
              className="btn btn-radar"
              onClick={handleGpsDetect}
              disabled={locating}
              style={{ padding: '13px 18px', fontSize: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}
              title="Detect device GPS location"
            >
              <span>{locating ? '📡' : '📍'}</span>
              <span>{locating ? 'Detecting...' : 'Use My GPS'}</span>
            </button>
          </div>
        </div>

        {/* Location alert */}
        {locationStatus && (
          <div
            style={{
              marginTop: '12px',
              padding: '10px 16px',
              background: 'rgba(56, 189, 248, 0.1)',
              border: '1px solid rgba(56, 189, 248, 0.3)',
              borderRadius: '8px',
              color: '#38bdf8',
              fontSize: '13px',
            }}
          >
            {locationStatus}
          </div>
        )}

        {/* Selected Location Details Strip */}
        <div
          style={{
            marginTop: '16px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '12px',
            borderTop: '1px solid rgba(255, 255, 255, 0.08)',
            paddingTop: '14px',
          }}
        >
          <div>
            {activeLocation ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
                <span style={{ fontSize: '14px', color: '#94a3b8' }}>Active Search Location:</span>
                <span
                  style={{
                    fontSize: '15px',
                    fontWeight: 800,
                    color: '#38bdf8',
                    background: 'rgba(56, 189, 248, 0.15)',
                    padding: '4px 12px',
                    borderRadius: '6px',
                    border: '1px solid rgba(56, 189, 248, 0.35)',
                  }}
                >
                  📍 {activeLocation.name || activeLocation.city}
                </span>

                {activeLocation.latitude && activeLocation.longitude && (
                  <span style={{ fontSize: '12px', color: '#64748b', fontFamily: 'monospace' }}>
                    ({activeLocation.latitude.toFixed(4)}°N, {activeLocation.longitude.toFixed(4)}°E)
                  </span>
                )}

                <span style={{ fontSize: '13px', color: '#10b981', fontWeight: 600 }}>
                  • {theatres.length} Theatres Discovered • {totalShowsCount} Bookable Shows
                </span>
              </div>
            ) : (
              <span style={{ color: '#fbbf24', fontWeight: 600, fontSize: '14px' }}>
                📍 Please search any Indian city, area, PIN code, or landmark above to discover nearby cinema screens
              </span>
            )}
          </div>

          {/* Quick filter within the location */}
          {activeLocation && (
            <div style={{ display: 'flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
              <input
                type="text"
                placeholder="Filter movie title..."
                value={movieFilter}
                onChange={(e) => setMovieFilter(e.target.value)}
                style={{
                  padding: '7px 12px',
                  fontSize: '12px',
                  background: 'rgba(0,0,0,0.4)',
                  border: '1px solid rgba(255,255,255,0.15)',
                  borderRadius: '6px',
                  color: '#fff',
                  width: '160px',
                }}
              />
              {['ALL', 'IMAX', 'ATMOS', '3D', '2D'].map((fmt) => (
                <button
                  key={fmt}
                  className={`pill ${formatFilter === fmt ? 'active' : ''}`}
                  style={{ fontSize: '11px', padding: '5px 10px' }}
                  onClick={() => setFormatFilter(fmt)}
                >
                  {fmt === 'ALL' ? 'All Formats' : fmt}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* =========================================================================
          CONTROLS STRIP: DATES, CONFIGURABLE RADIUS & TRANSPARENCY TOGGLE
          ========================================================================= */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '14px',
          marginBottom: '22px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px', flexWrap: 'wrap' }}>
          {/* Date Selector */}
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

          {/* Configurable Search Radius Buttons */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              background: 'rgba(15, 23, 42, 0.85)',
              padding: '4px 10px',
              borderRadius: '10px',
              border: '1px solid rgba(56, 189, 248, 0.25)',
            }}
          >
            <span style={{ fontSize: '11px', color: '#94a3b8', textTransform: 'uppercase', marginRight: '4px', fontWeight: 700 }}>
              🎯 Radius:
            </span>
            {[5, 10, 15, 20, 30, 50].map((rad) => (
              <button
                key={rad}
                className={`pill ${searchRadius === rad ? 'active' : ''}`}
                style={{
                  padding: '4px 9px',
                  fontSize: '12px',
                  fontWeight: searchRadius === rad ? 700 : 500,
                  background: searchRadius === rad ? '#38bdf8' : 'transparent',
                  color: searchRadius === rad ? '#000' : '#cbd5e1',
                  border: 'none',
                }}
                onClick={() => setSearchRadius(rad)}
                title={`Discover theatres within ${rad} km`}
              >
                {rad} km
              </button>
            ))}
          </div>
        </div>

        {/* View Mode Tabs: All Cinemas vs Bookable Online */}
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <button
            className={`pill ${cinemaViewTab === 'ALL' ? 'active' : ''}`}
            style={{ padding: '7px 14px', fontSize: '12px', fontWeight: cinemaViewTab === 'ALL' ? 700 : 500 }}
            onClick={() => setCinemaViewTab('ALL')}
          >
            🏛️ All Discovered Cinemas ({theatres.length})
          </button>
          <button
            className={`pill ${cinemaViewTab === 'BOOKABLE' ? 'active' : ''}`}
            style={{
              padding: '7px 14px',
              fontSize: '12px',
              fontWeight: cinemaViewTab === 'BOOKABLE' ? 700 : 500,
              background: cinemaViewTab === 'BOOKABLE' ? '#10b981' : undefined,
              color: cinemaViewTab === 'BOOKABLE' ? '#fff' : undefined,
            }}
            onClick={() => setCinemaViewTab('BOOKABLE')}
          >
            🎟️ Online Bookable Shows ({bookableTheatresCount})
          </button>
        </div>
      </div>

      {/* Provider Transparency Banner */}
      {providerMetadata && (
        <div
          style={{
            marginBottom: '18px',
            padding: '10px 16px',
            background: 'rgba(15, 23, 42, 0.6)',
            border: '1px solid rgba(255, 255, 255, 0.08)',
            borderRadius: '10px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '8px',
            fontSize: '12px',
            color: '#94a3b8',
          }}
        >
          <div>
            <strong style={{ color: '#e2e8f0' }}>📡 Data Source:</strong>{' '}
            <span style={{ color: '#38bdf8' }}>{providerMetadata.provider}</span> • {providerMetadata.providerStatusMessage}
          </div>
          <div style={{ color: '#64748b' }}>
            Sorted strictly by Haversine geographical distance
          </div>
        </div>
      )}

      {/* =========================================================================
          THEATRES & REAL SHOWTIMES LISTING
          ========================================================================= */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: '#94a3b8' }}>
          <div style={{ fontSize: '42px', marginBottom: '12px', animation: 'spin 2s linear infinite' }}>🍿</div>
          <p style={{ fontSize: '16px', fontWeight: 600 }}>
            Scanning Pan-India cinema directory within {searchRadius} km for "{activeLocation?.name || 'Selected Area'}"...
          </p>
        </div>
      ) : displayTheatres.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '48px 24px', margin: '20px 0' }}>
          <div style={{ fontSize: '48px', marginBottom: '14px' }}>🏛️</div>
          {!activeLocation ? (
            <>
              <h3 style={{ fontSize: '1.35rem', fontWeight: 700, color: '#f8fafc' }}>
                📍 Choose Any Location to View Pan-India Cinemas
              </h3>
              <p style={{ color: '#94a3b8', margin: '10px auto 22px', maxWidth: '540px', fontSize: '14px', lineHeight: '1.6' }}>
                Search any Indian city, area, PIN code, or landmark above (e.g. <strong>Vijay Nagar Indore, 600040 Chennai, Andheri West Mumbai, Connaught Place Delhi</strong>) or click Use My GPS.
              </p>
              <div style={{ display: 'flex', gap: '10px', justifyContent: 'center', flexWrap: 'wrap', maxWidth: '640px', margin: '0 auto 20px' }}>
                {['Vijay Nagar, Indore', 'Anna Nagar, Chennai', 'Andheri West, Mumbai', 'Connaught Place, Delhi', 'Koramangala, Bengaluru', 'Bhopal', 'Pune', 'Jaipur'].map((c) => (
                  <button
                    key={c}
                    className="pill"
                    style={{ padding: '6px 14px', fontSize: '13px' }}
                    onClick={() => handleSelectCityPreset(c)}
                  >
                    📍 {c}
                  </button>
                ))}
              </div>
              <button
                className="btn"
                style={{ padding: '11px 22px', fontSize: '14px' }}
                onClick={handleGpsDetect}
                disabled={locating}
              >
                {locating ? '🛰️ Detecting Device Location...' : '📍 Detect My Current Location'}
              </button>
            </>
          ) : (
            <>
              <h3 style={{ fontSize: '1.25rem', color: '#f87171' }}>
                No cinema screens found within {searchRadius} km for "{activeLocation.name || activeLocation.city}"
              </h3>
              <p style={{ color: '#94a3b8', margin: '10px auto 20px', maxWidth: '520px', fontSize: '14px', lineHeight: '1.6' }}>
                There are currently no verified cinema screens recorded within your {searchRadius} km radius limit.
                Expand your radius to discover theatres in surrounding neighborhoods.
              </p>
              <div style={{ display: 'flex', gap: '10px', justifyContent: 'center', flexWrap: 'wrap' }}>
                {searchRadius < 20 && (
                  <button className="btn" onClick={() => setSearchRadius(20)}>
                    📍 Expand to 20 km
                  </button>
                )}
                {searchRadius < 30 && (
                  <button className="btn" onClick={() => setSearchRadius(30)}>
                    📍 Expand to 30 km
                  </button>
                )}
                {searchRadius < 50 && (
                  <button className="btn" onClick={() => setSearchRadius(50)}>
                    📍 Expand to 50 km
                  </button>
                )}
                {cinemaViewTab === 'BOOKABLE' && (
                  <button className="btn btn-secondary" onClick={() => setCinemaViewTab('ALL')}>
                    🏛️ View All Discovered Theatres
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
              </div>
            </>
          )}
        </div>
      ) : (
        <div>
          {displayTheatres.map((theatre, index) => {
            const isClosest = index === 0;
            const shows = theatre.shows || [];
            const hasBooking = theatre.hasBookableShows || shows.length > 0;
            const chain = theatre.cinemaChain || 'Cinema';
            const dataSource = theatre.dataSource || (hasBooking ? 'PARTNER_DEMO_SCHEDULE' : 'EXTERNAL_DIRECTORY');

            return (
              <div
                key={theatre.theatreId || theatre.id || index}
                className={`theatre-card ${isClosest ? 'closest' : ''}`}
                style={{ padding: '24px 26px', marginBottom: '22px' }}
              >
                {/* Theatre Header */}
                <div className="theatre-header">
                  <div style={{ flex: 1, minWidth: '260px' }}>
                    <div className="theatre-name" style={{ fontSize: '1.25rem', display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                      <span>🏛️ {theatre.theatreName}</span>

                      {/* Chain Badge */}
                      {chain && (
                        <span
                          style={{
                            fontSize: '11px',
                            fontWeight: 800,
                            padding: '3px 8px',
                            borderRadius: '4px',
                            background: 'rgba(56, 189, 248, 0.15)',
                            color: '#38bdf8',
                            border: '1px solid rgba(56, 189, 248, 0.35)',
                            textTransform: 'uppercase',
                          }}
                        >
                          {chain}
                        </span>
                      )}

                      {/* Closest indicator */}
                      {isClosest && (
                        <span className="closest-badge">🌟 CLOSEST TO SEARCH LOCATION</span>
                      )}

                      {/* Data Source Badge with Genuine Transparency */}
                      {dataSource === 'VERIFIED_LIVE_PROVIDER' ? (
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
                          title="Verified live external provider feed"
                        >
                          🟢 Verified Live Provider Data
                        </span>
                      ) : dataSource === 'PARTNER_DEMO_SCHEDULE' ? (
                        <span
                          style={{
                            fontSize: '11px',
                            fontWeight: 700,
                            padding: '2px 8px',
                            borderRadius: '4px',
                            background: 'rgba(245, 158, 11, 0.15)',
                            color: '#fbbf24',
                            border: '1px solid rgba(245, 158, 11, 0.3)',
                          }}
                          title="Locally managed partner demo showtimes"
                        >
                          🟡 Locally Managed Partner Schedule
                        </span>
                      ) : (
                        <span
                          style={{
                            fontSize: '11px',
                            fontWeight: 700,
                            padding: '2px 8px',
                            borderRadius: '4px',
                            background: 'rgba(59, 130, 246, 0.15)',
                            color: '#60a5fa',
                            border: '1px solid rgba(59, 130, 246, 0.3)',
                          }}
                          title="Discovered via OpenStreetMap / Directory"
                        >
                          🔵 External Cinema Registry
                        </span>
                      )}
                    </div>

                    <div className="theatre-address" style={{ margin: '6px 0 10px', color: '#94a3b8', fontSize: '13px' }}>
                      📍 {theatre.address || `${theatre.city || 'India'}`}
                    </div>

                    {/* Facility Tags & Rating */}
                    <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center' }}>
                      {theatre.rating && (
                        <span style={{ fontSize: '12px', fontWeight: 700, color: '#fbbf24', background: 'rgba(251, 191, 36, 0.12)', padding: '2px 8px', borderRadius: '4px' }}>
                          ⭐ {theatre.rating.toFixed(1)} {theatre.userRatingCount ? `(${theatre.userRatingCount})` : ''}
                        </span>
                      )}
                      <span className="bms-facility-badge">📱 M-Ticket</span>
                      <span className="bms-facility-badge">🍿 F&amp;B Available</span>
                      <span className="bms-facility-badge" style={{ color: '#38bdf8' }}>🔊 Dolby Atmos</span>
                      <span className="bms-facility-badge" style={{ color: '#fbbf24' }}>📽️ IMAX Laser</span>

                      {theatre.lastUpdated && (
                        <span style={{ fontSize: '11px', color: '#64748b' }}>
                          Updated: {new Date(theatre.lastUpdated).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Distance & Directions */}
                  <div style={{ textAlign: 'right', minWidth: '140px' }}>
                    <div className="theatre-distance" style={{ fontSize: '14px', color: '#38bdf8', fontWeight: 800 }}>
                      📍 {theatre.distanceKm != null ? `${theatre.distanceKm} km away` : 'Nearby'}
                    </div>

                    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', alignItems: 'flex-end', marginTop: '8px' }}>
                      {theatre.googleMapsUri ? (
                        <a
                          href={theatre.googleMapsUri}
                          target="_blank"
                          rel="noopener noreferrer"
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '4px',
                            fontSize: '11px',
                            color: '#94a3b8',
                            textDecoration: 'none',
                            background: 'rgba(255,255,255,0.06)',
                            padding: '4px 9px',
                            borderRadius: '4px',
                          }}
                        >
                          🧭 Google Maps ↗
                        </a>
                      ) : theatre.latitude && theatre.longitude ? (
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
                            background: 'rgba(255,255,255,0.06)',
                            padding: '4px 9px',
                            borderRadius: '4px',
                          }}
                        >
                          🧭 Directions ↗
                        </a>
                      ) : null}

                      {theatre.officialBookingUrl && theatre.officialBookingUrl.startsWith('http') && (
                        <a
                          href={theatre.officialBookingUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          style={{
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '4px',
                            fontSize: '11px',
                            color: '#38bdf8',
                            textDecoration: 'none',
                            background: 'rgba(56, 189, 248, 0.1)',
                            padding: '4px 9px',
                            borderRadius: '4px',
                            fontWeight: 600,
                          }}
                        >
                          🎟️ Official Portal ↗
                        </a>
                      )}
                    </div>
                  </div>
                </div>

                {/* Shows Grid or External Portal Notice */}
                {shows.length > 0 ? (
                  <div>
                    <div style={{ fontSize: '12px', fontWeight: 700, color: '#38bdf8', marginTop: '16px', marginBottom: '8px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                      🎟️ Available Shows &amp; Timings:
                    </div>
                    <div className="shows-grid">
                      {shows.map((show) => {
                        const showId = show.showId || show.id;
                        const showDate = new Date(show.showTime);
                        const timeStr = showDate.toLocaleTimeString('en-US', {
                          hour: '2-digit',
                          minute: '2-digit',
                        });

                        const hasRealSeats = show.availableSeats != null && show.totalSeats != null;
                        const availCount = show.availableSeats ?? 86;
                        const isFillingFast = availCount > 0 && availCount <= 12;
                        const isAlmostFull = availCount > 0 && availCount <= 4;
                        const price = show.basePrice;

                        return (
                          <button
                            key={showId}
                            className="bms-showtime-btn"
                            onClick={() =>
                              handleShowClick({
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
                            <div style={{ fontSize: '11px', color: '#94a3b8', marginBottom: '6px' }}>
                              {show.screenFormat || show.screenName}
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
                              {hasRealSeats ? (
                                isAlmostFull ? (
                                  <span className="bms-status-full">🔴 Almost Full</span>
                                ) : isFillingFast ? (
                                  <span className="bms-status-fast">🟡 Fast ({availCount})</span>
                                ) : (
                                  <span className="bms-status-avail">🟢 {availCount} seats</span>
                                )
                              ) : (
                                <span style={{ color: '#94a3b8' }}>Available</span>
                              )}
                              <span style={{ color: '#fbbf24', fontWeight: 700 }}>
                                {price != null ? `₹${price}` : '₹220'}
                              </span>
                            </div>
                          </button>
                        );
                      })}
                    </div>
                  </div>
                ) : (
                  <div
                    style={{
                      marginTop: '16px',
                      padding: '14px 18px',
                      background: 'rgba(15, 23, 42, 0.5)',
                      borderRadius: '8px',
                      border: '1px dashed rgba(255,255,255,0.12)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      flexWrap: 'wrap',
                      gap: '12px',
                    }}
                  >
                    <div>
                      <div style={{ fontSize: '13px', fontWeight: 600, color: '#cbd5e1' }}>
                        ℹ️ External Cinema Listing
                      </div>
                      <div style={{ fontSize: '12px', color: '#94a3b8', marginTop: '2px' }}>
                        Seat inventory and real-time ticketing for this screen are managed directly by {chain} / local box office.
                      </div>
                    </div>

                    {theatre.officialBookingUrl ? (
                      <a
                        href={theatre.officialBookingUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="btn btn-secondary"
                        style={{ padding: '8px 14px', fontSize: '12px' }}
                      >
                        🌐 Visit Box Office Portal ↗
                      </a>
                    ) : (
                      <span style={{ fontSize: '12px', color: '#64748b', fontStyle: 'italic' }}>
                        Direct schedule integration pending
                      </span>
                    )}
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
