import { NavLink, useNavigate } from "react-router-dom"
import Navigation from "./Navigation"
import { useAuth } from "@shared/auth/auth-service"
import { IcoBtn } from "@shared/ico-text-btn"
import { FaUserCircle } from "react-icons/fa"

export const Header = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="py-4 mb-4 sticky top-0 bg-bg z-50">
      <header className="content flex gap-4 justify-between items-center">
        <NavLink to={"/"} className="text-2xl font-oswald rounded-lg">Pantry</NavLink>
        <Navigation />
        {user && (
          <div className="flex gap-2">
            <IcoBtn icon={<FaUserCircle />} onClick={() => navigate("/profile")}>{user.name}</IcoBtn>
          </div>
          )}
      </header>
    </div>
  )
}