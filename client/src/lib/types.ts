export type UserRole = 'admin' | 'manager' | 'employee'

export type UserStatus = 'active' | 'pending' | 'blocked'

export interface User {
  email: string;
  id: string;
  name: string;
  role: UserRole;
  status?: UserStatus;
  telegramLinked?: boolean;
  telegramUsername?: string | null;
}

export interface TelegramAuthData {
  id: number;
  first_name?: string;
  last_name?: string;
  username?: string;
  photo_url?: string;
  auth_date: number;
  hash: string;
}

export interface ApiErrorBody {
  error?: {
    code?: string;
    message?: string;
    details?: unknown;
  };
}

export type Unit = "g" | "pcs" | "ml"
