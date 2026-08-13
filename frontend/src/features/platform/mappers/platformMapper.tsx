import type {
  AdminWithdrawalApiResponse,
  AdminWithdrawalItem,
  AdminDisputeReviewApiResponse,
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
  DisputeReviewItem,
  DisputeStatus,
  EscrowStatus,
  PageAdminTicketApiResponse,
  PageAdminTicketList,
  PageUserList,
  PageAdminWithdrawalApiResponse,
  PageAdminWithdrawalList,
  PageUserListApiResponse,
  PlatformDashboard,
  ReportApiResponse,
  ReportItem,
  RefundRequestApiResponse,
  RefundRequestItem,
  RefundRequestStatus,
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
  WithdrawalListFilters,
  WithdrawalRequestStatus,
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

const formatDate = (value: string | null | undefined) => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date);
};

const formatCurrency = (value: number | null | undefined) => {
  if (typeof value !== 'number') return '—';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value);
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

function verificationTypeLabel(type: VerificationType, role?: string | null): string {
  if (type === 'TUTOR_PROFILE' && role === 'CLIENT') {
    return 'Xác minh danh tính';
  }
  return VERIFICATION_TYPE_LABELS[type] ?? type;
}

const VERIFICATION_STATUS_LABELS: Record<VerificationStatus, string> = {
  DRAFT: 'Nháp',
  SUBMITTED: 'Chờ duyệt',
  UNDER_REVIEW: 'Đang xem xét',
  VERIFIED: 'Đã duyệt',
  REJECTED: 'Từ chối',
};

const REPORT_CATEGORY_LABELS: Record<ReportCategory, string> = {
  FRAUD: 'Sai sự thật / gian lận',
  ABUSE: 'Lăng mạ / xúc phạm',
  SPAM: 'Spam',
  INAPPROPRIATE: 'Nội dung không phù hợp',
  OTHER: 'Lý do khác',
};

const REPORT_STATUS_LABELS: Record<ReportStatus, string> = {
  PENDING: 'Đang mở',
  RESOLVED: 'Đã xử lý',
};

const CLASS_STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Nháp',
  OPEN: 'Đang mở',
  MATCHED: 'Đã ghép',
  ENROLLMENT_CLOSED: 'Đã đóng ghi danh',
  IN_PROGRESS: 'Đang diễn ra',
  COMPLETED: 'Đã hoàn tất',
  CANCELLED: 'Đã hủy',
  DISPUTED: 'Đang tranh chấp',
};

const WITHDRAWAL_STATUS_LABELS: Record<WithdrawalRequestStatus, string> = {
  PENDING: 'Chờ xử lý',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',
  COMPLETED: 'Thành công',
};

const REFUND_STATUS_LABELS: Record<RefundRequestStatus, string> = {
  PENDING: 'Chờ xử lý',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',
  COMPLETED: 'Đã hoàn tiền',
};

const DISPUTE_STATUS_LABELS: Record<DisputeStatus, string> = {
  OPEN: 'Mới mở',
  UNDER_INVESTIGATION: 'Đang xem xét',
  WAITING: 'Chờ bổ sung',
  RESOLVED: 'Đã xử lý',
};

const ESCROW_STATUS_LABELS: Record<EscrowStatus, string> = {
  PENDING: 'Chờ khóa',
  FUNDED: 'Đã khóa',
  ON_HOLD: 'Tạm giữ',
  DISPUTED: 'Tranh chấp',
  RELEASED: 'Đã giải ngân',
  REFUNDED: 'Đã hoàn tiền',
};

const TARGET_TYPE_LABELS: Record<string, string> = {
  USER: 'Người dùng',
  TUTOR: 'Gia sư',
  CLASS: 'Lớp học',
  MESSAGE: 'Tin nhắn',
};

function extractClassIssueUserDescription(description: string | null | undefined) {
  if (!description?.trim()) return '—';
  const beforeHandling =
    description.split('[UC-30]')[0].split('[UC-55]')[0].trim() || description.trim();
  const marker = 'Mô tả:';
  const markerIndex = beforeHandling.indexOf(marker);
  if (markerIndex < 0) return beforeHandling;
  const body = beforeHandling.slice(markerIndex + marker.length).trim();
  return body || beforeHandling;
}

