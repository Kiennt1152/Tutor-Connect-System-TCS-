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
  detailsJson: string | null;
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
  /** Số gia sư đã ứng tuyển vào lớp. */
  applicationCount: number | null;
}

/** Một gia sư ứng tuyển vào lớp, kèm điểm gợi ý của AI (ApplicantResponse). */
export interface ApplicantResponse {
  applicationId: number;
  tutorId: number;
  userId: number;
  fullName: string;
  avatar: string | null;
  bio: string | null;
  experienceYears: number | null;
  hourlyRate: number | null;
  ratingAvg: number | null;
  verificationStatus: 'UNDER_VERIFY' | 'VERIFIED' | 'REJECTED';
  proposedRate: number | null;
  coverLetter: string | null;
  status: 'SUBMITTED' | 'UNDER_REVIEW' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN';
  appliedAt: string;
  /** Điểm AI gợi ý 0–100. */
  matchScore: number;
  /** Nằm trong Top 5 AI gợi ý. */
  recommended: boolean;
}

export type Gender = 'MALE' | 'FEMALE' | 'OTHER';

export interface TutorEducationItem {
  educationId: number;
  institution: string;
  degree: string;
  fieldOfStudy: string | null;
  startYear: number | null;
  endYear: number | null;
}

export interface TutorCertificateItem {
  certificateId: number;
  name: string;
  issuer: string;
  issueDate: string | null;
}

/** Hồ sơ gia sư hiện tại (GET /profile/me) — dùng cho form ứng tuyển. */
export interface TutorProfileCard {
  userId: number;
  role: string;
  fullName: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  avatarUrl: string | null;
  dateOfBirth: string | null;
  gender: Gender | null;
  bio: string | null;
  experienceYears: number | null;
  hourlyRate: number | null;
  verificationStatus: 'UNDER_VERIFY' | 'VERIFIED' | 'REJECTED' | null;
  educations: TutorEducationItem[] | null;
  certificates: TutorCertificateItem[] | null;
}

/** Payload gửi lên khi tạo/sửa lớp (CreateClassRequest). */
export interface ClassRequestPayload {
  title?: string;
  description?: string;
  detailsJson?: string;
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
  /** Tên môn học tự nhập khi chọn "Khác". */
  subjectOther: string;
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
  /** Học phí/giờ theo từng môn (key = subjectId, kể cả "other"). */
  subjectFees: Record<string, string>;
  billingCycle: BillingCycle;
  /** Số tháng cụ thể khi chọn "Theo tháng". */
  months: string;
  /** WEEKLY = lặp lại hàng tuần (theo Thứ); CUSTOM = chọn ngày cụ thể. */
  scheduleMode: ScheduleMode;
  /** Độ dài chu kỳ lặp, tính bằng tuần ("1" = hàng tuần, "3" = chu kỳ 3 tuần). Chỉ dùng khi WEEKLY. */
  repeatEveryWeeks: string;
  /** Những tuần HỌC trong mỗi chu kỳ, đánh số 1..N (N = repeatEveryWeeks); tuần không có
   *  trong danh sách là tuần nghỉ. Chu kỳ 4 tuần + [1, 3] = học tuần 1 và 3, nghỉ tuần 2 và 4.
   *  Mặc định [1]. Cho phép tuần học nằm rời rạc, không bắt buộc liền nhau. */
  studyWeeks: number[];
  /** Lịch học theo từng môn: mỗi phần tử = 1 buổi của 1 môn (không trùng giờ nhau). */
  slots: ScheduleSlot[];
  note: string;
}

export type ScheduleMode = 'WEEKLY' | 'CUSTOM';

export const SCHEDULE_MODE_OPTIONS: readonly { value: ScheduleMode; label: string }[] = [
  { value: 'WEEKLY', label: 'Lặp lại hàng tuần' },
  { value: 'CUSTOM', label: 'Chọn ngày cụ thể (lịch cá nhân)' },
];

