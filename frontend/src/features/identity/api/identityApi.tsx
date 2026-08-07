import axiosClient from '../../../shared/api/axiosClient';
import { authStorage } from '../../../shared/auth/authStorage';
import type {
  AuthResponse,
  GoogleCompleteRequest,
  GoogleLoginRequest,
  GoogleLoginResponse,
  LoginRequest,
  RegisterRequest,
  RegisterResponse,
  SendOtpRequest,
  SendOtpResponse,
  VerifyOtpRequest,
  VerifyOtpResponse,
  PasswordResetOtpResponse,
  RequestPasswordResetOtpRequest,
  VerifyPasswordResetOtpRequest,
  ResetPasswordRequest,
  ChangePasswordRequest,
} from '../types/identityTypes';

const BASE = '/identity';

export const identityApi = {
  async login(body: LoginRequest): Promise<AuthResponse> {
    const { data } = await axiosClient.post<AuthResponse>(`${BASE}/login`, body);
    return data;
  },

  async loginWithGoogle(body: GoogleLoginRequest): Promise<GoogleLoginResponse> {
    const { data } = await axiosClient.post<GoogleLoginResponse>(`${BASE}/google`, body);
    return data;
  },

  async completeGoogleSignup(body: GoogleCompleteRequest): Promise<GoogleLoginResponse> {
    const { data } = await axiosClient.post<GoogleLoginResponse>(`${BASE}/google/complete`, body);
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

  async requestPasswordResetOtp(body: RequestPasswordResetOtpRequest): Promise<PasswordResetOtpResponse> {
    const { data } = await axiosClient.post<PasswordResetOtpResponse>(`${BASE}/password/forgot`, body);
    return data;
  },

  async verifyPasswordResetOtp(body: VerifyPasswordResetOtpRequest): Promise<PasswordResetOtpResponse> {
    const { data } = await axiosClient.post<PasswordResetOtpResponse>(`${BASE}/password/forgot/verify-otp`, body);
    return data;
  },

  async resetPassword(body: ResetPasswordRequest): Promise<{ message: string }> {
    const { data } = await axiosClient.post<{ message: string }>(`${BASE}/password/reset`, body);
    return data;
  },

  async changePassword(body: ChangePasswordRequest): Promise<{ message: string }> {
    const { data } = await axiosClient.put<{ message: string }>(`${BASE}/password`, body);
    return data;
  },
};

export function persistAuth(response: Pick<AuthResponse, 'accessToken' | 'userId' | 'email' | 'role' | 'displayName'>) {
  authStorage.setToken(response.accessToken);
  authStorage.setUser({
    userId: response.userId,
    email: response.email,
    role: response.role,
    displayName: response.displayName,
  });
}
