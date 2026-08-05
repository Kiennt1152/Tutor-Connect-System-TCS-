import type {
  AdminTicketCategory,
  AdminTicketDetail,
  AdminTicketDetailApiResponse,
  AdminTicketFilters,
  AdminTicketListItem,
  AdminTicketListItemApiResponse,
  AdminTicketMessage,
  AdminTicketMessageApiResponse,
  AdminTicketPriority,
  AdminTicketStatus,
  DashboardApiResponse,
  PageAdminTicketApiResponse,
  PageAdminTicketList,
  PageUserList,
  PageUserListApiResponse,
  PlatformDashboard,
  ReportApiResponse,
  ReportItem,
  ReviewVerificationApiRequest,
  UpdateUserStatusApiRequest,
  UserListFilters,
  UserListItem,
  UserListItemApiResponse,
  UserRole,
  UserStatus,
  VerificationRequestApiResponse,
  VerificationRequestItem,
  VerificationStatus,
  VerificationType,
  ReportCategory,
  ReportStatus,
} from '../types/platformTypes';

const ROLE_LABELS: Record<UserRole, string> = {
  PLATFORM_ADMIN: 'Quản trị viên',
  TUTOR: 'Gia sư',
  TUTOR_CENTER: 'Trung tâm',
  CLIENT: 'Phụ huynh/Học sinh',
  UNKNOWN: 'Không xác định',
};

const STATUS_LABELS: Record<UserStatus, string> = {
  ACTIVE: 'Hoạt động',
  SUSPENDED: 'Tạm ngưng',
  BANNED: 'Đã khóa',
};

const formatDateTime = (value: string | null | undefined) => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
};

export function mapDashboardResponse(response: DashboardApiResponse): PlatformDashboard {
  return {
    totalUsers: response.totalUsers || 0,
    totalTutors: response.totalTutors || 0,
    totalClasses: response.totalClasses || 0,
    activeClasses: response.activeClasses || 0,
    pendingVerifications: response.pendingVerifications || 0,
    openReports: response.openReports || 0,
    openTickets: response.openTickets || 0,
    pendingWithdrawals: response.pendingWithdrawals || 0,
    openDisputes: response.openDisputes || 0,
    totalRevenue: response.totalRevenue || 0,
    platformFeeRevenue: response.platformFeeRevenue || 0,
    alerts: (response.alerts || []).map((a) => ({
      type: a.type,
      title: a.title,
      message: a.message,
      actionUrl: a.actionUrl,
    })),
  };
}

export function mapUserListItem(item: UserListItemApiResponse): UserListItem {
  return {
    id: String(item.userId),
    email: item.email,
    phone: item.phone?.trim() || '—',
    status: item.status,
    role: item.role,
    roleLabel: ROLE_LABELS[item.role] ?? item.role,
    statusLabel: STATUS_LABELS[item.status] ?? item.status,
    displayName: item.displayName?.trim() || item.email,
    createdAt: formatDateTime(item.createdAt),
    updatedAt: formatDateTime(item.updatedAt),
  };
}

export function mapPageUserList(response: PageUserListApiResponse): PageUserList {
  return {
    items: response.content.map(mapUserListItem),
    page: response.page,
    size: response.size,
    totalElements: response.totalElements,
    totalPages: response.totalPages,
  };
}

export function buildUserListQuery(filters: UserListFilters) {
  const params = new URLSearchParams();
  params.set('page', String(filters.page));
  params.set('size', String(filters.size));
  if (filters.status) params.set('status', filters.status);
  if (filters.role) params.set('role', filters.role);
  if (filters.keyword?.trim()) params.set('keyword', filters.keyword.trim());
  return params.toString();
}

export function buildUpdateStatusPayload(status: UserStatus): UpdateUserStatusApiRequest {
  return { status };
}

const VERIFICATION_TYPE_LABELS: Record<VerificationType, string> = {
  TUTOR_PROFILE: 'Hồ sơ gia sư',
  TUTOR_CENTER_LICENSE: 'Giấy phép trung tâm',
};

