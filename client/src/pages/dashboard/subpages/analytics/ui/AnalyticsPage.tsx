import { DailyDishesChart } from "@widgets/charts/daily-dishes-chart";
import { DailyIngredientsChart } from "@widgets/charts/daily-ingredients-chart";
import { SupplyForecastChart } from "@widgets/charts/supply-forecast-chart";
import dayjs from "dayjs";
import type { ReactNode } from "react";
import { useState } from "react";
import { FaChartBar } from "react-icons/fa";
import { AnalyticsChart } from "./AnalyticsChart";

interface Variant {
  label: string;
  withDate?: boolean;
  render: (date: string) => ReactNode;
}

const variants = {
  supply: {
    label: "Следует закупить",
    render: () => <AnalyticsChart path="/supply-suggestions" Chart={SupplyForecastChart} />,
  },
  dishes: {
    label: "Продано блюд",
    withDate: true,
    render: (date) => (
      <AnalyticsChart path="/daily/dishes" params={{ date }} Chart={DailyDishesChart} />
    ),
  },
  ingredients: {
    label: "Потрачено ингредиентов",
    withDate: true,
    render: (date) => (
      <AnalyticsChart path="/daily/ingredients" params={{ date }} Chart={DailyIngredientsChart} />
    ),
  },
} satisfies Record<string, Variant>;

type VariantId = keyof typeof variants;

const variantIds = Object.keys(variants) as VariantId[];

const isVariantId = (value: string): value is VariantId => value in variants;

export const AnalyticsPage = () => {
  const [variantId, setVariantId] = useState<VariantId>("supply");
  const [date, setDate] = useState(() => dayjs().format("YYYY-MM-DD"));

  const variant: Variant = variants[variantId];

  return (
    <div className="flex flex-col h-full gap-2">
      <h1 className="p-2 bg-accent text-2xl font-oswald shrink-0 flex gap-2 items-center justify-between">
        <span>Аналитика</span>
        <FaChartBar />
      </h1>

      <div className="flex gap-2 flex-wrap shrink-0 p-2">
        <select
          value={variantId}
          onChange={(e) => isVariantId(e.target.value) && setVariantId(e.target.value)}
          className="p-1 rounded-md w-fit cursor-pointer"
        >
          {variantIds.map((id) => (
            <option value={id} key={id}>
              {variants[id].label}
            </option>
          ))}
        </select>

        {variant.withDate && (
          <input
            type="date"
            value={date}
            max={dayjs().format("YYYY-MM-DD")}
            onChange={(e) => e.target.value && setDate(e.target.value)}
            className="p-1 rounded-md cursor-pointer"
          />
        )}
      </div>
      <div key={variantId} className="flex-1 p-2 min-h-0 overflow-x-hidden flex flex-col">
        {variant.render(date)}
      </div>
    </div>
  );
};
