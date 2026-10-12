import type { Unit } from "@lib/types";

export type ForecastCause = "consumption" | "expiry" | "already_stopped";

export interface LimitingIngredient {
  ingredientId: string;
  ingredientName: string;
  unit: Unit;
  availableQuantity: number;
  requiredPerPortion: number;
  dailyConsumption: number;
}

export interface StockForecastItem {
  dishId: string;
  dishName: string;
  availablePortions: number;
  // null — в пределах горизонта в стоп не уйдет
  stopAt: string | null;
  cause: ForecastCause | null;
  limitingIngredient: LimitingIngredient;
}

// GET /analytics/stock-forecast
export interface StockForecast {
  generatedAt: string;
  horizonDays: number;
  consumptionWindowDays: number;
  items: StockForecastItem[];
  page: number;
  limit: number;
  total: number;
}
