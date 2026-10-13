import { useCallback, useEffect, useMemo, useState } from 'react';
import { AuthContext } from '../context/auth.context';
import api, { refreshAccessToken, setAccessToken } from '@lib/api';
import type { TelegramAuthData, User } from '@lib/types';

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    const checkSession = async () => {
      try {
        const token = await refreshAccessToken();
        if (!token) {
          return;
        }
        const response = await api.get('/auth/me');
        if (isMounted) {
          setUser(response.data.user ?? response.data);
        }
      } catch {
        if (isMounted) {
          setAccessToken(null);
          setUser(null);
        }
      } finally {
        if (isMounted) setLoading(false);
      }
    };

    checkSession();
    return () => { isMounted = false; };
  }, []);

  useEffect(() => {
    const handleLogout = () => {
      setAccessToken(null);
      setUser(null);
    };

    window.addEventListener('auth:logout', handleLogout);
    return () => window.removeEventListener('auth:logout', handleLogout);
  }, []);

  const startSession = useCallback(async (accessToken: string, fallback: User) => {
    setAccessToken(accessToken);
    try {
      const me = await api.get<User>('/auth/me');
      setUser(me.data);
    } catch {
      setUser(fallback);
    }
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const response = await api.post('/auth/login', { email, password });
    const { accessToken, user } = response.data;

    await startSession(accessToken, user);

    return response.data;
  }, [startSession]);

  const loginTelegram = useCallback(async (data: TelegramAuthData) => {
    const response = await api.post('/auth/telegram', data);
    const { accessToken, user } = response.data;

    await startSession(accessToken, user);
  }, [startSession]);

  const updateUser = useCallback((next: User) => setUser(next), []);

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout');
    } finally {
      setAccessToken(null);
      setUser(null);
    }
  }, []);

  const value = useMemo(() => ({
    user,
    loading,
    isAuthenticated: Boolean(user),
    login,
    loginTelegram,
    updateUser,
    logout,
  }), [user, loading, login, loginTelegram, updateUser, logout]);

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}
