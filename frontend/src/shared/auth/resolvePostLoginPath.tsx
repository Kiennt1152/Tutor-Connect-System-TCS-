import { defaultHomePathForRole } from './rbac';

/** Chuyển hướng sau đăng nhập: luôn về landing page theo role. */
export function resolvePostLoginPath(from: string, role?: string): string {
  void from;
  return defaultHomePathForRole(role);
}
