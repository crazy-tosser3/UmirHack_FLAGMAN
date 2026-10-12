import { DashboardModule, DashboardPage } from "@pages/dashboard";
import { AnalyticsPage } from "@pages/dashboard/subpages/analytics";
import { NotFoundPage } from "@pages/not-found";
import { ProfilePage } from "@pages/profile";
import type { IconType } from "react-icons";
import { FaChartBar, FaHome } from "react-icons/fa";
import { Navigate, Route } from "react-router-dom";
import type { UserRole } from "./types";
import { DishesTable } from "@widgets/dishes-table";
import { FaPlateWheat } from "react-icons/fa6";

// Навигация (синхронизировано с созданием страниц)
interface Page {
  path: string;
  nested?: React.ReactNode;
  element: React.ReactNode;
}

interface DashboardLink {
  role: UserRole[];
  path: string;
  element: React.ReactNode;
  ico: IconType;
  title: string;
}

interface Nav {
  title: string;
  ico: IconType;
  path: string;
}

interface Navs {
  categoryName: string;
  role: UserRole[];
  navs: {
    primary?: Nav[],
    secondary?: Nav[];
  };
}

export const dashboardNavigation: DashboardLink[] = [
  {
    role: ["employee", "manager", "admin"],
    path: "dishes",
    element: <DashboardModule rowSpan={2} colSpan={4}><DishesTable /></DashboardModule>,
    ico: FaPlateWheat,
    title: "Блюда"
  },
  {
    role: ["manager", "admin"],
    path: "analytics",
    element: <DashboardModule rowSpan={2} colSpan={4}><AnalyticsPage /></DashboardModule>,
    ico: FaChartBar,
    title: "Аналитика"
  }
]

export const pages: Page[] = [
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

export const navigation: Navs[] = [
  {
    categoryName: "Данные",
    role: ["employee", "manager", "admin"],
    navs: {
      primary: [
        {
          title: "Главная",
          ico: FaHome,
          path: "/dashboard"
        }
      ],
      secondary: [
        {
          title: "Блюда",
          ico: FaPlateWheat,
          path: "/dashboard/dishes"
        }
      ]
    }
  },
  {
    categoryName: "Управление",
    role: ["manager", "admin"],
    navs: {
      primary: [
        {
          title: "Аналитика",
          ico: FaChartBar,
          path: "/dashboard/analytics"
        }
      ]
    }
  }
]