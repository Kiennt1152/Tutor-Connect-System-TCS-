import type {
  PageUserList,
  PageUserListApiResponse,
  UpdateUserStatusApiRequest,
  UserListFilters,
  UserListItem,
  UserListItemApiResponse,
  UserRole,
  UserStatus,
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
