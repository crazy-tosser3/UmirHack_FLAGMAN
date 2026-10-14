import api from "@lib/api";
import { useDelayedValue } from "@shared/hooks/use-delayed-value";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { AnimatePresence, motion } from "framer-motion";
import { useState } from "react";
import { FaChevronLeft, FaChevronRight, FaCircleNotch } from "react-icons/fa";
import { FaChevronDown, FaHand, FaPause, FaPlay } from "react-icons/fa6";
import { isManualStop, type Reason, type StopListData, type StopListDish } from "../model/types";
import { cn, copyText, isStaff, units } from "@lib/utils";
import { useAuth } from "@shared/auth/auth-service";
import { useManualStop } from "@shared/stop-list";

const ReasonRow = ({ reason }: { reason: Reason }) => {
  if (reason.code === "manual") {
    return (
      <li className="flex gap-2 items-center text-warn">
        <FaHand className="shrink-0" />
        <span>Остановлено вручную</span>
      </li>
    );
  }

  const label = reason.code === "expired_ingredient" ? "просрочен" : null;

  return (
    <li className="flex gap-4 items-center justify-between">
      <span className="truncate">{reason.ingredientName ?? "Ингредиент"}</span>
      <span className="bg-err-bg text-err px-2 rounded-full tabular-nums shrink-0">
        {label ?? `${reason.availableQuantity ?? 0} / ${reason.requiredQuantity ?? 0} ${reason.unit ? units[reason.unit] : ""}`}
      </span>
    </li>
  );
};

const Dish = ({ dish, index }: { dish: StopListDish; index: number }) => {
  const [showDetails, setShowDetails] = useState(false);
  const { user } = useAuth();
  const manualStop = useManualStop();
  const manual = isManualStop(dish);
  const canManage = user ? isStaff(user.role) : false;

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.05, duration: 0.25 }}
      className="flex gap-2 min-w-0"
    >
      <div className="flex flex-col gap-1 min-w-0 flex-1">
        <div className="flex items-center gap-2 min-w-0">
          <p className="font-manrope truncate flex-1">{dish.dishName}</p>
          {manual && (
            <span className="shrink-0 text-xs bg-warn-bg text-warn px-2 rounded-full">вручную</span>
          )}
          {canManage && manual && (
            <button
              type="button"
              title="Вернуть в меню"
              disabled={manualStop.isPending}
              onClick={() => manualStop.mutate({ dishId: dish.dishId, stop: false })}
              className="shrink-0 grid size-7 place-items-center rounded-lg border border-primary transition-colors hover:bg-success-bg hover:text-success disabled:opacity-40"
            >
              <FaPlay size={10} />
            </button>
          )}
        </div>

        <button
          type="button"
          className="text-primary flex items-center justify-between gap-2 p-1 px-2 rounded-lg hover:bg-secondary transition-colors"
          onClick={() => setShowDetails(prev => !prev)}
        >
          <span>Подробнее</span>
          <FaChevronDown className={cn("transition-transform", showDetails ? "rotate-180" : "")} />
        </button>

        <AnimatePresence initial={false}>
          {showDetails && (
            <motion.div
              key="details"
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: "auto", opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.25, ease: "easeInOut" }}
              className="overflow-hidden"
            >
              <ul className="p-2 border rounded-md border-primary flex flex-col gap-2">
                {dish.reasons.map((reason, index) => (
                  <ReasonRow key={index} reason={reason} />
                ))}

                <button
                  type="button"
                  onClick={() => copyText(dish.dishId)}
                  className="text-xs text-primary/60 truncate"
                >
                  ID блюда: {dish.dishId}
                </button>
              </ul>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </motion.div>
  );
};

const EmptyState = () => (
  <motion.div
    initial={{ opacity: 0, scale: 0.96 }}
    animate={{ opacity: 1, scale: 1 }}
    transition={{ duration: 0.3 }}
    className="m-2 flex-1 grid place-items-center text-center"
  >
    <div className="flex flex-col items-center gap-2 text-primary/60">
      <h3 className="font-manrope text-xl">Стоп-лист пуст</h3>
      <p className="text-sm">Все блюда доступны к заказу</p>
    </div>
  </motion.div>
);

export const StopList = () => {
  const [page, setPage] = useState(1);

  const { data, isPending, isError, isPlaceholderData } = useQuery({
    queryKey: ["stop-list", page],
    queryFn: async () => {
      const res = await api.get("/stop-list?page=" + page);
      return res.data as StopListData;
    },
    placeholderData: keepPreviousData,
  });

  const showLoader = useDelayedValue(isPending, { minDisplay: 300 });

  const pages = data ? Math.ceil(data.total / data.limit) : 1;
  const isEmpty = !!data && data.items.length === 0;

  const handleNextPage = () => page < pages && setPage(prev => prev + 1);
  const handlePrevPage = () => page > 1 && setPage(prev => prev - 1);

  return (
    <div className="flex flex-col h-full">
      <h1 className="bg-text text-bg p-2 text-2xl font-oswald shrink-0 flex gap-2 items-center justify-between">
        <span>Стоп-лист</span>
        <FaPause />
      </h1>

      {showLoader ? (
        <div className="flex-1 grid place-items-center">
          <FaCircleNotch size="20" className="animate-spin" />
        </div>
      ) : isError ? (
        <p className="m-2 text-err bg-err-bg p-2 rounded-xl text-center shrink-0">
          Не удалось загрузить данные
        </p>
      ) : isEmpty ? (
        <EmptyState />
      ) : (
        <div className="flex-1 min-h-0 overflow-auto flex flex-col gap-3">
          <AnimatePresence mode="wait">
            <motion.div
              key={page}
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -12 }}
              transition={{ duration: 0.2 }}
              className={cn("p-2 flex flex-col", isPlaceholderData && "opacity-50 transition-opacity")}
            >
              {data?.items.map((dish, index) => (
                <Dish dish={dish} key={dish.dishId} index={index} />
              ))}
            </motion.div>
          </AnimatePresence>

          <div className="flex p-2 sticky bottom-0 border-t border-primary pt-1 bg-bg items-center justify-between mt-auto">
            <span>Всего: {data?.total} ед.</span>

            {pages > 1 && (
              <>
                <span className="text-primary">Страница {page} из {pages}</span>
                <div className="flex gap-2">
                  <button
                    type="button"
                    className={cn(
                      "p-2 rounded-lg bg-secondary active:bg-primary transition-colors",
                      (page === 1 || isPlaceholderData) && "opacity-30"
                    )}
                    onClick={handlePrevPage}
                    disabled={page === 1 || isPlaceholderData}
                  >
                    <FaChevronLeft />
                  </button>
                  <button
                    type="button"
                    className={cn(
                      "p-2 rounded-lg bg-secondary active:bg-primary transition-colors",
                      (page === pages || isPlaceholderData) && "opacity-30"
                    )}
                    onClick={handleNextPage}
                    disabled={page === pages || isPlaceholderData}
                  >
                    <FaChevronRight />
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
};