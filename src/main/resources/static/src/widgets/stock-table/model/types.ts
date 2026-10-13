import type { Unit } from "@lib/types";

export interface StockItem {
  ingredientId: string;
  name: string;
  unit: Unit;
  totalQuantity: number;
  availableQuantity: number;
  reservedQuantity: number;
  expiredQuantity: number;
  nearestExpiryAt: string | null;
}

export interface StockPage {
  items: StockItem[];
  page: number;
  limit: number;
  total: number;
}