const VERIFICATION_STATUS_LABELS: Record<VerificationStatus, string> = {
  DRAFT: 'Nháp',
  SUBMITTED: 'Chờ duyệt',
  UNDER_REVIEW: 'Đang xem xét',
  VERIFIED: 'Đã duyệt',
  REJECTED: 'Từ chối',
};

const REPORT_CATEGORY_LABELS: Record<ReportCategory, string> = {
  FRAUD: 'Gian lận',
  ABUSE: 'Lạm dụng',
  SPAM: 'Spam',
};

const REPORT_STATUS_LABELS: Record<ReportStatus, string> = {
  PENDING: 'Đang mở',
  RESOLVED: 'Đã xử lý',
};

const TARGET_TYPE_LABELS: Record<string, string> = {
  USER: 'Người dùng',
  TUTOR: 'Gia sư',
  CLASS: 'Lớp học',
  MESSAGE: 'Tin nhắn',
};

export function mapVerificationItem(item: VerificationRequestApiResponse): VerificationRequestItem {
  const canReview = item.status === 'SUBMITTED' || item.status === 'UNDER_REVIEW';
  const isReviewed = item.status === 'VERIFIED' || item.status === 'REJECTED';
  return {
    id: String(item.verificationId),
    userId: String(item.userId),
    userEmail: item.userEmail,
    verificationType: item.verificationType,
    typeLabel: VERIFICATION_TYPE_LABELS[item.verificationType] ?? item.verificationType,
    status: item.status,
    statusLabel: VERIFICATION_STATUS_LABELS[item.status] ?? item.status,
    adminNotes: item.adminNotes?.trim() || '—',
    submittedAt: formatDateTime(item.submittedAt),
    reviewedAt: formatDateTime(item.reviewedAt),
    canReview,
    isReviewed,
  };
}

export function mapReportItem(item: ReportApiResponse): ReportItem {
  return {
    id: String(item.reportId),
    reporterId: String(item.reporterId),
    targetType: item.targetType,
    targetTypeLabel: TARGET_TYPE_LABELS[item.targetType] ?? item.targetType,
    targetId: String(item.targetId),
    category: item.category,
    categoryLabel: REPORT_CATEGORY_LABELS[item.category] ?? item.category,
    description: item.description?.trim() || '—',
    status: item.status,
    statusLabel: REPORT_STATUS_LABELS[item.status] ?? item.status,
    createdAt: formatDateTime(item.createdAt),
  };
}

export function buildReviewVerificationPayload(
  status: 'VERIFIED' | 'REJECTED',
  adminNotes?: string,
  expectedUpdatedAt?: string,
): ReviewVerificationApiRequest {
  return {
    status,
    adminNotes: adminNotes?.trim() || undefined,
    expectedUpdatedAt: expectedUpdatedAt || undefined,
  };
}

/* ── Support Ticket mappers ── */

const TICKET_CATEGORY_LABELS: Record<AdminTicketCategory, string> = {
  DISPUTE: 'Tranh chap',
  SYSTEM_ERROR: 'Loi he thong',
  REPORT_USER: 'Bao cao nguoi dung',
  BUG_REPORT: 'Loi phan mem',
  INQUIRY: 'Cau hoi chung',
};

const TICKET_PRIORITY_LABELS: Record<AdminTicketPriority, string> = {
  LOW: 'Thap',
  MEDIUM: 'Trung binh',
  HIGH: 'Cao',
  URGENT: 'Khan cap',
};

const TICKET_PRIORITY_TONES: Record<AdminTicketPriority, 'low' | 'medium' | 'high' | 'urgent'> = {
  LOW: 'low',
  MEDIUM: 'medium',
  HIGH: 'high',
  URGENT: 'urgent',
};

const TICKET_STATUS_LABELS: Record<AdminTicketStatus, string> = {
  OPEN: 'Cho xu ly',
  IN_PROGRESS: 'Dang xu ly',
  IN_REVIEW: 'Cho phan hoi',
  RESOLVED: 'Da giai quyet',
  CLOSED: 'Da dong',
};

