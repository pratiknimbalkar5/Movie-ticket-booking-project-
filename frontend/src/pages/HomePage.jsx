import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../services/api';
import { handlePosterError, getFallbackPosterSvg } from '../utils/posterHelper';
import BmsCinemaTracker from '../components/BmsCinemaTracker';

export default function HomePage({
  selectedCity = '',
  onCityChange,
  userCoords = null,
  onLocationDetected,
  onSelectMovie,
  onSelectShow,
}) {
  const navigate = useNavigate();
  const [heroSearchQuery, setHeroSearchQuery] = useState('');

  // Sections data
  const [nowShowing, setNowShowing] = useState([]);
  const [latestReleases, setLatestReleases] = useState([]);
  const [upcoming, setUpcoming] = useState([]);

  const [loadingNowShowing, setLoadingNowShowing] = useState(true);
  const [loadingLatest, setLoadingLatest] = useState(true);
  const [loadingUpcoming, setLoadingUpcoming] = useState(true);

  // Active home tab for movie carousel/grid
  const [activeMovieTab, setActiveMovieTab] = useState('now-showing'); // 'now-showing' | 'latest' | 'upcoming'

  useEffect(() => {
    // 1. Fetch Now In Theatres (movies with active bookable shows / current theatrical run)
    api.get('/movies/now-playing?region=IN&page=1')
      .then((res) => {
        const list = res.data?.data?.results || res.data?.data || [];
        setNowShowing(list.slice(0, 8));
      })
      .catch(() => {})
      .finally(() => setLoadingNowShowing(false));

    // 2. Fetch Latest Releases (Strictly within the last 30 days, sorted newest first)
    api.get('/movies/latest-releases?region=IN&days=30&page=1')
      .then((res) => {
        const list = res.data?.data?.results || res.data?.data || [];
        setLatestReleases(list.slice(0, 8));
      })
      .catch(() => {})
      .finally(() => setLoadingLatest(false));

    // 3. Fetch Coming Soon (Strictly future Indian release dates)
    api.get('/movies/upcoming?region=IN&page=1')
      .then((res) => {
        const list = res.data?.data?.results || res.data?.data || [];
        setUpcoming(list.slice(0, 8));
      })
      .catch(() => {})
      .finally(() => setLoadingUpcoming(false));
  }, []);

  const handleHeroSearchSubmit = (e) => {
    e.preventDefault();
    if (heroSearchQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(heroSearchQuery.trim())}`);
    } else {
      navigate('/search');
    }
  };

  const handleMovieClick = (movie) => {
    if (onSelectMovie) onSelectMovie(movie);
    const targetId = movie.localMovieId || movie.id;
    navigate(`/movie/${targetId}`, { state: { movie } });
  };

  const handleShowClick = (show) => {
    if (onSelectShow) onSelectShow(show);
    navigate(`/seat-selection/${show.id || show.showId}`, { state: { show } });
  };

  const currentList =
    activeMovieTab === 'now-showing'
      ? nowShowing
      : activeMovieTab === 'latest'
      ? latestReleases
      : upcoming;

  const currentLoading =
    activeMovieTab === 'now-showing'
      ? loadingNowShowing
      : activeMovieTab === 'latest'
      ? loadingLatest
      : loadingUpcoming;

  return (
    <div className="container" style={{ paddingBottom: 60 }}>
      {/* =========================================================================
          HERO BANNER
          ========================================================================= */}
      <section
        style={{
          marginTop: 20,
          marginBottom: 36,
          background: 'radial-gradient(ellipse at top left, #1e293b 0%, #0f172a 100%)',
          borderRadius: 20,
          padding: '44px 32px',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          boxShadow: '0 20px 40px rgba(0, 0, 0, 0.4)',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        <div style={{ maxWidth: 720, position: 'relative', zIndex: 2 }}>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 8,
              background: 'rgba(56, 189, 248, 0.1)',
              border: '1px solid rgba(56, 189, 248, 0.25)',
              padding: '6px 14px',
              borderRadius: 20,
              fontSize: '13px',
              color: '#38bdf8',
              fontWeight: 600,
              marginBottom: 16,
            }}
          >
            <span>🍿 Experience Cinema Like Never Before</span>
            {selectedCity && <span>• 📍 {selectedCity}</span>}
          </div>

          <h1
            style={{
              fontSize: '36px',
              fontWeight: 800,
              color: '#f8fafc',
              lineHeight: 1.2,
              marginBottom: 14,
              letterSpacing: '-0.5px',
            }}
          >
            Book Movie Tickets Online with Instant Seat Reservation
          </h1>
          <p style={{ color: '#94a3b8', fontSize: '15px', lineHeight: 1.6, marginBottom: 28 }}>
            Discover the latest theatrical releases in India, view live staggered showtimes across major multiplexes, and book your preferred seats with ease.
          </p>

          {/* Hero Quick Search Bar */}
          <form
            onSubmit={handleHeroSearchSubmit}
            style={{
              display: 'flex',
              alignItems: 'center',
              background: '#090d16',
              border: '2px solid rgba(56, 189, 248, 0.35)',
              borderRadius: 12,
              padding: '6px 10px 6px 18px',
              maxWidth: 580,
              boxShadow: '0 8px 24px rgba(0, 0, 0, 0.5)',
            }}
          >
            <span style={{ fontSize: '18px', marginRight: 10, color: '#38bdf8' }}>🔍</span>
            <input
              type="text"
              placeholder="Search for Movies, Theatres, or Genres..."
              value={heroSearchQuery}
              onChange={(e) => setHeroSearchQuery(e.target.value)}
              style={{
                flex: 1,
                background: 'transparent',
                border: 'none',
                color: '#fff',
                fontSize: '15px',
                outline: 'none',
              }}
            />
            <button
              type="submit"
              className="btn"
              style={{ padding: '10px 22px', fontSize: '14px', fontWeight: 700 }}
            >
              Search
            </button>
          </form>

          {/* Quick Shortcuts */}
          <div style={{ display: 'flex', gap: 12, marginTop: 20, flexWrap: 'wrap' }}>
            <button
              className="btn btn-secondary"
              style={{ fontSize: '13px', padding: '6px 14px' }}
              onClick={() => setActiveMovieTab('now-showing')}
            >
              🔥 Now In Theatres
            </button>
            <button
              className="btn btn-secondary"
              style={{ fontSize: '13px', padding: '6px 14px' }}
              onClick={() => setActiveMovieTab('latest')}
            >
              🆕 Latest (Last 30 Days)
            </button>
            <button
              className="btn btn-secondary"
              style={{ fontSize: '13px', padding: '6px 14px' }}
              onClick={() => setActiveMovieTab('upcoming')}
            >
              🗓️ Coming Soon
            </button>
            <Link
              to="/theatres"
              className="btn btn-secondary"
              style={{ fontSize: '13px', padding: '6px 14px', textDecoration: 'none' }}
            >
              📍 Nearby Theatres
            </Link>
          </div>
        </div>
      </section>

      {/* =========================================================================
          SECTION 1: MOVIES SHOWCASE (NOW SHOWING / LATEST RELEASES / UPCOMING)
          ========================================================================= */}
      <section style={{ marginBottom: 48 }}>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: 12,
            borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
            paddingBottom: 14,
            marginBottom: 24,
          }}
        >
          {/* Section Tab Switchers */}
          <div className="view-tabs" style={{ borderBottom: 'none', paddingBottom: 0 }}>
            <button
              className={`view-tab ${activeMovieTab === 'now-showing' ? 'active' : ''}`}
              onClick={() => setActiveMovieTab('now-showing')}
              style={{ fontSize: '15px', fontWeight: 700 }}
            >
              <span className="live-indicator"></span> Now In Theatres
            </button>
            <button
              className={`view-tab ${activeMovieTab === 'latest' ? 'active' : ''}`}
              onClick={() => setActiveMovieTab('latest')}
              style={{ fontSize: '15px', fontWeight: 700 }}
            >
              🆕 Latest Releases
            </button>
            <button
              className={`view-tab ${activeMovieTab === 'upcoming' ? 'active' : ''}`}
              onClick={() => setActiveMovieTab('upcoming')}
              style={{ fontSize: '15px', fontWeight: 700 }}
            >
              🗓️ Coming Soon
            </button>
          </div>

          <Link
            to={activeMovieTab === 'upcoming' ? '/upcoming' : '/now-showing'}
            style={{ color: '#38bdf8', fontSize: '13px', fontWeight: 600, textDecoration: 'none' }}
          >
            View All Movies ➔
          </Link>
        </div>

        {/* Loading Spinner */}
        {currentLoading ? (
          <div style={{ textAlign: 'center', padding: '50px 0', color: '#94a3b8' }}>
            <div style={{ fontSize: '38px', marginBottom: '12px', animation: 'spin 2s linear infinite' }}>🍿</div>
            <p>Loading theatrical titles...</p>
          </div>
        ) : currentList.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '40px 20px', borderRadius: 12 }}>
            <p style={{ color: '#94a3b8' }}>No movies available in this category currently.</p>
          </div>
        ) : (
          <div className="movies-grid">
            {currentList.map((movie) => {
              const isUpcoming = movie.isUpcoming || (movie.releaseDate && new Date(movie.releaseDate) > new Date());
              return (
                <div
                  key={movie.id || movie.tmdbId}
                  className="movie-card"
                  onClick={() => handleMovieClick(movie)}
                  style={{ cursor: 'pointer' }}
                >
                  <div style={{ position: 'relative' }}>
                    <img
                      src={movie.posterUrl || getFallbackPosterSvg(movie.title)}
                      alt={movie.title}
                      className="movie-poster"
                      onError={(e) => handlePosterError(e, movie.title)}
                      loading="lazy"
                    />
                    {isUpcoming ? (
                      <span
                        style={{
                          position: 'absolute',
                          top: 8,
                          right: 8,
                          background: 'rgba(217, 119, 6, 0.9)',
                          color: '#fff',
                          fontSize: '11px',
                          fontWeight: 700,
                          padding: '3px 8px',
                          borderRadius: 6,
                          backdropFilter: 'blur(4px)',
                        }}
                      >
                        🗓️ {movie.releaseDate ? new Date(movie.releaseDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) : 'COMING SOON'}
                      </span>
                    ) : (
                      <span
                        style={{
                          position: 'absolute',
                          top: 8,
                          right: 8,
                          background: 'rgba(16, 185, 129, 0.9)',
                          color: '#fff',
                          fontSize: '11px',
                          fontWeight: 700,
                          padding: '3px 8px',
                          borderRadius: 6,
                          backdropFilter: 'blur(4px)',
                        }}
                      >
                        🟢 NOW PLAYING
                      </span>
                    )}

                    {movie.rating > 0 && (
                      <span
                        style={{
                          position: 'absolute',
                          bottom: 8,
                          left: 8,
                          background: 'rgba(15, 23, 42, 0.85)',
                          color: '#fbbf24',
                          fontSize: '12px',
                          fontWeight: 700,
                          padding: '3px 8px',
                          borderRadius: 6,
                          border: '1px solid rgba(251, 191, 36, 0.3)',
                        }}
                      >
                        ★ {movie.rating.toFixed(1)}
                      </span>
                    )}
                  </div>

                  <div className="movie-info" style={{ padding: '14px' }}>
                    <h3
                      style={{
                        fontSize: '15px',
                        fontWeight: 700,
                        color: '#f8fafc',
                        marginBottom: 4,
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                      }}
                      title={movie.title}
                    >
                      {movie.title}
                    </h3>
                    <p
                      style={{
                        fontSize: '12px',
                        color: '#94a3b8',
                        marginBottom: 10,
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                      }}
                    >
                      {movie.genre || 'Action, Drama'}
                    </p>
                    <div
                      style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        fontSize: '12px',
                        color: '#cbd5e1',
                      }}
                    >
                      <span>
                        {movie.releaseDate ? new Date(movie.releaseDate).toLocaleDateString() : 'Active'}
                      </span>
                      <button
                        className="btn btn-sm"
                        style={{ padding: '4px 12px', fontSize: '11px', fontWeight: 600 }}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleMovieClick(movie);
                        }}
                      >
                        {isUpcoming ? 'View Details' : 'Book Show'}
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>

      {/* =========================================================================
          SECTION 2: NEARBY THEATRES & REAL STAGGERED SHOWTIMES
          ========================================================================= */}
      <section style={{ marginTop: 20 }}>
        <div style={{ marginBottom: 20 }}>
          <h2 style={{ fontSize: '22px', fontWeight: 700, color: '#f8fafc', marginBottom: 6 }}>
            📍 Nearby Theatres &amp; Staggered Showtimes
          </h2>
          <p style={{ color: '#94a3b8', fontSize: '14px' }}>
            Find the closest cinema screens based on your exact coordinates with chain-specific show scheduling and realistic pricing.
          </p>
        </div>

        <BmsCinemaTracker
          selectedCity={selectedCity}
          onCityChange={onCityChange}
          onSelectShow={handleShowClick}
          userLocation={userCoords}
          onLocationDetected={onLocationDetected}
        />
      </section>
    </div>
  );
}

