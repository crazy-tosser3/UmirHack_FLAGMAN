import api from "@lib/api";
import { useDelayedValue } from "@shared/hooks/use-delayed-value";
import { useQuery } from "@tanstack/react-query";
import { FaChevronLeft, FaChevronRight, FaCircleNotch } from "react-icons/fa";
import { FaChevronDown, FaPause } from "react-icons/fa6";
import type { StopListData, StopListDish } from "../model/types";
import { useState } from "react";
import { cn, copyText, units } from "@lib/utils";

const Dish = ({ dish }: { dish: StopListDish }) => {
  const [showDetails, setShowDetails] = useState(false);

  return (
    <div className="flex gap-2 min-w-0">
      <div className="flex flex-col gap-1 min-w-0 flex-1">
        <p className="font-manrope truncate">{dish.dishName}</p>
        
        <button 
          type="button"
          className="text-primary flex items-center justify-between gap-2 p-1 px-2 rounded-lg hover:bg-secondary transition-colors"
          onClick={() => setShowDetails(prev => !prev)}
        >
          <span>Подробнее</span>
          <FaChevronDown className={cn("transition-transform", showDetails ? "rotate-180" : "")} />
        </button>
        
        {showDetails && (
          <ul className="p-2 border rounded-md border-primary flex flex-col gap-2">
            {dish.reasons.map((reason, index) => (
              <li key={index} className="flex gap-4 items-center justify-between">
                <span>{reason.ingredientName}</span>
                <span className="bg-err-bg text-err px-2 rounded-full tabular-nums">{reason.availableQuantity} / {reason.requiredQuantity} {units[reason.unit]}.</span>
              </li>
            ))}
            
            <button onClick={() => copyText(dish.dishId)} className="text-xs text-primary/60 truncate">ID блюда: {dish.dishId}</button>
          </ul>
        )}
      </div>
    </div>
  );
};

export const StopList = () => {
  const [page, setPage] = useState(1);

  const { data, isPending, isError } = useQuery({
    queryKey: ["stop-list", page],
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
    <div className="flex flex-col h-full gap-2">
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
          {data?.total && (
            <>
              <div className="flex flex-col">
                {data.items.map((dish: StopListDish) => (
                  <Dish dish={dish} key={dish.dishId}/>
                ))}
              </div>
              <div className="flex sticky bottom-0 border-t border-primary pt-1 bg-bg items-center justify-between mt-auto">
                <span>Всего: {data.total} ед.</span>
                {pages > 1 && (
                  <>
                    <span className="text-primary">Страница {page} из {pages}</span>
                    <div className="flex gap-2">
                      <button 
                      className={cn("p-2 rounded-lg bg-secondary active:bg-primary transition-colors", page === 1 && "opacity-30" )}
                      onClick={handlePrevPage}
                      >
                        <FaChevronLeft />
                      </button>
                      <button 
                      className={cn("p-2 rounded-lg bg-secondary active:bg-primary transition-colors", page === pages && "opacity-30" )}
                      onClick={handleNextPage}
                      >
                        <FaChevronRight />
                      </button>
                    </div>
                  </>
                )}
            </div>
            </>
          )}
        </div>
      )}
    </div>
  );
};