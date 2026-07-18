import axiosClient from '../../../shared/api/axiosClient';
import type {
  AdminTicketDetailApiResponse,
  AdminTicketFilters,
  CloseTicketApiRequest,
  DashboardApiResponse,
  PageAdminTicketApiResponse,
  PageUserListApiResponse,
  ReportApiResponse,
  RespondTicketApiRequest,
  ReviewVerificationApiRequest,
  UpdateTicketApiRequest,
  UpdateUserStatusApiRequest,
  UserListItemApiResponse,
  UserListFilters,
  VerificationDetailApiResponse,
  VerificationRequestApiResponse,
} from '../types/platformTypes';
import { buildUserListQuery, buildTicketListQuery } from '../mappers/platformMapper';

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

  getTickets(filters: AdminTicketFilters) {
    return axiosClient.get<PageAdminTicketApiResponse>(`${BASE}/tickets?${buildTicketListQuery(filters)}`);
  },

  getTicketDetail(ticketId: string) {
    return axiosClient.get<AdminTicketDetailApiResponse>(`${BASE}/tickets/${ticketId}`);
  },

  updateTicket(ticketId: string, payload: UpdateTicketApiRequest) {
    return axiosClient.patch<AdminTicketDetailApiResponse>(`${BASE}/tickets/${ticketId}`, payload);
  },

  respondToTicket(ticketId: string, payload: RespondTicketApiRequest) {
    return axiosClient.post<AdminTicketDetailApiResponse>(`${BASE}/tickets/${ticketId}/messages`, payload);
  },

  closeTicket(ticketId: string, payload: CloseTicketApiRequest) {
    return axiosClient.patch<AdminTicketDetailApiResponse>(`${BASE}/tickets/${ticketId}/status`, payload);
  },
};
