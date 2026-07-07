export type UserRole = 'CLIENT' | 'TUTOR' | 'TUTOR_CENTER' | 'PLATFORM_ADMIN' | 'UNKNOWN';

export const USER_ROLES: UserRole[] = [
  'CLIENT',
  'TUTOR',
  'TUTOR_CENTER',
  'PLATFORM_ADMIN',
  'UNKNOWN',
];

export const BUSINESS_ROLES: UserRole[] = ['CLIENT', 'TUTOR', 'TUTOR_CENTER', 'PLATFORM_ADMIN'];

export function normalizeRole(role?: string | null): UserRole {
  if (role && USER_ROLES.includes(role as UserRole)) {
    return role as UserRole;
  }
  return 'UNKNOWN';
}

export function hasAnyRole(role: string | undefined | null, allowed: readonly UserRole[]): boolean {
  return allowed.includes(normalizeRole(role));
}

export function hasRole(role: string | undefined | null, expected: UserRole): boolean {
  return normalizeRole(role) === expected;
}

/** Default landing path after login when no deep-link is preserved. */
export function defaultHomePathForRole(role?: string | null): string {
  if (normalizeRole(role) === 'PLATFORM_ADMIN') {
    return '/platform';
  }
  return '/';
}
