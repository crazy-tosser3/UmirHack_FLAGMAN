import { Outlet } from "react-router-dom"
import DashboardSidebar from "./DashboardSidebar"
import HotOffers from "./modules/HotOffers"
import { DashboardModule } from "./DashboardModule"
import { StopList } from "./StopList"

export const DashboardPage = () => {
  return (
    <div className="flex-1 overflow-y-hidden flex flex-col sm:flex-row gap-4">
      <DashboardSidebar />
      <div className="sm:flex-2 md:flex-3 grid grid-rows-3 gap-4">
        <div className="row-span-2 grid grid-cols-4 grid-rows-2">
          <Outlet />
        </div>
        <div className="row-span-2 grid grid-cols-3 gap-4 ">
          <DashboardModule>
            <HotOffers />
          </DashboardModule>
          <DashboardModule>
            <StopList />
          </DashboardModule>
          <DashboardModule>
            <HotOffers />
          </DashboardModule>
        </div>
      </div>
    </div>
  )
}