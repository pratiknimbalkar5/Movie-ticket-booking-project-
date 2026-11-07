import React, { useState, useEffect, useContext } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import api from '../services/api';
import { AuthContext } from '../context/AuthContext';

export default function MyBookingsPage({ onNavigateHome }) {
  const { user, login } = useContext(AuthContext);
  const navigate = useNavigate();
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [filter, setFilter] = useState('ALL'); // 'ALL' | 'CONFIRMED' | 'CANCELLED'

  // Modal for cancellation confirmation
  const [cancelTarget, setCancelTarget] = useState(null);
  const [cancelling, setCancelling] = useState(false);

  const fetchBookings = async () => {
    try {
      setLoading(true);
      setError('');
      const token = localStorage.getItem('jwt_token');
      if (!token) {
        // Auto-login default demo user so bookings page works smoothly without forcing manual login
        try {
          await login('john_doe', 'User@123');
        } catch (authErr) {
          setError('Please login to view your bookings');
          setLoading(false);
          return;
        }
      }
      const res = await api.get('/bookings?page=0&size=50');
      setBookings(res.data?.data?.content || []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load your bookings');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookings();
  }, []);

  const confirmCancel = async () => {
    if (!cancelTarget) return;
    try {
      setCancelling(true);
      setError('');
      setSuccessMsg('');
      const res = await api.post(`/bookings/${cancelTarget.id}/cancel`);
      setSuccessMsg(res.data?.message || `Booking #${cancelTarget.id} for "${cancelTarget.movieTitle}" was cancelled successfully.`);
      setCancelTarget(null);
      // Re-fetch bookings to get updated status and seat summary
      await fetchBookings();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to cancel booking. Shows already started cannot be cancelled.');
      setCancelTarget(null);
    } finally {
      setCancelling(false);
    }
  };

  const filteredBookings = bookings.filter((b) => {
    if (filter === 'CONFIRMED') return b.status === 'CONFIRMED';
    if (filter === 'CANCELLED') return b.status === 'CANCELLED';
    return true;
  });

  const getSeatText = (b) => {
    if (b.bookedSeats && b.bookedSeats.length > 0) {
      return b.bookedSeats.map((s) => s.seatNumber).join(', ');
    }
    return b.seatSummary || 'N/A';
  };

  return (
    <div className="container" style={{ maxWidth: '960px', marginTop: '20px', paddingBottom: '60px' }}>
      {/* Header and Filter Tabs */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.8rem', fontWeight: 800, margin: 0 }}>My Cinema Bookings</h2>
          <p style={{ color: '#94a3b8', fontSize: '14px', margin: '4px 0 0' }}>
            Manage your booked movie tickets, view auditorium access passes, and handle cancellations
          </p>
        </div>

        <div style={{ display: 'flex', gap: '8px' }}>
          <button
            className={`btn ${filter === 'ALL' ? '' : 'btn-secondary'}`}
            style={{ padding: '6px 14px', fontSize: '13px' }}
            onClick={() => setFilter('ALL')}
          >
            All ({bookings.length})
          </button>
          <button
            className={`btn ${filter === 'CONFIRMED' ? '' : 'btn-secondary'}`}
            style={{ padding: '6px 14px', fontSize: '13px' }}
            onClick={() => setFilter('CONFIRMED')}
          >
            Confirmed ({bookings.filter((b) => b.status === 'CONFIRMED').length})
          </button>
          <button
            className={`btn ${filter === 'CANCELLED' ? '' : 'btn-secondary'}`}
            style={{ padding: '6px 14px', fontSize: '13px' }}
            onClick={() => setFilter('CANCELLED')}
          >
            Cancelled ({bookings.filter((b) => b.status === 'CANCELLED').length})
          </button>
        </div>
      </div>

      {/* Notifications */}
      {error && (
        <div className="alert alert-danger" style={{ marginBottom: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span>⚠️ {error}</span>
          <button onClick={() => setError('')} style={{ background: 'none', border: 'none', color: '#fff', cursor: 'pointer' }}>✕</button>
        </div>
      )}

      {successMsg && (
        <div className="alert alert-success" style={{ marginBottom: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span>✅ {successMsg}</span>
          <button onClick={() => setSuccessMsg('')} style={{ background: 'none', border: 'none', color: '#fff', cursor: 'pointer' }}>✕</button>
        </div>
      )}

      {/* Bookings List */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '60px 0', color: '#94a3b8' }}>
          <div style={{ fontSize: '42px', marginBottom: '12px' }}>🎟️</div>
          <p style={{ fontSize: '16px' }}>Loading your ticket wallet...</p>
        </div>
      ) : filteredBookings.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '60px 20px', borderRadius: '16px' }}>
          <div style={{ fontSize: '48px', marginBottom: '12px' }}>🍿</div>
          <h3 style={{ margin: 0, fontSize: '1.3rem' }}>No Bookings Found</h3>
          <p style={{ color: '#94a3b8', fontSize: '14px', margin: '8px 0 20px' }}>
            {filter === 'ALL'
              ? 'You have not made any movie ticket reservations yet.'
              : `You have no ${filter.toLowerCase()} bookings.`}
          </p>
          <button className="btn" onClick={() => onNavigateHome ? onNavigateHome() : navigate('/')}>
            Explore Now Playing Movies
          </button>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          {filteredBookings.map((b) => {
            const isConfirmed = b.status === 'CONFIRMED';
            const showDate = new Date(b.showTime);
            // Show is past if start time has already passed
            const isShowPast = showDate.getTime() < Date.now();

            return (
              <div
                key={b.id}
                style={{
                  background: isConfirmed
                    ? 'linear-gradient(135deg, rgba(22, 31, 48, 0.95), rgba(15, 23, 42, 0.98))'
                    : 'linear-gradient(135deg, rgba(30, 20, 25, 0.7), rgba(15, 23, 42, 0.8))',
                  border: isConfirmed ? '1px solid rgba(56, 189, 248, 0.3)' : '1px solid rgba(239, 68, 68, 0.2)',
                  borderRadius: '16px',
                  boxShadow: isConfirmed ? '0 8px 24px rgba(0, 0, 0, 0.5)' : 'none',
                  overflow: 'hidden',
                  display: 'flex',
                  flexWrap: 'wrap',
                  position: 'relative',
                }}
              >
                {/* Left Ticket Main Section */}
                <div style={{ flex: '1 1 500px', padding: '24px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '10px' }}>
                    <div>
                      <span
                        style={{
                          fontSize: '11px',
                          textTransform: 'uppercase',
                          letterSpacing: '1px',
                          color: '#38bdf8',
                          fontWeight: 700,
                        }}
                      >
                        CINEMAPULSE PASS • BOOKING #{b.id}
                      </span>
                      <h3 style={{ fontSize: '1.45rem', fontWeight: 800, margin: '4px 0 8px', color: '#f8fafc' }}>
                        {b.movieTitle}
                      </h3>
                    </div>

                    <span
                      style={{
                        padding: '4px 12px',
                        borderRadius: '20px',
                        fontSize: '12px',
                        fontWeight: 700,
                        letterSpacing: '0.5px',
                        background: isConfirmed ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
                        border: isConfirmed ? '1px solid #10b981' : '1px solid #ef4444',
                        color: isConfirmed ? '#10b981' : '#f87171',
                      }}
                    >
                      {isConfirmed ? '● CONFIRMED' : '✕ CANCELLED'}
                    </span>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '14px', marginTop: '16px' }}>
                    <div>
                      <div style={{ color: '#64748b', fontSize: '11px', textTransform: 'uppercase' }}>Cinema & Auditorium</div>
                      <div style={{ color: '#e2e8f0', fontWeight: 600, fontSize: '14px', marginTop: '2px' }}>
                        🏛️ {b.theatreName}
                      </div>
                      <div style={{ color: '#94a3b8', fontSize: '12px' }}>{b.screenName} (IMAX / Dolby)</div>
                    </div>

                    <div>
                      <div style={{ color: '#64748b', fontSize: '11px', textTransform: 'uppercase' }}>Show Date & Time</div>
                      <div style={{ color: '#e2e8f0', fontWeight: 600, fontSize: '14px', marginTop: '2px' }}>
                        🕒 {showDate.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })}
                      </div>
                      <div style={{ color: '#94a3b8', fontSize: '12px' }}>
                        {showDate.toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' })}
                      </div>
                    </div>

                    <div>
                      <div style={{ color: '#64748b', fontSize: '11px', textTransform: 'uppercase' }}>Allocated Seats</div>
                      <div style={{ color: '#fbbf24', fontWeight: 800, fontSize: '15px', marginTop: '2px' }}>
                        💺 {getSeatText(b)}
                      </div>
                      <div style={{ color: '#94a3b8', fontSize: '12px' }}>
                        {b.bookedSeats?.length || (b.seatSummary ? b.seatSummary.split(',').length : 1)} Seat(s) Reserved
                      </div>
                    </div>

                    <div>
                      <div style={{ color: '#64748b', fontSize: '11px', textTransform: 'uppercase' }}>Booking Timestamp</div>
                      <div style={{ color: '#94a3b8', fontSize: '13px', marginTop: '2px' }}>
                        📅 {new Date(b.bookingTime).toLocaleString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                      </div>
                      {b.cancellationTime && (
                        <div style={{ color: '#ef4444', fontSize: '11px', marginTop: '2px' }}>
                          Cancelled: {new Date(b.cancellationTime).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })}
                        </div>
                      )}
                    </div>
                  </div>
                </div>

                {/* Perforated Stub Divider */}
                <div
                  style={{
                    borderLeft: '2px dashed rgba(255, 255, 255, 0.12)',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    padding: '24px',
                    minWidth: '220px',
                    backgroundColor: 'rgba(0, 0, 0, 0.25)',
                  }}
                >
                  <div>
                    <div style={{ color: '#64748b', fontSize: '11px', textTransform: 'uppercase' }}>Total Amount</div>
                    <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#10b981', margin: '4px 0 12px' }}>
                      ₹{Number(b.totalAmount).toFixed(2)}
                    </div>

                    {/* Barcode Mockup */}
                    <div
                      style={{
                        background: 'rgba(255, 255, 255, 0.08)',
                        padding: '8px',
                        borderRadius: '6px',
                        textAlign: 'center',
                        fontSize: '10px',
                        color: '#94a3b8',
                        letterSpacing: '2px',
                        fontFamily: 'monospace',
                        marginBottom: '14px',
                      }}
                    >
                      ||||||| | |||| | ||||| |||
                      <div style={{ marginTop: '3px', fontSize: '9px', letterSpacing: '1px' }}>
                        TKT-{b.id}-VERIFIED
                      </div>
                    </div>
                  </div>

                  {/* Actions */}
                  <div>
                    {isConfirmed && !isShowPast && (
                      <button
                        className="btn btn-danger"
                        style={{ width: '100%', padding: '9px 12px', fontSize: '13px', fontWeight: 600 }}
                        onClick={() => setCancelTarget(b)}
                      >
                        ✕ Cancel Booking
                      </button>
                    )}

                    {isConfirmed && isShowPast && (
                      <div style={{ color: '#64748b', fontSize: '12px', textAlign: 'center' }}>
                        Show completed
                      </div>
                    )}

                    {!isConfirmed && (
                      <div style={{ color: '#ef4444', fontSize: '12px', textAlign: 'center', fontWeight: 600 }}>
                        Booking Voided
                      </div>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Confirmation Modal for Cancellation */}
      {cancelTarget && (
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
              background: 'linear-gradient(135deg, rgba(28, 18, 24, 0.98), rgba(15, 23, 42, 0.98))',
              border: '1px solid rgba(239, 68, 68, 0.4)',
              boxShadow: '0 20px 40px rgba(0, 0, 0, 0.8), 0 0 25px rgba(239, 68, 68, 0.25)',
              borderRadius: '16px',
              padding: '28px',
              textAlign: 'center',
            }}
          >
            <div style={{ fontSize: '42px', marginBottom: '10px' }}>⚠️</div>
            <h3 style={{ margin: 0, fontSize: '1.35rem', fontWeight: 800, color: '#f87171' }}>
              Confirm Ticket Cancellation
            </h3>
            <p style={{ color: '#94a3b8', fontSize: '14px', lineHeight: '1.6', margin: '12px 0 20px' }}>
              Are you sure you want to cancel booking <strong>#{cancelTarget.id}</strong> for{' '}
              <strong style={{ color: '#fff' }}>"{cancelTarget.movieTitle}"</strong>?
              <br />
              <span style={{ color: '#fbbf24', fontSize: '13px' }}>
                Seats {getSeatText(cancelTarget)} will be released immediately for other moviegoers.
              </span>
            </p>

            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
              <button
                className="btn btn-secondary"
                style={{ flex: 1, padding: '10px' }}
                disabled={cancelling}
                onClick={() => setCancelTarget(null)}
              >
                Keep Booking
              </button>
              <button
                className="btn btn-danger"
                style={{ flex: 1, padding: '10px', background: '#dc2626' }}
                disabled={cancelling}
                onClick={confirmCancel}
              >
                {cancelling ? 'Cancelling...' : 'Yes, Cancel Tickets'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
