import { useAuth } from '@shared/auth/auth-service';
import { Navigate, Outlet } from 'react-router-dom';

export function ProtectedRoute() {
  const { isAuthenticated, loading } = useAuth();

  if (loading) {
    return <div></div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}