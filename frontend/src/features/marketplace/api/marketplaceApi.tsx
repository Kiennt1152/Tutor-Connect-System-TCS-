import axiosClient from '../../../shared/api/axiosClient';
import type {
  ClassTerminationResponse,
  CreateClassTerminationRequest,
  CenterSummary,
  ClassRequest,
  CreateClassRequestPayload,
  MarketplaceClass,
} from '../types/marketplaceTypes';

export const MARKETPLACE_API_BASE = '/marketplace';

export const marketplaceApi = {
  http: axiosClient,
  basePath: MARKETPLACE_API_BASE,

  getOpenClasses() {
    return axiosClient.get<MarketplaceClass[]>(`${MARKETPLACE_API_BASE}/classes?status=OPEN`);
  },

  getClass(
    classId: number,
    target?: { assignmentId?: number; classStudentId?: number },
  ) {
    return axiosClient.get<MarketplaceClass>(`${MARKETPLACE_API_BASE}/classes/${classId}`, {
      params: {
        assignmentId: target?.assignmentId,
        classStudentId: target?.classStudentId,
      },
    });
  },

  register(classId: number) {
    return axiosClient.post<{ message: string }>(
      `${MARKETPLACE_API_BASE}/classes/${classId}/register`,
    );
  },

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

  // ----- Yêu cầu mở lớp gửi tới một trung tâm (phía phụ huynh) -----
  listCenters() {
    return axiosClient.get<CenterSummary[]>(`${MARKETPLACE_API_BASE}/centers`);
  },
  createClassRequest(centerId: number, payload: CreateClassRequestPayload) {
    return axiosClient.post<ClassRequest>(
      `${MARKETPLACE_API_BASE}/centers/${centerId}/class-requests`,
      payload,
    );
  },
  getMyClassRequests() {
    return axiosClient.get<ClassRequest[]>(`${MARKETPLACE_API_BASE}/class-requests/mine`);
  },
  cancelClassRequest(requestId: string) {
    return axiosClient.delete<{ message: string }>(
      `${MARKETPLACE_API_BASE}/class-requests/${requestId}`,
    );
  },
};
