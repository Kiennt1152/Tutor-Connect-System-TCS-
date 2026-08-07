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

// --- Dependent profile linker types ---

export type ChildProfile = {
  childProfileId: number;
  fullName: string;
  dateOfBirth?: string;
  gender?: Gender;
  gradeId?: number;
  gradeName?: string;
  schoolName?: string;
  notes?: string;
  createdAt?: string;
  linkedToUserAccount?: boolean;
  childUserId?: number;
  childEmail?: string;
};

export type GuardianProfile = {
  parentUserId: number;
  fullName: string;
  email: string;
  phone: string;
  linkedAt?: string;
};

export type DependentLinkStatus = {
  dateOfBirthMissing: boolean;
  minorAccount: boolean;
  guardianRequired: boolean;
  guardianLinked: boolean;
  childrenLinkOptional: boolean;
  linkedChildrenCount: number;
  profileLinkComplete: boolean;
  /** @deprecated dùng profileLinkComplete */
  canProceedToPayment: boolean;
  legalProceduresDelegatedToParent: boolean;
  legalAccountUserId?: number;
  legalAccountHolderName?: string;
  legalAccountEmail?: string;
  /** Học sinh vị thành niên: sau khi thao tác cần phụ huynh xác nhận. */
  parentApprovalRequired: boolean;
};

export type ChildProfileRequest = {
  fullName: string;
  dateOfBirth?: string;
  gender?: Gender;
  gradeId?: number;
  schoolName?: string;
  notes?: string;
};

export type UpdateChildProfileRequest = {
  fullName?: string;
  dateOfBirth?: string;
  gender?: Gender;
  gradeId?: number | null;
  schoolName?: string;
  notes?: string;
};

export type LinkGuardianRequest = {
  parentEmail: string;
};

export type LinkChildAccountRequest = {
  childEmail: string;
};

export type CatalogItem = {
  id: number;
  name: string;
  description?: string;
};