/** Bước tăng/giảm của ô học phí/giờ (đ) — bấm mũi tên là nhảy 50k. */
export const FEE_PER_HOUR_STEP = 50000;

/** Học phí/giờ thấp nhất chấp nhận được (đ) — gia sư là sinh viên cũng đã từ mức này trở lên. */
export const FEE_PER_HOUR_MIN = 50000;

/** Độ dài chu kỳ lặp của lịch WEEKLY: 1–4 tuần. */
export const REPEAT_WEEKS_OPTIONS: readonly { value: string; label: string }[] = [
  { value: '1', label: 'Hàng tuần (học đều, không nghỉ)' },
  { value: '2', label: 'Chu kỳ 2 tuần' },
  { value: '3', label: 'Chu kỳ 3 tuần' },
  { value: '4', label: 'Chu kỳ 4 tuần' },
];


/** Một buổi học: thuộc môn nào; theo Thứ (WEEKLY) hoặc ngày cụ thể (CUSTOM); buổi + khung giờ. */
export interface ScheduleSlot {
  subjectId: string;
  day: string;
  date: string;
  session: string;
  start: string;
  end: string;
}

// Buổi trong ngày: khung giờ cho phép (min–max) + gợi ý (chọn buổi tự điền).
export const SESSION_OPTIONS: readonly {
  value: string;
  label: string;
  min: string;
  max: string;
  start: string;
  end: string;
}[] = [
  { value: 'Sáng', label: 'Sáng (6h–12h)', min: '06:00', max: '12:00', start: '08:00', end: '10:00' },
  { value: 'Chiều', label: 'Chiều (12h–18h)', min: '12:00', max: '18:00', start: '14:00', end: '16:00' },
  { value: 'Tối', label: 'Tối (18h–23h30)', min: '18:00', max: '23:30', start: '18:00', end: '20:00' },
];

export type BillingCycle = 'MONTH' | 'TERM' | 'QUARTER' | 'YEAR';

// Gợi ý đáp án cho "Mục tiêu học tập".
export const LEARNING_GOAL_OPTIONS: readonly string[] = [
  'Lấy lại gốc',
  'Ôn thi học kỳ',
  'Luyện thi chuyển cấp (vào 10)',
  'Luyện thi Đại học',
  'Luyện thi chứng chỉ (IELTS, TOEIC...)',
];

export const LEARNING_GOAL_OTHER = 'Khác';

/** Giá trị đại diện cho môn học "Khác" (tự nhập). */
export const OTHER_SUBJECT = 'other';

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

// Học theo tháng / quý / nửa năm / năm — dùng để ước tính tổng học phí.
// CẢNH BÁO: tên khóa KHÔNG khớp nhãn và không được đổi — detailsJson của các lớp đã lưu
// dùng đúng các khóa này, đổi khóa sẽ làm lớp cũ rơi về "Tháng" và sai tổng học phí.
//   TERM    = quý (3 tháng)      — trước đây gọi nhầm là "kỳ"
//   QUARTER = nửa năm (6 tháng)  — trước đây gọi nhầm là "quý" (quý đúng ra là 3 tháng)
export const BILLING_CYCLE_OPTIONS: readonly {
  value: BillingCycle;
  /** Nhãn đầy đủ trong ô chọn. */
  label: string;
  /** Tên gọn để ghép câu: "9.600.000 đ / năm", "Tổng học phí ước tính (quý)". */
  short: string;
  weeks: number;
}[] = [
  { value: 'MONTH', label: 'Tháng', short: 'tháng', weeks: 4 },
  { value: 'TERM', label: 'Quý (3 tháng)', short: 'quý', weeks: 12 },
  { value: 'QUARTER', label: 'Nửa năm (6 tháng)', short: 'nửa năm', weeks: 24 },
  { value: 'YEAR', label: 'Một năm (12 tháng)', short: 'năm', weeks: 48 },
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
