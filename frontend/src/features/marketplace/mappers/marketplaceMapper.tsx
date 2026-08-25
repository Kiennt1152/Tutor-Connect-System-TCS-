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

function migrateOtherSubjects(parsed: LegacyForm): Record<string, string> {
  if (parsed.subjectOthers && typeof parsed.subjectOthers === 'object') return parsed.subjectOthers;
  if (parsed.subjectOther && (parsed.subjectIds ?? []).includes(OTHER_SUBJECT)) {
    return { [OTHER_SUBJECT]: parsed.subjectOther };
  }
  return {};
}

function migrateStudyWeeks(parsed: LegacyForm): number[] {
  if (Array.isArray(parsed.studyWeeks) && parsed.studyWeeks.length > 0) return parsed.studyWeeks;
  const k = Math.trunc(Number(parsed.weeksOnPerCycle));
  if (Number.isInteger(k) && k >= 1) return Array.from({ length: k }, (_, i) => i + 1);
  return [1];
}

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

function slotHours(start: string, end: string): number {
  if (!start || !end) return 0;
  const [sh, sm] = start.split(':').map(Number);
  const [eh, em] = end.split(':').map(Number);
  const endMin = end === '23:59' ? 24 * 60 : eh * 60 + em;
  const diff = endMin - (sh * 60 + sm);
  return diff > 0 ? diff / 60 : 0;
}

export function resolveLearningGoal(form: ClassFormValues): string {
  if (form.learningGoal === LEARNING_GOAL_OTHER) {
    return form.learningGoalOther.trim();
  }
  return form.learningGoal.trim();
}

export function resolveTutorRequirement(form: ClassFormValues): string {
  const detail = form.tutorRequirementDetail.trim();
  if (detail) {
    return `${form.tutorRequirement} — ${detail}`;
  }
  return form.tutorRequirement;
}

export function durationCountOf(form: ClassFormValues): number {
  return Math.max(1, Number(form.months) || 1);
}

export function totalMonthsOf(form: ClassFormValues): number {
  const n = durationCountOf(form);
  return form.durationUnit === 'YEAR' ? n * 12 : n;
}

export function weeksForCycle(form: ClassFormValues): number {
  if (form.billingCycle === 'MONTH') {
    return totalMonthsOf(form) * 4;
  }
  return BILLING_CYCLE_OPTIONS.find((o) => o.value === form.billingCycle)?.weeks ?? 4;
}

export function cycleLabelOf(form: ClassFormValues): string {
  if (form.billingCycle === 'MONTH') {
    const n = durationCountOf(form);
    return form.durationUnit === 'YEAR' ? `${n} năm` : `${n} tháng`;
  }
  return BILLING_CYCLE_OPTIONS.find((o) => o.value === form.billingCycle)?.label ?? '1 tháng';
}

export function repeatWeeksOf(form: ClassFormValues): number {
  if (form.scheduleMode !== 'WEEKLY') return 1;
  const n = Number(form.repeatEveryWeeks) || 1;
  return Math.min(4, Math.max(1, Math.trunc(n)));
}

export function studyWeeksOf(form: ClassFormValues): number[] {
  const n = repeatWeeksOf(form);
  if (n <= 1) return [1];
  const raw = Array.isArray(form.studyWeeks) ? form.studyWeeks : [];
  const out = [...new Set(raw.map(Number))]
    .filter((w) => Number.isInteger(w) && w >= 1 && w <= n)
    .sort((a, b) => a - b);
  return out.length > 0 ? out : [1];
}

export function restWeeksOf(form: ClassFormValues): number[] {
  const on = new Set(studyWeeksOf(form));
  return Array.from({ length: repeatWeeksOf(form) }, (_, i) => i + 1).filter((w) => !on.has(w));
}

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

export function repeatLabel(form: ClassFormValues): string {
  const n = repeatWeeksOf(form);
  const on = studyWeeksOf(form);
  if (n === 1 || on.length >= n) return 'hàng tuần';
  if (on.length === 1 && on[0] === 1) return `mỗi ${n} tuần`;
  return `học tuần ${on.join(', ')} trong mỗi ${n} tuần`;
}

export function estimatedSessions(form: ClassFormValues): number {
  const perRepeat = Math.max(1, form.slots.length);
  return perRepeat * patternRepeats(form);
}

export function totalHoursPerRepeat(form: ClassFormValues): number {
  return form.slots.reduce((sum, s) => sum + slotHours(s.start, s.end), 0);
}

export function hoursPerRepeatForSubject(form: ClassFormValues, subjectId: string): number {
  return form.slots
    .filter((s) => s.subjectId === subjectId)
    .reduce((sum, s) => sum + slotHours(s.start, s.end), 0);
}

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

export function weekdayVi(dateStr: string): string {
  if (!dateStr) return '';
  const d = new Date(`${dateStr}T00:00:00`);
  return ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'][d.getDay()] ?? '';
}

export function buildScheduleSummary(form: ClassFormValues, subjects: CatalogOption[] = []): string {
  const parts = [
    `Lịch học ${cycleLabelOf(form).toLowerCase()}${
      form.scheduleMode === 'WEEKLY' ? ` — ${repeatLabel(form)}` : ''
    } (${form.slots.length} buổi/tuần)`,
  ];
  const money = new Intl.NumberFormat('vi-VN');
  const nameOf = (id: string) =>
    isOtherSubject(id)
      ? form.subjectOthers[id]?.trim() || 'Môn học khác'
      : (subjects.find((s) => String(s.id) === id)?.name ?? '');
  const dayLabel = (v: string) => DAY_OF_WEEK_OPTIONS.find((d) => d.value === v)?.label ?? v;
  const whenOf = (s: ClassFormValues['slots'][number]) =>
    form.scheduleMode === 'WEEKLY' ? dayLabel(s.day) : `${weekdayVi(s.date)} ${s.date}`;
  const bySubject = form.subjectIds
    .map((sid) => {
      const hm = (t: string) => (t === '23:59' ? '00:00' : t);
      const rows = form.slots
        .filter((s) => s.subjectId === sid)
        .map((s) => `${whenOf(s)} ${s.session} (${hm(s.start)}–${hm(s.end)})`);
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
