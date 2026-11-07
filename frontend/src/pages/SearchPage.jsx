import React, { useState, useEffect, useRef } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import api from '../services/api';
import { handlePosterError, getFallbackPosterSvg } from '../utils/posterHelper';

const GENRES_LIST = [
  'All',
  'Action',
  'Adventure',
  'Animation',
  'Comedy',
  'Crime',
  'Drama',
  'Horror',
  'Romance',
  'Sci-Fi',
  'Thriller',
];

export default function SearchPage({ onSelectMovie }) {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  const initialQuery = searchParams.get('q') || '';
  const [searchTerm, setSearchTerm] = useState(initialQuery);
  const [selectedGenre, setSelectedGenre] = useState('All');
  const [statusFilter, setStatusFilter] = useState('ALL'); // ALL | NOW_SHOWING | UPCOMING

  const [movies, setMovies] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [hasSearched, setHasSearched] = useState(Boolean(initialQuery));

  const searchDebounceRef = useRef(null);

  const executeSearch = async (queryText) => {
    const q = queryText.trim();
    if (!q) {
      // If empty query, fetch default trending/now-playing to not leave screen bare
      try {
        setLoading(true);
        setError('');
        const res = await api.get('/movies/now-playing?region=IN&page=1');
        const list = res.data?.data?.results || res.data?.data || [];
        setMovies(list);
        setHasSearched(false);
      } catch (err) {
        setError('Failed to load initial movies.');
      } finally {
        setLoading(false);
      }
      return;
    }

    try {
      setLoading(true);
      setError('');
      setHasSearched(true);
      const res = await api.get(`/movies/search?query=${encodeURIComponent(q)}&page=1`);
      const results = res.data?.data?.results || res.data?.data || [];
      setMovies(results);
    } catch (err) {
      setError(err.response?.data?.message || 'Search failed. Please try again.');
      setMovies([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    executeSearch(searchTerm);
  }, []);

  const handleInputChange = (e) => {
    const val = e.target.value;
    setSearchTerm(val);

    if (searchDebounceRef.current) {
      clearTimeout(searchDebounceRef.current);
    }

    searchDebounceRef.current = setTimeout(() => {
      setSearchParams(val.trim() ? { q: val.trim() } : {});
      executeSearch(val);
    }, 350);
  };

  const handleClear = () => {
    setSearchTerm('');
    setSearchParams({});
    executeSearch('');
  };

  // Filter movies based on genre and status
  const filteredMovies = movies.filter((m) => {
    if (selectedGenre !== 'All') {
      const g = (m.genre || '').toLowerCase();
      if (!g.includes(selectedGenre.toLowerCase())) {
        return false;
      }
    }
    if (statusFilter === 'NOW_SHOWING') {
      if (m.isUpcoming) return false;
    } else if (statusFilter === 'UPCOMING') {
      if (!m.isUpcoming) return false;
    }
    return true;
  });

  const handleMovieCardClick = (movie) => {
    if (onSelectMovie) {
      onSelectMovie(movie);
    }
    const targetId = movie.localMovieId || movie.id;
    navigate(`/movie/${targetId}`, { state: { movie } });
  };

  return (
    <div className="container" style={{ marginTop: 24, paddingBottom: 60 }}>
      {/* Search Header Banner */}
      <div
        style={{
          background: 'linear-gradient(135deg, rgba(30, 41, 59, 0.9) 0%, rgba(15, 23, 42, 0.95) 100%)',
          borderRadius: 16,
          padding: '28px 24px',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          boxShadow: '0 10px 30px rgba(0, 0, 0, 0.3)',
          marginBottom: 28,
        }}
      >
        <h1 style={{ fontSize: '24px', fontWeight: 700, color: '#f8fafc', marginBottom: 8 }}>
          🔍 Explore &amp; Find Movies
        </h1>
        <p style={{ color: '#94a3b8', fontSize: '14px', marginBottom: 20 }}>
          Search across thousands of titles, actors, directors, genres, and active theatre schedules.
        </p>

        {/* Search Bar Input */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            background: 'rgba(15, 23, 42, 0.8)',
            border: '2px solid rgba(56, 189, 248, 0.4)',
            borderRadius: 12,
            padding: '8px 16px',
            gap: 12,
          }}
        >
          <span style={{ fontSize: '18px', color: '#38bdf8' }}>🔍</span>
          <input
            type="text"
            placeholder="Search by movie title, star cast, or keywords (e.g. Inception, Marvel, Rajkumar)..."
            value={searchTerm}
            onChange={handleInputChange}
            autoFocus
            style={{
              flex: 1,
              background: 'transparent',
              border: 'none',
              color: '#f8fafc',
              fontSize: '15px',
              outline: 'none',
            }}
          />
          {searchTerm && (
            <button
              onClick={handleClear}
              style={{
                background: 'rgba(255, 255, 255, 0.1)',
                border: 'none',
                color: '#94a3b8',
                borderRadius: '50%',
                width: 26,
                height: 26,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '12px',
              }}
              title="Clear Search"
            >
              ✕
            </button>
          )}
        </div>

        {/* Filters Bar */}
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: 12,
            marginTop: 20,
            paddingTop: 16,
            borderTop: '1px solid rgba(255, 255, 255, 0.08)',
          }}
        >
          {/* Status Filters */}
          <div style={{ display: 'flex', gap: 8 }}>
            <button
              className={`pill ${statusFilter === 'ALL' ? 'active' : ''}`}
              onClick={() => setStatusFilter('ALL')}
            >
              All Movies
            </button>
            <button
              className={`pill ${statusFilter === 'NOW_SHOWING' ? 'active' : ''}`}
              onClick={() => setStatusFilter('NOW_SHOWING')}
            >
              <span className="live-indicator"></span> Now In Theatres
            </button>
            <button
              className={`pill ${statusFilter === 'UPCOMING' ? 'active' : ''}`}
              onClick={() => setStatusFilter('UPCOMING')}
            >
              🗓️ Upcoming
            </button>
          </div>

          {/* Genre Chips */}
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
            {GENRES_LIST.map((genre) => (
              <button
                key={genre}
                className={`pill ${selectedGenre === genre ? 'active' : ''}`}
                onClick={() => setSelectedGenre(genre)}
                style={{ fontSize: '12px', padding: '4px 10px' }}
              >
                {genre}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Results Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h2 style={{ fontSize: '18px', fontWeight: 600, color: '#e2e8f0' }}>
          {hasSearched ? `Search Results for "${searchTerm}"` : 'Featured & Now Playing'}
          <span style={{ fontSize: '13px', fontWeight: 400, color: '#94a3b8', marginLeft: 8 }}>
            ({filteredMovies.length} {filteredMovies.length === 1 ? 'title' : 'titles'} found)
          </span>
        </h2>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      {/* Loading State */}
      {loading && (
        <div style={{ textAlign: 'center', padding: '60px 0', color: '#94a3b8' }}>
          <div style={{ fontSize: '38px', marginBottom: '12px', animation: 'spin 2s linear infinite' }}>🍿</div>
          <p>Searching theatrical database...</p>
        </div>
      )}

      {/* Empty State */}
      {!loading && filteredMovies.length === 0 && (
        <div className="card" style={{ textAlign: 'center', padding: '50px 20px', borderRadius: 16 }}>
          <div style={{ fontSize: '42px', marginBottom: 12 }}>🎬</div>
          <h3 style={{ color: '#f8fafc', marginBottom: 8 }}>No movies found matching your criteria</h3>
          <p style={{ color: '#94a3b8', fontSize: '14px', maxWidth: 440, margin: '0 auto 20px' }}>
            We could not find any titles matching &quot;{searchTerm}&quot; with the selected filters. Try searching for a different keyword or resetting filters.
          </p>
          <button
            className="btn btn-secondary"
            onClick={() => {
              setSearchTerm('');
              setSelectedGenre('All');
              setStatusFilter('ALL');
              executeSearch('');
            }}
          >
            Reset Filters
          </button>
        </div>
      )}

      {/* Movies Grid */}
      {!loading && filteredMovies.length > 0 && (
        <div className="movies-grid">
          {filteredMovies.map((movie) => {
            const isUpcoming = movie.isUpcoming || (movie.releaseDate && new Date(movie.releaseDate) > new Date());
            return (
              <div
                key={movie.id || movie.tmdbId}
                className="movie-card"
                onClick={() => handleMovieCardClick(movie)}
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
                      🗓️ UPCOMING
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
                      🟢 IN THEATRES
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
                      marginBottom: 8,
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
                    <span>{movie.releaseDate ? new Date(movie.releaseDate).toLocaleDateString() : 'Active'}</span>
                    <button
                      className="btn btn-sm"
                      style={{ padding: '4px 12px', fontSize: '11px', fontWeight: 600 }}
                      onClick={(e) => {
                        e.stopPropagation();
                        handleMovieCardClick(movie);
                      }}
                    >
                      {isUpcoming ? 'Details' : 'Book'}
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

