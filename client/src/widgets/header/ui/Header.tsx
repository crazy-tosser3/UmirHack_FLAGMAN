import { NavLink } from "react-router-dom"
import Navigation from "./Navigation"
import { motion, useScroll, useTransform } from "framer-motion"

export const Header = () => {
  const { scrollY } = useScroll();
  return (
    <div className="py-4 mb-4 sticky top-0 bg-bg">
      <motion.div style={{opacity: useTransform(scrollY, [0, 100], [0, 1])}} className="-z-1 absolute inset-0 shadow-md" />
      <header className="content flex gap-4 justify-between items-center">
        <NavLink to={"/"} className="text-2xl font-oswald rounded-lg">Pantry</NavLink>
        <Navigation />
      </header>
    </div>
  )
}