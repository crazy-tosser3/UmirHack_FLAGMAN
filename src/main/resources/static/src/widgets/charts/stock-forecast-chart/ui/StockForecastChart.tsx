import { useMemo } from "react";
import {
  Bar, BarChart, CartesianGrid, Cell,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import "dayjs/locale/ru";
import { cn } from "@lib/utils";
import { ChartEmpty, ChartTooltip } from "../../shared";
import type { ForecastCause, StockForecast } from "../model/types";

dayjs.extend(relativeTime);
dayjs.locale("ru");

const ROW_HEIGHT = 36;

const causes: Record<ForecastCause, { label: string; color: string; className: string }> = {
  already_stopped: { label: "Уже в стопе", color: "var(--color-err)", className: "text-err bg-err-bg" },
  expiry: { label: "Испортится", color: "var(--accent-2)", className: "text-warn bg-warn-bg" },
  consumption: { label: "Закончится", color: "var(--primary)", className: "bg-secondary" },
};

export const StockForecastChart = ({ data }: { data: StockForecast }) => {
  const atRisk = useMemo(
    () => data.items.filter((item) => item.stopAt && item.cause),
    [data.items],
  );

  const chartData = useMemo(
    () =>
      atRisk.map((item) => ({
        name: item.dishName,
        hours: Math.max(0, Math.round(dayjs(item.stopAt).diff(dayjs(), "hour", true))),
        cause: item.cause as ForecastCause,
      })),
    [atRisk],
  );

  const safeCount = data.items.length - atRisk.length;

  if (atRisk.length === 0) {
    return <ChartEmpty text={`В ближайшие ${data.horizonDays} дн. блюда не уйдут в стоп-лист`} />;
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex gap-2 flex-wrap">
        {(Object.keys(causes) as ForecastCause[]).map((cause) => (
          <span key={cause} className="flex items-center gap-2 bg-secondary px-3 py-1 rounded-full">
            <i className="w-3 h-3 rounded-full" style={{ background: causes[cause].color }} />
            {causes[cause].label}
          </span>
        ))}
        {safeCount > 0 && (
          <span className="bg-secondary px-3 py-1 rounded-full tabular-nums">
            Без риска: {safeCount}
          </span>
        )}
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
              unit=" ч"
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

            <Bar dataKey="hours" name="Часов до стоп-листа" radius={[0, 4, 4, 0]} maxBarSize={20}>
              {chartData.map((entry) => (
                <Cell key={entry.name} fill={causes[entry.cause].color} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>

      <div className="flex flex-col gap-2">
        {atRisk.map((item) => {
          const cause = causes[item.cause as ForecastCause];
          return (
            <div key={item.dishId} className="flex flex-col gap-1 min-w-0 p-2 rounded-lg border border-primary">
              <div className="flex justify-between items-start gap-2">
                <p className="truncate flex-1">{item.dishName}</p>
                <span className={cn("text-sm whitespace-nowrap px-2 rounded-full", cause.className)}>
                  {item.cause === "already_stopped" ? cause.label : dayjs().to(dayjs(item.stopAt))}
                </span>
              </div>
              <div className="flex justify-between items-center gap-2 text-sm text-primary">
                <span className="truncate">
                  {cause.label}: {item.limitingIngredient.ingredientName}
                </span>
                <span className="tabular-nums whitespace-nowrap">
                  Порций: {item.availablePortions}
                </span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
