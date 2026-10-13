import { FaReceipt } from "react-icons/fa";
import { FaCartPlus } from "react-icons/fa6";
import { NavLink } from "react-router-dom";
import { OrdersList } from "./OrdersList";

export const OrdersPage = () => (
  <div className="flex h-full flex-col min-h-0">
    <h1 className="bg-accent p-2 text-2xl font-oswald shrink-0 flex gap-2 items-center justify-between">
      <span>Заказы</span>
      <FaReceipt />
    </h1>

    <div className="flex flex-col gap-2 p-2 flex-1 min-h-0">
      <OrdersList />
      <NavLink
        to="/dashboard/orders/new"
        className="shrink-0 w-fit ml-auto flex items-center gap-2 rounded-xl bg-accent px-4 py-2 text-sm transition-colors hover:bg-accent/70"
      >
        <FaCartPlus />
        Новый заказ
      </NavLink>
    </div>
  </div>
);
