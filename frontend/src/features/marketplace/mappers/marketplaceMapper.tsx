import {
  BILLING_CYCLE_OPTIONS,
  DAY_OF_WEEK_OPTIONS,
  LEARNING_GOAL_OPTIONS,
  LEARNING_GOAL_OTHER,
  OTHER_SUBJECT,
  TUTOR_REQUIREMENT_OPTIONS,
  type CatalogOption,
  type ClassFormValues,
  type ClassRequestPayload,
  type ClassResponse,
} from '../types/marketplaceTypes';

/** Giá trị mặc định cho form tạo mới. */
export function emptyForm(): ClassFormValues {
  return {
    subjectIds: [],
    subjectOther: '',
    gradeId: '',
    learningGoal: '',
    learningGoalOther: '',
    tutorRequirement: TUTOR_REQUIREMENT_OPTIONS[0],
    tutorRequirementDetail: '',
    lessonMode: 'OFFLINE',
    provinceId: '',
    provinceName: '',
    districtId: '',
    districtName: '',
    wardId: '',
    wardName: '',
    address: '',
    subjectFees: {},
    billingCycle: 'MONTH',
    months: '1',
    scheduleMode: 'WEEKLY',
    repeatEveryWeeks: '1',
    studyWeeks: [1],
    slots: [],
    note: '',
  };
}

/** Snapshot form đã lưu, có thể là bản cũ chưa có studyWeeks. */
type LegacyForm = Partial<ClassFormValues> & { weeksOnPerCycle?: string };

/** Suy ra danh sách tuần học cho snapshot cũ:
 *  - đã có studyWeeks → dùng luôn;
 *  - chỉ có weeksOnPerCycle (học K tuần ĐẦU) → [1..K];
 *  - không có gì → [1] (học tuần đầu mỗi chu kỳ, đúng hành vi trước đây). */
function migrateStudyWeeks(parsed: LegacyForm): number[] {
  if (Array.isArray(parsed.studyWeeks) && parsed.studyWeeks.length > 0) return parsed.studyWeeks;
  const k = Math.trunc(Number(parsed.weeksOnPerCycle));
  if (Number.isInteger(k) && k >= 1) return Array.from({ length: k }, (_, i) => i + 1);
  return [1];
}

/** Nạp dữ liệu lớp đã có vào form khi sửa. */
export function classToForm(c: ClassResponse): ClassFormValues {
  // Ưu tiên nạp lại nguyên trạng form từ JSON snapshot (khôi phục 100%).
  if (c.detailsJson) {
    try {
      const parsed = JSON.parse(c.detailsJson) as LegacyForm;
      return { ...emptyForm(), ...parsed, studyWeeks: migrateStudyWeeks(parsed) };
    } catch {
      // JSON hỏng → rơi xuống nạp theo cột bên dưới.
    }
  }
  const goalMatched =
    !!c.learningGoal && LEARNING_GOAL_OPTIONS.includes(c.learningGoal);
  const reqMatched =
    !!c.tutorRequirement && TUTOR_REQUIREMENT_OPTIONS.includes(c.tutorRequirement);
  const fee = c.tuitionFee != null ? String(c.tuitionFee) : '';
  const subjIds = c.subjectId != null ? [String(c.subjectId)] : [];
  return {
    subjectIds: subjIds,
    subjectOther: '',
    gradeId: c.gradeId != null ? String(c.gradeId) : '',
    learningGoal: c.learningGoal ? (goalMatched ? c.learningGoal : LEARNING_GOAL_OTHER) : '',
    learningGoalOther: c.learningGoal && !goalMatched ? c.learningGoal : '',
    tutorRequirement: reqMatched ? (c.tutorRequirement as string) : TUTOR_REQUIREMENT_OPTIONS[0],
    tutorRequirementDetail: !reqMatched && c.tutorRequirement ? c.tutorRequirement : '',
    lessonMode: c.lessonMode,
    provinceId: '',
    provinceName: '',
    districtId: '',
    districtName: '',
    wardId: '',
    wardName: '',
    address: c.address ?? '',
    subjectFees: subjIds[0] ? { [subjIds[0]]: fee } : {},
    billingCycle: 'MONTH',
    months: '1',
    scheduleMode: 'WEEKLY',
    repeatEveryWeeks: '1',
    studyWeeks: [1],
    slots: [],
    note: c.description ?? '',
  };
}

