import axiosClient from '../../../shared/api/axiosClient';
import type { MarketplaceClass } from '../types/marketplaceTypes';

export const MARKETPLACE_API_BASE = '/marketplace';

export const marketplaceApi = {
  getOpenClasses() {
    return axiosClient.get<MarketplaceClass[]>(`${MARKETPLACE_API_BASE}/classes?status=OPEN`);
  },
  register(classId: number) {
    return axiosClient.post<{ message: string }>(
      `${MARKETPLACE_API_BASE}/classes/${classId}/register`,
    );
  },
};
