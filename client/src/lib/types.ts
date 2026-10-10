type UserRole = 'admin' | 'manager' | 'employee'

export interface User {
  email: string;
  name: string;
  role: UserRole;
  createdAt: string;
}