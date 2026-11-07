import React, { useState, useEffect, useRef } from 'react';
import api from '../services/api';
import { handlePosterError, getFallbackPosterSvg } from '../utils/posterHelper';
import BmsCinemaTracker from '../components/BmsCinemaTracker';

export default function MoviesPage({
  onSelectMovie,
  onSelectShow,
  selectedCity = '',
  onCityChange,
  initialTab = 'cinemas', // Default to City Mode as requested!
  userCoords: propCoords = null,
  onLocationDetected,
}) {
  // Primary mode: 'cinemas' (City Shows) | 'now-playing' | 'upcoming' | 'popular' | 'trending'
  const [activeTab, setActiveTab] = useState(
    initialTab === 'now-playing' ? 'now-playing'
    : initialTab === 'upcoming' ? 'upcoming'
    : initialTab === 'popular' ? 'popular'
    : initialTab === 'trending' ? 'trending'
    : 'cinemas'
  );

  const [movies, setMovies] = useState([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [movieSearch, setMovieSearch] = useState('');
  const [activeMovieSearch, setActiveMovieSearch] = useState('');
  const [genres, setGenres] = useState(['All']);
  const [selectedGenre, setSelectedGenre] = useState('All');
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState('');
  const [toastMessage, setToastMessage] = useState('');
  const [userCoords, setUserCoords] = useState(propCoords);

  const searchDebounceRef = useRef(null);

  useEffect(() => {
    if (propCoords) setUserCoords(propCoords);
  }, [propCoords]);

  // Sync initialTab if changed by parent
  useEffect(() => {
    if (initialTab) {
      if (initialTab === 'near-you' || initialTab === 'cinemas') {
        setActiveTab('cinemas');
      } else {
        setActiveTab(initialTab);
      }
    }
  }, [initialTab]);

  // Fetch movies for catalog tabs
  const fetchMovies = async (tabToFetch, pageNum = 1, append = false, query = '') => {
    if (tabToFetch === 'cinemas') return;

    try {
      if (append) setLoadingMore(true);
      else setLoading(true);
      setError('');

      let endpoint = '';
      if (query && query.trim().length > 0) {
        endpoint = `/movies/search?query=${encodeURIComponent(query.trim())}&page=${pageNum}`;
      } else {
        switch (tabToFetch) {
          case 'upcoming':
            endpoint = `/movies/upcoming?region=IN&page=${pageNum}`;
            break;
          case 'popular':
            endpoint = `/movies/popular?page=${pageNum}`;
            break;
          case 'trending':
            endpoint = `/movies/trending?window=day&page=${pageNum}`;
            break;
          case 'now-playing':
          default:
            endpoint = `/movies/now-playing?region=IN&page=${pageNum}`;
            break;
        }
      }

      const res = await api.get(endpoint);
      const data = res.data?.data;

      let fetchedResults = [];
      let maxPages = 1;

      if (data && Array.isArray(data.results)) {
        fetchedResults = data.results;
        maxPages = data.totalPages || 1;
      } else if (Array.isArray(data)) {
        fetchedResults = data;
        maxPages = 1;
      } else if (data && Array.isArray(data.content)) {
        fetchedResults = data.content;
        maxPages = data.totalPages || 1;
      }

      if (append) {
        setMovies((prev) => [...prev, ...fetchedResults]);
      } else {
        setMovies(fetchedResults);
      }

      setPage(pageNum);
      setTotalPages(maxPages);

      const allGenres = new Set(['All']);
      fetchedResults.forEach((m) => {
        if (m.genre) {
          m.genre.split(/[\/,]/).forEach((g) => {
            const trimmed = g.trim();
            if (trimmed) allGenres.add(trimmed);
          });
        }
      });
      if (!append) setGenres(Array.from(allGenres));
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to connect to backend server');
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  };

  useEffect(() => {
    if (activeTab !== 'cinemas') {
      fetchMovies(activeTab, 1, false, activeMovieSearch);
    }
  }, [activeTab, activeMovieSearch]);

  const handleMovieSearchChange = (e) => {
    const val = e.target.value;
    setMovieSearch(val);
    if (searchDebounceRef.current) clearTimeout(searchDebounceRef.current);
    searchDebounceRef.current = setTimeout(() => {
      setActiveMovieSearch(val.trim());
    }, 450);
  };

  const handleNotifyMe = (movieTitle) => {
    setToastMessage(`🔔 Pre-booking reminder registered for "${movieTitle}"!`);
    setTimeout(() => setToastMessage(''), 4500);
  };

  const filteredMovies = movies.filter((m) => {
    if (selectedGenre === 'All') return true;
    return m.genre?.toLowerCase().includes(selectedGenre.toLowerCase());
  });

  return (
    <div className="container">
      {/* Toast Alert */}
      {toastMessage && (
        <div
          className="alert alert-success"
          style={{
            position: 'sticky',
            top: '72px',
            zIndex: 95,
            boxShadow: '0 6px 20px rgba(16, 185, 129, 0.35)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <span>{toastMessage}</span>
          <button
            onClick={() => setToastMessage('')}
            style={{ background: 'transparent', border: 'none', color: '#fff', cursor: 'pointer', fontSize: '16px' }}
          >
            ✕
          </button>
        </div>
      )}

      {/* Prominent Movie Search Bar at the Top of Homepage */}
      <div
        style={{
          display: 'flex',
          gap: '12px',
          alignItems: 'center',
          marginBottom: '18px',
          background: 'linear-gradient(90deg, rgba(22, 31, 48, 0.95), rgba(15, 23, 42, 0.95))',
          padding: '12px 18px',
          borderRadius: '12px',
          border: '1px solid rgba(56, 189, 248, 0.3)',
          boxShadow: '0 4px 16px rgba(0, 0, 0, 0.3)',
        }}
      >
        <span style={{ fontSize: '18px', color: '#38bdf8' }}>🔍</span>
        <input
          type="text"
          placeholder="Search movies by title, cast, or genre across India..."
          value={movieSearch}
          onChange={(e) => {
            handleMovieSearchChange(e);
            if (activeTab === 'cinemas' && e.target.value.trim().length > 0) {
              setActiveTab('now-playing');
            }
          }}
          style={{
            flex: 1,
            background: 'transparent',
            border: 'none',
            color: '#fff',
            fontSize: '15px',
            outline: 'none',
          }}
        />
        {movieSearch && (
          <button
            onClick={() => {
              setMovieSearch('');
              setActiveMovieSearch('');
            }}
            style={{ background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer', fontSize: '16px' }}
            title="Clear search"
          >
            ✕
          </button>
        )}
      </div>

      {/* Top Mode Navigation Tabs */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '12px',
          borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
          paddingBottom: '14px',
          marginBottom: '20px',
        }}
      >
        <div className="view-tabs" style={{ borderBottom: 'none', paddingBottom: 0 }}>
          <button
            className={`view-tab ${activeTab === 'cinemas' ? 'active' : ''}`}
            onClick={() => {
              setActiveTab('cinemas');
              setMovieSearch('');
              setActiveMovieSearch('');
            }}
            style={{ fontSize: '15px', fontWeight: 700 }}
          >
            📍 City Shows &amp; Theatres {selectedCity ? `(${selectedCity})` : ''}
          </button>
          <button
            className={`view-tab ${activeTab === 'now-playing' ? 'active' : ''}`}
            onClick={() => setActiveTab('now-playing')}
          >
            <span className="live-indicator"></span> Now In Theatres
          </button>
          <button
            className={`view-tab ${activeTab === 'upcoming' ? 'active' : ''}`}
            onClick={() => setActiveTab('upcoming')}
          >
            🗓️ Upcoming Releases
          </button>
          <button
            className={`view-tab ${activeTab === 'popular' ? 'active' : ''}`}
            onClick={() => setActiveTab('popular')}
          >
            ⭐ Popular
          </button>
          <button
            className={`view-tab ${activeTab === 'trending' ? 'active' : ''}`}
            onClick={() => setActiveTab('trending')}
          >
            🔥 Trending
          </button>
        </div>
      </div>

      {/* =========================================================================
          PRIMARY VIEW 1: CITY MODE (DIRECT THEATRES & REAL SHOWS)
          ========================================================================= */}
      {activeTab === 'cinemas' && (
        <BmsCinemaTracker
          selectedCity={selectedCity}
          onCityChange={onCityChange}
          onSelectShow={onSelectShow}
          userLocation={userCoords}
          onLocationDetected={(data) => {
            setUserCoords(data);
            if (onLocationDetected) onLocationDetected(data);
          }}
        />
      )}

      {/* =========================================================================
          VIEW 2: MOVIES CATALOG (When user browses by movie title/poster)
          ========================================================================= */}
      {activeTab !== 'cinemas' && (
        <div>
          {/* Movie search input */}
          <div
            style={{
              display: 'flex',
              gap: '12px',
              alignItems: 'center',
              marginBottom: '20px',
              background: 'rgba(22, 31, 48, 0.7)',
              padding: '12px 18px',
              borderRadius: '10px',
              border: '1px solid rgba(255, 255, 255, 0.08)',
            }}
          >
            <span style={{ fontSize: '16px', color: '#94a3b8' }}>🔍</span>
            <input
              type="text"
              placeholder="Search movies by title, cast, or genre..."
              value={movieSearch}
              onChange={handleMovieSearchChange}
              style={{
                flex: 1,
                background: 'transparent',
                border: 'none',
                color: '#fff',
                fontSize: '14px',
                outline: 'none',
              }}
            />
            {movieSearch && (
              <button
                onClick={() => {
                  setMovieSearch('');
                  setActiveMovieSearch('');
                }}
                style={{ background: 'transparent', border: 'none', color: '#94a3b8', cursor: 'pointer' }}
              >
                ✕
              </button>
            )}
          </div>

          {/* Genre filter pills */}
          {genres.length > 1 && !activeMovieSearch && (
            <div className="pill-container" style={{ marginBottom: '20px' }}>
              {genres.map((g) => (
                <button
                  key={g}
                  className={`pill ${selectedGenre === g ? 'active' : ''}`}
                  onClick={() => setSelectedGenre(g)}
                >
                  {g}
                </button>
              ))}
            </div>
          )}

          {error && <div className="alert alert-danger">{error}</div>}

          {loading ? (
            <div style={{ textAlign: 'center', padding: '60px 0', color: '#94a3b8' }}>
              <div style={{ fontSize: '38px', marginBottom: '12px', animation: 'spin 2s linear infinite' }}>🍿</div>
              <p>Loading movies catalog...</p>
            </div>
          ) : filteredMovies.length === 0 ? (
            <div className="card" style={{ textAlign: 'center', padding: '48px 20px', margin: '20px 0' }}>
              <div style={{ fontSize: '42px', marginBottom: '12px' }}>🎬</div>
              <h3>No movies found</h3>
              <p style={{ color: '#94a3b8', margin: '8px 0 16px' }}>Try searching another title or clearing filters.</p>
              <button
                className="btn btn-secondary"
                onClick={() => {
                  setMovieSearch('');
                  setActiveMovieSearch('');
                  setSelectedGenre('All');
                }}
              >
                Reset Search
              </button>
            </div>
          ) : (
            <>
              <div className="movies-grid">
                {filteredMovies.map((m) => (
                  <div key={m.id} className="movie-card">
                    <div className="poster-wrapper" onClick={() => onSelectMovie(m)} style={{ cursor: 'pointer' }}>
                      <img
                        src={m.posterUrl || getFallbackPosterSvg(m.title, m.genre)}
                        alt={m.title}
                        className="movie-poster"
                        loading="lazy"
                        onError={(e) => handlePosterError(e, m.title, m.genre)}
                      />
                      {m.isUpcoming ? (
                        <div className="upcoming-badge">
                          SOON •{' '}
                          {m.releaseDate
                            ? new Date(m.releaseDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
                            : '2026'}
                        </div>
                      ) : (
                        <div className="rating-badge">
                          ⭐ {m.rating ? m.rating.toFixed(1) : '7.5'}
                        </div>
                      )}
                      <div className="lang-badge">{m.language || 'EN'}</div>
                    </div>

                    <div className="movie-info">
                      <h3
                        className="movie-title"
                        title={m.title}
                        onClick={() => onSelectMovie(m)}
                        style={{ cursor: 'pointer' }}
                      >
                        {m.title}
                      </h3>
                      <div className="movie-genre">{m.genre || 'Action / Drama'}</div>
                      <div className="movie-meta-row">
                        <span>
                          🗓️{' '}
                          {m.releaseDate
                            ? new Date(m.releaseDate).toLocaleDateString('en-US', { month: 'short', year: 'numeric' })
                            : '2026'}
                        </span>
                        <span>
                          {m.isUpcoming
                            ? '🗓️ Upcoming'
                            : m.hasBookableShows === false
                            ? '⚪ Released (No Shows)'
                            : '🟢 In Theatres'}
                        </span>
                      </div>

                      {m.overview && (
                        <p
                          style={{
                            fontSize: '12px',
                            color: '#94a3b8',
                            lineHeight: '1.4',
                            marginTop: '6px',
                            display: '-webkit-box',
                            WebkitLineClamp: 2,
                            WebkitBoxOrient: 'vertical',
                            overflow: 'hidden',
                          }}
                        >
                          {m.overview}
                        </p>
                      )}

                      <div style={{ marginTop: '12px' }}>
                        {m.isUpcoming ? (
                          <button
                            className="btn btn-secondary btn-block"
                            onClick={() => handleNotifyMe(m.title)}
                          >
                            🔔 Remind Me
                          </button>
                        ) : m.hasBookableShows === false ? (
                          <button
                            className="btn btn-secondary btn-block"
                            onClick={() => handleNotifyMe(m.title)}
                            style={{ opacity: 0.85 }}
                            title="No active shows scheduled in database yet"
                          >
                            🔔 Notify When Shows Open
                          </button>
                        ) : (
                          <button
                            className="btn btn-block"
                            onClick={() => onSelectMovie(m)}
                          >
                            🎟️ View Shows {selectedCity ? `(${selectedCity})` : ''}
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {page < totalPages && (
                <div style={{ textAlign: 'center', margin: '40px 0' }}>
                  <button
                    className="btn btn-secondary"
                    onClick={() => fetchMovies(activeTab, page + 1, true, activeMovieSearch)}
                    disabled={loadingMore}
                  >
                    {loadingMore ? 'Loading next page...' : `Load More Movies (Page ${page + 1} of ${totalPages})`}
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      )}

      {/* Footer */}
      <footer
        style={{
          marginTop: '60px',
          padding: '28px 20px',
          borderTop: '1px solid rgba(255, 255, 255, 0.08)',
          textAlign: 'center',
          color: '#64748b',
          fontSize: '13px',
        }}
      >
        <p style={{ margin: 0 }}>
          🎬 CinemaPulse • Real-time Movie Booking Platform with Live Seat Reservation
        </p>
      </footer>
    </div>
  );
}
