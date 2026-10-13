import api from "@lib/api";
import type { TelegramAuthData, User } from "@lib/types";
import { apiErrorMessage, canUseTelegram } from "@lib/utils";
import { useAuth } from "@shared/auth/auth-service";
import { useToastStore } from "@shared/toasts";
import { useMutation } from "@tanstack/react-query";
import { TelegramAuth } from "@widgets/telegram-auth";
import { FaTelegram } from "react-icons/fa";
import { FaLinkSlash } from "react-icons/fa6";

export const TelegramLink = ({ user }: { user: User }) => {
  const { updateUser } = useAuth();
  const newToast = useToastStore((s) => s.newToast);

  const unlink = useMutation({
    mutationFn: async () => (await api.delete<User>("/auth/telegram/link")).data,
    onSuccess: (next) => {
      updateUser(next);
      newToast("Telegram отвязан", "success");
    },
    onError: (err) => newToast(apiErrorMessage(err, "Не удалось отвязать Telegram"), "error"),
  });

  const handleLink = async (data: TelegramAuthData) => {
    try {
      const res = await api.post<User>("/auth/telegram/link", data);
      updateUser(res.data);
      newToast("Telegram привязан", "success");
    } catch (err) {
      newToast(apiErrorMessage(err, "Не удалось привязать Telegram"), "error");
    }
  };

  if (!canUseTelegram(user.role)) return null;

  return (
    <>
      <h2 className="text-xl font-manrope border-b border-primary flex items-center gap-2">
        <FaTelegram />
        Telegram
      </h2>
      {user.telegramLinked ? (
        <div className="flex gap-2 items-center flex-wrap">
          <span className="text-primary">Привязан:</span>
          <span className="bg-success-bg text-success px-2 rounded-full truncate">
            {user.telegramUsername ? `@${user.telegramUsername}` : "без username"}
          </span>
          <button
            type="button"
            disabled={unlink.isPending}
            onClick={() => unlink.mutate()}
            className="ml-auto md:ml-0 flex items-center gap-2 p-2 px-3 bg-secondary rounded-lg transition-colors hover:bg-err-bg hover:text-err disabled:opacity-60"
          >
            <FaLinkSlash />
            Отвязать
          </button>
        </div>
      ) : (
        <div className="flex flex-col gap-2">
          <p className="text-sm text-primary">
            Привяжите Telegram, чтобы входить в один клик и получать уведомления от бота
          </p>
          <TelegramAuth role={user.role} onAuth={handleLink} size="medium" />
        </div>
      )}
    </>
  );
};
