import { isStaff } from "@lib/utils";
import { useAuth } from "@shared/auth/auth-service";
import { FaShoppingCart } from "react-icons/fa";
import { FaBox } from "react-icons/fa6";
import type { IconType } from "react-icons";
import { NavLink } from "react-router-dom";

interface Action {
  title: string;
  path: string;
  ico: IconType;
  staffOnly?: boolean;
}

const ACTIONS: Action[] = [
  { title: "Новая поставка", path: "/dashboard/deliveries/new", ico: FaBox, staffOnly: true },
  { title: "Новый заказ", path: "/dashboard/orders/new", ico: FaShoppingCart },
];

const FastActions = () => {
  const { user } = useAuth();
  const staff = user ? isStaff(user.role) : false;

  return (
    <div className="flex flex-col gap-4 p-2 h-full">
      {ACTIONS.filter((a) => staff || !a.staffOnly).map((action) => (
        <NavLink
          key={action.path}
          to={action.path}
          className="bg-accent flex-1 min-h-24 p-4 rounded-lg relative overflow-hidden flex hover:brightness-110 active:scale-95 transition-transform"
        >
          <action.ico className="absolute right-3 bottom-3 h-3/4 max-h-28 w-auto text-bg/40" />
          <span className="text-xl font-manrope mr-auto relative">{action.title}</span>
        </NavLink>
      ))}
    </div>
  );
};

export default FastActions;
