export interface DeliveryListItem {
  id: string;
  deliveryNumber: string;
  receivedAt: string;
  status: string;
  itemsCount: number;
}

export interface DeliveriesList {
  items: DeliveryListItem[];
  page: number;
  limit: number;
  total: number;
}

export interface DeliveryItem {
  batchId: string;
  ingredientId: string;
  receivedQuantity: number;
  remainingQuantity: number;
  expiresAt: string;
}

export interface Delivery {
  id: string;
  deliveryNumber: string;
  createdAt: string;
  receivedAt: string;
  status: string;
  items: DeliveryItem[];
}

export interface CreateDeliveryPayload {
  deliveryNumber: string;
  receivedAt: string;
  items: { ingredientId: string; quantity: number; expiresAt: string }[];
}
