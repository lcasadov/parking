import { type ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { Spinner } from '../components/Spinner';
import { ROUTES, type Role } from '../routes/paths';
import { useAuth } from './useAuth';

interface ProtectedRouteProps {
  children: ReactNode;
  requiredRole?: Role;
}

// Guard de ruta: exige sesion, respeta passwordMustChange y (opcional) rol.
export function ProtectedRoute({ children, requiredRole }: ProtectedRouteProps) {
  const { user, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <Spinner />;
  }

  if (!user) {
    return <Navigate to={ROUTES.login} replace />;
  }

  if (user.passwordMustChange && location.pathname !== ROUTES.changePassword) {
    return <Navigate to={ROUTES.changePassword} replace />;
  }

  if (requiredRole && user.role !== requiredRole) {
    return <Navigate to={ROUTES.login} replace />;
  }

  return <>{children}</>;
}
