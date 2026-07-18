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
