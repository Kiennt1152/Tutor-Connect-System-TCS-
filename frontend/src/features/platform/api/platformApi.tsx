/**
 * ============================================================================
 * [BF-10 / BF-09] API CLIENT QUẢN TRỊ NỀN TẢNG & HỖ TRỢ KHÁCH HÀNG (PLATFORM API)
 * ============================================================================
 * Tác giả       : mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo      : 2026-06-23
 * 
 * Mô tả:
 *   - Tập hợp toàn bộ các hàm gọi RESTful API từ Frontend lên Backend phục vụ:
 *     1. Bảng điều khiển quản trị Admin Dashboard và phân tích số liệu tài chính [UC-56, UC-41, UC-43].
 *     2. Quản lý người dùng, duyệt hồ sơ eKYC CCCD, xử phạt vi phạm và kiểm toán [UC-07, UC-11, UC-60, UC-61].
 *     3. Quản trị hệ thống phiếu hỗ trợ khách hàng (Support Ticket), SLA và FAQ [UC-63, UC-66, UC-67].
 *     4. Quản lý ký quỹ Escrow, biểu phí sàn, mẫu hợp đồng và phát hiện lách sàn [UC-58, UC-46, UC-45, UC-59].
 * ============================================================================
 */
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
  CreateUserApiRequest,
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
  CenterFinancialAnalyticsApiResponse,
  TutorFinancialAnalyticsApiResponse,
  ClientFinancialAnalyticsApiResponse,
  PageFinancialLedgerApiResponse,
  CenterFeeConfigApiResponse,
  UpdateCenterFeeApiRequest,
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

  createUser(payload: CreateUserApiRequest) {
    return axiosClient.post<UserListItemApiResponse>(`${BASE}/users`, payload);
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

  completeWithdrawal(withdrawalId: string, payload?: WithdrawalDecisionApiRequest) {
    return axiosClient.post(`/finance/withdrawals/${withdrawalId}/complete`, payload || {});
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

  /** Admin xử lý báo cáo nhắm vào đánh giá. */
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

  /** Admin lấy danh sách đánh giá (lọc theo trạng thái nếu có). */
  getReviews(status?: ReviewModerationStatus) {
    const query = status ? `?status=${status}` : '';
    return axiosClient.get<AdminReviewApiResponse[]>(`${BASE}/reviews${query}`);
  },

  /** Admin đổi trạng thái hiển thị của đánh giá. */
  moderateReview(reviewId: number, status: ReviewModerationStatus) {
    return axiosClient.patch<AdminReviewApiResponse>(`${BASE}/reviews/${reviewId}`, { status });
  },

  /** Admin xoá vĩnh viễn đánh giá. */
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
    return axiosClient.post<AiKnowledgeReindexApiResponse>(`${BASE}/ai/reindex`, {}, {
      timeout: 180000,
    });
  },

  getAnalyticsSummary(from?: string, to?: string) {
    const params = new URLSearchParams();
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    return axiosClient.get<AnalyticsSummaryApiResponse>(`${BASE}/analytics/summary?${params}`);
  },

  getCenterAnalytics(from?: string, to?: string) {
    const params = new URLSearchParams();
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    return axiosClient.get<CenterFinancialAnalyticsApiResponse[]>(`${BASE}/analytics/entities/centers?${params}`);
  },

  getTutorAnalytics(from?: string, to?: string) {
    const params = new URLSearchParams();
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    return axiosClient.get<TutorFinancialAnalyticsApiResponse[]>(`${BASE}/analytics/entities/tutors?${params}`);
  },

  getClientAnalytics(from?: string, to?: string) {
    const params = new URLSearchParams();
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    return axiosClient.get<ClientFinancialAnalyticsApiResponse[]>(`${BASE}/analytics/entities/clients?${params}`);
  },

  getFinancialLedger(filters?: {
    role?: string;
    direction?: string;
    search?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }) {
    const params = new URLSearchParams();
    if (filters?.role) params.set('role', filters.role);
    if (filters?.direction) params.set('direction', filters.direction);
    if (filters?.search) params.set('search', filters.search);
    if (filters?.from) params.set('from', filters.from);
    if (filters?.to) params.set('to', filters.to);
    if (typeof filters?.page === 'number') params.set('page', String(filters.page));
    if (typeof filters?.size === 'number') params.set('size', String(filters.size));
    return axiosClient.get<PageFinancialLedgerApiResponse>(`${BASE}/analytics/ledger?${params}`);
  },

  exportAnalyticsCsv(type: string, from?: string, to?: string) {
    const params = new URLSearchParams({ type, format: 'csv' });
    if (from) params.set('from', from);
    if (to) params.set('to', to);
    return axiosClient.get<Blob>(`${BASE}/analytics/export?${params}`, {
      responseType: 'blob',
    });
  },

  getClasses(status?: string) {
    const params = new URLSearchParams();
    if (status) params.set('status', status);
    return axiosClient.get<any[]>(`/marketplace/classes${params.toString() ? `?${params}` : ''}`);
  },

  getClassDetail(classId: number | string) {
    return axiosClient.get<any>(`/marketplace/classes/${classId}`);
  },

  getPlatformSchedule(date?: string) {
    const q = date ? `?date=${date}` : '';
    return axiosClient.get<import('../../center/types/centerTypes').ScheduleClass[]>(`${BASE}/classes/schedule${q}`);
  },

  getContractTemplates() {
    return axiosClient.get<any[]>(`${BASE}/contract-templates`);
  },

  createContractTemplate(payload: { name: string; content: string; contractType?: string }) {
    return axiosClient.post<any>(`${BASE}/contract-templates`, payload);
  },

  updateContractTemplate(templateId: number, payload: { name: string; content: string; contractType?: string }) {
    return axiosClient.put<any>(`${BASE}/contract-templates/${templateId}`, payload);
  },

  deleteContractTemplate(templateId: number) {
    return axiosClient.delete(`${BASE}/contract-templates/${templateId}`);
  },

  getCenterFeeConfigs() {
    return axiosClient.get<CenterFeeConfigApiResponse[]>(`${BASE}/fees/centers`);
  },

  updateCenterFeeConfig(centerId: number, payload: UpdateCenterFeeApiRequest) {
    return axiosClient.put<CenterFeeConfigApiResponse>(`${BASE}/fees/centers/${centerId}`, payload);
  },

  resetCenterFeeConfig(centerId: number) {
    return axiosClient.delete<CenterFeeConfigApiResponse>(`${BASE}/fees/centers/${centerId}`);
  },
};
