// Phía gia sư: nhận lớp → lịch dạy → điểm danh từng buổi.

export type AssignmentStatus = 'PENDING' | 'ACTIVE' | 'DECLINED' | 'TERMINATED';

export type AttendanceStatus = 'PENDING' | 'COMPLETED' | 'ABSENT' | 'DISPUTED';

/** Một lớp gia sư được Client chọn (AssignmentResponse). */
export interface AssignmentResponse {
  assignmentId: number;
  classId: number;
  classTitle: string;
  clientName: string;
  /** Gia sư được chọn — Client cần biết ai đang dạy lớp của mình. */
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
}

/** Một buổi dạy cụ thể (LessonResponse). */
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
  /** Buổi diễn ra đúng hôm nay — chỉ khi đó mới điểm danh được (server cũng chặn). */
  canCheckInToday: boolean;
}

export type RescheduleRequestType = 'RESCHEDULE' | 'EXTRA';

export type RescheduleRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface RescheduleRequestResponse {
  requestId: number;
  classId: number;
  classTitle: string;
  requestType: RescheduleRequestType;
  status: RescheduleRequestStatus;

  /** Buổi bị dời — null với yêu cầu thêm buổi. */
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

  /** Mình là bên phải duyệt và yêu cầu còn chờ. */
  canDecide: boolean;
  /** Mình là người gửi và yêu cầu còn chờ — thu hồi được. */
  canCancel: boolean;
}

/** Một lớp cùng danh sách môn — dựng từ chính các buổi đã có của lớp. */
export interface ClassOption {
  classId: number;
  classTitle: string;
  subjects: { subjectId: number; subjectName: string }[];
}

/** Gom các buổi thành danh sách lớp + môn để form "thêm buổi" có gì mà chọn. */
export function classOptionsFrom(lessons: LessonResponse[]): ClassOption[] {
  const byClass = new Map<number, ClassOption>();
  for (const lesson of lessons) {
    const option = byClass.get(lesson.classId) ?? {
      classId: lesson.classId,
      classTitle: lesson.classTitle,
      subjects: [],
    };
    if (
      lesson.subjectId !== null &&
      lesson.subjectName !== null &&
      !option.subjects.some((s) => s.subjectId === lesson.subjectId)
    ) {
      option.subjects.push({ subjectId: lesson.subjectId, subjectName: lesson.subjectName });
    }
    byClass.set(lesson.classId, option);
  }
  return [...byClass.values()];
}

export interface RescheduleLessonPayload {
  newDate: string;
  newStartTime: string;
  newEndTime: string;
  reason?: string;
}

export interface ExtraLessonPayload {
  classId: number;
  lessonDate: string;
  startTime: string;
  endTime: string;
  subjectId?: number | null;
  reason?: string;
}

export const REQUEST_TYPE_LABELS: Record<RescheduleRequestType, string> = {
  RESCHEDULE: 'Đổi lịch',
  EXTRA: 'Thêm buổi',
};

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

export const ATTENDANCE_STATUS_LABELS: Record<AttendanceStatus, string> = {
  PENDING: 'Chưa điểm danh',
  COMPLETED: 'Đã hoàn thành',
  ABSENT: 'Vắng',
  DISPUTED: 'Đang tranh chấp',
};
