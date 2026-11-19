import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Student Registration Page
 * 
 * Creates a new student user in the PostgreSQL database with BCrypt-hashed password.
 * Default assigned role: STUDENT.
 */
export default function Register() {
  const { user, register } = useAuth();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    confirmPassword: '',
  });

  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // If already logged in, redirect to dashboard
  useEffect(() => {
    if (user) {
      navigate('/dashboard', { replace: true });
    }
  }, [user, navigate]);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const validateForm = () => {
    const { fullName, email, password, confirmPassword } = formData;

    if (!fullName.trim()) {
      return 'Please enter your full name.';
    }

    if (!email.trim()) {
      return 'Please enter your email address.';
    }

    // Basic email format check
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email.trim())) {
      return 'Please provide a valid email address.';
    }

    if (!password) {
      return 'Please enter a password.';
    }

    if (password.length < 8) {
      return 'Password must be at least 8 characters long.';
    }

    if (password !== confirmPassword) {
      return 'Passwords do not match.';
    }

    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const validationError = validateForm();
    if (validationError) {
      setError(validationError);
      return;
    }

    setIsSubmitting(true);
    try {
      await register(
        formData.fullName.trim(),
        formData.email.trim(),
        formData.password,
        formData.confirmPassword
      );

      // Redirect to login with success indicator
      navigate('/login?registered=true');
    } catch (err) {
      setError(err.message || 'Registration failed. Please verify your details.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <h2>Create Student Account</h2>
          <p className="auth-subtitle">Join NutriScan to track calories on your student budget</p>
        </div>

        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="auth-form" noValidate>
          <div className="form-group">
            <label htmlFor="reg-fullname">Full Name</label>
            <input
              id="reg-fullname"
              name="fullName"
              type="text"
              value={formData.fullName}
              onChange={handleChange}
              placeholder="e.g. Maria Santos"
              required
              disabled={isSubmitting}
              autoComplete="name"
            />
          </div>

          <div className="form-group">
            <label htmlFor="reg-email">Email Address</label>
            <input
              id="reg-email"
              name="email"
              type="email"
              value={formData.email}
              onChange={handleChange}
              placeholder="e.g. maria@university.edu.ph"
              required
              disabled={isSubmitting}
              autoComplete="email"
            />
          </div>

          <div className="form-group">
            <label htmlFor="reg-password">Password (min 8 characters)</label>
            <input
              id="reg-password"
              name="password"
              type="password"
              value={formData.password}
              onChange={handleChange}
              placeholder="At least 8 characters"
              required
              disabled={isSubmitting}
              autoComplete="new-password"
            />
          </div>

          <div className="form-group">
            <label htmlFor="reg-confirm-password">Confirm Password</label>
            <input
              id="reg-confirm-password"
              name="confirmPassword"
              type="password"
              value={formData.confirmPassword}
              onChange={handleChange}
              placeholder="Re-enter your password"
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
            {isSubmitting ? 'Creating Account...' : 'Register'}
          </button>
        </form>

        <div className="auth-footer">
          <p>
            Already have an account?{' '}
            <Link to="/login" className="auth-link">Log in here</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
