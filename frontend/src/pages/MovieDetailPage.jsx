import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import api from '../services/api';
import { handlePosterError, getFallbackPosterSvg } from '../utils/posterHelper';
import CitySelectorModal from '../components/CitySelectorModal';

export default function MovieDetailPage({
  movie: propMovie,
  onBack,
  onSelectShow,
  selectedCity = '',
  onCityChange,
}) {
  const { id: paramId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();

  const [movie, setMovie] = useState(propMovie || location.state?.movie || null);
  const [loadingMovie, setLoadingMovie] = useState(!movie && Boolean(paramId));
  const [movieError, setMovieError] = useState('');

  const [city, setCity] = useState(selectedCity || '');
  const [cityModalOpen, setCityModalOpen] = useState(false);
  const [cities, setCities] = useState(['Mumbai', 'Bengaluru', 'Delhi NCR', 'Pune', 'Hyderabad']);
  const [nearbyTheatres, setNearbyTheatres] = useState([]);
  const [fallbackShows, setFallbackShows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reminderToast, setReminderToast] = useState('');

  // Date selector
  const [selectedDate, setSelectedDate] = useState(() => {
    return new Date().toISOString().split('T')[0];
  });

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

  // Load cities on mount
  useEffect(() => {
    api.get('/theatres/cities')
      .then((res) => {
        if (res.data?.data && res.data.data.length > 0) {
          setCities(res.data.data);
        }
      })
      .catch(() => {});
  }, []);

  // Fetch movie by ID if directly accessed via URL
  useEffect(() => {
    if (!movie && paramId) {
      setLoadingMovie(true);
      setMovieError('');
      api.get(`/movies/${paramId}`)
        .then((res) => {
          if (res.data?.data) {
            setMovie(res.data.data);
          } else {
            setMovieError('Movie not found');
          }
        })
        .catch((err) => {
          setMovieError(err.response?.data?.message || 'Failed to load movie details');
        })
        .finally(() => setLoadingMovie(false));
    }
  }, [paramId, movie]);

  // Sync city if parent changes
  useEffect(() => {
    if (selectedCity) {
      setCity(selectedCity);
    }
  }, [selectedCity]);

  const [resolvedMovieId, setResolvedMovieId] = useState(movie?.localMovieId || movie?.id || null);
  const [cast, setCast] = useState(movie?.cast || []);
  const [castLoading, setCastLoading] = useState(Boolean(movie && (!movie.cast || movie.cast.length === 0)));

  // Update resolvedMovieId when movie arrives
  useEffect(() => {
    if (movie) {
      setResolvedMovieId(movie.localMovieId || movie.id);
      if (movie.cast && movie.cast.length > 0) {
        setCast(movie.cast);
        setCastLoading(false);
      }
    }
  }, [movie]);

  // Fetch live star cast with photos
  useEffect(() => {
    if (!movie) return;
    if (movie.cast && movie.cast.length > 0) {
      setCast(movie.cast);
      setCastLoading(false);
      return;
    }
    const targetId = movie.tmdbId || movie.id;
    if (targetId) {
      setCastLoading(true);
      api.get(`/movies/${targetId}/cast`)
        .then((res) => {
          if (res.data?.data && Array.isArray(res.data.data)) {
            setCast(res.data.data);
          }
        })
        .catch(() => {})
        .finally(() => setCastLoading(false));
    }
  }, [movie?.id, movie?.tmdbId, movie?.cast]);

  // Sync TMDB movie to local database if needed
  useEffect(() => {
    if (movie && movie.id) {
      api.post(`/movies/tmdb/${movie.id}/sync-local`)
        .then((res) => {
          if (res.data?.data?.id) {
            setResolvedMovieId(res.data.data.id);
          }
        })
        .catch(() => {
          // If sync fails, continue with movie.id
        });
    }
  }, [movie?.id]);

  // Fetch shows whenever movie, city, or date changes
  useEffect(() => {
    if (!movie) return;
    if (movie.isUpcoming) {
      setLoading(false);
      return;
    }

    const fetchShows = async () => {
      try {
        setLoading(true);
        setError('');

        const targetId = resolvedMovieId || movie.id;
        const nearbyRes = await api.get(
          `/shows/nearby?city=${encodeURIComponent(city)}&movieId=${targetId}&date=${selectedDate}`
        );

        const theatreList = nearbyRes.data?.data || [];
        setNearbyTheatres(theatreList);

        if (theatreList.length === 0) {
          const fallbackRes = await api.get(`/shows?movieId=${targetId}`);
          setFallbackShows(fallbackRes.data?.data?.content || []);
        } else {
          setFallbackShows([]);
        }
      } catch (err) {
        setError(err.response?.data?.message || 'Failed to load shows');
      } finally {
        setLoading(false);
      }
    };

    fetchShows();
  }, [movie?.id, resolvedMovieId, movie?.isUpcoming, city, selectedDate]);

  const handleReminderClick = () => {
    setReminderToast(`🔔 Pre-booking reminder set for "${movie?.title}"! We'll notify you as soon as bookings go live.`);
    setTimeout(() => setReminderToast(''), 4500);
  };

  const handleBackAction = () => {
    if (onBack) {
      onBack();
    } else {
      navigate(-1);
    }
  };

  const handleShowSelection = (showPayload) => {
    if (onSelectShow) {
      onSelectShow(showPayload);
    }
    const showTargetId = showPayload.id || showPayload.showId;
    navigate(`/seat-selection/${showTargetId}`, { state: { show: showPayload, movie } });
  };

  if (loadingMovie) {
    return (
      <div className="container" style={{ textAlign: 'center', padding: '60px 0', color: '#94a3b8' }}>
        <div style={{ fontSize: '38px', marginBottom: '12px', animation: 'spin 2s linear infinite' }}>🍿</div>
        <p>Loading movie information...</p>
      </div>
    );
  }

  if (movieError || !movie) {
    return (
      <div className="container" style={{ maxWidth: 560, marginTop: 40, textAlign: 'center' }}>
        <div className="card" style={{ padding: '36px 24px', borderRadius: 16 }}>
          <div style={{ fontSize: '48px', marginBottom: '12px' }}>⚠️</div>
          <h2 style={{ color: '#f87171', marginBottom: '8px' }}>Movie Not Found</h2>
          <p style={{ color: '#94a3b8', fontSize: '14px', marginBottom: '24px' }}>
            {movieError || 'Could not find details for the requested movie.'}
          </p>
          <button className="btn" onClick={handleBackAction}>
            ← Go Back
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="container">
      <button className="btn btn-secondary" style={{ marginBottom: '20px' }} onClick={handleBackAction}>
        ← Back to Movies
      </button>

      {reminderToast && (
        <div className="alert alert-success" style={{ marginBottom: '20px' }}>
          {reminderToast}
        </div>
      )}

      {/* Movie Details Showcase Card */}
      <div
        className="card"
        style={{
          display: 'flex',
          gap: '28px',
          alignItems: 'flex-start',
          flexWrap: 'wrap',
          background: 'linear-gradient(135deg, rgba(22, 31, 48, 0.95), rgba(17, 24, 39, 0.95))',
        }}
      >
        <img
          src={movie.posterUrl || getFallbackPosterSvg(movie.title, movie.genre)}
          alt={movie.title}
          onError={(e) => handlePosterError(e, movie.title, movie.genre)}
          style={{
            width: '200px',
            borderRadius: '12px',
            boxShadow: '0 8px 24px rgba(0,0,0,0.5)',
            aspectRatio: '2/3',
            objectFit: 'cover',
          }}
        />

        <div style={{ flex: 1, minWidth: '280px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
            <h1 style={{ fontSize: '2rem', fontWeight: 800 }}>{movie.title}</h1>
            {movie.isUpcoming ? (
              <span
                style={{
                  background: 'linear-gradient(135deg, #f59e0b, #d97706)',
                  color: '#000',
                  padding: '4px 12px',
                  borderRadius: '6px',
                  fontWeight: 800,
                  fontSize: '12px',
                  letterSpacing: '0.5px',
                }}
              >
                COMING SOON
              </span>
            ) : (
              <span
                style={{
                  background: 'rgba(245, 158, 11, 0.15)',
                  border: '1px solid rgba(245, 158, 11, 0.4)',
                  color: '#fbbf24',
                  padding: '4px 10px',
                  borderRadius: '6px',
                  fontWeight: 700,
                  fontSize: '14px',
                }}
              >
                ⭐ {movie.rating ? movie.rating.toFixed(1) : '8.0'} / 10
              </span>
            )}
          </div>

          <div
            style={{
              color: '#94a3b8',
              fontSize: '14px',
              margin: '12px 0 16px',
              display: 'flex',
              gap: '12px',
              flexWrap: 'wrap',
            }}
          >
            <span><strong>Genre:</strong> {movie.genre}</span>
            <span>•</span>
            <span><strong>Language:</strong> {movie.language}</span>
            <span>•</span>
            <span><strong>Duration:</strong> {movie.durationMinutes} mins</span>
            {movie.releaseDate && (
              <>
                <span>•</span>
                <span><strong>Release:</strong> {new Date(movie.releaseDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}</span>
              </>
            )}
          </div>

          <p style={{ color: '#cbd5e1', lineHeight: '1.7', fontSize: '14px' }}>
            {movie.overview || movie.description || 'Experience this movie on the big screen with immersive surround sound.'}
          </p>

          {movie.isUpcoming && (
            <div
              style={{
                marginTop: '20px',
                padding: '16px',
                background: 'rgba(245, 158, 11, 0.1)',
                border: '1px solid rgba(245, 158, 11, 0.3)',
                borderRadius: '8px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                flexWrap: 'wrap',
                gap: '12px',
              }}
            >
              <div>
                <strong style={{ color: '#f59e0b' }}>🗓️ Theatrical Release:</strong>{' '}
                {movie.releaseDate
                  ? new Date(movie.releaseDate).toLocaleDateString('en-US', {
                      weekday: 'long',
                      year: 'numeric',
                      month: 'long',
                      day: 'numeric',
                    })
                  : 'Releasing Soon'}
                <div style={{ color: '#94a3b8', fontSize: '12px', marginTop: '4px' }}>
                  Bookings open 3–5 days prior to release date.
                </div>
              </div>
              <button
                className="btn btn-secondary"
                onClick={handleReminderClick}
                style={{ fontSize: '13px' }}
              >
                🔔 Remind Me
              </button>
            </div>
          )}

          {/* Cast Showcase with Photos */}
          <div style={{ marginTop: '24px' }}>
            <h3 style={{ fontSize: '16px', color: '#f8fafc', marginBottom: '12px', fontWeight: 700 }}>
              🎭 Top Cast &amp; Crew
            </h3>
            {castLoading ? (
              <div style={{ color: '#94a3b8', fontSize: '13px' }}>Loading cast information...</div>
            ) : cast && cast.length > 0 ? (
              <div
                style={{
                  display: 'flex',
                  gap: '12px',
                  overflowX: 'auto',
                  paddingBottom: '8px',
                }}
              >
                {cast.slice(0, 10).map((member, i) => (
                  <div
                    key={member.id || i}
                    style={{
                      minWidth: '85px',
                      maxWidth: '85px',
                      textAlign: 'center',
                      flexShrink: 0,
                    }}
                  >
                    <img
                      src={member.profileUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(member.name)}&background=1e293b&color=38bdf8`}
                      alt={member.name}
                      onError={(e) => {
                        e.target.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(member.name)}&background=1e293b&color=38bdf8`;
                      }}
                      style={{
                        width: '64px',
                        height: '64px',
                        borderRadius: '50%',
                        objectFit: 'cover',
                        margin: '0 auto 6px',
                        border: '2px solid rgba(56, 189, 248, 0.3)',
                      }}
                    />
                    <div
                      style={{
                        fontSize: '11px',
                        fontWeight: 600,
                        color: '#f8fafc',
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                      }}
                      title={member.name}
                    >
                      {member.name}
                    </div>
                    {member.character && (
                      <div
                        style={{
                          fontSize: '10px',
                          color: '#94a3b8',
                          whiteSpace: 'nowrap',
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                        }}
                        title={member.character}
                      >
                        {member.character}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ color: '#64748b', fontSize: '13px' }}>Cast information not available.</div>
            )}
          </div>
        </div>
      </div>

      {/* Shows and Theatres Section */}
      {!movie.isUpcoming && (
        <>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              margin: '32px 0 16px',
              flexWrap: 'wrap',
              gap: '12px',
            }}
          >
            <div>
              <h2 style={{ fontSize: '1.5rem', fontWeight: 700 }}>Select Theatres &amp; Showtimes</h2>
              <p style={{ color: '#94a3b8', fontSize: '13px', marginTop: '2px' }}>
                Showing screens in <strong>{city || 'Select City'}</strong>
              </p>
            </div>

            <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
              <button
                className="btn btn-secondary"
                style={{ padding: '8px 14px', fontSize: '13px' }}
                onClick={() => setCityModalOpen(true)}
              >
                📍 Change City ({city || 'Select'})
              </button>
            </div>
          </div>

          {/* Date Picker Ribbon */}
          <div style={{ display: 'flex', gap: '10px', marginBottom: '24px', overflowX: 'auto', paddingBottom: '4px' }}>
            {dateOptions.map((opt) => (
              <button
                key={opt.iso}
                className={`btn ${selectedDate === opt.iso ? '' : 'btn-secondary'}`}
                style={{ padding: '8px 16px', fontSize: '13px', fontWeight: 600 }}
                onClick={() => setSelectedDate(opt.iso)}
              >
                {opt.label}
              </button>
            ))}
          </div>

          {error && <div className="alert alert-danger">{error}</div>}

          {loading ? (
            <div style={{ textAlign: 'center', padding: '40px 0', color: '#94a3b8' }}>
              <div style={{ fontSize: '32px', marginBottom: '8px', animation: 'spin 2s linear infinite' }}>🍿</div>
              <p>Fetching scheduled showtimes...</p>
            </div>
          ) : nearbyTheatres.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {nearbyTheatres.map((theatre) => (
                <div key={theatre.theatreId} className="card" style={{ padding: '20px' }}>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'baseline',
                      borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
                      paddingBottom: '12px',
                      marginBottom: '16px',
                      flexWrap: 'wrap',
                      gap: '8px',
                    }}
                  >
                    <div>
                      <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#f8fafc' }}>
                        {theatre.theatreName}
                      </h3>
                      <p style={{ color: '#94a3b8', fontSize: '12px', marginTop: '2px' }}>
                        📍 {theatre.address || theatre.theatreAddress} • {theatre.city}
                      </p>
                    </div>
                    {theatre.distanceKm != null && (
                      <span style={{ color: '#38bdf8', fontSize: '12px', fontWeight: 600 }}>
                        {theatre.distanceKm.toFixed(1)} km away
                      </span>
                    )}
                  </div>

                  {/* Show slots grid */}
                  <div className="shows-grid">
                    {theatre.shows.map((show) => {
                      const showId = show.showId || show.id;
                      const timeStr = new Date(show.showTime).toLocaleTimeString('en-US', {
                        hour: '2-digit',
                        minute: '2-digit',
                      });

                      return (
                        <div
                          key={showId}
                          className="show-slot"
                          onClick={() =>
                            handleShowSelection({
                              ...show,
                              id: showId,
                              movieTitle: movie.title,
                              moviePoster: movie.posterUrl,
                              movieCast: cast,
                              theatreName: theatre.theatreName,
                              theatreAddress: theatre.address || theatre.theatreAddress,
                              theatreCity: theatre.city,
                            })
                          }
                        >
                          <div className="slot-time">{timeStr}</div>
                          <div className="slot-screen">{show.screenName}</div>
                          <div className="slot-seats">
                            <span className="seats-avail">🟢 {show.availableSeats} seats left</span>
                            <span className="slot-price">₹{show.basePrice}</span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="card" style={{ textAlign: 'center', padding: '40px 20px' }}>
              <h3>No screenings found in {city} for {selectedDate}</h3>
              <p style={{ color: '#94a3b8', margin: '12px 0 20px' }}>
                Check out other cities or try another date (e.g. Tomorrow).
              </p>

              <div style={{ display: 'flex', gap: '10px', justifyContent: 'center', flexWrap: 'wrap' }}>
                {cities
                  .filter((c) => c !== city)
                  .map((otherCity) => (
                    <button
                      key={otherCity}
                      className="btn btn-secondary"
                      onClick={() => setCity(otherCity)}
                    >
                      Switch to {otherCity}
                    </button>
                  ))}
              </div>

              {fallbackShows.length > 0 && (
                <div style={{ marginTop: '30px', textAlign: 'left' }}>
                  <h4 style={{ marginBottom: '12px', color: '#38bdf8' }}>
                    All scheduled shows across regions:
                  </h4>
                  <div className="shows-grid">
                    {fallbackShows.map((show) => (
                      <div
                        key={show.id}
                        className="show-slot"
                        onClick={() =>
                          handleShowSelection({
                            ...show,
                            movieTitle: movie.title,
                            moviePoster: movie.posterUrl,
                            movieCast: cast,
                          })
                        }
                      >
                        <div className="slot-time">
                          {new Date(show.showTime).toLocaleTimeString('en-US', {
                            hour: '2-digit',
                            minute: '2-digit',
                          })}
                        </div>
                        <div className="slot-movie">{show.theatreName}</div>
                        <div className="slot-screen">
                          {show.screenName} • ₹{show.basePrice}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </>
      )}

      {/* City & Locality Selector Modal */}
      <CitySelectorModal
        isOpen={cityModalOpen}
        onClose={() => setCityModalOpen(false)}
        selectedCity={city}
        onSelectCity={(newCity) => {
          setCity(newCity);
          if (onCityChange) onCityChange(newCity);
        }}
      />
      {/* TMDB Official Attribution Footer */}
      <footer
        style={{
          marginTop: '60px',
          padding: '24px 20px',
          borderTop: '1px solid rgba(255, 255, 255, 0.08)',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: '10px',
          textAlign: 'center',
          color: '#64748b',
          fontSize: '13px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap', justifyContent: 'center' }}>
          <img
            src="https://www.themoviedb.org/assets/2/v4/logos/v2/blue_short-8e7b30f73a4020692ccca9c88bafe5dcb6f8a62a4c6bc55cd9ba82bb2cd95f6c.svg"
            alt="The Movie Database (TMDB)"
            style={{ height: '15px', opacity: 0.85 }}
          />
          <span style={{ color: '#94a3b8' }}>
            This product uses the TMDB API but is not endorsed or certified by TMDB.
          </span>
        </div>
      </footer>
    </div>
  );
}
