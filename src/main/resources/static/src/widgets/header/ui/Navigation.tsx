import { navigation } from "@lib/config"
import { useAuth } from "@shared/auth/auth-service";
import { useDelayedValue } from "@shared/hooks/use-delayed-value";
import { AnimatePresence, motion } from "framer-motion";
import { Fragment, useState } from "react"
import { FaChevronDown } from "react-icons/fa6";
import { NavLink, useLocation } from "react-router-dom";

const Navigation = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [curCategory, setCurCategory] = useState(0);

  const location = useLocation();

  const delayedOpen = useDelayedValue(isOpen, {
    delay: 0,
    minDisplay: 1000
  });

  const { user } = useAuth();

  return (
    <div className="flex flex-col gap-2 md:absolute left-1/2 md:-translate-x-1/2"
    onPointerEnter={() => setIsOpen(true)}
    onPointerLeave={() => setIsOpen(false)}
    >
      <nav className="flex flex-col md:flex-row">
        {navigation.map((navs, index) => (
          <Fragment key={navs.categoryName}>
            {user && navs.role?.includes(user.role) && (
              <button
              onPointerEnter={() => {
                setCurCategory(index);
              }}
              onClick={() => {
                setCurCategory(index);
                setIsOpen(prev => !prev);
              }}
              className="flex gap-2 items-center p-1 px-2 rounded-lg"
              >
                {navs.categoryName}
                <FaChevronDown />
              </button>
            )}
          </Fragment>
        ))}
      </nav>
      <AnimatePresence>
        {delayedOpen && (
          <motion.div 
          className="max-w-screen w-max absolute left-1/2 bottom-0 -translate-x-1/2 translate-y-full p-2 rounded-xl bg-bg/50 backdrop-blur-md overflow-hidden"
          initial={{opacity: 0}}
          animate={{opacity: 1}}
          exit={{opacity: 0}}
          transition={{type: "spring", stiffness: 120 , damping: 15}}
          >
            <AnimatePresence mode="wait">
              <motion.nav 
              className="m-auto flex gap-4 justify-between flex-wrap"
              initial={{opacity: 0, x: 30}}
              animate={{opacity: 1, x: 0}}
              exit={{opacity: 0, x: -30}}
              key={curCategory}
              transition={{type: "spring", stiffness: 200, damping: 25}}
              >
                {navigation[curCategory].navs.primary?.map((nav) => (
                  <NavLink 
                  to={nav.path} 
                  className="bg-bg h-32 p-2 border border-primary rounded-lg flex flex-col gap-2 relative overflow-hidden aspect-video"
                  key={nav.path}
                  >
                    <h2 className="text-2xl font-manrope relative z-10">{nav.title}</h2>
                    {location.pathname.startsWith(nav.path) && <span className="text-xs p-1 px-2 bg-success-bg text-success w-fit rounded-full">Уже здесь</span>}
                    <nav.ico size={128} className="text-accent absolute translate-x-1/5 translate-y-1/5 right-0 bottom-0" />
                  </NavLink>
                ))}
                {navigation[curCategory].navs.secondary?.map((nav) => (
                  <NavLink 
                  to={nav.path} 
                  className="bg-bg h-32 p-2 aspect-square border border-primary rounded-lg flex flex-col items-center justify-center gap-2 relative overflow-hidden"
                  key={nav.path}
                  >
                    <nav.ico size={32} className="text-accent" />
                    <h2 className="font-manrope relative z-10">{nav.title}</h2>
                    {location.pathname.startsWith(nav.path) && <span className="text-xs p-1 px-2 bg-success-bg text-success w-fit rounded-full">Уже здесь</span>}
                  </NavLink>
                ))}
              </motion.nav>
            </AnimatePresence>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}

export default Navigation