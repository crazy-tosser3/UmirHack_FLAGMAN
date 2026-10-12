export type UserRole = 'admin' | 'manager' | 'employee'

export interface User {
  email: string;
  id: string;
  name: string;
  role: UserRole;
}

export type Unit = "g" | "pcs" | "ml"
