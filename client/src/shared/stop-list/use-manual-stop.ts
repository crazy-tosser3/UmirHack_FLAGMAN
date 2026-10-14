import api from "@lib/api";
import { apiErrorMessage } from "@lib/utils";
import { useToastStore } from "@shared/toasts";
import { useMutation, useQueryClient } from "@tanstack/react-query";

interface ManualStopResult {
  dishId: string;
  dishName: string;
  manualStop: boolean;
  isAvailable: boolean;
}

const INVALIDATE_KEYS = ["stop-list", "dishes", "hot-offers"];

export function useManualStop() {
  const queryClient = useQueryClient();
  const newToast = useToastStore((s) => s.newToast);

  return useMutation({
    mutationFn: async ({ dishId, stop }: { dishId: string; stop: boolean }) =>
      (stop
        ? await api.put<ManualStopResult>(`/stop-list/${dishId}`)
        : await api.delete<ManualStopResult>(`/stop-list/${dishId}`)
      ).data,
    onSuccess: (result) => {
      INVALIDATE_KEYS.forEach((key) => queryClient.invalidateQueries({ queryKey: [key] }));
      if (result.manualStop) {
        newToast(`«${result.dishName}» поставлено в стоп-лист`, "success");
      } else if (result.isAvailable) {
        newToast(`«${result.dishName}» снова в меню`, "success");
      } else {
        newToast(`«${result.dishName}» снято с ручного стопа, но всё ещё недоступно из-за остатков`, "info");
      }
    },
    onError: (err) => newToast(apiErrorMessage(err, "Не удалось изменить стоп-лист"), "error"),
  });
}
