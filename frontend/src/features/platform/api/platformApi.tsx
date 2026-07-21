import axiosClient from '../../../shared/api/axiosClient';
import type {
  AppealDisputeApiRequest,
  AdminDisputeReviewApiResponse,
  DashboardApiResponse,
  DisputeStatus,
  ExecuteRefundApiRequest,
  RefundExecutionApiResponse,
  ExecuteSettlementApiRequest,
  PageAdminWithdrawalApiResponse,
  PageUserListApiResponse,
  ReportApiResponse,
  ReviewVerificationApiRequest,
  ResolveDisputeApiRequest,
  UpdateUserStatusApiRequest,
  UserListItemApiResponse,
  UserListFilters,
  VerificationDetailApiResponse,
  VerificationRequestApiResponse,
  WithdrawalListFilters,
} from '../types/platformTypes';
import { buildUserListQuery, buildWithdrawalListQuery } from '../mappers/platformMapper';

const BASE = '/platform';

export const platformApi = {
  getDashboard() {
    return axiosClient.get<DashboardApiResponse>(`${BASE}/dashboard`);
  },

  getUsers(filters: UserListFilters) {
    return axiosClient.get<PageUserListApiResponse>(`${BASE}/users?${buildUserListQuery(filters)}`);
  },

  getWithdrawals(filters: WithdrawalListFilters) {
    return axiosClient.get<PageAdminWithdrawalApiResponse>(
      `/finance/withdrawals?${buildWithdrawalListQuery(filters)}`,
    );
  },

  acceptWithdrawal(withdrawalId: string) {
    return axiosClient.post(`/finance/withdrawals/${withdrawalId}/accept`);
  },

  updateUserStatus(userId: string, payload: UpdateUserStatusApiRequest) {
    return axiosClient.patch<UserListItemApiResponse>(`${BASE}/users/${userId}/status`, payload);
  },

  getVerifications() {
    return axiosClient.get<VerificationRequestApiResponse[]>(`${BASE}/verifications`);
  },

  getVerificationDetail(verificationId: string) {
    return axiosClient.get<VerificationDetailApiResponse>(`${BASE}/verifications/${verificationId}`);
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

  getDisputes(status?: DisputeStatus) {
    const query = status ? `?status=${status}` : '';
    return axiosClient.get<AdminDisputeReviewApiResponse[]>(`/disputes${query}`);
  },

  getDispute(disputeId: string) {
    return axiosClient.get<AdminDisputeReviewApiResponse>(`/disputes/${disputeId}`);
  },

  resolveDispute(disputeId: string, payload: ResolveDisputeApiRequest) {
    return axiosClient.post<AdminDisputeReviewApiResponse>(`/disputes/${disputeId}/resolve`, payload);
  },

  appealDispute(disputeId: string, payload: AppealDisputeApiRequest) {
    return axiosClient.post<AdminDisputeReviewApiResponse>(`/disputes/${disputeId}/appeal`, payload);
  },

  executeSettlement(payload: ExecuteSettlementApiRequest) {
    return axiosClient.post<string>('/finance/settlements/execute', payload);
  },

  executeRefund(payload: ExecuteRefundApiRequest) {
    return axiosClient.post<RefundExecutionApiResponse>('/finance/refunds/execute', payload);
  },
};
