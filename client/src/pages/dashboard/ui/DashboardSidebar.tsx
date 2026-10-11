import { dashboardNavigation } from "@lib/config"
import { useMediaQuery } from "@shared/hooks/use-media-query"
import { AnimatePresence, motion } from "framer-motion"
import { useState } from "react"
import { FaList } from "react-icons/fa"
import { NavLink } from "react-router-dom"

const DashboardSidebar = () => {
  const [expanded, setExpanded] = useState(false);
  const isDesktop = useMediaQuery("(min-width: 640px)");
  const isOpen = isDesktop && expanded;

  return (
    <aside className="p-2 z-45 backdrop-blur-md sticky top-0 flex flex-col gap-4 items-center border border-primary rounded-xl">
      <motion.nav
        className="p-2 rounded-lg bg-text text-bg flex sm:flex-col gap-1"
        animate={{
          width: isDesktop ? (expanded ? "fit-content" : 52) : "fit-content",
        }}
      >
        <button
          type="button"
          className="hidden sm:block w-full p-2 rounded-sm hover:bg-primary/20 transition-colors border-b border-primary"
          onClick={() => setExpanded((prev) => !prev)}
        >
          <FaList size={20} className="m-auto" />
        </button>

        {dashboardNavigation.map((link) => (
          <NavLink
            to={link.path}
            key={link.path}
            className="flex gap-2 items-center rounded-sm hover:bg-primary/20 transition-colors p-2"
          >
            <link.ico className="min-w-5 min-h-5 shrink-0" size={20} />

            <AnimatePresence mode="wait">
              {isOpen && (
                <motion.span
                  key="title"
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: 20 }}
                  transition={{damping: 20}}
                  className="text-sm text-nowrap"
                >
                  {link.title}
                </motion.span>
              )}
            </AnimatePresence>
          </NavLink>
        ))}
      </motion.nav>
    </aside>
  );
};

export default DashboardSidebar;