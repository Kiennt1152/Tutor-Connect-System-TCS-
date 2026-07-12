import { useEffect, useMemo, useState } from 'react';
import {
  BILLING_CYCLE_OPTIONS,
  DAY_OF_WEEK_OPTIONS,
  LEARNING_GOAL_OPTIONS,
  LEARNING_GOAL_OTHER,
  LESSON_MODE_OPTIONS,
  OTHER_SUBJECT,
  SCHEDULE_MODE_OPTIONS,
  SESSION_OPTIONS,
  TUTOR_REQUIREMENT_OPTIONS,
  type BillingCycle,
  type CatalogOption,
  type ClassFormValues,
  type ClassRequestPayload,
  type LessonMode,
  type ScheduleMode,
  type ScheduleSlot,
} from '../types/marketplaceTypes';
import {
  cycleLabelOf,
  formToPayload,
  totalBudget,
  totalHoursPerWeek,
  weeksForCycle,
} from '../mappers/marketplaceMapper';
import { marketplaceApi } from '../api/marketplaceApi';
import '../pages/MarketplacePage.css';

interface ClassRequestFormProps {
  readonly initial: ClassFormValues;
  readonly subjects: CatalogOption[];
  readonly grades: CatalogOption[];
  readonly provinces: CatalogOption[];
  readonly isEdit: boolean;
  readonly submitting: boolean;
  readonly error: string | null;
  readonly onSubmit: (payload: ClassRequestPayload) => void;
  readonly onCancel: () => void;
}

const currency = new Intl.NumberFormat('vi-VN');

/** Sinh các mốc giờ 24h (bước 30 phút) trong khoảng [min, max]. */
function buildTimeSlots(min: string, max: string, stepMinutes = 30): string[] {
  const toMinutes = (t: string) => {
    const [h, m] = t.split(':').map(Number);
    return h * 60 + m;
  };
  const out: string[] = [];
  for (let x = toMinutes(min); x <= toMinutes(max); x += stepMinutes) {
    out.push(`${String(Math.floor(x / 60)).padStart(2, '0')}:${String(x % 60).padStart(2, '0')}`);
  }
  return out;
}

function emptySlot(subjectId: string): ScheduleSlot {
  return { subjectId, day: '', date: '', session: '', start: '', end: '' };
}

/** Mục tiêu theo LỚP (vd Lớp 1 không có "Luyện thi Đại học"). */
function goalsByGrade(gradeName: string | undefined): readonly string[] {
  if (!gradeName) return LEARNING_GOAL_OPTIONS;
  const m = /^Lớp\s+(\d+)/.exec(gradeName);
  if (m) {
    const n = Number(m[1]);
    return LEARNING_GOAL_OPTIONS.filter((g) => {
      if (g === 'Luyện thi chuyển cấp (vào 10)') return n === 9;
      if (g === 'Luyện thi Đại học') return n === 12;
      return true;
    });
  }
  if (gradeName.includes('Đại học')) {
    return LEARNING_GOAL_OPTIONS.filter((g) =>
      ['Lấy lại gốc', 'Luyện thi Đại học', 'Luyện thi chứng chỉ (IELTS, TOEIC...)'].includes(g),
    );
  }
  if (gradeName.toLowerCase().includes('chứng chỉ')) {
    return LEARNING_GOAL_OPTIONS.filter((g) =>
      ['Lấy lại gốc', 'Luyện thi chứng chỉ (IELTS, TOEIC...)'].includes(g),
    );
  }
  return LEARNING_GOAL_OPTIONS;
}

/** Mục tiêu phù hợp với cả LỚP và MÔN HỌC.
 *  Mục tiêu ngoại ngữ (IELTS/TOEIC, Giao tiếp) chỉ hiện khi có môn ngôn ngữ. */
