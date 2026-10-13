import api from "@lib/api";
import { useDelayedValue } from "@shared/hooks/use-delayed-value";
import { Table } from "@shared/table";
import type { Cell, ColumnConfig } from "@shared/table/model/types";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { FaChevronLeft, FaChevronRight, FaCircleNotch } from "react-icons/fa";
import type { OrderListItem, OrdersPage, OrderStatus } from "../model/types";

const COLUMNS: ColumnConfig[] = [
  { label: "Заказ", align: "left" },
  { label: "Статус", align: "center" },
  { label: "Позиций", align: "right" },
  { label: "Принят", align: "right" },
];

const LIMIT = 20;

const STATUS_LABELS: Record<OrderStatus, string> = {
  confirmed: "Принят",
  cancelled: "Отменен",
};

const transformOrder = (order: OrderListItem): Cell[] => [
  { body: order.externalOrderId ?? order.id.slice(0, 8), align: "left" },
  {
    body: STATUS_LABELS[order.status] ?? order.status,
    status: order.status === "confirmed" ? "high" : "low",
    align: "center",
  },
  { body: order.itemsCount, align: "right" },
  {
    body: new Date(order.createdAt).toLocaleString("ru-RU", {
      day: "numeric",
      month: "short",
      hour: "2-digit",
      minute: "2-digit",
    }),
    align: "right",
  },
];

export const OrdersList = () => {
  const [page, setPage] = useState(1);
  const [status, setStatus] = useState<OrderStatus | "">("");

  const [prevStatus, setPrevStatus] = useState(status);
  if (prevStatus !== status) {
    setPrevStatus(status);
    if (page !== 1) setPage(1);
  }

  const { data, isPending, isError, isFetching } = useQuery<OrdersPage>({
    queryKey: ["orders", page, status],
    queryFn: async () =>
      (await api.get<OrdersPage>("/orders", {
        params: { page, limit: LIMIT, status: status || undefined },
      })).data,
    placeholderData: (prev) => prev,
  });

  const rows = (data?.items ?? []).map(transformOrder);
  const totalPages = data ? Math.max(1, Math.ceil(data.total / LIMIT)) : 1;
  const safePage = Math.min(page, totalPages);
  const showLoader = useDelayedValue(isPending && !data, { minDisplay: 300 });
  const showOverlay = isFetching && Boolean(data);

  return (
    <div className="flex flex-col gap-2 h-full min-h-0">
      <div className="flex items-center justify-between gap-2 shrink-0">
        <h2 className="font-manrope text-lg">Последние заказы</h2>
        <select
          value={status}
          onChange={(e) => setStatus(e.target.value as OrderStatus | "")}
          className="rounded-xl border border-primary bg-bg px-3 py-1 text-sm cursor-pointer outline-none"
        >
          <option value="">Все</option>
          <option value="confirmed">Принятые</option>
          <option value="cancelled">Отмененные</option>
        </select>
      </div>

      <div className="relative flex-1 min-h-48 overflow-hidden">
        {showLoader ? (
          <div className="h-full grid place-items-center">
            <FaCircleNotch size="20" className="animate-spin" />
          </div>
        ) : isError ? (
          <p className="text-err bg-err-bg p-2 rounded-xl text-center">
            Не удалось загрузить заказы
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

      <div className="flex items-center justify-between gap-3 shrink-0 text-sm">
        <span className="text-primary tabular-nums">
          {data && data.total > 0 ? (
            <>Всего {data.total} | стр. {safePage} из {totalPages}</>
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
        </div>
      </div>
    </div>
  );
};
