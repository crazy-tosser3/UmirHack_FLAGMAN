import api from "@lib/api"
import type { Unit } from "@lib/types"
import { cn, copyText } from "@lib/utils"
import { useDelayedValue } from "@shared/hooks/use-delayed-value"
import { useQuery } from "@tanstack/react-query"
import { useState } from "react"
import { FaCircleNotch } from "react-icons/fa"
import { FaChevronDown } from "react-icons/fa6"

interface Reason {
  availableQuantity: number;
  expiresAt: string;
  explanation: string;
  ingredientId: string;
  ingredientName: string;
  unit: Unit;
}

interface Dish {
  availablePortions: number;
  calculatedAt: string;
  dishId: string;
  dishName: string
  priority: number;
  reasons: Reason[];
}

const HotOffer = ({ dish }: { dish: Dish }) => {
  const [showDetails, setShowDetails] = useState(false);

  return (
    <div className="flex gap-2 min-w-0">
      <div className={cn(
        "shrink-0 w-12 h-12 grid place-items-center rounded-xl text-2xl font-oswald tabular-nums",
        getPriorityClass(dish.priority)
      )}>
        {Math.floor(dish.priority)}
      </div>
      
      <div className="flex flex-col gap-1 min-w-0 flex-1">
        <p className="font-manrope truncate">{dish.dishName}</p>
        
        <button 
          type="button"
          className="flex items-center justify-between gap-2 p-1 px-2 rounded-lg hover:bg-secondary transition-colors"
          onClick={() => setShowDetails(prev => !prev)}
        >
          <span>Подробнее</span>
          <FaChevronDown className={cn("transition-transform", showDetails ? "rotate-180" : "")} />
        </button>
        
        {showDetails && (
          <>
            {dish.reasons.map((reason) => (
              <li>
                <h3 className="text-xl font-manrope">{reason.ingredientName}</h3>
                <p>{reason.explanation}</p>
              </li>
            ))}
            <button onClick={() => copyText(dish.dishId)} className="text-xs text-primary/60 truncate">ID: {dish.dishId}</button>
          </>
        )}
      </div>
    </div>
  );
};

const HotOffers = () => {
  const { data, isPending, isError } = useQuery({
    queryKey: ["hot-offers"],
    queryFn: async () => {
      const res = await api.get("/offers?limit=10&minPriority=30");
      return res.data;
    }
  });

  const showLoader = useDelayedValue(isPending, { minDisplay: 300 });

  return (
    <div className="flex flex-col h-full gap-2"> {/* ← h-full */}
      <h1 className="text-2xl font-oswald shrink-0">Горячие предложения</h1>
      
      {showLoader ? (
        <div className="flex-1 grid place-items-center">
          <FaCircleNotch size="20" className="animate-spin" />
        </div>
      ) : isError ? (
        <p className="text-err bg-err-bg p-2 rounded-xl text-center shrink-0">
          Не удалось загрузить данные
        </p>
      ) : (
        <div className="flex-1 min-h-0 overflow-auto flex flex-col gap-3">
          {data?.items.map((dish: Dish, index: number) => (
            <HotOffer dish={dish} key={index} />
          ))}
        </div>
      )}
    </div>
  );
};

function getPriorityClass(priority: number){
  if (priority > 80) return "bg-err-bg text-err";
  if (priority < 60) return "bg-success-bg text-success";
  return "bg-warn-bg text-warn";
}

export default HotOffers