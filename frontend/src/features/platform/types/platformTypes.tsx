export type UserStatus = 'ACTIVE' | 'SUSPENDED' | 'BANNED';

export type UserRole =
  | 'PLATFORM_ADMIN'
  | 'TUTOR'
  | 'TUTOR_CENTER'
  | 'CLIENT'
  | 'UNKNOWN';

export interface UserListItemApiResponse {
  userId: number;
  email: string;
  phone: string | null;
  status: UserStatus;
  role: UserRole;
  displayName: string;
  createdAt: string;
  updatedAt: string;
}

export interface PageUserListApiResponse {
  content: UserListItemApiResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UpdateUserStatusApiRequest {
  status: UserStatus;
}

export interface UserListItem {
  id: string;
  email: string;
  phone: string;
  status: UserStatus;
  role: UserRole;
  roleLabel: string;
  statusLabel: string;
  displayName: string;
  createdAt: string;
  updatedAt: string;
}

export interface PageUserList {
  items: UserListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UserListFilters {
  page: number;
  size: number;
  status?: UserStatus;
  role?: UserRole;
  keyword?: string;
}

export interface DashboardApiResponse {
  totalUsers: number;
  totalTutors: number;
  totalClasses: number;
  pendingVerifications: number;
  openReports: number;
}

export interface PlatformDashboard {
  totalUsers: number;
  totalTutors: number;
  totalClasses: number;
  pendingVerifications: number;
  openReports: number;
}

export type VerificationStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'VERIFIED'
  | 'REJECTED';

export type VerificationType = 'TUTOR_PROFILE' | 'TUTOR_CENTER_LICENSE';

export interface VerificationRequestApiResponse {
  verificationId: number;
  userId: number;
  userEmail: string;
  verificationType: VerificationType;
  status: VerificationStatus;
  adminNotes: string | null;
  submittedAt: string | null;
  reviewedAt: string | null;
}

export interface VerificationRequestItem {
  id: string;
  userId: string;
  userEmail: string;
  verificationType: VerificationType;
  typeLabel: string;
  status: VerificationStatus;
  statusLabel: string;
  adminNotes: string;
  submittedAt: string;
  reviewedAt: string;
  canReview: boolean;
  isReviewed: boolean;
}

export interface ReviewVerificationApiRequest {
  status: 'VERIFIED' | 'REJECTED';
  adminNotes?: string;
  expectedUpdatedAt?: string;
}

export type VerificationDocumentType = 'ID_CARD' | 'DEGREE' | 'CERTIFICATE' | 'LICENSE';

export interface VerificationDocumentApiResponse {
  documentId: number;
  documentType: VerificationDocumentType;
  fileId: number | null;
  fileName: string | null;
  fileUrl: string | null;
  mimeType: string | null;
  fileSize: number | null;
  available: boolean;
}

export interface VerificationDetailApiResponse {
  verificationId: number;
  userId: number;
  userEmail: string;
  verificationType: VerificationType;
  status: VerificationStatus;
  adminNotes: string | null;
  submittedAt: string | null;
  reviewedAt: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  submitterName: string | null;
  submitterPhone: string | null;
  submitterDetails: Record<string, string>;
  documents: VerificationDocumentApiResponse[];
  hasUnreadableDocument: boolean;
}

export type ReportStatus = 'PENDING' | 'RESOLVED';
export type ReportCategory = 'FRAUD' | 'ABUSE' | 'SPAM';
export type ReportTargetType = string;

export interface ReportApiResponse {
  reportId: number;
  reporterId: number;
  targetType: ReportTargetType;
  targetId: number;
  category: ReportCategory;
  description: string;
  status: ReportStatus;
  createdAt: string;
}

export interface ReportItem {
  id: string;
  reporterId: string;
  targetType: string;
  targetTypeLabel: string;
  targetId: string;
  category: ReportCategory;
  categoryLabel: string;
  description: string;
  status: ReportStatus;
  statusLabel: string;
  createdAt: string;
}

export type ReviewModerationStatus = 'VISIBLE' | 'HIDDEN' | 'MODERATED';

export interface AdminReviewApiResponse {
  reviewId: number;
  rating: number | null;
  comment: string | null;
  criteriaJson: string | null;
  status: ReviewModerationStatus;
  reviewerId: number;
  reviewerName: string | null;
  reviewerEmail: string | null;
  anonymous: boolean;
  publicDisplayName: string | null;
  tutorUserId: number;
  tutorName: string | null;
  classId: number | null;
  classTitle: string | null;
  subjectName: string | null;
  tutorReply: string | null;
  createdAt: string;
}
