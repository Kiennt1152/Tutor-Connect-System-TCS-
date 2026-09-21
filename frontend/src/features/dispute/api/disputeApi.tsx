import axiosClient from '../../../shared/api/axiosClient';
import type {
  CreateClassIssueRequest,
  DisputeResponse,
  EvidenceUploadResponse,
  ParticipantDispute,
} from '../types/disputeTypes';

export const disputeApi = {
  async mine(classId?: number): Promise<ParticipantDispute[]> {
    return (await axiosClient.get<ParticipantDispute[]>('/disputes/mine', { params: { classId } })).data;
  },
  async explain(id: number, note: string, evidenceUrls: string[]): Promise<ParticipantDispute> {
    return (await axiosClient.post<ParticipantDispute>(`/disputes/${id}/explanation`, {
      note, evidenceUrls: evidenceUrls.join('\n'),
    })).data;
  },
  async withdraw(id: number, reason: string): Promise<ParticipantDispute> {
    return (await axiosClient.post<ParticipantDispute>(`/disputes/${id}/withdraw`, { reason })).data;
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

  async createClassIssue(payload: CreateClassIssueRequest): Promise<DisputeResponse> {
    const response = await axiosClient.post<DisputeResponse>('/class-issues', payload);
    return response.data;
  },
};
