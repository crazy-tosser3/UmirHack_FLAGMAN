import { dashboardNavigation } from "@lib/config"
import { NavLink } from "react-router-dom"

const DashboardSidebar = () => {
  return (
    <aside className="p-2 flex sm:flex-col gap-4 items-center border border-primary rounded-xl">
      <nav className="p-2 rounded-lg bg-text text-bg">
        {dashboardNavigation.map((link) => (
          <NavLink to={link.path} key={link.path}>
            <link.ico className="h-8 w-8 p-2 rounded-md hover:bg-primary" />
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}

export default DashboardSidebar