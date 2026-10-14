import type { Unit } from "@lib/types";

export interface DeficitItem {
  ingredientId: string;
  ingredientName: string;
  unit: Unit;
  deficitCount: number;
  missingQuantity: number;
  lastDeficitAt: string;
}

export interface Deficits {
  from: string;
  to: string;
  items: DeficitItem[];
  page: number;
  limit: number;
  total: number;
}
