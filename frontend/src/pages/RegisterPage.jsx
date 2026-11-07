import React, { useState, useContext } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { AuthContext } from '../context/AuthContext';

export default function RegisterPage({ onSuccess, onSwitchToLogin }) {
  const { register } = useContext(AuthContext);
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
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
      await register(username, email, password);
      handleSuccessfulAuth();
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container" style={{ maxWidth: 420, marginTop: 40 }}>
      <div className="card">
        <h2 style={{ textAlign: 'center', marginBottom: 20 }}>Create Account</h2>
        {error && <div className="alert alert-danger">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Username</label>
            <input
              type="text"
              className="form-control"
              required
              minLength={3}
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label>Email Address</label>
            <input
              type="email"
              className="form-control"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div className="form-group">
            <label>Password (min 6 characters)</label>
            <input
              type="password"
              className="form-control"
              required
              minLength={6}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          <button type="submit" className="btn" style={{ width: '100%', marginTop: 10 }} disabled={loading}>
            {loading ? 'Creating Account...' : 'Register'}
          </button>
        </form>
        <p style={{ textAlign: 'center', marginTop: 15, fontSize: 13 }}>
          Already have an account?{' '}
          {onSwitchToLogin ? (
            <a style={{ color: '#0284c7', cursor: 'pointer' }} onClick={onSwitchToLogin}>
              Login here
            </a>
          ) : (
            <Link to="/login" style={{ color: '#0284c7' }}>
              Login here
            </Link>
          )}
        </p>
      </div>
    </div>
  );
}
