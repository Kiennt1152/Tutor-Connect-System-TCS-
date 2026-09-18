// ===================== Tin tuyển gia sư (FT-33) =====================

export type RecruitmentPostStatus = 'DRAFT' | 'ACTIVE' | 'CLOSED';

export type RecruitmentApplicationStatus =
  | 'APPLIED'
  | 'SCREENING'
  | 'INTERVIEW'
  | 'PASSED'
  | 'HIRED'
  | 'REJECTED'
  | 'WITHDRAWN';

/** Một tin tuyển gia sư (FT-33). */
export interface RecruitmentPost {
  recruitmentId: number;
  centerId: number;
  centerName: string | null;
  /** Lớp mà tin này tuyển cho (nếu có). Null = tin tuyển chung. */
  classId: number | null;
  classTitle: string | null;
  title: string;
  description: string;
  requirements: string | null;
  benefits: string | null;
  requiredExperience: number | null;
  maxPositions: number | null;
  subjectId: number | null;
  subjectName: string | null;
  locationId: number | null;
  locationLabel: string | null;
  provinceName: string | null;
  wardName: string | null;
  addressDetail: string | null;
  /** true khi bạn đã thuộc đội ngũ gia sư của trung tâm này — không được ứng tuyển. */
  alreadyCenterTutor?: boolean;
  status: RecruitmentPostStatus;
  publishedAt: string | null;
  /** Mốc tin hết hạn hiển thị (30 ngày kể từ lúc đăng), null khi tin chưa đăng. */
  expiresAt?: string | null;
  closedAt: string | null;
  createdAt: string;
  updatedAt: string;
  applicationCount: number;
}

/** Dữ liệu tạo/sửa tin. */
export interface SaveRecruitmentPostRequest {
  /** Lớp cần tuyển (tuỳ chọn). Null/bỏ trống = tin tuyển chung. */
  classId?: number | null;
  title: string;
  description: string;
  requirements?: string;
  benefits?: string;
  requiredExperience?: number | null;
  maxPositions?: number | null;
  subjectName?: string;
  provinceName?: string;
  wardName?: string;
  addressDetail?: string;
}

/** Một đơn ứng tuyển. */
export interface RecruitmentApplication {
  recruitmentAppId: number;
  recruitmentId: number;
  postTitle: string | null;
  centerName: string | null;
  tutorId: number;
  tutorName: string | null;
  tutorPhone: string | null;
  tutorAvatar: string | null;
  experienceYears: number | null;
  ratingAvg: number | null;
  verificationStatus: string | null;
  coverLetter: string | null;
  status: RecruitmentApplicationStatus;
  appliedAt: string;
  reviewedAt: string | null;
  /** Bằng cấp / chứng chỉ đã xác minh gia sư đã nộp (không gồm ảnh CCCD). */
  certificates?: CertificateInfo[];
}

export type VerificationDocumentType = 'ID_CARD' | 'DEGREE' | 'CERTIFICATE' | 'LICENSE';

export interface CenterStatsTotals {
  classCount: number;
  activeClassCount: number;
  completedClassCount: number;
  studentCount: number;
  present: number;
  absent: number;
  excused: number;
  totalMarks: number;
  attendanceRate: number;
}
export interface CenterClassStat {
  classId: number;
  title: string;
  status: string;
  tutorId: number | null;
  tutorName: string | null;
  studentCount: number;
  present: number;
  absent: number;
  excused: number;
  attendanceRate: number;
  totalSessions?: number;
  completedSessions?: number;
  progressPercent?: number;
  startDate?: string | null;
  endDate?: string | null;
}
export interface CenterStudentStat {
  classStudentId: number;
  studentName: string;
  classId: number;
  className: string;
  tutorId: number | null;
  tutorName: string | null;
  present: number;
  absent: number;
  excused: number;
  attendanceRate: number;
}
export interface CenterStats {
  totals: CenterStatsTotals;
  classes: CenterClassStat[];
  students: CenterStudentStat[];
}

