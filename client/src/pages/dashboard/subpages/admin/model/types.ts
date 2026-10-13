import type { UserRole, UserStatus } from "@lib/types";

export interface AdminUser {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  status: UserStatus;
  createdAt: string;
  updatedAt: string;
  telegramLinked: boolean;
  telegramUsername: string | null;
}

export interface AdminUsersPage {
  items: AdminUser[];
  page: number;
  limit: number;
  total: number;
}

export interface CreateInvitePayload {
  role: UserRole;
  expiresAt: string;
  clicks: number;
}

export interface Invite {
  inviteUrl: string;
  expiresAt: string;
  clicks: number;
}
