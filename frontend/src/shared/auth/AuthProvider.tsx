import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { identityApi, persistAuth } from '../../features/identity/api/identityApi';
import type { AuthResponse, LoginRequest } from '../../features/identity/types/identityTypes';
import { authStorage, type StoredUser } from '../auth/authStorage';

type AuthContextValue = {
  user: StoredUser | null;
  isAuthenticated: boolean;
  login: (body: LoginRequest) => Promise<AuthResponse>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<StoredUser | null>(() => authStorage.getUser());

  const login = useCallback(async (body: LoginRequest) => {
    const response = await identityApi.login(body);
    persistAuth(response);
    setUser(authStorage.getUser());
    return response;
  }, []);

  const logout = useCallback(() => {
    authStorage.clearAll();
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: !!user && !!authStorage.getToken(),
      login,
      logout,
    }),
    [user, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
