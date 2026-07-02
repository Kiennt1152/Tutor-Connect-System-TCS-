export type UserRole = 'PLATFORM_ADMIN' | 'TUTOR' | 'TUTOR_CENTER' | 'CLIENT' | 'UNKNOWN';

export type Gender = 'MALE' | 'FEMALE' | 'OTHER';

export type ProfileVerificationStatus = 'UNDER_VERIFY' | 'VERIFIED' | 'REJECTED';

export interface ProfileResponse {
  userId: number;
  role: UserRole;
  fullName?: string | null;
  email?: string | null;
  phone?: string | null;
  address?: string | null;
  avatarUrl?: string | null;
  dateOfBirth?: string | null;
  gender?: Gender | null;
  bio?: string | null;
  experienceYears?: number | null;
  hourlyRate?: number | string | null;
  companyName?: string | null;
  licenseNo?: string | null;
  description?: string | null;
  verificationStatus?: ProfileVerificationStatus | null;
}

export interface UpdateProfileRequest {
  fullName?: string;
  phone?: string;
  address?: string;
  avatarUrl?: string;
  dateOfBirth?: string;
  gender?: Gender;
  bio?: string;
  experienceYears?: number;
  hourlyRate?: number;
  companyName?: string;
  description?: string;
  licenseNo?: string;
}

export interface AvatarUploadResponse {
  avatarUrl: string;
}
