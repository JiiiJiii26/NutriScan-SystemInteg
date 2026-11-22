import React, { createContext, useContext, useState, useEffect } from 'react';
import { apiFetch } from '../api';

const AuthContext = createContext(null);

/**
 * Authentication Provider component.
 * Manages user state based on Spring Security HTTP sessions.
 * Never stores passwords or JWTs in localStorage.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Check if there is an active session on backend by calling /api/me
  const checkAuth = async () => {
    try {
      setLoading(true);
      const data = await apiFetch('/api/me');
      setUser(data);
      return data;
    } catch (err) {
      // 401 or network error indicates unauthenticated state
      setUser(null);
      return null;
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    checkAuth();
  }, []);

  // Local login via POST /api/auth/login
  const login = async (email, password) => {
    const userData = await apiFetch('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    setUser(userData);
    return userData;
  };

  // Local student registration via POST /api/auth/register
  const register = async (fullName, email, password, confirmPassword) => {
    return await apiFetch('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ fullName, email, password, confirmPassword }),
    });
  };

  // Immediate state update when user edits profile
  const updateUser = (updatedUserData) => {
    setUser((prev) => ({ ...prev, ...updatedUserData }));
  };

  // Logout via POST /api/auth/logout
  const logout = async () => {
    try {
      await apiFetch('/api/auth/logout', { method: 'POST' });
    } catch (err) {
      console.warn('Logout notification error:', err);
    } finally {
      // Clear state regardless of backend response
      setUser(null);
    }
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, checkAuth, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * Hook for consuming authentication context across components.
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
