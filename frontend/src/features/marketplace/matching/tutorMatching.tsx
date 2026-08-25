
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

/** Một khung giờ gia sư nêu ra. Được phép để trống một vế:
 *  {day:'T3', session:'Tối'} = "tối thứ 3" · {day:'T3', session:''} = "thứ 3" ·
 *  {day:'', session:'Chiều'} = "buổi chiều". */
export interface AvailabilitySlot {
  day: string; // 'T2'..'CN' hoặc '' nếu không nêu thứ
  session: string; // 'Sáng' | 'Chiều' | 'Tối' hoặc '' nếu không nêu buổi
}

/** Tiêu chí gia sư đặt ra. Mỗi trường ở đây soi vào đúng một trường trong tin lớp học. */
export interface TutorCriteria {
  subjectIds: string[]; // -> S : đối chiếu subjectIds của lớp
  otherSubjectText: string; // -> S : môn ngoài danh mục, so theo tên
  gradeIds: string[]; // -> E : đối chiếu gradeId của lớp
  /** id khối -> số lớp (Lớp 7 = 7). Khối phi số (chứng chỉ, đại học) không có mặt ở đây. */
  gradeLevels: Record<string, number>;
  provinceName: string; // -> L : đối chiếu provinceName của lớp
  wardName: string; // -> L : đối chiếu wardName (hoặc districtName) của lớp
  onlineOnly: boolean; // -> L : chỉ nhận lớp online
  expectedFee: string; // -> P : đối chiếu subjectFees của lớp
  availability: AvailabilitySlot[]; // -> T : đối chiếu slots của lớp
  /** Vế nào được nêu trước trong câu tìm — quyết định thứ tự xếp khi điểm bằng nhau. */
  scheduleLead: 'day' | 'session';
  weights: MatchWeights;
}

export const DEFAULT_WEIGHTS: MatchWeights = {
  subject: 5,
  location: 5,
  salary: 5,
  schedule: 5,
  grade: 5,
};

export function emptyCriteria(): TutorCriteria {
  return {
    subjectIds: [],
    otherSubjectText: '',
    gradeIds: [],
    gradeLevels: {},
    provinceName: '',
    wardName: '',
    onlineOnly: false,
    expectedFee: '',
    availability: [],
    scheduleLead: 'day',
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
  /** Học phí cao nhất trong các môn — dùng để chấm tiêu chí P. */
  feePerHour: number;
  /** Học phí thấp nhất; khác feePerHour khi lớp nhiều môn mỗi môn một giá. */
  feeMin: number;
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
    const feeMin = feeValues.length ? Math.min(...feeValues) : feePerHour;
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
      feeMin,
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
    feeMin: Number(raw.tuitionFee) || 0,
  };
}

