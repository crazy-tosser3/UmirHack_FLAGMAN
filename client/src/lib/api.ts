import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';

// По умолчанию запросы идут через прокси Vite (см. vite.config.ts) — тот же origin, cookie работают без CORS
const BASE_URL = import.meta.env.VITE_API_URL ?? '/api/v1';

const api = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Access token живет только в памяти; refresh token — в HttpOnly cookie, JS его не видит
let accessToken: string | null = null;

export const setAccessToken = (token: string | null) => {
  accessToken = token;
};

// На эти запросы 401 означает неверные данные, а не истекший токен
const NO_REFRESH_ENDPOINTS = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout', '/auth/invite'];

let refreshing: Promise<string | null> | null = null;

// Один refresh на всех: бэкенд ротирует refresh token, и повторная отправка старого
// (например, из двух параллельных запросов) считается кражей — вся сессия отзывается
export function refreshAccessToken(): Promise<string | null> {
  refreshing ??= axios
    .post(`${BASE_URL}/auth/refresh`, null, { withCredentials: true })
    .then((response) => {
      accessToken = response.data.accessToken;
      return accessToken;
    })
    .catch(() => {
      accessToken = null;
      return null;
    })
    .finally(() => {
      refreshing = null;
    });
  return refreshing;
}

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean };

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetriableConfig | undefined;
    const url = config?.url ?? '';
    const canRefresh = error.response?.status === 401
      && config
      && !config._retried
      && !NO_REFRESH_ENDPOINTS.some((ep) => url.includes(ep));

    if (!canRefresh) {
      return Promise.reject(error);
    }

    // Access token истек — обновляем и повторяем исходный запрос один раз
    config._retried = true;
    const token = await refreshAccessToken();
    if (!token) {
      window.dispatchEvent(new CustomEvent('auth:logout'));
      return Promise.reject(error);
    }
    config.headers.Authorization = `Bearer ${token}`;
    return api(config);
  }
);

export default api;
