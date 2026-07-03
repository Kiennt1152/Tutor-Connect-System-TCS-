import { APP_ROUTES } from '../constants/routes';
import { defaultHomePathForRole } from './rbac';

/** Chuyển hướng sau đăng nhập: admin → dashboard, còn lại về trang chủ (hoặc giữ deep-link). */
export function resolvePostLoginPath(from: string, role?: string): string {
  const normalizedFrom = from.split('?')[0].split('#')[0];
  const isDefaultEntry =
    normalizedFrom === '/' ||
    normalizedFrom === APP_ROUTES.login ||
    normalizedFrom === APP_ROUTES.register;

  if (!isDefaultEntry) {
    return normalizedFrom;
  }

  return defaultHomePathForRole(role);
}
