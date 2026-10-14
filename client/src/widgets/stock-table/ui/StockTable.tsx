import api from "@lib/api";
import { units } from "@lib/utils";
import { CreateIngredientsDialog } from "@widgets/create-ingredients-dialog";
import { useDebounce } from "@shared/hooks/use-debounce";
import { useDelayedValue } from "@shared/hooks/use-delayed-value";
import { Table } from "@shared/table";
import type { Cell, CellStatus, ColumnConfig } from "@shared/table/model/types";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { FaBoxes, FaChevronLeft, FaChevronRight, FaCircleNotch, FaSearch } from "react-icons/fa";
import type { StockItem, StockPage } from "../model/types";
import type { Unit } from "@lib/types";

const COLUMNS: ColumnConfig[] = [
  { label: "Название", sortable: true, sortKey: "name", align: "left" },
  { label: "Всего", sortable: true, sortKey: "total", align: "right" },
  { label: "Доступно", sortable: true, sortKey: "available", align: "right" },
  { label: "В резерве", sortable: true, sortKey: "reserved", align: "right" },
  { label: "Просрочено", sortable: true, sortKey: "expired", align: "right" },
  { label: "Ближайший срок", sortable: true, sortKey: "expiry", align: "center" },
];

const LIMIT_OPTIONS = [20, 50, 100];
const DAY_MS = 86_400_000;

const formatQuantity = (value: number, unit: Unit) =>
  `${value.toLocaleString("ru-RU")} ${units[unit] ?? unit}`;

const expiryCell = (iso: string | null): { body: string; status?: CellStatus } => {
  if (!iso) return { body: "—" };

  const days = Math.ceil((Date.parse(iso) - Date.now()) / DAY_MS);
  if (days < 0) return { body: "Просрочено", status: "low" };
  if (days === 0) return { body: "Сегодня", status: "low" };
  if (days <= 3) return { body: `Через ${days} дн.`, status: "medium" };
  return { body: `Через ${days} дн.` };
};

const transformStock = (item: StockItem): Cell[] => {
  const expiry = expiryCell(item.nearestExpiryAt);

  return [
    { body: item.name, align: "left" },
    { body: formatQuantity(item.totalQuantity, item.unit), align: "right" },
    {
      body: formatQuantity(item.availableQuantity, item.unit),
      align: "right",
      status: item.availableQuantity === 0 ? "low" : undefined,
    },
    { body: formatQuantity(item.reservedQuantity, item.unit), align: "right" },
    {
      body: formatQuantity(item.expiredQuantity, item.unit),
      align: "right",
      status: item.expiredQuantity > 0 ? "low" : undefined,
    },
    { body: expiry.body, align: "center", status: expiry.status },
  ];
};

export const StockTable = () => {
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(20);
  const [searchInput, setSearchInput] = useState("");
  const [sort, setSort] = useState<{ key: string; direction: "asc" | "desc" } | null>(null);

  const search = useDebounce(searchInput, 300);

  const [prevFilters, setPrevFilters] = useState({ search, limit });
  if (prevFilters.search !== search || prevFilters.limit !== limit) {
    setPrevFilters({ search, limit });
    if (page !== 1) setPage(1);
  }

  const { data, isPending, isError } = useQuery<StockPage>({
    queryKey: ["stock", page, limit, search],
    queryFn: async () => {
      const params = new URLSearchParams({
        page: String(page),
        limit: String(limit),
      });
      if (search) params.set("search", search);

      const res = await api.get<StockPage>("/stock", { params });
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
        case "total":
          cmp = a.totalQuantity - b.totalQuantity;
          break;
        case "available":
          cmp = a.availableQuantity - b.availableQuantity;
          break;
        case "reserved":
          cmp = a.reservedQuantity - b.reservedQuantity;
          break;
        case "expired":
          cmp = a.expiredQuantity - b.expiredQuantity;
          break;
        case "expiry":
          cmp =
            (a.nearestExpiryAt ? Date.parse(a.nearestExpiryAt) : Infinity) -
            (b.nearestExpiryAt ? Date.parse(b.nearestExpiryAt) : Infinity);
          break;
      }
      return sort.direction === "asc" ? cmp : -cmp;
    });
    return sorted;
  })();

  const rows: Cell[][] = sortedItems.map(transformStock);

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
        <span>Складские остатки</span>
        <FaBoxes />
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
          <div className="flex-1 grid place-items-center">
            <FaCircleNotch size="20" className="animate-spin" />
          </div>
        ) : isError ? (
          <p className="text-err bg-err-bg p-2 rounded-xl text-center">
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
            <FaCircleNotch size={20} className="animate-spin text-primary" />
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

          <CreateIngredientsDialog />
        </div>
      </div>
    </div>
  );
};