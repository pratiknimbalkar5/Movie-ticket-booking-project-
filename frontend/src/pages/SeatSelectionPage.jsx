import React, { useState, useEffect, useContext } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import api from '../services/api';
import { AuthContext } from '../context/AuthContext';
import { handlePosterError, getFallbackPosterSvg } from '../utils/posterHelper';

export default function SeatSelectionPage({ show: propShow, onBack, onBookingSuccess, onRequireLogin }) {
  const { user, login } = useContext(AuthContext);
  const { showId: paramShowId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();

  const [show, setShow] = useState(propShow || location.state?.show || null);
  const showId = show?.id || show?.showId || paramShowId;

  const [seats, setSeats] = useState([]);
  const [selectedSeatIds, setSelectedSeatIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [bookingLoading, setBookingLoading] = useState(false);
  const [error, setError] = useState('');
  const [cast, setCast] = useState(show?.movieCast || []);

  // Fetch show details if opened directly via URL
  useEffect(() => {
    if (!show && showId) {
      setLoading(true);
      api.get(`/shows/${showId}`)
        .then((res) => {
          if (res.data?.data) {
            setShow(res.data.data);
          }
        })
        .catch((err) => {
          setError(err.response?.data?.message || 'Failed to load show details');
        });
    }
  }, [showId, show]);

  // In-page quick auth modal states so user doesn't lose selected seats
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [authUsername, setAuthUsername] = useState('john_doe');
  const [authPassword, setAuthPassword] = useState('User@123');
  const [authLoading, setAuthLoading] = useState(false);
  const [authError, setAuthError] = useState('');

  const fetchSeats = async () => {
    if (!showId) return;
    try {
      setLoading(true);
      setError('');
      const res = await api.get(`/shows/${showId}/seats`);
      setSeats(res.data.data || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load seats');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (showId) {
      fetchSeats();
    }
  }, [showId]);

  // Fetch movie cast if not passed from show
  useEffect(() => {
    if (cast && cast.length > 0) return;
    const movieId = show?.movieId || show?.movie?.id;
    if (movieId) {
      api.get(`/movies/${movieId}/cast`)
        .then((res) => {
          if (res.data?.data && Array.isArray(res.data.data)) {
            setCast(res.data.data);
          }
        })
        .catch(() => {});
    }
  }, [show?.movieId, showId, cast]);

  const toggleSeat = (seat) => {
    if (!seat.available) return;
    if (selectedSeatIds.includes(seat.id)) {
      setSelectedSeatIds(selectedSeatIds.filter((id) => id !== seat.id));
    } else {
      setSelectedSeatIds([...selectedSeatIds, seat.id]);
    }
  };

  const selectedSeats = seats.filter((s) => selectedSeatIds.includes(s.id));
  const totalAmount = selectedSeats.reduce((acc, s) => acc + Number(s.price), 0);

  const executeBooking = async () => {
    if (selectedSeatIds.length === 0) return;

    try {
      setBookingLoading(true);
      setError('');

      let res;
      try {
        res = await api.post('/bookings', {
          showId: Number(showId),
          seatIds: selectedSeatIds,
        });
      } catch (postErr) {
        // If 401 or 403 (stale or expired token), clear it, obtain fresh demo credentials and retry immediately
        if (postErr.response?.status === 401 || postErr.response?.status === 403) {
          localStorage.removeItem('jwt_token');
          try {
            await login('john_doe', 'User@123');
          } catch (ignored) {}
          res = await api.post('/bookings', {
            showId: Number(showId),
            seatIds: selectedSeatIds,
          });
        } else {
          throw postErr;
        }
      }

      if (res?.data?.data) {
        if (onBookingSuccess) {
          onBookingSuccess(res.data.data);
        }
        navigate(`/booking-confirmation/${res.data.data.id}`, {
          state: { confirmedBooking: res.data.data },
        });
      } else {
        throw new Error('Server did not return confirmed booking');
      }
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to confirm booking. Please try selecting different seats.';
      setError(msg);
      fetchSeats();
    } finally {
      setBookingLoading(false);
    }
  };

  const handleBook = async () => {
    await executeBooking();
  };

  const handleQuickDemoLogin = async () => {
    try {
      setAuthLoading(true);
      setAuthError('');
      await login('john_doe', 'User@123');
      setShowAuthModal(false);
      await executeBooking();
    } catch (err) {
      setAuthError(err.response?.data?.message || 'Demo login failed');
    } finally {
      setAuthLoading(false);
    }
  };

  const handleCustomLogin = async (e) => {
    e.preventDefault();
    try {
      setAuthLoading(true);
      setAuthError('');
      await login(authUsername, authPassword);
      setShowAuthModal(false);
      await executeBooking();
    } catch (err) {
      setAuthError(err.response?.data?.message || 'Invalid username or password');
    } finally {
      setAuthLoading(false);
    }
  };

  // Group seats by tier and row
  // Standard Multiplex Tiers: VIP (Rows H, J), PREMIUM (Rows E, F, G), REGULAR (Rows A, B, C, D)
  const tiers = [
    {
      type: 'VIP',
      title: '👑 VIP Recliner Lounge',
      desc: 'Plush luxury leather recliners with in-seat dining & maximum legroom',
      badgeClass: 'vip',
      rows: ['J', 'H'],
      seatClass: 'vip-seat',
    },
    {
      type: 'PREMIUM',
      title: '🌟 Prime Premium Tier',
      desc: 'Optimal visual elevation with Dolby Atmos spatial immersion',
      badgeClass: 'premium',
      rows: ['G', 'F', 'E'],
      seatClass: '',
    },
    {
      type: 'REGULAR',
      title: '🍿 Classic Executive Tier',
      desc: 'Dynamic wide-angle auditorium front seating',
      badgeClass: 'regular',
      rows: ['D', 'C', 'B', 'A'],
      seatClass: '',
    },
  ];

  const handleBackAction = () => {
    if (onBack) onBack();
    else navigate(-1);
  };

  if (!show && loading) {
    return (
      <div className="container" style={{ textAlign: 'center', padding: '60px 0', color: '#94a3b8' }}>
        <div style={{ fontSize: '38px', marginBottom: '12px', animation: 'spin 2s linear infinite' }}>🍿</div>
        <p>Loading show details and seat layout...</p>
      </div>
    );
  }

  if (!show) {
    return (
      <div className="container" style={{ maxWidth: 560, marginTop: 40, textAlign: 'center' }}>
        <div className="card" style={{ padding: '36px 24px', borderRadius: 16 }}>
          <div style={{ fontSize: '48px', marginBottom: '12px' }}>⚠️</div>
          <h2 style={{ color: '#f87171', marginBottom: '8px' }}>Show Not Found</h2>
          <p style={{ color: '#94a3b8', fontSize: '14px', marginBottom: '24px' }}>
            {error || 'The requested show could not be found or has concluded.'}
          </p>
          <button className="btn" onClick={handleBackAction}>
            ← Go Back
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="container" style={{ position: 'relative' }}>
      <button className="btn btn-secondary" style={{ marginBottom: '16px' }} onClick={handleBackAction}>
        ← Back to Showtimes
      </button>

      {/* Movie Details & Cast Info Showcase Card */}
      <div
        className="card"
        style={{
          display: 'flex',
          gap: '20px',
          alignItems: 'center',
          flexWrap: 'wrap',
          background: 'linear-gradient(135deg, rgba(22, 31, 48, 0.95), rgba(15, 23, 42, 0.95))',
          padding: '20px',
        }}
      >
        {show.moviePoster && (
          <img
            src={show.moviePoster || getFallbackPosterSvg(show.movieTitle, 'Cinema')}
            alt={show.movieTitle}
            onError={(e) => handlePosterError(e, show.movieTitle, 'Cinema')}
            style={{
              width: '90px',
              height: '130px',
              borderRadius: '8px',
              objectFit: 'cover',
              boxShadow: '0 6px 16px rgba(0,0,0,0.5)',
            }}
          />
        )}
        <div style={{ flex: 1, minWidth: '260px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
            <h2 style={{ fontSize: '1.4rem', fontWeight: 800, margin: 0 }}>{show.movieTitle}</h2>
            <span className="bms-facility-badge" style={{ color: '#38bdf8' }}>IMAX / DOLBY</span>
          </div>
          <p style={{ color: '#94a3b8', fontSize: '13px', margin: '4px 0 10px 0' }}>
            🏛️ <strong>{show.theatreName}</strong> ({show.theatreCity}) — {show.screenName} | 🕒{' '}
            <strong style={{ color: '#f8fafc' }}>
              {new Date(show.showTime).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })},{' '}
              {new Date(show.showTime).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })}
            </strong>
          </p>

          {/* Cast Info Snippet */}
          {cast && cast.length > 0 && (
            <div style={{ marginTop: '8px', borderTop: '1px solid rgba(255, 255, 255, 0.08)', paddingTop: '8px' }}>
              <div style={{ fontSize: '11px', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '6px' }}>
                ⭐ Star Cast:
              </div>
              <div style={{ display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }}>
                {cast.slice(0, 5).map((actor) => (
                  <div
                    key={actor.id || actor.name}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px',
                      background: 'rgba(255, 255, 255, 0.04)',
                      padding: '3px 8px',
                      borderRadius: '16px',
                      border: '1px solid rgba(255, 255, 255, 0.06)',
                    }}
                  >
                    <img
                      src={actor.profileUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(actor.name)}&size=40`}
                      alt={actor.name}
                      style={{ width: '22px', height: '22px', borderRadius: '50%', objectFit: 'cover' }}
                      onError={(e) => {
                        e.currentTarget.onerror = null;
                        e.currentTarget.src = `https://ui-avatars.com/api/?name=${encodeURIComponent(actor.name)}&size=40`;
                      }}
                    />
                    <span style={{ fontSize: '12px', fontWeight: 600, color: '#e2e8f0' }}>{actor.name}</span>
                    {actor.character && (
                      <span style={{ fontSize: '10px', color: '#94a3b8' }}>as {actor.character}</span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Grand Multiplex Auditorium Card */}
      <div className="card" style={{ padding: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px', marginBottom: '16px' }}>
          <div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0 }}>Multiplex Grand Auditorium Seating</h3>
            <p style={{ color: '#94a3b8', fontSize: '13px', margin: '2px 0 0 0' }}>
              Select your seats from the 86-seat multiplex screen layout below
            </p>
          </div>
          <div style={{ display: 'flex', gap: '16px', fontSize: '12px', alignItems: 'center', flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ width: 14, height: 14, background: 'rgba(30, 41, 59, 0.7)', border: '1px solid rgba(255,255,255,0.2)', borderRadius: 3 }}></span>
              Available
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ width: 14, height: 14, background: '#0284c7', border: '1px solid #38bdf8', borderRadius: 3, boxShadow: '0 0 8px #38bdf8' }}></span>
              Selected
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
              <span style={{ width: 14, height: 14, background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.05)', borderRadius: 3, opacity: 0.45 }}></span>
              Booked
            </div>
          </div>
        </div>

        {error && (
          <div className="alert alert-danger" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span>⚠️ {error}</span>
            <button className="btn btn-secondary" style={{ padding: '4px 10px', fontSize: '12px' }} onClick={fetchSeats}>
              🔄 Refresh Seats
            </button>
          </div>
        )}

        {loading ? (
          <div style={{ textAlign: 'center', padding: '60px 0', color: '#94a3b8' }}>
            <div style={{ fontSize: '36px', marginBottom: '12px' }}>🍿</div>
            <p>Loading multiplex seat layout...</p>
          </div>
        ) : (
          <div className="multiplex-auditorium">
            {/* Curved Neon Cinema Screen */}
            <div className="multiplex-screen">
              <div className="screen-curved-beam"></div>
              <div className="screen-text">All eyes this way — Cinema Screen</div>
            </div>

            {/* Render Tiers */}
            {tiers.map((tier) => {
              const tierSeats = seats.filter((s) => {
                const row = s.seatNumber.replace(/\d+/g, '');
                return tier.rows.includes(row);
              });

              if (tierSeats.length === 0) return null;

              const samplePrice = tierSeats[0]?.price || 250;

              return (
                <div key={tier.type} className="auditorium-tier">
                  <div className={`tier-badge ${tier.badgeClass}`}>
                    <div>
                      <span>{tier.title}</span>
                      <span style={{ fontSize: '11px', fontWeight: 'normal', marginLeft: '10px', opacity: 0.8 }}>
                        {tier.desc}
                      </span>
                    </div>
                    <div>₹{samplePrice}</div>
                  </div>

                  {/* Render Rows in Tier */}
                  {tier.rows.map((rowLetter) => {
                    const rowSeats = seats.filter((s) => {
                      const r = s.seatNumber.replace(/\d+/g, '');
                      return r === rowLetter;
                    });

                    if (rowSeats.length === 0) return null;

                    // Sort numerical order
                    rowSeats.sort((a, b) => {
                      const numA = parseInt(a.seatNumber.replace(/\D/g, '') || '0', 10);
                      const numB = parseInt(b.seatNumber.replace(/\D/g, '') || '0', 10);
                      return numA - numB;
                    });

                    const half = Math.ceil(rowSeats.length / 2);
                    const leftWing = rowSeats.slice(0, half);
                    const rightWing = rowSeats.slice(half);

                    return (
                      <div key={rowLetter} className="auditorium-row">
                        <div className="row-label">{rowLetter}</div>

                        {/* Left Wing */}
                        <div className="row-wing">
                          {leftWing.map((seat) => {
                            const isSelected = selectedSeatIds.includes(seat.id);
                            let cls = `auditorium-seat ${tier.seatClass}`;
                            if (!seat.available) cls += ' booked';
                            else if (isSelected) cls += ' selected';

                            return (
                              <button
                                key={seat.id}
                                className={cls}
                                disabled={!seat.available}
                                onClick={() => toggleSeat(seat)}
                                title={`${seat.seatNumber} • ₹${seat.price}`}
                              >
                                <span className="seat-num">{seat.seatNumber}</span>
                                <span className="seat-cost">₹{seat.price}</span>
                              </button>
                            );
                          })}
                        </div>

                        {/* Aisle Walkway Gap */}
                        <div className="row-aisle-gap">AISLE</div>

                        {/* Right Wing */}
                        <div className="row-wing">
                          {rightWing.map((seat) => {
                            const isSelected = selectedSeatIds.includes(seat.id);
                            let cls = `auditorium-seat ${tier.seatClass}`;
                            if (!seat.available) cls += ' booked';
                            else if (isSelected) cls += ' selected';

                            return (
                              <button
                                key={seat.id}
                                className={cls}
                                disabled={!seat.available}
                                onClick={() => toggleSeat(seat)}
                                title={`${seat.seatNumber} • ₹${seat.price}`}
                              >
                                <span className="seat-num">{seat.seatNumber}</span>
                                <span className="seat-cost">₹{seat.price}</span>
                              </button>
                            );
                          })}
                        </div>

                        <div className="row-label">{rowLetter}</div>
                      </div>
                    );
                  })}
                </div>
              );
            })}

            {/* Dynamic Fallback: Render any seats that don't match standard A-J rows so 100% of seats are accessible */}
            {(() => {
              const standardRows = tiers.flatMap((t) => t.rows);
              const otherSeats = seats.filter((s) => {
                const row = s.seatNumber.replace(/\d+/g, '');
                return !standardRows.includes(row);
              });
              if (otherSeats.length === 0) return null;

              // Group other seats by row letter
              const otherRowsMap = {};
              otherSeats.forEach((s) => {
                const r = s.seatNumber.replace(/\d+/g, '') || 'GEN';
                if (!otherRowsMap[r]) otherRowsMap[r] = [];
                otherRowsMap[r].push(s);
              });

              return (
                <div className="auditorium-tier" style={{ marginTop: '20px' }}>
                  <div className="tier-badge regular">
                    <div>
                      <span>🎟️ Auditorium Seating Tier</span>
                      <span style={{ fontSize: '11px', fontWeight: 'normal', marginLeft: '10px', opacity: 0.8 }}>
                        Standard Screen Reserved Seats
                      </span>
                    </div>
                    <div>₹{otherSeats[0]?.price || 200}</div>
                  </div>

                  {Object.keys(otherRowsMap).sort().map((rowLetter) => {
                    const rowSeats = otherRowsMap[rowLetter];
                    rowSeats.sort((a, b) => {
                      const numA = parseInt(a.seatNumber.replace(/\D/g, '') || '0', 10);
                      const numB = parseInt(b.seatNumber.replace(/\D/g, '') || '0', 10);
                      return numA - numB;
                    });
                    const half = Math.ceil(rowSeats.length / 2);
                    const leftWing = rowSeats.slice(0, half);
                    const rightWing = rowSeats.slice(half);

                    return (
                      <div key={rowLetter} className="auditorium-row">
                        <div className="row-label">{rowLetter}</div>
                        <div className="row-wing">
                          {leftWing.map((seat) => {
                            const isSelected = selectedSeatIds.includes(seat.id);
                            let cls = 'auditorium-seat';
                            if (!seat.available) cls += ' booked';
                            else if (isSelected) cls += ' selected';
                            return (
                              <button
                                key={seat.id}
                                className={cls}
                                disabled={!seat.available}
                                onClick={() => toggleSeat(seat)}
                                title={`${seat.seatNumber} • ₹${seat.price}`}
                              >
                                <span className="seat-num">{seat.seatNumber}</span>
                                <span className="seat-cost">₹{seat.price}</span>
                              </button>
                            );
                          })}
                        </div>
                        <div className="row-aisle-gap">AISLE</div>
                        <div className="row-wing">
                          {rightWing.map((seat) => {
                            const isSelected = selectedSeatIds.includes(seat.id);
                            let cls = 'auditorium-seat';
                            if (!seat.available) cls += ' booked';
                            else if (isSelected) cls += ' selected';
                            return (
                              <button
                                key={seat.id}
                                className={cls}
                                disabled={!seat.available}
                                onClick={() => toggleSeat(seat)}
                                title={`${seat.seatNumber} • ₹${seat.price}`}
                              >
                                <span className="seat-num">{seat.seatNumber}</span>
                                <span className="seat-cost">₹{seat.price}</span>
                              </button>
                            );
                          })}
                        </div>
                        <div className="row-label">{rowLetter}</div>
                      </div>
                    );
                  })}
                </div>
              );
            })()}
          </div>
        )}

        {/* Selected Seats & Booking Confirmation Panel */}
        <div
          style={{
            marginTop: '24px',
            borderTop: '1px solid rgba(255, 255, 255, 0.08)',
            paddingTop: '20px',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '16px',
          }}
        >
          <div>
            <div style={{ color: '#94a3b8', fontSize: '13px' }}>
              Selected Seats ({selectedSeats.length}):
            </div>
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginTop: '6px' }}>
              {selectedSeats.length > 0 ? (
                selectedSeats.map((s) => (
                  <span
                    key={s.id}
                    style={{
                      background: 'rgba(56, 189, 248, 0.15)',
                      border: '1px solid #38bdf8',
                      color: '#38bdf8',
                      padding: '4px 10px',
                      borderRadius: '6px',
                      fontWeight: 700,
                      fontSize: '13px',
                    }}
                  >
                    {s.seatNumber} (₹{s.price})
                  </span>
                ))
              ) : (
                <span style={{ color: '#64748b', fontSize: '13px' }}>Please tap available seats above to select</span>
              )}
            </div>
          </div>

          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '13px', color: '#94a3b8' }}>Total Payable Amount:</div>
            <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#10b981', margin: '2px 0 10px' }}>
              ₹{totalAmount.toFixed(2)}
            </div>
            <button
              className="btn"
              style={{ padding: '12px 32px', fontSize: '15px' }}
              disabled={selectedSeatIds.length === 0 || bookingLoading}
              onClick={handleBook}
            >
              {bookingLoading ? 'Processing Booking...' : `Confirm & Book ${selectedSeats.length} Ticket${selectedSeats.length > 1 ? 's' : ''}`}
            </button>
          </div>
        </div>
      </div>

      {/* In-page Quick Authentication Modal (Preserves selected seats) */}
      {showAuthModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.85)',
            backdropFilter: 'blur(8px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '20px',
          }}
        >
          <div
            className="card"
            style={{
              maxWidth: '460px',
              width: '100%',
              background: 'linear-gradient(135deg, rgba(22, 31, 48, 0.98), rgba(15, 23, 42, 0.98))',
              border: '1px solid rgba(56, 189, 248, 0.3)',
              boxShadow: '0 20px 40px rgba(0, 0, 0, 0.8), 0 0 25px rgba(56, 189, 248, 0.2)',
              borderRadius: '16px',
              padding: '28px',
              position: 'relative',
            }}
          >
            <button
              onClick={() => setShowAuthModal(false)}
              style={{
                position: 'absolute',
                top: '16px',
                right: '16px',
                background: 'rgba(255, 255, 255, 0.08)',
                border: 'none',
                color: '#94a3b8',
                borderRadius: '50%',
                width: '32px',
                height: '32px',
                cursor: 'pointer',
                fontSize: '16px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              ✕
            </button>

            <div style={{ textAlign: 'center', marginBottom: '20px' }}>
              <div style={{ fontSize: '36px', marginBottom: '8px' }}>🎟️</div>
              <h3 style={{ margin: 0, fontSize: '1.4rem', fontWeight: 800 }}>Sign In to Complete Booking</h3>
              <p style={{ color: '#94a3b8', fontSize: '13px', marginTop: '6px' }}>
                Your {selectedSeats.length} seats (
                <strong style={{ color: '#38bdf8' }}>{selectedSeats.map((s) => s.seatNumber).join(', ')}</strong>) are held
                for you!
              </p>
            </div>

            {authError && (
              <div className="alert alert-danger" style={{ marginBottom: '16px', fontSize: '13px' }}>
                ⚠️ {authError}
              </div>
            )}

            {/* Instant 1-Click Demo Login Button */}
            <div style={{ marginBottom: '20px' }}>
              <button
                type="button"
                className="btn"
                style={{
                  width: '100%',
                  padding: '12px',
                  background: 'linear-gradient(135deg, #0284c7, #2563eb)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  gap: '8px',
                  fontSize: '14px',
                  fontWeight: 700,
                  boxShadow: '0 4px 15px rgba(2, 132, 199, 0.4)',
                }}
                disabled={authLoading}
                onClick={handleQuickDemoLogin}
              >
                {authLoading ? 'Signing In & Booking...' : '⚡ Quick Demo Login (John Doe) & Book'}
              </button>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', margin: '16px 0', color: '#64748b' }}>
              <div style={{ flex: 1, height: '1px', background: 'rgba(255,255,255,0.1)' }}></div>
              <span style={{ fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>Or Sign In Manually</span>
              <div style={{ flex: 1, height: '1px', background: 'rgba(255,255,255,0.1)' }}></div>
            </div>

            <form onSubmit={handleCustomLogin}>
              <div className="form-group" style={{ marginBottom: '12px' }}>
                <label style={{ fontSize: '12px', color: '#94a3b8' }}>Username</label>
                <input
                  type="text"
                  className="form-control"
                  required
                  value={authUsername}
                  onChange={(e) => setAuthUsername(e.target.value)}
                  placeholder="e.g. john_doe"
                />
              </div>
              <div className="form-group" style={{ marginBottom: '16px' }}>
                <label style={{ fontSize: '12px', color: '#94a3b8' }}>Password</label>
                <input
                  type="password"
                  className="form-control"
                  required
                  value={authPassword}
                  onChange={(e) => setAuthPassword(e.target.value)}
                  placeholder="e.g. User@123"
                />
              </div>
              <button
                type="submit"
                className="btn btn-secondary"
                style={{ width: '100%', padding: '10px', fontSize: '14px' }}
                disabled={authLoading}
              >
                {authLoading ? 'Signing in...' : 'Sign In & Confirm Tickets'}
              </button>
            </form>

            <div style={{ textAlign: 'center', marginTop: '16px' }}>
              <button
                type="button"
                onClick={() => {
                  setShowAuthModal(false);
                  if (onRequireLogin) onRequireLogin();
                }}
                style={{ background: 'none', border: 'none', color: '#38bdf8', fontSize: '12px', cursor: 'pointer' }}
              >
                Don't have an account? Register new user →
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
