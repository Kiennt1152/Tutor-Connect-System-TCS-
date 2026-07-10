// Kiểu dữ liệu cho tính năng Client tạo/sửa yêu cầu tìm gia sư (lớp PRIVATE).

export type LessonMode = 'ONLINE' | 'OFFLINE' | 'HYBRID';
export type RecurringType = 'ONCE' | 'WEEKLY';
export type ClassStatus =
  | 'DRAFT'
  | 'OPEN'
  | 'MATCHED'
  | 'ENROLLMENT_CLOSED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'DISPUTED';

/** Mục catalog dùng chung (môn học, khối/lớp, tỉnh thành). */
export interface CatalogOption {
  id: number;
  name: string;
  description?: string | null;
}

/** Địa điểm cấp quận/huyện trả về từ /catalog/locations. */
export interface LocationOption {
  locationId: number;
  provinceId: number;
  provinceName: string;
  districtName: string | null;
  wardName: string | null;
}

/** Lớp trả về từ backend (ClassResponse). */
export interface ClassResponse {
  classId: number;
  title: string;
  description: string;
  creatorId: number;
  creatorName: string;
  subjectId: number | null;
  subjectName: string | null;
  gradeId: number | null;
  gradeName: string | null;
  learningGoal: string | null;
  tutorRequirement: string | null;
  locationId: number | null;
  locationName: string | null;
  address: string | null;
  lessonMode: LessonMode;
  numberOfSessions: number;
  startDate: string | null;
  endDate: string | null;
  tuitionFee: number | null;
  budget: number | null;
  recurringType: RecurringType;
  status: ClassStatus;
  createdAt: string;
}

/** Payload gửi lên khi tạo/sửa lớp (CreateClassRequest). */
export interface ClassRequestPayload {
  title?: string;
  description?: string;
  subjectId: number | null;
  gradeId: number | null;
  learningGoal?: string | null;
  tutorRequirement?: string | null;
  locationId?: number | null;
  address?: string | null;
  lessonMode: LessonMode;
  numberOfSessions: number;
  startDate: string;
  endDate: string;
  tuitionFee: number;
  budget: number;
  recurringType: RecurringType;
}

/** Giá trị của form (trạng thái nhập liệu). */
export interface ClassFormValues {
  subjectIds: string[];
  gradeId: string;
  learningGoal: string;
  learningGoalOther: string;
  tutorRequirement: string;
  tutorRequirementDetail: string;
  lessonMode: LessonMode;
  provinceId: string;
  provinceName: string;
  districtId: string;
  districtName: string;
  wardId: string;
  wardName: string;
  address: string;
  feePerSession: string;
  billingCycle: BillingCycle;
  sessionsPerWeek: string;
  daysOfWeek: string[];
  /** Khung giờ học theo từng thứ đã chọn (key = giá trị thứ, vd 'T4'). */
  dayTimes: Record<string, DayTime>;
  startDate: string;
  note: string;
}

/** Khung giờ học của một buổi (kèm buổi Sáng/Chiều/Tối). */
export interface DayTime {
  session: string;
  start: string;
  end: string;
}

// Buổi trong ngày + khung giờ gợi ý (chọn buổi sẽ tự điền, vẫn sửa được).
export const SESSION_OPTIONS: readonly { value: string; start: string; end: string }[] = [
  { value: 'Sáng', start: '08:00', end: '10:00' },
  { value: 'Chiều', start: '14:00', end: '16:00' },
  { value: 'Tối', start: '18:00', end: '20:00' },
];

export type BillingCycle = 'MONTH' | 'TERM';

// Gợi ý đáp án cho "Mục tiêu học tập".
export const LEARNING_GOAL_OPTIONS: readonly string[] = [
  'Lấy lại gốc',
  'Ôn thi học kỳ',
  'Luyện thi chuyển cấp (vào 10)',
  'Luyện thi Đại học',
  'Luyện thi chứng chỉ (IELTS, TOEIC...)',
  'Giao tiếp cơ bản',
];

export const LEARNING_GOAL_OTHER = 'Khác';

// Gợi ý đáp án cho "Yêu cầu đối với gia sư".
export const TUTOR_REQUIREMENT_OPTIONS: readonly string[] = [
  'Không yêu cầu cụ thể',
  'Sinh viên',
  'Giáo viên',
  'Gia sư có chứng chỉ / bằng cấp',
];

export const LESSON_MODE_OPTIONS: readonly { value: LessonMode; label: string }[] = [
  { value: 'OFFLINE', label: 'Offline (học tại nhà)' },
  { value: 'ONLINE', label: 'Online' },
];

export const RECURRING_OPTIONS: readonly { value: RecurringType; label: string }[] = [
  { value: 'WEEKLY', label: 'Học đều hàng tuần' },
  { value: 'ONCE', label: 'Học một đợt' },
];

// Học theo tháng / theo kỳ — dùng để ước tính tổng học phí.
export const BILLING_CYCLE_OPTIONS: readonly {
  value: BillingCycle;
  label: string;
  weeks: number;
  suffix: string;
}[] = [
  { value: 'MONTH', label: 'Theo tháng', weeks: 4, suffix: 'đ / tháng' },
  { value: 'TERM', label: 'Theo kỳ (3 tháng)', weeks: 12, suffix: 'đ / kỳ' },
];

// Các thứ trong tuần.
export const DAY_OF_WEEK_OPTIONS: readonly { value: string; label: string }[] = [
  { value: 'T2', label: 'Thứ 2' },
  { value: 'T3', label: 'Thứ 3' },
  { value: 'T4', label: 'Thứ 4' },
  { value: 'T5', label: 'Thứ 5' },
  { value: 'T6', label: 'Thứ 6' },
  { value: 'T7', label: 'Thứ 7' },
  { value: 'CN', label: 'Chủ nhật' },
];

export const CLASS_STATUS_LABELS: Record<ClassStatus, string> = {
  DRAFT: 'Nháp',
  OPEN: 'Đang mở',
  MATCHED: 'Đã ghép',
  ENROLLMENT_CLOSED: 'Đóng ghi danh',
  IN_PROGRESS: 'Đang học',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
  DISPUTED: 'Tranh chấp',
};
