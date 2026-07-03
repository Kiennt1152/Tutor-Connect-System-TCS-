import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from './AuthProvider';
import { hasAnyRole } from './rbac';
import type { UserRole } from '../types/userRole';
import { APP_ROUTES } from '../constants/routes';

type ProtectedRouteProps = {
  children: ReactNode;
  /** If omitted, any authenticated user is allowed. */
  roles?: UserRole[];
};

export function ProtectedRoute({ children, roles }: ProtectedRouteProps) {
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to={APP_ROUTES.login} state={{ from: location.pathname }} replace />;
  }

  if (roles && !hasAnyRole(user?.role, roles)) {
    return <Navigate to={APP_ROUTES.forbidden} replace />;
  }

  return <>{children}</>;
}
