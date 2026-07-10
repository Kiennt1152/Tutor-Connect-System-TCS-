import {
  BILLING_CYCLE_OPTIONS,
  DAY_OF_WEEK_OPTIONS,
  LEARNING_GOAL_OPTIONS,
  LEARNING_GOAL_OTHER,
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
    feePerHour: '',
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
  const feePerHour = c.tuitionFee != null ? String(c.tuitionFee) : '';
  return {
    subjectIds: c.subjectId != null ? [String(c.subjectId)] : [],
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
    feePerHour,
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

/** Tổng số giờ học mỗi tuần = cộng thời lượng (end-start) của tất cả các thứ đã chọn. */
export function totalHoursPerWeek(form: ClassFormValues): number {
  let minutes = 0;
  for (const day of form.daysOfWeek) {
    const t = form.dayTimes[day];
    if (t?.start && t?.end) {
      const [sh, sm] = t.start.split(':').map(Number);
      const [eh, em] = t.end.split(':').map(Number);
      const diff = eh * 60 + em - (sh * 60 + sm);
      if (diff > 0) minutes += diff;
    }
  }
  return minutes / 60;
}

/** Câu mô tả lịch học để lưu vào phần mô tả (backend chưa có cột riêng). */
export function buildScheduleSummary(form: ClassFormValues, subjects: CatalogOption[] = []): string {
  const perWeek = Math.max(1, Number(form.sessionsPerWeek) || 1);
  const cycleLabel = form.billingCycle === 'TERM' ? 'theo kỳ' : 'theo tháng';
  const parts = [`Lịch học ${cycleLabel}: ${perWeek} buổi/tuần`];
  const nameOf = (id: string) => subjects.find((s) => String(s.id) === id)?.name ?? '';
  // Liệt kê từng thứ: môn học + buổi + khung giờ (theo thứ tự trong tuần).
  const dayParts = DAY_OF_WEEK_OPTIONS.filter((d) => form.daysOfWeek.includes(d.value)).map((d) => {
    const t = form.dayTimes[d.value];
    const subj = t?.subjects?.length ? ` - ${t.subjects.map(nameOf).filter(Boolean).join(', ')}` : '';
    const session = t?.session ? ` buổi ${t.session}` : '';
    const range = t?.start && t?.end ? ` (${t.start}–${t.end})` : '';
    return `${d.label}${subj}${session}${range}`;
  });
  if (dayParts.length > 0) {
    parts.push(`vào ${dayParts.join('; ')}`);
  }
  return `${parts.join(' ')}.`;
}

/** Chuyển form → payload gửi backend.
 *  `subjects` (catalog) để lấy tên môn ghi vào mô tả khi chọn nhiều môn. */
export function formToPayload(
  form: ClassFormValues,
  subjects: CatalogOption[] = [],
): ClassRequestPayload {
  const sessions = estimatedSessions(form);
  const feePerHour = Number(form.feePerHour) || 0;
  // Tổng học phí = đơn giá/giờ × tổng số giờ/tuần × số tuần trong chu kỳ.
  const budget = Math.round(feePerHour * totalHoursPerWeek(form) * weeksForCycle(form));
  const startDate = form.startDate || new Date().toISOString().slice(0, 10);
  // Backend chỉ lưu 1 môn (subjectId) → môn đầu là môn chính; nếu chọn nhiều
  // môn thì ghi cả danh sách vào mô tả.
  const subjectNames = form.subjectIds
    .map((id) => subjects.find((s) => String(s.id) === id)?.name)
    .filter((n): n is string => !!n);
  const subjectLine = subjectNames.length > 1 ? `Các môn học: ${subjectNames.join(', ')}` : '';
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
    subjectId: form.subjectIds[0] ? Number(form.subjectIds[0]) : null,
    gradeId: form.gradeId ? Number(form.gradeId) : null,
    learningGoal: resolveLearningGoal(form) || null,
    tutorRequirement: resolveTutorRequirement(form) || null,
    locationId: null,
    address: fullAddress || null,
    lessonMode: form.lessonMode,
    numberOfSessions: sessions,
    startDate,
    endDate: startDate,
    tuitionFee: feePerHour,
    budget,
    recurringType: 'WEEKLY',
    description: description || undefined,
  };
}
