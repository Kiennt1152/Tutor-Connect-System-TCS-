import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import axios from 'axios';
import { identityApi, persistAuth } from '../../features/identity/api/identityApi';
import type {
  AuthResponse,
  GoogleCompleteRequest,
  GoogleLoginRequest,
  GoogleLoginResponse,
  LoginRequest,
} from '../../features/identity/types/identityTypes';
import { authStorage, type StoredUser } from '../auth/authStorage';

type AuthContextValue = {
  user: StoredUser | null;
  isAuthenticated: boolean;
  authLoading: boolean;
  login: (body: LoginRequest) => Promise<AuthResponse>;
  /** newUser=true nghia la chua co tai khoan; goi completeGoogleSignup de hoan tat. */
  loginWithGoogle: (body: GoogleLoginRequest) => Promise<GoogleLoginResponse>;
  /** Hoàn tất đăng ký bằng Google (vai trò + số điện thoại) và đăng nhập luôn. */
  completeGoogleSignup: (body: GoogleCompleteRequest) => Promise<GoogleLoginResponse>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<StoredUser | null>(() => authStorage.getUser());
  const [authLoading, setAuthLoading] = useState(() => {
    const token = authStorage.getToken();
    return !!token && !authStorage.isSessionExpired();
  });

  useEffect(() => {
    const token = authStorage.getToken();
    if (!token || authStorage.isSessionExpired()) {
      authStorage.clearAll();
      setUser(null);
      setAuthLoading(false);
      return;
    }

    let cancelled = false;
    identityApi
      .getMe()
      .then((me) => {
        if (cancelled) return;
        const refreshedUser: StoredUser = {
          userId: me.userId,
          email: me.email,
          role: me.role,
          displayName: me.displayName,
        };
        authStorage.setUser(refreshedUser);
        setUser(refreshedUser);
      })
      .catch((error: unknown) => {
        if (cancelled) return;
        // Chỉ đăng xuất khi server TỪ CHỐI token (401/403). Backend đang restart hoặc
        // mất mạng thì lần gọi này hỏng chứ phiên vẫn còn hạn — xoá phiên ở đây sẽ đá
        // người dùng ra ngoài mỗi lần backend khởi động lại.
        const status = axios.isAxiosError(error) ? error.response?.status : undefined;
        if (status === 401 || status === 403) {
          authStorage.clearAll();
          setUser(null);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setAuthLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!user) {
      return;
    }
    const expiresAt = authStorage.getSessionExpiresAt();
    if (!expiresAt) {
      return;
    }
    const delay = Math.max(0, expiresAt - Date.now());
    const timer = window.setTimeout(() => {
      authStorage.clearAll();
      setUser(null);
    }, delay);
    return () => window.clearTimeout(timer);
  }, [user]);

  const login = useCallback(async (body: LoginRequest) => {
    const response = await identityApi.login(body);
    persistAuth(response);
    setUser(authStorage.getUser());
    return response;
  }, []);

  /** Đăng nhập Google: tài khoản đã có thì lưu phiên ngay; tài khoản mới thì trả về để hỏi thêm thông tin. */
  const loginWithGoogle = useCallback(async (body: GoogleLoginRequest) => {
    const response = await identityApi.loginWithGoogle(body);
    if (!response.newUser) {
      persistAuth(response as Required<Pick<GoogleLoginResponse, 'accessToken' | 'userId' | 'email' | 'role' | 'displayName' | 'tokenExpiresInSeconds'>>);
      setUser(authStorage.getUser());
    }
    return response;
  }, []);

  /** Gửi thông tin hoàn tất đăng ký Google, lưu phiên và cập nhật người dùng hiện tại. */
  const completeGoogleSignup = useCallback(async (body: GoogleCompleteRequest) => {
    const response = await identityApi.completeGoogleSignup(body);
    persistAuth(response as Required<Pick<GoogleLoginResponse, 'accessToken' | 'userId' | 'email' | 'role' | 'displayName' | 'tokenExpiresInSeconds'>>);
    setUser(authStorage.getUser());
    return response;
  }, []);

  const logout = useCallback(async () => {
    try {
      await identityApi.logout();
    } finally {
      authStorage.clearAll();
      setUser(null);
    }
  }, []);

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: !!user && !!authStorage.getToken(),
      authLoading,
      login,
      loginWithGoogle,
      completeGoogleSignup,
      logout,
    }),
    [user, authLoading, login, loginWithGoogle, completeGoogleSignup, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
