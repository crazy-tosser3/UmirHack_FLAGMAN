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

  return (
    <aside style={{width: expanded ? 220+16 : 52+16}} className="z-20 sticky top-0 flex flex-col gap-4 items-center rounded-xl">
      <motion.nav
        className="p-2 mr-auto ml-2 shadow-md rounded-xl bg-text text-bg flex sm:flex-col gap-1 overflow-hidden"
        animate={{
          width: isDesktop ? (expanded ? 220 : 52) : "auto",
        }}
        transition={{ type: "spring", stiffness: 300, damping: 30 }}
      >
        <button
          type="button"
          className="hidden sm:block w-full p-2 rounded-sm hover:bg-primary/20 transition-colors border-b border-primary shrink-0"
          onClick={() => setExpanded((prev) => !prev)}
        >
          <FaList size={20} className="m-auto" />
        </button>

        {user && dashboardNavigation.map((link) => {
          
          if (!link.role.includes(user.role)) return null;

          return (
            <NavLink
              to={link.path}
              key={link.path}
              end
              className={({ isActive }) => cn("flex gap-2 items-center rounded-sm hover:bg-primary/20 transition-colors p-2", isActive && "text-accent")}
            >
              <link.ico className="min-w-5 min-h-5 shrink-0" size={20} />

              <AnimatePresence>
                {isOpen && (
                  <motion.span
                    key="title"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 0.15 }}
                    className="text-sm whitespace-nowrap"
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