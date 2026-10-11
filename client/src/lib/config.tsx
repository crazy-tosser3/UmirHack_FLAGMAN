import { DashboardModule, DashboardPage } from "@pages/dashboard";
import { AnalyticsPage } from "@pages/dashboard/subpages/analytics";
import { NotFoundPage } from "@pages/not-found";
import { ProfilePage } from "@pages/profile";
import type { IconType } from "react-icons";
import { FaChartBar } from "react-icons/fa";
import { Navigate, Route } from "react-router-dom";

// Навигация (синхронизировано с созданием страниц)
interface Nav {
  path: string;
  nested?: React.ReactNode;
  element: React.ReactNode;
}

interface Link {
  path: string;
  element: React.ReactNode;
  ico: IconType;
  title: string;
}

export const dashboardNavigation: Link[] = [
  {
    path: "analytics",
    element: <DashboardModule rowSpan={2} colSpan={4}><AnalyticsPage /></DashboardModule>,
    ico: FaChartBar,
    title: "Аналитика"
  }
]

export const navigation: Nav[] = [
 {
  path: "/dashboard",
  nested: (
    <>
      <Route index element={<Navigate to={dashboardNavigation[0].path} replace />} />
      {dashboardNavigation.map((s) => (
        <Route key={s.path} path={s.path} element={s.element} />
      ))}
      <Route path="*" element={<NotFoundPage />} />
    </>
  ),
  element: <DashboardPage />
 },
 {
  path: "/profile",
  element: <ProfilePage />
 }
]

