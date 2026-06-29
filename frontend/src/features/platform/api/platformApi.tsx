import axiosClient from '../../../shared/api/axiosClient';
import type {
  PageUserListApiResponse,
  UpdateUserStatusApiRequest,
  UserListItemApiResponse,
  UserListFilters,
} from '../types/platformTypes';
import { buildUserListQuery } from '../mappers/platformMapper';

const BASE = '/platform';

export const platformApi = {
  getUsers(filters: UserListFilters) {
    return axiosClient.get<PageUserListApiResponse>(`${BASE}/users?${buildUserListQuery(filters)}`);
  },

  updateUserStatus(userId: string, payload: UpdateUserStatusApiRequest) {
    return axiosClient.patch<UserListItemApiResponse>(`${BASE}/users/${userId}/status`, payload);
  },
};
