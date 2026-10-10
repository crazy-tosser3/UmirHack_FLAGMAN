import { useToastStore } from "@shared/toasts";
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