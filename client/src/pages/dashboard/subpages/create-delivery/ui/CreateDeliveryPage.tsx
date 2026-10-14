import api from "@lib/api";
import { apiErrorMessage, formatDateTime, randomId, units } from "@lib/utils";
import { useIngredientsCatalog } from "@shared/hooks/use-ingredients-catalog";
import { useToastStore } from "@shared/toasts";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import dayjs from "dayjs";
import { useState } from "react";
import { FaTruckLoading } from "react-icons/fa";
import { FaCheck, FaTrash } from "react-icons/fa6";
import { NavLink } from "react-router-dom";
import type { CreateDeliveryPayload, Delivery } from "../../deliveries/model/types";

interface DeliveryRow {
  key: string;
  ingredientId: string;
  quantity: string;
  expiresOn: string;
}

const INVALIDATE_KEYS = ["deliveries", "stock", "expiring-batches", "stop-list", "hot-offers", "analytics"];

const newRow = (): DeliveryRow => ({
  key: randomId(),
  ingredientId: "",
  quantity: "",
  expiresOn: dayjs().add(7, "day").format("YYYY-MM-DD"),
});

const nowLocal = () => dayjs().format("YYYY-MM-DDTHH:mm");

const defaultNumber = () => `DLV-${dayjs().format("YYYYMMDD-HHmm")}`;

const endOfDayIso = (date: string) => dayjs(date).endOf("day").toISOString();

