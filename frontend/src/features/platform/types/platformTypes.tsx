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
  action?: DisputeResolutionAction;
  status?: Exclude<DisputeStatus, 'OPEN'>;
  resolution: string;
  releaseToBeneficiary?: number;
  refundToPayer?: number;
}

export interface AppealDisputeApiRequest {
  reason: string;
  evidenceUrls?: string;
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
export type ClassIssueResolutionAction =
  | 'REQUEST_MORE_INFORMATION'
  | 'CONTINUE_CLASS'
  | 'RESCHEDULE'
  | 'REPLACE_TUTOR'
  | 'TERMINATE_CLASS'
  | 'ESCALATE_TO_DISPUTE'
  | 'CLOSE_NO_ACTION';
export type DisputeStatus = 'OPEN' | 'UNDER_INVESTIGATION' | 'RESOLVED' | 'WAITING';
export type DisputeResolutionAction =
  | 'CONTINUE_CLASS'
  | 'TERMINATE_CLASS'
  | 'APPROVE_FULL_REFUND'
  | 'APPROVE_PARTIAL_REFUND'
  | 'REJECT_REFUND'
  | 'CLOSE_MUTUAL_AGREEMENT'
  | 'REQUEST_MORE_EVIDENCE';
export type EscrowStatus = 'PENDING' | 'FUNDED' | 'RELEASED' | 'REFUNDED' | 'ON_HOLD' | 'DISPUTED';
export type PaymentTransactionType = 'DEPOSIT' | 'WITHDRAWAL' | 'REFUND' | 'ESCROW_DEPOSIT' | 'ESCROW_RELEASE';
export type PaymentTransactionStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';
export type ClassTerminationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED';
export type RefundRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED';
export type WithdrawalRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED';
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
  reporterEmail: string | null;
  targetType: ReportTargetType;
  targetId: number;
  classTitle: string | null;
  classStatus: string | null;
  category: ReportCategory;
  description: string;
  evidenceUrls: string | null;
  evidenceUrlList: string[];
  status: ReportStatus;
  issueType: string | null;
  issueTypeLabel: string | null;
  lessonRef: string | null;
  occurredAt: string | null;
  requestedAction: string | null;
  requestedActionLabel: string | null;
  linkedDisputeId: number | null;
  createdAt: string;
  updatedAt: string | null;
}

export interface ResolveClassIssueRequest {
  action: ClassIssueResolutionAction;
  notes: string;
}

export interface ReportItem {
  id: string;
  reporterId: string;
  reporterEmail: string;
  targetType: string;
  targetTypeLabel: string;
  targetId: string;
  classTitle: string;
  classStatus: string;
  category: ReportCategory;
  categoryLabel: string;
  description: string;
  userDescription: string;
  evidenceUrlList: string[];
  evidenceCount: number;
  status: ReportStatus;
  statusLabel: string;
  issueType: string;
  issueTypeLabel: string;
  lessonRef: string;
  occurredAt: string;
  requestedAction: string;
  requestedActionLabel: string;
  linkedDisputeId: number | null;
  createdAt: string;
  updatedAt: string;
  raw: ReportApiResponse;
}

export interface AdminWithdrawalApiResponse {
  withdrawalId: number;
  walletId: number | null;
  requesterEmail: string | null;
  amount: number;
  status: WithdrawalRequestStatus;
  paymentMethodId: number | null;
  bankName: string | null;
  accountNoMasked: string | null;
  transactionId: number | null;
  transactionStatus: PaymentTransactionStatus | null;
  referenceCode: string | null;
  externalTransactionId: string | null;
  requestedAt: string | null;
  processedAt: string | null;
  failureReason: string | null;
}

export interface PageAdminWithdrawalApiResponse {
  content: AdminWithdrawalApiResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AdminWithdrawalItem {
  id: string;
  walletId: string;
  requester: string;
  amount: string;
  rawAmount: number;
  status: WithdrawalRequestStatus;
  statusLabel: string;
  bankName: string;
  accountNoMasked: string;
  referenceCode: string;
  transactionStatusLabel: string;
  requestedAt: string;
  processedAt: string;
  canApprove: boolean;
  canReject: boolean;
  canMarkTransferFailed: boolean;
  raw: AdminWithdrawalApiResponse;
}

export interface PageAdminWithdrawalList {
  items: AdminWithdrawalItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface WithdrawalListFilters {
  page: number;
  size: number;
  status?: WithdrawalRequestStatus;
}

export interface WithdrawalDecisionApiRequest {
  reason?: string;
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

export interface RefundRequestApiResponse {
  refundId: number;
  escrowId: number | null;
  escrowStatus: EscrowStatus | null;
  requesterId: number | null;
  requesterEmail: string | null;
  classId: number | null;
  classTitle: string | null;
  assignmentId: number | null;
  classStudentId: number | null;
  escrowAmount: number | null;
  amount: number;
  status: RefundRequestStatus;
  reason: string | null;
  requestedAt: string | null;
  processedAt: string | null;
}

export interface RefundRequestItem {
  id: string;
  escrowId: string;
  requester: string;
  classTitle: string;
  amount: string;
  rawAmount: number;
  escrowAmount: string;
  status: RefundRequestStatus;
  statusLabel: string;
  escrowStatus: EscrowStatus | null;
  escrowStatusLabel: string;
  reason: string;
  requestedAt: string;
  processedAt: string;
  canDecide: boolean;
  raw: RefundRequestApiResponse;
}

export interface RefundDecisionApiRequest {
  approvedAmount?: number;
  reason?: string;
}

export interface SettlementSuggestionApiResponse {
  totalSessions: number | null;
  completedSessions: number | null;
  releaseAmount: number | null;
  refundAmount: number | null;
  reason: string | null;
}

export interface AuditReviewApiResponse {
  auditId: number | null;
  actorId: number | null;
  actorEmail: string | null;
  action: string | null;
  oldValue: string | null;
  newValue: string | null;
  createdAt: string | null;
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
  settlementSuggestion: SettlementSuggestionApiResponse | null;
  auditTrail: AuditReviewApiResponse[];
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
