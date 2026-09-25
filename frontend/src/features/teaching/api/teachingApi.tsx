import axiosClient from '../../../shared/api/axiosClient';
import type { SaveRefundPayoutRequest } from '../../contract/types/contractTypes';
import type {
  AssignmentResponse,
  ContractView,
  LessonResponse,
  RescheduleLessonPayload,
  RescheduleRequestResponse,
} from '../types/teachingTypes';

export const TEACHING_API_BASE = '/marketplace';

export const teachingApi = {
  http: axiosClient,
  basePath: TEACHING_API_BASE,

  /** Phân công lớp riêng của người đăng nhập. */
  listMyAssignments: () =>
    axiosClient.get<AssignmentResponse[]>('/marketplace/assignments/mine').then((r) => r.data),

  /** Gia sư nhận lớp. */
  acceptAssignment: (assignmentId: number) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/assignments/${assignmentId}/accept`)
      .then((r) => r.data),

  /** Gia sư từ chối lời mời nhận lớp. */
  declineAssignment: (assignmentId: number) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/assignments/${assignmentId}/decline`)
      .then((r) => r.data),

  /** Dữ liệu trang ký hợp đồng của một phân công. */
  getAssignmentContract: (assignmentId: number) =>
    axiosClient
      .get<ContractView>(`/marketplace/assignments/${assignmentId}/contract`)
      .then((r) => r.data),

  /** Yêu cầu gửi mã OTP ký hợp đồng về email. */
  requestSignOtp: (assignmentId: number) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/assignments/${assignmentId}/sign/request-otp`)
      .then((r) => r.data),

  /** Ký hợp đồng bằng mã OTP. */
  signAssignmentContract: (assignmentId: number, otp: string) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/assignments/${assignmentId}/sign`, {
        otp,
      })
      .then((r) => r.data),

  /** Bên A lưu điều khoản bổ sung của hợp đồng. */
  saveContractTerms: (assignmentId: number, termsB: string) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/assignments/${assignmentId}/contract-terms`, {
        termsB,
      })
      .then((r) => r.data),

  saveAssignmentRefundPayoutInfo: (assignmentId: number, payload: SaveRefundPayoutRequest) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/assignments/${assignmentId}/refund-payout`, payload)
      .then((r) => r.data),

  /** Thời khoá biểu lớp riêng của người đăng nhập. */
  listMyLessons: () =>
    axiosClient.get<LessonResponse[]>('/marketplace/lessons/mine').then((r) => r.data),

  /** Gia sư bắt đầu buổi học. */
  checkInLesson: (lessonId: number) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/lessons/${lessonId}/checkin`)
      .then((r) => r.data),

  /** Gia sư kết thúc buổi học. */
  checkOutLesson: (lessonId: number) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/lessons/${lessonId}/checkout`)
      .then((r) => r.data),

  /** Điểm danh nhanh có mặt/vắng cho buổi học. */
  markAttendance: (lessonId: number, present: boolean) =>
    axiosClient
      .post<{ message: string }>(
        `/marketplace/lessons/${lessonId}/attend?present=${present}`,
      )
      .then((r) => r.data),

  /** Gửi yêu cầu đổi lịch một buổi học. */
  requestReschedule: (lessonId: number, payload: RescheduleLessonPayload) =>
    axiosClient
      .post<RescheduleRequestResponse>(`/marketplace/lessons/${lessonId}/reschedule`, payload)
      .then((r) => r.data),

  /** Danh sách yêu cầu đổi lịch/thêm buổi của các lớp mình tham gia. */
  listRescheduleRequests: () =>
    axiosClient
      .get<RescheduleRequestResponse[]>('/marketplace/lessons/requests')
      .then((r) => r.data),

  /** Duyệt hoặc từ chối yêu cầu đổi lịch (kèm ghi chú). */
  decideRequest: (requestId: number, approve: boolean, note?: string) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/lessons/requests/${requestId}/decision`, {
        approve,
        note,
      })
      .then((r) => r.data),

  /** Thu hồi yêu cầu đổi lịch do mình gửi. */
  cancelRequest: (requestId: number) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/lessons/requests/${requestId}/cancel`)
      .then((r) => r.data),

  /** UC "Hoàn thành lớp": gia sư bấm hoàn thành; lớp đóng sau khi học viên đánh giá gia sư. */
  confirmClassCompletion: (classId: number) =>
    axiosClient
      .post<{ message: string }>(`/marketplace/classes/${classId}/complete`)
      .then((r) => r.data),
};
