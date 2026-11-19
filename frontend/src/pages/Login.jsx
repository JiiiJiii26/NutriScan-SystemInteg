import React, { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Login Page
 * 
 * Primary: Google OAuth 2.0 One-Click Login
 * Fallback: Local Email and Password Authentication
 */
export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // If already authenticated, redirect straight to dashboard
  useEffect(() => {
    if (user) {
      navigate('/dashboard', { replace: true });
    }
  }, [user, navigate]);

  // Check for registration success query parameter
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    if (params.get('registered') === 'true') {
      setSuccessMessage('Registration successful! Please log in with your credentials.');
    }
    if (params.get('error') === 'oauth_failed') {
      setError('Google Sign-In failed or was cancelled. Please try again or use email login.');
    }
  }, [location]);

  // Trigger Google OAuth 2.0 login
  const handleGoogleLogin = () => {
    // Redirects browser to Spring Security OAuth2 authorization endpoint
    window.location.href = 'http://localhost:8080/oauth2/authorization/google';
  };

  // Handle local credentials login
  const handleLocalLogin = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMessage('');

    if (!email.trim() || !password) {
      setError('Please provide both email and password.');
      return;
    }

    setIsSubmitting(true);
    try {
      await login(email.trim(), password);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Invalid email or password. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <h2>Welcome to NutriScan</h2>
          <p className="auth-subtitle">Track your packaged food & calorie intake with ease</p>
        </div>

        {/* Success message banner */}
        {successMessage && (
          <div className="alert alert-success" role="alert">
            {successMessage}
          </div>
        )}

        {/* Error message banner */}
        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}

        {/* PRIMARY: Google OAuth 2.0 Button */}
        <button
          type="button"
          onClick={handleGoogleLogin}
          className="btn-google-login"
        >
          <svg className="google-icon" viewBox="0 0 24 24" width="20" height="20">
            <path
              fill="#4285F4"
              d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
            />
            <path
              fill="#34A853"
              d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
            />
            <path
              fill="#FBBC05"
              d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z"
            />
            <path
              fill="#EA4335"
              d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z"
            />
          </svg>
          <span>Continue with Google</span>
        </button>

        {/* Divider */}
        <div className="auth-divider">
          <span>or log in with email</span>
        </div>

        {/* FALLBACK: Local Credentials Form */}
        <form onSubmit={handleLocalLogin} className="auth-form" noValidate>
          <div className="form-group">
            <label htmlFor="login-email">Email Address</label>
            <input
              id="login-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="e.g. maria@university.edu.ph"
              required
              disabled={isSubmitting}
              autoComplete="email"
            />
          </div>

          <div className="form-group">
            <label htmlFor="login-password">Password</label>
            <input
              id="login-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              required
              disabled={isSubmitting}
              autoComplete="current-password"
            />
          </div>

          <button
            type="submit"
            className="btn-submit"
            disabled={isSubmitting}
          >
            {isSubmitting ? 'Logging in...' : 'Sign In'}
          </button>
        </form>

        <div className="auth-footer">
          <p>
            Don't have an account yet?{' '}
            <Link to="/register" className="auth-link">Create a student account</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
