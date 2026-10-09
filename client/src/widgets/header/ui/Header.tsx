import { NavLink } from "react-router-dom"
import Navigation from "./Navigation"

export const Header = () => {
  return (
    <div className="border-b border-secondary p-4 mb-4 sticky top-0 bg-bg">
      <header className="content flex gap-4 justify-between items-center">
        <NavLink to={"/"} className="text-2xl font-oswald">Pantry</NavLink>
        <Navigation />
      </header>
    </div>
  )
}