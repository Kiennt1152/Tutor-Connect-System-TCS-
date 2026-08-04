import axiosClient from '../../../shared/api/axiosClient';
import type {
  CenterSummary,
  ClassRequest,
  CreateClassRequestPayload,
  MarketplaceClass,
} from '../types/marketplaceTypes';

export const MARKETPLACE_API_BASE = '/marketplace';

export const marketplaceApi = {
  getOpenClasses() {
    return axiosClient.get<MarketplaceClass[]>(`${MARKETPLACE_API_BASE}/classes?status=OPEN`);
  },
  getClass(classId: number) {
    return axiosClient.get<MarketplaceClass>(`${MARKETPLACE_API_BASE}/classes/${classId}`);
  },
  register(classId: number) {
    return axiosClient.post<{ message: string }>(
      `${MARKETPLACE_API_BASE}/classes/${classId}/register`,
    );
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
