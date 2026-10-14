import { pages } from "@lib/config";
import { LoginPage } from "@pages/login";
import { NotFoundPage } from "@pages/not-found";
import { RegisterPage } from "@pages/register";
import { useAuth } from "@shared/auth/auth-service";
import { ProtectedRoute } from "@shared/auth/protected-route";
import { useDelayedValue } from "@shared/hooks/use-delayed-value";
import { Toasts } from "@shared/toasts";
import { ScrollToTop } from "@shared/scroll-to-top";
import { Header } from "@widgets/header";
import { AnimatePresence, motion } from "framer-motion";
import { FaCircleNotch } from "react-icons/fa";
import { Navigate, Outlet, Route, Routes, useLocation } from "react-router-dom";

const AppLayout = () => {

  const { loading } = useAuth();
  const showLoader = useDelayedValue(loading, {minDisplay: 500})

  const location = useLocation();

  return (
    <div className="min-h-dvh flex flex-col sm:h-screen sm:overflow-hidden">
      <Toasts />
      <ScrollToTop />
      <Header />
        <AnimatePresence mode="wait">
          {showLoader ? (
            <motion.i
            key="loader"
            initial={{opacity: 0}}
            animate={{opacity: 1}}
            exit={{opacity: 0}}
            >
              <FaCircleNotch 
              size="20" 
              className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 animate-spin duration-700 m-auto"
              />
            </motion.i>
          ) : (
          <>
            {loading || (
              <motion.main 
              key={location.pathname.split('/')[1]}
              className="min-h-0 content flex flex-col flex-1 w-full py-2"
              initial={{y: 50, opacity: 0}}
              animate={{y: 0, opacity: 1}}
              exit={{y: 50, opacity: 0}}
              transition={{type: "spring", damping: 20}}
              >
                <Outlet />
              </motion.main>
            )}
          </>
          )}
        </AnimatePresence>
    </div>
  )
}

const App = () => {

  return (
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route element={<ProtectedRoute />}> 
            <Route index element={<Navigate to="/dashboard" />} />
            {pages.map((page, index) => (
              <Route key={index} path={page.path} element={page.element}>{page.nested}</Route>
            ))}
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
  )
}

export default App
