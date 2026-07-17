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
  subjectName: string | null;
  attendanceStatus: AttendanceStatus;
  tutorCheckInAt: string | null;
  tutorCheckOutAt: string | null;
  /** Buổi diễn ra đúng hôm nay — chỉ khi đó mới điểm danh được (server cũng chặn). */
  canCheckInToday: boolean;
}

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
