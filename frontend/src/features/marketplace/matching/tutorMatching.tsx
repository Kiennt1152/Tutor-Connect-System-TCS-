
import {
  LEARNING_GOAL_OTHER,
  isOtherSubject,
  type ClassFormValues,
  type ClassResponse,
} from '../types/marketplaceTypes';

/** Bỏ dấu + hạ chữ thường. Lớp do khách đăng chỉ lưu TÊN tỉnh/phường (không lưu id), nên
 *  mọi phép so địa danh đều phải đi qua đây. */
export const normalizeName = (s: string | null | undefined): string =>
  (s ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/đ/g, 'd')
    .replace(/\s+/g, ' ')
    .trim();

/** "Thành phố Hà Nội", "TP Hà Nội" và "Hà Nội" phải coi là một. */
export const provinceKey = (s: string | null | undefined): string =>
  normalizeName(s).replace(/^(tp|thanh pho|tinh)\s+/, '');

/** "Phường Cầu Giấy", "Quận Cầu Giấy" và "Cầu Giấy" phải coi là một — dữ liệu cũ lưu
 *  quận/huyện, dữ liệu mới lưu phường/xã. */
export const wardKey = (s: string | null | undefined): string =>
  normalizeName(s).replace(/^(phuong|xa|thi tran|thi xa|quan|huyen|tx|tp|thanh pho)\s+/, '');

export interface MatchWeights {
  subject: number; // Ws — môn học
  location: number; // Wl — địa điểm
  salary: number; // Wp — học phí
  schedule: number; // Wt — lịch học
  grade: number; // We — khối lớp
}

export interface AvailabilitySlot {
  day: string; // 'T2'..'CN'
  session: string; // 'Sáng' | 'Chiều' | 'Tối'
}

/** Tiêu chí gia sư đặt ra. Mỗi trường ở đây soi vào đúng một trường trong tin lớp học. */
export interface TutorCriteria {
  subjectIds: string[]; // -> S : đối chiếu subjectIds của lớp
  otherSubjectText: string; // -> S : môn ngoài danh mục, so theo tên
  gradeIds: string[]; // -> E : đối chiếu gradeId của lớp
  provinceName: string; // -> L : đối chiếu provinceName của lớp
  wardName: string; // -> L : đối chiếu wardName (hoặc districtName) của lớp
  onlineOnly: boolean; // -> L : chỉ nhận lớp online
  expectedFee: string; // -> P : đối chiếu subjectFees của lớp
  availability: AvailabilitySlot[]; // -> T : đối chiếu slots của lớp
  weights: MatchWeights;
}

export const DEFAULT_WEIGHTS: MatchWeights = {
  subject: 5,
  location: 3,
  salary: 3,
  schedule: 4,
  grade: 2,
};

export function emptyCriteria(): TutorCriteria {
  return {
    subjectIds: [],
    otherSubjectText: '',
    gradeIds: [],
    provinceName: '',
    wardName: '',
    onlineOnly: false,
    expectedFee: '',
    availability: [],
    weights: { ...DEFAULT_WEIGHTS },
  };
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
  wardName: string;
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
      wardName: form.wardName ?? '',
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
    wardName: '',
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
  grade: number;
  score: number;
}

export interface MatchResult {
  parsed: ParsedClass;
  breakdown: MatchBreakdown;
}

function otherSubjectMatches(classText: string, wanted: string): boolean {
  const a = normalizeName(classText);
  const b = normalizeName(wanted);
  if (!a || !b) return false;
  return a.includes(b) || b.includes(a);
}

/**
 * S — Môn học. Tỉ lệ môn mà LỚP đang cần và gia sư dạy được.
 * Lớp cần Toán + Văn + Anh, gia sư tìm "toán văn" -> 2/3; tìm đủ 3 môn -> 1.
 * Khối lớp KHÔNG tính ở đây nữa (đã tách sang tiêu chí E).
 */
function scoreSubject(pc: ParsedClass, c: TutorCriteria): number {
  const wantsOther = c.otherSubjectText.trim() !== '';
  if (c.subjectIds.length === 0 && !wantsOther) return 1; // không lọc theo môn -> bỏ qua tiêu chí

  const totalNeeded = pc.subjectIds.length + (pc.hasOtherSubject ? 1 : 0);
  if (totalNeeded === 0) return 0.5; // lớp không ghi môn -> trung tính

  let matched = pc.subjectIds.filter((id) => c.subjectIds.includes(id)).length;
  if (pc.hasOtherSubject && wantsOther && otherSubjectMatches(pc.subjectOther, c.otherSubjectText)) {
    matched += 1;
  }
  return matched / totalNeeded;
}

/**
 * E — Khối lớp. Chính là ô "Lớp" mà khách chọn khi đăng tin (Lớp 1..12,
 * Luyện thi chứng chỉ, Luyện thi Đại học): trùng thì trọn điểm, khác thì 0.
 */
