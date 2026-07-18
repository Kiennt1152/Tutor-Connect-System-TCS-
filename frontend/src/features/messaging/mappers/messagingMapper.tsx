import type {
  SupportTicketApiResponse,
  SupportTicketCategory,
  SupportTicketDetail,
  SupportTicketDetailApiResponse,
  SupportTicketItem,
  SupportTicketPriority,
  SupportTicketStatus,
  TicketMessage,
  TicketMessageApiResponse,
} from '../types/messagingTypes';

const CATEGORY_LABELS: Record<SupportTicketCategory, string> = {
  DISPUTE: 'Tranh chấp',
  SYSTEM_ERROR: 'Lỗi hệ thống',
  REPORT_USER: 'Báo cáo người dùng',
  BUG_REPORT: 'Lỗi phần mềm',
  INQUIRY: 'Câu hỏi chung',
};

const PRIORITY_LABELS: Record<SupportTicketPriority, string> = {
  LOW: 'Thấp',
  MEDIUM: 'Trung bình',
  HIGH: 'Cao',
  URGENT: 'Khẩn cấp',
};

const PRIORITY_TONES: Record<SupportTicketPriority, 'low' | 'medium' | 'high' | 'urgent'> = {
  LOW: 'low',
  MEDIUM: 'medium',
  HIGH: 'high',
  URGENT: 'urgent',
};

const STATUS_LABELS: Record<SupportTicketStatus, string> = {
  OPEN: 'Chờ xử lý',
  IN_PROGRESS: 'Đang xử lý',
  IN_REVIEW: 'Chờ phản hồi',
  RESOLVED: 'Đã giải quyết',
  CLOSED: 'Đã đóng',
};

const STATUS_TONES: Record<SupportTicketStatus, 'open' | 'active' | 'review' | 'done'> = {
  OPEN: 'open',
  IN_PROGRESS: 'active',
  IN_REVIEW: 'review',
  RESOLVED: 'done',
  CLOSED: 'done',
};

const formatDateTime = (value: string | null | undefined): string => {
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

export function mapTicketItem(item: SupportTicketApiResponse): SupportTicketItem {
  return {
    id: String(item.ticketId),
    category: item.category,
    categoryLabel: CATEGORY_LABELS[item.category] ?? item.category,
    subject: item.subject,
    priority: item.priority,
    priorityLabel: PRIORITY_LABELS[item.priority] ?? item.priority,
    priorityTone: PRIORITY_TONES[item.priority] ?? 'low',
    status: item.status,
    statusLabel: STATUS_LABELS[item.status] ?? item.status,
    statusTone: STATUS_TONES[item.status] ?? 'open',
    createdAt: formatDateTime(item.createdAt),
    updatedAt: formatDateTime(item.updatedAt),
  };
}

export function mapTicketMessage(msg: TicketMessageApiResponse): TicketMessage {
  return {
    id: String(msg.messageId),
    senderId: String(msg.senderId),
    senderName: msg.senderName,
    fromAdmin: msg.fromAdmin,
    content: msg.content,
    sentAt: formatDateTime(msg.sentAt),
  };
}

export function mapTicketDetail(item: SupportTicketDetailApiResponse): SupportTicketDetail {
  return {
    ...mapTicketItem(item),
    description: item.description,
    evidenceUrls: item.evidenceUrls,
    assignedAdminId: item.assignedAdminId != null ? String(item.assignedAdminId) : null,
    messages: item.messages.map(mapTicketMessage),
  };
}
