import axiosClient from '../../../shared/api/axiosClient';
import type { CreateClassIssueRequest, DisputeResponse } from '../types/disputeTypes';

export const disputeApi = {
  async createClassIssue(payload: CreateClassIssueRequest): Promise<DisputeResponse> {
    const response = await axiosClient.post<DisputeResponse>('/class-issues', payload);
    return response.data;
  },
};
