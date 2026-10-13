import api from "@lib/api";
import { formatDateTime, units } from "@lib/utils";
import { useIngredientsCatalog } from "@shared/hooks/use-ingredients-catalog";
import * as Dialog from "@radix-ui/react-dialog";
import { useQuery } from "@tanstack/react-query";
import { FaCircleNotch } from "react-icons/fa";
import { FaXmark } from "react-icons/fa6";
import type { Delivery } from "../model/types";

interface DeliveryDetailsDialogProps {
  deliveryId: string | null;
  onClose: () => void;
}

export const DeliveryDetailsDialog = ({ deliveryId, onClose }: DeliveryDetailsDialogProps) => {
  const open = deliveryId !== null;

  const { data, isPending, isError } = useQuery<Delivery>({
    queryKey: ["deliveries", "details", deliveryId],
    queryFn: async () => (await api.get<Delivery>(`/deliveries/${deliveryId}`)).data,
    enabled: open,
  });

  const { data: ingredients } = useIngredientsCatalog(open);
  const ingredientOf = (id: string) => ingredients?.find((i) => i.id === id);

  return (
    <Dialog.Root open={open} onOpenChange={(next) => !next && onClose()}>
      <Dialog.Portal>
        <Dialog.Overlay className="animate-overlay-show fixed inset-0 z-40 bg-primary/40 backdrop-blur-sm" />

        <Dialog.Content className="animate-content-show fixed inset-0 z-50 m-auto h-fit max-h-[85vh] w-[calc(100%-2rem)] max-w-lg overflow-auto rounded-xl border border-primary bg-bg p-4 shadow-2xl outline-none">
          <div className="flex items-start justify-between gap-4">
            <div>
              <Dialog.Title className="text-xl font-oswald">
                Поставка {data?.deliveryNumber ?? ""}
              </Dialog.Title>
              <Dialog.Description className="mt-1 text-sm text-primary">
                {data ? `Принята ${formatDateTime(data.receivedAt)}` : "Состав поставки"}
              </Dialog.Description>
            </div>

            <Dialog.Close asChild>
              <button
                type="button"
                className="grid size-8 shrink-0 place-items-center rounded-lg border border-primary transition-colors hover:bg-secondary"
              >
                <FaXmark size={14} />
              </button>
            </Dialog.Close>
          </div>

          <div className="mt-4">
            {isPending ? (
              <div className="grid place-items-center p-6">
                <FaCircleNotch size={20} className="animate-spin" />
              </div>
            ) : isError ? (
              <p className="text-err bg-err-bg p-2 rounded-xl text-center text-sm">
                Не удалось загрузить поставку
              </p>
            ) : (
              <ul className="flex flex-col divide-y divide-primary/40 rounded-xl border border-primary">
                {data.items.map((item) => {
                  const ingredient = ingredientOf(item.ingredientId);
                  const unit = ingredient ? units[ingredient.unit] : "";
                  return (
                    <li key={item.batchId} className="flex flex-col gap-1 p-2 text-sm">
                      <div className="flex justify-between gap-4">
                        <span className="truncate font-manrope">{ingredient?.name ?? "Ингредиент"}</span>
                        <span className="tabular-nums shrink-0">
                          {item.receivedQuantity} {unit}
                        </span>
                      </div>
                      <div className="flex justify-between gap-4 text-xs text-primary">
                        <span>Годен до {formatDateTime(item.expiresAt)}</span>
                        <span className="tabular-nums shrink-0">
                          Осталось {item.remainingQuantity} {unit}
                        </span>
                      </div>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
};
