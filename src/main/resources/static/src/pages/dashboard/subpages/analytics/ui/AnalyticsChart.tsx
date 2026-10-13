import api from "@lib/api";
import { useDelayedValue } from "@shared/hooks/use-delayed-value";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import type { ComponentType } from "react";
import { FaCircleNotch } from "react-icons/fa";

interface AnalyticsChartProps<T> {
  path: string;
  params?: Record<string, string | number | undefined>;
  Chart: ComponentType<{ data: T }>;
}

export const AnalyticsChart = <T,>({ path, params, Chart }: AnalyticsChartProps<T>) => {
  const { data, isPending, isError, isFetching } = useQuery({
    queryKey: ["analytics", path, params],
    queryFn: async () => {
      const res = await api.get<T>("/analytics" + path, { params });
      return res.data;
    },
    placeholderData: keepPreviousData,
  });

  const showLoader = useDelayedValue(isPending, { minDisplay: 300 });

  if (showLoader || isPending) {
    return (
      <div className="flex-1 grid place-items-center">
        {showLoader && <FaCircleNotch size="20" className="animate-spin" />}
      </div>
    );
  }

  if (isError) {
    return (
      <p className="text-err bg-err-bg p-2 rounded-xl text-center shrink-0">
        Не удалось загрузить данные
      </p>
    );
  }

  return (
    <div className={isFetching ? "opacity-60 transition-opacity" : "transition-opacity"}>
      <Chart data={data} />
    </div>
  );
};
