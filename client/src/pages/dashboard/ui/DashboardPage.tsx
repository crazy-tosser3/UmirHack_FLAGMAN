import { Outlet } from "react-router-dom"
import DashboardSidebar from "./DashboardSidebar"

export const DashboardPage = () => {
  return (
    <div className="flex-1 overflow-y-hidden flex flex-col sm:flex-row gap-4">
      <DashboardSidebar />
      <div className="sm:flex-2 md:flex-3 grid grid-rows-5">
        <Outlet />
      </div>
    </div>
  )
}