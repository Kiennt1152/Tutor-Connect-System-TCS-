import axiosClient from '../../../shared/api/axiosClient';

export interface NotificationItem {
  notificationId: number;
  type: string;
  title: string;
  content: string;
  isRead: boolean;
  createdAt: string;
  referenceType?: string | null;
  referenceId?: number | null;
}

export const notificationsApi = {
  list: () =>
    axiosClient.get<NotificationItem[]>('/messaging/notifications').then((r) => r.data),
  markRead: (id: number) =>
    axiosClient
      .patch<{ message: string }>(`/messaging/notifications/${id}/read`)
      .then((r) => r.data),
};
