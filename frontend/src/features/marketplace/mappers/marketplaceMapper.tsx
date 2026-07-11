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
    feePerHour: '',
    billingCycle: 'MONTH',
    months: '1',
    scheduleMode: 'WEEKLY',
    slots: [],
    note: '',
  };
}

/** Nạp dữ liệu lớp đã có vào form khi sửa. */
export function classToForm(c: ClassResponse): ClassFormValues {
  // Ưu tiên nạp lại nguyên trạng form từ JSON snapshot (khôi phục 100%).
  if (c.detailsJson) {
    try {
      const parsed = JSON.parse(c.detailsJson) as Partial<ClassFormValues>;
      return { ...emptyForm(), ...parsed };
    } catch {
      // JSON hỏng → rơi xuống nạp theo cột bên dưới.
    }
  }
  const goalMatched =
    !!c.learningGoal && LEARNING_GOAL_OPTIONS.includes(c.learningGoal);
  const reqMatched =
    !!c.tutorRequirement && TUTOR_REQUIREMENT_OPTIONS.includes(c.tutorRequirement);
  const feePerHour = c.tuitionFee != null ? String(c.tuitionFee) : '';
  return {
    subjectIds: c.subjectId != null ? [String(c.subjectId)] : [],
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
    feePerHour,
    billingCycle: 'MONTH',
    months: '1',
    scheduleMode: 'WEEKLY',
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

/** Số tuần ước tính theo chu kỳ học phí (Theo tháng = số tháng × 4 tuần). */
export function weeksForCycle(form: ClassFormValues): number {
  if (form.billingCycle === 'MONTH') {
    return Math.max(1, Number(form.months) || 1) * 4;
  }
  return BILLING_CYCLE_OPTIONS.find((o) => o.value === form.billingCycle)?.weeks ?? 4;
}

/** Nhãn chu kỳ để hiển thị/mô tả (Theo tháng kèm số tháng). */
export function cycleLabelOf(form: ClassFormValues): string {
  if (form.billingCycle === 'MONTH') {
    return `${Math.max(1, Number(form.months) || 1)} tháng`;
  }
  return BILLING_CYCLE_OPTIONS.find((o) => o.value === form.billingCycle)?.label ?? 'Theo tháng';
}

/** Tổng số buổi ước tính = số buổi/tuần (số slot) × số tuần trong chu kỳ. */
export function estimatedSessions(form: ClassFormValues): number {
  const perWeek = Math.max(1, form.slots.length);
  return perWeek * weeksForCycle(form);
}

/** Tổng số giờ học mỗi tuần = cộng thời lượng của tất cả buổi trong lịch. */
export function totalHoursPerWeek(form: ClassFormValues): number {
  return form.slots.reduce((sum, s) => sum + slotHours(s.start, s.end), 0);
}

/** Tên thứ (Vi) của một ngày YYYY-MM-DD. */
export function weekdayVi(dateStr: string): string {
  if (!dateStr) return '';
  const d = new Date(`${dateStr}T00:00:00`);
  return ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'][d.getDay()] ?? '';
}

/** Câu mô tả lịch học để lưu vào phần mô tả (nhóm theo môn, theo ngày). */
export function buildScheduleSummary(form: ClassFormValues, subjects: CatalogOption[] = []): string {
  const parts = [`Lịch học ${cycleLabelOf(form).toLowerCase()} (${form.slots.length} buổi)`];
  const nameOf = (id: string) => subjects.find((s) => String(s.id) === id)?.name ?? '';
  const dayLabel = (v: string) => DAY_OF_WEEK_OPTIONS.find((d) => d.value === v)?.label ?? v;
  const whenOf = (s: ClassFormValues['slots'][number]) =>
    form.scheduleMode === 'WEEKLY' ? `${dayLabel(s.day)} hàng tuần` : `${weekdayVi(s.date)} ${s.date}`;
  // Nhóm slot theo môn.
  const bySubject = form.subjectIds
    .map((sid) => {
      const rows = form.slots
        .filter((s) => s.subjectId === sid)
        .map((s) => `${whenOf(s)} ${s.session} (${s.start}–${s.end})`);
      return rows.length ? `${nameOf(sid)}: ${rows.join(', ')}` : '';
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
  const feePerHour = Number(form.feePerHour) || 0;
  // Tổng học phí = đơn giá/giờ × tổng số giờ/tuần × số tuần trong chu kỳ.
  const budget = Math.round(feePerHour * totalHoursPerWeek(form) * weeksForCycle(form));
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
    tuitionFee: feePerHour,
    budget,
    recurringType: 'WEEKLY',
    description: description || undefined,
    // Lưu nguyên trạng form để nạp lại đầy đủ khi Sửa.
    detailsJson: JSON.stringify(form),
  };
}
