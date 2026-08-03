import axiosClient from '../../../shared/api/axiosClient';
import type { EvidenceUploadResponse } from '../../dispute/types/disputeTypes';
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

  async uploadEvidenceImage(file: File): Promise<EvidenceUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    const response = await axiosClient.post<EvidenceUploadResponse>(
      '/disputes/evidence/upload',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      },
    );
    return response.data;
  },
};
