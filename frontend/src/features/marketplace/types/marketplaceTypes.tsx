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
}

export interface ClassTerminationResponse {
  terminationId: number;
  classId: number;
  assignmentId: number | null;
  classStudentId: number | null;
  requestedByUserId: number;
  reason: string;
  effectiveDate: string | null;
  status: ClassTerminationStatus;
  createdAt: string;
  processedAt: string | null;
}

export interface ScheduleSlot {
  slotId: number;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
}

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
  maxStudents: number | null;
  enrolledCount: number;
  canRequestTermination: boolean;
  terminationAssignmentId: number | null;
  terminationClassStudentId: number | null;
  schedule: ScheduleSlot[];
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

export type ClassRequestStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

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
}

export interface CreateClassRequestPayload {
  categoryId?: number | null;
  note: string;
  desiredBudget?: number | null;
}
