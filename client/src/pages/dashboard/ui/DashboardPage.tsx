import DashboardModule from "./DashboardModule"
import HotOffers from "./modules/HotOffers"

export const DashboardPage = () => {
  return (
    <div className="flex-1 overflow-y-hidden grid md:grid-cols-3 lg:grid-cols-4 grid-rows-5 gap-4">
      <DashboardModule rowSpan={5} colSpan={1}>
        <HotOffers />
      </DashboardModule>
    </div>
  )
}