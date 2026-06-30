import axiosClient from '../../../shared/api/axiosClient';
import { authStorage } from '../../../shared/auth/authStorage';
import type { AuthResponse, LoginRequest, RegisterRequest } from '../types/identityTypes';

const BASE = '/identity';

export const identityApi = {
  async login(body: LoginRequest): Promise<AuthResponse> {
    const { data } = await axiosClient.post<AuthResponse>(`${BASE}/login`, body);
    return data;
  },

  async register(body: RegisterRequest): Promise<AuthResponse> {
    const { data } = await axiosClient.post<AuthResponse>(`${BASE}/register`, body);
    return data;
  },

  async getMe() {
    const { data } = await axiosClient.get(`${BASE}/me`);
    return data;
  },
};

export function persistAuth(response: AuthResponse) {
  authStorage.setToken(response.accessToken);
  authStorage.setUser({
    userId: response.userId,
    email: response.email,
    role: response.role,
    displayName: response.displayName,
  });
}
