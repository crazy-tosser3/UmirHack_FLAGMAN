import api from "@lib/api";
import type { ApiErrorBody } from "@lib/types";
import { apiErrorCode, apiErrorMessage, cn, formatDateTime, isStaff, randomId, units } from "@lib/utils";
import { useAuth } from "@shared/auth/auth-service";
import type { Dish } from "@shared/table";
import { useToastStore } from "@shared/toasts";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { useState } from "react";
import { FaCartPlus } from "react-icons/fa";
import { FaCheck, FaMinus, FaPlus, FaSliders, FaTrash } from "react-icons/fa6";
import { NavLink } from "react-router-dom";
import type {
  CatalogIngredient,
  CreateOrderPayload,
  InsufficientStockDetail,
  Order,
} from "../../orders/model/types";
import { DishMenu } from "./DishMenu";

interface CartLine {
  key: string;
  dish: Dish;
  quantity: number;
  excluded: string[];
}

const MAX_QUANTITY = 1000;
const MAX_EXCLUDED = 50;

const lineKey = (dishId: string, excluded: string[]) => `${dishId}|${[...excluded].sort().join(",")}`;

const mergeLine = (lines: CartLine[], line: CartLine): CartLine[] => {
  const existing = lines.find((l) => l.key === line.key);
  if (!existing) return [...lines, line];
  return lines.map((l) =>
    l.key === line.key ? { ...l, quantity: Math.min(MAX_QUANTITY, l.quantity + line.quantity) } : l,
  );
};
const INVALIDATE_KEYS = ["orders", "stock", "stop-list", "hot-offers", "expiring-batches", "analytics"];

const shortageDetails = (err: unknown): InsufficientStockDetail[] => {
  if (apiErrorCode(err) !== "INSUFFICIENT_STOCK" || !isAxiosError<ApiErrorBody>(err)) return [];
  const details = err.response?.data?.error?.details;
  return Array.isArray(details) ? (details as InsufficientStockDetail[]) : [];
};

