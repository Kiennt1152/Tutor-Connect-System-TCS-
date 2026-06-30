import axiosClient from '../../../shared/api/axiosClient';
import { authStorage } from '../../../shared/auth/authStorage';
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  RegisterResponse,
  SendOtpRequest,
  SendOtpResponse,
  VerifyOtpRequest,
  VerifyOtpResponse,
} from '../types/identityTypes';

const BASE = '/identity';

export const identityApi = {
  async login(body: LoginRequest): Promise<AuthResponse> {
    const { data } = await axiosClient.post<AuthResponse>(`${BASE}/login`, body);
    return data;
  },

  async sendOtp(body: SendOtpRequest): Promise<SendOtpResponse> {
    const { data } = await axiosClient.post<SendOtpResponse>(`${BASE}/send-otp`, body);
    return data;
  },

  async verifyOtp(body: VerifyOtpRequest): Promise<VerifyOtpResponse> {
    const { data } = await axiosClient.post<VerifyOtpResponse>(`${BASE}/verify-otp`, body);
    return data;
  },

  async register(body: RegisterRequest): Promise<RegisterResponse> {
    const { data } = await axiosClient.post<RegisterResponse>(`${BASE}/register`, body);
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
