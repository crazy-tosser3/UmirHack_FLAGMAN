import api from "@lib/api";
import type { UserRole, UserStatus } from "@lib/types";
import { roleLabels, statusLabels, typedEntries } from "@lib/utils";
import { useAuth } from "@shared/auth/auth-service";
import { useDebounce } from "@shared/hooks/use-debounce";
import { useDelayedValue } from "@shared/hooks/use-delayed-value";
import { Table } from "@shared/table";
import type { Cell, CellStatus, ColumnConfig } from "@shared/table/model/types";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { FaChevronLeft, FaChevronRight, FaCircleNotch, FaSearch, FaTelegram, FaUserShield } from "react-icons/fa";
import type { AdminUser, AdminUsersPage } from "../model/types";
import { InviteDialog } from "./InviteDialog";
import { RoleSelect, StatusToggle } from "./UserActions";

const COLUMNS: ColumnConfig[] = [
  { label: "Имя", align: "left" },
  { label: "Почта", align: "left" },
  { label: "Роль", align: "center" },
  { label: "Статус", align: "center" },
  { label: "Telegram", align: "center" },
  { label: "Создан", align: "center" },
  { label: "", align: "center" },
];

const LIMIT_OPTIONS = [20, 50, 100];

const STATUS_CELL: Record<UserStatus, CellStatus> = {
  active: "high",
  pending: "medium",
  blocked: "low",
};

const transformUser = (user: AdminUser, isSelf: boolean): Cell[] => [
  { body: isSelf ? `${user.name} (вы)` : user.name, align: "left" },
  { body: user.email, align: "left" },
  { body: <RoleSelect user={user} disabled={isSelf} />, align: "center" },
  { body: statusLabels[user.status], status: STATUS_CELL[user.status], align: "center" },
  {
    body: user.telegramLinked ? (
      <span className="inline-flex items-center gap-1 text-primary">
        <FaTelegram />
        {user.telegramUsername ? `@${user.telegramUsername}` : "привязан"}
      </span>
    ) : "—",
    align: "center",
  },
  { body: new Date(user.createdAt).toLocaleDateString("ru-RU"), align: "center" },
  { body: <StatusToggle user={user} disabled={isSelf} />, align: "center" },
];

export const AdminPage = () => {
  const { user: me } = useAuth();
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(20);
  const [searchInput, setSearchInput] = useState("");
  const [role, setRole] = useState<UserRole | "">("");
  const [status, setStatus] = useState<UserStatus | "">("");

  const search = useDebounce(searchInput, 300);

  const [prevFilters, setPrevFilters] = useState({ search, limit, role, status });
  if (
    prevFilters.search !== search ||
    prevFilters.limit !== limit ||
    prevFilters.role !== role ||
    prevFilters.status !== status
  ) {
    setPrevFilters({ search, limit, role, status });
    if (page !== 1) setPage(1);
  }

  const { data, isPending, isError, isFetching } = useQuery<AdminUsersPage>({
    queryKey: ["admin-users", page, limit, search, role, status],
    queryFn: async () => {
      const res = await api.get<AdminUsersPage>("/admin/users", {
        params: {
          page,
          limit,
          search: search || undefined,
          role: role || undefined,
          status: status || undefined,
        },
      });
      return res.data;
    },
    placeholderData: (prev) => prev,
  });

  const rows = (data?.items ?? []).map((u) => transformUser(u, u.id === me?.id));

  const totalPages = data ? Math.max(1, Math.ceil(data.total / limit)) : 1;
  const safePage = Math.min(page, totalPages);
  const showLoader = useDelayedValue(isPending && !data, { minDisplay: 300 });
  const showOverlay = isFetching && Boolean(data);

  return (
    <div className="flex h-full flex-col min-h-0">
      <h1 className="bg-accent p-2 text-2xl font-oswald shrink-0 flex gap-2 items-center justify-between">
        <span>Пользователи</span>
        <FaUserShield />
      </h1>

      <div className="flex flex-wrap items-center gap-2 shrink-0 p-2">
        <div className="relative flex-1 min-w-50">
          <FaSearch
            size={12}
            className="absolute left-3 top-1/2 -translate-y-1/2 text-primary"
          />
          <input
            type="text"
            placeholder="Поиск по имени или почте"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="w-full rounded-xl border border-primary bg-bg py-2 pl-9 pr-3 text-sm outline-none focus:border-primary transition-colors"
          />
        </div>

        <select
          value={role}
          onChange={(e) => setRole(e.target.value as UserRole | "")}
          className="rounded-xl border border-primary bg-bg px-3 py-2 text-sm cursor-pointer outline-none"
        >
          <option value="">Все роли</option>
          {typedEntries(roleLabels).map(([value, label]) => (
            <option key={value} value={value}>{label}</option>
          ))}
        </select>

        <select
          value={status}
          onChange={(e) => setStatus(e.target.value as UserStatus | "")}
          className="rounded-xl border border-primary bg-bg px-3 py-2 text-sm cursor-pointer outline-none"
        >
          <option value="">Все статусы</option>
          {typedEntries(statusLabels).map(([value, label]) => (
            <option key={value} value={value}>{label}</option>
          ))}
        </select>

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
            Не удалось загрузить пользователей
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

          <InviteDialog />
        </div>
      </div>
    </div>
  );
};