function allowedGoals(
  gradeName: string | undefined,
  subjectNames: string[],
): readonly string[] {
  const byGrade = goalsByGrade(gradeName);
  if (subjectNames.length === 0) return byGrade;
  const hasEnglish = subjectNames.includes('Tiếng Anh');
  const hasCert = subjectNames.some((n) => n.toLowerCase().includes('chứng chỉ'));
  return byGrade.filter((g) => {
    if (g === 'Luyện thi chứng chỉ (IELTS, TOEIC...)') return hasEnglish || hasCert;
    return true;
  });
}

export function ClassRequestForm({
  initial,
  subjects,
  grades,
  provinces,
  isEdit,
  submitting,
  error,
  onSubmit,
  onCancel,
}: ClassRequestFormProps) {
  const [form, setForm] = useState<ClassFormValues>(initial);
  const [touched, setTouched] = useState(false);
  const [districts, setDistricts] = useState<CatalogOption[]>([]);
  const [wards, setWards] = useState<CatalogOption[]>([]);

  function set<K extends keyof ClassFormValues>(key: K, value: ClassFormValues[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  const subjName = (id: string) =>
    id === OTHER_SUBJECT
      ? form.subjectOther.trim() || 'Môn học khác'
      : (subjects.find((s) => String(s.id) === id)?.name ?? id);
  const dayLabel = (v: string) => DAY_OF_WEEK_OPTIONS.find((d) => d.value === v)?.label ?? v;
  const isWeekly = form.scheduleMode === 'WEEKLY';
  const currentSubjectNames = form.subjectIds.map(subjName);
  const gradeName = grades.find((g) => String(g.id) === form.gradeId)?.name;
  const goalOptions = allowedGoals(gradeName, currentSubjectNames);

  // Giữ mục tiêu nếu vẫn hợp (hoặc "Khác"), ngược lại xóa để chọn lại.
  function keepGoal(goal: string, allowed: readonly string[]): string {
    return goal === LEARNING_GOAL_OTHER || allowed.includes(goal) ? goal : '';
  }

  // Đổi lớp → cập nhật mục tiêu nếu không còn hợp.
  function handleGradeChange(gradeId: string) {
    const gName = grades.find((g) => String(g.id) === gradeId)?.name;
    setForm((prev) => ({
      ...prev,
      gradeId,
      learningGoal: keepGoal(prev.learningGoal, allowedGoals(gName, prev.subjectIds.map(subjName))),
    }));
  }

  // Tải quận/huyện khi đổi tỉnh; tải phường/xã khi đổi quận/huyện.
  useEffect(() => {
    if (!form.provinceId) {
      setDistricts([]);
      return;
    }
    let alive = true;
    marketplaceApi
      .listDistricts(Number(form.provinceId))
      .then((d) => alive && setDistricts(d))
      .catch(() => alive && setDistricts([]));
    return () => {
      alive = false;
    };
  }, [form.provinceId]);

  useEffect(() => {
    if (!form.districtId) {
      setWards([]);
      return;
    }
    let alive = true;
    marketplaceApi
      .listWards(Number(form.districtId))
      .then((w) => alive && setWards(w))
      .catch(() => alive && setWards([]));
    return () => {
      alive = false;
    };
  }, [form.districtId]);

  function handleProvinceChange(value: string) {
    const name = provinces.find((p) => String(p.id) === value)?.name ?? '';
    setForm((prev) => ({
      ...prev,
      provinceId: value,
      provinceName: name,
      districtId: '',
      districtName: '',
      wardId: '',
      wardName: '',
    }));
  }

  function handleDistrictChange(value: string) {
    const name = districts.find((d) => String(d.id) === value)?.name ?? '';
    setForm((prev) => ({ ...prev, districtId: value, districtName: name, wardId: '', wardName: '' }));
  }

  function handleWardChange(value: string) {
    const name = wards.find((w) => String(w.id) === value)?.name ?? '';
    setForm((prev) => ({ ...prev, wardId: value, wardName: name }));
  }

  // Chọn/bỏ môn học — bỏ môn thì xóa lịch của môn đó; thêm môn thì tạo sẵn 1 buổi.
  function toggleSubject(id: string) {
    setForm((prev) => {
      const removing = prev.subjectIds.includes(id);
      const subjectIds = removing
        ? prev.subjectIds.filter((s) => s !== id)
        : [...prev.subjectIds, id];
      const slots = removing
        ? prev.slots.filter((s) => s.subjectId !== id)
        : [...prev.slots, emptySlot(id)];
      const subjectFees = { ...prev.subjectFees };
      if (removing) delete subjectFees[id];
      else if (!(id in subjectFees)) subjectFees[id] = '';
      // Mục tiêu có thể không còn hợp với môn mới → cập nhật.
      const gName = grades.find((g) => String(g.id) === prev.gradeId)?.name;
      const learningGoal = keepGoal(prev.learningGoal, allowedGoals(gName, subjectIds.map(subjName)));
      return { ...prev, subjectIds, slots, subjectFees, learningGoal };
    });
  }

  function setSubjectFee(subjectId: string, value: string) {
    setForm((prev) => ({
      ...prev,
      subjectFees: { ...prev.subjectFees, [subjectId]: value },
    }));
  }

  function addSlot(subjectId: string) {
    setForm((prev) => ({ ...prev, slots: [...prev.slots, emptySlot(subjectId)] }));
  }

  function removeSlot(index: number) {
    setForm((prev) => ({ ...prev, slots: prev.slots.filter((_, i) => i !== index) }));
  }

  function updateSlot(index: number, patch: Partial<ScheduleSlot>) {
    setForm((prev) => ({
      ...prev,
      slots: prev.slots.map((s, i) => (i === index ? { ...s, ...patch } : s)),
    }));
  }

  // Chọn buổi → điền sẵn khung giờ gợi ý (vẫn sửa được).
  function setSlotSession(index: number, session: string) {
    const preset = SESSION_OPTIONS.find((s) => s.value === session);
    updateSlot(index, preset ? { session, start: preset.start, end: preset.end } : { session });
  }

  const cycle =
    BILLING_CYCLE_OPTIONS.find((o) => o.value === form.billingCycle) ?? BILLING_CYCLE_OPTIONS[0];
  const isMonth = form.billingCycle === 'MONTH';
  const weeks = weeksForCycle(form);
  const cycleName = cycleLabelOf(form); // "N tháng" hoặc nhãn cố định
  const cycleLabelDisplay = isMonth ? cycleName : cycle.label.toLowerCase();
  const cycleSuffix = isMonth ? `đ / ${cycleName}` : cycle.suffix;

  const hoursPerWeek = useMemo(() => totalHoursPerWeek(form), [form]);
  const total = useMemo(() => totalBudget(form), [form]);

  const isOffline = form.lessonMode !== 'ONLINE';
  const today = new Date().toLocaleDateString('en-CA');

  // Kiểm tra từng buổi: đủ thông tin (Thứ hoặc Ngày) + ngày không quá khứ + giờ hợp lệ.
  const slotErrorSet = new Set<string>();
  form.slots.forEach((s) => {
    const nm = subjName(s.subjectId);
    const whenMissing = isWeekly ? !s.day : !s.date;
    if (whenMissing || !s.session || !s.start || !s.end) {
      slotErrorSet.add(`${nm}: có buổi chưa đủ thông tin (${isWeekly ? 'thứ' : 'ngày'} / buổi / giờ)`);
    } else if (!isWeekly && s.date < today) {
      slotErrorSet.add(`${nm}: ngày học không được ở quá khứ`);
    } else if (s.end <= s.start) {
      slotErrorSet.add(`${nm}: giờ kết thúc phải sau giờ bắt đầu`);
    } else {
      const sess = SESSION_OPTIONS.find((o) => o.value === s.session);
      if (sess && (s.start < sess.min || s.end > sess.max)) {
        slotErrorSet.add(`${nm}: giờ phải trong ${sess.label}`);
      }
    }
  });
  for (const sid of form.subjectIds) {
    if (!form.slots.some((s) => s.subjectId === sid)) {
      slotErrorSet.add(`${subjName(sid)}: chưa có buổi học nào`);
    }
    if (!(Number(form.subjectFees[sid]) > 0)) {
      slotErrorSet.add(`${subjName(sid)}: chưa nhập học phí/giờ`);
    }
  }
  const slotErrors = [...slotErrorSet];

  // Chống trùng giờ: 2 buổi cùng Thứ (WEEKLY) hoặc cùng Ngày (CUSTOM) và giờ giao nhau.
  const conflicts: string[] = [];
  for (let i = 0; i < form.slots.length; i++) {
    for (let j = i + 1; j < form.slots.length; j++) {
      const a = form.slots[i];
      const b = form.slots[j];
      const sameWhen = isWeekly ? !!a.day && a.day === b.day : !!a.date && a.date === b.date;
      if (sameWhen && a.start && a.end && b.start && b.end && a.start < b.end && b.start < a.end) {
        const when = isWeekly ? dayLabel(a.day) : `ngày ${a.date}`;
        conflicts.push(
          `Trùng giờ ${when}: ${subjName(a.subjectId)} (${a.start}–${a.end}) & ${subjName(
            b.subjectId,
          )} (${b.start}–${b.end})`,
        );
      }
    }
  }

  const missing: string[] = [];
  if (form.subjectIds.length === 0) missing.push('Môn học');
  if (form.subjectIds.includes(OTHER_SUBJECT) && !form.subjectOther.trim())
    missing.push('Tên môn học khác');
  if (!form.gradeId) missing.push('Lớp');
  if (isOffline) {
    if (!form.provinceId) missing.push('Tỉnh / Thành phố');
    if (!form.districtId) missing.push('Quận / Huyện');
    if (!form.wardId) missing.push('Phường / Xã');
    if (!form.address.trim()) missing.push('Địa chỉ chi tiết');
  }
  if (!form.tutorRequirementDetail.trim()) missing.push('Yêu cầu bổ sung');
  if (form.slots.length === 0) missing.push('Lịch học');
  if (!form.note.trim()) missing.push('Ghi chú / yêu cầu khác');

  function handleSubmit() {
    setTouched(true);
    if (missing.length > 0 || slotErrors.length > 0 || conflicts.length > 0) return;
    onSubmit(formToPayload(form, subjects));
  }

  return (
    <div className="mkt-form">
      {/* Môn học — chọn nhiều môn */}
      <div className="mkt-field">
        <span className="mkt-field__label">
          Môn học <em>*</em>
          <span className="mkt-field__hint-inline"> (có thể chọn nhiều môn)</span>
        </span>
        {subjects.length === 0 ? (
          <p className="mkt-hint">Đang tải danh sách môn học…</p>
        ) : (
          <div className="mkt-checks">
            {subjects.map((s) => {
              const id = String(s.id);
              return (
                <label key={s.id} className="mkt-check">
                  <input
                    type="checkbox"
                    checked={form.subjectIds.includes(id)}
                    onChange={() => toggleSubject(id)}
                  />
                  <span>{s.name}</span>
                </label>
              );
            })}
            <label className="mkt-check">
              <input
                type="checkbox"
                checked={form.subjectIds.includes(OTHER_SUBJECT)}
                onChange={() => toggleSubject(OTHER_SUBJECT)}
              />
              <span>Khác</span>
            </label>
          </div>
        )}
        {form.subjectIds.includes(OTHER_SUBJECT) && (
          <label className="mkt-field mkt-subject-other">
            <span className="mkt-field__label">
              Tên môn học khác <em>*</em>
            </span>
            <input
              type="text"
              value={form.subjectOther}
              placeholder="Nhập tên môn học khác…"
              onChange={(e) => set('subjectOther', e.target.value)}
            />
          </label>
        )}
      </div>

      {/* Lớp */}
      <div className="mkt-form__grid">
        <label className="mkt-field">
          <span className="mkt-field__label">
            Lớp <em>*</em>
          </span>
          <select value={form.gradeId} onChange={(e) => handleGradeChange(e.target.value)}>
            <option value="">-- Chọn lớp --</option>
            {grades.map((g) => (
              <option key={g.id} value={g.id}>
                {g.name}
              </option>
            ))}
          </select>
        </label>
      </div>

      {/* Mục tiêu học tập */}
      <div className="mkt-form__grid">
        <label className="mkt-field">
          <span className="mkt-field__label">Mục tiêu học tập</span>
          <select value={form.learningGoal} onChange={(e) => set('learningGoal', e.target.value)}>
            <option value="">-- Chọn mục tiêu --</option>
            {goalOptions.map((goal) => (
              <option key={goal} value={goal}>
                {goal}
              </option>
            ))}
          </select>
        </label>
      </div>

      {/* Hình thức học */}
      <div className="mkt-field">
        <span className="mkt-field__label">Hình thức học mong muốn</span>
        <div className="mkt-radios">
          {LESSON_MODE_OPTIONS.map((opt) => (
            <label key={opt.value} className="mkt-radio">
              <input
                type="radio"
                name="lessonMode"
                checked={form.lessonMode === opt.value}
                onChange={() => set('lessonMode', opt.value as LessonMode)}
              />
              <span>{opt.label}</span>
            </label>
          ))}
        </div>
      </div>

      {/* Địa điểm (chỉ khi offline) — cascade Tỉnh → Quận/Huyện → Phường/Xã */}
      {isOffline && (
        <>
          <div className="mkt-form__grid mkt-form__grid--3">
            <label className="mkt-field">
              <span className="mkt-field__label">
                Tỉnh / Thành phố <em>*</em>
              </span>
              <select value={form.provinceId} onChange={(e) => handleProvinceChange(e.target.value)}>
                <option value="">-- Chọn tỉnh thành --</option>
                {provinces.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="mkt-field">
              <span className="mkt-field__label">
                Quận / Huyện <em>*</em>
              </span>
              <select
                value={form.districtId}
                onChange={(e) => handleDistrictChange(e.target.value)}
                disabled={!form.provinceId}
              >
                <option value="">
                  {form.provinceId ? '-- Chọn quận/huyện --' : 'Chọn tỉnh thành trước'}
                </option>
                {districts.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
              </select>
            </label>
            <label className="mkt-field">
              <span className="mkt-field__label">
                Phường / Xã <em>*</em>
              </span>
              <select
                value={form.wardId}
                onChange={(e) => handleWardChange(e.target.value)}
                disabled={!form.districtId}
              >
                <option value="">
                  {form.districtId ? '-- Chọn phường/xã --' : 'Chọn quận/huyện trước'}
                </option>
                {wards.map((w) => (
                  <option key={w.id} value={w.id}>
                    {w.name}
                  </option>
                ))}
              </select>
            </label>
          </div>
          <label className="mkt-field">
            <span className="mkt-field__label">
              Địa chỉ chi tiết <em>*</em>
            </span>
            <input
              type="text"
              value={form.address}
              placeholder="Số nhà, tên đường…"
              onChange={(e) => set('address', e.target.value)}
            />
          </label>
        </>
      )}

      {/* Yêu cầu đối với gia sư */}
      <div className="mkt-form__grid">
        <label className="mkt-field">
          <span className="mkt-field__label">Yêu cầu đối với gia sư</span>
          <select
            value={form.tutorRequirement}
            onChange={(e) => set('tutorRequirement', e.target.value)}
          >
            {TUTOR_REQUIREMENT_OPTIONS.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
          </select>
        </label>
        <label className="mkt-field">
          <span className="mkt-field__label">
            Yêu cầu bổ sung (chứng chỉ, bằng cấp…) <em>*</em>
          </span>
          <input
            type="text"
            value={form.tutorRequirementDetail}
            placeholder="VD: Có chứng chỉ IELTS 7.0"
            onChange={(e) => set('tutorRequirementDetail', e.target.value)}
          />
        </label>
      </div>

      {/* Học theo */}
      <div className="mkt-form__grid">
        <label className="mkt-field">
          <span className="mkt-field__label">Học theo</span>
          <select
            value={form.billingCycle}
            onChange={(e) => set('billingCycle', e.target.value as BillingCycle)}
          >
            {BILLING_CYCLE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </label>
        {isMonth && (
          <label className="mkt-field">
            <span className="mkt-field__label">Số tháng</span>
            <select value={form.months} onChange={(e) => set('months', e.target.value)}>
              {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
                <option key={m} value={String(m)}>
                  {m} tháng
                </option>
              ))}
            </select>
          </label>
        )}
      </div>

      {/* Chế độ lịch: lặp hàng tuần hay chọn ngày cụ thể */}
      <div className="mkt-field">
        <span className="mkt-field__label">Kiểu lịch học</span>
        <div className="mkt-radios">
          {SCHEDULE_MODE_OPTIONS.map((opt) => (
            <label key={opt.value} className="mkt-radio">
              <input
                type="radio"
                name="scheduleMode"
                checked={form.scheduleMode === opt.value}
                onChange={() => set('scheduleMode', opt.value as ScheduleMode)}
              />
              <span>{opt.label}</span>
            </label>
          ))}
        </div>
        <span className="mkt-hint">
          {isWeekly
            ? 'Lặp lại cùng khung giờ theo Thứ ở các tuần tiếp theo.'
            : 'Chọn từng ngày cụ thể (không lặp lại) — hết các ngày/tuần đã chọn, bạn cần nhập lại lịch cho tuần tiếp theo.'}
        </span>
      </div>

      {/* Lịch học theo từng môn — mỗi môn có nhiều buổi, không trùng giờ */}
      <div className="mkt-field">
        <span className="mkt-field__label">
          Lịch học theo môn <em>*</em>
          <span className="mkt-field__hint-inline"> (các buổi không được trùng giờ)</span>
        </span>
        {form.subjectIds.length === 0 ? (
          <p className="mkt-hint">Hãy chọn môn học ở trên trước, rồi đặt lịch cho từng môn.</p>
        ) : (
          <div className="mkt-day-cards">
            {form.subjectIds.map((sid) => (
              <div key={sid} className="mkt-day-card">
                <div className="mkt-day-card__head mkt-day-card__head--fee">
                  <span>{subjName(sid)}</span>
                  <span className="mkt-subj-fee">
                    <input
                      type="number"
                      min={0}
                      step={10000}
                      value={form.subjectFees[sid] ?? ''}
                      placeholder="Học phí/giờ"
                      aria-label={`Học phí/giờ môn ${subjName(sid)}`}
                      onChange={(e) => setSubjectFee(sid, e.target.value)}
                    />
                    <span className="mkt-subj-fee__unit">đ/giờ</span>
                  </span>
                </div>
                {form.slots
                  .map((slot, idx) => ({ slot, idx }))
                  .filter((x) => x.slot.subjectId === sid)
                  .map(({ slot, idx }) => {
                    const sess = SESSION_OPTIONS.find((o) => o.value === slot.session);
                    const times = buildTimeSlots(sess?.min ?? '00:00', sess?.max ?? '23:30');
                    const endTimes = slot.start ? times.filter((t) => t > slot.start) : times;
                    return (
                      <div key={idx} className="mkt-slot-row">
                        {isWeekly ? (
                          <select
                            className="mkt-day-time__session"
                            aria-label="Thứ"
                            value={slot.day}
                            onChange={(e) => updateSlot(idx, { day: e.target.value })}
                          >
                            <option value="">Thứ…</option>
                            {DAY_OF_WEEK_OPTIONS.map((d) => (
                              <option key={d.value} value={d.value}>
                                {d.label}
                              </option>
                            ))}
                          </select>
                        ) : (
                          <input
                            type="date"
                            className="mkt-slot-date"
                            aria-label="Ngày học"
                            min={today}
                            value={slot.date}
                            onChange={(e) => updateSlot(idx, { date: e.target.value })}
                          />
                        )}
                        <select
                          className="mkt-day-time__session"
                          aria-label="Buổi"
                          value={slot.session}
                          onChange={(e) => setSlotSession(idx, e.target.value)}
                        >
                          <option value="">Buổi…</option>
                          {SESSION_OPTIONS.map((s) => (
                            <option key={s.value} value={s.value}>
                              {s.label}
                            </option>
                          ))}
                        </select>
                        <select
                          className="mkt-day-time__session"
                          aria-label="Giờ bắt đầu"
                          value={slot.start}
                          onChange={(e) => updateSlot(idx, { start: e.target.value })}
                        >
                          <option value="">Từ…</option>
                          {times.map((t) => (
                            <option key={t} value={t}>
                              {t}
                            </option>
                          ))}
                        </select>
                        <span className="mkt-day-time__sep">–</span>
                        <select
                          className="mkt-day-time__session"
                          aria-label="Giờ kết thúc"
                          value={slot.end}
                          onChange={(e) => updateSlot(idx, { end: e.target.value })}
                        >
                          <option value="">Đến…</option>
                          {endTimes.map((t) => (
                            <option key={t} value={t}>
                              {t}
                            </option>
                          ))}
                        </select>
                        <button
                          type="button"
                          className="mkt-slot-remove"
                          aria-label="Xóa buổi"
                          onClick={() => removeSlot(idx)}
                        >
                          ×
                        </button>
                      </div>
                    );
                  })}
                <button type="button" className="mkt-btn mkt-btn--ghost mkt-btn--sm" onClick={() => addSlot(sid)}>
                  + Thêm buổi
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Tổng học phí (học phí/giờ nhập theo từng môn ở trên) */}
      <div className="mkt-field">
        <span className="mkt-field__label">Tổng học phí ước tính ({cycleLabelDisplay})</span>
        <div className="mkt-total">
          {currency.format(total)} {cycleSuffix}
        </div>
        <span className="mkt-hint">
          {hoursPerWeek} giờ/tuần · {form.slots.length} buổi/tuần — Tổng: {hoursPerWeek * weeks} giờ ·{' '}
          {form.slots.length * weeks} buổi.
        </span>
      </div>

      {/* Ghi chú bổ sung */}
      <label className="mkt-field">
        <span className="mkt-field__label">
          Ghi chú / yêu cầu khác <em>*</em>
        </span>
        <textarea
          rows={3}
          value={form.note}
          placeholder="Mô tả thêm về nhu cầu, lịch học mong muốn, tính cách học sinh…"
          onChange={(e) => set('note', e.target.value)}
        />
      </label>

      {touched && conflicts.length > 0 && (
        <div className="mkt-alert mkt-alert--error">{conflicts.join('. ')}.</div>
      )}
      {touched && slotErrors.length > 0 && (
        <div className="mkt-alert mkt-alert--error">{slotErrors.join('. ')}.</div>
      )}
      {touched && missing.length > 0 && (
        <div className="mkt-alert mkt-alert--error">Vui lòng nhập: {missing.join(', ')}.</div>
      )}
      {error && <div className="mkt-alert mkt-alert--error">{error}</div>}

      <div className="mkt-form__actions">
        <button
          type="button"
          className="mkt-btn mkt-btn--ghost"
          onClick={onCancel}
          disabled={submitting}
        >
          Hủy
        </button>
        <button
          type="button"
          className="mkt-btn mkt-btn--primary"
          onClick={handleSubmit}
          disabled={submitting}
        >
          {submitting ? 'Đang lưu…' : isEdit ? 'Lưu thay đổi' : 'Tạo lớp'}
        </button>
      </div>
    </div>
  );
}
