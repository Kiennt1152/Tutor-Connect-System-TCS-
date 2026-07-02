import axiosClient from '../../../shared/api/axiosClient';
import type {
  PageUserListApiResponse,
  ReviewVerificationRequest,
  UpdateUserStatusApiRequest,
  UserListItemApiResponse,
  UserListFilters,
  VerificationDetail,
  VerificationListItem,
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

  // UC-11: Manage Document Verifications
  getVerifications() {
    return axiosClient.get<VerificationListItem[]>(`${BASE}/verifications`);
  },

  openVerification(verificationId: number) {
    return axiosClient.post<VerificationDetail>(`${BASE}/verifications/${verificationId}/open`);
  },

  reviewVerification(verificationId: number, payload: ReviewVerificationRequest) {
    return axiosClient.patch<VerificationListItem>(
      `${BASE}/verifications/${verificationId}`,
      payload,
    );
  },
};
