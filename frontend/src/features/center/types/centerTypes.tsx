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
  lessonMode: LessonMode;
  numberOfSessions: number;
  recurringType: RecurringType;
  startDate: string;
  endDate: string;
  tuitionFee: number;
  status: ClassStatus;
  createdAt: string;
  updatedAt: string;
  schedule: ScheduleSlot[];
}

export interface SaveClassRequest {
  title: string;
  description?: string;
  categoryName: string;
  subjectName: string;
  gradeName: string;
  locationText: string;
  lessonMode: LessonMode | null;
  numberOfSessions: number | null;
  recurringType: RecurringType | null;
  startDate: string | null;
  endDate: string | null;
  tuitionFee: number | null;
  schedule: ScheduleSlot[];
}