const TICKET_STATUS_TONES: Record<AdminTicketStatus, 'open' | 'active' | 'review' | 'done'> = {
  OPEN: 'open',
  IN_PROGRESS: 'active',
  IN_REVIEW: 'review',
  RESOLVED: 'done',
  CLOSED: 'done',
};

export function mapAdminTicketListItem(item: AdminTicketListItemApiResponse): AdminTicketListItem {
  return {
    id: String(item.ticketId),
    userId: String(item.userId),
    userEmail: item.userEmail,
    assignedAdminId: item.assignedAdminId != null ? String(item.assignedAdminId) : null,
    assignedAdminName: item.assignedAdminName ?? '—',
    category: item.category,
    categoryLabel: TICKET_CATEGORY_LABELS[item.category] ?? item.category,
    subject: item.subject,
    priority: item.priority,
    priorityLabel: TICKET_PRIORITY_LABELS[item.priority] ?? item.priority,
    priorityTone: TICKET_PRIORITY_TONES[item.priority] ?? 'low',
    status: item.status,
    statusLabel: TICKET_STATUS_LABELS[item.status] ?? item.status,
    statusTone: TICKET_STATUS_TONES[item.status] ?? 'open',
    dueAt: formatDateTime(item.dueAt),
    slaBreached: item.slaBreached,
    responseSlaMs: item.responseSlaMs,
    createdAt: formatDateTime(item.createdAt),
    updatedAt: formatDateTime(item.updatedAt),
  };
}

function mapAdminTicketMessage(msg: AdminTicketMessageApiResponse): AdminTicketMessage {
  return {
    id: String(msg.messageId),
    senderId: String(msg.senderId),
    senderName: msg.senderName,
    fromAdmin: msg.fromAdmin,
    content: msg.content,
    sentAt: formatDateTime(msg.sentAt),
  };
}

export function mapAdminTicketDetail(item: AdminTicketDetailApiResponse): AdminTicketDetail {
  const listItem: AdminTicketListItem = {
    id: String(item.ticketId),
    userId: String(item.userId),
    userEmail: '',
    assignedAdminId: item.assignedAdminId != null ? String(item.assignedAdminId) : null,
    assignedAdminName: '—',
    category: item.category,
    categoryLabel: TICKET_CATEGORY_LABELS[item.category] ?? item.category,
    subject: item.subject,
    priority: item.priority,
    priorityLabel: TICKET_PRIORITY_LABELS[item.priority] ?? item.priority,
    priorityTone: TICKET_PRIORITY_TONES[item.priority] ?? 'low',
    status: item.status,
    statusLabel: TICKET_STATUS_LABELS[item.status] ?? item.status,
    statusTone: TICKET_STATUS_TONES[item.status] ?? 'open',
    dueAt: formatDateTime(item.dueAt),
    slaBreached: item.slaBreached,
    responseSlaMs: item.responseSlaMs,
    createdAt: formatDateTime(item.createdAt),
    updatedAt: formatDateTime(item.updatedAt),
  };
  return {
    ...listItem,
    description: item.description,
    evidenceUrls: item.evidenceUrls,
    targetClassId: item.targetClassId != null ? String(item.targetClassId) : null,
    messages: item.messages.map(mapAdminTicketMessage),
  };
}

export function mapPageAdminTicketList(response: PageAdminTicketApiResponse): PageAdminTicketList {
  return {
    items: response.content.map(mapAdminTicketListItem),
    page: response.page,
    size: response.size,
    totalElements: response.totalElements,
    totalPages: response.totalPages,
  };
}

export function buildTicketListQuery(filters: AdminTicketFilters): string {
  const params = new URLSearchParams();
  params.set('page', String(filters.page));
  params.set('size', String(filters.size));
  if (filters.status) params.set('status', filters.status);
  if (filters.category) params.set('category', filters.category);
  if (filters.priority) params.set('priority', filters.priority);
  if (filters.keyword?.trim()) params.set('keyword', filters.keyword.trim());
  return params.toString();
}
