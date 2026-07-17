import axiosClient from '../../../shared/api/axiosClient';
import type { AssignmentResponse, LessonResponse } from '../types/teachingTypes';

export const TEACHING_API_BASE = '/marketplace';

export const teachingApi = {
  http: axiosClient,
  basePath: TEACHING_API_BASE,

  /** Lời mời nhận lớp + lớp đang dạy. */
  listMyAssignments: () =>
    axiosClient.get<AssignmentResponse[]>('/marketplace/assignments/mine').then((r) => r.data),

  acceptAssignment: (assignmentId: number) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/assignments/${assignmentId}/accept`)
      .then((r) => r.data),

  declineAssignment: (assignmentId: number) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/assignments/${assignmentId}/decline`)
      .then((r) => r.data),

  /** Lịch dạy — mọi buổi của các lớp đã nhận, xếp theo ngày. */
  listMyLessons: () =>
    axiosClient.get<LessonResponse[]>('/marketplace/lessons/mine').then((r) => r.data),

  checkInLesson: (lessonId: number) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/lessons/${lessonId}/checkin`)
      .then((r) => r.data),

  checkOutLesson: (lessonId: number) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/lessons/${lessonId}/checkout`)
      .then((r) => r.data),
};
