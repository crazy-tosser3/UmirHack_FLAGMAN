import api from "@lib/api";
import { cn } from "@lib/utils";
import { useDebounce } from "@shared/hooks/use-debounce";
import { useDelayedValue } from "@shared/hooks/use-delayed-value";
import type { Dish, DishesPage } from "@shared/table";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { FaCircleNotch, FaSearch } from "react-icons/fa";
import { FaPlus } from "react-icons/fa6";
import { isManualStop, type StopListData } from "../../../ui/StopList/model/types";

interface DishAvailability {
  blocked: boolean;
  missing: { id: string; name: string }[];
}

interface DishMenuProps {
  cart: Record<string, number>;
  onAdd: (dish: Dish, excludedIngredientIds: string[]) => void;
}

export const DishMenu = ({ cart, onAdd }: DishMenuProps) => {
  const [searchInput, setSearchInput] = useState("");
  const search = useDebounce(searchInput, 300);

  const { data, isPending, isError } = useQuery<DishesPage>({
    queryKey: ["dishes", "menu", search],
    queryFn: async () =>
      (await api.get<DishesPage>("/dishes", {
        params: { active: true, limit: 100, search: search || undefined },
      })).data,
    placeholderData: (prev) => prev,
  });

  const { data: stopList } = useQuery<StopListData>({
    queryKey: ["stop-list", "all"],
    queryFn: async () => (await api.get<StopListData>("/stop-list", { params: { limit: 100 } })).data,
  });

  const availability = new Map<string, DishAvailability>();
  for (const item of stopList?.items ?? []) {
    if (item.isAvailable && !isManualStop(item)) continue;
    const missing = new Map<string, string>();
    for (const r of item.reasons) {
      if (r.code !== "manual" && r.ingredientId) missing.set(r.ingredientId, r.ingredientName ?? "ингредиент");
    }
    availability.set(item.dishId, {
      blocked: isManualStop(item) || missing.size === 0,
      missing: [...missing].map(([id, name]) => ({ id, name })),
    });
  }

  const showLoader = useDelayedValue(isPending && !data, { minDisplay: 300 });

  const handleAdd = (dish: Dish) => {
    const info = availability.get(dish.id);
    const recipeIds = new Set(dish.ingredients.map((i) => i.ingredientId));
    onAdd(dish, info ? info.missing.map((m) => m.id).filter((id) => recipeIds.has(id)) : []);
  };

  return (
    <div className="flex flex-col gap-2 min-h-0 h-full">
      <div className="relative shrink-0">
        <FaSearch size={12} className="absolute left-3 top-1/2 -translate-y-1/2 text-primary" />
        <input
          type="text"
          placeholder="Поиск блюда"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          className="w-full rounded-xl border border-primary bg-bg py-2 pl-9 pr-3 text-sm outline-none"
        />
      </div>

      <div className="flex-1 min-h-0 overflow-auto">
        {showLoader ? (
          <div className="h-full grid place-items-center">
            <FaCircleNotch size="20" className="animate-spin" />
          </div>
        ) : isError ? (
          <p className="text-err bg-err-bg p-2 rounded-xl text-center">Не удалось загрузить меню</p>
        ) : data && data.items.length === 0 ? (
          <p className="p-8 text-center text-sm text-primary">Ничего не найдено</p>
        ) : (
          <ul className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-2">
            {data?.items.map((dish) => {
              const info = availability.get(dish.id);
              const count = cart[dish.id] ?? 0;
              const partial = info && !info.blocked;
              return (
                <li key={dish.id}>
                  <button
                    type="button"
                    disabled={info?.blocked}
                    title={partial ? `Нет в наличии: ${info.missing.map((m) => m.name).join(", ")}` : undefined}
                    onClick={() => handleAdd(dish)}
                    className={cn(
                      "w-full h-full text-left p-2 rounded-lg border border-primary flex items-center gap-2 transition-colors hover:bg-secondary active:scale-95 disabled:opacity-50 disabled:hover:bg-transparent disabled:active:scale-100 disabled:cursor-not-allowed",
                      partial && "border-warn",
                      count > 0 && "border-accent bg-accent/20",
                    )}
                  >
                    <span className="flex-1 min-w-0">
                      <span className="block font-manrope truncate">{dish.name}</span>
                      {partial ? (
                        <span className="block text-xs text-warn truncate">
                          без: {info.missing.map((m) => m.name.toLowerCase()).join(", ")}
                        </span>
                      ) : (
                        <span className="block text-xs text-primary truncate">
                          {dish.ingredients.map((i) => i.ingredientName).join(", ")}
                        </span>
                      )}
                    </span>
                    {info?.blocked ? (
                      <span className="shrink-0 text-xs bg-err-bg text-err px-2 py-0.5 rounded-full">Стоп</span>
                    ) : count > 0 ? (
                      <span className="shrink-0 text-xs bg-accent px-2 py-0.5 rounded-full tabular-nums">× {count}</span>
                    ) : (
                      <FaPlus size={12} className="shrink-0 text-primary" />
                    )}
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </div>
  );
};
