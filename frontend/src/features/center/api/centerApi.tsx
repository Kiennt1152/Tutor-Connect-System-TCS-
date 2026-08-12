import axiosClient from '../../../shared/api/axiosClient';
import type { ClassRequest } from '../../marketplace/types/marketplaceTypes';
import type {
  CenterContractInfo,
  CenterMember,
  ClassResponse,
  ContractTemplate,
  MembershipStatus,
  RecruitmentApplication,
  RecruitmentPost,
  Reschedule,
  SaveClassRequest,
  SaveRecruitmentPostRequest,
  ScheduleClass,
  Substitution,
  TutorOption,
} from '../types/centerTypes';
import type {
  ReportApiResponse,
  ResolveClassIssueRequest,
} from '../../platform/types/platformTypes';

export const CENTER_API_BASE = '/center';

export const centerApi = {
  // Tải file chứng chỉ đã xác minh của gia sư (endpoint kèm JWT) -> Blob để hiển thị.
  // Ảnh/giấy tờ private không xem được bằng <img src> vì thẻ img không gửi được token.
  async getCertificateBlob(fileId: number): Promise<Blob> {
    const res = await axiosClient.get(`/files/certificate/${fileId}`, {
      responseType: 'blob',
    });
    return res.data as Blob;
  },
  // ----- Tin tuyển gia sư — phía trung tâm (FT-33) -----
  getMyPosts() {
    return axiosClient.get<RecruitmentPost[]>(`${CENTER_API_BASE}/recruitment/my-posts`);
  },
  createPost(payload: SaveRecruitmentPostRequest) {
    return axiosClient.post<RecruitmentPost>(`${CENTER_API_BASE}/recruitment`, payload);
  },
  updatePost(recruitmentId: number, payload: SaveRecruitmentPostRequest) {
    return axiosClient.put<RecruitmentPost>(
      `${CENTER_API_BASE}/recruitment/${recruitmentId}`,
      payload,
    );
  },
  publishPost(recruitmentId: number) {
    return axiosClient.post<RecruitmentPost>(
      `${CENTER_API_BASE}/recruitment/${recruitmentId}/publish`,
    );
  },
  closePost(recruitmentId: number) {
    return axiosClient.post<RecruitmentPost>(
      `${CENTER_API_BASE}/recruitment/${recruitmentId}/close`,
    );
  },
  getApplications(recruitmentId: number) {
    return axiosClient.get<RecruitmentApplication[]>(
      `${CENTER_API_BASE}/recruitment/${recruitmentId}/applications`,
    );
  },
  decideApplication(
    recruitmentAppId: number,
    approve: boolean,
    contractTemplateId?: number,
    contractContent?: string,
  ) {
    return axiosClient.post<RecruitmentApplication>(
      `${CENTER_API_BASE}/recruitment/applications/${recruitmentAppId}/decision`,
      { approve, contractTemplateId, contractContent },
    );
  },

  // ----- Tin tuyển gia sư — phía gia sư -----
  getOpenPosts() {
    return axiosClient.get<RecruitmentPost[]>(`${CENTER_API_BASE}/recruitment`);
  },
  apply(recruitmentId: number, coverLetter: string) {
    return axiosClient.post<{ message: string }>(
      `${CENTER_API_BASE}/recruitment/${recruitmentId}/apply`,
      { coverLetter },
    );
  },
  getMyApplications() {
    return axiosClient.get<RecruitmentApplication[]>(
      `${CENTER_API_BASE}/recruitment/applications/mine`,
    );
  },
  // Gia sư rút đơn ứng tuyển (chỉ khi đơn còn ở trạng thái mới nộp).
  withdrawApplication(recruitmentAppId: number) {
    return axiosClient.post<{ message: string }>(
      `${CENTER_API_BASE}/recruitment/applications/${recruitmentAppId}/withdraw`,
    );
  },

  // ----- Quản lý danh sách gia sư của trung tâm -----
  getMembers() {
    return axiosClient.get<CenterMember[]>(`${CENTER_API_BASE}/members`);
  },
  updateMemberStatus(membershipId: number, status: MembershipStatus) {
    return axiosClient.patch<CenterMember>(
      `${CENTER_API_BASE}/members/${membershipId}/status`,
      { status },
    );
  },

  // ----- Lớp học của trung tâm (UC-14-B) -----
  getMyClasses() {
    return axiosClient.get<ClassResponse[]>(`${CENTER_API_BASE}/classes`);
  },
  getClass(classId: number) {
    return axiosClient.get<ClassResponse>(`${CENTER_API_BASE}/classes/${classId}`);
  },
  createClass(payload: SaveClassRequest) {
    return axiosClient.post<ClassResponse>(`${CENTER_API_BASE}/classes`, payload);
  },
  updateClass(classId: number, payload: SaveClassRequest) {
    return axiosClient.put<ClassResponse>(`${CENTER_API_BASE}/classes/${classId}`, payload);
  },
  publishClass(classId: number) {
    return axiosClient.post<ClassResponse>(`${CENTER_API_BASE}/classes/${classId}/publish`);
  },
  closeEnrollment(classId: number) {
    return axiosClient.post<ClassResponse>(
      `${CENTER_API_BASE}/classes/${classId}/close-enrollment`,
    );
  },
  activateClass(classId: number) {
    return axiosClient.post<ClassResponse>(`${CENTER_API_BASE}/classes/${classId}/activate`);
  },
  getTutors(classId?: number) {
    const q = classId != null ? `?classId=${classId}` : '';
    return axiosClient.get<TutorOption[]>(`${CENTER_API_BASE}/tutors${q}`);
  },
  assignTutor(classId: number, tutorId: number) {
    return axiosClient.post<ClassResponse>(`${CENTER_API_BASE}/classes/${classId}/assign-tutor`, {
      tutorId,
    });
  },
  unassignTutor(classId: number) {
    return axiosClient.delete<ClassResponse>(`${CENTER_API_BASE}/classes/${classId}/assign-tutor`);
  },
  assignAssistant(classId: number, tutorId: number) {
    return axiosClient.post<ClassResponse>(
      `${CENTER_API_BASE}/classes/${classId}/assign-assistant`,
      { tutorId },
    );
  },
  unassignAssistant(classId: number) {
    return axiosClient.delete<ClassResponse>(
      `${CENTER_API_BASE}/classes/${classId}/assign-assistant`,
    );
  },
  getSchedule(date?: string) {
    const q = date ? `?date=${date}` : '';
    return axiosClient.get<ScheduleClass[]>(`${CENTER_API_BASE}/schedule${q}`);
  },
  getReschedules() {
    return axiosClient.get<Reschedule[]>(`${CENTER_API_BASE}/reschedules`);
  },
  decideReschedule(classId: number, originalDate: string, approve: boolean) {
    return axiosClient.post<Reschedule>(`${CENTER_API_BASE}/reschedules/decision`, {
      classId,
      originalDate,
      approve,
    });
  },
  getSubstitutions() {
    return axiosClient.get<Substitution[]>(`${CENTER_API_BASE}/substitutions`);
  },

  // ----- Mẫu hợp đồng -----
  getContractTemplates() {
    return axiosClient.get<ContractTemplate[]>(`${CENTER_API_BASE}/contract-templates`);
  },
  createContractTemplate(payload: { name: string; content: string; contractType?: string }) {
    return axiosClient.post<ContractTemplate>(`${CENTER_API_BASE}/contract-templates`, payload);
  },
  updateContractTemplate(
    templateId: number,
    payload: { name: string; content: string; contractType?: string },
  ) {
    return axiosClient.put<ContractTemplate>(
      `${CENTER_API_BASE}/contract-templates/${templateId}`,
      payload,
    );
  },

  // ----- Thông tin trung tâm trên hợp đồng (BÊN A) -----
  getContractInfo() {
    return axiosClient.get<CenterContractInfo>(`${CENTER_API_BASE}/contract-info`);
  },
  saveContractInfo(payload: {
    website?: string;
    representativeName?: string;
    representativePosition?: string;
  }) {
    return axiosClient.put<CenterContractInfo>(`${CENTER_API_BASE}/contract-info`, payload);
  },

  // ----- Yêu cầu mở lớp do phụ huynh gửi tới trung tâm -----
  getClassRequests() {
    return axiosClient.get<ClassRequest[]>(`${CENTER_API_BASE}/class-requests`);
  },
  startSearchClassRequest(requestId: string) {
    return axiosClient.post<{ message: string }>(
      `${CENTER_API_BASE}/class-requests/${requestId}/start-search`,
    );
  },
  // Trung tâm đề cử / gỡ gia sư (thuộc đội) vào shortlist của yêu cầu.
  proposeTutor(requestId: string, tutorId: number) {
    return axiosClient.post<{ message: string }>(
      `${CENTER_API_BASE}/class-requests/${requestId}/candidates/${tutorId}`,
    );
  },
  removeCandidate(requestId: string, tutorId: number) {
    return axiosClient.delete<{ message: string }>(
      `${CENTER_API_BASE}/class-requests/${requestId}/candidates/${tutorId}`,
    );
  },
  // Đăng tin tuyển gia sư NGOÀI đội cho một yêu cầu (tin ACTIVE ngay).
  postRecruitmentForRequest(requestId: string) {
    return axiosClient.post<ClassRequest>(
      `${CENTER_API_BASE}/class-requests/${requestId}/recruitment`,
    );
  },
  // Đơn ứng tuyển vào tin đã đăng cho yêu cầu (để duyệt vào shortlist).
  getRequestApplications(requestId: string) {
    return axiosClient.get<RecruitmentApplication[]>(
      `${CENTER_API_BASE}/class-requests/${requestId}/applications`,
    );
  },
  acceptClassRequest(requestId: string, payload: SaveClassRequest) {
    return axiosClient.post<ClassResponse>(
      `${CENTER_API_BASE}/class-requests/${requestId}/accept`,
      payload,
    );
  },
  rejectClassRequest(requestId: string, reason: string) {
    return axiosClient.post<{ message: string }>(
      `${CENTER_API_BASE}/class-requests/${requestId}/reject`,
      { reason },
    );
  },
  // Trung tâm không tìm được gia sư -> đóng yêu cầu + thông báo cho phụ huynh.
  giveUpClassRequest(requestId: string, reason: string) {
    return axiosClient.post<{ message: string }>(
      `${CENTER_API_BASE}/class-requests/${requestId}/give-up`,
      { reason },
    );
  },
  decideSubstitution(classId: number, date: string, approve: boolean) {
    return axiosClient.post<Substitution>(`${CENTER_API_BASE}/substitutions/decision`, {
      classId,
      date,
      approve,
    });
  },
  getReports() {
    return axiosClient.get<ReportApiResponse[]>(`${CENTER_API_BASE}/reports`);
  },
  resolveReport(reportId: string, payload: ResolveClassIssueRequest) {
    return axiosClient.patch<ReportApiResponse>(
      `${CENTER_API_BASE}/reports/${reportId}/resolve`,
      payload,
    );
  },
};
