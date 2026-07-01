export type Gender = 'MALE' | 'FEMALE';

export type ProfileResponse = {
  userId: number;
  role: string;
  fullName: string;
  email: string;
  phone: string;
  address?: string;
  avatarUrl?: string;
  dateOfBirth?: string;
  gender?: Gender;
};

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

export type LinkGuardianRequest = {
  parentEmail: string;
};

export type UpdateProfileRequest = {
  fullName?: string;
  phone?: string;
  dateOfBirth?: string;
  gender?: Gender;
  address?: string;
};

export type CatalogItem = {
  id: number;
  name: string;
  description?: string;
};
