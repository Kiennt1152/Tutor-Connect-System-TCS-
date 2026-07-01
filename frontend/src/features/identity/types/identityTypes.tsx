export type UserRole = 'CLIENT' | 'TUTOR' | 'TUTOR_CENTER' | 'PLATFORM_ADMIN' | 'UNKNOWN';

export type RegisterRole = 'CLIENT' | 'TUTOR' | 'TUTOR_CENTER';

export type LoginRequest = {
  email: string;
  password: string;
};

export type GoogleLoginRequest = {
  /** Google ID token (JWT) tra ve tu Google Identity Services. */
  credential: string;
};

export type AuthResponse = {
  accessToken: string;
  userId: number;
  email: string;
  role: UserRole;
  displayName: string;
  status: string;
};

// ---- UC-01 Register Account (OTP qua email) ----

export type SendOtpRequest = {
  email: string;
  role: RegisterRole;
};

export type SendOtpResponse = {
  email: string;
  message: string;
  otpExpiresInSeconds: number;
  resendCooldownSeconds: number;
};

export type VerifyOtpRequest = {
  email: string;
  code: string;
};

export type VerifyOtpResponse = {
  email: string;
  message: string;
  verifiedEmailToken: string;
  tokenExpiresInSeconds: number;
};

export type RegisterRequest = {
  email: string;
  role: RegisterRole;
  displayName: string;
  phone: string;
  password: string;
  confirmPassword: string;
  verifiedEmailToken: string;
};

export type RegisterResponse = {
  email: string;
  message: string;
};
