import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { apiFetch } from '../api';

/**
 * Forgot Password Page (Increment 2)
 * Allows students to request a password reset link by entering their email address.
 */
export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!email.trim()) {
      setError('Please provide your student email address.');
      return;
    }

    setIsSubmitting(true);
    try {
      await apiFetch('/api/auth/forgot-password', {
        method: 'POST',
        body: JSON.stringify({ email: email.trim() }),
      });
      setSubmitted(true);
    } catch (err) {
      setError(err.message || 'Something went wrong. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <h2>Reset Your Password</h2>
          <p className="auth-subtitle">
            Enter your registered student email to receive a password reset link.
          </p>
        </div>

        {submitted ? (
          <div className="auth-success-box">
            <div className="alert alert-success" role="alert">
              If an account with that email exists, a reset link has been sent. Check your email (or the backend console for this demo).
            </div>

            <div className="demo-callout-box">
              <span className="demo-tag">Instructor / Demo Notice</span>
              <p>
                In this Increment, email delivery is simulated. Please inspect the <strong>Spring Boot backend terminal console</strong> to find the generated reset link.
              </p>
            </div>

            <div className="auth-footer" style={{ marginTop: '1.5rem' }}>
              <Link to="/login" className="btn-secondary" style={{ display: 'inline-block', textAlign: 'center', width: '100%' }}>
                Return to Login
              </Link>
            </div>
          </div>
        ) : (
          <>
            {error && (
              <div className="alert alert-danger" role="alert">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="auth-form" noValidate>
              <div className="form-group">
                <label htmlFor="forgot-email">Student Email Address</label>
                <input
                  id="forgot-email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="e.g. maria@university.edu.ph"
                  required
                  disabled={isSubmitting}
                  autoComplete="email"
                />
              </div>

              <button
                type="submit"
                className="btn-submit"
                disabled={isSubmitting}
              >
                {isSubmitting ? 'Sending Link...' : 'Send Reset Link'}
              </button>
            </form>

            <div className="auth-footer">
              <p>
                Remembered your password?{' '}
                <Link to="/login" className="auth-link">Back to Sign In</Link>
              </p>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
