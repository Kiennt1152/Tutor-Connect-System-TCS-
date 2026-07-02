import axiosClient from '../../../shared/api/axiosClient';
import type {
  DashboardApiResponse,
  PageUserListApiResponse,
  UpdateUserStatusApiRequest,
  UserListItemApiResponse,
  UserListFilters,
} from '../types/platformTypes';
import { buildUserListQuery } from '../mappers/platformMapper';

const BASE = '/platform';

export const platformApi = {
  getDashboard() {
    return axiosClient.get<DashboardApiResponse>(`${BASE}/dashboard`);
  },

  getUsers(filters: UserListFilters) {
    return axiosClient.get<PageUserListApiResponse>(`${BASE}/users?${buildUserListQuery(filters)}`);
  },

  updateUserStatus(userId: string, payload: UpdateUserStatusApiRequest) {
    return axiosClient.patch<UserListItemApiResponse>(`${BASE}/users/${userId}/status`, payload);
  },
};
