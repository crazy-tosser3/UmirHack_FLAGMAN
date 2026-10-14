import type { Unit } from "@lib/types";

export type CellStatus = 'high' | 'medium' | 'low';
export type SortDirection = 'asc' | 'desc';
type Align = 'left' | 'center' | 'right';

export interface Cell {
  status?: CellStatus;
  align?: Align;
  body: React.ReactNode;
}

export interface ColumnConfig {
  label: string;
  sortable?: boolean;
  sortKey?: string;
  align?: Align;
}

export interface TableProps {
  columns: ColumnConfig[];
  data: Cell[][];
  sort?: { key: string; direction: SortDirection };
  onSort?: (key: string) => void;
}

export interface DishIngredient {
  ingredientId: string;
  ingredientName: string;
  quantity: number;
  unit: Unit;
}

export interface Dish {
  id: string;
  name: string;
  isActive: boolean;
  ingredients: DishIngredient[];
}

export interface DishesPage {
  items: Dish[];
  page: number;
  limit: number;
  total: number;
}