// GET /analytics/daily/dishes
export interface DailyDishItem {
  dishId: string;
  dishName: string;
  portions: number;
  // В скольких заказах встречалось блюдо
  orders: number;
}

export interface DailyDishes {
  // YYYY-MM-DD
  date: string;
  // Границы дня в часовом поясе ресторана
  from: string;
  to: string;
  totalPortions: number;
  totalOrders: number;
  items: DailyDishItem[];
}
