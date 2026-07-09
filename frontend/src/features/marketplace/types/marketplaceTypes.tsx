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
  slotId: number;
  dayOfWeek: number; // 1 = Thứ Hai ... 7 = Chủ Nhật
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
