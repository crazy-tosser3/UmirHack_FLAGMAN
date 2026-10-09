import { NotFoundPage } from "@pages/not-found";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Footer } from "@widgets/footer";
import { Header } from "@widgets/header";
import { Outlet, Route, Routes } from "react-router-dom";

const client = new QueryClient();

const AppLayout = () => {
  return (
    <>
      <div className="min-h-screen">
        <Header />
        <main className="content">
          <Outlet />
        </main>
      </div>
      <Footer />
    </>
  )
}

const App = () => {

  return (
    <QueryClientProvider client={client}>
      <Routes>
        <Route element={<AppLayout />}>
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </QueryClientProvider>
  )
}

export default App
