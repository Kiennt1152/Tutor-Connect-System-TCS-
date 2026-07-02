import { APP_ROUTES } from '../constants/routes';

/** Chuyen huong sau dang nhap: admin ve dashboard, con lai giu from hoac trang chu. */
export function resolvePostLoginPath(from: string, role?: string): string {
  const normalizedFrom = from.split('?')[0].split('#')[0];
  const isDefaultEntry =
    normalizedFrom === '/' || normalizedFrom === '/login' || normalizedFrom === '/register';

  if (!isDefaultEntry) {
    return normalizedFrom;
  }

  if (role === 'PLATFORM_ADMIN') {
    return APP_ROUTES.platform;
  }

  return '/';
}
