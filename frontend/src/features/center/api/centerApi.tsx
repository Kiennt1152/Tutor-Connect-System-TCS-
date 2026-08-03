import axiosClient from '../../../shared/api/axiosClient';
import type {
  ClassResponse,
  Reschedule,
  SaveClassRequest,
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
