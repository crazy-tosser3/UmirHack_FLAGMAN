// GET /analytics/daily/dishes
export interface DailyDishItem {
  dishId: string;
  dishName: string;
  portions: number;
  orders: number;
}

export interface DailyDishes {
  // YYYY-MM-DD
  date: string;
  from: string;
  to: string;
  totalPortions: number;
  totalOrders: number;
  items: DailyDishItem[];
}
