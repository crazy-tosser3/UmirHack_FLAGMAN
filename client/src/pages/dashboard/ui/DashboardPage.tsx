import { Outlet } from "react-router-dom"
import DashboardSidebar from "./DashboardSidebar"
import HotOffers from "./modules/HotOffers"
import { DashboardModule } from "./DashboardModule"
import { StopList } from "./StopList"
import ExpiringBatches from "./modules/ExpiringBatches"
import { useAuth } from "@shared/auth/auth-service"
import FastActions from "./modules/FastActions"

export const DashboardPage = () => {
  const { user } = useAuth();

  return (
    <div className="overflow-auto flex-1 flex flex-col sm:flex-row gap-4 pb-24 sm:pb-0">
      <DashboardSidebar />
      <div className="sm:flex-2 md:flex-3 grid md:grid-rows-3 gap-4">
        <div className="row-span-2 grid md:grid-cols-4 md:grid-rows-2">
          <Outlet />
        </div>
        <div className="row-span-2 grid md:grid-cols-3 gap-4 ">
          <DashboardModule>
            <HotOffers />
          </DashboardModule>
          <DashboardModule>
            <StopList />
          </DashboardModule>
          <DashboardModule>
            {user?.role !== "employee" ? <ExpiringBatches /> : <FastActions />}
          </DashboardModule>
        </div>
      </div>
    </div>
  )
}