import api from "@lib/api";
import type { UserRole, UserStatus } from "@lib/types";
import { apiErrorMessage, roleLabels, typedEntries } from "@lib/utils";
import { useToastStore } from "@shared/toasts";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { FaLock, FaLockOpen } from "react-icons/fa6";
import type { AdminUser } from "../model/types";

const useUserMutation = <T,>(user: AdminUser, path: "role" | "status", success: string) => {
  const queryClient = useQueryClient();
  const newToast = useToastStore((s) => s.newToast);

  return useMutation({
    mutationFn: async (value: T) => {
      await api.patch(`/admin/users/${user.id}/${path}`, { [path]: value });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
      newToast(success, "success");
    },
    onError: (err) => newToast(apiErrorMessage(err, "Не удалось изменить пользователя"), "error"),
  });
};

export const RoleSelect = ({ user, disabled }: { user: AdminUser; disabled: boolean }) => {
  const mutation = useUserMutation<UserRole>(user, "role", "Роль изменена");

  return (
    <select
      value={user.role}
      disabled={disabled || mutation.isPending}
      onChange={(e) => mutation.mutate(e.target.value as UserRole)}
      className="rounded-lg border border-primary bg-bg px-2 py-1 text-sm cursor-pointer disabled:cursor-not-allowed disabled:opacity-60"
    >
      {typedEntries(roleLabels).map(([role, label]) => (
        <option key={role} value={role}>{label}</option>
      ))}
    </select>
  );
};

export const StatusToggle = ({ user, disabled }: { user: AdminUser; disabled: boolean }) => {
  const blocked = user.status === "blocked";
  const mutation = useUserMutation<Exclude<UserStatus, "pending">>(
    user,
    "status",
    blocked ? "Пользователь разблокирован" : "Пользователь заблокирован",
  );

  return (
    <button
      type="button"
      title={blocked ? "Разблокировать" : "Заблокировать"}
      disabled={disabled || mutation.isPending}
      onClick={() => mutation.mutate(blocked ? "active" : "blocked")}
      className={
        blocked
          ? "grid size-8 place-items-center rounded-lg border border-primary transition-colors hover:bg-success-bg hover:text-success disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-inherit"
          : "grid size-8 place-items-center rounded-lg border border-primary transition-colors hover:bg-err-bg hover:text-err disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-inherit"
      }
    >
      {blocked ? <FaLockOpen size={12} /> : <FaLock size={12} />}
    </button>
  );
};
