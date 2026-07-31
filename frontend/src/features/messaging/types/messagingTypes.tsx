export type NotificationType =
  | 'PAYMENT'
  | 'APPLICATION'
  | 'SYSTEM'
  | 'CLASS'
  | 'VERIFICATION'
  | 'CHAT';

export interface NotificationItem {
  notificationId: number;
  type: NotificationType;
  title: string | null;
  content: string;
  referenceType: string | null;
  referenceId: number | null;
  isRead: boolean;
  createdAt: string | null;
}

export interface SubmitDisputeEvidenceRequest {
  evidenceUrls: string;
  note?: string;
}