/** UC-41: báo cáo tài chính của trung tâm. */
export interface CenterFinanceSummary {
  from: string;
  to: string;
  /** Học phí học viên đã nộp vào ký quỹ trong kỳ (trước phí nền tảng). */
  grossCollected: number;
  /** Đã giải ngân về ví trong kỳ, tính gộp trước phí. */
  releasedGross: number;
  platformFee: number;
  /** Thực nhận = đã giải ngân − phí nền tảng. */
  netReceived: number;
  refunded: number;
  withdrawn: number;
  /** Ba số dưới là số dư tức thời, không theo kỳ. */
  heldInEscrow: number;
  availableBalance: number;
  frozenBalance: number;
}
export interface CenterFinanceClassRow {
  classId: number;
  title: string;
  subjectName: string | null;
  status: ClassStatus | null;
  tuitionFee: number | null;
  paidStudents: number;
  gross: number;
  held: number;
  released: number;
  refunded: number;
}
export interface CenterFinanceMonthRow {
  /** yyyy-MM */
  month: string;
  gross: number;
  released: number;
}
export interface CenterFinanceReport {
  summary: CenterFinanceSummary;
  classes: CenterFinanceClassRow[];
  months: CenterFinanceMonthRow[];
}

export interface CertificateInfo {
  documentType: VerificationDocumentType | null;
  fileId: number | null;
  fileName: string;
  fileUrl: string;
  mimeType: string | null;
  fileSize: number | null;
}

export type MembershipStatus = 'ACTIVE' | 'INACTIVE' | 'TERMINATED';

/** Một tin tuyển dụng của trung tâm mà gia sư đã ứng tuyển. */
export interface AppliedPost {
  recruitmentId: number;
  postTitle: string | null;
  applicationStatus: RecruitmentApplicationStatus;
  appliedAt: string;
}

/** Một gia sư là thành viên của trung tâm. */
export interface CenterMember {
  membershipId: number;
  tutorId: number;
  tutorName: string | null;
  tutorPhone: string | null;
  tutorAvatar: string | null;
  experienceYears: number | null;
  ratingAvg: number | null;
  verificationStatus: string | null;
  joinedAt: string;
  status: MembershipStatus;
  appliedPosts?: AppliedPost[];
}

// ===================== Quản lý lớp học của Trung tâm (UC-14-B) =====================

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

export interface ScheduleSlot {
  slotId?: number;
  dayOfWeek: number; // 1 = Thứ Hai ... 7 = Chủ Nhật
  startTime: string; // "HH:mm"
  endTime: string; // "HH:mm"
}

export interface ClassResponse {
  classId: number;
  title: string;
  description: string | null;
  creatorId: number;
  centerId: number | null;
  categoryId: number | null;
  categoryName: string | null;
  subjectId: number | null;
  subjectName: string | null;
  gradeId: number | null;
  gradeName: string | null;
  locationId: number | null;
  locationLabel: string | null;
  locationText: string | null;
  provinceName: string | null;
  wardName: string | null;
  addressDetail: string | null;
  lessonMode: LessonMode;
  numberOfSessions: number;
  recurringType: RecurringType;
  startDate: string;
  endDate: string;
  tuitionFee: number;
  maxStudents: number | null;
  minStudents: number | null;
  enrolledCount: number;
  /** BF-04: hạn ghi danh (30 ngày kể từ lúc đăng tải), null khi lớp chưa mở ghi danh. */
  enrollmentDeadline?: string | null;
  /** Mốc ghi danh thực sự đóng — dùng cho đồng hồ đếm ngược. */
  enrollmentExpiresAt?: string | null;
  /** Backend quyết định: false ngay khi có học sinh đăng ký, kể cả đang chờ ký hợp đồng. */
  editable?: boolean;
  /** Lý do lớp bị khoá sửa, null khi còn sửa được. */
  editLockReason?: string | null;
  /** EXTERNAL = yêu cầu ngoài (đã có học sinh); SELF = trung tâm tự tạo. */
  originType: string | null;
  /** Mẫu hợp đồng học viên đã chọn cho lớp (đổ lại form khi sửa). */
  contractTemplateId: number | null;
  /** Nội dung điều khoản HĐ học viên đã lưu cho lớp (đổ lại form khi sửa). */
  contractContent: string | null;
  status: ClassStatus;
  createdAt: string;
  updatedAt: string;
  schedule: ScheduleSlot[];
  assignedTutorId: number | null;
  assignedTutorName: string | null;
  assistantTutorId: number | null;
  assistantTutorName: string | null;
  students?: StudentAttendance[];
  /** true nếu gia sư đã xác nhận hoàn thành — trung tâm cần xác nhận để đóng lớp. */
  tutorCompletionConfirmed?: boolean;
}

export interface TutorOption {
  tutorId: number;
  fullName: string;
  experienceYears: number | null;
  ratingAvg: number | null;
  verificationStatus: string | null;
  phone: string | null;
  avatar: string | null;
  bio: string | null;
  scheduleConflict?: boolean;
  conflictClassTitle?: string | null;
}

