import axiosClient from '../../../shared/api/axiosClient';
import type { ClassResponse, SaveClassRequest, TutorOption } from '../types/centerTypes';

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
  getTutors() {
    return axiosClient.get<TutorOption[]>(`${CENTER_API_BASE}/tutors`);
  },
  assignTutor(classId: number, tutorId: number) {
    return axiosClient.post<ClassResponse>(`${CENTER_API_BASE}/classes/${classId}/assign-tutor`, {
      tutorId,
    });
  },
  unassignTutor(classId: number) {
    return axiosClient.delete<ClassResponse>(`${CENTER_API_BASE}/classes/${classId}/assign-tutor`);
  },
};
