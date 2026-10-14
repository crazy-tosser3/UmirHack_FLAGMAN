import { useManualStop } from "@shared/stop-list";
import { FaPause, FaPlay } from "react-icons/fa6";

export const StopToggle = ({ dishId, stopped }: { dishId: string; stopped: boolean }) => {
  const manualStop = useManualStop();

  return (
    <button
      type="button"
      title={stopped ? "Вернуть в меню" : "Поставить в стоп-лист"}
      disabled={manualStop.isPending}
      onClick={() => manualStop.mutate({ dishId, stop: !stopped })}
      className={
        stopped
          ? "mx-auto grid size-8 place-items-center rounded-lg border border-warn bg-warn-bg text-warn transition-colors hover:bg-success-bg hover:text-success hover:border-success disabled:opacity-40"
          : "mx-auto grid size-8 place-items-center rounded-lg border border-primary transition-colors hover:bg-err-bg hover:text-err disabled:opacity-40"
      }
    >
      {stopped ? <FaPlay size={11} /> : <FaPause size={11} />}
    </button>
  );
};
