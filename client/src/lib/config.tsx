import { DashboardModule, DashboardPage } from "@pages/dashboard";
import { AnalyticsPage } from "@pages/dashboard/subpages/analytics";
import { NotFoundPage } from "@pages/not-found";
import { ProfilePage } from "@pages/profile";
import type { IconType } from "react-icons";
import { FaBoxes, FaChartBar, FaHome } from "react-icons/fa";
import { Navigate, Route } from "react-router-dom";
import type { UserRole } from "./types";
import { DishesTable } from "@widgets/dishes-table";
import { FaPlateWheat } from "react-icons/fa6";
import { StockTable } from "@widgets/stock-table";
import { OrdersPage } from "@pages/dashboard/subpages/orders";
import { AdminPage } from "@pages/dashboard/subpages/admin";
import { FaReceipt, FaShoppingCart, FaUserShield } from "react-icons/fa";
import { CreateOrderPage } from "@pages/dashboard/subpages/create-order";
import { DeliveriesPage } from "@pages/dashboard/subpages/deliveries";
import { CreateDeliveryPage } from "@pages/dashboard/subpages/create-delivery";
import { FaTruck, FaTruckLoading } from "react-icons/fa";
import { PageTitle } from "@shared/page-title";
import { CalendarPage } from "@pages/calendar";
import { FaCalendarAlt } from "react-icons/fa";

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
    path: "stock",
    element: <DashboardModule rowSpan={2} colSpan={4}><StockTable /></DashboardModule>,
    ico: FaBoxes,
    title: "Остатки"
  },
  {
    role: ["manager", "admin"],
    path: "analytics",
    element: <DashboardModule rowSpan={2} colSpan={4}><AnalyticsPage /></DashboardModule>,
    ico: FaChartBar,
    title: "Аналитика"
  },
  {
    role: ["employee", "manager", "admin"],
    path: "orders/new",
    element: <DashboardModule rowSpan={2} colSpan={4}><CreateOrderPage /></DashboardModule>,
    ico: FaShoppingCart,
    title: "Новый заказ"
  },
  {
    role: ["manager", "admin"],
    path: "orders",
    element: <DashboardModule rowSpan={2} colSpan={4}><OrdersPage /></DashboardModule>,
    ico: FaReceipt,
    title: "Заказы"
  },
  {
    role: ["manager", "admin"],
    path: "deliveries/new",
    element: <DashboardModule rowSpan={2} colSpan={4}><CreateDeliveryPage /></DashboardModule>,
    ico: FaTruckLoading,
    title: "Новая поставка"
  },
  {
    role: ["manager", "admin"],
    path: "deliveries",
    element: <DashboardModule rowSpan={2} colSpan={4}><DeliveriesPage /></DashboardModule>,
    ico: FaTruck,
    title: "Поставки"
  },
  {
    role: ["admin"],
    path: "admin",
    element: <DashboardModule rowSpan={2} colSpan={4}><AdminPage /></DashboardModule>,
    ico: FaUserShield,
    title: "Пользователи"
  }
]

export const pages: Page[] = [
 {
  path: "/dashboard",
  nested: (
    <>
      <Route index element={<Navigate to={dashboardNavigation[0].path} replace />} />
      {dashboardNavigation.map((s) => (
        <Route key={s.path} path={s.path} element={<><PageTitle title={s.title} />{s.element}</>} />
      ))}
      <Route path="*" element={<NotFoundPage />} />
    </>
  ),
  element: <DashboardPage />
 },
 {
  path: "/profile",
  element: <ProfilePage />
 },
 {
  path: "/calendar",
  element: <CalendarPage />
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
        },
        {
          title: "Новый заказ",
          ico: FaShoppingCart,
          path: "/dashboard/orders/new"
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
        },
        {
          title: "Календарь",
          ico: FaCalendarAlt,
          path: "/calendar"
        },
      ],
      secondary: [
        {
          title: "Остатки",
          ico: FaBoxes,
          path: "/dashboard/stock"
        },
        {
          title: "Заказы",
          ico: FaReceipt,
          path: "/dashboard/orders"
        },
        {
          title: "Поставки",
          ico: FaTruck,
          path: "/dashboard/deliveries"
        },
      ]
    }
  },
  {
    categoryName: "Администрирование",
    role: ["admin"],
    navs: {
      primary: [
        {
          title: "Пользователи",
          ico: FaUserShield,
          path: "/dashboard/admin"
        }
      ]
    }
  }
]
