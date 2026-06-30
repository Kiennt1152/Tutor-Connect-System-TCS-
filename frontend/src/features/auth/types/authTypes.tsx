export type RegisterRole = 'CLIENT' | 'TUTOR' | 'TUTOR_CENTER';

export interface RegisterFormValues {
  role: RegisterRole;
  email: string;
  password: string;
  confirmPassword: string;
  displayName: string;
  phone: string;
}

export interface SendOtpPayload {
  email: string;
  role: RegisterRole;
}

export interface SendOtpResponse {
  email: string;
  message: string;
  otpExpiresInSeconds: number;
  resendCooldownSeconds: number;
}

export interface VerifyOtpPayload {
  email: string;
  code: string;
}

export interface VerifyOtpResponse {
  email: string;
  message: string;
  verifiedEmailToken: string;
  tokenExpiresInSeconds: number;
}

export interface RegisterPayload {
  token: string;
  email: string;
  role: RegisterRole;
  password: string;
  confirmPassword: string;
  displayName: string;
  phone: string;
}

export interface RegisterResponse {
  email: string;
  message: string;
}
