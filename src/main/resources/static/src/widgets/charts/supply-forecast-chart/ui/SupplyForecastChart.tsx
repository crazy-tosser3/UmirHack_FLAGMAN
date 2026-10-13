import { useMemo, useState } from "react";
import {
  Bar, BarChart, CartesianGrid, Legend,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts";
import { FaChevronLeft, FaChevronRight } from "react-icons/fa";
import { ChartEmpty, ChartTooltip } from "../../shared";
import type { SupplyForecast } from "../model/types";
import { useMediaQuery } from "@shared/hooks/use-media-query";

const compactNumber = new Intl.NumberFormat("ru-RU", { notation: "compact", maximumFractionDigits: 1 });

export const SupplyForecastChart = ({ data }: { data: SupplyForecast }) => {
  const pageSize = useMediaQuery("(min-width: 900px)") ? 6 : 1;
  const [page, setPage] = useState(0);

  const sortedItems = useMemo(
    () => [...data.items].sort((a, b) => b.suggestedQuantity - a.suggestedQuantity),
    [data.items],
  );

  const totalPages = Math.ceil(sortedItems.length / pageSize);
  const safePage = Math.max(0, Math.min(page, totalPages - 1));

  const paginatedItems = useMemo(() => {
    const start = safePage * pageSize;
    return sortedItems.slice(start, start + pageSize);
  }, [sortedItems, safePage, pageSize]);

  const chartData = useMemo(
    () =>
      paginatedItems.map((item) => ({
        name: item.ingredientName,
        unit: item.unit,
        available: Math.round(item.availableQuantity),
        required: Math.round(item.dailyConsumption * data.coverDays),
        suggested: Math.round(item.suggestedQuantity),
      })),
    [paginatedItems, data.coverDays],
  );

  if (data.items.length === 0) return <ChartEmpty text="Закупать ничего не нужно" />;

  return (
    <div className="flex flex-col gap-2">
      <div className="h-80 w-full">
        <ResponsiveContainer>
          <BarChart data={chartData} barGap={4}>
            <CartesianGrid
              strokeDasharray="3 3"
              vertical={false}
              stroke="var(--primary)"
              strokeOpacity={0.15}
            />
            <XAxis
              dataKey="name"
              interval={0}
              tick={{ fill: "var(--text)", fontSize: 12 }}
              tickLine={false}
              axisLine={false}
            />
            <YAxis
              width={56}
              tickFormatter={(value: number) => compactNumber.format(value)}
              tick={{ fill: "var(--text)", fontSize: 12 }}
              tickLine={false}
              axisLine={false}
            />
            <Tooltip
              content={ChartTooltip}
              cursor={{ fill: "var(--primary)", fillOpacity: 0.06 }}
            />
            <Legend wrapperStyle={{ fontSize: 13 }} />

            <Bar dataKey="available" name="В наличии"
                 fill="var(--color-accent-2)" radius={[4, 4, 0, 0]} maxBarSize={40} />
            <Bar dataKey="required" name={`Нужно на ${data.coverDays} дн.`}
                 fill="var(--primary)" radius={[4, 4, 0, 0]} maxBarSize={40} />
            <Bar dataKey="suggested" name="Рекомендуется закупить"
                 fill="var(--accent)" radius={[4, 4, 0, 0]} maxBarSize={40} />
          </BarChart>
        </ResponsiveContainer>
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-3">
          <button
            type="button"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={safePage === 0}
            className="p-2 rounded-lg border border-primary/20 hover:bg-secondary transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
          >
            <FaChevronLeft size={12} />
          </button>

          <span className="text-sm text-primary/60 tabular-nums">
            {safePage + 1} / {totalPages}
          </span>

          <button
            type="button"
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={safePage === totalPages - 1}
            className="p-2 rounded-lg border border-primary/20 hover:bg-secondary transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
          >
            <FaChevronRight size={12} />
          </button>
        </div>
      )}
    </div>
  );
};