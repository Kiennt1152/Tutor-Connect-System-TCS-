import axiosClient from '../../../shared/api/axiosClient';
import type {
  AdminDisputeReviewApiResponse,
  AdminReviewApiResponse,
  AdminTicketDetailApiResponse,
  AdminTicketFilters,
  AnnouncementApiResponse,
  AppealDisputeApiRequest,
  AuditLogFilters,
  CloseTicketApiRequest,
  DashboardApiResponse,
  DisputeStatus,
  ExecuteRefundApiRequest,
  ExecuteSettlementApiRequest,
  IssuePenaltyApiRequest,
  PageAdminTicketApiResponse,
  PageAdminWithdrawalApiResponse,
  PageAuditLogApiResponse,
  PagePenaltyApiResponse,
  PageTaskItemApiResponse,
  PageUserListApiResponse,
  PenaltyApiResponse,
  PenaltyFilters,
  RefundDecisionApiRequest,
  RefundExecutionApiResponse,
  RefundRequestApiResponse,
  RefundRequestStatus,
  ReportApiResponse,
  ReviewModerationStatus,
  RespondTicketApiRequest,
  ReviewVerificationApiRequest,
  ResolveClassIssueRequest,
  ResolveDisputeApiRequest,
  ResolveReportApiRequest,
  ResolveReviewReportRequest,
  RevokePenaltyApiRequest,
  TaskFilters,
  TaskQueueSummaryApiResponse,
  UpdateTicketApiRequest,
  UpdateUserStatusApiRequest,
  UpsertAnnouncementApiRequest,
  UserListItemApiResponse,
  UserListFilters,
  VerificationDetailApiResponse,
  VerificationRequestApiResponse,
  WithdrawalDecisionApiRequest,
  WithdrawalListFilters,
  AnalyticsSummaryApiResponse,
  NotificationTemplateApiResponse,
  NotificationTemplatePreviewApiResponse,
  UpsertNotificationTemplateApiRequest,
  AdminEscrowPageApiResponse,
  CircumventionStatus,
  PageCircumventionEventApiResponse,
  CircumventionEventApiResponse,
  AiKnowledgeStatsApiResponse,
  AiKnowledgeReindexApiResponse,
} from '../types/platformTypes';
import {
  buildTicketListQuery,
  buildUserListQuery,
  buildWithdrawalListQuery,
} from '../mappers/platformMapper';

const BASE = '/platform';

