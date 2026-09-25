import { useCallback, useState } from 'react';
import axios from 'axios';
import { identityApi } from '../api/identityApi';
import type { AuthResponse, LoginRequest } from '../types/identityTypes';

const TOKEN_KEY = 'token';
const USER_KEY = 'user';

export type AuthUser = {
  userId: number;
  email: string;
  status: string;
};

/** Đọc người dùng đã lưu trong localStorage (JSON lỗi thì null). */
function readStoredUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    return null;
  }
}

/** Lưu token và thông tin người dùng sau khi đăng nhập vào localStorage. */
function persist(auth: AuthResponse): AuthUser {
  const user: AuthUser = { userId: auth.userId, email: auth.email, status: auth.status };
  localStorage.setItem(TOKEN_KEY, auth.accessToken);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
  return user;
}

/** Lấy câu lỗi từ phản hồi API (message hoặc lỗi đầu tiên); không có thì câu dự phòng. */
function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as Record<string, string> | undefined;
    if (data) {
      return data.message ?? Object.values(data)[0] ?? fallback;
    }
  }
  return fallback;
}

export function useIdentity() {
  const [user, setUser] = useState<AuthUser | null>(() => readStoredUser());
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /** Đăng nhập, lưu phiên; lỗi thì ghi câu lỗi và ném tiếp. */
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

  /** Đăng xuất phía trình duyệt: xoá token và người dùng đã lưu. */
  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  return { user, loading, error, login, logout };
}
