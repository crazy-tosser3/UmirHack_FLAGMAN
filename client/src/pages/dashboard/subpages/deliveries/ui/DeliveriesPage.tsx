import api from "@lib/api";
import { useDebounce } from "@shared/hooks/use-debounce";
import { useDelayedValue } from "@shared/hooks/use-delayed-value";
import { Table } from "@shared/table";
import type { Cell, ColumnConfig } from "@shared/table/model/types";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { FaChevronLeft, FaChevronRight, FaCircleNotch, FaSearch, FaTruck } from "react-icons/fa";
import { FaEye, FaPlus } from "react-icons/fa6";
import { NavLink } from "react-router-dom";
import type { DeliveriesList, DeliveryListItem } from "../model/types";
import { DeliveryDetailsDialog } from "./DeliveryDetailsDialog";

const COLUMNS: ColumnConfig[] = [
  { label: "Номер", align: "left" },
  { label: "Принята", align: "center" },
  { label: "Статус", align: "center" },
  { label: "Позиций", align: "right" },
  { label: "", align: "center" },
];

const LIMIT_OPTIONS = [20, 50, 100];

const STATUS_LABELS: Record<string, string> = {
  accepted: "Принята",
};

export const DeliveriesPage = () => {
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(20);
  const [searchInput, setSearchInput] = useState("");
  const [openedId, setOpenedId] = useState<string | null>(null);

  const search = useDebounce(searchInput, 300);

  const [prevFilters, setPrevFilters] = useState({ search, limit });
  if (prevFilters.search !== search || prevFilters.limit !== limit) {
    setPrevFilters({ search, limit });
    if (page !== 1) setPage(1);
  }

  const { data, isPending, isError, isFetching } = useQuery<DeliveriesList>({
    queryKey: ["deliveries", page, limit, search],
    queryFn: async () =>
      (await api.get<DeliveriesList>("/deliveries", {
        params: { page, limit, search: search || undefined },
      })).data,
    placeholderData: (prev) => prev,
  });

  const transformDelivery = (delivery: DeliveryListItem): Cell[] => [
    { body: delivery.deliveryNumber, align: "left" },
    {
      body: new Date(delivery.receivedAt).toLocaleString("ru-RU", {
        day: "numeric",
        month: "short",
        hour: "2-digit",
        minute: "2-digit",
      }),
      align: "center",
    },
    {
      body: STATUS_LABELS[delivery.status.toLowerCase()] ?? delivery.status,
      status: "high",
      align: "center",
    },
    { body: delivery.itemsCount, align: "right" },
    {
      body: (
        <button
          type="button"
          title="Состав поставки"
          onClick={() => setOpenedId(delivery.id)}
          className="grid size-8 place-items-center rounded-lg border border-primary transition-colors hover:bg-secondary mx-auto"
        >
          <FaEye size={12} />
        </button>
      ),
      align: "center",
    },
  ];

  const rows = (data?.items ?? []).map(transformDelivery);
  const totalPages = data ? Math.max(1, Math.ceil(data.total / limit)) : 1;
  const safePage = Math.min(page, totalPages);
  const showLoader = useDelayedValue(isPending && !data, { minDisplay: 300 });
  const showOverlay = isFetching && Boolean(data);

  return (
    <div className="flex h-full flex-col min-h-0">
      <h1 className="bg-accent p-2 text-2xl font-oswald shrink-0 flex gap-2 items-center justify-between">
        <span>Поставки</span>
        <FaTruck />
      </h1>

      <div className="flex flex-wrap items-center gap-2 shrink-0 p-2">
        <div className="relative flex-1 min-w-50">
          <FaSearch size={12} className="absolute left-3 top-1/2 -translate-y-1/2 text-primary" />
          <input
            type="text"
            placeholder="Поиск по номеру поставки"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="w-full rounded-xl border border-primary bg-bg py-2 pl-9 pr-3 text-sm outline-none"
          />
        </div>

        <select
          value={limit}
          onChange={(e) => setLimit(Number(e.target.value))}
          className="rounded-xl border border-primary bg-bg px-3 py-2 text-sm cursor-pointer outline-none"
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
          <div className="h-full grid place-items-center">
            <FaCircleNotch size="20" className="animate-spin" />
          </div>
        ) : isError ? (
          <p className="text-err bg-err-bg p-2 rounded-xl text-center">
            Не удалось загрузить поставки
          </p>
        ) : (
          <Table columns={COLUMNS} data={rows} />
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
            <>Показано {rows.length} из {data.total} | стр. {safePage} из {totalPages}</>
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
          <NavLink
            to="/dashboard/deliveries/new"
            title="Новая поставка"
            className="grid bg-accent size-8 place-items-center rounded-lg transition-colors hover:bg-accent/70"
          >
            <FaPlus size={12} />
          </NavLink>
        </div>
      </div>

      <DeliveryDetailsDialog deliveryId={openedId} onClose={() => setOpenedId(null)} />
    </div>
  );
};
