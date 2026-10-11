import api from "@lib/api";
import { useDelayedValue } from "@shared/hooks/use-delayed-value";
import { useQuery } from "@tanstack/react-query";
import { SupplyForecastChart } from "@widgets/charts/supply-forecast-chart";
import type { ComponentType } from "react";
import { useState } from "react";
import { FaChartBar, FaCircleNotch } from "react-icons/fa";

interface ChartProps {
  data: unknown;
}

const variants: Array<{
  label: string;
  api: string;
  Chart: ComponentType<ChartProps>;
}> = [
  {
    label: "Следует закупить",
    api: "/supply-suggestions",
    Chart: SupplyForecastChart,
  },
  {
    label: "Продано блюд",
    api: "/daily/dishes",
    Chart: SupplyForecastChart,
  },
  {
    label: "Потрачено ингредиентов",
    api: "/daily/ingredients",
    Chart: SupplyForecastChart,
  },
];

export const AnalyticsPage = () => {
  const [apiPath, setApiPath] = useState(variants[0].api);

  const { data, isPending, isError } = useQuery({
    queryKey: ["analytics", apiPath],
    queryFn: async () => {
      const res = await api.get("/analytics" + apiPath);
      return res.data;
    },
  });

  const showLoader = useDelayedValue(isPending, { minDisplay: 300 });

  const activeVariant = variants.find((v) => v.api === apiPath) ?? variants[0];
  const Chart = activeVariant.Chart;

  return (
    <div className="flex flex-col h-full gap-2">
      <h1 className="text-2xl font-oswald shrink-0 flex gap-2 items-center justify-between">
        <span>Аналитика</span>
        <FaChartBar />
      </h1>

      {showLoader ? (
        <div className="flex-1 grid place-items-center">
          <FaCircleNotch size="20" className="animate-spin" />
        </div>
      ) : isError ? (
        <p className="text-err bg-err-bg p-2 rounded-xl text-center shrink-0">
          Не удалось загрузить данные
        </p>
      ) : (
        <div className="flex-1 min-h-0 overflow-auto flex flex-col gap-3">
          <select
            onChange={(e) => setApiPath(e.target.value)}
            className="p-1 rounded-md w-fit cursor-pointer"
          >
            {variants.map((variant) => (
              <option value={variant.api} key={variant.label}>
                {variant.label}
              </option>
            ))}
          </select>

          {data && <Chart data={data} />}
        </div>
      )}
    </div>
  );
};