
import {
  LEARNING_GOAL_OTHER,
  isOtherSubject,
  type ClassFormValues,
  type ClassResponse,
} from '../types/marketplaceTypes';

export type TutorLevel = 'NONE' | 'STUDENT' | 'CERTIFIED' | 'TEACHER';

export const TUTOR_LEVEL_OPTIONS: readonly { value: TutorLevel; label: string; rank: number }[] = [
  { value: 'NONE', label: 'Chưa có bằng cấp cụ thể', rank: 0 },
  { value: 'STUDENT', label: 'Sinh viên', rank: 1 },
  { value: 'CERTIFIED', label: 'Có chứng chỉ / bằng cấp', rank: 2 },
  { value: 'TEACHER', label: 'Giáo viên', rank: 3 },
];

export interface MatchWeights {
  subject: number; // Ws
  location: number; // Wl
  salary: number; // Wp
  schedule: number; // Wt
  experience: number; // We
}

export interface AvailabilitySlot {
  day: string; // 'T2'..'CN'
  session: string; // 'Sáng' | 'Chiều' | 'Tối'
}

export interface TutorCriteria {
  subjectIds: string[];
  gradeIds: string[];
  provinceId: string;
  districtId: string;
  onlineOnly: boolean;
  expectedFee: string;
  availability: AvailabilitySlot[];
  level: TutorLevel;
  weights: MatchWeights;
}

export const DEFAULT_WEIGHTS: MatchWeights = {
  subject: 5,
  location: 3,
  salary: 3,
  schedule: 4,
  experience: 2,
};

export function emptyCriteria(): TutorCriteria {
  return {
    subjectIds: [],
    gradeIds: [],
    provinceId: '',
    districtId: '',
    onlineOnly: false,
    expectedFee: '',
    availability: [],
    level: 'STUDENT',
    weights: { ...DEFAULT_WEIGHTS },
  };
}

function requiredLevelRank(tutorRequirement: string | null | undefined): number {
  const s = (tutorRequirement ?? '').toLowerCase();
  if (s.includes('giáo viên')) return 3;
  if (s.includes('chứng chỉ') || s.includes('bằng cấp')) return 2;
  if (s.includes('sinh viên')) return 1;
  return 0; // "Không yêu cầu cụ thể" hoặc trống
}

function levelRank(level: TutorLevel): number {
  return TUTOR_LEVEL_OPTIONS.find((o) => o.value === level)?.rank ?? 0;
}

function weekdayCode(dateStr: string): string {
  if (!dateStr) return '';
  const d = new Date(`${dateStr}T00:00:00`);
  const codes = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'];
  return codes[d.getDay()] ?? '';
}

export interface ParsedClass {
  raw: ClassResponse;
  subjectIds: string[]; // id môn thật (bỏ "other")
  hasOtherSubject: boolean;
  subjectOther: string;
  gradeId: string;
  provinceId: string;
  provinceName: string;
  districtId: string;
  districtName: string;
  lessonMode: string;
  slots: ClassFormValues['slots'];
  scheduleMode: ClassFormValues['scheduleMode'];
  repeatEveryWeeks: number;
  studyWeeks: number[];
  tutorRequirement: string | null;
  learningGoal: string | null;
  feePerHour: number;
}

export function parseClass(raw: ClassResponse): ParsedClass {
  let form: (Partial<ClassFormValues> & { subjectOther?: string }) | null = null;
  if (raw.detailsJson) {
    try {
      form = JSON.parse(raw.detailsJson) as Partial<ClassFormValues> & { subjectOther?: string };
    } catch {
      form = null;
    }
  }

  if (form) {
    const allIds = form.subjectIds ?? [];
    const subjectIds = allIds.filter((id) => !isOtherSubject(id));
    const otherText =
      Object.values(form.subjectOthers ?? {})
        .map((v) => (v ?? '').trim())
        .filter(Boolean)
        .join(', ') || (form.subjectOther ?? '');
    const fees = form.subjectFees ?? {};
    const feeValues = Object.values(fees)
      .map((v) => Number(v) || 0)
      .filter((v) => v > 0);
    const feePerHour = feeValues.length ? Math.max(...feeValues) : Number(raw.tuitionFee) || 0;
    return {
      raw,
      subjectIds,
      hasOtherSubject: allIds.some(isOtherSubject),
      subjectOther: otherText,
      gradeId: form.gradeId ?? (raw.gradeId != null ? String(raw.gradeId) : ''),
      provinceId: form.provinceId ?? '',
      provinceName: form.provinceName ?? '',
      districtId: form.districtId ?? '',
      districtName: form.districtName ?? '',
      lessonMode: form.lessonMode ?? raw.lessonMode,
      slots: form.slots ?? [],
      scheduleMode: form.scheduleMode ?? 'WEEKLY',
      repeatEveryWeeks: Math.min(4, Math.max(1, Number(form.repeatEveryWeeks) || 1)),
      studyWeeks: Array.isArray(form.studyWeeks) && form.studyWeeks.length > 0 ? form.studyWeeks : [1],
      tutorRequirement: form.tutorRequirementDetail
        ? `${form.tutorRequirement} ${form.tutorRequirementDetail}`
        : (form.tutorRequirement ?? raw.tutorRequirement),
      learningGoal:
        form.learningGoal === LEARNING_GOAL_OTHER
          ? (form.learningGoalOther || raw.learningGoal || null)
          : (form.learningGoal ?? raw.learningGoal ?? null),
      feePerHour,
    };
  }

  return {
    raw,
    subjectIds: raw.subjectId != null ? [String(raw.subjectId)] : [],
    hasOtherSubject: false,
    subjectOther: '',
    gradeId: raw.gradeId != null ? String(raw.gradeId) : '',
    provinceId: '',
    provinceName: '',
    districtId: '',
    districtName: '',
    lessonMode: raw.lessonMode,
    slots: [],
    scheduleMode: 'WEEKLY',
    repeatEveryWeeks: 1,
    studyWeeks: [1],
    tutorRequirement: raw.tutorRequirement,
    learningGoal: raw.learningGoal ?? null,
    feePerHour: Number(raw.tuitionFee) || 0,
  };
}

