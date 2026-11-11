import React, { useContext, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';
import CitySelectorModal from './CitySelectorModal';
import { detectCurrentLocation, normalizeIndianCity } from '../services/bmsLocationService';

export default function Navbar({
  selectedCity,
  onCityChange,
  neighborhood = '',
  onLocationDetected,
  userCoords = null,
}) {
  const { user, logout } = useContext(AuthContext);
  const navigate = useNavigate();
  const [modalOpen, setModalOpen] = useState(false);
  const [gpsLoading, setGpsLoading] = useState(false);

  const handleQuickGps = async () => {
    try {
      setGpsLoading(true);
      const res = await detectCurrentLocation();
      if (res.success) {
        if (onLocationDetected) {
          onLocationDetected({
            latitude: res.latitude,
            longitude: res.longitude,
            neighborhood: res.neighborhood,
          });
        }
        if (onCityChange && res.city) {
          onCityChange(res.city);
        }
        navigate('/theatres');
      } else {
        setModalOpen(true);
      }
    } finally {
      setGpsLoading(false);
    }
  };

  return (
    <>
      <nav className="navbar">
        <div className="nav-left">
          {/* Project Title / Brand Logo - ALWAYS navigates back to clean homepage */}
          <Link
            to="/"
            className="brand"
            style={{ textDecoration: 'none', cursor: 'pointer' }}
            title="Return to CinemaPulse Homepage"
          >
            🎬 CinemaPulse
          </Link>

          <NavLink
            to="/"
            end
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
          >
            🏠 Home
          </NavLink>

          <NavLink
            to="/search"
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
          >
            🔍 Search
          </NavLink>

          <NavLink
            to="/theatres"
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
          >
            📍 Theatres
          </NavLink>

          <NavLink
            to="/now-showing"
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
          >
            <span className="live-indicator"></span> Now Showing
          </NavLink>

          <NavLink
            to="/upcoming"
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
          >
            🗓️ Upcoming
          </NavLink>

          <NavLink
            to="/bookings"
            className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
            title="View tickets and cancel bookings"
          >
            🎟️ My Bookings
          </NavLink>
        </div>

        <div className="nav-right">
          {/* Direct GPS Quick Detect Pill */}
          <button
            className={`btn-gps-pill ${userCoords ? 'active' : ''}`}
            onClick={handleQuickGps}
            disabled={gpsLoading}
            title="Use device GPS to locate nearest cinema screens"
          >
            <span className="gps-dot"></span>
            {gpsLoading ? '🛰️ Scanning GPS...' : userCoords ? '🎯 GPS Active' : '📍 Detect GPS'}
          </button>

          {/* City / Locality Selector Button */}
          <button
            className="navbar-city-btn"
            onClick={() => setModalOpen(true)}
            title="Click to search city or residential cinema locality"
            aria-label="Select City or Locality"
          >
            <span style={{ fontSize: '15px' }}>🏛️</span>
            <span
              style={{
                fontWeight: 700,
                color: '#f8fafc',
                maxWidth: '140px',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
            >
              {neighborhood ? `${neighborhood}, ` : ''}
              {normalizeIndianCity(selectedCity) || 'Select City'}
            </span>
            <span style={{ fontSize: '11px', color: '#94a3b8' }}>▾</span>
          </button>

          {user ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <span style={{ color: '#94a3b8', fontSize: '13px' }}>
                👤 <strong>{user.username}</strong>
              </span>
              <button
                className="btn btn-secondary"
                style={{ padding: '6px 12px', fontSize: '12px' }}
                onClick={logout}
              >
                Logout
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', gap: '8px' }}>
              <Link
                to="/login"
                className="btn btn-secondary"
                style={{ padding: '6px 14px', fontSize: '13px', textDecoration: 'none' }}
              >
                Login
              </Link>
              <Link
                to="/register"
                className="btn"
                style={{ padding: '6px 14px', fontSize: '13px', textDecoration: 'none' }}
              >
                Register
              </Link>
            </div>
          )}
        </div>
      </nav>

      {/* City & Residential Cinema Selection Modal */}
      <CitySelectorModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        selectedCity={selectedCity}
        onSelectCity={(city) => {
          if (onCityChange) onCityChange(city);
        }}
        onLocationDetected={(data) => {
          if (onLocationDetected) onLocationDetected(data);
        }}
      />
    </>
  );
}
