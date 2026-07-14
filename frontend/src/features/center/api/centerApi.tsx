import axiosClient from '../../../shared/api/axiosClient';
import type {
  ClassResponse,
  Reschedule,
  SaveClassRequest,
  ScheduleClass,
  TutorOption,
} from '../types/centerTypes';

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
};
