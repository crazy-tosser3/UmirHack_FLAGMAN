import { useAuth } from '@shared/auth/auth-service';
import { Navigate, Outlet, useLocation } from 'react-router-dom';

export function ProtectedRoute() {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return null;
  }

  if (!isAuthenticated) {
    const redirect = location.pathname + location.search;

    return <Navigate to={`/login?redirect=${encodeURIComponent(redirect)}`} replace />;
  }

  return <Outlet />;
}