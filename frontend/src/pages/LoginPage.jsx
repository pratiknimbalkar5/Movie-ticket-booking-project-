import React, { useState, useContext } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

export default function LoginPage({ onSuccess, onSwitchToRegister }) {
  const { login } = useContext(AuthContext);
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSuccessfulAuth = () => {
    if (onSuccess) onSuccess();
    else navigate('/');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      setLoading(true);
      setError('');
      await login(username, password);
      handleSuccessfulAuth();
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid username or password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container" style={{ maxWidth: 420, marginTop: 40 }}>
      <div className="card">
        <h2 style={{ textAlign: 'center', marginBottom: 20 }}>Login</h2>
        {error && <div className="alert alert-danger">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Username</label>
            <input
              type="text"
              className="form-control"
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label>Password</label>
            <input
              type="password"
              className="form-control"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <button type="submit" className="btn" style={{ width: '100%', marginTop: 10 }} disabled={loading}>
            {loading ? 'Logging in...' : 'Login'}
          </button>
        </form>

        <div style={{ marginTop: '20px', paddingTop: '16px', borderTop: '1px solid rgba(255, 255, 255, 0.08)' }}>
          <div style={{ fontSize: '12px', color: '#64748b', textAlign: 'center', marginBottom: '10px' }}>
            ⚡ QUICK DEMO CREDENTIALS
          </div>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              style={{ flex: 1, padding: '8px', fontSize: '12px' }}
              onClick={async () => {
                setUsername('john_doe');
                setPassword('User@123');
                try {
                  setLoading(true);
                  setError('');
                  await login('john_doe', 'User@123');
                  handleSuccessfulAuth();
                } catch (err) {
                  setError(err.response?.data?.message || 'Demo login failed');
                } finally {
                  setLoading(false);
                }
              }}
            >
              👤 John Doe (User)
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              style={{ flex: 1, padding: '8px', fontSize: '12px' }}
              onClick={async () => {
                setUsername('admin');
                setPassword('Admin@123');
                try {
                  setLoading(true);
                  setError('');
                  await login('admin', 'Admin@123');
                  handleSuccessfulAuth();
                } catch (err) {
                  setError(err.response?.data?.message || 'Admin login failed');
                } finally {
                  setLoading(false);
                }
              }}
            >
              🛡️ Admin
            </button>
          </div>
        </div>
        <p style={{ textAlign: 'center', marginTop: 15, fontSize: 13 }}>
          Don't have an account?{' '}
          {onSwitchToRegister ? (
            <a style={{ color: '#0284c7', cursor: 'pointer' }} onClick={onSwitchToRegister}>
              Register here
            </a>
          ) : (
            <Link to="/register" style={{ color: '#0284c7' }}>
              Register here
            </Link>
          )}
        </p>
      </div>
    </div>
  );
}
