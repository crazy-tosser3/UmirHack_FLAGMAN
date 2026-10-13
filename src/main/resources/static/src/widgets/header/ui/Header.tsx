import { NavLink, useNavigate } from "react-router-dom"
import Navigation from "./Navigation"
import { useAuth } from "@shared/auth/auth-service"
import { IcoBtn } from "@shared/ico-text-btn"
import { FaUserCircle } from "react-icons/fa"
import { useServerEvents, type ServerEventsStatus } from "@shared/server-events"
import { cn } from "@lib/utils"

const STATUS: Record<ServerEventsStatus, { title: string; className: string }> = {
  idle: { title: "Обновления отключены", className: "bg-primary" },
  connecting: { title: "Подключение к обновлениям...", className: "bg-warn animate-pulse" },
  open: { title: "Обновления в реальном времени", className: "bg-success" },
  reconnecting: { title: "Нет связи с сервером, переподключение...", className: "bg-err animate-pulse" },
}

export const Header = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const eventsStatus = useServerEvents();

  return (
    <div className="py-4 sticky top-0 bg-bg z-40">
      <header className="content flex gap-4 justify-between items-center relative">
        <NavLink to={"/"} className="text-2xl font-oswald rounded-lg">Pantry</NavLink>
        <Navigation />
        {user && (
          <div className="flex gap-2 items-center">
            <span
              title={STATUS[eventsStatus].title}
              className={cn("size-2.5 rounded-full shrink-0 transition-colors", STATUS[eventsStatus].className)}
            />
            <IcoBtn icon={<FaUserCircle />} onClick={() => navigate("/profile")}>{user.name}</IcoBtn>
          </div>
          )}
      </header>
    </div>
  )
}