import axiosClient from '../../../shared/api/axiosClient';
import type {
  DashboardApiResponse,
  PageUserListApiResponse,
  ReportApiResponse,
  ReviewVerificationApiRequest,
  UpdateUserStatusApiRequest,
  UserListItemApiResponse,
  UserListFilters,
  VerificationRequestApiResponse,
} from '../types/platformTypes';
import type { AdminWithdrawal, WithdrawalStatus } from '../../finance/types/financeTypes';
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

  getVerifications() {
    return axiosClient.get<VerificationRequestApiResponse[]>(`${BASE}/verifications`);
  },

  reviewVerification(verificationId: string, payload: ReviewVerificationApiRequest) {
    return axiosClient.patch<VerificationRequestApiResponse>(
      `${BASE}/verifications/${verificationId}`,
      payload,
    );
  },

  getReports() {
    return axiosClient.get<ReportApiResponse[]>(`${BASE}/reports`);
  },

  getWithdrawals(status?: WithdrawalStatus) {
    const query = status ? `?status=${status}` : '';
    return axiosClient.get<AdminWithdrawal[]>(`${BASE}/withdrawals${query}`);
  },

  reviewWithdrawal(withdrawalId: number, payload: { approve: boolean; reason?: string }) {
    return axiosClient.patch<AdminWithdrawal>(`${BASE}/withdrawals/${withdrawalId}`, payload);
  },
};
