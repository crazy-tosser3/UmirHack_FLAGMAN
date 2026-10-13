import { useToastStore } from "@shared/toasts";
import type { ApiErrorBody, UserRole, UserStatus } from "./types";
import { isAxiosError } from "axios";
import {clsx, type ClassValue} from "clsx";
import { twMerge } from "tailwind-merge";

export const cn = (...classes:ClassValue[]) => twMerge(clsx(classes));

export const typedEntries = <K extends PropertyKey, V>(obj: Record<K, V>): [K, V][] =>
  Object.entries(obj) as [K,V][];

export async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text);
    useToastStore.getState().newToast("Скопировано!", "success");
    return true;
  } catch {
    useToastStore.getState().newToast("Не удалось скопировать", "error")
    return false;
  }
}

export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString('ru-RU', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export const units = {
  "g": "г",
  "ml": "мл",
  "pcs": "шт"
}

export const roleLabels: Record<UserRole, string> = {
  admin: "Администратор",
  manager: "Менеджер",
  employee: "Сотрудник",
}

export const statusLabels: Record<UserStatus, string> = {
  active: "Активен",
  pending: "Ожидает",
  blocked: "Заблокирован",
}

export const isStaff = (role: UserRole) => role === "manager" || role === "admin";

export const canUseTelegram = isStaff;

export function apiErrorCode(err: unknown): string | undefined {
  return isAxiosError<ApiErrorBody>(err) ? err.response?.data?.error?.code : undefined;
}

export function apiErrorMessage(err: unknown, fallback: string): string {
  return (isAxiosError<ApiErrorBody>(err) && err.response?.data?.error?.message) || fallback;
}
