import axiosClient from '../../../shared/api/axiosClient';
import type {
  CreateClassIssueRequest,
  DisputeResponse,
  EvidenceUploadResponse,
} from '../types/disputeTypes';

export const disputeApi = {
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

  async createClassIssue(payload: CreateClassIssueRequest): Promise<DisputeResponse> {
    const response = await axiosClient.post<DisputeResponse>('/class-issues', payload);
    return response.data;
  },
};
