import type { Unit } from "@lib/types";

export type StopReasonCode = "manual" | "insufficient_stock" | "expired_ingredient";

export interface Reason {
  code: StopReasonCode | string;
  ingredientId: string | null;
  ingredientName: string | null;
  requiredQuantity: number | null;
  availableQuantity: number | null;
  unit: Unit | null;
}

export interface StopListDish {
  dishId: string;
  dishName: string;
  manualStop?: boolean;
  isAvailable: boolean;
  reasons: Reason[];
  updatedAt?: string;
}

export interface StopListData {
  items: StopListDish[];
  limit: number;
  page: number;
  total: number;
}

export const isManualStop = (dish: Pick<StopListDish, "manualStop" | "reasons">) =>
  dish.manualStop ?? dish.reasons.some((r) => r.code === "manual");
