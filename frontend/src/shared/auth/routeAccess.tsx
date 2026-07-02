import type { UserRole } from './rbac';

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
  '/catalog': { type: 'roles', roles: ['PLATFORM_ADMIN'] },
  '/marketplace': { type: 'roles', roles: ['CLIENT', 'TUTOR', 'TUTOR_CENTER'] },
  '/contract': { type: 'roles', roles: ['CLIENT', 'TUTOR', 'TUTOR_CENTER'] },
  '/messaging': { type: 'roles', roles: ['CLIENT', 'TUTOR', 'TUTOR_CENTER', 'PLATFORM_ADMIN'] },
  '/center': { type: 'roles', roles: ['TUTOR_CENTER'] },
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
