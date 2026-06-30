import axiosClient from '../../../shared/api/axiosClient';
import type {
  LoginPayload,
  LoginResponse,
  RegisterPayload,
  RegisterResponse,
  SendOtpPayload,
  SendOtpResponse,
  VerifyOtpPayload,
  VerifyOtpResponse,
} from '../types/authTypes';

export const AUTH_API_BASE = '/auth';

export const authApi = {
  http: axiosClient,
  basePath: AUTH_API_BASE,
  sendOtp: (payload: SendOtpPayload) =>
    axiosClient
      .post<SendOtpResponse>(`${AUTH_API_BASE}/send-otp`, payload)
      .then((response) => response.data),
  verifyOtp: (payload: VerifyOtpPayload) =>
    axiosClient
      .post<VerifyOtpResponse>(`${AUTH_API_BASE}/verify-otp`, payload)
      .then((response) => response.data),
  register: (payload: RegisterPayload) =>
    axiosClient
      .post<RegisterResponse>(`${AUTH_API_BASE}/register`, payload)
      .then((response) => response.data),
  login: (payload: LoginPayload) =>
    axiosClient
      .post<LoginResponse>(`${AUTH_API_BASE}/login`, payload)
      .then((response) => response.data),
};