/** Thời lượng (giờ) của một buổi từ start/end "HH:MM". */
function slotHours(start: string, end: string): number {
  if (!start || !end) return 0;
  const [sh, sm] = start.split(':').map(Number);
  const [eh, em] = end.split(':').map(Number);
  const diff = eh * 60 + em - (sh * 60 + sm);
  return diff > 0 ? diff / 60 : 0;
}

/** Ghép mục tiêu học tập cuối cùng (chọn sẵn hoặc tự nhập). */
export function resolveLearningGoal(form: ClassFormValues): string {
  if (form.learningGoal === LEARNING_GOAL_OTHER) {
    return form.learningGoalOther.trim();
  }
  return form.learningGoal.trim();
}

/** Ghép yêu cầu gia sư (đáp án + chi tiết bổ sung). */
export function resolveTutorRequirement(form: ClassFormValues): string {
  const detail = form.tutorRequirementDetail.trim();
  if (detail) {
    return `${form.tutorRequirement} — ${detail}`;
  }
  return form.tutorRequirement;
}

/** Số tuần ước tính theo chu kỳ học phí (học theo Tháng = số tháng × 4 tuần). */
export function weeksForCycle(form: ClassFormValues): number {
  if (form.billingCycle === 'MONTH') {
    return Math.max(1, Number(form.months) || 1) * 4;
  }
  return BILLING_CYCLE_OPTIONS.find((o) => o.value === form.billingCycle)?.weeks ?? 4;
}

/** Nhãn chu kỳ để hiển thị/mô tả (học theo Tháng thì kèm số tháng). */
export function cycleLabelOf(form: ClassFormValues): string {
  if (form.billingCycle === 'MONTH') {
    return `${Math.max(1, Number(form.months) || 1)} tháng`;
  }
  return BILLING_CYCLE_OPTIONS.find((o) => o.value === form.billingCycle)?.label ?? '1 tháng';
}

/** Độ dài chu kỳ lặp, tính bằng tuần (1–4). Lịch CUSTOM đã có ngày cụ thể nên luôn là 1. */
export function repeatWeeksOf(form: ClassFormValues): number {
  if (form.scheduleMode !== 'WEEKLY') return 1;
  const n = Number(form.repeatEveryWeeks) || 1;
  return Math.min(4, Math.max(1, Math.trunc(n)));
}

/** Những tuần HỌC trong mỗi chu kỳ, đã khử trùng/sắp tăng và bỏ tuần ngoài 1..N.
 *  Rỗng hoặc hỏng → [1]; chu kỳ 1 tuần thì luôn là [1]. */
export function studyWeeksOf(form: ClassFormValues): number[] {
  const n = repeatWeeksOf(form);
  if (n <= 1) return [1];
  const raw = Array.isArray(form.studyWeeks) ? form.studyWeeks : [];
  const out = [...new Set(raw.map(Number))]
    .filter((w) => Number.isInteger(w) && w >= 1 && w <= n)
    .sort((a, b) => a - b);
  return out.length > 0 ? out : [1];
}

/** Những tuần NGHỈ trong mỗi chu kỳ (phần bù của studyWeeksOf trong 1..N). */
export function restWeeksOf(form: ClassFormValues): number[] {
  const on = new Set(studyWeeksOf(form));
  return Array.from({ length: repeatWeeksOf(form) }, (_, i) => i + 1).filter((w) => !on.has(w));
}

/** Tổng số TUẦN HỌC trong cả chu kỳ học phí: mỗi chu kỳ N tuần lặp đủ đóng góp |on| tuần học;
 *  đoạn lẻ cuối chỉ đếm những tuần học rơi vào đó. studyWeeks=[1] → đúng bằng ceil(W / N) như trước. */
