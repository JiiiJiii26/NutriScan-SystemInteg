import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Main application navigation bar.
 * Responsive, clean, and shows role and authentication states.
 */
export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <header className="navbar-container">
      <div className="navbar-inner">
        <Link to="/" className="navbar-brand">
          <span className="brand-logo" aria-hidden="true">🥗</span>
          <div className="brand-text">
            <span className="brand-title">NutriScan</span>
            <span className="brand-subtitle">Student Food Diary</span>
          </div>
        </Link>

        <nav className="navbar-links" aria-label="Main Navigation">
          {user ? (
            <>
              <Link
                to="/dashboard"
                className={`nav-link ${location.pathname === '/dashboard' ? 'active' : ''}`}
              >
                Dashboard
              </Link>

              <Link
                to="/profile"
                className={`nav-link ${location.pathname === '/profile' ? 'active' : ''}`}
              >
                Profile
              </Link>
              
              {/* Disabled/greyed-out future navigation items */}
              <span className="nav-link disabled" title="Available in next increment">
                Diary <span className="badge-coming-soon">Soon</span>
              </span>
              <span className="nav-link disabled" title="Available in next increment">
                Barcode Scanner <span className="badge-coming-soon">Soon</span>
              </span>

              <div className="user-profile-badge">
                <span className="user-name">{user.fullName}</span>
                <span className="user-role-tag">{user.role}</span>
              </div>

              <button onClick={handleLogout} className="btn-logout" title="Sign out of your account">
                Logout
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="nav-link">Log In</Link>
              <Link to="/register" className="btn-register">Register</Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
