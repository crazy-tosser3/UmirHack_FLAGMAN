import { DashboardModule, DashboardPage, StopList } from "@pages/dashboard";
import { NotFoundPage } from "@pages/not-found";
import { ProfilePage } from "@pages/profile";
import type { IconType } from "react-icons";
import { FaPause } from "react-icons/fa6";
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
    path: "stop-list",
    element: <DashboardModule rowSpan={2} colSpan={4}><StopList /></DashboardModule>,
    ico: FaPause,
    title: "Стоп-лист"
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

