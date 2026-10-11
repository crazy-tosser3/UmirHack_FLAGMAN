import { useMemo, useState } from "react";
import {
  Bar, BarChart, CartesianGrid, Legend,
  ResponsiveContainer, Tooltip, XAxis, YAxis,
} from "recharts";
import type { Unit } from "@lib/types";
import { cn, units } from "@lib/utils";
import { ChartEmpty, ChartTooltip } from "../../shared";
import type { DailyIngredients } from "../model/types";

const ROW_HEIGHT = 36;

export const DailyIngredientsChart = ({ data }: { data: DailyIngredients }) => {
  // Разные единицы на одной оси несравнимы, поэтому показываем одну единицу за раз
  const availableUnits = useMemo(
    () => [...new Set(data.items.map((item) => item.unit))],
    [data.items],
  );
  const [selectedUnit, setSelectedUnit] = useState<Unit | null>(null);
  const unit = selectedUnit && availableUnits.includes(selectedUnit)
    ? selectedUnit
    : availableUnits[0];

  const chartData = useMemo(
    () =>
      data.items
        .filter((item) => item.unit === unit)
        .sort((a, b) => b.totalQuantity - a.totalQuantity)
        .map((item) => ({
          name: item.ingredientName,
          unit: item.unit,
          consumed: item.consumedQuantity,
          writtenOff: item.writtenOffQuantity,
        })),
    [data.items, unit],
  );

  if (!unit) return <ChartEmpty text="За выбранный день списаний нет" />;

  return (
    <div className="flex flex-col gap-3">
      {availableUnits.length > 1 && (
        <div className="flex gap-1 w-fit bg-secondary p-1 rounded-lg">
          {availableUnits.map((u) => (
            <button
              key={u}
              type="button"
              onClick={() => setSelectedUnit(u)}
              className={cn(
                "px-3 py-0.5 rounded-md transition-colors",
                u === unit ? "bg-bg" : "hover:bg-bg/50",
              )}
            >
              {units[u]}
            </button>
          ))}
        </div>
      )}

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
              unit={` ${units[unit]}`}
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

            <Bar dataKey="consumed" name="Под заказы" stackId="total"
                 fill="var(--primary)" maxBarSize={20} />
            <Bar dataKey="writtenOff" name="Просрочено" stackId="total"
                 fill="var(--color-err)" radius={[0, 4, 4, 0]} maxBarSize={20} />
          </BarChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};
