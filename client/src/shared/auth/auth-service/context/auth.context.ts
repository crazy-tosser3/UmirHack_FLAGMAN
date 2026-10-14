import type { TelegramAuthData, User } from '@lib/types';
import { createContext } from 'react';

interface AuthContext{
  user: User | null; 
  loading: boolean; 
  isAuthenticated: boolean;
  login: (email: string, password: string) => void; 
  loginTelegram: (data: TelegramAuthData) => Promise<void>;
  updateUser: (user: User) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthContext | null>(null);

AuthContext.displayName = 'AuthContext';