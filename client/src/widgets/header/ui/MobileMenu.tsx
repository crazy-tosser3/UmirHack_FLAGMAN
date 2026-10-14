import { navigation } from "@lib/config";
import { cn, roleLabels } from "@lib/utils";
import { useAuth } from "@shared/auth/auth-service";
import { AnimatePresence, motion } from "framer-motion";
import { useEffect, useRef, useState } from "react";
import { FaUserCircle } from "react-icons/fa";
import { FaBars, FaXmark } from "react-icons/fa6";
import { NavLink, useLocation } from "react-router-dom";

export const MobileMenu = () => {
  const { user } = useAuth();
  const location = useLocation();
  const [openedAt, setOpenedAt] = useState<string | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  const isOpen = openedAt === location.pathname;
  const close = () => setOpenedAt(null);

  useEffect(() => {
    if (!isOpen) return;

    const onPointerDown = (e: PointerEvent) => {
      if (!containerRef.current?.contains(e.target as Node)) setOpenedAt(null);
    };
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpenedAt(null);
    };

    document.addEventListener("pointerdown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [isOpen]);

  if (!user) return null;

  const categories = navigation.filter((c) => c.role.includes(user.role));

  return (
    <div ref={containerRef} className="md:hidden">
      <button
        type="button"
        aria-label={isOpen ? "Закрыть меню" : "Открыть меню"}
        aria-expanded={isOpen}
        onClick={() => setOpenedAt(isOpen ? null : location.pathname)}
        className="grid size-10 place-items-center rounded-lg bg-secondary active:bg-primary transition-colors"
      >
        {isOpen ? <FaXmark size={18} /> : <FaBars size={18} />}
      </button>

      <AnimatePresence>
        {isOpen && (
          <motion.nav
            className="absolute left-0 right-0 top-full mt-2 max-h-[calc(100dvh-5rem)] overflow-auto rounded-xl border border-primary bg-bg/90 backdrop-blur-md shadow-lg p-3 flex flex-col gap-4"
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ type: "spring", stiffness: 300, damping: 30 }}
          >
            <NavLink
              to="/profile"
              onClick={close}
              className="flex items-center gap-3 p-2 rounded-lg bg-secondary"
            >
              <FaUserCircle size={28} className="shrink-0" />
              <span className="flex flex-col min-w-0">
                <span className="truncate font-manrope">{user.name}</span>
                <span className="text-xs text-primary">{roleLabels[user.role]}</span>
              </span>
            </NavLink>

            {categories.map((category) => (
              <section key={category.categoryName} className="flex flex-col gap-2">
                <h2 className="text-sm text-primary border-b border-primary/40">{category.categoryName}</h2>
                <div className="grid grid-cols-2 gap-2">
                  {[...(category.navs.primary ?? []), ...(category.navs.secondary ?? [])].map((nav) => (
                    <NavLink
                      key={nav.path}
                      to={nav.path}
                      end
                      onClick={close}
                      className={({ isActive }) =>
                        cn(
                          "flex items-center gap-2 p-2 rounded-lg border border-primary/40 text-sm transition-colors active:bg-secondary",
                          isActive && "border-accent bg-accent/20",
                        )
                      }
                    >
                      <nav.ico size={18} className="shrink-0 text-accent" />
                      <span className="truncate">{nav.title}</span>
                    </NavLink>
                  ))}
                </div>
              </section>
            ))}
          </motion.nav>
        )}
      </AnimatePresence>
    </div>
  );
};
