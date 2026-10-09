import { cn } from "@lib/utils"
import { AnimatePresence, motion } from "framer-motion"
import { useState } from "react"

interface PushBtnProps{
  className?: string;
  onClick: () => void;
  children: React.ReactNode;
}
export const TextPushBtn = ({className="", onClick, children}:PushBtnProps) => {
  const [isHover, setIsHover] = useState(false);
  
  return (
    <button 
    className={cn("overflow-hidden", className)} 
    onPointerEnter={() => setIsHover(true)}
    onPointerLeave={() => setIsHover(false)}
    onClick={onClick}
    >
      <AnimatePresence mode="wait">
        <motion.div
        key={+isHover}
        initial={{y: 30, opacity: 0, scale: 0.1}}
        animate={{y: 0, opacity: 1, scale: 1}}
        exit={{y: -30, opacity: 0, scale: 0.1}}
        transition={{type: 'tween', duration: .3}}
        >
          {children}
        </motion.div>
      </AnimatePresence>
    </button>
  )
}