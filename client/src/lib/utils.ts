import {clsx, type ClassValue} from "clsx";
import { twMerge } from "tailwind-merge";

export const cn = (...classes:ClassValue[]) => twMerge(clsx(classes));

export const typedEntries = <K extends PropertyKey, V>(obj: Record<K, V>): [K, V][] =>
  Object.entries(obj) as [K,V][];