export const CreateDeliveryPage = () => {
  const [deliveryNumber, setDeliveryNumber] = useState(defaultNumber);
  const [receivedAt, setReceivedAt] = useState(nowLocal);
  const [rows, setRows] = useState<DeliveryRow[]>(() => [newRow()]);
  const [formError, setFormError] = useState<string | null>(null);
  const [idempotencyKey, setIdempotencyKey] = useState(() => randomId());
  const [lastDelivery, setLastDelivery] = useState<Delivery | null>(null);

  const queryClient = useQueryClient();
  const newToast = useToastStore((s) => s.newToast);
  const { data: ingredients, isPending: ingredientsPending } = useIngredientsCatalog();

  const ingredientOf = (id: string) => ingredients?.find((i) => i.id === id);

  const resetForm = () => {
    setDeliveryNumber(defaultNumber());
    setReceivedAt(nowLocal());
    setRows([newRow()]);
    setFormError(null);
    setIdempotencyKey(randomId());
  };

  const createMutation = useMutation({
    mutationFn: async (payload: CreateDeliveryPayload) =>
      (await api.post<Delivery>("/deliveries", payload, {
        headers: { "Idempotency-Key": idempotencyKey },
      })).data,
    onSuccess: (delivery) => {
      INVALIDATE_KEYS.forEach((key) => queryClient.invalidateQueries({ queryKey: [key] }));
      newToast(`Поставка ${delivery.deliveryNumber} принята`, "success");
      setLastDelivery(delivery);
      resetForm();
    },
    onError: (err) => {
      setFormError(apiErrorMessage(err, "Не удалось принять поставку"));
      setIdempotencyKey(randomId());
    },
  });

  const updateRow = (key: string, patch: Partial<DeliveryRow>) => {
    setFormError(null);
    setRows((prev) => prev.map((r) => (r.key === key ? { ...r, ...patch } : r)));
  };

  const validate = (): string | null => {
    if (!deliveryNumber.trim()) return "Укажите номер поставки";
    if (!receivedAt) return "Укажите дату приёмки";
    if (dayjs(receivedAt).isAfter(dayjs().add(1, "minute"))) return "Дата приёмки не может быть в будущем";

    const seen = new Set<string>();
    for (const row of rows) {
      if (!row.ingredientId) return "Выберите ингредиент во всех строках";
      const qty = Number(row.quantity);
      if (!row.quantity || Number.isNaN(qty) || qty <= 0) return "Количество должно быть больше нуля";
      if (!row.expiresOn) return "Укажите срок годности во всех строках";
      if (!dayjs(row.expiresOn).endOf("day").isAfter(dayjs())) return "Срок годности уже истёк";
      const key = `${row.ingredientId}:${row.expiresOn}`;
      if (seen.has(key)) return "Одна и та же партия указана дважды - объедините строки";
      seen.add(key);
    }
    return null;
  };

  const handleSubmit = (e: React.SubmitEvent) => {
    e.preventDefault();
    const error = validate();
    if (error) {
      setFormError(error);
      return;
    }

    createMutation.mutate({
      deliveryNumber: deliveryNumber.trim(),
      receivedAt: dayjs(receivedAt).toISOString(),
      items: rows.map((r) => ({
        ingredientId: r.ingredientId,
        quantity: Number(r.quantity),
        expiresAt: endOfDayIso(r.expiresOn),
      })),
    });
  };

  return (
    <div className="flex h-full flex-col min-h-0">
      <h1 className="bg-accent p-2 text-2xl font-oswald shrink-0 flex gap-2 items-center justify-between">
        <span>Новая поставка</span>
        <FaTruckLoading />
      </h1>

      <form onSubmit={handleSubmit} className="flex flex-col gap-4 p-2 flex-1 min-h-0 overflow-auto">
        <div className="flex flex-col sm:flex-row gap-4 shrink-0">
          <label className="flex flex-1 flex-col gap-1 text-sm">
            <span className="text-primary">Номер поставки</span>
            <input
              type="text"
              maxLength={100}
              value={deliveryNumber}
              onChange={(e) => setDeliveryNumber(e.target.value)}
              className="rounded-xl border border-primary bg-bg px-2 py-2"
            />
          </label>
          <label className="flex flex-1 flex-col gap-1 text-sm">
            <span className="text-primary">Принята</span>
            <input
              type="datetime-local"
              value={receivedAt}
              max={nowLocal()}
              onChange={(e) => setReceivedAt(e.target.value)}
              className="rounded-xl border border-primary bg-bg px-2 py-2 cursor-pointer"
            />
          </label>
        </div>

        <fieldset className="flex flex-col gap-2">
          <legend className="mb-1 text-sm">Партии</legend>

          <div className="hidden md:grid grid-cols-[1fr_9rem_10rem_2rem] gap-2 px-1 text-xs text-primary">
            <span>Ингредиент</span>
            <span>Количество</span>
            <span>Годен до</span>
            <span />
          </div>

          {rows.map((row) => {
            const unit = ingredientOf(row.ingredientId)?.unit;
            return (
              <div
                key={row.key}
                className="grid grid-cols-[1fr_auto] md:grid-cols-[1fr_9rem_10rem_2rem] items-center gap-2 rounded-xl border border-primary/40 md:border-0 p-2 md:p-0"
              >
                <select
                  value={row.ingredientId}
                  onChange={(e) => updateRow(row.key, { ingredientId: e.target.value })}
                  disabled={ingredientsPending}
                  className="col-span-2 md:col-span-1 min-w-0 cursor-pointer rounded-xl border border-primary bg-bg px-2 py-2 text-sm"
                >
                  <option value="">{ingredientsPending ? "Загрузка..." : "- ингредиент -"}</option>
                  {ingredients?.map((i) => (
                    <option key={i.id} value={i.id}>
                      {i.name}
                    </option>
                  ))}
                </select>

                <div className="flex items-center gap-2">
                  <input
                    type="number"
                    min={0}
                    step="any"
                    value={row.quantity}
                    onChange={(e) => updateRow(row.key, { quantity: e.target.value })}
                    placeholder="0"
                    className="w-full min-w-0 rounded-xl border border-primary bg-bg px-2 py-2 text-sm tabular-nums"
                  />
                  <span className="w-6 shrink-0 text-sm text-primary">{unit ? units[unit] : "-"}</span>
                </div>

                <input
                  type="date"
                  value={row.expiresOn}
                  min={dayjs().format("YYYY-MM-DD")}
                  onChange={(e) => updateRow(row.key, { expiresOn: e.target.value })}
                  className="rounded-xl border border-primary bg-bg px-2 py-2 text-sm cursor-pointer"
                />

                <button
                  type="button"
                  onClick={() => setRows((prev) => prev.filter((r) => r.key !== row.key))}
                  disabled={rows.length === 1}
                  className="grid size-8 shrink-0 place-items-center rounded-lg border border-primary transition-colors hover:bg-err-bg hover:text-err disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-inherit"
                >
                  <FaTrash size={12} />
                </button>
              </div>
            );
          })}

          <button
            type="button"
            onClick={() => setRows((prev) => [...prev, newRow()])}
            className="w-fit rounded-xl border border-primary px-2 py-1 text-sm transition-colors hover:bg-secondary"
          >
            + добавить партию
          </button>
        </fieldset>

        {formError && (
          <p className="rounded-xl bg-err-bg p-2 text-sm text-err shrink-0">{formError}</p>
        )}

        <div className="flex items-center justify-between gap-2 shrink-0">
          <span className="text-sm text-primary tabular-nums">Партий: {rows.length}</span>
          <div className="flex gap-2">
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
              {createMutation.isPending ? "Отправка..." : "Принять поставку"}
            </button>
          </div>
        </div>

        {lastDelivery && (
          <div className="flex flex-col gap-2 rounded-xl border border-primary p-2 text-sm shrink-0">
            <div className="flex items-center justify-between gap-2 flex-wrap">
              <span className="flex items-center gap-2 bg-success-bg text-success px-2 py-0.5 rounded-full">
                <FaCheck size={10} />
                Принята {lastDelivery.deliveryNumber}
              </span>
              <span className="text-primary">{formatDateTime(lastDelivery.receivedAt)}</span>
            </div>
            <ul className="flex flex-col gap-1">
              {lastDelivery.items.map((item) => {
                const ingredient = ingredientOf(item.ingredientId);
                return (
                  <li key={item.batchId} className="flex justify-between gap-4">
                    <span className="truncate">{ingredient?.name ?? "Ингредиент"}</span>
                    <span className="tabular-nums shrink-0">
                      {item.receivedQuantity} {ingredient ? units[ingredient.unit] : ""}
                    </span>
                  </li>
                );
              })}
            </ul>
            <NavLink to="/dashboard/deliveries" className="text-primary hover:text-text w-fit">
              Все поставки →
            </NavLink>
          </div>
        )}
      </form>
    </div>
  );
};
