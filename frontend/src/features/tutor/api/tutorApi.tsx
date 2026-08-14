import axiosClient from '../../../shared/api/axiosClient';
import type {
  AttendanceStatus,
  Reschedule,
  ScheduleClass,
  Substitution,
} from '../../center/types/centerTypes';

const BASE = '/tutor';

export const tutorApi = {
  getSchedule(date?: string) {
    const q = date ? `?date=${date}` : '';
    return axiosClient.get<ScheduleClass[]>(`${BASE}/schedule${q}`);
  },
  getSession(classId: number, date?: string) {
    const q = date ? `?date=${date}` : '';
    return axiosClient.get<ScheduleClass>(`${BASE}/classes/${classId}/session${q}`);
  },
  saveAttendance(
    classId: number,
    records: { classStudentId: number; status: AttendanceStatus }[],
    date?: string,
  ) {
    const q = date ? `?date=${date}` : '';
    return axiosClient.post<ScheduleClass>(`${BASE}/classes/${classId}/attendance/batch${q}`, {
      records,
    });
  },
  requestReschedule(
    classId: number,
    payload: {
      originalDate: string;
      newDate: string;
      newStartTime: string;
      newEndTime: string;
      reason: string;
    },
  ) {
    return axiosClient.post<Reschedule>(`${BASE}/classes/${classId}/reschedule`, payload);
  },
  getReschedules() {
    return axiosClient.get<Reschedule[]>(`${BASE}/reschedules`);
  },
  requestSubstitute(classId: number, payload: { date: string; reason: string }) {
    return axiosClient.post<Substitution>(`${BASE}/classes/${classId}/substitute`, payload);
  },
  getSubstitutions() {
    return axiosClient.get<Substitution[]>(`${BASE}/substitutions`);
  },
  // Bước 13: gia sư xác nhận khóa học đã hoàn thành -> tất toán + đóng lớp.
  confirmClassCompletion(classId: number) {
    return axiosClient.post<{ message: string }>(`${BASE}/classes/${classId}/complete`);
  },
};
