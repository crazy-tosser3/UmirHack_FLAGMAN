import api from "@lib/api";
import { cn, units } from "@lib/utils";
import { useDelayedValue } from "@shared/hooks/use-delayed-value";
import { useQuery } from "@tanstack/react-query";
import { FaCircleNotch, FaClock } from "react-icons/fa";
import dayjs from "dayjs";
import relativeTime from "dayjs/plugin/relativeTime";
import "dayjs/locale/ru";
import { AnimatePresence, motion } from "framer-motion";

dayjs.extend(relativeTime);
dayjs.locale('ru');

interface ExpiringBatch {
  batchId: string;
  ingredientName: string;
  remainingQuantity: number;
  expiresAt: string;
  remainingShelfLifeSeconds: number;
  ingredientId: string;
}

const ExpiringBatchItem = ({ batch }: { batch: ExpiringBatch }) => {
  const expiresAt = dayjs(batch.expiresAt);
  const timeRemaining = dayjs().to(expiresAt);

  return (
    <div className="flex flex-col gap-1 min-w-0 p-2 rounded-lg border border-primary">
      <div className="flex justify-between items-start gap-2">
        <p className="truncate flex-1">{batch.ingredientName}</p>
        <span className="text-sm tabular-nums whitespace-nowrap">
          {batch.remainingQuantity} {units["g"]}
        </span>
      </div>
      <div className="flex justify-between items-center text-sm text-primary">
        <span className="truncate">Годен до: {expiresAt.format('DD.MM HH:mm')}</span>
        <span className={cn(
          "px-2 rounded-full",
          batch.remainingShelfLifeSeconds < 3600 * 3 ? "text-err bg-err-bg" : "text-warn bg-warn-bg"
        )}>
          {timeRemaining}
        </span>
      </div>
    </div>
  );
};

const ExpiringBatches = () => {
  const { data, isPending, isError } = useQuery({
    queryKey: ["expiring-batches"],
    queryFn: async () => {
      const res = await api.get("/stock/batches?expiryStatus=expiring&limit=15");
      return res.data;
    }
  });

  const showLoader = useDelayedValue(isPending, { minDisplay: 300 });
  const isEmpty = data?.items?.length === 0;

  return (
    <div className="flex flex-col h-full">
      <h1 className="p-2 bg-text text-bg text-2xl font-oswald shrink-0 flex gap-2 items-center justify-between">
        <span>Истекают сроки</span>
        <FaClock />
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
        <div className="flex-1 grid place-items-center text-center p-4">
          <div className="flex flex-col items-center gap-2 text-primary/60">
            <h3 className="font-manrope text-xl">Все свежее</h3>
            <p className="text-sm">Нет партий с истекающим сроком годности</p>
          </div>
        </div>
      ) : (
        <div className="flex-1 min-h-0 overflow-auto flex flex-col gap-2">
          
          <AnimatePresence mode="wait">
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -12 }}
              transition={{ duration: 0.2 }}
              className="p-2 flex flex-col gap-2"
            >
              {data?.items.map((batch: ExpiringBatch) => (
                <ExpiringBatchItem batch={batch} key={batch.batchId} />
              ))}
            </motion.div>
          </AnimatePresence>

          <div className="flex p-2 sticky bottom-0 border-t border-primary pt-1 bg-bg items-center justify-between mt-auto">
            Всего: {data?.total} ед.
          </div>
        </div>
      )}
    </div>
  );
};

export default ExpiringBatches;