export function mapVerificationItem(item: VerificationRequestApiResponse): VerificationRequestItem {
  const canReview = item.status === 'SUBMITTED' || item.status === 'UNDER_REVIEW';
  const isReviewed = item.status === 'VERIFIED' || item.status === 'REJECTED';
  return {
    id: String(item.verificationId),
    userId: String(item.userId),
    userEmail: item.userEmail,
    userRole: item.userRole,
    verificationType: item.verificationType,
    typeLabel: verificationTypeLabel(item.verificationType, item.userRole),
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
  const evidenceUrlList = item.evidenceUrlList ?? [];
  return {
    id: String(item.reportId),
    reporterId: String(item.reporterId),
    reporterEmail: item.reporterEmail?.trim() || `Người dùng #${item.reporterId}`,
    targetType: item.targetType,
    targetTypeLabel: TARGET_TYPE_LABELS[item.targetType] ?? item.targetType,
    targetId: String(item.targetId),
    classTitle:
      item.classTitle?.trim()
      || item.reportedReview?.classTitle?.trim()
      || (item.targetType === 'CLASS' ? `Lớp #${item.targetId}` : '—'),
    reportedReview: item.reportedReview ?? null,
    classStatus: item.classStatus ? (CLASS_STATUS_LABELS[item.classStatus] ?? item.classStatus) : '—',
    category: item.category,
    categoryLabel: REPORT_CATEGORY_LABELS[item.category] ?? item.category,
    description: item.description?.trim() || '—',
    userDescription: extractClassIssueUserDescription(item.description),
    evidenceUrlList,
    evidenceCount: evidenceUrlList.length,
    status: item.status,
    statusLabel: REPORT_STATUS_LABELS[item.status] ?? item.status,
    issueType: item.issueType?.trim() || '—',
    issueTypeLabel: item.issueTypeLabel?.trim() || '—',
    lessonRef: item.lessonRef?.trim() || '—',
    occurredAt: formatDate(item.occurredAt),
    requestedAction: item.requestedAction?.trim() || '—',
    requestedActionLabel: item.requestedActionLabel?.trim() || '—',
    linkedDisputeId: item.linkedDisputeId,
    createdAt: formatDateTime(item.createdAt),
    updatedAt: formatDateTime(item.updatedAt),
    raw: item,
  };
}

export function mapAdminWithdrawalItem(item: AdminWithdrawalApiResponse): AdminWithdrawalItem {
  return {
    id: String(item.withdrawalId),
    walletId: item.walletId ? String(item.walletId) : '—',
    requester: item.requesterEmail?.trim() || (item.walletId ? `Ví #${item.walletId}` : '—'),
    amount: formatCurrency(item.amount),
    rawAmount: item.amount,
    status: item.status,
    statusLabel: WITHDRAWAL_STATUS_LABELS[item.status] ?? item.status,
    bankName: item.bankName?.trim() || '—',
    accountNo: item.accountNo?.trim() || '',
    accountNoMasked: item.accountNoMasked?.trim() || '—',
    accountHolderName: item.accountHolderName?.trim() || '—',
    referenceCode: item.referenceCode?.trim() || '—',
    transactionStatusLabel: item.transactionStatus
      ? (item.transactionStatus === 'SUCCESS' ? 'Thành công'
        : item.transactionStatus === 'PENDING' ? 'Đang chờ'
        : item.transactionStatus === 'FAILED' ? 'Thất bại'
        : item.transactionStatus === 'CANCELLED' ? 'Đã hủy'
        : item.transactionStatus)
      : '—',
    requestedAt: formatDateTime(item.requestedAt),
    processedAt: formatDateTime(item.processedAt),
    canApprove: item.status === 'PENDING',
    canReject: item.status === 'PENDING' || item.status === 'APPROVED',
    canMarkTransferFailed: item.status === 'APPROVED',
    raw: item,
  };
}

export function mapPageAdminWithdrawalList(
  response: PageAdminWithdrawalApiResponse,
): PageAdminWithdrawalList {
  return {
    items: response.content.map(mapAdminWithdrawalItem),
    page: response.page,
    size: response.size,
    totalElements: response.totalElements,
    totalPages: response.totalPages,
  };
}

export function buildWithdrawalListQuery(filters: WithdrawalListFilters) {
  const params = new URLSearchParams();
  params.set('page', String(filters.page));
  params.set('size', String(filters.size));
  if (filters.status) params.set('status', filters.status);
  return params.toString();
}

export function mapDisputeReviewItem(item: AdminDisputeReviewApiResponse): DisputeReviewItem {
  const reportId = item.reportId ? String(item.reportId) : '—';
  const reporter = item.reporterEmail?.trim() || (item.reporterId ? `#${item.reporterId}` : '—');
  const targetType = item.targetType ? (TARGET_TYPE_LABELS[item.targetType] ?? item.targetType) : '—';
  const targetId = item.targetId ? `#${item.targetId}` : '—';
  const category = item.category ? (REPORT_CATEGORY_LABELS[item.category] ?? item.category) : '—';
  const escrowStatus = item.escrow?.status ?? null;

  return {
    id: String(item.disputeId),
    status: item.disputeStatus,
    statusLabel: DISPUTE_STATUS_LABELS[item.disputeStatus] ?? item.disputeStatus,
    reportId,
    reporter,
    target: `${targetType} ${targetId}`,
    category,
    description: item.description?.trim() || '—',
    evidenceCount: item.evidenceUrlList?.length ?? 0,
    escrowStatus,
    escrowStatusLabel: escrowStatus ? (ESCROW_STATUS_LABELS[escrowStatus] ?? escrowStatus) : '—',
    amount: formatCurrency(item.escrow?.amount),
    classTitle: item.tutoringClass?.title?.trim() || '—',
    createdAt: formatDateTime(item.disputeCreatedAt),
    raw: item,
  };
}

export function mapRefundRequestItem(item: RefundRequestApiResponse): RefundRequestItem {
  const escrowStatus = item.escrowStatus ?? null;
  return {
    id: String(item.refundId),
    escrowId: item.escrowId ? String(item.escrowId) : '—',
    requester: item.requesterEmail?.trim() || (item.requesterId ? `Người dùng #${item.requesterId}` : '—'),
    classTitle: item.classTitle?.trim() || (item.classId ? `Lớp #${item.classId}` : '—'),
    amount: formatCurrency(item.amount),
    rawAmount: item.amount,
    escrowAmount: formatCurrency(item.escrowAmount),
    bankName: item.bankName?.trim() || '—',
    accountNoMasked: item.accountNoMasked?.trim() || '—',
    accountHolderName: item.accountHolderName?.trim() || '—',
    refundReferenceCode: item.refundReferenceCode?.trim() || '—',
    transferStatus: item.transferStatus?.trim() || '—',
    status: item.status,
    statusLabel: REFUND_STATUS_LABELS[item.status] ?? item.status,
    escrowStatus,
    escrowStatusLabel: escrowStatus ? (ESCROW_STATUS_LABELS[escrowStatus] ?? escrowStatus) : '—',
    reason: item.reason?.trim() || '—',
    requestedAt: formatDateTime(item.requestedAt),
    processedAt: formatDateTime(item.processedAt),
    canDecide: item.status === 'PENDING',
    raw: item,
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
