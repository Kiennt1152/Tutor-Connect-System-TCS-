import axiosClient from '../../../shared/api/axiosClient';
import type { ClassRequest } from '../../marketplace/types/marketplaceTypes';
import type {
  CenterMember,
  ClassResponse,
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
  decideApplication(recruitmentAppId: number, approve: boolean) {
    return axiosClient.post<RecruitmentApplication>(
      `${CENTER_API_BASE}/recruitment/applications/${recruitmentAppId}/decision`,
      { approve },
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

  // ----- Yêu cầu mở lớp do phụ huynh gửi tới trung tâm -----
  getClassRequests() {
    return axiosClient.get<ClassRequest[]>(`${CENTER_API_BASE}/class-requests`);
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