export function patternRepeats(form: ClassFormValues): number {
  const weeks = weeksForCycle(form);
  const n = repeatWeeksOf(form);
  if (n <= 1) return weeks;
  const on = studyWeeksOf(form);
  const remainder = weeks % n;
  return Math.floor(weeks / n) * on.length + on.filter((w) => w <= remainder).length;
}

/** Nhãn tần suất lặp: "hàng tuần" / "mỗi 3 tuần" / "học tuần 1, 3 trong mỗi 4 tuần". */
export function repeatLabel(form: ClassFormValues): string {
  const n = repeatWeeksOf(form);
  const on = studyWeeksOf(form);
  if (n === 1 || on.length >= n) return 'hàng tuần';
  if (on.length === 1 && on[0] === 1) return `mỗi ${n} tuần`;
  return `học tuần ${on.join(', ')} trong mỗi ${n} tuần`;
}

/** Tổng số buổi ước tính = số buổi mỗi lần lặp (số slot) × số lần lặp trong chu kỳ. */
export function estimatedSessions(form: ClassFormValues): number {
  const perRepeat = Math.max(1, form.slots.length);
  return perRepeat * patternRepeats(form);
}

/** Tổng số giờ học mỗi lần lặp = cộng thời lượng của tất cả buổi trong lịch. */
export function totalHoursPerRepeat(form: ClassFormValues): number {
  return form.slots.reduce((sum, s) => sum + slotHours(s.start, s.end), 0);
}

/** Số giờ mỗi lần lặp của một môn (cộng thời lượng các buổi của môn đó). */
export function hoursPerRepeatForSubject(form: ClassFormValues, subjectId: string): number {
  return form.slots
    .filter((s) => s.subjectId === subjectId)
    .reduce((sum, s) => sum + slotHours(s.start, s.end), 0);
}

/** Tổng học phí = Σ (học phí/giờ mỗi môn × giờ mỗi lần lặp × số lần lặp trong chu kỳ). */
export function totalBudget(form: ClassFormValues): number {
  const repeats = patternRepeats(form);
  return Math.round(
    form.subjectIds.reduce(
      (sum, sid) =>
        sum + (Number(form.subjectFees[sid]) || 0) * hoursPerRepeatForSubject(form, sid) * repeats,
      0,
    ),
  );
}

/** Tên thứ (Vi) của một ngày YYYY-MM-DD. */
export function weekdayVi(dateStr: string): string {
  if (!dateStr) return '';
  const d = new Date(`${dateStr}T00:00:00`);
  return ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'][d.getDay()] ?? '';
}

/** Câu mô tả lịch học để lưu vào phần mô tả (nhóm theo môn, theo ngày). */
export function buildScheduleSummary(form: ClassFormValues, subjects: CatalogOption[] = []): string {
  // Nêu nhịp lặp một lần ở đầu câu, không lặp lại ở từng buổi cho đỡ rối.
  const parts = [
    `Lịch học ${cycleLabelOf(form).toLowerCase()}${
      form.scheduleMode === 'WEEKLY' ? ` — ${repeatLabel(form)}` : ''
    } (${form.slots.length} buổi/tuần)`,
  ];
  const money = new Intl.NumberFormat('vi-VN');
  const nameOf = (id: string) =>
    id === OTHER_SUBJECT
      ? form.subjectOther.trim() || 'Môn học khác'
      : (subjects.find((s) => String(s.id) === id)?.name ?? '');
  const dayLabel = (v: string) => DAY_OF_WEEK_OPTIONS.find((d) => d.value === v)?.label ?? v;
  const whenOf = (s: ClassFormValues['slots'][number]) =>
    form.scheduleMode === 'WEEKLY' ? dayLabel(s.day) : `${weekdayVi(s.date)} ${s.date}`;
  // Nhóm slot theo môn (kèm học phí/giờ của môn đó).
  const bySubject = form.subjectIds
    .map((sid) => {
      const rows = form.slots
        .filter((s) => s.subjectId === sid)
        .map((s) => `${whenOf(s)} ${s.session} (${s.start}–${s.end})`);
      if (!rows.length) return '';
      const fee = Number(form.subjectFees[sid]) || 0;
      const feeStr = fee > 0 ? ` [${money.format(fee)}đ/giờ]` : '';
      return `${nameOf(sid)}${feeStr}: ${rows.join(', ')}`;
    })
    .filter(Boolean);
  if (bySubject.length > 0) {
    parts.push(bySubject.join('; '));
  }
  return `${parts.join('. ')}.`;
}

