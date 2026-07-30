
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

export interface CatalogOption {
  id: number;
  name: string;
  description?: string | null;
}

export interface LocationOption {
  locationId: number;
  provinceId: number;
  provinceName: string;
  districtName: string | null;
  wardName: string | null;
}

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
  applicationCount: number | null;
}

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
  proposedRates: Record<string, number> | null;
  coverLetter: string | null;
  status: 'SUBMITTED' | 'UNDER_REVIEW' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN';
  appliedAt: string;
  matchScore: number;
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

export interface ClassFormValues {
  subjectIds: string[];
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
  subjectFees: Record<string, string>;
  billingCycle: BillingCycle;
  months: string;
  scheduleMode: ScheduleMode;
  repeatEveryWeeks: string;
  studyWeeks: number[];
  slots: ScheduleSlot[];
  note: string;
}

export type ScheduleMode = 'WEEKLY' | 'CUSTOM';

export const SCHEDULE_MODE_OPTIONS: readonly { value: ScheduleMode; label: string }[] = [
  { value: 'WEEKLY', label: 'Lặp lại hàng tuần' },
  { value: 'CUSTOM', label: 'Chọn ngày cụ thể (lịch cá nhân)' },
];

export const FEE_PER_HOUR_STEP = 50000;

export const FEE_PER_HOUR_MIN = 50000;

export const REPEAT_WEEKS_OPTIONS: readonly { value: string; label: string }[] = [
  { value: '1', label: 'Hàng tuần (học đều, không nghỉ)' },
  { value: '2', label: 'Chu kỳ 2 tuần' },
  { value: '3', label: 'Chu kỳ 3 tuần' },
  { value: '4', label: 'Chu kỳ 4 tuần' },
];

export interface ScheduleSlot {
  subjectId: string;
  day: string;
  date: string;
  session: string;
  start: string;
  end: string;
}

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
  { value: 'Tối', label: 'Tối (18h–0h)', min: '18:00', max: '23:59', start: '18:00', end: '20:00' },
];

export type BillingCycle = 'MONTH' | 'TERM' | 'QUARTER' | 'YEAR';

export const LEARNING_GOAL_OPTIONS: readonly string[] = [
  'Lấy lại gốc',
  'Ôn thi học kỳ',
  'Luyện thi chuyển cấp (vào 10)',
  'Luyện thi Đại học',
  'Luyện thi chứng chỉ (IELTS, TOEIC...)',
];

export const LEARNING_GOAL_OTHER = 'Khác';

export const OTHER_SUBJECT = 'other';

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

export const BILLING_CYCLE_OPTIONS: readonly {
  value: BillingCycle;
  label: string;
  short: string;
  weeks: number;
}[] = [
  { value: 'MONTH', label: 'Tháng', short: 'tháng', weeks: 4 },
  { value: 'TERM', label: 'Quý (3 tháng)', short: 'quý', weeks: 12 },
  { value: 'QUARTER', label: 'Nửa năm (6 tháng)', short: 'nửa năm', weeks: 24 },
  { value: 'YEAR', label: 'Một năm (12 tháng)', short: 'năm', weeks: 48 },
];

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
