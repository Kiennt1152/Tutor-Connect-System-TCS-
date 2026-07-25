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
  reason: string;
  effectiveDate?: string;
}

export interface ClassTerminationResponse {
  terminationId: number;
  classId: number;
  assignmentId: number;
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
  schedule: ScheduleSlot[];
  createdAt: string;
}