/** Chuyển form → payload gửi backend.
 *  `subjects` (catalog) để lấy tên môn ghi vào mô tả khi chọn nhiều môn. */
export function formToPayload(
  form: ClassFormValues,
  subjects: CatalogOption[] = [],
): ClassRequestPayload {
  const sessions = estimatedSessions(form);
  // Tổng học phí = cộng theo từng môn (học phí/giờ riêng × giờ/tuần môn đó × số tuần).
  const budget = totalBudget(form);
  // Học phí/giờ đại diện gửi backend = của môn chính đầu tiên.
  const primaryFee = Number(form.subjectFees[form.subjectIds[0]]) || 0;
  // Ngày bắt đầu/kết thúc: CUSTOM suy từ các ngày đã đặt; WEEKLY = hôm nay → +số tuần.
  const today = new Date().toISOString().slice(0, 10);
  let startDate: string;
  let endDate: string;
  if (form.scheduleMode === 'CUSTOM') {
    const dates = form.slots.map((s) => s.date).filter(Boolean).sort();
    startDate = dates[0] ?? today;
    endDate = dates[dates.length - 1] ?? startDate;
  } else {
    startDate = today;
    const d = new Date(`${today}T00:00:00`);
    d.setDate(d.getDate() + weeksForCycle(form) * 7);
    endDate = d.toISOString().slice(0, 10);
  }
  // Backend chỉ lưu 1 môn (subjectId) → môn đầu là môn chính; nếu chọn nhiều
  // môn thì ghi cả danh sách vào mô tả.
  const subjectNames = form.subjectIds
    .map((id) =>
      id === OTHER_SUBJECT
        ? form.subjectOther.trim() || 'Môn học khác'
        : subjects.find((s) => String(s.id) === id)?.name,
    )
    .filter((n): n is string => !!n);
  const subjectLine = subjectNames.length > 0 ? `Môn học: ${subjectNames.join(', ')}` : '';
  // Môn chính gửi backend = môn thật đầu tiên (bỏ qua "Khác"); null nếu chỉ có "Khác".
  const primarySubjectId = form.subjectIds.find((id) => id !== OTHER_SUBJECT);
  const description = [subjectLine, buildScheduleSummary(form, subjects), form.note.trim()]
    .filter(Boolean)
    .join('\n');
  // Backend chưa có cột riêng cho tỉnh/khu vực → gộp thành một chuỗi địa chỉ đầy đủ:
  // [số nhà/đường], [phường/xã], [quận/huyện], [tỉnh/thành].
  const fullAddress = [
    form.address.trim(),
    form.wardName.trim(),
    form.districtName.trim(),
    form.provinceName.trim(),
  ]
    .filter(Boolean)
    .join(', ');

  return {
    subjectId: primarySubjectId ? Number(primarySubjectId) : null,
    gradeId: form.gradeId ? Number(form.gradeId) : null,
    learningGoal: resolveLearningGoal(form) || null,
    tutorRequirement: resolveTutorRequirement(form) || null,
    locationId: null,
    address: fullAddress || null,
    lessonMode: form.lessonMode,
    numberOfSessions: sessions,
    startDate,
    endDate,
    tuitionFee: primaryFee,
    budget,
    recurringType: 'WEEKLY',
    description: description || undefined,
    // Lưu nguyên trạng form để nạp lại đầy đủ khi Sửa.
    detailsJson: JSON.stringify(form),
  };
}
