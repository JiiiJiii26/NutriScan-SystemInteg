import React, { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { apiFetch } from '../api';

/**
 * Reset Password Page (Increment 2)
 * Reads token from URL and allows setting a new password.
 */
export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');

  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!token) {
      setError('Invalid reset link. Missing token.');
      return;
    }
    if (!newPassword) {
      setError('New password is required.');
      return;
    }
    if (newPassword.length < 8) {
      setError('New password must be at least 8 characters long.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setIsSubmitting(true);
    try {
      await apiFetch('/api/auth/reset-password', {
        method: 'POST',
        body: JSON.stringify({
          token,
          newPassword,
          confirmPassword,
        }),
      });
      setSuccess(true);
    } catch (err) {
      setError(err.message || 'Failed to reset password. The link may have expired or already been used.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // If token is absent
  if (!token) {
    return (
      <div className="auth-container">
        <div className="auth-card">
          <div className="auth-header">
            <h2>Invalid Reset Link</h2>
            <p className="auth-subtitle">
              No reset token was found in the URL.
            </p>
          </div>
          <div className="alert alert-danger" role="alert">
            The link you clicked appears to be incomplete or broken. Please request a new password reset link.
          </div>
          <div className="auth-footer" style={{ marginTop: '1.5rem' }}>
            <Link to="/forgot-password" className="btn-submit" style={{ display: 'block', textAlign: 'center', textDecoration: 'none' }}>
              Request New Reset Link
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <h2>Create New Password</h2>
          <p className="auth-subtitle">
            Enter your new credentials to regain access to your account.
          </p>
        </div>

        {success ? (
          <div className="auth-success-box">
            <div className="alert alert-success" role="alert">
              Password reset successful! You can now log in with your new password.
            </div>
            <div className="auth-footer" style={{ marginTop: '1.5rem' }}>
              <Link to="/login" className="btn-submit" style={{ display: 'block', textAlign: 'center', textDecoration: 'none' }}>
                Proceed to Log In
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
                <label htmlFor="reset-newPassword">New Password (min. 8 characters)</label>
                <input
                  id="reset-newPassword"
                  type="password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Enter your new password"
                  required
                  disabled={isSubmitting}
                  autoComplete="new-password"
                />
              </div>

              <div className="form-group">
                <label htmlFor="reset-confirmPassword">Confirm New Password</label>
                <input
                  id="reset-confirmPassword"
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Re-type your new password"
                  required
                  disabled={isSubmitting}
                  autoComplete="new-password"
                />
              </div>

              <button
                type="submit"
                className="btn-submit"
                disabled={isSubmitting}
              >
                {isSubmitting ? 'Resetting Password...' : 'Save New Password'}
              </button>
            </form>

            <div className="auth-footer">
              <p>
                Cancel and return to{' '}
                <Link to="/login" className="auth-link">Sign In</Link>
              </p>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