export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'EXCUSED';

export interface StudentAttendance {
  classStudentId: number;
  studentName: string;
  studentPhone: string | null;
  status: AttendanceStatus | null;
}

export interface ScheduleClass {
  classId: number;
  title: string;
  subjectName: string | null;
  gradeName: string | null;
  lessonMode: LessonMode;
  slots: ScheduleSlot[];
  assignedTutorId: number | null;
  assignedTutorName: string | null;
  studentCount: number;
  students: StudentAttendance[];
  attendanceTaken: boolean;
  rescheduled?: boolean;
  rescheduleNote?: string | null;
  /** Buổi này do gia sư phụ dạy thay (đã duyệt). */
  substituted?: boolean;
  substituteNote?: string | null;
  /** (Góc nhìn gia sư chính) buổi này đã bàn giao cho gia sư phụ — không thao tác nữa. */
  handedOff?: boolean;
  /** Gia sư phụ của lớp (nếu có) — để gia sư chính biết có thể nhờ dạy thay. */
  assistantTutorId?: number | null;
  assistantTutorName?: string | null;
  /** true nếu đây là buổi cuối của khóa — nơi hiện nút "Xác nhận hoàn thành". */
  finalSession?: boolean;
  /** true nếu lớp đã được xác nhận hoàn thành. */
  classCompleted?: boolean;
  /** true nếu gia sư đã xác nhận hoàn thành (đang chờ trung tâm đóng lớp). */
  tutorCompletionConfirmed?: boolean;
}

export type RescheduleStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface Reschedule {
  classId: number;
  className: string | null;
  originalDate: string;
  newDate: string;
  newStartTime: string | null;
  newEndTime: string | null;
  status: RescheduleStatus;
  tutorId: number | null;
  tutorName: string | null;
  reason: string | null;
}

export interface Substitution {
  classId: number;
  className: string | null;
  date: string;
  status: RescheduleStatus;
  reason: string | null;
  mainTutorId: number | null;
  mainTutorName: string | null;
  assistantTutorId: number | null;
  assistantTutorName: string | null;
}

export interface SaveClassRequest {
  title: string;
  description?: string;
  categoryName: string;
  subjectName: string;
  gradeName: string;
  provinceName: string;
  wardName: string;
  addressDetail: string;
  lessonMode: LessonMode | null;
  numberOfSessions: number | null;
  recurringType: RecurringType | null;
  startDate: string | null;
  endDate: string | null;
  tuitionFee: number | null;
  maxStudents: number | null;
  minStudents?: number | null;
  /** EXTERNAL (yêu cầu ngoài) / SELF (tự tạo). Mặc định SELF. */
  originType?: string;
  /** Mẫu hợp đồng đã chọn cho lớp (tuỳ chọn). */
  contractTemplateId?: number | null;
  /** Nội dung điều khoản HĐ học viên center nhập khi tạo/sửa lớp (tuỳ chọn). */
  contractContent?: string;
  schedule: ScheduleSlot[];
}

/** Mẫu hợp đồng trung tâm dùng/quản lý. */
/** Thông tin trung tâm hiển thị ở khối BÊN A của hợp đồng. */
export interface CenterContractInfo {
  companyName: string | null;
  address: string | null;
  phone: string | null;
  email: string | null;
  website: string | null;
  representativeName: string | null;
  representativePosition: string | null;
  verificationStatus: string | null;
}

export interface ContractTemplate {
  templateId: number;
  name: string;
  content: string;
  /**
   * Phân loại hợp đồng:
   * - PRIVATE_TUTORING: Hợp đồng dạy kèm 1:1 cá nhân (Phụ huynh <-> Gia sư)
   * - CENTER_CLASS: Hợp đồng đào tạo theo lớp trung tâm (Học viên <-> Trung tâm)
   * - RECRUITMENT: Thỏa thuận hợp tác tuyển dụng gia sư (Trung tâm <-> Gia sư)
   * - SPECIALIZED_GUARANTEE: Hợp đồng cam kết đầu ra / luyện thi chứng chỉ quốc tế
   * - CLASS: Tương thích ngược với hệ thống cũ
   */
  contractType?: 'RECRUITMENT' | 'CLASS' | 'PRIVATE_TUTORING' | 'CENTER_CLASS' | 'SPECIALIZED_GUARANTEE' | string;
  defaultTemplate: boolean;
  status: string;
  /** true = mẫu hệ thống (không sửa được). */
  system: boolean;
}
