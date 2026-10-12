import { DailyDishesChart } from "@widgets/charts/daily-dishes-chart";
import { DailyIngredientsChart } from "@widgets/charts/daily-ingredients-chart";
import { DeficitsChart } from "@widgets/charts/deficits-chart";
import { StockForecastChart } from "@widgets/charts/stock-forecast-chart";
import { SupplyForecastChart } from "@widgets/charts/supply-forecast-chart";
import { WriteOffsChart } from "@widgets/charts/write-offs-chart";
import dayjs from "dayjs";
import type { ReactNode } from "react";
import { useState } from "react";
import { FaChartBar } from "react-icons/fa";
import { AnalyticsChart } from "./AnalyticsChart";

interface Filters {
  date: string;
  // Начало периода для отчетов за несколько дней
  from: string;
}

interface Variant {
  label: string;
  withDate?: boolean;
  withPeriod?: boolean;
  render: (filters: Filters) => ReactNode;
}

const variants = {
  supply: {
    label: "Следует закупить",
    render: () => <AnalyticsChart path="/supply-suggestions" Chart={SupplyForecastChart} />,
  },
  dishes: {
    label: "Продано блюд",
    withDate: true,
    render: ({ date }) => (
      <AnalyticsChart path="/daily/dishes" params={{ date }} Chart={DailyDishesChart} />
    ),
  },
  ingredients: {
    label: "Потрачено ингредиентов",
    withDate: true,
    render: ({ date }) => (
      <AnalyticsChart path="/daily/ingredients" params={{ date }} Chart={DailyIngredientsChart} />
    ),
  },
  forecast: {
    label: "Прогноз стоп-листа",
    render: () => (
      <AnalyticsChart path="/stock-forecast" params={{ limit: 100 }} Chart={StockForecastChart} />
    ),
  },
  writeOffs: {
    label: "Списано просрочки",
    withPeriod: true,
    render: ({ from }) => (
      <AnalyticsChart path="/write-offs" params={{ from, limit: 100 }} Chart={WriteOffsChart} />
    ),
  },
  deficits: {
    label: "Нехватка продуктов",
    withPeriod: true,
    render: ({ from }) => (
      <AnalyticsChart path="/deficits" params={{ from, limit: 100 }} Chart={DeficitsChart} />
    ),
  },
} satisfies Record<string, Variant>;

type VariantId = keyof typeof variants;

const variantIds = Object.keys(variants) as VariantId[];

const isVariantId = (value: string): value is VariantId => value in variants;

const periods = [
  { days: 1, label: "За сегодня" },
  { days: 7, label: "За неделю" },
  { days: 30, label: "За 30 дней" },
];

export const AnalyticsPage = () => {
  const [variantId, setVariantId] = useState<VariantId>("supply");
  const [date, setDate] = useState(() => dayjs().format("YYYY-MM-DD"));
  const [days, setDays] = useState(7);

  const variant: Variant = variants[variantId];
  // Начало дня — чтобы ключ запроса не менялся при каждом рендере
  const from = dayjs().subtract(days - 1, "day").startOf("day").toISOString();

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

        {variant.withPeriod && (
          <select
            value={days}
            onChange={(e) => setDays(Number(e.target.value))}
            className="p-1 rounded-md w-fit cursor-pointer"
          >
            {periods.map((period) => (
              <option value={period.days} key={period.days}>
                {period.label}
              </option>
            ))}
          </select>
        )}
      </div>
      <div key={variantId} className="flex-1 p-2 min-h-0 overflow-x-hidden flex flex-col">
        {variant.render({ date, from })}
      </div>
    </div>
  );
};