export interface MatchBreakdown {
  subject: number;
  location: number;
  salary: number;
  schedule: number;
  experience: number;
  score: number;
}

export interface MatchResult {
  parsed: ParsedClass;
  breakdown: MatchBreakdown;
}

function scoreSubject(pc: ParsedClass, c: TutorCriteria): number {
  if (c.subjectIds.length === 0) return 0; // gia sư chưa chọn môn → không thể khớp
  const classSubjects = pc.subjectIds;
  let overlap: number;
  if (classSubjects.length === 0) {
    overlap = pc.hasOtherSubject ? 0.5 : 0; // lớp chỉ có môn "Khác" → trung tính
  } else {
    const matched = classSubjects.filter((id) => c.subjectIds.includes(id)).length;
    overlap = matched / classSubjects.length;
  }
  if (overlap === 0) return 0;
  const gradeOk = c.gradeIds.length === 0 || (pc.gradeId ? c.gradeIds.includes(pc.gradeId) : true);
  return overlap * (gradeOk ? 1 : 0.5);
}

function scoreLocation(pc: ParsedClass, c: TutorCriteria): number {
  if (pc.lessonMode === 'ONLINE') return 1;
  if (c.onlineOnly) return 0; // gia sư chỉ dạy online mà lớp offline
  if (!pc.provinceId || !c.provinceId) return 0.5; // thiếu dữ liệu vị trí → trung tính
  if (pc.provinceId !== c.provinceId) return 0.2; // khác tỉnh
  if (c.districtId && pc.districtId) {
    return c.districtId === pc.districtId ? 1 : 0.6;
  }
  return 1; // cùng tỉnh, không đủ dữ liệu huyện → coi như khớp
}

function scoreSalary(pc: ParsedClass, c: TutorCriteria): number {
  const expected = Number(c.expectedFee) || 0;
  if (expected <= 0) return 1; // không đặt kỳ vọng
  if (pc.feePerHour <= 0) return 0.5; // lớp không ghi học phí
  return Math.min(1, pc.feePerHour / expected);
}

function scoreSchedule(pc: ParsedClass, c: TutorCriteria): number {
  if (c.availability.length === 0) return 1; // gia sư linh hoạt
  const required = pc.slots
    .map((s) => ({ day: pc.scheduleMode === 'CUSTOM' ? weekdayCode(s.date) : s.day, session: s.session }))
    .filter((s) => s.day && s.session);
  if (required.length === 0) return 1; // lớp chưa có lịch cụ thể
  const has = (day: string, session: string) =>
    c.availability.some((a) => a.day === day && a.session === session);
  const matched = required.filter((s) => has(s.day, s.session)).length;
  return matched / required.length;
}

function scoreExperience(pc: ParsedClass, c: TutorCriteria): number {
  const req = requiredLevelRank(pc.tutorRequirement);
  if (req === 0) return 1; // lớp không yêu cầu cụ thể
  const have = levelRank(c.level);
  return have >= req ? 1 : have / req;
}

export function scoreClass(pc: ParsedClass, c: TutorCriteria): MatchBreakdown {
  const S = scoreSubject(pc, c);
  const L = scoreLocation(pc, c);
  const P = scoreSalary(pc, c);
  const T = scoreSchedule(pc, c);
  const E = scoreExperience(pc, c);
  const w = c.weights;
  const wSum = w.subject + w.location + w.salary + w.schedule + w.experience;
  const weighted =
    w.subject * S + w.location * L + w.salary * P + w.schedule * T + w.experience * E;
  const score = wSum > 0 ? (weighted / wSum) * 100 : ((S + L + P + T + E) / 5) * 100;
  return { subject: S, location: L, salary: P, schedule: T, experience: E, score };
}

export function rankClasses(classes: ClassResponse[], c: TutorCriteria): MatchResult[] {
  return classes
    .map((raw) => {
      const parsed = parseClass(raw);
      return { parsed, breakdown: scoreClass(parsed, c) };
    })
    .sort((a, b) => {
      if (b.breakdown.score !== a.breakdown.score) return b.breakdown.score - a.breakdown.score;
      return (b.parsed.raw.createdAt ?? '').localeCompare(a.parsed.raw.createdAt ?? '');
    });
}

export const CRITERIA_LABELS: readonly { key: keyof MatchBreakdown; label: string; short: string }[] = [
  { key: 'subject', label: 'Môn & lớp', short: 'S' },
  { key: 'location', label: 'Địa điểm', short: 'L' },
  { key: 'salary', label: 'Học phí', short: 'P' },
  { key: 'schedule', label: 'Lịch học', short: 'T' },
  { key: 'experience', label: 'Trình độ', short: 'E' },
];
