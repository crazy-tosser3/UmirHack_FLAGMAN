import type { Unit } from "@lib/types";

export interface DailyIngredientItem {
  ingredientId: string;
  ingredientName: string;
  unit: Unit;
  consumedQuantity: number;
  writtenOffQuantity: number;
  totalQuantity: number;
}

export interface DailyIngredients {
  date: string;
  from: string;
  to: string;
  items: DailyIngredientItem[];
}
