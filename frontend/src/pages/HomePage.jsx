import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../services/api';
import { handlePosterError, getFallbackPosterSvg } from '../utils/posterHelper';

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

  // Exactly TWO sections: Section A (Now Showing) and Section B (Coming Soon)
  const [nowShowing, setNowShowing] = useState([]);
  const [comingSoon, setComingSoon] = useState([]);
  const [loadingNowShowing, setLoadingNowShowing] = useState(true);
  const [loadingComingSoon, setLoadingComingSoon] = useState(true);

  useEffect(() => {
    // Section A: Now Showing in Theatres (movies with verified current theatrical screening in India)
    api.get('/movies/now-playing?region=IN&page=1')
      .then((res) => {
        const list = res.data?.data?.results || res.data?.data || [];
        // Filter strictly to non-upcoming with current screening eligibility
        const filtered = list.filter((m) => !m.isUpcoming);
        setNowShowing(filtered.slice(0, 8));
      })
      .catch(() => {})
      .finally(() => setLoadingNowShowing(false));

    // Section B: Coming Soon (movies with verified future Indian theatrical release dates)
    api.get('/movies/upcoming?region=IN&page=1')
      .then((res) => {
        const list = res.data?.data?.results || res.data?.data || [];
        const today = new Date().toISOString().split('T')[0];
        // Strictly filter to release dates in the future
        const futureOnly = list.filter((m) => {
          if (!m.releaseDate) return Boolean(m.isUpcoming);
          return m.releaseDate > today;
        });
        // Sort by nearest upcoming release date
        futureOnly.sort((a, b) => (a.releaseDate || '').localeCompare(b.releaseDate || ''));
        setComingSoon(futureOnly.slice(0, 8));
      })
      .catch(() => {})
      .finally(() => setLoadingComingSoon(false));
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
    // Always navigate using the stable TMDB / Movie ID
    const targetId = movie.tmdbId || movie.id || movie.localMovieId;
    navigate(`/movie/${targetId}`, { state: { movie } });
  };

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
          padding: '40px 32px',
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
            <span>🎬 CinemaPulse • Theatrical Discovery</span>
            {selectedCity && <span>• 📍 {selectedCity}</span>}
          </div>

          <h1
            style={{
              fontSize: '34px',
              fontWeight: 800,
              color: '#f8fafc',
              lineHeight: 1.25,
              marginBottom: 14,
              letterSpacing: '-0.5px',
            }}
          >
            Book Movie Tickets Online with Live Screen Schedules
          </h1>
          <p style={{ color: '#94a3b8', fontSize: '15px', lineHeight: 1.6, marginBottom: 26 }}>
            Discover current theatrical blockbusters in Indian cinemas and explore upcoming premieres with verified release dates.
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
              placeholder="Search movies by title, actor, or genre..."
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
          <div style={{ display: 'flex', gap: 12, marginTop: 18, flexWrap: 'wrap' }}>
            <Link
              to="/theatres"
              className="btn btn-secondary"
              style={{ fontSize: '13px', padding: '7px 16px', textDecoration: 'none', display: 'inline-flex', alignItems: 'center', gap: '6px' }}
            >
              📍 Discover Theatres &amp; Showtimes ➔
            </Link>
          </div>
        </div>
      </section>

      {/* =========================================================================
          SECTION A: NOW SHOWING IN THEATRES (FIRST AT THE TOP)
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
          <div>
            <h2 style={{ fontSize: '22px', fontWeight: 800, color: '#f8fafc', margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}>
              <span className="live-indicator"></span> Now Showing in Theatres
            </h2>
            <p style={{ color: '#94a3b8', fontSize: '13px', margin: '4px 0 0 0' }}>
              Verified live theatrical screenings in Indian cinemas
            </p>
          </div>

          <Link
            to="/now-showing"
            style={{ color: '#38bdf8', fontSize: '13px', fontWeight: 600, textDecoration: 'none' }}
          >
            View All Now Showing ➔
          </Link>
        </div>

        {/* Section A Movie Grid */}
        {loadingNowShowing ? (
          <div style={{ textAlign: 'center', padding: '40px 0', color: '#94a3b8' }}>
            <div style={{ fontSize: '38px', marginBottom: '12px', animation: 'spin 2s linear infinite' }}>🍿</div>
            <p>Loading theatrical titles...</p>
          </div>
        ) : nowShowing.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '40px 20px', borderRadius: 12 }}>
            <p style={{ color: '#94a3b8' }}>No verified theatrical screenings currently reported for this region.</p>
          </div>
        ) : (
          <div className="movies-grid">
            {nowShowing.map((movie) => {
              const ratingVal = movie.rating && movie.rating > 0 ? movie.rating.toFixed(1) : null;
              return (
                <div
                  key={`now-${movie.id || movie.tmdbId}`}
                  className="movie-card"
                  onClick={() => handleMovieClick(movie)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      handleMovieClick(movie);
                    }
                  }}
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
                    <span
                      style={{
                        position: 'absolute',
                        top: 8,
                        right: 8,
                        background: 'rgba(16, 185, 129, 0.92)',
                        color: '#fff',
                        fontSize: '11px',
                        fontWeight: 700,
                        padding: '3px 8px',
                        borderRadius: 6,
                        backdropFilter: 'blur(4px)',
                      }}
                    >
                      🟢 IN CINEMAS
                    </span>

                    {ratingVal && (
                      <span
                        style={{
                          position: 'absolute',
                          bottom: 8,
                          left: 8,
                          background: 'rgba(15, 23, 42, 0.88)',
                          color: '#fbbf24',
                          fontSize: '12px',
                          fontWeight: 700,
                          padding: '3px 8px',
                          borderRadius: 6,
                          border: '1px solid rgba(251, 191, 36, 0.3)',
                        }}
                      >
                        ★ {ratingVal}
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
                      {movie.genre || 'Action / Drama'}
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
                        Book Show
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
          SECTION B: COMING SOON (BELOW NOW SHOWING)
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
          <div>
            <h2 style={{ fontSize: '22px', fontWeight: 800, color: '#f8fafc', margin: 0 }}>
              🗓️ Coming Soon to Indian Theatres
            </h2>
            <p style={{ color: '#94a3b8', fontSize: '13px', margin: '4px 0 0 0' }}>
              Upcoming Indian theatrical releases with verified future premiere dates
            </p>
          </div>

          <Link
            to="/upcoming"
            style={{ color: '#38bdf8', fontSize: '13px', fontWeight: 600, textDecoration: 'none' }}
          >
            View All Upcoming ➔
          </Link>
        </div>

        {/* Section B Movie Grid */}
        {loadingComingSoon ? (
          <div style={{ textAlign: 'center', padding: '40px 0', color: '#94a3b8' }}>
            <div style={{ fontSize: '38px', marginBottom: '12px', animation: 'spin 2s linear infinite' }}>🍿</div>
            <p>Loading upcoming releases...</p>
          </div>
        ) : comingSoon.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '40px 20px', borderRadius: 12 }}>
            <p style={{ color: '#94a3b8' }}>No upcoming theatrical releases scheduled in the near future.</p>
          </div>
        ) : (
          <div className="movies-grid">
            {comingSoon.map((movie) => {
              const releaseDateFormatted = movie.releaseDate
                ? new Date(movie.releaseDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
                : 'COMING SOON';

              return (
                <div
                  key={`soon-${movie.id || movie.tmdbId}`}
                  className="movie-card"
                  onClick={() => handleMovieClick(movie)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      handleMovieClick(movie);
                    }
                  }}
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
                    <span
                      style={{
                        position: 'absolute',
                        top: 8,
                        right: 8,
                        background: 'rgba(217, 119, 6, 0.92)',
                        color: '#fff',
                        fontSize: '11px',
                        fontWeight: 700,
                        padding: '3px 8px',
                        borderRadius: 6,
                        backdropFilter: 'blur(4px)',
                      }}
                    >
                      🗓️ {releaseDateFormatted}
                    </span>
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
                      {movie.genre || 'Action / Drama'}
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
                      <span style={{ color: '#f59e0b', fontWeight: 600 }}>
                        Releases {releaseDateFormatted}
                      </span>
                      <button
                        className="btn btn-secondary btn-sm"
                        style={{ padding: '4px 12px', fontSize: '11px', fontWeight: 600 }}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleMovieClick(movie);
                        }}
                      >
                        View Details
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}
