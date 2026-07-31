import axiosClient from '../../../shared/api/axiosClient';
import type { NotificationItem, SubmitDisputeEvidenceRequest } from '../types/messagingTypes';

export const MESSAGING_API_BASE = '/messaging';

export const messagingApi = {
  async getNotifications() {
    const response = await axiosClient.get<NotificationItem[]>(
      `${MESSAGING_API_BASE}/notifications`,
    );
    return response.data;
  },

  async markNotificationRead(notificationId: number) {
    await axiosClient.patch(`${MESSAGING_API_BASE}/notifications/${notificationId}/read`);
  },

  async submitDisputeEvidence(disputeId: number, payload: SubmitDisputeEvidenceRequest) {
    await axiosClient.post(`/disputes/${disputeId}/evidence`, payload);
  },
};
