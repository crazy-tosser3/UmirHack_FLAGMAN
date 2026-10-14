import api from "@lib/api";
import { cn, formatDateTime, units } from "@lib/utils";
import { useIngredientsCatalog } from "@shared/hooks/use-ingredients-catalog";
import * as Dialog from "@radix-ui/react-dialog";
import { useQuery } from "@tanstack/react-query";
import { FaCircleNotch } from "react-icons/fa";
import { FaXmark } from "react-icons/fa6";
import type { Order, OrderStatus } from "../model/types";

const STATUS: Record<OrderStatus, { label: string; className: string }> = {
  confirmed: { label: "Принят", className: "bg-success-bg text-success" },
  cancelled: { label: "Отменен", className: "bg-err-bg text-err" },
};

interface OrderDetailsDialogProps {
  orderId: string | null;
  onClose: () => void;
}

export const OrderDetailsDialog = ({ orderId, onClose }: OrderDetailsDialogProps) => {
  const open = orderId !== null;

  const { data, isPending, isError } = useQuery<Order>({
    queryKey: ["orders", "details", orderId],
    queryFn: async () => (await api.get<Order>(`/orders/${orderId}`)).data,
    enabled: open,
  });

  const { data: ingredients } = useIngredientsCatalog(open);
  const ingredientName = (id: string) => ingredients?.find((i) => i.id === id)?.name ?? "Ингредиент";

  const status = data ? STATUS[data.status] : undefined;
  const portions = data?.items.reduce((sum, i) => sum + i.quantity, 0) ?? 0;

  return (
    <Dialog.Root open={open} onOpenChange={(next) => !next && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay className="animate-overlay-show fixed inset-0 z-40 bg-primary/40 backdrop-blur-sm" />

        <Dialog.Content className="animate-content-show fixed inset-0 z-50 m-auto h-fit max-h-[85vh] w-[calc(100%-2rem)] max-w-lg overflow-auto rounded-xl border border-primary bg-bg p-4 shadow-2xl outline-none">
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0">
              <Dialog.Title className="text-xl font-oswald truncate">
                Заказ {data ? data.externalOrderId ?? data.id.slice(0, 8) : ""}
              </Dialog.Title>
              <Dialog.Description className="mt-1 text-sm text-primary">
                {data ? formatDateTime(data.createdAt) : "Состав заказа"}
              </Dialog.Description>
            </div>

            <div className="flex items-center gap-2 shrink-0">
              {status && (
                <span className={cn("text-xs px-2 py-0.5 rounded-full", status.className)}>
                  {status.label}
                </span>
              )}
              <Dialog.Close asChild>
                <button
                  type="button"
                  className="grid size-8 place-items-center rounded-lg border border-primary transition-colors hover:bg-secondary"
                >
                  <FaXmark size={14} />
                </button>
              </Dialog.Close>
            </div>
          </div>

          <div className="mt-4">
            {isPending ? (
              <div className="grid place-items-center p-6">
                <FaCircleNotch size={20} className="animate-spin" />
              </div>
            ) : isError ? (
              <p className="text-err bg-err-bg p-2 rounded-xl text-center text-sm">
                Не удалось загрузить заказ
              </p>
            ) : (
              <div className="flex flex-col gap-4">
                <section className="flex flex-col gap-2">
                  <h3 className="text-sm text-primary flex justify-between">
                    <span>Состав</span>
                    <span className="tabular-nums">порций: {portions}</span>
                  </h3>
                  <ul className="flex flex-col divide-y divide-primary/40 rounded-xl border border-primary">
                    {data.items.map((item, index) => (
                      <li key={`${item.dishId}-${index}`} className="flex justify-between gap-4 p-2 text-sm">
                        <span className="min-w-0">
                          <span className="block truncate font-manrope">{item.dishName}</span>
                          {item.excludedIngredients && item.excludedIngredients.length > 0 && (
                            <span className="block text-xs text-warn">
                              без: {item.excludedIngredients.map((e) => e.ingredientName.toLowerCase()).join(", ")}
                            </span>
                          )}
                        </span>
                        <span className="tabular-nums shrink-0">× {item.quantity}</span>
                      </li>
                    ))}
                  </ul>
                </section>

                {data.consumption.length > 0 && (
                  <section className="flex flex-col gap-2">
                    <h3 className="text-sm text-primary">Списано со склада</h3>
                    <ul className="flex flex-col divide-y divide-primary/40 rounded-xl border border-primary">
                      {data.consumption.map((c) => (
                        <li key={c.ingredientId} className="flex justify-between gap-4 p-2 text-sm">
                          <span className="min-w-0">
                            <span className="block truncate">{ingredientName(c.ingredientId)}</span>
                            {c.allocations.length > 1 && (
                              <span className="block text-xs text-primary">
                                из {c.allocations.length} партий
                              </span>
                            )}
                          </span>
                          <span className="tabular-nums shrink-0">
                            {c.quantity} {units[c.unit] ?? c.unit}
                          </span>
                        </li>
                      ))}
                    </ul>
                  </section>
                )}
              </div>
            )}
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
};
