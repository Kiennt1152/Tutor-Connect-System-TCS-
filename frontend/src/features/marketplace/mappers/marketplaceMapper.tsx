import {
  BILLING_CYCLE_OPTIONS,
  DAY_OF_WEEK_OPTIONS,
  LEARNING_GOAL_OPTIONS,
  LEARNING_GOAL_OTHER,
  OTHER_SUBJECT,
  isOtherSubject,
  TUTOR_REQUIREMENT_OPTIONS,
  type CatalogOption,
  type ClassFormValues,
  type ClassRequestPayload,
  type ClassResponse,
} from '../types/marketplaceTypes';

/** Giá trị mặc định của form tạo tin tìm gia sư (chưa chọn gì, lịch hàng tuần, 1 tháng). */
export function emptyForm(): ClassFormValues {
  return {
    subjectIds: [],
    subjectOthers: {},
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
    durationUnit: 'MONTH',
    scheduleMode: 'WEEKLY',
    repeatEveryWeeks: '1',
    studyWeeks: [1],
    slots: [],
    note: '',
  };
}

type LegacyForm = Partial<ClassFormValues> & {
  weeksOnPerCycle?: string;
  subjectOther?: string;
};

/** Đọc tên các môn "khác" từ detailsJson; hỗ trợ cả định dạng cũ chỉ có một môn khác. */
function migrateOtherSubjects(parsed: LegacyForm): Record<string, string> {
  if (parsed.subjectOthers && typeof parsed.subjectOthers === 'object') return parsed.subjectOthers;
  if (parsed.subjectOther && (parsed.subjectIds ?? []).includes(OTHER_SUBJECT)) {
    return { [OTHER_SUBJECT]: parsed.subjectOther };
  }
  return {};
}

/** Đọc các tuần học trong chu kỳ từ detailsJson; dữ liệu cũ (weeksOnPerCycle = k) đổi thành tuần 1..k. */
function migrateStudyWeeks(parsed: LegacyForm): number[] {
  if (Array.isArray(parsed.studyWeeks) && parsed.studyWeeks.length > 0) return parsed.studyWeeks;
  const k = Math.trunc(Number(parsed.weeksOnPerCycle));
  if (Number.isInteger(k) && k >= 1) return Array.from({ length: k }, (_, i) => i + 1);
  return [1];
}

/** Đọc kỳ thanh toán/thời lượng từ detailsJson; dữ liệu cũ "YEAR" đổi thành 1 năm theo tháng. */
function migrateDuration(parsed: LegacyForm): Pick<ClassFormValues, 'billingCycle' | 'months' | 'durationUnit'> {
  const cycle = parsed.billingCycle;
  if (cycle === 'YEAR') {
    return { billingCycle: 'MONTH', months: '1', durationUnit: 'YEAR' };
  }
  if (cycle === 'TERM' || cycle === 'QUARTER') {
    return { billingCycle: cycle, months: parsed.months ?? '1', durationUnit: 'MONTH' };
  }
  return {
    billingCycle: 'MONTH',
    months: parsed.months ?? '1',
    durationUnit: parsed.durationUnit ?? 'MONTH',
  };
}

/**
 * Đổi lớp từ API thành giá trị form để sửa: đọc detailsJson (có chuyển định dạng cũ),
 * không có detailsJson thì dựng form từ các cột cơ bản của lớp.
 */
