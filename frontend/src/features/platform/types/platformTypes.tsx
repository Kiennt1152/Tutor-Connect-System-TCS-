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
  /** updatedAt của hồ sơ lúc admin mở xem — để server chống ghi đè khi có người sửa song song. */
  expectedUpdatedAt?: string;
}

export interface ResolveDisputeApiRequest {
  status: Exclude<DisputeStatus, 'OPEN'>;
  resolution: string;
}

export interface ExecuteSettlementApiRequest {
  escrowId: number;
  releaseToBeneficiary: number;
  refundToPayer: number;
  reason: string;
}

export interface ExecuteRefundApiRequest {
  escrowId: number;
  releaseToBeneficiary: number;
  refundToPayer: number;
  reason: string;
}

export interface RefundExecutionApiResponse {
  refundId: number;
  escrowId: number;
  escrowStatus: EscrowStatus;
  refundStatus: RefundRequestStatus;
  escrowAmount: number;
  releaseToBeneficiary: number;
  refundToPayer: number;
  reason: string;
  requestedAt: string;
  processedAt: string | null;
  message: string;
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
export type DisputeStatus = 'OPEN' | 'UNDER_INVESTIGATION' | 'RESOLVED' | 'WAITING';
export type EscrowStatus = 'PENDING' | 'FUNDED' | 'RELEASED' | 'REFUNDED' | 'ON_HOLD' | 'DISPUTED';
export type PaymentTransactionType = 'DEPOSIT' | 'WITHDRAWAL' | 'REFUND' | 'ESCROW_DEPOSIT' | 'ESCROW_RELEASE';
export type PaymentTransactionStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';
export type ClassTerminationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED';
export type RefundRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED';
export type TutoringClassStatus =
  | 'DRAFT'
  | 'OPEN'
  | 'MATCHED'
  | 'ENROLLMENT_CLOSED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'DISPUTED';

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

export interface EscrowReviewApiResponse {
  escrowId: number | null;
  status: EscrowStatus | null;
  amount: number | null;
  depositedAt: string | null;
  releasedAt: string | null;
  assignmentId: number | null;
  classStudentId: number | null;
  paymentTransactionId: number | null;
  paymentType: PaymentTransactionType | null;
  paymentStatus: PaymentTransactionStatus | null;
  paymentReferenceCode: string | null;
  payerUserId: number | null;
  payerEmail: string | null;
}

export interface ClassReviewApiResponse {
  classId: number | null;
  title: string | null;
  status: TutoringClassStatus | null;
  creatorUserId: number | null;
  creatorEmail: string | null;
  assignmentId: number | null;
  tutorUserId: number | null;
  tutorEmail: string | null;
  tutorName: string | null;
  classStudentId: number | null;
  enrolledByUserId: number | null;
  enrolledByEmail: string | null;
  studentName: string | null;
}

export interface TerminationReviewApiResponse {
  terminationId: number | null;
  status: ClassTerminationStatus | null;
  requestedByUserId: number | null;
  requestedByEmail: string | null;
  reason: string | null;
  effectiveDate: string | null;
  createdAt: string | null;
  processedAt: string | null;
}

export interface RefundReviewApiResponse {
  refundId: number | null;
  status: RefundRequestStatus | null;
  amount: number | null;
  reason: string | null;
  requestedByUserId: number | null;
  requestedByEmail: string | null;
  requestedAt: string | null;
  processedAt: string | null;
}

export interface AdminDisputeReviewApiResponse {
  disputeId: number;
  disputeStatus: DisputeStatus;
  resolution: string | null;
  disputeCreatedAt: string | null;
  disputeUpdatedAt: string | null;
  reportId: number | null;
  reportStatus: ReportStatus | null;
  reporterId: number | null;
  reporterEmail: string | null;
  targetType: ReportTargetType | null;
  targetId: number | null;
  category: ReportCategory | null;
  description: string | null;
  evidenceUrls: string | null;
  evidenceUrlList: string[];
  reportCreatedAt: string | null;
  reportUpdatedAt: string | null;
  escrow: EscrowReviewApiResponse | null;
  latestRefundRequest: RefundReviewApiResponse | null;
  tutoringClass: ClassReviewApiResponse | null;
  terminationRequest: TerminationReviewApiResponse | null;
}

export interface DisputeReviewItem {
  id: string;
  status: DisputeStatus;
  statusLabel: string;
  reportId: string;
  reporter: string;
  target: string;
  category: string;
  description: string;
  evidenceCount: number;
  escrowStatus: EscrowStatus | null;
  escrowStatusLabel: string;
  amount: string;
  classTitle: string;
  createdAt: string;
  raw: AdminDisputeReviewApiResponse;
}
