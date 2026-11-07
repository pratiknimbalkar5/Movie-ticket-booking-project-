import React from 'react';
import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="container" style={{ maxWidth: 600, marginTop: 60, textAlign: 'center', padding: '40px 20px' }}>
      <div className="card" style={{ padding: '48px 24px', borderRadius: '16px' }}>
        <div style={{ fontSize: '64px', marginBottom: '16px' }}>🎬 404</div>
        <h2 style={{ fontSize: '26px', fontWeight: 700, marginBottom: '12px', color: '#f8fafc' }}>
          Page Not Found
        </h2>
        <p style={{ color: '#94a3b8', fontSize: '15px', lineHeight: 1.6, marginBottom: '28px' }}>
          The page or screen you are looking for does not exist or may have been moved.
        </p>
        <div>
          <Link to="/" className="btn" style={{ padding: '12px 28px', fontSize: '15px', textDecoration: 'none' }}>
            🏠 Return to Homepage
          </Link>
        </div>
      </div>
    </div>
  );
}

