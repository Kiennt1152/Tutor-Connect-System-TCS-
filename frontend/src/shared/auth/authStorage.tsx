const TOKEN_KEY = 'tcs_access_token';
const USER_KEY = 'tcs_user';
const SESSION_EXPIRES_AT_KEY = 'tcs_session_expires_at';

import type { UserRole } from '../types/userRole';

export type StoredUser = {
  userId: number;
  email: string;
  role: UserRole;
  displayName: string;
};

export const authStorage = {
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  },
  setToken(token: string) {
    localStorage.setItem(TOKEN_KEY, token);
  },
  clearToken() {
    localStorage.removeItem(TOKEN_KEY);
  },
  getUser(): StoredUser | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as StoredUser;
    } catch {
      return null;
    }
  },
  setUser(user: StoredUser) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  clearUser() {
    localStorage.removeItem(USER_KEY);
  },
  setSessionExpiresAt(expiresAtMs: number) {
    localStorage.setItem(SESSION_EXPIRES_AT_KEY, String(expiresAtMs));
  },
  getSessionExpiresAt(): number | null {
    const raw = localStorage.getItem(SESSION_EXPIRES_AT_KEY);
    const stored = raw ? Number(raw) : NaN;
    if (Number.isFinite(stored) && stored > 0) {
      return stored;
    }
    const decoded = decodeJwtExpiresAt(authStorage.getToken());
    if (decoded) {
      authStorage.setSessionExpiresAt(decoded);
    }
    return decoded;
  },
  isSessionExpired(bufferMs = 0): boolean {
    const token = authStorage.getToken();
    if (!token) {
      return true;
    }
    const expiresAt = authStorage.getSessionExpiresAt();
    return expiresAt != null && Date.now() + bufferMs >= expiresAt;
  },
  clearSessionExpiry() {
    localStorage.removeItem(SESSION_EXPIRES_AT_KEY);
  },
  clearAll() {
    authStorage.clearToken();
    authStorage.clearUser();
    authStorage.clearSessionExpiry();
  },
};

function decodeJwtExpiresAt(token: string | null): number | null {
  if (!token) {
    return null;
  }
  const [, payload] = token.split('.');
  if (!payload) {
    return null;
  }
  try {
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const normalized = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const decoded = JSON.parse(window.atob(normalized)) as { exp?: number };
    return typeof decoded.exp === 'number' ? decoded.exp * 1000 : null;
  } catch {
    return null;
  }
}
