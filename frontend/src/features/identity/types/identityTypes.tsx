export type UserRole = 'CLIENT' | 'TUTOR' | 'TUTOR_CENTER' | 'PLATFORM_ADMIN' | 'UNKNOWN';

export type RegisterRole = 'CLIENT' | 'TUTOR' | 'TUTOR_CENTER';

export type LoginRequest = {
  email: string;
  password: string;
};

export type GoogleLoginRequest = {
  /** Google OAuth2 access token tra ve tu Google Identity Services (initTokenClient). */
  accessToken: string;
};

export type GoogleCompleteRequest = {
  accessToken: string;
  role: RegisterRole;
  phone: string;
};

export type AuthResponse = {
  accessToken: string;
  userId: number;
  email: string;
  role: UserRole;
  displayName: string;
  status: string;
};

/**
 * Ket qua dang nhap Google. newUser=true nghia la tai khoan chua ton tai: chi co email +
 * suggestedDisplayName duoc dien, phai goi completeGoogleSignup de tao tai khoan. newUser=false
 * nghia la da dang nhap thanh cong, cac truong con lai giong AuthResponse.
 */
export type GoogleLoginResponse = {
  newUser: boolean;
  email: string;
  suggestedDisplayName?: string;
  accessToken?: string;
  userId?: number;
  role?: UserRole;
  displayName?: string;
  status?: string;
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
