import api from "@lib/api";
import type { Unit } from "@lib/types";
import { units } from "@lib/utils";
import * as Dialog from "@radix-ui/react-dialog";
import { useToastStore } from "@shared/toasts";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { FaPlus, FaTrash, FaXmark } from "react-icons/fa6";

interface IngredientRow {
  name: string;
  unit: Unit;
}

interface CreateIngredientsPayload {
  items: IngredientRow[];
}

interface CreatedIngredient {
  id: string;
  name: string;
  unit: Unit;
}

interface CreateIngredientsResponse {
  items: CreatedIngredient[];
  page: number;
  limit: number;
  total: number;
}

const EMPTY_ROW: IngredientRow = { name: "", unit: "g" };

export const CreateIngredientsDialog = () => {
  const [open, setOpen] = useState(false);
  const [rows, setRows] = useState<IngredientRow[]>([EMPTY_ROW]);
  const [formError, setFormError] = useState<string | null>(null);

  const newToast = useToastStore(state => state.newToast);

  const queryClient = useQueryClient();

  const resetForm = () => {
    setRows([EMPTY_ROW]);
    setFormError(null);
  };

  const createMutation = useMutation({
    mutationFn: async (payload: CreateIngredientsPayload) => {
      const res = await api.post<CreateIngredientsResponse>("/ingredients", payload);
      return res.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["ingredients"] });
      setOpen(false);
      resetForm();
      newToast("Ингредиенты добавлены!", "success")
    },
    onError: () => {
      setFormError("Не удалось добавить ингредиенты");
    },
  });

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    if (!next) resetForm();
  };

  const updateRow = (index: number, patch: Partial<IngredientRow>) =>
    setRows((prev) => prev.map((r, i) => (i === index ? { ...r, ...patch } : r)));

  const validate = (): string | null => {
    if (rows.length === 0) return "Добавьте хотя бы один ингредиент";

    const seen = new Set<string>();
    for (const row of rows) {
      const name = row.name.trim();
      if (name.length < 2) return "Название каждого ингредиента - минимум 2 символа";

      const key = name.toLowerCase();
      if (seen.has(key)) return `Дубликат в форме: «${name}»`;
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
      items: rows.map((r) => ({ name: r.name.trim(), unit: r.unit })),
    });
  };

  return (
    <Dialog.Root open={open} onOpenChange={handleOpenChange}>
      <Dialog.Trigger asChild>
        <button
          type="button"
          title="Добавить ингредиент"
          className="flex items-center gap-2 p-2 h-8 bg-accent place-items-center rounded-lg transition-colors hover:bg-accent/70"
        >
          <FaPlus size={12} />
          <span>Добавить ингредиенты</span>
        </button>
      </Dialog.Trigger>

      <Dialog.Portal>
        <Dialog.Overlay className="animate-overlay-show fixed inset-0 z-40 bg-primary/20 backdrop-blur-sm" />

        <Dialog.Content className="animate-content-show fixed inset-0 z-50 m-auto h-fit max-h-[85vh] w-[calc(100%-2rem)] max-w-md overflow-auto rounded-xl border border-primary bg-bg p-4 shadow-2xl outline-none">
          <div className="flex items-start justify-between gap-4">
            <div>
              <Dialog.Title className="text-xl font-oswald">Новые ингредиенты</Dialog.Title>
              <Dialog.Description className="mt-1 text-sm text-primary">
                Можно добавить несколько позиций за один раз
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
            <fieldset className="flex flex-col gap-2">
              <legend className="mb-1 text-sm">Название и единица измерения</legend>

              {rows.map((row, index) => (
                <div key={index} className="flex items-center gap-2">
                  <input
                    autoFocus={index === 0}
                    type="text"
                    value={row.name}
                    onChange={(e) => updateRow(index, { name: e.target.value })}
                    placeholder="Моцарелла"
                    className="min-w-0 flex-1 rounded-xl border border-primary bg-bg px-3 py-2 text-sm"
                  />

                  <select
                    value={row.unit}
                    onChange={(e) => updateRow(index, { unit: e.target.value as Unit })}
                    className="w-24 shrink-0 cursor-pointer rounded-xl border border-primary bg-bg px-2 py-2 text-sm"
                  >
                    {(Object.keys(units) as Unit[]).map((u) => (
                      <option key={u} value={u}>
                        {units[u]}
                      </option>
                    ))}
                  </select>

                  <button
                    type="button"
                    onClick={() => setRows((prev) => prev.filter((_, i) => i !== index))}
                    disabled={rows.length === 1}
                    className="grid size-8 shrink-0 place-items-center rounded-lg border border-primary transition-colors hover:bg-err-bg hover:text-err disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-inherit"
                  >
                    <FaTrash size={12} />
                  </button>
                </div>
              ))}

              <button
                type="button"
                onClick={() => setRows((prev) => [...prev, EMPTY_ROW])}
                className="w-fit rounded-xl border border-primary px-2 py-1 text-sm transition-colors hover:bg-secondary"
              >
                + добавить еще
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
                {createMutation.isPending
                  ? "Сохранение..."
                  : rows.length === 1
                    ? "Добавить"
                    : `Добавить (${rows.length})`}
              </button>
            </div>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
};