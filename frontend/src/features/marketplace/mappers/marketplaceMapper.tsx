import {
  BILLING_CYCLE_OPTIONS,
  DAY_OF_WEEK_OPTIONS,
  LEARNING_GOAL_OPTIONS,
  LEARNING_GOAL_OTHER,
  TUTOR_REQUIREMENT_OPTIONS,
  type ClassFormValues,
  type ClassRequestPayload,
  type ClassResponse,
} from '../types/marketplaceTypes';

/** Giá trị mặc định cho form tạo mới. */
export function emptyForm(): ClassFormValues {
  return {
    subjectId: '',
    gradeId: '',
    learningGoal: '',
    learningGoalOther: '',
    tutorRequirement: TUTOR_REQUIREMENT_OPTIONS[0],
    tutorRequirementDetail: '',
    lessonMode: 'OFFLINE',
    provinceId: '',
    provinceName: '',
    district: '',
    address: '',
    feePerSession: '',
    billingCycle: 'MONTH',
    sessionsPerWeek: '2',
    daysOfWeek: [],
    dayTimes: {},
    startDate: '',
    note: '',
  };
}

/** Nạp dữ liệu lớp đã có vào form khi sửa. */
export function classToForm(c: ClassResponse): ClassFormValues {
  const goalMatched =
    !!c.learningGoal && LEARNING_GOAL_OPTIONS.includes(c.learningGoal);
  const reqMatched =
    !!c.tutorRequirement && TUTOR_REQUIREMENT_OPTIONS.includes(c.tutorRequirement);
  const feePerSession = c.tuitionFee != null ? String(c.tuitionFee) : '';
  return {
    subjectId: c.subjectId != null ? String(c.subjectId) : '',
    gradeId: c.gradeId != null ? String(c.gradeId) : '',
    learningGoal: c.learningGoal ? (goalMatched ? c.learningGoal : LEARNING_GOAL_OTHER) : '',
    learningGoalOther: c.learningGoal && !goalMatched ? c.learningGoal : '',
    tutorRequirement: reqMatched ? (c.tutorRequirement as string) : TUTOR_REQUIREMENT_OPTIONS[0],
    tutorRequirementDetail: !reqMatched && c.tutorRequirement ? c.tutorRequirement : '',
    lessonMode: c.lessonMode,
    provinceId: '',
    provinceName: '',
    district: '',
    address: c.address ?? '',
    feePerSession,
    billingCycle: 'MONTH',
    sessionsPerWeek: '2',
    daysOfWeek: [],
    dayTimes: {},
    startDate: c.startDate ?? '',
    note: c.description ?? '',
  };
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

/** Số tuần ước tính theo chu kỳ học phí. */
export function weeksForCycle(form: ClassFormValues): number {
  return BILLING_CYCLE_OPTIONS.find((o) => o.value === form.billingCycle)?.weeks ?? 4;
}

/** Tổng số buổi ước tính = số buổi/tuần × số tuần trong chu kỳ. */
export function estimatedSessions(form: ClassFormValues): number {
  const perWeek = Math.max(1, Number(form.sessionsPerWeek) || 1);
  return perWeek * weeksForCycle(form);
}

/** Câu mô tả lịch học để lưu vào phần mô tả (backend chưa có cột riêng). */
export function buildScheduleSummary(form: ClassFormValues): string {
  const perWeek = Math.max(1, Number(form.sessionsPerWeek) || 1);
  const cycleLabel = form.billingCycle === 'TERM' ? 'theo kỳ' : 'theo tháng';
  const parts = [`Lịch học ${cycleLabel}: ${perWeek} buổi/tuần`];
  // Liệt kê từng thứ kèm khung giờ riêng (theo thứ tự trong tuần).
  const dayParts = DAY_OF_WEEK_OPTIONS.filter((d) => form.daysOfWeek.includes(d.value)).map((d) => {
    const t = form.dayTimes[d.value];
    return t?.start && t?.end ? `${d.label} (${t.start}–${t.end})` : d.label;
  });
  if (dayParts.length > 0) {
    parts.push(`vào ${dayParts.join(', ')}`);
  }
  return `${parts.join(' ')}.`;
}

/** Chuyển form → payload gửi backend. */
export function formToPayload(form: ClassFormValues): ClassRequestPayload {
  const sessions = estimatedSessions(form);
  const feePerSession = Number(form.feePerSession) || 0;
  const startDate = form.startDate || new Date().toISOString().slice(0, 10);
  const description = [buildScheduleSummary(form), form.note.trim()]
    .filter(Boolean)
    .join('\n');
  // Backend chưa có cột riêng cho tỉnh/khu vực → gộp thành một chuỗi địa chỉ đầy đủ.
  const fullAddress = [form.address.trim(), form.district.trim(), form.provinceName.trim()]
    .filter(Boolean)
    .join(', ');

  return {
    subjectId: form.subjectId ? Number(form.subjectId) : null,
    gradeId: form.gradeId ? Number(form.gradeId) : null,
    learningGoal: resolveLearningGoal(form) || null,
    tutorRequirement: resolveTutorRequirement(form) || null,
    locationId: null,
    address: fullAddress || null,
    lessonMode: form.lessonMode,
    numberOfSessions: sessions,
    startDate,
    endDate: startDate,
    tuitionFee: feePerSession,
    budget: feePerSession * sessions,
    recurringType: 'WEEKLY',
    description: description || undefined,
  };
}
