import type { TelegramAuthData, UserRole } from "@lib/types";
import { canUseTelegram, cn } from "@lib/utils";
import { useEffect, useState } from "react";
import { FaCircleNotch, FaTelegram } from "react-icons/fa";

const BOT_ID = Number(import.meta.env.VITE_TELEGRAM_BOT_ID);
const WIDGET_SRC = "https://telegram.org/js/telegram-widget.js?22";

interface TelegramLogin {
  auth: (
    options: { bot_id: number; request_access?: "write" },
    callback: (data: TelegramAuthData | false) => void,
  ) => void;
}

declare global {
  interface Window {
    Telegram?: { Login?: TelegramLogin };
  }
}

let widgetLoading: Promise<TelegramLogin> | null = null;

const loadWidget = () => {
  if (window.Telegram?.Login) return Promise.resolve(window.Telegram.Login);

  widgetLoading ??= new Promise<TelegramLogin>((resolve, reject) => {
    const script = document.createElement("script");
    script.src = WIDGET_SRC;
    script.async = true;
    script.onload = () =>
      window.Telegram?.Login ? resolve(window.Telegram.Login) : reject(new Error("Telegram widget"));
    script.onerror = () => {
      script.remove();
      widgetLoading = null;
      reject(new Error("Telegram widget"));
    };
    document.head.appendChild(script);
  });

  return widgetLoading;
};

interface TelegramAuthProps {
  onAuth: (data: TelegramAuthData) => Promise<void> | void;
  role?: UserRole;
  label?: string;
  className?: string;
}

export const TelegramAuth = ({
  onAuth,
  role,
  label = "Войти через Telegram",
  className,
}: TelegramAuthProps) => {
  const [pending, setPending] = useState(false);
  const [status, setStatus] = useState<string | null>(null);

  const allowed = Boolean(BOT_ID) && (!role || canUseTelegram(role));

  useEffect(() => {
    if (allowed) loadWidget().catch(() => undefined);
  }, [allowed]);

  if (role && !canUseTelegram(role)) return null;

  if (!BOT_ID) {
    return (
      <p className={cn("flex items-center gap-2 text-sm text-primary", className)}>
        <FaTelegram className="shrink-0" />
        Вход через Telegram не настроен
      </p>
    );
  }

  const handleClick = () => {
    const login = window.Telegram?.Login;
    if (!login) {
      setStatus("Telegram ещё загружается, попробуйте через пару секунд");
      loadWidget().catch(() => setStatus("Не удалось загрузить Telegram, проверьте соединение"));
      return;
    }

    setPending(true);
    setStatus("Подтвердите вход в окне Telegram...");

    login.auth({ bot_id: BOT_ID, request_access: "write" }, async (data) => {
      if (!data) {
        setPending(false);
        setStatus("Вход отменён");
        return;
      }

      setStatus(null);
      try {
        await onAuth(data);
      } finally {
        setPending(false);
      }
    });
  };

  return (
    <div className={cn("flex flex-col items-center gap-2", className)}>
      <button
        type="button"
        disabled={pending}
        onClick={handleClick}
        className="inline-flex items-center gap-2.5 rounded-full bg-[#54a9eb] px-6 py-3 font-semibold text-white transition-colors hover:bg-[#4096d8] disabled:cursor-default disabled:opacity-60"
      >
        {pending ? <FaCircleNotch size={20} className="animate-spin" /> : <FaTelegram size={22} />}
        {label}
      </button>
      {status && <p className="text-sm text-primary text-center">{status}</p>}
    </div>
  );
};
