import type { Unit } from "@lib/types";

export interface Reason {
  availableQuantity: number;
  code: string;
  ingredientId: string;
  ingredientName: string;
  requiredQuantity: number;
  unit: Unit;
}

export interface StopListDish {
  dishId: string;
  dishName: string;
  isAvailable: boolean;
  reasons: Reason[];
  updatedAt: string;
}

export interface StopListData {
  items: StopListDish[];
  limit: number;
  page: number;
  total: number;
}