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
  lessonMode: LessonMode;
  numberOfSessions: number;
  recurringType: RecurringType;
  startDate: string;
  endDate: string;
  tuitionFee: number;
  maxStudents: number | null;
  status: ClassStatus;
  createdAt: string;
  updatedAt: string;
  schedule: ScheduleSlot[];
  assignedTutorId: number | null;
  assignedTutorName: string | null;
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
  maxStudents: number | null;
  schedule: ScheduleSlot[];
}
