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

export interface DashboardAlertApiResponse {
  type: 'WARNING' | 'CRITICAL' | 'INFO';
  title: string;
  message: string;
  actionUrl: string;
}

export interface DashboardApiResponse {
  totalUsers: number;
  totalTutors: number;
  totalClasses: number;
  activeClasses: number;
  pendingVerifications: number;
  openReports: number;
  openTickets: number;
  pendingWithdrawals: number;
  openDisputes: number;
  totalRevenue: number;
  platformFeeRevenue: number;
  alerts: DashboardAlertApiResponse[];
}

export interface DashboardAlert {
  type: 'WARNING' | 'CRITICAL' | 'INFO';
  title: string;
  message: string;
  actionUrl: string;
}

export interface PlatformDashboard {
  totalUsers: number;
  totalTutors: number;
  totalClasses: number;
  activeClasses: number;
  pendingVerifications: number;
  openReports: number;
  openTickets: number;
  pendingWithdrawals: number;
  openDisputes: number;
  totalRevenue: number;
  platformFeeRevenue: number;
  alerts: DashboardAlert[];
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
  userRole: UserRole | null;
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
  userRole: UserRole | null;
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

export interface ResolveDisputeApiRequest {
  action?: DisputeResolutionAction;
  status?: Exclude<DisputeStatus, 'OPEN'>;
  resolution: string;
  releaseToBeneficiary?: number;
  refundToPayer?: number;
  refundPayoutInfo?: RefundPayoutInfoApiRequest;
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
  refundPayoutInfo?: RefundPayoutInfoApiRequest;
}

export interface ExecuteRefundApiRequest {
  escrowId: number;
  releaseToBeneficiary: number;
  refundToPayer: number;
  reason: string;
  refundPayoutInfo?: RefundPayoutInfoApiRequest;
}

export interface RefundPayoutInfoApiRequest {
  bankName: string;
  accountNo: string;
  accountHolderName: string;
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
  userRole: UserRole | null;
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
  /** Chỉ có với báo cáo targetType = REVIEW; null nếu đánh giá đã bị xóa. */
  reportedReview: AdminReviewApiResponse | null;
}

export interface ResolveClassIssueRequest {
  action: ClassIssueResolutionAction;
  notes: string;
}

export type ReviewReportAction = 'KEEP_REVIEW' | 'HIDE_REVIEW' | 'MARK_VIOLATION' | 'DELETE_REVIEW';

export interface ResolveReviewReportRequest {
  action: ReviewReportAction;
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
  reportedReview: AdminReviewApiResponse | null;
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
  bankName?: string | null;
  accountNoMasked?: string | null;
  accountHolderName?: string | null;
  effectiveDate: string | null;
  createdAt: string | null;
  processedAt: string | null;
}

export interface RefundReviewApiResponse {
  refundId: number | null;
  status: RefundRequestStatus | null;
  amount: number | null;
  bankName?: string | null;
  accountNoMasked?: string | null;
  accountHolderName?: string | null;
  refundReferenceCode?: string | null;
  transferStatus?: string | null;
  reason: string | null;
  requestedByUserId: number | null;
  requestedByEmail: string | null;
  requestedAt: string | null;
  processedAt: string | null;
  transferProcessedAt?: string | null;
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
  bankName?: string | null;
  accountNoMasked?: string | null;
  accountHolderName?: string | null;
  refundReferenceCode?: string | null;
  transferStatus?: string | null;
  status: RefundRequestStatus;
  reason: string | null;
  requestedAt: string | null;
  processedAt: string | null;
  transferProcessedAt?: string | null;
}

export interface RefundRequestItem {
  id: string;
  escrowId: string;
  requester: string;
  classTitle: string;
  amount: string;
  rawAmount: number;
  escrowAmount: string;
  bankName: string;
  accountNoMasked: string;
  accountHolderName: string;
  refundReferenceCode: string;
  transferStatus: string;
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

/* ── Support Tickets (admin) ── */

export type AdminTicketCategory =
  | 'DISPUTE'
  | 'SYSTEM_ERROR'
  | 'REPORT_USER'
  | 'BUG_REPORT'
  | 'INQUIRY';

export type AdminTicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export type AdminTicketStatus =
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'IN_REVIEW'
  | 'RESOLVED'
  | 'CLOSED';

export interface AdminTicketListItemApiResponse {
  ticketId: number;
  userId: number;
  userEmail: string;
  assignedAdminId: number | null;
  assignedAdminName: string | null;
  category: AdminTicketCategory;
  subject: string;
  priority: AdminTicketPriority;
  status: AdminTicketStatus;
  dueAt?: string;
  slaBreached?: boolean;
  responseSlaMs?: number;
  createdAt: string;
  updatedAt: string;
}

export interface AdminTicketMessageApiResponse {
  messageId: number;
  senderId: number;
  senderName: string;
  fromAdmin: boolean;
  content: string;
  sentAt: string;
}

export interface AdminTicketDetailApiResponse {
  ticketId: number;
  userId: number;
  targetClassId: number | null;
  assignedAdminId: number | null;
  category: AdminTicketCategory;
  subject: string;
  description: string;
  evidenceUrls: string | null;
  priority: AdminTicketPriority;
  status: AdminTicketStatus;
  resolvedAt: string | null;
  closedAt: string | null;
  dueAt?: string;
  slaBreached?: boolean;
  responseSlaMs?: number;
  createdAt: string;
  updatedAt: string;
  messages: AdminTicketMessageApiResponse[];
}

export interface PageAdminTicketApiResponse {
  content: AdminTicketListItemApiResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface UpdateTicketApiRequest {
  category?: AdminTicketCategory;
  priority?: AdminTicketPriority;
}

export interface RespondTicketApiRequest {
  content: string;
}

export interface CloseTicketApiRequest {
  status: 'RESOLVED' | 'CLOSED';
  adminNotes?: string;
}

export interface AdminTicketFilters {
  page: number;
  size: number;
  status?: AdminTicketStatus;
  category?: AdminTicketCategory;
  priority?: AdminTicketPriority;
  keyword?: string;
}

/* view-models */

export interface AdminTicketListItem {
  id: string;
  userId: string;
  userEmail: string;
  assignedAdminId: string | null;
  assignedAdminName: string;
  category: AdminTicketCategory;
  categoryLabel: string;
  subject: string;
  priority: AdminTicketPriority;
  priorityLabel: string;
  priorityTone: 'low' | 'medium' | 'high' | 'urgent';
  status: AdminTicketStatus;
  statusLabel: string;
  statusTone: 'open' | 'active' | 'review' | 'done';
  dueAt?: string;
  slaBreached?: boolean;
  responseSlaMs?: number;
  createdAt: string;
  updatedAt: string;
}

export interface AdminTicketMessage {
  id: string;
  senderId: string;
  senderName: string;
  fromAdmin: boolean;
  content: string;
  sentAt: string;
}

export interface AdminTicketDetail extends AdminTicketListItem {
  description: string;
  evidenceUrls: string | null;
  targetClassId: string | null;
  messages: AdminTicketMessage[];
}

export interface PageAdminTicketList {
  items: AdminTicketListItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type AnnouncementTargetRole = 'CLIENT' | 'TUTOR' | 'TUTOR_CENTER' | 'PLATFORM_ADMIN';

export interface AnnouncementApiResponse {
  announcementId: number;
  title: string;
  content: string;
  targetRole: AnnouncementTargetRole | null;
  active: boolean;
  startsAt: string | null;
  endsAt: string | null;
  createdByName: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface UpsertAnnouncementApiRequest {
  title: string;
  content: string;
  targetRole: AnnouncementTargetRole | null;
  active: boolean;
  startsAt: string | null;
  endsAt: string | null;
}

export interface AnnouncementItem {
  announcementId: number;
  title: string;
  content: string;
  targetRole: AnnouncementTargetRole | null;
  active: boolean;
  startsAt: string | null;
  endsAt: string | null;
  createdByName: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

/* ── User Penalties ── */

export type PenaltyType = 'WARNING' | 'FEATURE_RESTRICTION' | 'TEMPORARY_BAN' | 'PERMANENT_BAN';
export type PenaltyStatus = 'ACTIVE' | 'EXPIRED' | 'REVOKED';

export interface PenaltyApiResponse {
  penaltyId: number;
  userId: number;
  userEmail: string;
  userName: string | null;
  penaltyType: PenaltyType;
  reason: string;
  evidenceUrls: string | null;
  restrictionDetails: string | null;
  startsAt: string;
  expiresAt: string | null;
  status: PenaltyStatus;
  revokedAt: string | null;
  revokedReason: string | null;
  createdAt: string;
  issuedByName: string | null;
}

export interface PagePenaltyApiResponse {
  content: PenaltyApiResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface IssuePenaltyApiRequest {
  userId: number;
  penaltyType: PenaltyType;
  reason: string;
  evidenceUrls?: string;
  restrictionDetails?: string;
  expiresAt?: string;
}

export interface RevokePenaltyApiRequest {
  revokedReason: string;
}

export interface PenaltyFilters {
  page: number;
  size: number;
  status?: PenaltyStatus;
  type?: PenaltyType;
  userId?: number;
}

/* ── Audit Logs ── */

export interface AuditLogApiResponse {
  auditId: number;
  actorId: number | null;
  actorEmail: string | null;
  actorRole?: string;
  action: string;
  entityType: string;
  entityId: number;
  oldValue: string | null;
  newValue: string | null;
  ipAddress: string | null;
  createdAt: string;
}

export interface PageAuditLogApiResponse {
  content: AuditLogApiResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AuditLogFilters {
  page: number;
  size: number;
  actorId?: number;
  action?: string;
  entityType?: string;
  from?: string;
  to?: string;
}

/* ── Operational Task Queue ── */

export interface TaskQueueSummaryApiResponse {
  pendingVerifications: number;
  openReports: number;
  openTickets: number;
  pendingWithdrawals: number;
  pendingRefunds: number;
  openDisputes: number;
  totalPendingTasks: number;
}

export type TaskQueueItemType = 'VERIFICATION' | 'REPORT' | 'SUPPORT_TICKET' | 'WITHDRAWAL' | 'REFUND_REQUEST' | 'DISPUTE';
export type TaskPriority = 'URGENT' | 'HIGH' | 'MEDIUM' | 'LOW';

export interface TaskItemApiResponse {
  taskId: string;
  taskType: TaskQueueItemType;
  title: string;
  description: string;
  entityId: number;
  targetRoute: string;
  status: string;
  priority: TaskPriority;
  createdAt: string;
}

export interface PageTaskItemApiResponse {
  content: TaskItemApiResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TaskFilters {
  type?: string;
  page: number;
  size: number;
}

export interface ResolveReportApiRequest {
  status: 'RESOLVED';
  adminNotes?: string;
}

/* ── Reports & Analytics (UC-56) ── */

export interface MonthlyMetricApiResponse {
  month: string;
  newUsers: number;
  newClasses: number;
  revenue: number;
}

export interface AnalyticsSummaryApiResponse {
  totalUsers: number;
  totalTutors: number;
  totalParents: number;
  totalCenters: number;
  totalStudents: number;
  totalClasses: number;
  activeClasses: number;
  completedClasses: number;
  totalRevenue: number;
  platformFeeRevenue: number;
  verificationConversionRate: number;
  disputeRate: number;
  contractCompletionRate: number;
  monthlyMetrics: MonthlyMetricApiResponse[];
}
