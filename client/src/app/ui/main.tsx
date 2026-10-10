import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '@app/styles/index.css'
import App from './App.tsx'
import { BrowserRouter } from 'react-router-dom'
import { HelmetProvider } from 'react-helmet-async';
import { AuthProvider } from '@shared/auth/auth-service'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

const client = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,
      gcTime: 1000 * 60 * 10,
      retry: 2,
      refetchOnWindowFocus: false,
    },
  },
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={client}>
      <BrowserRouter>
        <HelmetProvider>
          <AuthProvider>
            <App />
          </AuthProvider>
        </HelmetProvider>
      </BrowserRouter>
    </QueryClientProvider>    
  </StrictMode>,
)
