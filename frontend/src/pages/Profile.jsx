import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { apiFetch } from '../api';

/**
 * Profile Page (Increment 2)
 * - Profile details & edit form (PUT /api/profile)
 * - Change password section (POST /api/auth/change-password)
 */
export default function Profile() {
  const { user, updateUser } = useAuth();

  // Profile edit state
  const [fullName, setFullName] = useState(user?.fullName || '');
  const [email, setEmail] = useState(user?.email || '');
  const [profileSuccess, setProfileSuccess] = useState('');
  const [profileError, setProfileError] = useState('');
  const [isUpdatingProfile, setIsUpdatingProfile] = useState(false);

  // Change password state
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordSuccess, setPasswordSuccess] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [isChangingPassword, setIsChangingPassword] = useState(false);

  // Sync state if user changes
  useEffect(() => {
    if (user) {
      setFullName(user.fullName || '');
      setEmail(user.email || '');
    }
  }, [user]);

  // Format member since timestamp
  const formatMemberSince = (dateString) => {
    if (!dateString) return 'Recently';
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      });
    } catch {
      return dateString;
    }
  };

  // Handle Profile Update
  const handleProfileSubmit = async (e) => {
    e.preventDefault();
    setProfileSuccess('');
    setProfileError('');

    if (!fullName.trim()) {
      setProfileError('Full name cannot be blank.');
      return;
    }
    if (fullName.trim().length < 2 || fullName.trim().length > 100) {
      setProfileError('Full name must be between 2 and 100 characters.');
      return;
    }
    if (!email.trim()) {
      setProfileError('Email cannot be blank.');
      return;
    }

    setIsUpdatingProfile(true);
    try {
      const updatedUser = await apiFetch('/api/profile', {
        method: 'PUT',
        body: JSON.stringify({
          fullName: fullName.trim(),
          email: email.trim(),
        }),
      });

      // Update AuthContext so Navbar greeting updates immediately
      updateUser(updatedUser);
      setProfileSuccess('Profile updated successfully!');
    } catch (err) {
      setProfileError(err.message || 'Failed to update profile.');
    } finally {
      setIsUpdatingProfile(false);
    }
  };

  // Handle Change Password
  const handlePasswordSubmit = async (e) => {
    e.preventDefault();
    setPasswordSuccess('');
    setPasswordError('');

    if (!currentPassword) {
      setPasswordError('Current password is required.');
      return;
    }
    if (!newPassword) {
      setPasswordError('New password is required.');
      return;
    }
    if (newPassword.length < 8) {
      setPasswordError('New password must be at least 8 characters long.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordError('New passwords do not match.');
      return;
    }
    if (newPassword === currentPassword) {
      setPasswordError('New password cannot be the same as your current password.');
      return;
    }

    setIsChangingPassword(true);
    try {
      const response = await apiFetch('/api/auth/change-password', {
        method: 'POST',
        body: JSON.stringify({
          currentPassword,
          newPassword,
          confirmPassword,
        }),
      });

      setPasswordSuccess(response.message || 'Password changed successfully!');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      setPasswordError(err.message || 'Failed to change password.');
    } finally {
      setIsChangingPassword(false);
    }
  };

  const isGoogleUser = user?.provider === 'GOOGLE';

  return (
    <div className="profile-container">
      <div className="profile-card">
        {/* Header / Summary Card */}
        <div className="profile-header-section">
          <div className="profile-avatar" aria-hidden="true">
            {user?.fullName ? user.fullName.charAt(0).toUpperCase() : 'S'}
          </div>
          <div className="profile-info-summary">
            <h2>{user?.fullName}</h2>
            <p className="profile-email-badge">{user?.email}</p>
            <div className="profile-badges">
              <span className="badge badge-role">{user?.role}</span>
              <span className={`badge badge-provider badge-provider-${user?.provider?.toLowerCase()}`}>
                {user?.provider === 'GOOGLE' ? 'Google Account' : 'Local Account'}
              </span>
              <span className="badge badge-date">
                Member since {formatMemberSince(user?.createdAt)}
              </span>
            </div>
          </div>
        </div>

        <hr className="section-divider" />

        {/* Section 1: Edit Profile */}
        <div className="profile-section">
          <h3>Edit Profile Information</h3>
          <p className="section-desc">Update your personal account display details.</p>

          {profileSuccess && (
            <div className="alert alert-success" role="alert">
              {profileSuccess}
            </div>
          )}

          {profileError && (
            <div className="alert alert-danger" role="alert">
              {profileError}
            </div>
          )}

          <form onSubmit={handleProfileSubmit} className="profile-form">
            <div className="form-group">
              <label htmlFor="profile-fullName">Full Name</label>
              <input
                id="profile-fullName"
                type="text"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Your full name"
                required
                disabled={isUpdatingProfile}
              />
            </div>

            <div className="form-group">
              <label htmlFor="profile-email">
                Email Address
                {isGoogleUser && (
                  <span className="input-hint"> (Managed by Google OAuth &bull; Read-only)</span>
                )}
              </label>
              <input
                id="profile-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="student@university.edu.ph"
                required
                disabled={isGoogleUser || isUpdatingProfile}
              />
              {isGoogleUser && (
                <small className="field-note">
                  Google account emails cannot be changed directly here.
                </small>
              )}
            </div>

            <button
              type="submit"
              className="btn-primary"
              disabled={isUpdatingProfile}
            >
              {isUpdatingProfile ? 'Saving Changes...' : 'Save Profile Changes'}
            </button>
          </form>
        </div>

        <hr className="section-divider" />

        {/* Section 2: Change Password */}
        <div className="profile-section">
          <h3>Security & Password</h3>
          <p className="section-desc">Manage your account authentication credentials.</p>

          {isGoogleUser ? (
            <div className="oauth-notice-box">
              <span className="notice-icon" aria-hidden="true">ℹ️</span>
              <div>
                <strong>Google Single Sign-On Active</strong>
                <p>
                  You signed in with Google. Password changes are managed directly within your Google account.
                </p>
              </div>
            </div>
          ) : (
            <>
              {passwordSuccess && (
                <div className="alert alert-success" role="alert">
                  {passwordSuccess}
                </div>
              )}

              {passwordError && (
                <div className="alert alert-danger" role="alert">
                  {passwordError}
                </div>
              )}

              <form onSubmit={handlePasswordSubmit} className="profile-form">
                <div className="form-group">
                  <label htmlFor="change-currentPassword">Current Password</label>
                  <input
                    id="change-currentPassword"
                    type="password"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    placeholder="Enter your current password"
                    required
                    disabled={isChangingPassword}
                    autoComplete="current-password"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="change-newPassword">New Password (min. 8 characters)</label>
                  <input
                    id="change-newPassword"
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder="Enter new strong password"
                    required
                    disabled={isChangingPassword}
                    autoComplete="new-password"
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="change-confirmPassword">Confirm New Password</label>
                  <input
                    id="change-confirmPassword"
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="Re-type new password"
                    required
                    disabled={isChangingPassword}
                    autoComplete="new-password"
                  />
                </div>

                <button
                  type="submit"
                  className="btn-warning"
                  disabled={isChangingPassword}
                >
                  {isChangingPassword ? 'Updating Password...' : 'Change Password'}
                </button>
              </form>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
