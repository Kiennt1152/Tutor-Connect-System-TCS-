import type { ReactNode } from 'react';
import axiosClient from './axiosClient';

type ApiProviderProps = {
  children: ReactNode;
};

export function ApiProvider({ children }: ApiProviderProps) {
  void axiosClient;
  return <>{children}</>;
}
