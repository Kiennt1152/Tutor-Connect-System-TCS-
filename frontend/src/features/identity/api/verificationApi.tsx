import axiosClient from '../../../shared/api/axiosClient';
import type { Verification, VerificationDecision, VerificationSubmission } from '../types/verificationTypes';

const BASE = '/identity/verification';

export const verificationApi = {
  async uploadFile(userId: number, file: File) {
    const formData = new FormData();
    formData.append('userId', String(userId));
    formData.append('file', file);

    const response = await axiosClient.post<{ fileId: number; fileName: string; fileUrl: string; mimeType: string; fileSize: number }>(
      `${BASE}/upload`,
      formData,
      {
        headers: { 'Content-Type': 'multipart/form-data' },
      }
    );
    return response.data;
  },

  async submitVerification(userId: number, payload: VerificationSubmission) {
    const response = await axiosClient.post<Verification>(
      `${BASE}/submit?userId=${userId}`,
      payload
    );
    return response.data;
  },

  async getVerificationsByUser(userId: number) {
    const response = await axiosClient.get<Verification[]>(`${BASE}/user/${userId}`);
    return response.data;
  },

  async getVerificationById(verificationId: number) {
    const response = await axiosClient.get<Verification>(`${BASE}/${verificationId}`);
    return response.data;
  },

  async getVerificationsByStatus(status: string) {
    const response = await axiosClient.get<Verification[]>(`${BASE}/status/${status}`);
    return response.data;
  },

  async reviewVerification(verificationId: number, adminId: number, payload: VerificationDecision) {
    const response = await axiosClient.post<Verification>(
      `${BASE}/${verificationId}/review?adminId=${adminId}`,
      payload
    );
    return response.data;
  },

  async getModerationQueue() {
    const response = await axiosClient.get<Verification[]>(`${BASE}/moderation-queue`);
    return response.data;
  },
};
