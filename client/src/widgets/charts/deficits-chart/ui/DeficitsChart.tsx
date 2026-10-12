import { useMemo } from "react";
import {
  Bar, BarChart, CartesianGrid, Legend,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts";
import dayjs from "dayjs";
import { units } from "@lib/utils";
import { ChartEmpty, ChartTooltip } from "../../shared";
import type { Deficits } from "../model/types";

const ROW_HEIGHT = 36;

export const DeficitsChart = ({ data }: { data: Deficits }) => {
  const chartData = useMemo(
    () =>
      data.items.map((item) => ({
        name: item.ingredientName,
        deficits: item.deficitCount,
      })),
    [data.items],
  );

  const deficitsCount = data.items.reduce((sum, item) => sum + item.deficitCount, 0);

  if (data.items.length === 0) return <ChartEmpty text="За выбранный период нехватки продуктов не было" />;

  return (
    <div className="flex flex-col gap-3">
      <div className="flex gap-2 flex-wrap">
        <span className="bg-secondary px-3 py-1 rounded-full tabular-nums">
          Отказов в заказах: {deficitsCount}
        </span>
        <span className="bg-secondary px-3 py-1 rounded-full tabular-nums">
          Продуктов: {data.total}
        </span>
      </div>

      <div className="w-full" style={{ height: Math.max(chartData.length * ROW_HEIGHT + 60, 200) }}>
        <ResponsiveContainer>
          <BarChart data={chartData} layout="vertical" margin={{ left: 8, right: 16 }}>
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

            <Bar dataKey="deficits" name="Отказов из-за нехватки"
                 fill="var(--accent-2)" radius={[0, 4, 4, 0]} maxBarSize={20} />
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="flex flex-col gap-2">
        {data.items.map((item) => (
          <div key={item.ingredientId} className="flex flex-col gap-1 min-w-0 p-2 rounded-lg border border-primary">
            <div className="flex justify-between items-start gap-2">
              <p className="truncate flex-1">{item.ingredientName}</p>
              <span className="text-sm tabular-nums whitespace-nowrap text-err bg-err-bg px-2 rounded-full">
                не хватило {item.missingQuantity} {units[item.unit]}
              </span>
            </div>
            <span className="text-sm text-primary">
              Последний раз: {dayjs(item.lastDeficitAt).format("DD.MM HH:mm")}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};
