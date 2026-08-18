
import type {
  ContractRefundPayoutInfo,
  EscrowPaymentInfo,
} from '../../contract/types/contractTypes';

export type AssignmentStatus = 'PENDING' | 'ACTIVE' | 'DECLINED' | 'TERMINATED';

export type ClassStatus =
  | 'OPEN'
  | 'MATCHED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'DISPUTED'
  | 'PENDING'
  | 'CLOSED';

export type AttendanceStatus = 'PENDING' | 'COMPLETED' | 'ABSENT' | 'DISPUTED';

/** Trạng thái "Hoàn thành lớp" theo góc nhìn người dùng hiện tại. */
export type CompletionState =
  | 'NONE'
  | 'COMPLETED'
  | 'TUTOR_CAN_CONFIRM'
  | 'TUTOR_BLOCKED'
  | 'TUTOR_WAITING'
  | 'CLIENT_WAITING_TUTOR'
  | 'CLIENT_MUST_REVIEW';

export interface AssignmentResponse {
  assignmentId: number;
  classId: number;
  classTitle: string;
  classStatus: ClassStatus | string;
  clientName: string;
  tutorName: string | null;
  status: AssignmentStatus;
  assignedDate: string;
  subjectNames: string[] | null;
  gradeName: string | null;
  address: string | null;
  lessonMode: string;
  startDate: string | null;
  endDate: string | null;
  lessonCount: number;
  tutorSignedAt: string | null;
  clientSignedAt: string | null;
  paymentMethod: PaymentMethod | null;
  /** UC "Xác nhận lớp đã hoàn thành" (lớp PRIVATE). */
  classCompleted: boolean;
  completionState: CompletionState;
  completionBlockedReason: string | null;
}

export type PaymentMethod = 'FULL' | 'DEPOSIT_1M';

export interface ContractView {
  contractId: number | null;
  assignmentId: number;
  classId: number;
  classTitle: string;
  detailsJson: string | null;
  gradeName: string | null;
  address: string | null;
  lessonMode: string;
  startDate: string | null;
  endDate: string | null;
  numberOfSessions: number;
  subjectNames: string[] | null;
  tuitionFee: number | null;
  totalTuitionAmount: number | string | null;
  escrowAmount: number | string | null;
  clientName: string | null;
  clientPhone: string | null;
  clientAddress: string | null;
  clientDob: string | null;
  clientCccd: string | null;
  tutorName: string | null;
  tutorPhone: string | null;
  tutorAddress: string | null;
  tutorDob: string | null;
  tutorCccd: string | null;
  tutorSigned: boolean;
  clientSigned: boolean;
  tutorSignedAt: string | null;
  clientSignedAt: string | null;
  paymentMethod: PaymentMethod | null;
  myRole: 'CLIENT' | 'TUTOR';
  escrowPayment: EscrowPaymentInfo | null;
  refundPayoutInfo: ContractRefundPayoutInfo | null;
  termsB: string | null;
}

export interface LessonResponse {
  lessonId: number;
  classId: number;
  classTitle: string;
  sequenceNo: number;
  lessonDate: string;
  startTime: string;
  endTime: string;
  subjectId: number | null;
  subjectName: string | null;
  attendanceStatus: AttendanceStatus;
  tutorCheckInAt: string | null;
  tutorCheckOutAt: string | null;
  canCheckInToday: boolean;
  /** True khi lớp đã điểm danh buổi cuối — khóa đổi lịch. */
  rescheduleLocked: boolean;
}

export type RescheduleRequestType = 'RESCHEDULE';

export type RescheduleRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface RescheduleRequestResponse {
  requestId: number;
  classId: number;
  classTitle: string;
  requestType: RescheduleRequestType;
  status: RescheduleRequestStatus;

  lessonId: number | null;
  oldDate: string | null;
  oldStartTime: string | null;
  oldEndTime: string | null;

  newDate: string;
  newStartTime: string;
  newEndTime: string;
  subjectName: string | null;

  reason: string | null;
  requestedByName: string;
  createdAt: string;

  decidedByName: string | null;
  decidedAt: string | null;
  decisionNote: string | null;

  canDecide: boolean;
  canCancel: boolean;
}

export interface RescheduleLessonPayload {
  newDate: string;
  newStartTime: string;
  newEndTime: string;
  reason?: string;
}

export const REQUEST_STATUS_LABELS: Record<RescheduleRequestStatus, string> = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Bị từ chối',
  CANCELLED: 'Đã thu hồi',
};

export const ASSIGNMENT_STATUS_LABELS: Record<AssignmentStatus, string> = {
  PENDING: 'Chờ bạn nhận lớp',
  ACTIVE: 'Đang dạy',
  DECLINED: 'Bạn đã từ chối',
  TERMINATED: 'Đã kết thúc',
};

export const CLASS_STATUS_LABELS: Record<string, string> = {
  OPEN: 'Đang mở',
  MATCHED: 'Đã ghép gia sư',
  IN_PROGRESS: 'Đang diễn ra',
  COMPLETED: 'Đã hoàn thành',
  CANCELLED: 'Đã hủy',
  DISPUTED: 'Đang tranh chấp',
  PENDING: 'Đang chờ',
  CLOSED: 'Đã đóng',
};

export const ATTENDANCE_STATUS_LABELS: Record<AttendanceStatus, string> = {
  PENDING: 'Chưa điểm danh',
  COMPLETED: 'Đã điểm danh',
  ABSENT: 'Vắng',
  DISPUTED: 'Đang tranh chấp',
};
