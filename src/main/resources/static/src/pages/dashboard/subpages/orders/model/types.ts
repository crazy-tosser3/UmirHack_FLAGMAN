import type { Unit } from "@lib/types";

export type OrderStatus = "confirmed" | "cancelled";

export interface CreateOrderPayload {
  externalOrderId?: string;
  items: { dishId: string; quantity: number }[];
}

export interface ConsumptionItem {
  ingredientId: string;
  quantity: number;
  unit: Unit;
  allocations: { batchId: string; quantity: number }[];
}

export interface Order {
  id: string;
  externalOrderId: string | null;
  status: OrderStatus;
  createdAt: string;
  items: { dishId: string; dishName: string; quantity: number }[];
  consumption: ConsumptionItem[];
  stockVersion: number | null;
}

export interface OrderListItem {
  id: string;
  externalOrderId: string | null;
  status: OrderStatus;
  createdAt: string;
  itemsCount: number;
}

export interface OrdersPage {
  items: OrderListItem[];
  page: number;
  limit: number;
  total: number;
}

export interface InsufficientStockDetail {
  ingredientId: string;
  requiredQuantity: number;
  availableQuantity: number;
  missingQuantity: number;
  unit: Unit;
}

export interface CatalogIngredient {
  id: string;
  name: string;
  unit: Unit;
}
