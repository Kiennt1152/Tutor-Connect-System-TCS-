import { useCallback, useState } from 'react';
import axios from 'axios';
import { identityApi } from '../api/identityApi';
import type { AuthResponse, AuthUser, LoginRequest } from '../types/identityTypes';

const TOKEN_KEY = 'token';
const USER_KEY = 'user';

function readStoredUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

function persist(auth: AuthResponse): AuthUser {
  const user: AuthUser = { userId: auth.userId, email: auth.email, status: auth.status };
  localStorage.setItem(TOKEN_KEY, auth.token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
  return user;
}

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as Record<string, string> | undefined;
    if (data) {
      // Backend trả { message: ... } cho lỗi nghiệp vụ, hoặc { field: message } cho lỗi validate
      return data.message ?? Object.values(data)[0] ?? fallback;
    }
  }
  return fallback;
}

export function useIdentity() {
  const [user, setUser] = useState<AuthUser | null>(() => readStoredUser());
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const login = useCallback(async (payload: LoginRequest) => {
    setLoading(true);
    setError(null);
    try {
      const auth = await identityApi.login(payload);
      setUser(persist(auth));
    } catch (err) {
      setError(extractError(err, 'Đăng nhập thất bại'));
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  return { user, loading, error, login, logout };
}
