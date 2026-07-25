import axiosClient from '../../../shared/api/axiosClient';
import type {
  AssignmentResponse,
  ExtraLessonPayload,
  LessonResponse,
  RescheduleLessonPayload,
  RescheduleRequestResponse,
} from '../types/teachingTypes';

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

  /** Điểm danh một buổi bằng một cú bấm (chỉ trong đúng ngày buổi học). */
  markAttendance: (lessonId: number) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/lessons/${lessonId}/attend`)
      .then((r) => r.data),

  requestReschedule: (lessonId: number, payload: RescheduleLessonPayload) =>
    axiosClient
      .post<RescheduleRequestResponse>(`/marketplace/lessons/${lessonId}/reschedule`, payload)
      .then((r) => r.data),

  requestExtraLesson: (payload: ExtraLessonPayload) =>
    axiosClient
      .post<RescheduleRequestResponse>('/marketplace/lessons/extra', payload)
      .then((r) => r.data),

  listRescheduleRequests: () =>
    axiosClient
      .get<RescheduleRequestResponse[]>('/marketplace/lessons/requests')
      .then((r) => r.data),

  decideRequest: (requestId: number, approve: boolean, note?: string) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/lessons/requests/${requestId}/decision`, {
        approve,
        note,
      })
      .then((r) => r.data),

  cancelRequest: (requestId: number) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/lessons/requests/${requestId}/cancel`)
      .then((r) => r.data),
};
