import axiosClient from '../../../shared/api/axiosClient';
import type {
  AdminReviewApiResponse,
  DashboardApiResponse,
  PageUserListApiResponse,
  ReportApiResponse,
  ReviewModerationStatus,
  ReviewVerificationApiRequest,
  UpdateUserStatusApiRequest,
  UserListItemApiResponse,
  UserListFilters,
  VerificationDetailApiResponse,
  VerificationRequestApiResponse,
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

  getReviews(status?: ReviewModerationStatus) {
    const query = status ? `?status=${status}` : '';
    return axiosClient.get<AdminReviewApiResponse[]>(`${BASE}/reviews${query}`);
  },

  moderateReview(reviewId: number, status: ReviewModerationStatus) {
    return axiosClient.patch<AdminReviewApiResponse>(`${BASE}/reviews/${reviewId}`, { status });
  },

  deleteReview(reviewId: number) {
    return axiosClient.delete<void>(`${BASE}/reviews/${reviewId}`);
  },
};
