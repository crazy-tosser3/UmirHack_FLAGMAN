export type CalendarEventType = "expired" | "expiring" | "expiry" | "supply" | "delivery";

export interface CalendarEvent {
  id: string;
  type: CalendarEventType;
  day: string;
  at: string;
  title: string;
  details: string;
}

export interface BatchItem {
  batchId: string;
  deliveryId: string | null;
  ingredientId: string;
  ingredientName: string;
  remainingQuantity: number;
  expiresAt: string;
  expiryStatus: "valid" | "expiring" | "expired";
}

export interface DeliveryItem {
  id: string;
  deliveryNumber: string;
  receivedAt: string;
  itemsCount: number;
}

export interface SupplyItem {
  ingredientId: string;
  ingredientName: string;
  depletionAt: string | null;
  suggestedQuantity: number;
}

export interface Paged<T> {
  items: T[];
  page: number;
  limit: number;
  total: number;
}
