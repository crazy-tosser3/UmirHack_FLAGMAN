import { cn } from "@lib/utils";
import { AnimatePresence, motion } from "framer-motion";
import { FaCheck, FaExclamationTriangle, FaInfoCircle } from "react-icons/fa";
import { useToastStore } from "../model/ToastStore";

const statusConfig = {
  error: {
    icon: <FaExclamationTriangle />,
    classes: "text-err bg-err-bg",
  },
  info: {
    icon: <FaInfoCircle />,
    classes: "text-warn bg-warn-bg",
  },
  success: {
    icon: <FaCheck />,
    classes: "text-success bg-success-bg",
  },
};

type ToastProps = {
  id: string;
  msg: string;
  status?: keyof typeof statusConfig;
};

const Toast = ({ id, msg, status = "info" }: ToastProps) => {
  const removeToast = useToastStore((state) => state.removeToast);
  
  const config = statusConfig[status] || statusConfig.info;

  return (
    <motion.button
    role="alert"
    aria-live="polite" 
    layout
    className={cn("flex gap-2 items-center p-2 px-4 rounded-xl max-w-75 bg-foreground-top", config.classes)}
    initial={{ opacity: 0, y: 50, scale: 0.95 }}
    animate={{ opacity: 1, y: 0, scale: 1 }}
    exit={{ opacity: 0, x: 100, scale: 0.95 }}
    onClick={() => removeToast(id)}
    >
      {config.icon}
      <span className="text-left">{msg}</span>
    </motion.button>
  )
}

export const Toasts = () => {
  const toasts = useToastStore((state) => state.toasts);

  return (
    <aside 
    className={cn("flex flex-col gap-2 fixed bottom-4 right-4 z-45")}
    style={{ 
      bottom: 'max(1rem, env(safe-area-inset-bottom) + 1rem)' 
    }}>
      <AnimatePresence mode="popLayout">
        {toasts.slice(-3).map((toast) => (
          <Toast key={toast.id} id={toast.id} msg={toast.msg} status={toast.status} />
        ))}
      </AnimatePresence>
      {toasts.length > 3 && <span className="p-2 px-4 text-primary rounded-xl bg-secondary ml-auto">+{toasts.length-3}</span>}
    </aside>
  )
}