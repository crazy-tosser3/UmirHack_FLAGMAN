import { DashboardPage } from "@pages/dashboard/ui/DashboardPage";
import { ProfilePage } from "@pages/profile";

// const mockTableHead = [
//   "Дата создания", "ID", "№ поставки", "Продукты"
// ]

// Навигация (синхронизировано с созданием страниц)
interface Nav {
  path: string;
  element: React.ReactNode;
}

export const navigation: Nav[] = [
 {
  path: "/dashboard",
  element: <DashboardPage />
 },
 {
  path: "/profile",
  element: <ProfilePage />
 }
]