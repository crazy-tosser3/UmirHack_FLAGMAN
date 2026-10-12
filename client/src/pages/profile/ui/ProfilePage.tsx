import { copyText } from "@lib/utils";
import { useAuth } from "@shared/auth/auth-service"
import { IcoBtn } from "@shared/ico-text-btn";
import { FaDoorOpen, FaUser } from "react-icons/fa";
import { FaCopy } from "react-icons/fa6";

export const ProfilePage = () => {

  const { user, logout } = useAuth();

  if (!user) return null;

  return (
    <div className="bg-bg flex flex-col gap-4 border border-primary shadow-md p-4 rounded-xl overflow-y-auto">
      <FaUser size={48} className="h-16 w-16 m-auto p-2 bg-secondary rounded-lg"/>
      <h1 className="font-oswald text-4xl text-center">{user.name}</h1>
      <h2 className="font-manrope text-xl text-center">{userRole(user.role)}</h2>
      <h2 className="text-xl font-manrope border-b border-primary">Личная информация:</h2>
      <div className="flex gap-2 items-center">
        <span className="text-primary">Email:</span>
        <span className="truncate">{user.email}</span>
        <button className="ml-auto md:ml-0 p-2 bg-secondary rounded-lg hover:scale-110 active:scale-90 transition-transform" onClick={() => copyText(user.email)}>
          <FaCopy />
        </button>
      </div>
      <div className="flex gap-2 items-center">
        <span className="text-primary">ID:</span>
        <span className="truncate">{user.id}</span>
        <button className="ml-auto md:ml-0 p-2 bg-secondary rounded-lg hover:scale-110 active:scale-90 transition-transform" onClick={() => copyText(user.email)}>
          <FaCopy />
        </button>
      </div>
      <IcoBtn className="w-fit bg-accent active:bg-accent active:brightness-110" onClick={() => logout()} icon={<FaDoorOpen />}>Выйти</IcoBtn>
    </div>
  )
}

const userRole = (role: string) => {
  switch (role){
    case "employee": return "Сотрудник";
    case "manager": return "Менеджер";
    case "admin": return "Администратор";
  }
}