function scoreGrade(pc: ParsedClass, c: TutorCriteria): number {
  if (c.gradeIds.length === 0) return 1; // gia sư không kén khối
  if (!pc.gradeId) return 0.5; // tin không ghi khối -> trung tính
  return c.gradeIds.includes(pc.gradeId) ? 1 : 0;
}

/**
 * L — Địa điểm. So theo TÊN tỉnh trước (khác tỉnh là không match), rồi mới xét
 * phường/xã. Dữ liệu cũ lưu quận/huyện nên lấy wardName trước, thiếu thì dùng districtName.
 */
function scoreLocation(pc: ParsedClass, c: TutorCriteria): number {
  if (pc.lessonMode === 'ONLINE') return 1; // học online thì ở đâu cũng dạy được
  if (c.onlineOnly) return 0; // gia sư chỉ nhận online mà lớp lại offline
  const wantProvince = provinceKey(c.provinceName);
  if (!wantProvince) return 1; // gia sư không kén nơi dạy -> bỏ qua tiêu chí
  const gotProvince = provinceKey(pc.provinceName);
  if (!gotProvince) return 0.5; // tin không ghi tỉnh -> trung tính
  if (gotProvince !== wantProvince) return 0; // khác tỉnh -> không match

  const wantWard = wardKey(c.wardName);
  if (!wantWard) return 1; // chỉ chọn tới cấp tỉnh
  const gotWard = wardKey(pc.wardName || pc.districtName);
  if (!gotWard) return 0.7; // tin chỉ ghi tới tỉnh
  return gotWard === wantWard ? 1 : 0.5; // cùng tỉnh khác phường vẫn còn đi lại được
}

/**
 * P — Học phí. Đối chiếu mức mong muốn của gia sư với học phí cao nhất trong tin.
 * Đạt hoặc vượt là match trọn điểm; thiếu bao nhiêu trừ theo tỉ lệ bấy nhiêu.
 */
function scoreSalary(pc: ParsedClass, c: TutorCriteria): number {
  const expected = Number(c.expectedFee) || 0;
  if (expected <= 0) return 1; // không đặt kỳ vọng -> bỏ qua tiêu chí
  if (pc.feePerHour <= 0) return 0.5; // tin không ghi học phí -> trung tính
  return Math.min(1, pc.feePerHour / expected);
}

/**
 * T — Lịch học. Tỉ lệ buổi trong tin rơi vào khung giờ gia sư khai rảnh.
 * Lịch CUSTOM ghi theo ngày nên phải suy ra thứ trước khi so.
 */
function scoreSchedule(pc: ParsedClass, c: TutorCriteria): number {
  if (c.availability.length === 0) return 1; // gia sư linh hoạt -> bỏ qua tiêu chí
  const required = pc.slots
    .map((s) => ({ day: pc.scheduleMode === 'CUSTOM' ? weekdayCode(s.date) : s.day, session: s.session }))
    .filter((s) => s.day && s.session);
  if (required.length === 0) return 0.5; // tin chưa ghi lịch -> trung tính
  const has = (day: string, session: string) =>
    c.availability.some((a) => a.day === day && a.session === session);
  return required.filter((s) => has(s.day, s.session)).length / required.length;
}

export function scoreClass(pc: ParsedClass, c: TutorCriteria): MatchBreakdown {
  const S = scoreSubject(pc, c);
  const L = scoreLocation(pc, c);
  const P = scoreSalary(pc, c);
  const T = scoreSchedule(pc, c);
  const E = scoreGrade(pc, c);
  const w = c.weights;
  const wSum = w.subject + w.location + w.salary + w.schedule + w.grade;
  const weighted = w.subject * S + w.location * L + w.salary * P + w.schedule * T + w.grade * E;
  const score = wSum > 0 ? (weighted / wSum) * 100 : ((S + L + P + T + E) / 5) * 100;
  return { subject: S, location: L, salary: P, schedule: T, grade: E, score };
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
  { key: 'subject', label: 'Môn học', short: 'S' },
  { key: 'location', label: 'Địa điểm', short: 'L' },
  { key: 'salary', label: 'Học phí', short: 'P' },
  { key: 'schedule', label: 'Lịch học', short: 'T' },
  { key: 'grade', label: 'Khối lớp', short: 'E' },
];

export const WEEKDAYS: readonly { code: string; label: string }[] = [
  { code: 'T2', label: 'T2' },
  { code: 'T3', label: 'T3' },
  { code: 'T4', label: 'T4' },
  { code: 'T5', label: 'T5' },
  { code: 'T6', label: 'T6' },
  { code: 'T7', label: 'T7' },
  { code: 'CN', label: 'CN' },
];

export const SESSIONS: readonly string[] = ['Sáng', 'Chiều', 'Tối'];
