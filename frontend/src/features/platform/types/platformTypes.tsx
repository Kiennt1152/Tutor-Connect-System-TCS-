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
