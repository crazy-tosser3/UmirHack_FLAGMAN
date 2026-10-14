import type { Unit } from "@lib/types";

export interface StockUpdatedEvent {
  stockVersion: number;
  ingredientIds: string[];
}

export interface StopListUpdatedEvent {
  updatedAt: string;
  affectedDishIds: string[];
}

export interface OffersUpdatedEvent {
  generatedAt: string;
}

export interface DeliveryCreatedEvent {
  deliveryId: string;
  deliveryNumber: string | null;
  ingredientIds: string[];
}

export interface OrderConfirmedEvent {
  orderId: string;
  externalOrderId: string | null;
}

export interface BatchExpiryEvent {
  batchId: string;
  ingredientId: string;
  ingredientName: string;
  unit: Unit;
  quantity: number;
  expiresAt: string;
  writtenOff?: boolean | null;
}

export interface ServerEventMap {
  "stock.updated": StockUpdatedEvent;
  "stop-list.updated": StopListUpdatedEvent;
  "offers.updated": OffersUpdatedEvent;
  "delivery.created": DeliveryCreatedEvent;
  "order.confirmed": OrderConfirmedEvent;
  "batch.expiring": BatchExpiryEvent;
  "batch.expired": BatchExpiryEvent;
}

export type ServerEventName = keyof ServerEventMap;

export type ServerEvent = {
  [K in ServerEventName]: { name: K; data: ServerEventMap[K] };
}[ServerEventName];
