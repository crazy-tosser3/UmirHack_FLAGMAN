import api from "@lib/api";
import { cn } from "@lib/utils";
import { useDebounce } from "@shared/hooks/use-debounce";
import { useDelayedValue } from "@shared/hooks/use-delayed-value";
import type { Dish, DishesPage } from "@shared/table";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { FaCircleNotch, FaSearch } from "react-icons/fa";
import { FaPlus } from "react-icons/fa6";
import type { StopListData } from "../../../ui/StopList/model/types";

interface DishMenuProps {
  cart: Record<string, number>;
  onAdd: (dish: Dish) => void;
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

  const stopped = new Set(
    stopList?.items.filter((d) => !d.isAvailable).map((d) => d.dishId) ?? [],
  );

  const showLoader = useDelayedValue(isPending && !data, { minDisplay: 300 });

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
              const inStop = stopped.has(dish.id);
              const count = cart[dish.id] ?? 0;
              return (
                <li key={dish.id}>
                  <button
                    type="button"
                    disabled={inStop}
                    onClick={() => onAdd(dish)}
                    className={cn(
                      "w-full h-full text-left p-2 rounded-lg border border-primary flex items-center gap-2 transition-colors hover:bg-secondary active:scale-95 disabled:opacity-50 disabled:hover:bg-transparent disabled:active:scale-100 disabled:cursor-not-allowed",
                      count > 0 && "border-accent bg-accent/20",
                    )}
                  >
                    <span className="flex-1 min-w-0">
                      <span className="block font-manrope truncate">{dish.name}</span>
                      <span className="block text-xs text-primary truncate">
                        {dish.ingredients.map((i) => i.ingredientName).join(", ")}
                      </span>
                    </span>
                    {inStop ? (
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
