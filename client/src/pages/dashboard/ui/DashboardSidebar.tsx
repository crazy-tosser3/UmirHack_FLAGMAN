import { dashboardNavigation } from "@lib/config"
import { cn } from "@lib/utils"
import { useAuth } from "@shared/auth/auth-service"
import { useMediaQuery } from "@shared/hooks/use-media-query"
import { AnimatePresence, motion } from "framer-motion"
import { useState } from "react"
import { FaList } from "react-icons/fa"
import { NavLink } from "react-router-dom"

const DashboardSidebar = () => {
  const [expanded, setExpanded] = useState(false);
  const isDesktop = useMediaQuery("(min-width: 640px)");
  const isOpen = isDesktop && expanded;

  const { user } = useAuth();

  const links = user ? dashboardNavigation.filter((link) => link.role.includes(user.role)) : [];
  const showMobileLabels = !isDesktop && links.length <= 4;

  return (
    <aside
      style={isDesktop ? { width: expanded ? 220 + 16 : 52 + 16 } : { paddingBottom: "max(0.5rem, env(safe-area-inset-bottom))" }}
      className="bottom-nav z-30 fixed inset-x-0 bottom-0 px-2 sm:z-20 sm:sticky sm:inset-auto sm:top-0 sm:px-0 flex flex-col gap-4 items-center rounded-xl"
    >
      <motion.nav
        className="p-1.5 sm:p-2 shadow-[0_-4px_16px_rgb(0_0_0/0.15)] sm:shadow-md rounded-2xl sm:rounded-xl bg-text text-bg flex sm:flex-col gap-0.5 sm:gap-1 overflow-x-auto sm:overflow-hidden"
        animate={{
          width: isDesktop ? (expanded ? 220 : 52) : "100%",
        }}
        transition={{ type: "spring", stiffness: 300, damping: 30 }}
      >
        <button
          type="button"
          className="hidden sm:block w-full p-2 rounded-sm hover:bg-primary/20 transition-colors border-b border-primary shrink-0"
          onClick={() => setExpanded((prev) => !prev)}
        >
          <FaList className="m-auto" />
        </button>

        {links.map((link) => {
          return (
            <NavLink
              to={link.path}
              key={link.path}
              end
              title={link.title}
              aria-label={link.title}
              className={({ isActive }) => cn(
                "flex items-center rounded-lg sm:rounded-sm hover:bg-primary/20 transition-colors",
                "flex-1 min-w-9 min-h-11 justify-center flex-col gap-1 p-2 active:bg-primary/30",
                "sm:flex-none sm:min-w-0 sm:min-h-0 sm:justify-start sm:flex-row sm:gap-2 sm:active:bg-transparent",
                isActive && "text-accent bg-primary/15 sm:bg-transparent",
              )}
            >
              <link.ico className="size-6 sm:size-4 shrink-0" />
              {showMobileLabels && (
                <span className="text-[11px] leading-tight text-center">{link.title}</span>
              )}

              <AnimatePresence>
                {isOpen && (
                  <motion.span
                    key="title"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 0.15 }}
                    className="text-xs whitespace-nowrap"
                  >
                    {link.title}
                  </motion.span>
                )}
              </AnimatePresence>
            </NavLink>
          )
        })}
      </motion.nav>
    </aside>
  );
};

export default DashboardSidebar;