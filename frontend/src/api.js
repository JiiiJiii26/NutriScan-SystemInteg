/**
 * NutriScan API Client
 * 
 * Centralized fetch wrapper that automatically includes HTTP session cookies
 * ('credentials: include') to support Spring Security session-based auth.
 */

const API_BASE_URL = 'http://localhost:8080';

export async function apiFetch(endpoint, options = {}) {
  // Use relative path if proxied, or full URL to localhost:8080
  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;

  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    ...(options.headers || {}),
  };

  const config = {
    ...options,
    headers,
    // CRITICAL: Must be 'include' so that JSESSIONID cookie is sent with every request
    credentials: 'include',
  };

  const response = await fetch(url, config);

  // Try to parse JSON response body
  let data = null;
  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    try {
      data = await response.json();
    } catch (e) {
      data = null;
    }
  }

  // Handle non-2xx HTTP responses
  if (!response.ok) {
    const errorMessage = data?.error || data?.message || `Request failed with status ${response.status}`;
    const error = new Error(errorMessage);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}
