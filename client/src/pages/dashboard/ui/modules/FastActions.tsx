import { FaShoppingCart } from "react-icons/fa";
import { FaBox } from "react-icons/fa6"
import { useNavigate } from "react-router-dom"

const FastActions = () => {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col gap-4 p-2 h-full">
      <button onClick={() => navigate('/deliveries/create')} className="bg-accent flex-1 p-4 rounded-lg relative overflow-hidden flex hover:brightness-110 active:scale-90 transition-transform">
        <FaBox size={128} className="max-h-full absolute -right-4 -bottom-4 text-bg/40" />
        <span className="text-xl font-manrope mr-auto">Новая поставка</span>
      </button>
      <button onClick={() => navigate('/dashboard/orders/new')} className="bg-accent flex-1 p-4 rounded-lg relative overflow-hidden flex hover:brightness-110 active:scale-90 transition-transform">
        <FaShoppingCart size={128} className="max-h-full absolute -right-4 -bottom-4 text-bg/40" />
        <span className="text-xl font-manrope mr-auto">Новый заказ</span>
      </button>
    </div>
  )
}

export default FastActions