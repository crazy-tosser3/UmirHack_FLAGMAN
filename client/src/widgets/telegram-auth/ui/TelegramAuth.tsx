import type { TelegramAuthData, UserRole } from "@lib/types";
import { canUseTelegram, cn } from "@lib/utils";
import { useEffect, useId, useRef, useState } from "react";
import { FaCircleNotch, FaTelegram } from "react-icons/fa";

const BOT_USERNAME: string | undefined = import.meta.env.VITE_TELEGRAM_BOT_USERNAME;
const WIDGET_SRC = "https://telegram.org/js/telegram-widget.js?22";

type Callbacks = Record<string, (data: TelegramAuthData) => void>;

interface TelegramAuthProps {
  onAuth: (data: TelegramAuthData) => Promise<void> | void;
  role?: UserRole;
  size?: "large" | "medium" | "small";
  className?: string;
}

export const TelegramAuth = ({ onAuth, role, size = "large", className }: TelegramAuthProps) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const onAuthRef = useRef(onAuth);
  const [pending, setPending] = useState(false);
  const callbackName = "onTelegramAuth" + useId().replace(/[^a-zA-Z0-9]/g, "");

  const allowed = !role || canUseTelegram(role);

  useEffect(() => {
    onAuthRef.current = onAuth;
  }, [onAuth]);

  useEffect(() => {
    const container = containerRef.current;
    if (!allowed || !BOT_USERNAME || !container) return;

    const callbacks = window as unknown as Callbacks;
    callbacks[callbackName] = async (data) => {
      setPending(true);
      try {
        await onAuthRef.current(data);
      } finally {
        setPending(false);
      }
    };

    const script = document.createElement("script");
    script.src = WIDGET_SRC;
    script.async = true;
    script.dataset.telegramLogin = BOT_USERNAME;
    script.dataset.size = size;
    script.dataset.radius = "8";
    script.dataset.requestAccess = "write";
    script.dataset.userpic = "false";
    script.dataset.onauth = `${callbackName}(user)`;
    container.replaceChildren(script);

    return () => {
      container.replaceChildren();
      delete callbacks[callbackName];
    };
  }, [allowed, callbackName, size]);

  if (!allowed) return null;

  if (!BOT_USERNAME) {
    return (
      <p className={cn("flex items-center gap-2 text-sm text-primary", className)}>
        <FaTelegram className="shrink-0" />
        Вход через Telegram не настроен
      </p>
    );
  }

  return (
    <div className={cn("relative flex items-center min-h-10", className)}>
      <div ref={containerRef} className={cn("transition-opacity", pending && "opacity-40 pointer-events-none")} />
      {pending && (
        <FaCircleNotch size={16} className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 animate-spin" />
      )}
    </div>
  );
};
