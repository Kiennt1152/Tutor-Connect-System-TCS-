import axiosClient from '../../../shared/api/axiosClient';
import type { AttendanceStatus, ScheduleClass } from '../../center/types/centerTypes';

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
};
