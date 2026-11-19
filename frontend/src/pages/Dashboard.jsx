import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Role-Based User Dashboard (Role: STUDENT)
 * 
 * Accessible only to authenticated students.
 * Displays user welcome greeting, authentication metadata,
 * and placeholder cards for future increments.
 */
export default function Dashboard() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  if (!user) {
    return null;
  }

  return (
    <div className="dashboard-container">
      {/* Student Welcome Header */}
      <section className="welcome-banner">
        <div className="welcome-text">
          <div className="badge-row">
            <span className="role-pill">ROLE: {user.role}</span>
            <span className={`provider-pill provider-${user.provider?.toLowerCase()}`}>
              {user.provider === 'GOOGLE' ? 'Google Account' : 'Local Email Account'}
            </span>
          </div>
          <h1 className="welcome-title">
            Mabuhay, <span className="highlight-name">{user.fullName}</span>! 👋
          </h1>
          <p className="welcome-subtitle">
            Logged in as <strong>{user.email}</strong>. Welcome to your NutriScan student calorie portal.
          </p>
        </div>

        <div className="welcome-actions">
          <button onClick={handleLogout} className="btn-secondary-logout">
            Sign Out
          </button>
        </div>
      </section>

      {/* Navigation Shortcuts for Increment 1 */}
      <section className="dashboard-nav-card">
        <h3>Portal Navigation</h3>
        <div className="nav-card-items">
          <button className="nav-shortcut active">
            <span className="icon">📊</span>
            <span>Dashboard</span>
            <span className="tag-current">Active</span>
          </button>

          <button className="nav-shortcut disabled" disabled title="Coming in next increment">
            <span className="icon">📷</span>
            <span>Scan Packaged Food</span>
            <span className="tag-soon">Next Increment</span>
          </button>

          <button className="nav-shortcut disabled" disabled title="Coming in next increment">
            <span className="icon">📖</span>
            <span>Food Diary</span>
            <span className="tag-soon">Next Increment</span>
          </button>

          <button className="nav-shortcut disabled" disabled title="Coming in next increment">
            <span className="icon">⚙️</span>
            <span>Student Profile</span>
            <span className="tag-soon">Next Increment</span>
          </button>
        </div>
      </section>

      {/* Placeholder Cards as specified in SRS Increment 1 */}
      <section className="dashboard-grid">
        {/* Card 1: Calorie Target */}
        <div className="dashboard-card placeholder-card">
          <div className="card-top">
            <span className="card-icon" aria-hidden="true">🎯</span>
            <span className="coming-soon-badge">Coming in next increment</span>
          </div>
          <h3>Daily Calorie Target</h3>
          <p className="card-description">
            Your customized daily calorie budget based on your height, weight, and college activity level.
          </p>
          <div className="empty-state-visual">
            <div className="placeholder-bar-track">
              <div className="placeholder-bar-fill"></div>
            </div>
            <span className="empty-state-text">0 / 2,000 kcal (Placeholder)</span>
          </div>
          <div className="card-footer-note">
            Barcode scanning and nutritional calculation will unlock here in Increment 2.
          </div>
        </div>

        {/* Card 2: 7-Day History */}
        <div className="dashboard-card placeholder-card">
          <div className="card-top">
            <span className="card-icon" aria-hidden="true">📅</span>
            <span className="coming-soon-badge">Coming in next increment</span>
          </div>
          <h3>7-Day History</h3>
          <p className="card-description">
            Weekly log of your scanned packaged meals (instant noodles, canned goods, snacks, biscuits).
          </p>
          <div className="empty-state-visual table-placeholder">
            <div className="placeholder-row">
              <span className="ph-line short"></span>
              <span className="ph-line mini"></span>
            </div>
            <div className="placeholder-row">
              <span className="ph-line medium"></span>
              <span className="ph-line mini"></span>
            </div>
            <div className="placeholder-row">
              <span className="ph-line long"></span>
              <span className="ph-line mini"></span>
            </div>
            <span className="empty-state-text">No diary records yet</span>
          </div>
          <div className="card-footer-note">
            Barcode database integration will automatically log items here.
          </div>
        </div>

        {/* Card 3: Student Quick Guide */}
        <div className="dashboard-card info-card">
          <div className="card-top">
            <span className="card-icon" aria-hidden="true">💡</span>
            <span className="status-badge-active">Increment 1 Ready</span>
          </div>
          <h3>Student Quick Guide</h3>
          <p className="card-description">
            Authentication and role-based session management are successfully active!
          </p>
          <ul className="guide-checklist">
            <li>✅ PostgreSQL database stores your user account.</li>
            <li>✅ Passwords are securely hashed with BCrypt.</li>
            <li>✅ Spring Security maintains session cookies (JSESSIONID).</li>
            <li>✅ Protected endpoints verify your <code>STUDENT</code> role.</li>
          </ul>
        </div>
      </section>
    </div>
  );
}