export const CreateOrderPage = () => {
  const { user } = useAuth();
  const [lines, setLines] = useState<CartLine[]>([]);
  const [externalId, setExternalId] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [shortage, setShortage] = useState<InsufficientStockDetail[]>([]);
  const [idempotencyKey, setIdempotencyKey] = useState(() => randomId());
  const [lastOrder, setLastOrder] = useState<Order | null>(null);
  const [editingKey, setEditingKey] = useState<string | null>(null);

  const queryClient = useQueryClient();
  const newToast = useToastStore((s) => s.newToast);

  const { data: ingredients } = useQuery<{ items: CatalogIngredient[] }>({
    queryKey: ["ingredients", "catalog"],
    queryFn: async () =>
      (await api.get<{ items: CatalogIngredient[] }>("/ingredients", { params: { limit: 100 } })).data,
    enabled: shortage.length > 0,
    staleTime: 5 * 60 * 1000,
  });

  const ingredientName = (id: string) =>
    ingredients?.items.find((i) => i.id === id)?.name ?? "Ингредиент";

  const clearErrors = () => {
    setFormError(null);
    setShortage([]);
  };

  const resetCart = () => {
    setLines([]);
    setExternalId("");
    clearErrors();
    setEditingKey(null);
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
      resetCart();
    },
    onError: (err) => {
      setShortage(shortageDetails(err));
      setFormError(apiErrorMessage(err, "Не удалось принять заказ"));
      if (apiErrorCode(err) !== undefined) setIdempotencyKey(randomId());
    },
  });

  const addDish = (dish: Dish, excluded: string[]) => {
    clearErrors();
    setLines((prev) => mergeLine(prev, { key: lineKey(dish.id, excluded), dish, quantity: 1, excluded }));
  };

  const setQuantity = (key: string, quantity: number) => {
    clearErrors();
    setLines((prev) =>
      quantity <= 0
        ? prev.filter((l) => l.key !== key)
        : prev.map((l) => (l.key === key ? { ...l, quantity: Math.min(MAX_QUANTITY, quantity) } : l)),
    );
  };

  const toggleExcluded = (line: CartLine, ingredientId: string) => {
    const isExcluded = line.excluded.includes(ingredientId);
    const excluded = isExcluded
      ? line.excluded.filter((id) => id !== ingredientId)
      : [...line.excluded, ingredientId];

    if (!isExcluded && excluded.length >= line.dish.ingredients.length) {
      newToast("Нельзя убрать из блюда все ингредиенты", "error");
      return;
    }
    if (excluded.length > MAX_EXCLUDED) {
      newToast(`Можно исключить не больше ${MAX_EXCLUDED} ингредиентов`, "error");
      return;
    }

    clearErrors();
    const key = lineKey(line.dish.id, excluded);
    setLines((prev) => mergeLine(prev.filter((l) => l.key !== line.key), { ...line, key, excluded }));
    setEditingKey(key);
  };

  const ingredientNameIn = (dish: Dish, id: string) =>
    dish.ingredients.find((i) => i.ingredientId === id)?.ingredientName ?? "ингредиент";

  const cart = lines.reduce<Record<string, number>>((acc, l) => {
    acc[l.dish.id] = (acc[l.dish.id] ?? 0) + l.quantity;
    return acc;
  }, {});
  const totalPortions = lines.reduce((sum, l) => sum + l.quantity, 0);

  const handleSubmit = (e: React.SubmitEvent) => {
    e.preventDefault();
    clearErrors();
    if (lines.length === 0) {
      setFormError("Добавьте хотя бы одно блюдо");
      return;
    }

    createMutation.mutate({
      externalOrderId: externalId.trim() || undefined,
      items: lines.map((l) => ({
        dishId: l.dish.id,
        quantity: l.quantity,
        ...(l.excluded.length ? { excludedIngredientIds: l.excluded } : {}),
      })),
    });
  };

  return (
    <div className="flex h-full flex-col min-h-0">
      <h1 className="bg-accent p-2 text-2xl font-oswald shrink-0 flex gap-2 items-center justify-between">
        <span>Новый заказ</span>
        <FaCartPlus />
      </h1>

      <div className="flex flex-col md:flex-row gap-4 p-2 flex-1 min-h-0 overflow-auto">
        <section className="md:flex-3 min-h-72 md:min-h-0 flex flex-col">
          <DishMenu cart={cart} onAdd={addDish} />
        </section>

        <form
          onSubmit={handleSubmit}
          className="md:flex-2 flex flex-col gap-3 min-h-0 md:border-l md:border-primary md:pl-4"
        >
          <div className="flex items-center justify-between gap-2 shrink-0">
            <h2 className="font-manrope text-lg">Состав заказа</h2>
            {lines.length > 0 && (
              <button
                type="button"
                onClick={resetCart}
                className="text-sm text-primary hover:text-err transition-colors"
              >
                Очистить
              </button>
            )}
          </div>

          <div className="flex-1 min-h-24 overflow-auto rounded-xl border border-primary">
            {lines.length === 0 ? (
              <p className="h-full grid place-items-center p-6 text-center text-sm text-primary">
                Выберите блюда из меню слева
              </p>
            ) : (
              <ul className="divide-y divide-primary/40">
                {lines.map((line) => {
                  const { key, dish, quantity, excluded } = line;
                  const editing = editingKey === key;
                  return (
                    <li key={key} className="flex flex-col gap-2 p-2">
                      <div className="flex items-center gap-2">
                        <span className="flex-1 min-w-0">
                          <span className="block truncate">{dish.name}</span>
                          {excluded.length > 0 && (
                            <span className="block text-xs text-warn truncate">
                              без: {excluded.map((id) => ingredientNameIn(dish, id).toLowerCase()).join(", ")}
                            </span>
                          )}
                        </span>
                        <div className="flex items-center gap-1 shrink-0">
                          <button
                            type="button"
                            title="Изменить состав"
                            onClick={() => setEditingKey(editing ? null : key)}
                            className={cn(
                              "grid size-7 place-items-center rounded-lg border border-primary transition-colors hover:bg-secondary",
                              (editing || excluded.length > 0) && "bg-accent/30",
                            )}
                          >
                            <FaSliders size={10} />
                          </button>
                          <button
                            type="button"
                            onClick={() => setQuantity(key, quantity - 1)}
                            className="grid size-7 place-items-center rounded-lg border border-primary transition-colors hover:bg-secondary"
                          >
                            <FaMinus size={10} />
                          </button>
                          <input
                            type="number"
                            min={1}
                            max={MAX_QUANTITY}
                            value={quantity}
                            onChange={(e) => setQuantity(key, Math.floor(Number(e.target.value)) || 1)}
                            className="w-12 rounded-lg border border-primary bg-bg px-1 py-0.5 text-center text-sm tabular-nums"
                          />
                          <button
                            type="button"
                            onClick={() => setQuantity(key, quantity + 1)}
                            disabled={quantity >= MAX_QUANTITY}
                            className="grid size-7 place-items-center rounded-lg border border-primary transition-colors hover:bg-secondary disabled:opacity-40"
                          >
                            <FaPlus size={10} />
                          </button>
                          <button
                            type="button"
                            onClick={() => setQuantity(key, 0)}
                            className="grid size-7 place-items-center rounded-lg border border-primary transition-colors hover:bg-err-bg hover:text-err"
                          >
                            <FaTrash size={10} />
                          </button>
                        </div>
                      </div>

                      {editing && (
                        <div className="flex flex-col gap-1">
                          <span className="text-xs text-primary">Нажмите на ингредиент, чтобы убрать его из блюда</span>
                          <div className="flex flex-wrap gap-1">
                            {dish.ingredients.map((ing) => {
                              const off = excluded.includes(ing.ingredientId);
                              return (
                                <button
                                  key={ing.ingredientId}
                                  type="button"
                                  onClick={() => toggleExcluded(line, ing.ingredientId)}
                                  className={cn(
                                    "rounded-full border px-2 py-0.5 text-xs transition-colors",
                                    off
                                      ? "border-warn bg-warn-bg text-warn line-through"
                                      : "border-primary hover:bg-secondary",
                                  )}
                                >
                                  {ing.ingredientName}
                                </button>
                              );
                            })}
                          </div>
                        </div>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
          </div>

          <label className="flex flex-col gap-1 text-sm shrink-0">
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

          {formError && (
            <div className="flex flex-col gap-2 rounded-xl bg-err-bg p-2 text-sm text-err shrink-0">
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

          <div className="flex items-center justify-between gap-2 shrink-0">
            <span className="text-sm text-primary tabular-nums">
              Позиций: {lines.length} | порций: {totalPortions}
            </span>
            <button
              type="submit"
              disabled={createMutation.isPending || lines.length === 0}
              className="rounded-xl bg-accent px-4 py-2 text-sm transition-colors hover:bg-accent/70 disabled:opacity-60"
            >
              {createMutation.isPending ? "Отправка..." : "Принять заказ"}
            </button>
          </div>

          {lastOrder && (
            <div className="flex flex-col gap-2 rounded-xl border border-primary p-2 text-sm shrink-0">
              <div className="flex items-center justify-between gap-2">
                <span className="flex items-center gap-2 bg-success-bg text-success px-2 py-0.5 rounded-full">
                  <FaCheck size={10} />
                  Принят {lastOrder.externalOrderId ?? lastOrder.id.slice(0, 8)}
                </span>
                <span className="text-primary">{formatDateTime(lastOrder.createdAt)}</span>
              </div>
              <ul className="flex flex-col gap-1">
                {lastOrder.items.map((item, index) => (
                  <li key={`${item.dishId}-${index}`} className="flex justify-between gap-4">
                    <span className="min-w-0">
                      <span className="block truncate">{item.dishName}</span>
                      {item.excludedIngredients && item.excludedIngredients.length > 0 && (
                        <span className="block text-xs text-warn truncate">
                          без: {item.excludedIngredients.map((e) => e.ingredientName.toLowerCase()).join(", ")}
                        </span>
                      )}
                    </span>
                    <span className="tabular-nums shrink-0">× {item.quantity}</span>
                  </li>
                ))}
              </ul>
              {user && isStaff(user.role) && (
                <NavLink to="/dashboard/orders" className="text-primary hover:text-text w-fit">
                  Все заказы →
                </NavLink>
              )}
            </div>
          )}
        </form>
      </div>
    </div>
  );
};
