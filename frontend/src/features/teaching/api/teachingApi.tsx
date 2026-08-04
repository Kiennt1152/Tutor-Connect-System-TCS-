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

  markAttendance: (lessonId: number, present: boolean) =>
    axiosClient
      .post<{ message: string }>(
        `/marketplace/lessons/${lessonId}/attend?present=${present}`,
      )
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
