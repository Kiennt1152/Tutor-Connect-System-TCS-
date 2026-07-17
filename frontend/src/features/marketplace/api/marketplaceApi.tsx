import axiosClient from '../../../shared/api/axiosClient';
import type {
  ClassTerminationResponse,
  CreateClassTerminationRequest,
} from '../types/marketplaceTypes';

export const MARKETPLACE_API_BASE = '/marketplace';

export const marketplaceApi = {
  http: axiosClient,
  basePath: MARKETPLACE_API_BASE,

  async requestClassTermination(
    classId: number,
    payload: CreateClassTerminationRequest,
  ): Promise<ClassTerminationResponse> {
    const response = await axiosClient.post<ClassTerminationResponse>(
      `${MARKETPLACE_API_BASE}/classes/${classId}/termination`,
      payload,
    );
    return response.data;
  },
};
