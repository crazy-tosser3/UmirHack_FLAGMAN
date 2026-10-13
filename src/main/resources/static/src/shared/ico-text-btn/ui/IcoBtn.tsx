import { cn } from "@lib/utils";
import { AnimatePresence, motion } from "framer-motion";
import { useState } from "react"

interface IcoBtnProps {
  className?: string;
  icon: React.ReactNode;
  onClick: () => void;
  children: React.ReactNode;
}

const PADDING = 12;

export const IcoBtn = ({ className, icon, onClick, children }: IcoBtnProps) => {
  const [isHover, setIsHover] = useState(false);
  
  return (
    <motion.button 
    style={{paddingLeft: isHover ? PADDING : PADDING+12, paddingRight: isHover ? PADDING : PADDING+12}}
    className={cn("min-w-fit bg-secondary p-2 rounded-lg flex items-center justify-center gap-2 active:bg-primary transition-colors", className)}
    onMouseEnter={() => setIsHover(true)}
    onMouseLeave={() => setIsHover(false)}
    onClick={onClick}
    >
      <AnimatePresence mode="popLayout">
        {isHover && (
          <motion.i 
          key="ico"
          layout
          initial={{scale: 0.8, x: -10, opacity: 0}}
          animate={{scale: 1, x: 0, opacity: 1}}
          exit={{scale: 0.8, x: -10, opacity: 0}}
          >
            {icon}
          </motion.i>
        )}
        <motion.span key="label" layout>
          {children}
        </motion.span>
      </AnimatePresence>
    </motion.button>
  )
}