import { useToastStore } from "@shared/toasts";
import type { ApiErrorBody, UserRole, UserStatus } from "./types";
import { isAxiosError } from "axios";
import {clsx, type ClassValue} from "clsx";
import { twMerge } from "tailwind-merge";

export const cn = (...classes:ClassValue[]) => twMerge(clsx(classes));

export const typedEntries = <K extends PropertyKey, V>(obj: Record<K, V>): [K, V][] =>
  Object.entries(obj) as [K,V][];

function copyWithTextarea(text: string) {
  const textarea = document.createElement("textarea");
  textarea.value = text;
  textarea.setAttribute("readonly", "");
  textarea.style.position = "fixed";
  textarea.style.opacity = "0";
  document.body.appendChild(textarea);
  textarea.select();
  const ok = document.execCommand("copy");
  textarea.remove();
  if (!ok) throw new Error("copy");
}

export async function copyText(text: string) {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
    } else {
      copyWithTextarea(text);
    }
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

export function randomId(): string {
  if (typeof crypto.randomUUID === "function") return crypto.randomUUID();

  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}
