
export type LessonMode = 'ONLINE' | 'OFFLINE' | 'HYBRID';
export type RecurringType = 'DAILY' | 'WEEKLY' | 'ONCE';
export type ClassStatus =
  | 'DRAFT'
  | 'OPEN'
  | 'MATCHED'
  | 'ENROLLMENT_CLOSED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'DISPUTED';

export interface MarketplaceResponse {}

export type ClassTerminationStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED';

export interface CreateClassTerminationRequest {
  assignmentId?: number;
  classStudentId?: number;
  reason: string;
  effectiveDate?: string;
  bankName: string;
  accountNo: string;
  accountHolderName: string;
}

export interface ClassTerminationResponse {
  terminationId: number;
  classId: number;
  assignmentId: number | null;
  classStudentId: number | null;
  requestedByUserId: number;
  reason: string;
  effectiveDate: string | null;
  bankName?: string | null;
  accountNoMasked?: string | null;
  accountHolderName?: string | null;
  status: ClassTerminationStatus;
  createdAt: string;
  processedAt: string | null;
}

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
  /** Hạn hiển thị (đăng lớp + 30 ngày); null nếu không tính hạn. Chỉ có với lớp OPEN. */
  expiresAt: string | null;
  applicationCount: number | null;
  assignmentId: number | null;
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
  refundPayoutInfo?: RefundPayoutInfoPayload | null;
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
  subjectOthers: Record<string, string>;
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
  durationUnit: DurationUnit;
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

export type DurationUnit = 'MONTH' | 'YEAR';

export type DurationChoice = 'MONTH_FREE' | 'TERM' | 'QUARTER' | 'YEAR_FREE';

export const DURATION_CHOICE_OPTIONS: readonly { value: DurationChoice; label: string }[] = [
  { value: 'MONTH_FREE', label: 'Tháng' },
  { value: 'TERM', label: 'Quý (3 tháng)' },
  { value: 'QUARTER', label: 'Nửa năm (6 tháng)' },
  { value: 'YEAR_FREE', label: 'Năm' },
];

export const LEARNING_GOAL_OPTIONS: readonly string[] = [
  'Lấy lại gốc',
  'Ôn thi học kỳ',
  'Luyện thi chuyển cấp (vào 10)',
  'Luyện thi Đại học',
  'Luyện thi chứng chỉ (IELTS, TOEIC...)',
];

export const LEARNING_GOAL_OTHER = 'Khác';

export const OTHER_SUBJECT = 'other';
export const OTHER_PREFIX = 'other:';
export const isOtherSubject = (id: string): boolean =>
  id === OTHER_SUBJECT || id.startsWith(OTHER_PREFIX);
export const newOtherSubjectId = (): string =>
  `${OTHER_PREFIX}${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;

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

/** Lịch học một buổi do backend trả về (đăng ký lớp trực tiếp). */
export interface MarketplaceScheduleSlot {
  slotId: number;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
}

/** Lớp học hiển thị ở marketplace (đăng ký trực tiếp). */
export interface MarketplaceClass {
  classId: number;
  title: string;
  description: string | null;
  creatorId: number;
  creatorName: string | null;
  subjectId: number | null;
  subjectName: string | null;
  gradeId: number | null;
  gradeName: string | null;
  lessonMode: LessonMode;
  numberOfSessions: number;
  startDate: string;
  endDate: string;
  tuitionFee: number;
  budget: number | null;
  recurringType: RecurringType;
  status: ClassStatus;
  /** PRIVATE (lớp cá nhân) / CENTER (lớp của trung tâm — gia sư không tự đăng ký). */
  classType?: 'PRIVATE' | 'CENTER';
  maxStudents: number | null;
  enrolledCount: number;
  canRequestTermination: boolean;
  refundAllowed: boolean;
  refundBlockedReason: string | null;
  totalSessions: number | null;
  completedSessions: number | null;
  terminationAssignmentId: number | null;
  terminationClassStudentId: number | null;
  /** UC "Xác nhận lớp đã hoàn thành" (lớp PRIVATE 1 gia sư – 1 phụ huynh/học viên). */
  completionAssignmentId: number | null;
  canConfirmCompletion: boolean;
  completionPendingOther: boolean;
  completionBlockedReason: string | null;
  schedule: MarketplaceScheduleSlot[];
  createdAt: string;
}

/** Trung tâm đã xác minh — để phụ huynh chọn khi gửi yêu cầu mở lớp. */
export interface CenterSummary {
  centerId: number;
  companyName: string;
  description: string | null;
  address: string | null;
  phone: string | null;
  avatar: string | null;
}

export type ClassRequestStatus =
  | 'PAYMENT_PENDING'
  | 'PENDING'
  | 'SEARCHING'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'CANCELLED';
export type CenterRequestFeeStatus =
  | 'PENDING_PAYMENT'
  | 'HELD'
  | 'REFUND_REQUESTED'
  | 'RELEASED'
  | 'REFUNDED'
  | 'CANCELLED';

export interface RefundPayoutInfoPayload {
  bankName: string;
  accountNo: string;
  accountHolderName: string;
}

export interface CenterRequestFeePayment {
  requestId: string;
  feeHoldId: number;
  status: CenterRequestFeeStatus;
  amount: number;
  referenceCode: string;
  bankName: string;
  bankBin: string;
  accountNumber: string;
  accountName: string;
  transferContent: string;
  qrUrl: string;
  classId: number | null;
  assignmentId: number | null;
  payoutBankName: string | null;
  payoutAccountNoMasked: string | null;
  payoutAccountHolderName: string | null;
  paidAt: string | null;
  releasedAt: string | null;
  refundedAt: string | null;
}

/** Bằng cấp / chứng chỉ đã xác minh của gia sư (để phụ huynh xem trước khi chọn). */
export interface CandidateCertificate {
  documentType: string | null;
  fileName: string;
  fileUrl: string;
  mimeType: string | null;
  fileSize: number | null;
}

/** Gia sư trung tâm đề cử cho một yêu cầu (shortlist). */
export interface CandidateTutor {
  tutorId: number;
  fullName: string;
  experienceYears: number | null;
  ratingAvg: number | null;
  certificates?: CandidateCertificate[];
}

/** Yêu cầu mở lớp phụ huynh gửi tới một trung tâm. */
export interface ClassRequest {
  requestId: string;
  centerId: number;
  centerName: string | null;
  clientUserId: number;
  clientName: string | null;
  categoryId: number | null;
  categoryName: string | null;
  note: string;
  desiredBudget: number | null;
  status: ClassRequestStatus;
  reason: string | null;
  createdAt: string;
  /** Nguyên payload form "tìm gia sư" (JSON) để trung tâm xem chi tiết. */
  detailsJson: string | null;
  /** Gia sư trung tâm đề cử để phụ huynh chọn. */
  candidates: CandidateTutor[];
  /** Tin tuyển dụng trung tâm đã đăng cho yêu cầu này (null = chưa đăng). */
  recruitmentPostId: number | null;
  centerRequestFeePayment?: CenterRequestFeePayment | null;
}

export interface CreateClassRequestPayload {
  categoryId?: number | null;
  note: string;
  desiredBudget?: number | null;
  /** Nguyên payload form "tìm gia sư" (JSON). */
  detailsJson?: string;
  refundPayoutInfo?: RefundPayoutInfoPayload | null;
}
