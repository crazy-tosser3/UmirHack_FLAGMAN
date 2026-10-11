import type { Unit } from "@lib/types";

// GET /analytics/daily/ingredients
export interface DailyIngredientItem {
  ingredientId: string;
  ingredientName: string;
  unit: Unit;
  // Списано под заказы
  consumedQuantity: number;
  // Списано как просроченное
  writtenOffQuantity: number;
  totalQuantity: number;
}

export interface DailyIngredients {
  // YYYY-MM-DD
  date: string;
  // Границы дня в часовом поясе ресторана
  from: string;
  to: string;
  items: DailyIngredientItem[];
}
