// src/auth/auth.context.js
import type { User } from '@lib/types';
import { createContext } from 'react';

interface AuthContext{
  user: User | null; 
  loading: boolean; 
  isAuthenticated: boolean;
  login: (email: string, password: string) => void; 
  logout: () => void;
}

export const AuthContext = createContext<AuthContext | null>(null);

AuthContext.displayName = 'AuthContext';