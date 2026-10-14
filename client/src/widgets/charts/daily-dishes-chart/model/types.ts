export interface DailyDishItem {
  dishId: string;
  dishName: string;
  portions: number;
  orders: number;
}

export interface DailyDishes {
  date: string;
  from: string;
  to: string;
  totalPortions: number;
  totalOrders: number;
  items: DailyDishItem[];
}
