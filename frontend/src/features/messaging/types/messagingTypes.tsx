export type SupportTicketCategory =
  | 'DISPUTE'
  | 'SYSTEM_ERROR'
  | 'REPORT_USER'
  | 'BUG_REPORT'
  | 'INQUIRY';

export type SupportTicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export type SupportTicketStatus =
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'IN_REVIEW'
  | 'RESOLVED'
  | 'CLOSED';

/* ── API response shapes ── */

export interface SupportTicketApiResponse {
  ticketId: number;
  userId: number;
  targetClassId: number | null;
  assignedAdminId: number | null;
  category: SupportTicketCategory;
  subject: string;
  description: string;
  evidenceUrls: string | null;
  priority: SupportTicketPriority;
  status: SupportTicketStatus;
  resolvedAt: string | null;
  closedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TicketMessageApiResponse {
  messageId: number;
  senderId: number;
  senderName: string;
  fromAdmin: boolean;
  content: string;
  sentAt: string;
}

export interface SupportTicketDetailApiResponse extends SupportTicketApiResponse {
  messages: TicketMessageApiResponse[];
}

/* ── API request shapes ── */

export interface CreateSupportTicketApiRequest {
  category: SupportTicketCategory;
  subject: string;
  description: string;
  priority?: SupportTicketPriority;
  targetClassId?: number;
  evidenceUrls?: string;
}

/* ── View models ── */

export interface SupportTicketItem {
  id: string;
  category: SupportTicketCategory;
  categoryLabel: string;
  subject: string;
  priority: SupportTicketPriority;
  priorityLabel: string;
  priorityTone: 'low' | 'medium' | 'high' | 'urgent';
  status: SupportTicketStatus;
  statusLabel: string;
  statusTone: 'open' | 'active' | 'review' | 'done';
  createdAt: string;
  updatedAt: string;
}

export interface TicketMessage {
  id: string;
  senderId: string;
  senderName: string;
  fromAdmin: boolean;
  content: string;
  sentAt: string;
}

export interface SupportTicketDetail extends SupportTicketItem {
  description: string;
  evidenceUrls: string | null;
  assignedAdminId: string | null;
  messages: TicketMessage[];
}

export interface NotificationApiResponse {
  notificationId: number;
  type: string;
  title: string | null;
  content: string;
  isRead: boolean;
  createdAt: string;
}
