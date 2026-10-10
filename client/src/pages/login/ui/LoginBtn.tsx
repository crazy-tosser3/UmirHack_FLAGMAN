import { AnimatePresence, motion } from "framer-motion";
import { useState } from "react"
import { FaDoorOpen } from "react-icons/fa";

const LoginBtn = () => {
  const [isHover, setIsHover] = useState(false);
  
  return (
    <button 
    className="bg-accent p-2 px-4 rounded-lg flex items-center justify-center gap-2 active:brightness-110"
    onMouseEnter={() => setIsHover(true)}
    onMouseLeave={() => setIsHover(false)}
    >
      <AnimatePresence mode="popLayout">
        {isHover && (
          <motion.i 
          key="ico"
          layout
          initial={{scale: 0.8, x: -20, opacity: 0}}
          animate={{scale: 1, x: 0, opacity: 1}}
          exit={{scale: 0.8, x: -20, opacity: 0}}
          >
            <FaDoorOpen />
          </motion.i>
        )}
        <motion.span key="label" layout>
          Войти
        </motion.span>
      </AnimatePresence>
    </button>
  )
}

export default LoginBtn