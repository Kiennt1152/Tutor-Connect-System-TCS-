import type {
  DashboardApiResponse,
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
    totalUsers: response.totalUsers,
    totalTutors: response.totalTutors,
    totalClasses: response.totalClasses,
    pendingVerifications: response.pendingVerifications,
    openReports: response.openReports,
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
): ReviewVerificationApiRequest {
  return {
    status,
    adminNotes: adminNotes?.trim() || undefined,
  };
}
