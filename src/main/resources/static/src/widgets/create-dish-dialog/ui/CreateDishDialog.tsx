import api from "@lib/api";
import type { Unit } from "@lib/types";
import { units } from "@lib/utils";
import * as Dialog from "@radix-ui/react-dialog";
import { useToastStore } from "@shared/toasts";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { FaPlus, FaTrash, FaXmark } from "react-icons/fa6";

interface IngredientCatalogItem {
  id: string;
  name: string;
  unit: string;
}

interface IngredientsPage {
  items: IngredientCatalogItem[];
  page: number;
  limit: number;
  total: number;
}

interface CreateDishPayload {
  name: string;
  isActive: boolean;
  ingredients: { ingredientId: string; quantity: number }[];
}

interface IngredientRow {
  ingredientId: string;
  quantity: string;
}

const EMPTY_ROW: IngredientRow = { ingredientId: "", quantity: "" };

export const CreateDishDialog = () => {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState("");
  const [isActive, setIsActive] = useState(true);
  const [rows, setRows] = useState<IngredientRow[]>([EMPTY_ROW]);
  const [formError, setFormError] = useState<string | null>(null);

  const newToast = useToastStore(state => state.newToast);

  const queryClient = useQueryClient();

  const { data: ingredients } = useQuery<IngredientsPage>({
    queryKey: ["ingredients", "catalog"],
    queryFn: async () => {
      const res = await api.get<IngredientsPage>("/ingredients", {
        params: { limit: 100 },
      });
      return res.data;
    },
    enabled: open,
    staleTime: 5 * 60 * 1000,
  });

  const resetForm = () => {
    setName("");
    setIsActive(true);
    setRows([EMPTY_ROW]);
    setFormError(null);
  };

  const createMutation = useMutation({
    mutationFn: async (payload: CreateDishPayload) => {
      const res = await api.post("/dishes", {items: [payload]});
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["dishes"] });
      setOpen(false);
      resetForm();
      newToast("Блюдо добавлено!", "success")
    },
    onError: () => {
      setFormError(
        "Не удалось создать блюдо",
      );
    },
  });

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    if (!next) resetForm();
  };

  const unitOf = (ingredientId: string) =>
    ingredients?.items.find((i) => i.id === ingredientId)?.unit as Unit;

  const updateRow = (index: number, patch: Partial<IngredientRow>) =>
    setRows((prev) => prev.map((r, i) => (i === index ? { ...r, ...patch } : r)));

  const validate = (): string | null => {
    if (name.trim().length < 2) return "Укажите название блюда (минимум 2 символа)";
    if (rows.length === 0) return "Добавьте хотя бы один ингредиент";

    const seen = new Set<string>();
    for (const row of rows) {
      if (!row.ingredientId) return "Выберите ингредиент во всех строках состава";
      if (seen.has(row.ingredientId))
        return "Один ингредиент указан дважды - объедините строки";
      seen.add(row.ingredientId);

      const qty = Number(row.quantity);
      if (!row.quantity || Number.isNaN(qty) || qty <= 0)
        return "Количество должно быть больше нуля во всех строках";
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
      name: name.trim(),
      isActive,
      ingredients: rows.map((r) => ({
        ingredientId: r.ingredientId,
        quantity: Number(r.quantity),
      })),
    });
  };

  return (
    <Dialog.Root open={open} onOpenChange={handleOpenChange}>
      <Dialog.Trigger asChild>
        <button
          type="button"
          title="Добавить блюдо"
          className="grid bg-accent size-8 place-items-center rounded-lg transition-colors hover:bg-accent/70"
        >
          <FaPlus size={12} />
        </button>
      </Dialog.Trigger>

      <Dialog.Portal>
        <Dialog.Overlay className="animate-overlay-show fixed inset-0 z-40 bg-primary/40 backdrop-blur-sm" />

        <Dialog.Content className="animate-content-show fixed inset-0 z-50 m-auto h-fit max-h-[85vh] w-[calc(100%-2rem)] max-w-lg overflow-auto rounded-xl border border-primary bg-bg p-4 shadow-2xl outline-none">
          <div className="flex items-start justify-between gap-4">
            <div>
              <Dialog.Title className="text-xl font-oswald">Новое блюдо</Dialog.Title>
              <Dialog.Description className="mt-1 text-sm text-primary">
                Название, статус и состав из расчета на одну порцию
              </Dialog.Description>
            </div>

            <Dialog.Close asChild>
              <button
                type="button"
                className="grid size-8 shrink-0 place-items-center rounded-lg border border-primary transition-colors hover:bg-secondary"
              >
                <FaXmark size={14} />
              </button>
            </Dialog.Close>
          </div>

          <form onSubmit={handleSubmit} className="mt-4 flex flex-col gap-4">
            <label className="flex flex-col gap-1 text-sm">
              <span className="text-primary">Название</span>
              <input
                autoFocus
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Пицца Маргарита"
                className="rounded-xl border border-primary bg-bg px-2 py-1"
              />
            </label>

            <label className="flex w-fit cursor-pointer items-center gap-2 text-sm">
              <input
                type="checkbox"
                checked={isActive}
                onChange={(e) => setIsActive(e.target.checked)}
                className="size-4 cursor-pointer accent-accent"
              />
              Блюдо активно и доступно для заказа
            </label>

            <fieldset className="flex flex-col gap-2">
              <legend className="mb-1 text-sm">Состав на порцию</legend>

              {rows.map((row, index) => {
                const unit = unitOf(row.ingredientId);
                return (
                  <div key={index} className="flex flex-col sm:flex-row items-center gap-2">
                    <select
                      value={row.ingredientId}
                      onChange={(e) => updateRow(index, { ingredientId: e.target.value })}
                      className="min-w-0 flex-1 cursor-pointer rounded-xl border border-primary bg-bg px-2 py-2 text-sm"
                    >
                      <option value="">- ингредиент -</option>
                      {ingredients?.items.map((i) => (
                        <option key={i.id} value={i.id}>
                          {i.name}
                        </option>
                      ))}
                    </select>
                    <div className="flex  items-center gap-2">
                      <input
                        type="number"
                        min={0}
                        step="any"
                        value={row.quantity}
                        onChange={(e) => updateRow(index, { quantity: e.target.value })}
                        placeholder="0"
                        className="w-24 rounded-xl border border-primary bg-bg px-2 py-2 text-sm tabular-nums"
                      />

                      <span className="w-8 shrink-0 text-sm text-primary">
                        {unit ? units[unit] : "-"}
                      </span>

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
                );
              })}

              <button
                type="button"
                onClick={() => setRows((prev) => [...prev, EMPTY_ROW])}
                className="w-fit rounded-xl border border-primary px-2 py-1 text-sm transition-colors hover:bg-secondary"
              >
                + добавить ингредиент
              </button>
            </fieldset>

            {formError && (
              <p className="rounded-xl bg-err-bg p-2 text-sm text-err">{formError}</p>
            )}

            <div className="flex justify-end gap-2">
              <Dialog.Close asChild>
                <button
                  type="button"
                  className="rounded-xl border border-primary px-4 py-2 text-sm transition-colors hover:bg-secondary"
                >
                  Отмена
                </button>
              </Dialog.Close>

              <button
                type="submit"
                disabled={createMutation.isPending}
                className="rounded-xl bg-accent px-4 py-2 text-sm transition-colors hover:bg-accent/70 disabled:opacity-60"
              >
                {createMutation.isPending ? "Создание..." : "Создать блюдо"}
              </button>
            </div>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
};