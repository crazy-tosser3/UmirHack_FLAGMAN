import api from "@lib/api";
import { units } from "@lib/utils";
import { useIngredientsCatalog } from "@shared/hooks/use-ingredients-catalog";
import { useQuery } from "@tanstack/react-query";
import dayjs, { type Dayjs } from "dayjs";
import { useMemo } from "react";
import type { BatchItem, CalendarEvent, DeliveryItem, Paged, SupplyItem } from "./types";

const PAGE_LIMIT = 100;
const DAY_FORMAT = "YYYY-MM-DD";

const fetchAllPages = async <T,>(url: string, params: Record<string, string>) => {
  const items: T[] = [];
  for (let page = 1; ; page++) {
    const { data } = await api.get<Paged<T>>(url, { params: { ...params, page, limit: PAGE_LIMIT } });
    items.push(...data.items);
    if (items.length >= data.total || data.items.length < PAGE_LIMIT) break;
  }
  return items;
};

export function useCalendarEvents(rangeStart: Dayjs, rangeEnd: Dayjs) {
  const from = rangeStart.startOf("day").toISOString();
  const to = rangeEnd.endOf("day").toISOString();

  const batches = useQuery({
    queryKey: ["expiring-batches", "calendar", to],
    queryFn: () =>
      fetchAllPages<BatchItem>("/stock/batches", { expiryStatus: "all", expiresBefore: to }),
  });

  const deliveries = useQuery({
    queryKey: ["deliveries", "calendar", from, to],
    queryFn: () => fetchAllPages<DeliveryItem>("/deliveries", { from, to }),
  });

  const supply = useQuery({
    queryKey: ["analytics", "calendar-supply"],
    queryFn: async () =>
      (await api.get<{ items: SupplyItem[] }>("/analytics/supply-suggestions", {
        params: { coverDays: 30 },
      })).data.items,
  });

  const { data: ingredients } = useIngredientsCatalog();

  const events = useMemo(() => {
    const unitOf = (id: string) => {
      const unit = ingredients?.find((i) => i.id === id)?.unit;
      return unit ? units[unit] : "";
    };
    const today = dayjs();
    const result: CalendarEvent[] = [];

    for (const b of batches.data ?? []) {
      const at = dayjs(b.expiresAt);
      result.push({
        id: `batch-${b.batchId}`,
        type: b.expiryStatus === "expired" ? "expired" : b.expiryStatus === "expiring" ? "expiring" : "expiry",
        day: at.format(DAY_FORMAT),
        at: b.expiresAt,
        title: b.ingredientName,
        details: `${b.remainingQuantity} ${unitOf(b.ingredientId)} · срок до ${at.format("HH:mm")}`,
      });
    }

    for (const d of deliveries.data ?? []) {
      const at = dayjs(d.receivedAt);
      result.push({
        id: `delivery-${d.id}`,
        type: "delivery",
        day: at.format(DAY_FORMAT),
        at: d.receivedAt,
        title: `Поставка ${d.deliveryNumber}`,
        details: `${d.itemsCount} поз. · принята в ${at.format("HH:mm")}`,
      });
    }

    for (const s of supply.data ?? []) {
      if (!s.depletionAt) continue;
      const depletion = dayjs(s.depletionAt);
      const at = depletion.isBefore(today) ? today : depletion;
      result.push({
        id: `supply-${s.ingredientId}`,
        type: "supply",
        day: at.format(DAY_FORMAT),
        at: at.toISOString(),
        title: s.ingredientName,
        details: `закупить ${s.suggestedQuantity} ${unitOf(s.ingredientId)}${depletion.isBefore(today) ? " · уже закончился" : ` · закончится в ${depletion.format("HH:mm")}`}`,
      });
    }

    return result.sort((a, b) => a.at.localeCompare(b.at));
  }, [batches.data, deliveries.data, supply.data, ingredients]);

  return {
    events,
    isPending: batches.isPending || deliveries.isPending || supply.isPending,
    isError: batches.isError || deliveries.isError || supply.isError,
  };
}