export const platformApi = {
  getDashboard(from?: string, to?: string, granularity: string = 'DAY') {
    const params = new URLSearchParams();
    if (from) params.append('from', from);
    if (to) params.append('to', to);
    params.append('granularity', granularity);
    return axiosClient.get<DashboardApiResponse>(`${BASE}/dashboard?${params.toString()}`);
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

  approveWithdrawal(withdrawalId: string) {
    return axiosClient.post(`/finance/withdrawals/${withdrawalId}/approve`);
  },

  rejectWithdrawal(withdrawalId: string, payload: WithdrawalDecisionApiRequest) {
    return axiosClient.post(`/finance/withdrawals/${withdrawalId}/reject`, payload);
  },

  markWithdrawalTransferFailed(withdrawalId: string, payload: WithdrawalDecisionApiRequest) {
    return axiosClient.post(`/finance/withdrawals/${withdrawalId}/transfer-failed`, payload);
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

  resolveClassIssue(reportId: string, payload: ResolveClassIssueRequest) {
    return axiosClient.patch<ReportApiResponse>(`${BASE}/reports/${reportId}/resolve`, payload);
  },

  resolveReviewReport(reportId: string, payload: ResolveReviewReportRequest) {
    return axiosClient.patch<ReportApiResponse>(
      `${BASE}/reports/${reportId}/resolve-review`,
      payload,
    );
  },

  resolveReport(reportId: number, payload: ResolveReportApiRequest) {
    return axiosClient.patch<ReportApiResponse>(`${BASE}/reports/${reportId}`, payload);
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

  getRefundRequests(status?: RefundRequestStatus) {
    const query = status ? `?status=${status}` : '';
    return axiosClient.get<RefundRequestApiResponse[]>(`/finance/refund-requests${query}`);
  },

  approveRefundRequest(refundId: string, payload: RefundDecisionApiRequest) {
    return axiosClient.post<RefundRequestApiResponse>(`/finance/refund-requests/${refundId}/approve`, payload);
  },

  rejectRefundRequest(refundId: string, payload: RefundDecisionApiRequest) {
    return axiosClient.post<RefundRequestApiResponse>(`/finance/refund-requests/${refundId}/reject`, payload);
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

  mergeTicket(ticketId: string, payload: { targetTicketId: number; reason?: string }) {
    return axiosClient.post<AdminTicketDetailApiResponse>(`${BASE}/tickets/${ticketId}/merge`, payload);
  },

  redirectTicketToDispute(ticketId: string, payload: { targetClassId?: number; notes?: string }) {
    return axiosClient.post<AdminTicketDetailApiResponse>(`${BASE}/tickets/${ticketId}/redirect-dispute`, payload);
  },

  getAnnouncements() {
    return axiosClient.get<AnnouncementApiResponse[]>(`${BASE}/announcements`);
  },

  createAnnouncement(payload: UpsertAnnouncementApiRequest) {
    return axiosClient.post<AnnouncementApiResponse>(`${BASE}/announcements`, payload);
  },

  updateAnnouncement(announcementId: number, payload: UpsertAnnouncementApiRequest) {
    return axiosClient.patch<AnnouncementApiResponse>(`${BASE}/announcements/${announcementId}`, payload);
  },

  deleteAnnouncement(announcementId: number) {
    return axiosClient.delete(`${BASE}/announcements/${announcementId}`);
  },

  getEscrows(filters: Record<string, string>) {
    return axiosClient.get<AdminEscrowPageApiResponse>(`${BASE}/escrows?${new URLSearchParams(filters)}`);
  },

  getCircumventionEvents(status?: CircumventionStatus) {
    const query = status ? `?status=${status}` : '';
    return axiosClient.get<PageCircumventionEventApiResponse>(`${BASE}/circumvention-events${query}`);
  },

  reviewCircumventionEvent(eventId: number, status: Exclude<CircumventionStatus, 'PENDING'>, note: string) {
    return axiosClient.patch<CircumventionEventApiResponse>(`${BASE}/circumvention-events/${eventId}`, { status, note });
  },

  getCircumventionConversation(eventId: number) {
    return axiosClient.get<import('../types/platformTypes').CircumventionConversationApiResponse>(
      `${BASE}/circumvention-events/${eventId}/conversation`,
    );
  },

  getNotificationTemplates() {
    return axiosClient.get<NotificationTemplateApiResponse[]>(`${BASE}/notification-templates`);
  },

  createNotificationTemplate(payload: UpsertNotificationTemplateApiRequest) {
    return axiosClient.post<NotificationTemplateApiResponse>(`${BASE}/notification-templates`, payload);
  },

  updateNotificationTemplate(templateId: number, payload: UpsertNotificationTemplateApiRequest) {
    return axiosClient.patch<NotificationTemplateApiResponse>(`${BASE}/notification-templates/${templateId}`, payload);
  },

  disableNotificationTemplate(templateId: number) {
    return axiosClient.delete<NotificationTemplateApiResponse>(`${BASE}/notification-templates/${templateId}`);
  },

  previewNotificationTemplate(payload: Pick<UpsertNotificationTemplateApiRequest, 'titleTemplate' | 'contentTemplate'> & { variables: Record<string, string> }) {
    return axiosClient.post<NotificationTemplatePreviewApiResponse>(`${BASE}/notification-templates/preview`, payload);
  },

  getPublicAnnouncements() {
    return axiosClient.get<AnnouncementApiResponse[]>('/home/announcements');
  },

  getPenalties(filters: PenaltyFilters) {
    const params = new URLSearchParams();
    params.set('page', String(filters.page));
    params.set('size', String(filters.size));
    if (filters.status) params.set('status', filters.status);
    if (filters.type) params.set('type', filters.type);
    if (filters.userId) params.set('userId', String(filters.userId));
    if (filters.sourceType && filters.sourceType !== 'ALL') params.set('sourceType', filters.sourceType);
    return axiosClient.get<PagePenaltyApiResponse>(`${BASE}/penalties?${params}`);
  },

  issuePenalty(payload: IssuePenaltyApiRequest) {
    return axiosClient.post<PenaltyApiResponse>(`${BASE}/penalties`, payload);
  },

  revokePenalty(penaltyId: number, payload: RevokePenaltyApiRequest) {
    return axiosClient.patch<PenaltyApiResponse>(`${BASE}/penalties/${penaltyId}/revoke`, payload);
  },

  getAuditLogs(filters: AuditLogFilters) {
    const params = new URLSearchParams();
    params.set('page', String(filters.page));
    params.set('size', String(filters.size));
    if (filters.actorId) params.set('actorId', String(filters.actorId));
    if (filters.action) params.set('action', filters.action);
    if (filters.entityType) params.set('entityType', filters.entityType);
    if (filters.from) params.set('from', filters.from);
    if (filters.to) params.set('to', filters.to);
    return axiosClient.get<PageAuditLogApiResponse>(`${BASE}/audit-logs?${params}`);
  },

  getTaskSummary() {
    return axiosClient.get<TaskQueueSummaryApiResponse>(`${BASE}/tasks/summary`);
  },

  getTasks(filters: TaskFilters) {
    const params = new URLSearchParams();
    params.set('page', String(filters.page));
    params.set('size', String(filters.size));
    if (filters.type && filters.type !== 'ALL') params.set('type', filters.type);
    if (filters.priority && filters.priority !== 'ALL') params.set('priority', filters.priority);
    if (filters.slaBreached !== undefined) params.set('slaBreached', String(filters.slaBreached));
    return axiosClient.get<PageTaskItemApiResponse>(`${BASE}/tasks?${params}`);
  },

  getAiKnowledgeStats() {
    return axiosClient.get<AiKnowledgeStatsApiResponse>(`${BASE}/ai/knowledge/stats`);
  },

  reindexAiKnowledge() {
    return axiosClient.post<AiKnowledgeReindexApiResponse>(`${BASE}/ai/reindex`);
  },

  getAnalyticsSummary(from?: string, to?: string) {
    const params = new URLSearchParams();
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    return axiosClient.get<AnalyticsSummaryApiResponse>(`${BASE}/analytics/summary?${params}`);
  },

  exportAnalyticsCsv(type: 'users' | 'classes' | 'revenue' | 'cashflow' | 'transaction-breakdown', from?: string, to?: string) {
    const params = new URLSearchParams({ type, format: 'csv' });
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    return axiosClient.get<Blob>(`${BASE}/analytics/export?${params}`, {
      responseType: 'blob',
    });
  },
};
