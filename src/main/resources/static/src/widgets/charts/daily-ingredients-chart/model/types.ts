import type { Unit } from "@lib/types";

// GET /analytics/daily/ingredients
export interface DailyIngredientItem {
  ingredientId: string;
  ingredientName: string;
  unit: Unit;
  consumedQuantity: number;
  writtenOffQuantity: number;
  totalQuantity: number;
}

export interface DailyIngredients {
  // YYYY-MM-DD
  date: string;
  from: string;
  to: string;
  items: DailyIngredientItem[];
}
