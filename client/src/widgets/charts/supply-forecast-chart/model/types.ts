import type { Unit } from "@lib/types";

export interface SupplyItem {
  ingredientId: string;
  ingredientName: string;
  unit: Unit;
  availableQuantity: number;
  dailyConsumption: number;
  // null — остаток не иссякнет в пределах горизонта
  depletionAt: string | null;
  suggestedQuantity: number;
}

// GET /analytics/supply-suggestions
export interface SupplyForecast {
  generatedAt: string;
  coverDays: number;
  items: SupplyItem[];
}