import { useMemo } from "react";
import {
  Bar, BarChart, CartesianGrid, Legend,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts";
import { ChartEmpty, ChartTooltip } from "../../shared";
import type { DailyDishes } from "../model/types";

const ROW_HEIGHT = 44;

export const DailyDishesChart = ({ data }: { data: DailyDishes }) => {
  const chartData = useMemo(
    () =>
      [...data.items]
        .sort((a, b) => b.portions - a.portions)
        .map((item) => ({
          name: item.dishName,
          portions: item.portions,
          orders: item.orders,
        })),
    [data.items],
  );

  return (
    <div className="flex flex-col gap-3">
      <div className="flex gap-2 flex-wrap">
        <span className="bg-secondary px-3 py-1 rounded-full tabular-nums">
          Порций: {data.totalPortions}
        </span>
        <span className="bg-secondary px-3 py-1 rounded-full tabular-nums">
          Заказов: {data.totalOrders}
        </span>
      </div>

      {chartData.length === 0 ? (
        <ChartEmpty text="За выбранный день продаж нет" />
      ) : (
        <div className="w-full" style={{ height: Math.max(chartData.length * ROW_HEIGHT + 60, 200) }}>
          <ResponsiveContainer>
            <BarChart data={chartData} layout="vertical" barGap={2} margin={{ left: 8, right: 16 }}>
              <CartesianGrid
                strokeDasharray="3 3"
                horizontal={false}
                stroke="var(--primary)"
                strokeOpacity={0.15}
              />
              <XAxis
                type="number"
                allowDecimals={false}
                tick={{ fill: "var(--text)", fontSize: 12 }}
                tickLine={false}
                axisLine={false}
              />
              <YAxis
                type="category"
                dataKey="name"
                width={140}
                interval={0}
                tick={{ fill: "var(--text)", fontSize: 12 }}
                tickLine={false}
                axisLine={false}
              />
              <Tooltip
                content={ChartTooltip}
                cursor={{ fill: "var(--primary)", fillOpacity: 0.06 }}
              />
              <Legend wrapperStyle={{ fontSize: 13 }} />

              <Bar dataKey="portions" name="Порций"
                   fill="var(--primary)" radius={[0, 4, 4, 0]} maxBarSize={16} />
              <Bar dataKey="orders" name="Заказов"
                   fill="var(--accent)" radius={[0, 4, 4, 0]} maxBarSize={16} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
};
