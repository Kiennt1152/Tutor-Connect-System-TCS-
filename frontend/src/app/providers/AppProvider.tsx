import type { ReactNode } from 'react';
import { AuthProvider } from '../../shared/auth/AuthProvider';

type AppProviderProps = {
  children: ReactNode;
};

export function AppProvider({ children }: AppProviderProps) {
  return <AuthProvider>{children}</AuthProvider>;
}