export function classToForm(c: ClassResponse): ClassFormValues {
  if (c.detailsJson) {
    try {
      const parsed = JSON.parse(c.detailsJson) as LegacyForm;
      return {
        ...emptyForm(),
        ...parsed,
        subjectOthers: migrateOtherSubjects(parsed),
        studyWeeks: migrateStudyWeeks(parsed),
        ...migrateDuration(parsed),
      };
    } catch {
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
    subjectOthers: {},
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
    durationUnit: 'MONTH',
    scheduleMode: 'WEEKLY',
    repeatEveryWeeks: '1',
    studyWeeks: [1],
    slots: [],
    note: c.description ?? '',
  };
}

/** Số giờ của một khung học (00:00 ở giờ kết thúc hiểu là nửa đêm); dữ liệu sai thì 0. */
function slotHours(start: string, end: string): number {
  if (!start || !end) return 0;
  const [sh, sm] = start.split(':').map(Number);
  const [eh, em] = end.split(':').map(Number);
  // '00:00' ở giờ kết thúc là nửa đêm (24:00), không phải 0h đầu ngày.
  const endMin = end === '00:00' ? 24 * 60 : eh * 60 + em;
  const diff = endMin - (sh * 60 + sm);
  return diff > 0 ? diff / 60 : 0;
}

/** Mục tiêu học gửi lên server: lấy nội dung tự gõ nếu chọn "Khác". */
export function resolveLearningGoal(form: ClassFormValues): string {
  if (form.learningGoal === LEARNING_GOAL_OTHER) {
    return form.learningGoalOther.trim();
  }
  return form.learningGoal.trim();
}

/** Yêu cầu gia sư gửi lên server: ghép lựa chọn với phần mô tả chi tiết (nếu có). */
export function resolveTutorRequirement(form: ClassFormValues): string {
  const detail = form.tutorRequirementDetail.trim();
  if (detail) {
    return `${form.tutorRequirement} — ${detail}`;
  }
  return form.tutorRequirement;
}

/** Số tháng/năm người dùng chọn (tối thiểu 1). */
export function durationCountOf(form: ClassFormValues): number {
  return Math.max(1, Number(form.months) || 1);
}

/** Tổng số tháng của khoá học (năm đổi ra tháng). */
export function totalMonthsOf(form: ClassFormValues): number {
  const n = durationCountOf(form);
  return form.durationUnit === 'YEAR' ? n * 12 : n;
}

/** Số tuần học của khoá theo kỳ thanh toán (tháng × 4; học kỳ/quý lấy từ cấu hình). */
export function weeksForCycle(form: ClassFormValues): number {
  if (form.billingCycle === 'MONTH') {
    return totalMonthsOf(form) * 4;
  }
  return BILLING_CYCLE_OPTIONS.find((o) => o.value === form.billingCycle)?.weeks ?? 4;
}

/** Nhãn thời lượng hiển thị, ví dụ "3 tháng", "1 năm", "Học kỳ". */
export function cycleLabelOf(form: ClassFormValues): string {
  if (form.billingCycle === 'MONTH') {
    const n = durationCountOf(form);
    return form.durationUnit === 'YEAR' ? `${n} năm` : `${n} tháng`;
  }
  return BILLING_CYCLE_OPTIONS.find((o) => o.value === form.billingCycle)?.label ?? '1 tháng';
}

/** Chu kỳ lặp của lịch hàng tuần (1–4 tuần); lịch theo ngày luôn là 1. */
export function repeatWeeksOf(form: ClassFormValues): number {
  if (form.scheduleMode !== 'WEEKLY') return 1;
  const n = Number(form.repeatEveryWeeks) || 1;
  return Math.min(4, Math.max(1, Math.trunc(n)));
}

/** Các tuần có học trong chu kỳ lặp (đã lọc hợp lệ, sắp xếp; mặc định tuần 1). */
export function studyWeeksOf(form: ClassFormValues): number[] {
  const n = repeatWeeksOf(form);
  if (n <= 1) return [1];
  const raw = Array.isArray(form.studyWeeks) ? form.studyWeeks : [];
  const out = [...new Set(raw.map(Number))]
    .filter((w) => Number.isInteger(w) && w >= 1 && w <= n)
    .sort((a, b) => a - b);
  return out.length > 0 ? out : [1];
}

/** Các tuần nghỉ trong chu kỳ lặp. */
export function restWeeksOf(form: ClassFormValues): number[] {
  const on = new Set(studyWeeksOf(form));
  return Array.from({ length: repeatWeeksOf(form) }, (_, i) => i + 1).filter((w) => !on.has(w));
}

/** Số lần mẫu lịch được lặp trong cả khoá (lịch theo ngày chỉ tính 1 lần). */
export function patternRepeats(form: ClassFormValues): number {
  // Chọn ngày cụ thể: mỗi buổi là một buổi thực tế, không lặp -> chỉ tính đúng số buổi đã chọn.
  if (form.scheduleMode !== 'WEEKLY') return 1;
  const weeks = weeksForCycle(form);
  const n = repeatWeeksOf(form);
  if (n <= 1) return weeks;
  const on = studyWeeksOf(form);
  const remainder = weeks % n;
  return Math.floor(weeks / n) * on.length + on.filter((w) => w <= remainder).length;
}

/** Mô tả kiểu lặp: "hàng tuần", "mỗi N tuần" hoặc "học tuần 1, 3 trong mỗi N tuần". */
export function repeatLabel(form: ClassFormValues): string {
  const n = repeatWeeksOf(form);
  const on = studyWeeksOf(form);
  if (n === 1 || on.length >= n) return 'hàng tuần';
  if (on.length === 1 && on[0] === 1) return `mỗi ${n} tuần`;
  return `học tuần ${on.join(', ')} trong mỗi ${n} tuần`;
}

/** Số buổi học ước tính của cả khoá = số khung mỗi lần lặp × số lần lặp. */
export function estimatedSessions(form: ClassFormValues): number {
  const perRepeat = Math.max(1, form.slots.length);
  return perRepeat * patternRepeats(form);
}

/** Tổng số giờ học của tất cả khung trong một lần lặp. */
export function totalHoursPerRepeat(form: ClassFormValues): number {
  return form.slots.reduce((sum, s) => sum + slotHours(s.start, s.end), 0);
}

/** Tổng số giờ học của một môn trong một lần lặp. */
export function hoursPerRepeatForSubject(form: ClassFormValues, subjectId: string): number {
  return form.slots
    .filter((s) => s.subjectId === subjectId)
    .reduce((sum, s) => sum + slotHours(s.start, s.end), 0);
}

/** Tổng học phí dự kiến = Σ (học phí/giờ của môn × số giờ môn đó mỗi lần lặp × số lần lặp). */
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

/** Tên thứ tiếng Việt của một ngày "yyyy-MM-dd" (Chủ nhật, Thứ 2...). */
export function weekdayVi(dateStr: string): string {
  if (!dateStr) return '';
  const d = new Date(`${dateStr}T00:00:00`);
  return ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'][d.getDay()] ?? '';
}

/** Câu tóm tắt lịch học (thời lượng, kiểu lặp, số buổi, giờ học theo từng môn) để lưu vào mô tả lớp. */
export function buildScheduleSummary(form: ClassFormValues, subjects: CatalogOption[] = []): string {
  const parts = [
    `Lịch học ${cycleLabelOf(form).toLowerCase()}${
      form.scheduleMode === 'WEEKLY' ? ` — ${repeatLabel(form)}` : ''
    } (${form.slots.length} buổi/tuần)`,
  ];
  const money = new Intl.NumberFormat('vi-VN');
  /** Tên môn theo mã (môn "khác" lấy tên người dùng gõ). */
  const nameOf = (id: string) =>
    isOtherSubject(id)
      ? form.subjectOthers[id]?.trim() || 'Môn học khác'
      : (subjects.find((s) => String(s.id) === id)?.name ?? '');
  /** Nhãn thứ trong tuần theo giá trị. */
  const dayLabel = (v: string) => DAY_OF_WEEK_OPTIONS.find((d) => d.value === v)?.label ?? v;
  /** Thời điểm của khung học: thứ (lịch tuần) hoặc "Thứ x yyyy-MM-dd" (lịch theo ngày). */
  const whenOf = (s: ClassFormValues['slots'][number]) =>
    form.scheduleMode === 'WEEKLY' ? dayLabel(s.day) : `${weekdayVi(s.date)} ${s.date}`;
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

/**
 * Dịch state của form sang payload gửi backend — nơi quyết định một tin trông thế nào.
 *
 * <p>Việc chính là "làm phẳng": form có hàng chục trường, nhưng bảng tutoring_classes chỉ
 * có vài cột quen thuộc. Nên hàm này tự suy ra các cột đó rồi nhét phần còn lại vào
 * detailsJson:</p>
 *
 * <ul>
 *   <li><b>title</b> — tự sinh từ danh sách môn ("Cần tìm gia sư môn Toán, Vật lý...").</li>
 *   <li><b>description</b> — dòng môn học + bản tóm tắt lịch dạng chữ + ghi chú.</li>
 *   <li><b>startDate / endDate</b> — lịch hàng tuần thì lấy hôm nay + số tuần của chu kỳ;
 *       lịch chọn ngày cụ thể thì lấy ngày sớm nhất và muộn nhất trong các buổi.</li>
 *   <li><b>tuitionFee</b> — học phí của môn ĐẦU TIÊN, không phải tổng. Lớp nhiều môn muốn
 *       biết giá từng môn phải đọc subjectFees trong detailsJson.</li>
 *   <li><b>budget</b> — tổng tiền cả khóa, tính từ số giờ × học phí từng môn.</li>
 *   <li><b>detailsJson</b> — chuỗi JSON chứa nguyên vẹn mọi thứ người dùng đã điền, để khi
 *       mở lại tin còn dựng ngược về form được (xem classToForm ở dưới).</li>
 * </ul>
 */
export function formToPayload(
  form: ClassFormValues,
  subjects: CatalogOption[] = [],
): ClassRequestPayload {
  const sessions = estimatedSessions(form);
  const budget = totalBudget(form);
  const primaryFee = Number(form.subjectFees[form.subjectIds[0]]) || 0;
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
  const subjectNames = form.subjectIds
    .map((id) =>
      isOtherSubject(id)
        ? form.subjectOthers[id]?.trim() || 'Môn học khác'
        : subjects.find((s) => String(s.id) === id)?.name,
    )
    .filter((n): n is string => !!n);
  const subjectLine = subjectNames.length > 0 ? `Môn học: ${subjectNames.join(', ')}` : '';
  const primarySubjectId = form.subjectIds.find((id) => !isOtherSubject(id));
  const description = [subjectLine, buildScheduleSummary(form, subjects), form.note.trim()]
    .filter(Boolean)
    .join('\n');
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
    detailsJson: JSON.stringify(form),
  };
}