export interface MatchBreakdown {
  subject: number;
  location: number;
  salary: number;
  schedule: number;
  grade: number;
  /** Điểm % thực nhận của từng tiêu chí — cộng lại bằng score. */
  points: Record<keyof MatchWeights, number>;
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
 * S — Môn học: đếm số môn TRÙNG NHAU, rồi chia cho bên nào đòi nhiều môn hơn.
 *
 *   S = số môn trùng / max(số môn LỚP cần, số môn BẠN tìm)
 *
 * Chia cho bên lớn hơn nên thiếu ở phía nào cũng bị trừ như nhau: lớp cần hai môn mà bạn
 * chỉ nhận một thì mới lo được một nửa việc, và ngược lại bạn tìm hai môn mà lớp chỉ cần
 * một thì lớp đó cũng chỉ đáp ứng được một nửa ý muốn — cả hai đều 50%.
 *
 *   Lớp [Hóa, Sinh]      · tìm "sinh"          -> 1/2 = 0.50
 *   Lớp [Toán]           · tìm "toán anh"      -> 1/2 = 0.50
 *   Lớp [Toán, Anh]      · tìm "toán anh"      -> 2/2 = 1.00
 *   Lớp [Toán, Lý, Hóa]  · tìm "toán hóa"      -> 2/3 = 0.67
 *   Lớp [Toán, Lý]       · tìm "toán anh"      -> 1/2 = 0.50
 *   Không trùng môn nào                        -> 0.00
 */
function scoreSubject(pc: ParsedClass, c: TutorCriteria): number {
  const wantsOther = c.otherSubjectText.trim() !== '';
  const wantCount = c.subjectIds.length + (wantsOther ? 1 : 0);
  if (wantCount === 0) return 1; // không lọc theo môn -> bỏ qua tiêu chí

  const classCount = pc.subjectIds.length + (pc.hasOtherSubject ? 1 : 0);
  if (classCount === 0) return 0.5; // lớp không ghi môn -> trung tính

  let matched = pc.subjectIds.filter((id) => c.subjectIds.includes(id)).length;
  if (pc.hasOtherSubject && wantsOther && otherSubjectMatches(pc.subjectOther, c.otherSubjectText)) {
    matched += 1;
  }
  return matched / Math.max(classCount, wantCount);
}

/** Khối có số cao nhất trong danh mục. Khối phi số xếp ngay sau nó, ở bậc 13. */
const TOP_NUMERIC_GRADE = 12;
const NON_NUMERIC_GRADE_POS = TOP_NUMERIC_GRADE + 1;

/**
 * E — Khối lớp, chính là ô "Lớp" khách chọn khi đăng tin.
 *
 * Thang chạy NGƯỢC: khối bạn tìm được trọn điểm, khối THẤP HƠN càng xa càng nhiều điểm,
 * khối CAO HƠN thì bằng 0. Lý do: dạy được khối 10 thì dư sức kèm khối 1, còn khối 11–12
 * nằm ngoài khả năng đã khai.
 *
 * Xếp các khối thành một dãy bậc: Lớp 1..12 ở bậc 1..12, khối phi số (Luyện thi chứng chỉ,
 * Luyện thi Đại học) ở bậc 13. Gọi `pos` là bậc của khối BẠN TÌM:
 *
 *   E = (pos - bậc của tin) / pos        ·  trùng đúng khối -> 1  ·  tin ở bậc cao hơn -> 0
 *
 *   Tìm Lớp 10 (pos 10, mỗi bậc 10%):
 *     Lớp 10 -> 1.00  ·  Lớp 1 -> 9/10 = 0.90  ·  Lớp 7 -> 3/10 = 0.30  ·  Lớp 9 -> 0.10
 *     Lớp 11, Lớp 12, chứng chỉ, đại học -> 0
 *
 *   Tìm Luyện thi Đại học / chứng chỉ (pos 13, mỗi bậc 7,69%):
 *     đúng khối đó -> 1.00  ·  Lớp 8 -> (13 - 8)/13 = 0.3846  ·  Lớp 12 -> 1/13 = 0.0769
 *     khối phi số CÒN LẠI -> 0 (hai khối này không quy đổi được sang bậc số của nhau)
 */
function scoreGrade(pc: ParsedClass, c: TutorCriteria): number {
  if (c.gradeIds.length === 0) return 1; // gia sư không kén khối
  if (!pc.gradeId) return 0.5; // tin không ghi khối -> trung tính
  if (c.gradeIds.includes(pc.gradeId)) return 1; // trùng đúng khối -> trọn điểm

  const got = c.gradeLevels[pc.gradeId] ?? 0;
  if (got <= 0) return 0; // tin ở khối phi số mà không trùng -> 0

  const want = Math.max(0, ...c.gradeIds.map((id) => c.gradeLevels[id] ?? 0));
  const pos = want > 0 ? want : NON_NUMERIC_GRADE_POS;

  return Math.max(0, (pos - got) / pos);
}

/**
 * L — Địa điểm. TỈNH và XÃ/PHƯỜNG là HAI NỬA, mỗi nửa 50% — trừ khi bạn chỉ nêu tỉnh.
 *
 * Chỉ nêu TỈNH thì bỏ qua hẳn nửa xã: "Hà Nội" đã là một địa chỉ trọn vẹn, mọi tin ở
 * Hà Nội đều đáp ứng đủ -> 100%. Ngược lại chỉ nêu XÃ thì KHÔNG trọn vẹn, vì tên xã
 * trùng nhau giữa các tỉnh (Kim Liên có ở cả Hà Nội lẫn Nghệ An) nên chỉ 50%.
 *
 *   Tìm [Hà Nội]            · lớp bất kỳ ở Hà Nội        -> 1.00
 *   Tìm [Hà Nội]            · lớp ở Nghệ An              -> 0.00
 *   Tìm [chỉ Kim Liên]      · lớp Kim Liên, Hà Nội       -> 0.5 (chưa xác nhận được tỉnh)
 *   Tìm [chỉ Kim Liên]      · lớp Kim Liên, Nghệ An      -> 0.5
 *   Tìm [Hà Nội + Kim Liên] · lớp Kim Liên, Hà Nội       -> 0.5 + 0.5 = 1.00
 *   Tìm [Hà Nội + Kim Liên] · lớp Kim Liên, Nghệ An      -> 0   + 0.5 = 0.50
 *   Tìm [Hà Nội + Kim Liên] · lớp Thạch Thất, Hà Nội     -> 0.5 + 0   = 0.50
 *   Tìm [Hà Nội + Kim Liên] · lớp Nam Sơn, Hòa Bình      -> 0   + 0   = 0.00
 *
 * Không còn luật "đúng tỉnh rồi mới xét xã" — hai vế chấm độc lập.
 *
 * Riêng HAI HÌNH THỨC thì loại trừ nhau, không chia nửa:
 *   Chọn "Học online" · lớp online -> 1 · lớp có địa chỉ -> 0
 *   Nêu địa chỉ       · lớp online -> 0
 */
function locationHalf(want: string, got: string): number {
  if (!want) return 0; // gia sư chưa nêu vế này
  if (!got) return 0.5; // tin không ghi vế này -> trung tính
  return want === got ? 1 : 0;
}

function scoreLocation(pc: ParsedClass, c: TutorCriteria): number {
  const isOnline = pc.lessonMode === 'ONLINE';

  // Online và học tại địa chỉ loại trừ nhau: chọn online thì lớp có địa chỉ = 0, và
  // ngược lại đã nêu địa chỉ thì lớp online = 0 (đi dạy tận nơi mới là thứ đang tìm).
  if (c.onlineOnly) return isOnline ? 1 : 0;

  const wantProvince = provinceKey(c.provinceName);
  const wantWard = wardKey(c.wardName);
  if (!wantProvince && !wantWard) return 1; // không kén nơi dạy -> bỏ qua tiêu chí
  if (isOnline) return 0; // đã nêu địa chỉ cụ thể -> lớp online không đáp ứng

  const provinceScore = locationHalf(wantProvince, provinceKey(pc.provinceName));
  // Chỉ nêu tỉnh -> tỉnh ăn trọn tiêu chí, khỏi xét xã.
  if (!wantWard) return provinceScore;

  // Dữ liệu cũ lưu quận/huyện ở districtName, dữ liệu mới lưu phường/xã ở wardName.
  const wardScore = locationHalf(wantWard, wardKey(pc.wardName || pc.districtName));
  return 0.5 * provinceScore + 0.5 * wardScore;
}

/**
 * Mốc sàn học phí của mọi tin tuyển: dưới mức này thì coi như không trả gì.
 * Trùng với mức tối thiểu khi gia sư báo giá lúc ứng tuyển.
 */
export const FEE_FLOOR = 50000;

/**
 * P — Học phí. Thang chạy từ MỐC SÀN 50k (= 0 điểm) tới mức bạn mong muốn (= trọn điểm),
 * chứ không chia thẳng phí lớp cho mức mong muốn — chia thẳng thì tin 50k đã được 1/7
 * số điểm dù thực chất nó là mức thấp nhất sàn cho phép.
 *
 *   Mong muốn 350k · lớp trả 400k -> 1.00 (trả bằng hoặc hơn là trọn điểm)
 *   Mong muốn 350k · lớp trả 350k -> 1.00
 *   Mong muốn 350k · lớp trả 280k -> (280 - 50) / (350 - 50) = 0.77
 *   Mong muốn 350k · lớp trả 200k -> (200 - 50) / (350 - 50) = 0.50
 *   Mong muốn 350k · lớp trả  50k -> 0.00
 */
function scoreSalary(pc: ParsedClass, c: TutorCriteria): number {
  const expected = Number(c.expectedFee) || 0;
  if (expected <= 0) return 1; // không đặt kỳ vọng -> bỏ qua tiêu chí
  if (pc.feePerHour <= 0) return 0.5; // tin không ghi học phí -> trung tính
  if (pc.feePerHour >= expected) return 1;
  if (expected <= FEE_FLOOR) return 1; // mong muốn nằm dưới sàn -> tin nào cũng đạt
  return Math.max(0, (pc.feePerHour - FEE_FLOOR) / (expected - FEE_FLOOR));
}

/**
 * T — Lịch học. THỨ và BUỔI là hai vế chấm riêng, mỗi vế lấy TỈ LỆ số buổi của lớp
 * rơi đúng vào thứ (hoặc buổi) bạn nêu ra.
 *
 *   Chỉ nêu thứ  -> chỉ chấm vế thứ
 *   Chỉ nêu buổi -> chỉ chấm vế buổi
 *   Nêu cả hai   -> 0.5 × vế thứ + 0.5 × vế buổi
 *
 * Lớp có 3 buổi: T2 Sáng · T3 Tối · T5 Chiều
 *   Tìm "thứ 2"           -> vế thứ 1/3                          = 0.33
 *   Tìm "buổi tối"        -> vế buổi 1/3                         = 0.33
 *   Tìm "thứ 3 buổi sáng" -> 0.5 × 1/3 (T3) + 0.5 × 1/3 (Sáng) = 0.33
 *   Tìm "thứ 2 thứ 3"      -> vế thứ 2/3                          = 0.67
 *
 * Trùng thì nhân lên: lớp T3 Sáng · T3 Chiều · T5 Tối, tìm "thứ 3" -> 2/3.
 *
 * Các khung gia sư nêu được GỘP thành một tập thứ và một tập buổi, không so theo
 * từng cặp: nêu "sáng T3" và "tối T5" thì buổi "tối T3" của lớp vẫn khớp cả hai vế.
 * Lịch CUSTOM ghi theo ngày nên phải suy ra thứ trước khi so.
 */
interface ScheduleHalves {
  /** Tỉ lệ buổi của lớp rơi đúng thứ gia sư nêu; -1 khi gia sư không nêu thứ nào. */
  day: number;
  /** Tương tự cho buổi (Sáng/Chiều/Tối). */
  session: number;
}

/** Tính riêng tỉ lệ khớp của từng vế. Dùng cho cả chấm điểm lẫn xếp hạng. */
export function scheduleHalves(pc: ParsedClass, c: TutorCriteria): ScheduleHalves {
  const wantDays = new Set(c.availability.map((p) => p.day).filter(Boolean));
  const wantSessions = new Set(c.availability.map((p) => p.session).filter(Boolean));
  const slots = pc.slots.map((s) => ({
    day: pc.scheduleMode === 'CUSTOM' ? weekdayCode(s.date) : s.day,
    session: s.session,
  }));

  /** Tỉ lệ buổi của lớp rơi vào tập gia sư nêu, tính trên những buổi có ghi vế đó. */
  const ratio = (want: Set<string>, pick: (s: { day: string; session: string }) => string): number => {
    if (want.size === 0) return -1; // gia sư không nêu vế này
    const known = slots.filter((s) => pick(s) !== '');
    if (known.length === 0) return 0.5; // tin chưa ghi vế này -> trung tính
    return known.filter((s) => want.has(pick(s))).length / known.length;
  };

  return { day: ratio(wantDays, (s) => s.day), session: ratio(wantSessions, (s) => s.session) };
}

function scoreSchedule(pc: ParsedClass, c: TutorCriteria): number {
  if (c.availability.length === 0) return 1; // gia sư linh hoạt -> bỏ qua tiêu chí
  const { day, session } = scheduleHalves(pc, c);
  if (day < 0 && session < 0) return 1;
  if (session < 0) return day;
  if (day < 0) return session;
  return 0.5 * day + 0.5 * session;
}

/**
 * Tiêu chí nào gia sư đã nêu ra. Tiêu chí để trống vẫn được trọn phần điểm của nó
 * (không kén thì lớp nào cũng đạt), nhưng mức ưu tiên của nó bị bỏ qua — không nêu gì
 * thì không có gì để ưu tiên, mà kéo thanh trượt của nó lại thấy % nhảy thì người dùng
 * tưởng hệ thống chấm sai.
 */
function activeCriteria(c: TutorCriteria): Record<keyof MatchWeights, boolean> {
  return {
    subject: c.subjectIds.length > 0 || c.otherSubjectText.trim() !== '',
    location: c.provinceName.trim() !== '' || c.wardName.trim() !== '' || c.onlineOnly,
    salary: c.expectedFee.trim() !== '',
    schedule: c.availability.length > 0,
    grade: c.gradeIds.length > 0,
  };
}

export const CRITERIA_KEYS = ['subject', 'location', 'salary', 'schedule', 'grade'] as const;

export interface CriterionShare {
  /** Gia sư có nêu tiêu chí này không. */
  filled: boolean;
  /** Phần trăm tiêu chí này được chia trong 100%. */
  share: number;
  /** Trần điểm thực tế sau khi nhân mức ưu tiên. */
  cap: number;
}

/**
 * Chia 100% cho 5 tiêu chí — mặc định mỗi tiêu chí 20%.
 *
 *  · Kéo một thanh về 0 = bỏ hẳn tiêu chí đó, 20% của nó chia đều cho các thanh còn lại
 *    (bỏ 1 thanh -> 4 thanh còn lại mỗi thanh 25%).
 *  · Mức ưu tiên 1..5 quyết định lấy bao nhiêu trong phần đó: mức/5.
 *    Phần 20% ở mức 1 -> trần 4% · mức 3 -> 12% · mức 5 -> 20%.
 *  · Tiêu chí để trống giữ nguyên trọn phần (xem activeCriteria).
 */
export function criteriaShares(c: TutorCriteria): Record<keyof MatchWeights, CriterionShare> {
  const filled = activeCriteria(c);
  const kept = CRITERIA_KEYS.filter((k) => c.weights[k] > 0);
  const share = kept.length > 0 ? 100 / kept.length : 0;

  const out = {} as Record<keyof MatchWeights, CriterionShare>;
  for (const k of CRITERIA_KEYS) {
    const s = c.weights[k] > 0 ? share : 0;
    out[k] = {
      filled: filled[k],
      share: s,
      cap: filled[k] ? (s * c.weights[k]) / 5 : s,
    };
  }
  return out;
}

export function scoreClass(pc: ParsedClass, c: TutorCriteria): MatchBreakdown {
  const values: Record<keyof MatchWeights, number> = {
    subject: scoreSubject(pc, c),
    location: scoreLocation(pc, c),
    salary: scoreSalary(pc, c),
    schedule: scoreSchedule(pc, c),
    grade: scoreGrade(pc, c),
  };

  const shares = criteriaShares(c);
  const points = {} as Record<keyof MatchWeights, number>;
  let score = 0;
  for (const k of CRITERIA_KEYS) {
    points[k] = shares[k].cap * values[k];
    score += points[k];
  }

  // Kéo cả 5 thanh về 0 = không xếp hạng theo tiêu chí nào -> mọi lớp ngang nhau.
  const allOff = CRITERIA_KEYS.every((k) => c.weights[k] <= 0);

  return { ...values, points, score: allOff ? 100 : score };
}

/**
 * Làm tròn % phù hợp: đúng .5 thì XUỐNG, từ .6 mới lên (83,5% -> 83% · 83,6% -> 84%).
 * Math.round đẩy .5 lên nên lệch một điểm so với cách tính tay của nghiệp vụ.
 */
export const roundScore = (score: number): number => Math.ceil(score - 0.5);

/**
 * Độ "trùng nhiều" của một tin, dùng khi hai tin bằng điểm:
 *   exact = số tiêu chí khớp TRỌN (100%) · total = tổng mức khớp thô của cả 5 tiêu chí
 * Chỉ tính những thanh trượt còn bật — kéo về 0 là bỏ hẳn tiêu chí thì nó cũng không được
 * quyền xếp chỗ. Hai tin cùng 90% nhưng một tin đúng trọn 3 tiêu chí còn tin kia chỉ đúng
 * một nửa ở bốn chỗ -> tin đúng trọn nhiều hơn lên trước.
 */
function matchDepth(b: MatchBreakdown, c: TutorCriteria): { exact: number; total: number } {
  let exact = 0;
  let total = 0;
  for (const k of CRITERIA_KEYS) {
    if (c.weights[k] <= 0) continue;
    if (b[k] >= 0.999) exact += 1;
    total += b[k];
  }
  return { exact, total };
}

/** Tin có đúng phường/xã gia sư đã nêu. Dùng để xếp hạng, không cộng thêm điểm. */
function matchesWantedWard(pc: ParsedClass, c: TutorCriteria): boolean {
  const want = wardKey(c.wardName);
  if (!want) return false;
  const got = wardKey(pc.wardName || pc.districtName);
  return got !== '' && got === want;
}

/**
 * Xếp hạng, xét lần lượt:
 *   1. % phù hợp cao hơn
 *   2. TRÙNG NHIỀU HƠN — nhiều tiêu chí đúng trọn hơn, rồi tổng mức khớp cao hơn
 *   3. đúng PHƯỜNG/XÃ gia sư nêu
 *   4. đúng VẾ LỊCH ĐƯỢC NÊU TRƯỚC
 *   5. tin mới hơn
 *
 * Nêu cả thứ lẫn buổi thì hai vế mỗi vế 50%, nên tin trùng thứ và tin trùng buổi bằng điểm
 * nhau. Gõ "t3 chiều" là thứ đứng trước -> các tin có buổi vào T3 lên trước, rồi mới tới tin
 * chỉ trùng buổi chiều. Gõ "chiều t3" thì ngược lại. Bằng thứ tự, không cộng thêm điểm.
 *
 * Đúng tỉnh đã ăn trọn 100% điểm địa điểm nên tin cùng phường và tin khác phường trong
 * cùng tỉnh bằng điểm nhau. Đã nêu địa chỉ tới tận xã thì tin ngay tại xã đó phải nằm
 * trên — bằng thứ tự, không phải bằng cách cộng thêm điểm.
 */
export function rankClasses(classes: ClassResponse[], c: TutorCriteria): MatchResult[] {
  return classes
    .map((raw) => {
      const parsed = parseClass(raw);
      return { parsed, breakdown: scoreClass(parsed, c) };
    })
    .sort((a, b) => {
      if (b.breakdown.score !== a.breakdown.score) return b.breakdown.score - a.breakdown.score;

      const ad = matchDepth(a.breakdown, c);
      const bd = matchDepth(b.breakdown, c);
      if (ad.exact !== bd.exact) return bd.exact - ad.exact;
      if (Math.abs(ad.total - bd.total) > 1e-9) return bd.total - ad.total;

      const aWard = matchesWantedWard(a.parsed, c) ? 1 : 0;
      const bWard = matchesWantedWard(b.parsed, c) ? 1 : 0;
      if (aWard !== bWard) return bWard - aWard;

      if (c.availability.length > 0) {
        const ah = scheduleHalves(a.parsed, c);
        const bh = scheduleHalves(b.parsed, c);
        const order: (keyof ScheduleHalves)[] =
          c.scheduleLead === 'session' ? ['session', 'day'] : ['day', 'session'];
        for (const half of order) {
          if (ah[half] !== bh[half]) return bh[half] - ah[half];
        }
      }

      return (b.parsed.raw.createdAt ?? '').localeCompare(a.parsed.raw.createdAt ?? '');
    });
}

/**
 * Xếp hạng + lọc cứng cho mọi màn duyệt lớp.
 *
 * Chỉ MÔN HỌC lọc cứng: đang tìm gia sư dạy Toán mà hiện lớp Hoá thì vô nghĩa. Bốn tiêu
 * chí còn lại chỉ chấm điểm rồi tự tụt hạng — tìm Lớp 10 thì tin Lớp 11/12 vẫn hiện
 * nhưng ăn 0 điểm khối lớp, để còn đối chiếu được con số trên thẻ.
 */
export function searchClasses(classes: ClassResponse[], c: TutorCriteria): MatchResult[] {
  const out = rankClasses(classes, c);
  if (c.subjectIds.length > 0) {
    const wanted = new Set(c.subjectIds);
    return out.filter((r) => r.parsed.subjectIds.some((id) => wanted.has(id)));
  }
  if (c.otherSubjectText.trim()) return out.filter((r) => r.parsed.hasOtherSubject);
  return out;
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
