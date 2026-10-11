import api from "@lib/api";
import { useDelayedValue } from "@shared/hooks/use-delayed-value";
import { useQuery } from "@tanstack/react-query";
import { FaChevronLeft, FaChevronRight, FaCircleNotch } from "react-icons/fa";
import { FaPause } from "react-icons/fa6";
import type { StopListData } from "../model/types";
import { useState } from "react";

export const StopList = () => {
  const [page, setPage] = useState(1);

  const { data, isPending, isError } = useQuery({
    queryKey: ["stop-list"],
    queryFn: async () => {
      const res = await api.get("/stop-list?page="+page);

      return res.data as StopListData;
    }
  });

  const showLoader = useDelayedValue(isPending, { minDisplay: 300 });

  const pages = data ? Math.ceil(data.total / data.limit) : 1;

  function handleNextPage(){
    if (page < pages) setPage(prev => prev+1);
  }

  function handlePrevPage(){
    if (page > 1) setPage(prev => prev-1);
  }

  return (
    <div className="flex flex-col h-full gap-2"> {/* ← h-full */}
      <h1 className="text-2xl font-oswald shrink-0 flex gap-2 items-center justify-between">
        <span>Стоп-лист</span>
        <FaPause />
      </h1>
      
      {showLoader ? (
        <div className="flex-1 grid place-items-center">
          <FaCircleNotch size="20" className="animate-spin" />
        </div>
      ) : isError ?  (
        <p className="text-err bg-err-bg p-2 rounded-xl text-center shrink-0">
          Не удалось загрузить данные
        </p>
      ) : (
        <div className="flex-1 min-h-0 overflow-auto flex flex-col gap-3">
          {data?.total && <div className="flex items-center justify-between mt-auto">
            <span>Всего: {data.total}</span>
            {pages > 1 && (
              <>
                <span>Страница {data.total ?? 1} из {pages}</span>
                <div className="flex gap-2">
                  <button 
                  className="p-2 rounded-lg bg-secondary active:bg-primary transition-colors" 
                  onClick={handlePrevPage}
                  >
                    <FaChevronLeft />
                  </button>
                  <button 
                  className="p-2 rounded-lg bg-secondary active:bg-primary transition-colors" 
                  onClick={handleNextPage}
                  >
                    <FaChevronRight />
                  </button>
                </div>
              </>
            )}
          </div>}
        </div>
      )}
    </div>
  );
};