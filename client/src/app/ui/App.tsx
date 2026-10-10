import { LoginPage } from "@pages/login";
import { NotFoundPage } from "@pages/not-found";
import { useAuth } from "@shared/auth/auth-service";
import { ProtectedRoute } from "@shared/auth/protected-route";
import { useDelayedValue } from "@shared/hooks/use-delayed-value";
import { Footer } from "@widgets/footer";
import { Header } from "@widgets/header";
import { AnimatePresence, motion } from "framer-motion";
import { FaCircleNotch } from "react-icons/fa";
import { Navigate, Outlet, Route, Routes, useLocation } from "react-router-dom";

const AppLayout = () => {

  const { loading } = useAuth();
  const showLoader = useDelayedValue(loading, {minDisplay: 500})

  const location = useLocation();

  return (
    <>
      <div className="min-h-screen">
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
                <motion.div 
                key={location.pathname}
                className="content"
                initial={{y: 50, opacity: 0}}
                animate={{y: 0, opacity: 1}}
                exit={{y: 50, opacity: 0}}
                >
                  <Outlet />
                </motion.div>
              )}
            </>
            )}
          </AnimatePresence>
      </div>
      <Footer />
    </>
  )
}

const App = () => {

  return (
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<ProtectedRoute />}> 
            <Route index element={<Navigate to="/dashboard" />} />
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
  )
}

export default App
