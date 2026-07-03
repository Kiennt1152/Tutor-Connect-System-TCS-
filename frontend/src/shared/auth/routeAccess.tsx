import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import type { UserRole } from './rbac';
import { hasAnyRole } from './rbac';
import { useAuth } from './AuthProvider';
import { APP_ROUTES } from '../constants/routes';

export type RouteAccess =
  | { type: 'public' }
  | { type: 'authenticated' }
  | { type: 'roles'; roles: UserRole[] };

export const ROUTE_ACCESS: Record<string, RouteAccess> = {
  '/': { type: 'public' },
  '/login': { type: 'public' },
  '/register': { type: 'public' },
  '/forbidden': { type: 'public' },
  '/profile': { type: 'roles', roles: ['CLIENT', 'TUTOR', 'TUTOR_CENTER', 'PLATFORM_ADMIN'] },
  '/finance': { type: 'roles', roles: ['CLIENT', 'TUTOR', 'TUTOR_CENTER'] },
  '/catalog': { type: 'public' },
  '/marketplace': { type: 'roles', roles: ['CLIENT', 'TUTOR', 'TUTOR_CENTER'] },
  '/contract': { type: 'roles', roles: ['CLIENT', 'TUTOR', 'TUTOR_CENTER'] },
  '/messaging': { type: 'roles', roles: ['CLIENT', 'TUTOR', 'TUTOR_CENTER', 'PLATFORM_ADMIN'] },
  '/center': { type: 'roles', roles: ['TUTOR_CENTER'] },
  '/identity/verification': { type: 'roles', roles: ['TUTOR'] },
  '/platform': { type: 'roles', roles: ['PLATFORM_ADMIN'] },
  '/platform/users': { type: 'roles', roles: ['PLATFORM_ADMIN'] },
  '/platform/verifications': { type: 'roles', roles: ['PLATFORM_ADMIN'] },
  '/platform/reports': { type: 'roles', roles: ['PLATFORM_ADMIN'] },
  '/platform/profile': { type: 'roles', roles: ['PLATFORM_ADMIN'] },
};

export function getRouteAccess(path: string): RouteAccess {
  const normalized = path.split('?')[0].split('#')[0];
  return ROUTE_ACCESS[normalized] ?? { type: 'authenticated' };
}

/**
 * Route guard driven by `getRouteAccess`.
 * - `public`            : pass-through
 * - `authenticated`     : require login (redirect to /login)
 * - `roles`             : require login AND any of the listed roles (redirect to /forbidden)
 */
export function AccessGuard({
  access,
  children,
}: {
  readonly access: RouteAccess;
  readonly children: ReactNode;
}) {
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();

  if (access.type === 'public') {
    return <>{children}</>;
  }

  if (!isAuthenticated) {
    return <Navigate to={APP_ROUTES.login} state={{ from: location.pathname }} replace />;
  }

  if (access.type === 'roles' && !hasAnyRole(user?.role, access.roles)) {
    return <Navigate to={APP_ROUTES.forbidden} replace />;
  }

  return <>{children}</>;
}