export type UserRole = 'CLIENT' | 'TUTOR' | 'TUTOR_CENTER' | 'PLATFORM_ADMIN' | 'UNKNOWN';

export type LoginRequest = {
  email: string;
  password: string;
};

export type RegisterRequest = {
  email: string;
  password: string;
  phone?: string;
  role: UserRole;
  fullName: string;
  gender?: 'MALE' | 'FEMALE';
  address?: string;
  licenseNo?: string;
  companyName?: string;
  experienceYears?: number;
  hourlyRate?: number;
};

export type AuthResponse = {
  accessToken: string;
  userId: number;
  email: string;
  role: UserRole;
  displayName: string;
  status: string;
};
