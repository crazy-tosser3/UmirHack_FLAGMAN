import type { Unit } from "@lib/types";

export interface SupplyItem {
  ingredientId: string;
  ingredientName: string;
  unit: Unit;
  availableQuantity: number;
  dailyConsumption: number;
  depletionAt: string;
  suggestedQuantity: number;
}

export interface SupplyForecast {
  generatedAt: string;
  coverDays: number;
  items: SupplyItem[];
}