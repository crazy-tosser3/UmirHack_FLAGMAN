import api from "@lib/api";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { FaChevronLeft, FaChevronRight, FaCircleNotch, FaSearch } from "react-icons/fa";
import { Table } from "@shared/table";
import type {
  Cell,
  ColumnConfig,
  Dish,
  DishesPage,
  SortDirection,
} from "@shared/table/model/types";
import { useDebounce } from "@shared/hooks/use-debounce";
import { useDelayedValue } from "@shared/hooks/use-delayed-value";
import { units } from "@lib/utils";
import { FaPlateWheat } from "react-icons/fa6";
import { CreateDishDialog } from "@widgets/create-dish-dialog";
import { useAuth } from "@shared/auth/auth-service";

const COLUMNS: ColumnConfig[] = [
  { label: "Название",     sortable: true, sortKey: "name",        align: "left" },
  { label: "Статус",       sortable: true, sortKey: "isActive",    align: "center" },
  { label: "Ингредиентов", sortable: true, sortKey: "ingredients", align: "center" },
  { label: "Состав",       sortable: false,                        align: "left" },
];

const LIMIT_OPTIONS = [20, 50, 100];

const transformDish = (dish: Dish): Cell[] => [
  { body: dish.name, align: "left" },
  {
    body: dish.isActive ? "Активно" : "Неактивно",
    status: dish.isActive ? "high" : "low",
    align: "center",
  },
  { body: dish.ingredients.length, align: "center" },
  {
    body: dish.ingredients
      .map((i) => `${i.ingredientName} (${i.quantity} ${units[i.unit]})`)
      .join(", "),
    align: "left",
  },
];

export const DishesTable = () => {
  const { user } = useAuth();

  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(20);
  const [searchInput, setSearchInput] = useState("");
  const [activeFilter, setActiveFilter] = useState<"all" | "true" | "false">("all");
  const [sort, setSort] = useState<{ key: string; direction: SortDirection } | null>(null);

  const search = useDebounce(searchInput, 300);

  const [prevFilters, setPrevFilters] = useState({ search, activeFilter, limit });
  if (
    prevFilters.search !== search ||
    prevFilters.activeFilter !== activeFilter ||
    prevFilters.limit !== limit
  ) {
    setPrevFilters({ search, activeFilter, limit });
    if (page !== 1) setPage(1);
  }

  const { data, isPending, isError } = useQuery<DishesPage>({
    queryKey: ["dishes", page, limit, search, activeFilter],
    queryFn: async () => {
      const params = new URLSearchParams({
        page: String(page),
        limit: String(limit),
      });
      if (search) params.set("search", search);
      if (activeFilter !== "all") params.set("active", activeFilter);

      const res = await api.get<DishesPage>("/dishes", { params });
      return res.data;
    },
    placeholderData: (prev) => prev,
  });

  const sortedItems = (() => {
    const items = data?.items ?? [];
    if (!sort) return items;

    const sorted = [...items].sort((a, b) => {
      let cmp = 0;
      switch (sort.key) {
        case "name":
          cmp = a.name.localeCompare(b.name, "ru");
          break;
        case "isActive":
          cmp = Number(a.isActive) - Number(b.isActive);
          break;
        case "ingredients":
          cmp = a.ingredients.length - b.ingredients.length;
          break;
      }
      return sort.direction === "asc" ? cmp : -cmp;
    });
    return sorted;
  })();

  const rows: Cell[][] = sortedItems.map(transformDish);

  const handleSort = (key: string) => {
    setSort((prev) => {
      if (prev?.key !== key) return { key, direction: "asc" };
      if (prev.direction === "asc") return { key, direction: "desc" };
      return null;
    });
  };

  const totalPages = data ? Math.max(1, Math.ceil(data.total / limit)) : 1;
  const safePage = Math.min(page, totalPages);
  const showLoader = useDelayedValue(isPending && !data, { minDisplay: 300 });
  const showOverlay = isPending && Boolean(data);

  return (
    <div className="flex h-full flex-col min-h-0">
      <h1 className="bg-accent p-2 text-2xl font-oswald shrink-0 flex gap-2 items-center justify-between">
        <span>Блюда</span>
        <FaPlateWheat />
      </h1>
      <div className="flex flex-wrap items-center gap-2 shrink-0 p-2">
        <div className="relative flex-1 min-w-50">
          <FaSearch
            size={12}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-primary"
          />
          <input
            type="text"
            placeholder="Поиск по названию"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="w-full rounded-xl border border-primary bg-bg py-2 pl-9 pr-3 text-sm outline-none focus:border-primary transition-colors"
          />
        </div>

        <select
          value={activeFilter}
          onChange={(e) => setActiveFilter(e.target.value as typeof activeFilter)}
          className="rounded-xl border border-primary bg-bg px-3 py-2 text-sm cursor-pointer outline-none focus:border-primary"
        >
          <option value="all">Все блюда</option>
          <option value="true">Только активные</option>
          <option value="false">Неактивные</option>
        </select>

        <select
          value={limit}
          onChange={(e) => setLimit(Number(e.target.value))}
          className="rounded-xl border border-primary bg-bg px-3 py-2 text-sm cursor-pointer outline-none focus:border-primary"
        >
          {LIMIT_OPTIONS.map((n) => (
            <option key={n} value={n}>
              {n} на странице
            </option>
          ))}
        </select>
      </div>

      <div className="p-2 relative flex-1 min-h-0 overflow-hidden">
        {showLoader ? (
          <div className="absolute inset-0 z-10 grid place-items-center">
            <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
          </div>
        ) : isError ? (
          <p className=" text-err bg-err-bg p-2 rounded-xl text-center">
            Не удалось загрузить данные
          </p>
        ) : (
          <Table
            columns={COLUMNS}
            data={rows}
            sort={sort ?? undefined}
            onSort={handleSort}
          />
        )}

        {showOverlay && (
          <div className="absolute inset-0 z-10 grid place-items-center bg-bg/60 pointer-events-none">
            <FaCircleNotch size="20" className="animate-spin text-primary" />
          </div>
        )}
      </div>

      <div className="flex items-center justify-between gap-3 shrink-0 p-2 text-sm">
        <span className="text-primary tabular-nums">
          {data && data.total > 0 ? (
            <>
              Показано {rows.length} из {data.total} | стр. {safePage} из {totalPages}
              {sort && (
                <span className="ml-2 text-xs text-primary/60">
                  (отсортировано по текущей странице)
                </span>
              )}
            </>
          ) : (
            <span className="text-primary/60">Нет данных</span>
          )}
        </span>

        <div className="flex items-center gap-2">
          <button
            type="button"
            disabled={!data || safePage === 1}
            onClick={() => setPage((p) => Math.max(1, p - 1))}
            className="grid size-8 place-items-center rounded-lg border border-primary/20 transition-colors hover:bg-secondary disabled:opacity-40 disabled:hover:bg-transparent"
          >
            <FaChevronLeft size={12} />
          </button>

          <button
            type="button"
            disabled={!data || safePage === totalPages}
            onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
            className="grid size-8 place-items-center rounded-lg border border-primary/20 transition-colors hover:bg-secondary disabled:opacity-40 disabled:hover:bg-transparent"
          >
            <FaChevronRight size={12} />
          </button>

          {user?.role !== "employee" && <CreateDishDialog />}
        </div>
      </div>
    </div>
  );
};