import { dashboardNavigation } from "@lib/config"
import { AnimatePresence, motion } from "framer-motion"
import { useState } from "react"
import { FaList } from "react-icons/fa"
import { NavLink } from "react-router-dom"

const DashboardSidebar = () => {
  const [expanded, setExpanded] = useState(false);

  return (
    <aside className="p-2 flex sm:flex-col gap-4 items-center border border-primary rounded-xl">
      <motion.nav 
      className="p-2 rounded-lg bg-text text-bg flex flex-col gap-1"
      animate={{width: expanded ? "fit-content" : 52}}
      >
        <button 
        className="w-full p-2 rounded-sm hover:bg-primary/20 transition-colors border-b border-primary"
        onClick={() => setExpanded(prev => !prev)}
        >
          <FaList size={20} className="m-auto" />
        </button>
        {dashboardNavigation.map((link) => (
          <NavLink to={link.path} key={link.path} className="flex gap-2 items-center rounded-sm hover:bg-primary/20 p-2">
            <link.ico className="transition-colors min-w-5 min-h-5" size={20} />
            <AnimatePresence mode="wait">
              {expanded && <motion.span className="text-sm text-nowrap">{link.title}</motion.span>}
            </AnimatePresence>
          </NavLink>
        ))}
      </motion.nav>
    </aside>
  )
}

export default DashboardSidebar