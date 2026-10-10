import { useCallback, useEffect, useMemo, useState } from 'react';
import { AuthContext } from '../context/auth.context';
import api, { refreshAccessToken, setAccessToken } from '@lib/api';
import type { User } from '@lib/types';

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // После перезагрузки страницы access token в памяти пуст — восстанавливаем сессию по refresh cookie
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

  // Refresh не удался посреди работы — сессия закончилась, ProtectedRoute отправит на /login
  useEffect(() => {
    const handleLogout = () => {
      setAccessToken(null);
      setUser(null);
    };

    window.addEventListener('auth:logout', handleLogout);
    return () => window.removeEventListener('auth:logout', handleLogout);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const response = await api.post('/auth/login', { email, password });
    const { accessToken, user } = response.data;

    setAccessToken(accessToken);
    setUser(user);

    return response.data;
  }, []);

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
    logout,
  }), [user, loading, login, logout]);

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}
