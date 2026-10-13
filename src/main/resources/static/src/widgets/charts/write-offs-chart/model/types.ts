import type { Unit } from "@lib/types";

export interface WriteOffItem {
  ingredientId: string;
  ingredientName: string;
  unit: Unit;
  quantity: number;
  batchesCount: number;
}

// GET /analytics/write-offs
export interface WriteOffs {
  from: string;
  to: string;
  items: WriteOffItem[];
  page: number;
  limit: number;
  total: number;
}
