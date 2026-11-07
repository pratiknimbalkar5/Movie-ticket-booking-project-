import React, { useState, useEffect } from 'react';
import { useParams, useLocation, useNavigate, Link } from 'react-router-dom';
import api from '../services/api';

export default function BookingConfirmationPage() {
  const { bookingId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();

  const [booking, setBooking] = useState(location.state?.confirmedBooking || null);
  const [loading, setLoading] = useState(!booking);
  const [error, setError] = useState('');
  const [cancelLoading, setCancelLoading] = useState(false);
  const [cancelStatus, setCancelStatus] = useState(null);

  useEffect(() => {
    if (!booking && bookingId) {
      setLoading(true);
      setError('');
      api.get(`/bookings/${bookingId}`)
        .then((res) => {
          if (res.data?.data) {
            setBooking(res.data.data);
          }
        })
        .catch((err) => {
          setError(err.response?.data?.message || 'Failed to retrieve booking details.');
        })
        .finally(() => {
          setLoading(false);
        });
    }
  }, [bookingId, booking]);

  const handleCancelBooking = async () => {
    if (!booking) return;
    if (
      !window.confirm(
        `Are you sure you want to cancel Booking #${booking.id} for "${booking.movieTitle}"? Your seats will be released immediately.`
      )
    ) {
      return;
    }
    try {
      setCancelLoading(true);
      const res = await api.post(`/bookings/${booking.id}/cancel`);
      setCancelStatus({
        success: true,
        message: res.data?.message || 'Booking cancelled successfully. Seats have been released.',
      });
      setBooking((prev) => ({
        ...prev,
        status: 'CANCELLED',
      }));
    } catch (err) {
      setCancelStatus({
        success: false,
        message: err.response?.data?.message || 'Failed to cancel booking.',
      });
    } finally {
      setCancelLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="container" style={{ maxWidth: 560, marginTop: 60, textAlign: 'center', padding: '60px 20px' }}>
        <div style={{ fontSize: '38px', marginBottom: '16px', animation: 'spin 2s linear infinite' }}>🍿</div>
        <p style={{ color: '#94a3b8' }}>Loading booking details...</p>
      </div>
    );
  }

  if (error || !booking) {
    return (
      <div className="container" style={{ maxWidth: 560, marginTop: 60, textAlign: 'center', padding: '40px 20px' }}>
        <div className="card" style={{ padding: '36px 24px', borderRadius: 16 }}>
          <div style={{ fontSize: '48px', marginBottom: '12px' }}>⚠️</div>
          <h2 style={{ color: '#f87171', marginBottom: '8px' }}>Booking Not Found</h2>
          <p style={{ color: '#94a3b8', fontSize: '14px', marginBottom: '24px' }}>
            {error || 'We could not find the details for this booking.'}
          </p>
          <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
            <Link to="/bookings" className="btn" style={{ textDecoration: 'none' }}>
              🎟️ View My Bookings
            </Link>
            <Link to="/" className="btn btn-secondary" style={{ textDecoration: 'none' }}>
              🏠 Return to Homepage
            </Link>
          </div>
        </div>
      </div>
    );
  }

  const isCancelled = booking.status === 'CANCELLED';

  return (
    <div className="container" style={{ maxWidth: 560, marginTop: 40, paddingBottom: 60 }}>
      <div className="card" style={{ textAlign: 'center', padding: '36px 24px', borderRadius: 16 }}>
        <div style={{ fontSize: '48px', marginBottom: '12px' }}>
          {isCancelled ? '❌' : '🎉'}
        </div>
        <h2 style={{ color: isCancelled ? '#f87171' : '#10b981', marginBottom: '8px' }}>
          {isCancelled ? 'Booking Cancelled' : 'Booking Confirmed!'}
        </h2>
        <p style={{ color: '#94a3b8', fontSize: '14px', marginBottom: '20px' }}>
          {isCancelled
            ? 'Your booking was cancelled and seats have been released back to inventory.'
            : 'Your ticket has been booked successfully and confirmed in MySQL database.'}
        </p>

        {cancelStatus && (
          <div
            className={`alert ${cancelStatus.success ? 'alert-success' : 'alert-danger'}`}
            style={{ marginBottom: '20px', textAlign: 'center' }}
          >
            {cancelStatus.success ? '✅ ' : '⚠️ '} {cancelStatus.message}
          </div>
        )}

        <div
          style={{
            background: isCancelled ? 'rgba(239, 68, 68, 0.08)' : 'rgba(255, 255, 255, 0.04)',
            border: isCancelled ? '1px solid rgba(239, 68, 68, 0.3)' : '1px solid rgba(255, 255, 255, 0.08)',
            borderRadius: '10px',
            padding: '20px',
            textAlign: 'left',
            marginBottom: '24px',
            fontSize: '14px',
            lineHeight: '1.9',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
            <span>
              <strong>Booking Ref:</strong> <span style={{ color: '#38bdf8' }}>#{booking.id}</span>
            </span>
            <span
              style={{
                padding: '2px 10px',
                borderRadius: '12px',
                fontSize: '12px',
                fontWeight: 700,
                background: isCancelled ? 'rgba(239, 68, 68, 0.2)' : 'rgba(16, 185, 129, 0.2)',
                color: isCancelled ? '#f87171' : '#34d399',
              }}
            >
              {booking.status}
            </span>
          </div>
          <p>
            <strong>Movie:</strong> {booking.movieTitle}
          </p>
          <p>
            <strong>Theatre:</strong> {booking.theatreName} ({booking.screenName})
          </p>
          <p>
            <strong>Showtime:</strong> {new Date(booking.showTime).toLocaleString()}
          </p>
          <p>
            <strong>Seats:</strong>{' '}
            <span style={{ color: '#fbbf24', fontWeight: 'bold' }}>
              {booking.bookedSeats?.map((s) => s.seatNumber).join(', ') || booking.seatSummary || 'N/A'}
            </span>
          </p>
          <div
            style={{
              borderTop: '1px solid rgba(255, 255, 255, 0.1)',
              marginTop: '12px',
              paddingTop: '12px',
              display: 'flex',
              justifyContent: 'space-between',
              fontSize: '16px',
              fontWeight: 'bold',
            }}
          >
            <span>Total Amount:</span>
            <span style={{ color: isCancelled ? '#94a3b8' : '#10b981' }}>
              ₹{booking.totalAmount}
            </span>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', flexWrap: 'wrap' }}>
          {!isCancelled && (
            <button
              className="btn btn-danger"
              onClick={handleCancelBooking}
              disabled={cancelLoading}
              style={{ padding: '10px 20px', fontWeight: 700 }}
            >
              {cancelLoading ? 'Cancelling...' : '✕ Cancel This Booking Now'}
            </button>
          )}
          <Link to="/bookings" className="btn" style={{ textDecoration: 'none' }}>
            🎟️ View My Bookings Wallet
          </Link>
          <Link to="/" className="btn btn-secondary" style={{ textDecoration: 'none' }}>
            Book Another Movie
          </Link>
        </div>
      </div>
    </div>
  );
}

