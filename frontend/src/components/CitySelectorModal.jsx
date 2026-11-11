import React, { useState, useEffect, useMemo, useRef } from 'react';
import {
  POPULAR_CITIES,
  INDIAN_REGIONS,
  ALL_INDIAN_CITIES,
  detectCurrentLocation,
  locateByPincodeOrArea,
  normalizeIndianCity,
} from '../services/bmsLocationService';
import { searchLocationSuggestions } from '../services/api';

export default function CitySelectorModal({
  isOpen,
  onClose,
  selectedCity,
  onSelectCity,
  onLocationDetected,
}) {
  const [searchQuery, setSearchQuery] = useState('');
  const [activeTab, setActiveTab] = useState('Popular Metros');
  const [detecting, setDetecting] = useState(false);
  const [statusMsg, setStatusMsg] = useState('');
  const [dynamicSuggestions, setDynamicSuggestions] = useState([]);
  const [searchingRemote, setSearchingRemote] = useState(false);
  const debounceRef = useRef(null);

  const pincodeOrLocalityMatch = useMemo(() => {
    return locateByPincodeOrArea(searchQuery);
  }, [searchQuery]);

  // Debounced dynamic location search for ANY Indian city, locality, PIN code, landmark
  useEffect(() => {
    const q = searchQuery.trim();
    if (q.length < 2) {
      setDynamicSuggestions([]);
      setSearchingRemote(false);
      return;
    }

    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      setSearchingRemote(true);
      try {
        const results = await searchLocationSuggestions(q, 6);
        setDynamicSuggestions(results);
      } catch (err) {
        setDynamicSuggestions([]);
      } finally {
        setSearchingRemote(false);
      }
    }, 300);

    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [searchQuery]);

  // Handle GPS detection
  const handleDetectLocation = async () => {
    setDetecting(true);
    setStatusMsg('📡 Detecting your device coordinates & neighborhood...');
    const res = await detectCurrentLocation();
    setDetecting(false);

    if (res.success) {
      setStatusMsg(`📍 ${res.message}`);
      if (onLocationDetected) {
        onLocationDetected({
          latitude: res.latitude,
          longitude: res.longitude,
          neighborhood: res.neighborhood,
          city: res.city,
          state: res.state,
          postcode: res.postcode
        });
      }
      setTimeout(() => {
        onSelectCity(res.city);
        onClose();
        setStatusMsg('');
      }, 700);
    } else {
      setStatusMsg(`⚠️ ${res.message}`);
    }
  };

  const handleSelectDynamicLocation = (loc) => {
    const rawCity = loc.city || loc.name || searchQuery.trim();
    const canonicalCity = normalizeIndianCity(rawCity, loc.latitude, loc.longitude);
    if (onLocationDetected) {
      onLocationDetected({
        latitude: loc.latitude,
        longitude: loc.longitude,
        neighborhood: loc.locality || loc.name,
        city: canonicalCity,
        state: loc.state,
        postcode: loc.postcode,
      });
    }
    onSelectCity(canonicalCity);
    onClose();
  };

  // Filtered cities based on search
  const filteredCities = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) {
      if (activeTab === 'All (80+)') {
        return ALL_INDIAN_CITIES;
      }
      return INDIAN_REGIONS[activeTab] || POPULAR_CITIES;
    }
    return ALL_INDIAN_CITIES.filter((city) =>
      city.toLowerCase().includes(q)
    );
  }, [searchQuery, activeTab]);

  const isCustomCity =
    searchQuery.trim().length > 1 &&
    !ALL_INDIAN_CITIES.some((c) => c.toLowerCase() === searchQuery.trim().toLowerCase()) &&
    dynamicSuggestions.length === 0;

  if (!isOpen) return null;

  return (
    <div className="city-modal-overlay" onClick={onClose}>
      <div
        className="city-modal-container"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label="Select City and Nearby Theatres"
      >
        {/* Modal Header */}
        <div className="city-modal-header">
          <div>
            <h2 style={{ fontSize: '1.35rem', fontWeight: 800, margin: 0, color: '#f8fafc' }}>
              📍 Select Your City / Location
            </h2>
            <p style={{ margin: '4px 0 0 0', fontSize: '13px', color: '#94a3b8' }}>
              Search residential cinemas and live shows across India
            </p>
          </div>
          <button
            className="city-modal-close-btn"
            onClick={onClose}
            aria-label="Close"
          >
            ✕
          </button>
        </div>

        {/* Search & GPS Bar */}
        <div style={{ padding: '16px 24px', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
          <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
            <div style={{ position: 'relative', flex: 1 }}>
              <span
                style={{
                  position: 'absolute',
                  left: '14px',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  color: '#94a3b8',
                  fontSize: '15px',
                }}
              >
                🔍
              </span>
              <input
                type="text"
                className="city-search-input"
                placeholder="Search city, town, or residential area..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                autoFocus
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  style={{
                    position: 'absolute',
                    right: '12px',
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'transparent',
                    border: 'none',
                    color: '#94a3b8',
                    cursor: 'pointer',
                    fontSize: '14px',
                  }}
                >
                  ✕
                </button>
              )}
            </div>

            <button
              className="btn btn-radar"
              style={{ padding: '10px 16px', fontSize: '13px', whiteSpace: 'nowrap' }}
              onClick={handleDetectLocation}
              disabled={detecting}
            >
              {detecting ? '📡 Scanning...' : '📍 Auto Detect GPS'}
            </button>
          </div>

          {statusMsg && (
            <div
              style={{
                marginTop: '10px',
                padding: '8px 12px',
                borderRadius: '6px',
                background: 'rgba(56, 189, 248, 0.1)',
                border: '1px solid rgba(56, 189, 248, 0.25)',
                color: '#7dd3fc',
                fontSize: '12px',
              }}
            >
              {statusMsg}
            </div>
          )}
        </div>

        {/* Popular Cities Quick Bar */}
        <div style={{ padding: '14px 24px 8px 24px' }}>
          <div style={{ fontSize: '11px', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.6px', marginBottom: '8px' }}>
            Popular Cinema Hubs:
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            {POPULAR_CITIES.map((c) => (
              <button
                key={c}
                className={`city-pill-btn ${selectedCity === c ? 'active' : ''}`}
                onClick={() => {
                  onSelectCity(c);
                  onClose();
                }}
              >
                {selectedCity === c && '✓ '}
                {c}
              </button>
            ))}
          </div>
        </div>

        {/* Region Filter Tabs (only when not searching) */}
        {!searchQuery && (
          <div className="city-region-tabs">
            {['Popular Metros', 'North India', 'South India', 'West India', 'East & Central India', 'All (80+)'].map((tab) => (
              <button
                key={tab}
                className={`city-region-tab ${activeTab === tab ? 'active' : ''}`}
                onClick={() => setActiveTab(tab)}
              >
                {tab}
              </button>
            ))}
          </div>
        )}
        {/* Dynamic Pan-India Geocoded Suggestions */}
        {searchingRemote && (
          <div style={{ padding: '8px 24px', color: '#38bdf8', fontSize: '13px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span>🌐</span> Searching Indian addresses &amp; landmarks...
          </div>
        )}

        {dynamicSuggestions.length > 0 && (
          <div style={{ padding: '8px 24px' }}>
            <div style={{ fontSize: '11px', fontWeight: 700, color: '#38bdf8', textTransform: 'uppercase', letterSpacing: '0.6px', marginBottom: '8px' }}>
              📍 Verified Indian Addresses &amp; Localities:
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              {dynamicSuggestions.map((loc, idx) => (
                <div
                  key={`${loc.name}-${loc.latitude}-${idx}`}
                  onClick={() => handleSelectDynamicLocation(loc)}
                  style={{
                    background: 'rgba(15, 23, 42, 0.85)',
                    border: '1px solid rgba(56, 189, 248, 0.35)',
                    borderRadius: '8px',
                    padding: '10px 14px',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    transition: 'all 0.15s ease',
                  }}
                  onMouseEnter={(e) => (e.currentTarget.style.borderColor = '#38bdf8')}
                  onMouseLeave={(e) => (e.currentTarget.style.borderColor = 'rgba(56, 189, 248, 0.35)')}
                >
                  <div style={{ flex: 1, minWidth: 0, paddingRight: '12px' }}>
                    <div style={{ fontSize: '14px', fontWeight: 700, color: '#f8fafc', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      📍 {loc.name || loc.locality || loc.city}
                    </div>
                    <div style={{ fontSize: '12px', color: '#94a3b8', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {loc.displayName}
                    </div>
                    <div style={{ fontSize: '11px', color: '#38bdf8', marginTop: '2px' }}>
                      {loc.city ? `City: ${loc.city}` : ''} {loc.state ? `• ${loc.state}` : ''} {loc.postcode ? `• PIN: ${loc.postcode}` : ''}
                    </div>
                  </div>
                  <span style={{ fontSize: '12px', color: '#38bdf8', fontWeight: 700, whiteSpace: 'nowrap' }}>
                    Select →
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Pincode or Locality Match Result */}
        {pincodeOrLocalityMatch && (
          <div style={{ padding: '8px 24px' }}>
            <div
              onClick={() => {
                if (onLocationDetected) {
                  onLocationDetected({
                    latitude: pincodeOrLocalityMatch.latitude,
                    longitude: pincodeOrLocalityMatch.longitude,
                    neighborhood: pincodeOrLocalityMatch.neighborhood,
                  });
                }
                onSelectCity(pincodeOrLocalityMatch.city);
                onClose();
              }}
              style={{
                background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.15), rgba(56, 189, 248, 0.15))',
                border: '1px solid #10b981',
                borderRadius: '8px',
                padding: '12px 16px',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                transition: 'all 0.2s',
              }}
            >
              <div>
                <div style={{ fontWeight: 800, color: '#34d399', fontSize: '14px' }}>
                  🎯 Exact Residential Locality Found: {pincodeOrLocalityMatch.neighborhood || pincodeOrLocalityMatch.city}
                </div>
                <div style={{ fontSize: '12px', color: '#94a3b8', marginTop: '2px' }}>
                  City: {pincodeOrLocalityMatch.city} • Tap to view closest cinemas to this area
                </div>
              </div>
              <span style={{ fontSize: '18px' }}>➔</span>
            </div>
          </div>
        )}

        {/* Custom City Direct Selection option */}
        {isCustomCity && (
          <div style={{ padding: '8px 24px' }}>
            <div
              onClick={() => {
                onSelectCity(searchQuery.trim());
                onClose();
              }}
              style={{
                background: 'linear-gradient(135deg, rgba(225, 29, 72, 0.15), rgba(56, 189, 248, 0.15))',
                border: '1px dashed #e11d48',
                borderRadius: '8px',
                padding: '12px 16px',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                transition: 'all 0.2s',
              }}
            >
              <div>
                <span style={{ fontSize: '14px', fontWeight: 700, color: '#f8fafc' }}>
                  📍 Select "{searchQuery.trim()}"
                </span>
                <div style={{ fontSize: '12px', color: '#cbd5e1' }}>
                  Search and provision cinemas in this Indian city or town
                </div>
              </div>
              <span style={{ color: '#e11d48', fontWeight: 800 }}>Explore →</span>
            </div>
          </div>
        )}

        {/* City Grid List */}
        <div className="city-grid-scroll">
          {filteredCities.length > 0 ? (
            <div className="city-grid">
              {filteredCities.map((city) => (
                <div
                  key={city}
                  className={`city-grid-item ${selectedCity === city ? 'active' : ''}`}
                  onClick={() => {
                    onSelectCity(city);
                    onClose();
                  }}
                >
                  <span style={{ fontSize: '14px' }}>📍</span>
                  <span style={{ flex: 1, fontWeight: selectedCity === city ? 700 : 500 }}>
                    {city}
                  </span>
                  {selectedCity === city && (
                    <span style={{ color: '#e11d48', fontSize: '13px', fontWeight: 800 }}>✓</span>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <div style={{ padding: '30px', textAlign: 'center', color: '#94a3b8' }}>
              <div style={{ fontSize: '24px', marginBottom: '8px' }}>🏙️</div>
              <div>No preloaded city matched "{searchQuery}"</div>
              <div style={{ fontSize: '12px', marginTop: '6px' }}>
                You can click above to explore residential cinemas in "{searchQuery}" directly!
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
