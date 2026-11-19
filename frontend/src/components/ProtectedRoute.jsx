import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Route protection wrapper.
 * Redirects unauthenticated visitors to /login.
 * Enforces role-based access control (ROLE: STUDENT).
 */
export default function ProtectedRoute({ children, allowedRole = 'STUDENT' }) {
  const { user, loading } = useAuth();

  // Show accessible loading indicator while validating session
  if (loading) {
    return (
      <div className="auth-loading-screen">
        <div className="spinner"></div>
        <p>Checking authentication status...</p>
      </div>
    );
  }

  // Not authenticated -> redirect to login
  if (!user) {
    return <Navigate to="/login" replace />;
  }

  // Role validation
  if (allowedRole && user.role !== allowedRole) {
    return (
      <div className="container error-page">
        <div className="card">
          <h2>Access Denied</h2>
          <p>Your account ({user.role}) does not have permission to access this student resource.</p>
        </div>
      </div>
    );
  }

  return children;
}
