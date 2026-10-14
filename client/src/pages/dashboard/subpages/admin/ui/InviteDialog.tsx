import api from "@lib/api";
import type { UserRole } from "@lib/types";
import { apiErrorMessage, copyText, formatDateTime, roleLabels, typedEntries } from "@lib/utils";
import { useToastStore } from "@shared/toasts";
import * as Dialog from "@radix-ui/react-dialog";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { FaCopy, FaUserPlus, FaXmark } from "react-icons/fa6";
import type { CreateInvitePayload, Invite } from "../model/types";

const TTL_OPTIONS = [
  { label: "1 день", hours: 24 },
  { label: "2 дня", hours: 48 },
  { label: "7 дней", hours: 24 * 7 },
  { label: "30 дней", hours: 24 * 30 - 1 },
];

const inviteCode = (url: string) => {
  try {
    return new URL(url).searchParams.get("code");
  } catch {
    return null;
  }
};

export const InviteDialog = () => {
  const [open, setOpen] = useState(false);
  const [role, setRole] = useState<UserRole>("employee");
  const [ttlHours, setTtlHours] = useState(48);
  const [clicks, setClicks] = useState("1");
  const [invite, setInvite] = useState<Invite | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const newToast = useToastStore((s) => s.newToast);

  const resetForm = () => {
    setRole("employee");
    setTtlHours(48);
    setClicks("1");
    setInvite(null);
    setFormError(null);
  };

  const createMutation = useMutation({
    mutationFn: async (payload: CreateInvitePayload) =>
      (await api.post<Invite>("/admin/users/invite", payload)).data,
    onSuccess: (data) => {
      setInvite(data);
      setFormError(null);
    },
    onError: (err) => setFormError(apiErrorMessage(err, "Не удалось создать приглашение")),
  });

  const revokeMutation = useMutation({
    mutationFn: async (code: string) => {
      await api.delete(`/admin/users/invite/${code}`);
    },
    onSuccess: () => {
      newToast("Приглашение отозвано", "success");
      setInvite(null);
    },
    onError: (err) => setFormError(apiErrorMessage(err, "Не удалось отозвать приглашение")),
  });

  const handleOpenChange = (next: boolean) => {
    setOpen(next);
    if (!next) resetForm();
  };

  const handleSubmit = (e: React.SubmitEvent) => {
    e.preventDefault();
    const count = Number(clicks);
    if (clicks.trim() === "" || !Number.isInteger(count) || count < 0 || count > 1000) {
      setFormError("Количество активаций должно быть от 0 до 1000");
      return;
    }

    createMutation.mutate({
      role,
      clicks: count,
      expiresAt: new Date(Date.now() + ttlHours * 3_600_000).toISOString(),
    });
  };

  const code = invite ? inviteCode(invite.inviteUrl) : null;

  return (
    <Dialog.Root open={open} onOpenChange={handleOpenChange}>
      <Dialog.Trigger asChild>
        <button
          type="button"
          title="Пригласить пользователя"
          className="grid bg-accent size-8 place-items-center rounded-lg transition-colors hover:bg-accent/70"
        >
          <FaUserPlus size={12} />
        </button>
      </Dialog.Trigger>

      <Dialog.Portal>
        <Dialog.Overlay className="animate-overlay-show fixed inset-0 z-40 bg-primary/40 backdrop-blur-sm" />

        <Dialog.Content className="animate-content-show fixed inset-0 z-50 m-auto h-fit max-h-[85vh] w-[calc(100%-2rem)] max-w-lg overflow-auto rounded-xl border border-primary bg-bg p-4 shadow-2xl outline-none">
          <div className="flex items-start justify-between gap-4">
            <div>
              <Dialog.Title className="text-xl font-oswald">Приглашение</Dialog.Title>
              <Dialog.Description className="mt-1 text-sm text-primary">
                Ссылка для регистрации нового сотрудника с заданной ролью
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

          {invite ? (
            <div className="mt-4 flex flex-col gap-4">
              <div className="flex items-center gap-2 rounded-xl border border-primary p-2">
                <span className="flex-1 truncate text-sm">{invite.inviteUrl}</span>
                <button
                  type="button"
                  onClick={() => copyText(invite.inviteUrl)}
                  className="grid size-8 shrink-0 place-items-center rounded-lg bg-secondary hover:scale-110 active:scale-90 transition-transform"
                >
                  <FaCopy size={12} />
                </button>
              </div>

              <div className="flex flex-wrap gap-2 text-sm">
                <span className="bg-secondary px-3 py-1 rounded-full">{roleLabels[role]}</span>
                <span className="bg-secondary px-3 py-1 rounded-full tabular-nums">
                  {invite.clicks === 0 ? "Без ограничений" : `Активаций: ${invite.clicks}`}
                </span>
                <span className="bg-warn-bg text-warn px-3 py-1 rounded-full">
                  до {formatDateTime(invite.expiresAt)}
                </span>
              </div>

              {formError && (
                <p className="rounded-xl bg-err-bg p-2 text-sm text-err">{formError}</p>
              )}

              <div className="flex justify-end gap-2">
                {code && (
                  <button
                    type="button"
                    disabled={revokeMutation.isPending}
                    onClick={() => revokeMutation.mutate(code)}
                    className="rounded-xl border border-primary px-4 py-2 text-sm transition-colors hover:bg-err-bg hover:text-err disabled:opacity-60"
                  >
                    Отозвать
                  </button>
                )}
                <button
                  type="button"
                  onClick={resetForm}
                  className="rounded-xl bg-accent px-4 py-2 text-sm transition-colors hover:bg-accent/70"
                >
                  Создать еще
                </button>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="mt-4 flex flex-col gap-4">
              <label className="flex flex-col gap-1 text-sm">
                <span className="text-primary">Роль</span>
                <select
                  value={role}
                  onChange={(e) => setRole(e.target.value as UserRole)}
                  className="cursor-pointer rounded-xl border border-primary bg-bg px-2 py-2"
                >
                  {typedEntries(roleLabels).map(([value, label]) => (
                    <option key={value} value={value}>{label}</option>
                  ))}
                </select>
              </label>

              <div className="flex flex-col sm:flex-row gap-4">
                <label className="flex flex-1 flex-col gap-1 text-sm">
                  <span className="text-primary">Срок действия</span>
                  <select
                    value={ttlHours}
                    onChange={(e) => setTtlHours(Number(e.target.value))}
                    className="cursor-pointer rounded-xl border border-primary bg-bg px-2 py-2"
                  >
                    {TTL_OPTIONS.map((o) => (
                      <option key={o.hours} value={o.hours}>{o.label}</option>
                    ))}
                  </select>
                </label>

                <label className="flex flex-1 flex-col gap-1 text-sm">
                  <span className="text-primary">Количество активаций</span>
                  <input
                    type="number"
                    min={0}
                    max={1000}
                    value={clicks}
                    onChange={(e) => setClicks(e.target.value)}
                    className="rounded-xl border border-primary bg-bg px-2 py-2 tabular-nums"
                  />
                  <span className="text-xs text-primary/60">0 — без ограничений</span>
                </label>
              </div>

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
                  {createMutation.isPending ? "Создание..." : "Создать ссылку"}
                </button>
              </div>
            </form>
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
};
