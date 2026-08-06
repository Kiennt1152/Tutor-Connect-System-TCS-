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
  status: RecruitmentPostStatus;
  publishedAt: string | null;
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

export interface CertificateInfo {
  documentType: VerificationDocumentType | null;
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
  /** EXTERNAL = yêu cầu ngoài (đã có học sinh); SELF = trung tâm tự tạo. */
  originType: string | null;
  status: ClassStatus;
  createdAt: string;
  updatedAt: string;
  schedule: ScheduleSlot[];
  assignedTutorId: number | null;
  assignedTutorName: string | null;
  assistantTutorId: number | null;
  assistantTutorName: string | null;
  students?: StudentAttendance[];
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
  schedule: ScheduleSlot[];
}

/** Mẫu hợp đồng trung tâm dùng/quản lý. */
export interface ContractTemplate {
  templateId: number;
  name: string;
  content: string;
  /** RECRUITMENT (tuyển dụng/hợp tác gia sư) hoặc CLASS (học viên/dạy lớp). */
  contractType?: 'RECRUITMENT' | 'CLASS';
  defaultTemplate: boolean;
  status: string;
  /** true = mẫu hệ thống (không sửa được). */
  system: boolean;
}
