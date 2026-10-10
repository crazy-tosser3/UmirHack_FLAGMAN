import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

const AUTH_ENDPOINTS = ['/auth/login', '/auth/register', '/auth/me'];

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url ?? '';
    const isAuthRequest = AUTH_ENDPOINTS.some((ep) => url.includes(ep));

    if (error.response?.status === 401 && !isAuthRequest) {
      localStorage.removeItem('token');
      window.dispatchEvent(new CustomEvent('auth:logout'));
    }

    return Promise.reject(error);
  }
);

export default api;