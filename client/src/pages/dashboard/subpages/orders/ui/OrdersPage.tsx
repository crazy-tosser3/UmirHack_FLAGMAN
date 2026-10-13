import { isStaff } from "@lib/utils";
import { useAuth } from "@shared/auth/auth-service";
import { FaShoppingCart } from "react-icons/fa";
import { CreateOrderForm } from "./CreateOrderForm";
import { OrdersList } from "./OrdersList";

export const OrdersPage = () => {
  const { user } = useAuth();
  const staff = user ? isStaff(user.role) : false;

  return (
    <div className="flex h-full flex-col min-h-0">
      <h1 className="bg-accent p-2 text-2xl font-oswald shrink-0 flex gap-2 items-center justify-between">
        <span>Приемка заказов</span>
        <FaShoppingCart />
      </h1>

      <div className="flex flex-col md:flex-row gap-4 p-2 flex-1 min-h-0 overflow-auto">
        <section className="md:flex-1 md:overflow-auto">
          <CreateOrderForm />
        </section>
        {staff && (
          <section className="md:flex-1 min-h-0 md:border-l md:border-primary md:pl-4">
            <OrdersList />
          </section>
        )}
      </div>
    </div>
  );
};
