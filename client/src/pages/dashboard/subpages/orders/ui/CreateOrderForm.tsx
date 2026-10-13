import api from "@lib/api";
import type { ApiErrorBody } from "@lib/types";
import { apiErrorCode, apiErrorMessage, formatDateTime, randomId, units } from "@lib/utils";
import type { DishesPage } from "@shared/table";
import { useToastStore } from "@shared/toasts";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { useState } from "react";
import { FaCheck, FaTrash } from "react-icons/fa6";
import type {
  CatalogIngredient,
  CreateOrderPayload,
  InsufficientStockDetail,
  Order,
} from "../model/types";

interface OrderRow {
  dishId: string;
  quantity: string;
}

const EMPTY_ROW: OrderRow = { dishId: "", quantity: "1" };

const INVALIDATE_KEYS = ["orders", "stock", "stop-list", "offers", "analytics"];

const shortageDetails = (err: unknown): InsufficientStockDetail[] => {
  if (apiErrorCode(err) !== "INSUFFICIENT_STOCK" || !isAxiosError<ApiErrorBody>(err)) return [];
  const details = err.response?.data?.error?.details;
  return Array.isArray(details) ? (details as InsufficientStockDetail[]) : [];
};

export const CreateOrderForm = () => {
  const [externalId, setExternalId] = useState("");
  const [rows, setRows] = useState<OrderRow[]>([EMPTY_ROW]);
  const [formError, setFormError] = useState<string | null>(null);
  const [shortage, setShortage] = useState<InsufficientStockDetail[]>([]);
  const [idempotencyKey, setIdempotencyKey] = useState(() => randomId());
  const [lastOrder, setLastOrder] = useState<Order | null>(null);

  const queryClient = useQueryClient();
  const newToast = useToastStore((s) => s.newToast);

  const { data: dishes } = useQuery<DishesPage>({
    queryKey: ["dishes", "active-catalog"],
    queryFn: async () =>
      (await api.get<DishesPage>("/dishes", { params: { active: true, limit: 100 } })).data,
    staleTime: 60 * 1000,
  });

  const { data: ingredients } = useQuery<{ items: CatalogIngredient[] }>({
    queryKey: ["ingredients", "catalog"],
    queryFn: async () =>
      (await api.get<{ items: CatalogIngredient[] }>("/ingredients", { params: { limit: 100 } })).data,
    enabled: shortage.length > 0,
    staleTime: 5 * 60 * 1000,
  });

  const ingredientName = (id: string) =>
    ingredients?.items.find((i) => i.id === id)?.name ?? "Ингредиент";

  const resetForm = () => {
    setExternalId("");
    setRows([EMPTY_ROW]);
    setFormError(null);
    setShortage([]);
    setIdempotencyKey(randomId());
  };

  const createMutation = useMutation({
    mutationFn: async (payload: CreateOrderPayload) =>
      (await api.post<Order>("/orders", payload, {
        headers: { "Idempotency-Key": idempotencyKey },
      })).data,
    onSuccess: (order) => {
      INVALIDATE_KEYS.forEach((key) => queryClient.invalidateQueries({ queryKey: [key] }));
      newToast("Заказ принят", "success");
      setLastOrder(order);
      resetForm();
    },
    onError: (err) => {
      setShortage(shortageDetails(err));
      setFormError(apiErrorMessage(err, "Не удалось принять заказ"));
      if (apiErrorCode(err) !== undefined) setIdempotencyKey(randomId());
    },
  });

  const updateRow = (index: number, patch: Partial<OrderRow>) =>
    setRows((prev) => prev.map((r, i) => (i === index ? { ...r, ...patch } : r)));

  const validate = (): string | null => {
    const seen = new Set<string>();
    for (const row of rows) {
      if (!row.dishId) return "Выберите блюдо во всех строках";
      if (seen.has(row.dishId)) return "Блюдо указано дважды - объедините строки";
      seen.add(row.dishId);

      const qty = Number(row.quantity);
      if (!Number.isInteger(qty) || qty < 1 || qty > 1000)
        return "Количество должно быть целым числом от 1 до 1000";
    }
    return null;
  };

  const handleSubmit = (e: React.SubmitEvent) => {
    e.preventDefault();
    const error = validate();
    setShortage([]);
    if (error) {
      setFormError(error);
      return;
    }

    createMutation.mutate({
      externalOrderId: externalId.trim() || undefined,
      items: rows.map((r) => ({ dishId: r.dishId, quantity: Number(r.quantity) })),
    });
  };

  return (
    <div className="flex flex-col gap-4">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        <label className="flex flex-col gap-1 text-sm">
          <span className="text-primary">Номер заказа (необязательно)</span>
          <input
            type="text"
            maxLength={100}
            value={externalId}
            onChange={(e) => setExternalId(e.target.value)}
            placeholder="POS-000123"
            className="rounded-xl border border-primary bg-bg px-2 py-2"
          />
        </label>

        <fieldset className="flex flex-col gap-2">
          <legend className="mb-1 text-sm">Состав заказа</legend>

          {rows.map((row, index) => (
            <div key={index} className="flex flex-col sm:flex-row items-center gap-2">
              <select
                value={row.dishId}
                onChange={(e) => updateRow(index, { dishId: e.target.value })}
                className="w-full sm:w-auto min-w-0 flex-1 cursor-pointer rounded-xl border border-primary bg-bg px-2 py-2 text-sm"
              >
                <option value="">- блюдо -</option>
                {dishes?.items.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </select>
              <div className="flex items-center gap-2">
                <input
                  type="number"
                  min={1}
                  max={1000}
                  step={1}
                  value={row.quantity}
                  onChange={(e) => updateRow(index, { quantity: e.target.value })}
                  className="w-20 rounded-xl border border-primary bg-bg px-2 py-2 text-sm tabular-nums"
                />
                <span className="w-8 shrink-0 text-sm text-primary">шт</span>
                <button
                  type="button"
                  onClick={() => setRows((prev) => prev.filter((_, i) => i !== index))}
                  disabled={rows.length === 1}
                  className="grid size-8 shrink-0 place-items-center rounded-lg border border-primary transition-colors hover:bg-err-bg hover:text-err disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-inherit"
                >
                  <FaTrash size={12} />
                </button>
              </div>
            </div>
          ))}

          <button
            type="button"
            onClick={() => setRows((prev) => [...prev, EMPTY_ROW])}
            className="w-fit rounded-xl border border-primary px-2 py-1 text-sm transition-colors hover:bg-secondary"
          >
            + добавить блюдо
          </button>
        </fieldset>

        {formError && (
          <div className="flex flex-col gap-2 rounded-xl bg-err-bg p-2 text-sm text-err">
            <p>{formError}</p>
            {shortage.length > 0 && (
              <ul className="flex flex-col gap-1">
                {shortage.map((s) => (
                  <li key={s.ingredientId} className="flex justify-between gap-4">
                    <span className="truncate">{ingredientName(s.ingredientId)}</span>
                    <span className="tabular-nums shrink-0">
                      не хватает {s.missingQuantity} {units[s.unit] ?? s.unit}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        <div className="flex justify-end gap-2">
          <button
            type="button"
            onClick={resetForm}
            className="rounded-xl border border-primary px-4 py-2 text-sm transition-colors hover:bg-secondary"
          >
            Очистить
          </button>
          <button
            type="submit"
            disabled={createMutation.isPending}
            className="rounded-xl bg-accent px-4 py-2 text-sm transition-colors hover:bg-accent/70 disabled:opacity-60"
          >
            {createMutation.isPending ? "Отправка..." : "Принять заказ"}
          </button>
        </div>
      </form>

      {lastOrder && (
        <div className="flex flex-col gap-2 rounded-xl border border-primary p-2 text-sm">
          <div className="flex items-center justify-between gap-2">
            <span className="flex items-center gap-2 bg-success-bg text-success px-2 py-0.5 rounded-full">
              <FaCheck size={10} />
              Принят {lastOrder.externalOrderId ?? lastOrder.id.slice(0, 8)}
            </span>
            <span className="text-primary">{formatDateTime(lastOrder.createdAt)}</span>
          </div>
          <ul className="flex flex-col gap-1">
            {lastOrder.items.map((item) => (
              <li key={item.dishId} className="flex justify-between gap-4">
                <span className="truncate">{item.dishName}</span>
                <span className="tabular-nums shrink-0">× {item.quantity}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};
