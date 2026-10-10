import { useCallback, useEffect, useMemo, useState } from 'react';
import { AuthContext } from '../context/auth.context';
import api from '@lib/api';

export const AuthProvider = ({ children }:{ children: React.ReactNode }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    const checkSession = async () => {
      try {
        const response = await api.get('/auth/me');

        if (isMounted) {
          setUser(response.data.user ?? response.data);
        }
      } catch {
        if (isMounted) {
          setUser(null);
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    checkSession();

    return () => {
      isMounted = false;
    };
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const response = await api.post('/auth/login', { email, password });

    setUser(response.data.user ?? response.data);

    return response.data;
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout');
    } finally {
      setUser(null);
    }
  }, []);

  const value = useMemo(() => {
    return {
      user,
      loading,
      isAuthenticated: Boolean(user),
      login,
      logout,
    };
  }, [user, loading, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}