import {
  useEffect,
  useMemo,
  useState,
  type ClipboardEvent,
  type FormEvent,
  type ReactNode,
} from 'react';
import {
  BILLING_CYCLE_OPTIONS,
  DAY_OF_WEEK_OPTIONS,
  DURATION_CHOICE_OPTIONS,
  FEE_PER_HOUR_MIN,
  FEE_PER_HOUR_STEP,
  LEARNING_GOAL_OTHER,
  LESSON_MODE_OPTIONS,
  isOtherSubject,
  newOtherSubjectId,
  REPEAT_WEEKS_OPTIONS,
  SCHEDULE_MODE_OPTIONS,
  SESSION_OPTIONS,
  TUTOR_REQUIREMENT_OPTIONS,
  type CatalogOption,
  type ClassFormValues,
  type DurationChoice,
  type ClassRequestPayload,
  type LessonMode,
  type ScheduleMode,
  type ScheduleSlot,
} from '../types/marketplaceTypes';
import {
  cycleLabelOf,
  formToPayload,
  patternRepeats,
  repeatLabel,
  repeatWeeksOf,
  restWeeksOf,
  studyWeeksOf,
  totalBudget,
  totalHoursPerRepeat,
  weeksForCycle,
} from '../mappers/marketplaceMapper';
import { LocationPicker, type LocationValue } from '../../center/components/LocationPicker';
import '../pages/MarketplacePage.css';

interface ClassRequestFormProps {
  readonly initial: ClassFormValues;
  readonly subjects: CatalogOption[];
  readonly grades: CatalogOption[];
  readonly isEdit: boolean;
  readonly submitting: boolean;
  readonly error: string | null;
  readonly onSubmit: (payload: ClassRequestPayload) => void;
  readonly onCancel: () => void;
  readonly submitLabel?: string;
  readonly extraContent?: ReactNode;
  /**
   * Khi true: bỏ lưới môn cố định, phụ huynh TỰ ĐIỀN tên môn (kiểu ô "Khác").
   * Dùng cho luồng "nhờ trung tâm tìm gia sư" — trung tâm đọc tên môn tự do, không cần map danh mục.
   */
  readonly freeTextSubjects?: boolean;
}

const currency = new Intl.NumberFormat('vi-VN');

function buildTimeSlots(min: string, max: string, stepMinutes = 30): string[] {
  const toMinutes = (t: string) => {
    const [h, m] = t.split(':').map(Number);
    return h * 60 + m;
  };
  const out: string[] = [];
  for (let x = toMinutes(min); x <= toMinutes(max); x += stepMinutes) {
    out.push(`${String(Math.floor(x / 60)).padStart(2, '0')}:${String(x % 60).padStart(2, '0')}`);
  }
  if (out.length > 0 && out[out.length - 1] !== max) out.push(max);
  return out;
}

const MIDNIGHT_END = '23:59';
function fmtTime(t: string): string {
  return t === MIDNIGHT_END ? '00:00' : t;
}

function emptySlot(subjectId: string): ScheduleSlot {
  return { subjectId, day: '', date: '', session: '', start: '', end: '' };
}

const WEEKLY_DEFAULT_START = '18:00';
const WEEKLY_DEFAULT_END = '20:00';

function sessionFromStart(start: string): string {
  if (!start) return '';
  if (start < '12:00') return 'Sáng';
  if (start < '18:00') return 'Chiều';
  return 'Tối';
}

function toMinutes(t: string): number {
  const [h, m] = t.split(':').map(Number);
  return h * 60 + m;
}

function minutesToTime(mins: number): string {
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

function blockNonDigits(e: FormEvent<HTMLInputElement>) {
  const data = (e.nativeEvent as InputEvent).data;
  if (data && /\D/.test(data)) e.preventDefault();
}

function busyRangesOf(
  slots: ScheduleSlot[],
  index: number,
  key: 'day' | 'date',
  when: string,
): { start: string; end: string }[] {
  if (!when) return [];
  return slots
    .filter((o, i) => i !== index && o[key] === when && !!o.start && !!o.end)
    .map((o) => ({ start: o.start, end: o.end }));
}

function sessionFullyBusy(
  session: (typeof SESSION_OPTIONS)[number],
  busy: { start: string; end: string }[],
): boolean {
  const pool = buildTimeSlots(session.min, session.max).slice(0, -1);
  return pool.every((t) => busy.some((b) => b.start <= t && t < b.end));
}

function durationLabel(start: string, end: string): string {
  if (!start || !end || end <= start) return '';
  const endMin = end === MIDNIGHT_END ? 24 * 60 : toMinutes(end);
  const mins = endMin - toMinutes(start);
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  const parts = [];
  if (h) parts.push(`${h} giờ`);
  if (m) parts.push(`${m} phút`);
  return parts.join(' ');
}

const UNIVERSITY_ENTRANCE_SUBJECTS = [
  'Toán',
  'Vật lý',
  'Hóa học',
  'Sinh học',
  'Ngữ văn',
  'Tiếng Anh',
  'Lịch sử',
  'Địa lý',
];

function gradeMatchesSubjects(gradeName: string, subjectNames: string[]): boolean {
  const isCert = gradeName.toLowerCase().includes('chứng chỉ');
  const isUniversity = gradeName.includes('Đại học');
  if (!isCert && !isUniversity) return true;
  if (subjectNames.length === 0) return true;
  if (isCert) {
    return subjectNames.some((s) => s === 'Tiếng Anh' || s.toLowerCase().includes('chứng chỉ'));
  }
  return subjectNames.some((s) => UNIVERSITY_ENTRANCE_SUBJECTS.includes(s));
}

export function ClassRequestForm({
  initial,
  subjects,
  grades,
  isEdit,
  submitting,
  error,
  onSubmit,
  onCancel,
  submitLabel,
  extraContent,
  freeTextSubjects = false,
}: ClassRequestFormProps) {
  const [form, setForm] = useState<ClassFormValues>(initial);
  const [touched, setTouched] = useState(false);

  // Chế độ tự-điền môn: đảm bảo luôn có sẵn ít nhất 1 ô nhập tên môn.
  useEffect(() => {
    if (freeTextSubjects && form.subjectIds.filter(isOtherSubject).length === 0) {
      addOtherSubject();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [freeTextSubjects]);

  function set<K extends keyof ClassFormValues>(key: K, value: ClassFormValues[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function setDurationChoice(choice: DurationChoice) {
    setForm((prev) => {
      switch (choice) {
        case 'YEAR_FREE':
          return { ...prev, billingCycle: 'MONTH', durationUnit: 'YEAR' };
        case 'TERM':
          return { ...prev, billingCycle: 'TERM', durationUnit: 'MONTH' };
        case 'QUARTER':
          return { ...prev, billingCycle: 'QUARTER', durationUnit: 'MONTH' };
        default:
          return { ...prev, billingCycle: 'MONTH', durationUnit: 'MONTH' };
      }
    });
  }

  function setScheduleMode(mode: ScheduleMode) {
    setForm((prev) => {
      if (prev.scheduleMode === mode) return prev;
      const slots = prev.slots.filter((s) => (mode === 'WEEKLY' ? !!s.day : !!s.date));
      return { ...prev, scheduleMode: mode, slots };
    });
  }

  const subjName = (id: string) =>
    isOtherSubject(id)
      ? form.subjectOthers[id]?.trim() || 'Môn học khác'
      : (subjects.find((s) => String(s.id) === id)?.name ?? id);
  const otherIds = form.subjectIds.filter(isOtherSubject);
  const dayLabel = (v: string) => DAY_OF_WEEK_OPTIONS.find((d) => d.value === v)?.label ?? v;
  const isWeekly = form.scheduleMode === 'WEEKLY';
  const knownSubjectNames = form.subjectIds.filter((id) => !isOtherSubject(id)).map(subjName);
  const hasUnknownSubject = form.subjectIds.some(isOtherSubject);
  // Môn tự-điền (nhờ trung tâm) hoặc có môn "khác" -> không lọc, hiện đầy đủ danh sách lớp.
  const visibleGrades =
    freeTextSubjects || hasUnknownSubject
      ? grades
      : grades.filter((g) => gradeMatchesSubjects(g.name, knownSubjectNames));
  function handleGradeChange(gradeId: string) {
    setForm((prev) => ({ ...prev, gradeId }));
  }

  const locationValue: LocationValue = {
    province: form.provinceName,
    ward: form.wardName ?? '',
    addressDetail: form.address,
  };

  function handleLocationChange(v: LocationValue) {
    setForm((prev) => ({
      ...prev,
      provinceName: v.province,
      wardName: v.ward,
      address: v.addressDetail,
      provinceId: '',
      districtId: '',
      districtName: '',
      wardId: '',
    }));
  }

  function toggleSubject(id: string) {
    setForm((prev) => {
      const removing = prev.subjectIds.includes(id);
      const subjectIds = removing
        ? prev.subjectIds.filter((s) => s !== id)
        : [...prev.subjectIds, id];
      const slots = removing
        ? prev.slots.filter((s) => s.subjectId !== id)
        : prev.scheduleMode === 'WEEKLY'
          ? prev.slots
          : [...prev.slots, emptySlot(id)];
      const subjectFees = { ...prev.subjectFees };
      if (removing) delete subjectFees[id];
      else if (!(id in subjectFees)) subjectFees[id] = '';
      const knownNames = subjectIds.filter((s) => !isOtherSubject(s)).map(subjName);
      const hasUnknown = subjectIds.some(isOtherSubject);
      const gName = grades.find((g) => String(g.id) === prev.gradeId)?.name;
      const gradeId =
        prev.gradeId && !hasUnknown && gName && !gradeMatchesSubjects(gName, knownNames)
          ? ''
          : prev.gradeId;
      return { ...prev, subjectIds, slots, subjectFees, gradeId };
    });
  }

  function addOtherSubject() {
    setForm((prev) => {
      const id = newOtherSubjectId();
      const slots =
        prev.scheduleMode === 'WEEKLY' ? prev.slots : [...prev.slots, emptySlot(id)];
      return {
        ...prev,
        subjectIds: [...prev.subjectIds, id],
        subjectOthers: { ...prev.subjectOthers, [id]: '' },
        subjectFees: { ...prev.subjectFees, [id]: '' },
        slots,
      };
    });
  }

  function removeOtherSubject(id: string) {
    setForm((prev) => {
      const subjectOthers = { ...prev.subjectOthers };
      const subjectFees = { ...prev.subjectFees };
      delete subjectOthers[id];
      delete subjectFees[id];
      return {
        ...prev,
        subjectIds: prev.subjectIds.filter((s) => s !== id),
        subjectOthers,
        subjectFees,
        slots: prev.slots.filter((s) => s.subjectId !== id),
      };
    });
  }

  function setOtherName(id: string, name: string) {
    setForm((prev) => ({ ...prev, subjectOthers: { ...prev.subjectOthers, [id]: name } }));
  }

  function toggleOtherSection() {
    const otherSet = new Set(form.subjectIds.filter(isOtherSubject));
    if (otherSet.size === 0) {
      addOtherSubject();
      return;
    }
    setForm((prev) => {
      const subjectOthers = { ...prev.subjectOthers };
      const subjectFees = { ...prev.subjectFees };
      otherSet.forEach((id) => {
        delete subjectOthers[id];
        delete subjectFees[id];
      });
      return {
        ...prev,
        subjectIds: prev.subjectIds.filter((s) => !isOtherSubject(s)),
        subjectOthers,
        subjectFees,
        slots: prev.slots.filter((s) => !otherSet.has(s.subjectId)),
      };
    });
  }

  function setSubjectFee(subjectId: string, value: string) {
    const digits = value.replace(/\D/g, '');
    setForm((prev) => ({
      ...prev,
      subjectFees: { ...prev.subjectFees, [subjectId]: digits },
    }));
  }

  function addSlot(subjectId: string) {
    setForm((prev) => ({ ...prev, slots: [...prev.slots, emptySlot(subjectId)] }));
  }

  function toggleWeekday(subjectId: string, day: string) {
    setForm((prev) => {
      const has = prev.slots.some((s) => s.subjectId === subjectId && s.day === day);
      if (has) {
        return {
          ...prev,
          slots: prev.slots.filter((s) => !(s.subjectId === subjectId && s.day === day)),
        };
      }
      const busy = busyRangesOf(prev.slots, -1, 'day', day);
      const shared = prev.slots.find((s) => s.subjectId === subjectId);
      let start = shared?.start || WEEKLY_DEFAULT_START;
      let end = shared?.end || WEEKLY_DEFAULT_END;
      const overlaps = (a: string, b: string) => busy.some((r) => a < r.end && r.start < b);
      if (overlaps(start, end)) {
        const span = toMinutes(end) - toMinutes(start);
        const free = buildTimeSlots('06:00', '23:30').find(
          (t) => toMinutes(t) + span <= toMinutes('23:30') && !overlaps(t, minutesToTime(toMinutes(t) + span)),
        );
        if (!free) return prev;
        start = free;
        end = minutesToTime(toMinutes(free) + span);
      }
      return {
        ...prev,
        slots: [
          ...prev.slots,
          { subjectId, day, date: '', session: sessionFromStart(start), start, end },
        ],
      };
    });
  }

  function setSlotTime(index: number, patch: { start?: string; end?: string }) {
    setForm((prev) => ({
      ...prev,
      slots: prev.slots.map((s, i) => {
        if (i !== index) return s;
        const start = patch.start ?? s.start;
        let end = patch.end ?? s.end;
        if (end && start && end <= start) end = '';
        return { ...s, start, end, session: start ? sessionFromStart(start) : s.session };
      }),
    }));
  }

  function toggleStudyWeek(week: number) {
    setForm((prev) => {
      const cur = studyWeeksOf(prev);
      const next = cur.includes(week) ? cur.filter((w) => w !== week) : [...cur, week];
      if (next.length === 0) return prev;
      return { ...prev, studyWeeks: next.sort((a, b) => a - b) };
    });
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

  function setSlotSession(index: number, session: string) {
    const preset = SESSION_OPTIONS.find((s) => s.value === session);
    if (!preset) {
      updateSlot(index, { session, start: '', end: '' });
      return;
    }
    setForm((prev) => {
      const cur = prev.slots[index];
      const weekly = prev.scheduleMode === 'WEEKLY';
      const blocked = prev.slots.some(
        (o, i) =>
          i !== index &&
          (weekly ? !!o.day && o.day === cur.day : !!o.date && o.date === cur.date) &&
          !!o.start &&
          !!o.end &&
          preset.start < o.end &&
          o.start < preset.end,
      );
      const past =
        !weekly && cur.date === new Date().toLocaleDateString('en-CA')
          ? preset.start <= new Date().toTimeString().slice(0, 5)
          : false;
      const patch =
        blocked || past
          ? { session, start: '', end: '' }
          : { session, start: preset.start, end: preset.end };
      return { ...prev, slots: prev.slots.map((s, i) => (i === index ? { ...s, ...patch } : s)) };
    });
  }

  const cycle =
    BILLING_CYCLE_OPTIONS.find((o) => o.value === form.billingCycle) ?? BILLING_CYCLE_OPTIONS[0];
  const isMonth = form.billingCycle === 'MONTH';
  const isYearUnit = isMonth && form.durationUnit === 'YEAR';
  const durationMax = isYearUnit ? 10 : 12;
  const durationChoice: DurationChoice = isMonth
    ? isYearUnit
      ? 'YEAR_FREE'
      : 'MONTH_FREE'
    : form.billingCycle === 'QUARTER'
      ? 'QUARTER'
      : 'TERM';
  const repeats = patternRepeats(form);
  const cycleName = cycleLabelOf(form);
  // Chọn ngày cụ thể: tổng là số tiền cố định cho các buổi đã chọn, không theo chu kỳ.
  const cycleLabelDisplay = !isWeekly ? 'các buổi đã chọn' : isMonth ? cycleName : cycle.short;
  const cycleSuffix = !isWeekly ? 'đ' : `đ / ${isMonth ? cycleName : cycle.short}`;
  const perRepeatUnit = repeatWeeksOf(form) === 1 ? 'tuần' : 'tuần học';
  const studyWeeks = studyWeeksOf(form);
  const restWeeks = restWeeksOf(form);

  const hoursPerRepeat = useMemo(() => totalHoursPerRepeat(form), [form]);
  const total = useMemo(() => totalBudget(form), [form]);

  const isOffline = form.lessonMode !== 'ONLINE';
  const today = new Date().toLocaleDateString('en-CA');
  const nowHm = new Date().toTimeString().slice(0, 5);
  const todayDow = ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'][new Date().getDay()];

  const slotErrorSet = new Set<string>();
  form.slots.forEach((s) => {
    const nm = subjName(s.subjectId);
    const whenMissing = isWeekly ? !s.day : !s.date;
    if (whenMissing || !s.session || !s.start || !s.end) {
      slotErrorSet.add(`${nm}: có buổi chưa đủ thông tin (${isWeekly ? 'thứ' : 'ngày'} / buổi / giờ)`);
    } else if (!isWeekly && s.date < today) {
      slotErrorSet.add(`${nm}: ngày học không được ở quá khứ`);
    } else if (!isWeekly && s.date === today && s.start <= nowHm) {
      slotErrorSet.add(`${nm}: giờ học hôm nay đã qua (phải sau ${nowHm})`);
    } else if (isWeekly && s.day === todayDow && s.start <= nowHm) {
      slotErrorSet.add(
        `${nm}: buổi ${dayLabel(s.day)} ${s.start} đã qua giờ hôm nay — chọn thứ hoặc giờ khác`,
      );
    } else if (s.end <= s.start) {
      slotErrorSet.add(`${nm}: giờ kết thúc phải sau giờ bắt đầu`);
    }
  });
  for (const sid of form.subjectIds) {
    if (!form.slots.some((s) => s.subjectId === sid)) {
      slotErrorSet.add(`${subjName(sid)}: chưa có buổi học nào`);
    }
    const fee = Number(form.subjectFees[sid]);
    if (!(fee > 0)) {
      slotErrorSet.add(`${subjName(sid)}: chưa nhập học phí/giờ`);
    } else if (fee < FEE_PER_HOUR_MIN) {
      slotErrorSet.add(
        `${subjName(sid)}: học phí/giờ tối thiểu ${currency.format(FEE_PER_HOUR_MIN)}đ`,
      );
    }
  }
  const slotErrors = [...slotErrorSet];

  const conflicts: string[] = [];
  for (let i = 0; i < form.slots.length; i++) {
    for (let j = i + 1; j < form.slots.length; j++) {
      const a = form.slots[i];
      const b = form.slots[j];
      const sameWhen = isWeekly ? !!a.day && a.day === b.day : !!a.date && a.date === b.date;
      if (sameWhen && a.start && a.end && b.start && b.end && a.start < b.end && b.start < a.end) {
        const when = isWeekly ? dayLabel(a.day) : `ngày ${a.date}`;
        conflicts.push(
          `Trùng giờ ${when}: ${subjName(a.subjectId)} (${fmtTime(a.start)}–${fmtTime(a.end)}) & ${subjName(
            b.subjectId,
          )} (${fmtTime(b.start)}–${fmtTime(b.end)})`,
        );
      }
    }
  }

  const missing: string[] = [];
  if (form.subjectIds.length === 0) missing.push('Môn học');
  if (otherIds.some((id) => !form.subjectOthers[id]?.trim()))
    missing.push('Tên môn học khác');
  if (!form.gradeId) missing.push('Lớp');
  if (isOffline) {
    if (!form.provinceName) missing.push('Tỉnh / Thành phố');
    if (!form.wardName) missing.push('Phường / Xã');
    if (!form.address.trim()) missing.push('Địa chỉ cụ thể');
  }
  if (form.slots.length === 0) missing.push('Lịch học');

  function handleSubmit() {
    setTouched(true);
    if (missing.length > 0 || slotErrors.length > 0 || conflicts.length > 0) return;
    onSubmit(formToPayload(form, subjects));
  }

  return (
    <div className="mkt-form">
      <div className="mkt-field">
        <span className="mkt-field__label">
          Môn học <em>*</em>
          <span className="mkt-field__hint-inline">
            {freeTextSubjects ? ' (tự điền tên môn, có thể nhiều môn)' : ' (có thể chọn nhiều môn)'}
          </span>
        </span>
        {!freeTextSubjects &&
          (subjects.length === 0 ? (
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
                <input type="checkbox" checked={otherIds.length > 0} onChange={toggleOtherSection} />
                <span>Khác</span>
              </label>
            </div>
          ))}
        {(freeTextSubjects || otherIds.length > 0) && (
          <div className="mkt-field mkt-subject-other">
            {!freeTextSubjects && (
              <span className="mkt-field__label">
                Tên môn học khác <em>*</em> <span className="mkt-hint">(mỗi ô 1 môn)</span>
              </span>
            )}
            {otherIds.map((id, i) => (
              <div key={id} className="mkt-other-row">
                <input
                  type="text"
                  value={form.subjectOthers[id] ?? ''}
                  placeholder={freeTextSubjects ? `Tên môn ${i + 1}… (VD: Toán, Tiếng Anh)` : `Tên môn khác ${i + 1}…`}
                  onChange={(e) => setOtherName(id, e.target.value)}
                />
                <button
                  type="button"
                  className="mkt-other-remove"
                  aria-label="Xóa môn này"
                  onClick={() => removeOtherSubject(id)}
                >
                  ✕
                </button>
              </div>
            ))}
            <button type="button" className="mkt-btn mkt-btn--ghost mkt-other-add" onClick={addOtherSubject}>
              + Thêm môn
            </button>
            {touched && otherIds.some((id) => !form.subjectOthers[id]?.trim()) && (
              <span className="mkt-field__error">Nhập tên môn học khác</span>
            )}
          </div>
        )}
        {touched && form.subjectIds.length === 0 && (
          <span className="mkt-field__error">Chọn ít nhất một môn học</span>
        )}
      </div>

      <div className="mkt-form__grid">
        <label className="mkt-field">
          <span className="mkt-field__label">
            Lớp <em>*</em>
          </span>
          <select value={form.gradeId} onChange={(e) => handleGradeChange(e.target.value)}>
            <option value="">-- Chọn lớp --</option>
            {visibleGrades.map((g) => (
              <option key={g.id} value={g.id}>
                {g.name}
              </option>
            ))}
          </select>
          {touched && !form.gradeId && <span className="mkt-field__error">Chọn lớp</span>}
        </label>

        <label className="mkt-field">
          <span className="mkt-field__label">Mục tiêu học tập</span>
          <input
            type="text"
            value={form.learningGoal === LEARNING_GOAL_OTHER ? form.learningGoalOther : form.learningGoal}
            onChange={(e) =>
              setForm((prev) => ({ ...prev, learningGoal: e.target.value, learningGoalOther: '' }))
            }
            placeholder="VD: Lấy lại gốc, ôn thi vào 10, luyện IELTS…"
          />
        </label>
      </div>

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

      {isOffline && (
        <div className="mkt-field">
          <span className="mkt-field__label">
            Địa điểm <em>*</em>
          </span>
          <LocationPicker
            value={locationValue}
            onChange={handleLocationChange}
            showErrors={touched}
            errors={{
              province: !form.provinceName ? 'Chọn Tỉnh/Thành phố' : undefined,
              ward: !form.wardName ? 'Chọn Phường/Xã' : undefined,
              addressDetail: !form.address.trim() ? 'Nhập địa chỉ cụ thể' : undefined,
            }}
          />
        </div>
      )}

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
          <span className="mkt-field__label">Yêu cầu bổ sung (chứng chỉ, bằng cấp…)</span>
          <input
            type="text"
            value={form.tutorRequirementDetail}
            placeholder="VD: Có chứng chỉ IELTS 7.0"
            onChange={(e) => set('tutorRequirementDetail', e.target.value)}
          />
        </label>
      </div>

      <div className="mkt-form__grid">
        <label className="mkt-field">
          <span className="mkt-field__label">Chọn thời gian học</span>
          <select
            value={durationChoice}
            onChange={(e) => setDurationChoice(e.target.value as DurationChoice)}
          >
            {DURATION_CHOICE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </label>
        {isMonth && (
          <label className="mkt-field">
            <span className="mkt-field__label">{isYearUnit ? 'Số năm học' : 'Số tháng học'}</span>
            <div className="mkt-input-suffix">
              <input
                type="number"
                min={1}
                max={durationMax}
                step={1}
                value={form.months}
                placeholder={isYearUnit ? 'VD: 1' : 'VD: 3'}
                onChange={(e) => {
                  const raw = e.target.value;
                  if (raw === '') return set('months', '');
                  const n = Math.trunc(Number(raw));
                  if (!Number.isFinite(n)) return;
                  if (!isYearUnit && n >= 12) {
                    setForm((prev) => ({
                      ...prev,
                      billingCycle: 'MONTH',
                      durationUnit: 'YEAR',
                      months: '1',
                    }));
                    return;
                  }
                  set('months', String(Math.min(durationMax, Math.max(1, n))));
                }}
              />
              <span className="mkt-input-suffix__unit">{isYearUnit ? 'năm' : 'tháng'}</span>
            </div>
          </label>
        )}
      </div>

      <div className="mkt-field">
        <span className="mkt-field__label">Kiểu lịch học</span>
        <div className="mkt-radios">
          {SCHEDULE_MODE_OPTIONS.map((opt) => (
            <label key={opt.value} className="mkt-radio">
              <input
                type="radio"
                name="scheduleMode"
                checked={form.scheduleMode === opt.value}
                onChange={() => setScheduleMode(opt.value as ScheduleMode)}
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

      {isWeekly && (
        <div className="mkt-form__grid">
          <label className="mkt-field">
            <span className="mkt-field__label">Tần suất lặp</span>
            <select
              value={form.repeatEveryWeeks}
              onChange={(e) => set('repeatEveryWeeks', e.target.value)}
            >
              {REPEAT_WEEKS_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
            <span className="mkt-hint">
              {repeatWeeksOf(form) === 1
                ? 'Lịch lặp lại đều mỗi tuần, không có tuần nghỉ.'
                : `Chu kỳ ${repeatWeeksOf(form)} tuần lặp đi lặp lại đến hết ${cycleName}.`}
            </span>
          </label>
          {repeatWeeksOf(form) > 1 && (
            <div className="mkt-field">
              <span className="mkt-field__label">Tuần học trong chu kỳ</span>
              <div className="mkt-wdays mkt-wdays--weeks">
                {Array.from({ length: repeatWeeksOf(form) }, (_, i) => i + 1).map((w) => {
                  const on = studyWeeks.includes(w);
                  const locked = on && studyWeeks.length === 1;
                  return (
                    <button
                      key={w}
                      type="button"
                      className={`mkt-wday${on ? ' mkt-wday--on' : ''}`}
                      aria-pressed={on}
                      aria-label={`Tuần ${w}`}
                      disabled={locked}
                      title={
                        locked
                          ? `Tuần ${w}: phải giữ ít nhất một tuần học`
                          : `Tuần ${w}: ${on ? 'học — bấm để nghỉ' : 'nghỉ — bấm để học'}`
                      }
                      onClick={() => toggleStudyWeek(w)}
                    >
                      {w}
                    </button>
                  );
                })}
              </div>
              <span className="mkt-hint">
                {`Học tuần ${studyWeeks.join(', ')}`}
                {restWeeks.length > 0 ? ` · nghỉ tuần ${restWeeks.join(', ')}` : ''}
                {' — hết chu kỳ lại lặp lại như vậy.'}
              </span>
            </div>
          )}
        </div>
      )}

      <div className="mkt-field">
        <span className="mkt-field__label">
          Lịch học theo môn <em>*</em>
          <span className="mkt-field__hint-inline"> (các buổi không được trùng giờ)</span>
        </span>
        {touched && form.subjectIds.length > 0 && form.slots.length === 0 && (
          <span className="mkt-field__error">Thêm ít nhất một buổi học</span>
        )}
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
                      min={FEE_PER_HOUR_MIN}
                      step={FEE_PER_HOUR_STEP}
                      value={form.subjectFees[sid] ?? ''}
                      placeholder={`Từ ${currency.format(FEE_PER_HOUR_MIN)}`}
                      aria-label={`Học phí/giờ môn ${subjName(sid)}`}
                      title={`Học phí/giờ tối thiểu ${currency.format(FEE_PER_HOUR_MIN)}đ`}
                      onBeforeInput={blockNonDigits}
                      onPaste={(e: ClipboardEvent<HTMLInputElement>) => {
                        e.preventDefault();
                        const digits = e.clipboardData.getData('text').replace(/\D/g, '');
                        if (digits) setSubjectFee(sid, digits);
                      }}
                      onChange={(e) => setSubjectFee(sid, e.target.value)}
                    />
                    <span className="mkt-subj-fee__unit">đ/giờ</span>
                  </span>
                </div>
                {isWeekly ? (
                  (() => {
                    const allTimes = buildTimeSlots('06:00', '23:30');
                    const rows = form.slots
                      .map((slot, idx) => ({ slot, idx }))
                      .filter((x) => x.slot.subjectId === sid)
                      .sort(
                        (a, b) =>
                          DAY_OF_WEEK_OPTIONS.findIndex((d) => d.value === a.slot.day) -
                          DAY_OF_WEEK_OPTIONS.findIndex((d) => d.value === b.slot.day),
                      );
                    return (
                      <>
                        <div className="mkt-wday-row">
                          <span className="mkt-wday-row__label">Học vào</span>
                          <div className="mkt-wdays">
                            {DAY_OF_WEEK_OPTIONS.map((d) => {
                              const on = form.slots.some(
                                (s) => s.subjectId === sid && s.day === d.value,
                              );
                              const dayBusy = busyRangesOf(form.slots, -1, 'day', d.value);
                              const full = SESSION_OPTIONS.every((o) => sessionFullyBusy(o, dayBusy));
                              return (
                                <button
                                  key={d.value}
                                  type="button"
                                  className={`mkt-wday${on ? ' mkt-wday--on' : ''}`}
                                  aria-pressed={on}
                                  aria-label={d.label}
                                  disabled={!on && full}
                                  title={
                                    !on && full
                                      ? `${d.label}: các môn khác đã kín lịch cả ngày`
                                      : d.label
                                  }
                                  onClick={() => toggleWeekday(sid, d.value)}
                                >
                                  {d.value}
                                </button>
                              );
                            })}
                          </div>
                        </div>
                        {rows.map(({ slot, idx }) => {
                          const sess = SESSION_OPTIONS.find((o) => o.value === slot.session);
                          const pool = sess ? buildTimeSlots(sess.min, sess.max) : allTimes;
                          const busy = busyRangesOf(form.slots, idx, 'day', slot.day);
                          const startTimes = pool
                            .slice(0, -1)
                            .filter((t) => !busy.some((b) => b.start <= t && t < b.end));
                          const nextBusyStart = busy
                            .map((b) => b.start)
                            .filter((s) => s >= slot.start)
                            .sort()[0];
                          const endTimes = pool.filter(
                            (t) =>
                              (!slot.start || t > slot.start) &&
                              (!nextBusyStart || t <= nextBusyStart),
                          );
                          if (slot.end && !endTimes.includes(slot.end)) {
                            endTimes.push(slot.end);
                            endTimes.sort();
                          }
                          return (
                            <div key={slot.day} className="mkt-wday-row">
                              <span className="mkt-wday-row__label">{dayLabel(slot.day)}</span>
                              <select
                                className="mkt-day-time__session"
                                aria-label={`Buổi ${dayLabel(slot.day)}`}
                                value={slot.session}
                                onChange={(e) => setSlotSession(idx, e.target.value)}
                              >
                                {SESSION_OPTIONS.map((s) => {
                                  const full = sessionFullyBusy(s, busy);
                                  return (
                                    <option key={s.value} value={s.value} disabled={full}>
                                      {full ? `${s.label} — đã kín lịch` : s.label}
                                    </option>
                                  );
                                })}
                              </select>
                              <select
                                className="mkt-day-time__session"
                                aria-label={`Giờ bắt đầu ${dayLabel(slot.day)}`}
                                value={slot.start}
                                onChange={(e) => setSlotTime(idx, { start: e.target.value })}
                              >
                                <option value="">Từ…</option>
                                {startTimes.map((t) => (
                                  <option key={t} value={t}>
                                    {t}
                                  </option>
                                ))}
                              </select>
                              <span className="mkt-day-time__sep">–</span>
                              <select
                                className="mkt-day-time__session mkt-day-time__session--wide"
                                aria-label={`Giờ kết thúc ${dayLabel(slot.day)}`}
                                value={slot.end}
                                onChange={(e) => setSlotTime(idx, { end: e.target.value })}
                              >
                                <option value="">Đến…</option>
                                {endTimes.map((t) => (
                                  <option key={t} value={t}>
                                    {slot.start ? `${fmtTime(t)} (${durationLabel(slot.start, t)})` : fmtTime(t)}
                                  </option>
                                ))}
                              </select>
                            </div>
                          );
                        })}
                        <div className="mkt-wsummary">
                          {rows.length === 0
                            ? 'Chưa chọn ngày học nào'
                            : `${repeatLabel(form).replace(/^./, (c) => c.toUpperCase())}: ${rows
                                .map(
                                  ({ slot }) =>
                                    `${dayLabel(slot.day)}${
                                      slot.start && slot.end ? ` ${fmtTime(slot.start)}–${fmtTime(slot.end)}` : ''
                                    }`,
                                )
                                .join(' · ')}`}
                        </div>
                      </>
                    );
                  })()
                ) : (
                  <>
                    {form.slots
                      .map((slot, idx) => ({ slot, idx }))
                      .filter((x) => x.slot.subjectId === sid)
                      .map(({ slot, idx }) => {
                        const sess = SESSION_OPTIONS.find((o) => o.value === slot.session);
                        const sessTimes = buildTimeSlots(sess?.min ?? '00:00', sess?.max ?? '23:30');
                        const dayTimes =
                          slot.date === today ? sessTimes.filter((t) => t > nowHm) : sessTimes;
                        const busy = busyRangesOf(form.slots, idx, 'date', slot.date);
                        const times = dayTimes.filter(
                          (t) => !busy.some((b) => b.start <= t && t < b.end),
                        );
                        const nextBusyStart = busy
                          .map((b) => b.start)
                          .filter((s) => s >= slot.start)
                          .sort()[0];
                        const endTimes = slot.start
                          ? dayTimes.filter(
                              (t) => t > slot.start && (!nextBusyStart || t <= nextBusyStart),
                            )
                          : times;
                        return (
                          <div key={idx} className="mkt-slot-row">
                            <input
                              type="date"
                              className="mkt-slot-date"
                              aria-label="Ngày học"
                              min={today}
                              value={slot.date}
                              onChange={(e) => {
                                const date = e.target.value;
                                const past = date === today && !!slot.start && slot.start <= nowHm;
                                const clash =
                                  !!slot.start &&
                                  !!slot.end &&
                                  form.slots.some(
                                    (o, i) =>
                                      i !== idx &&
                                      o.date === date &&
                                      !!o.start &&
                                      !!o.end &&
                                      slot.start < o.end &&
                                      o.start < slot.end,
                                  );
                                updateSlot(
                                  idx,
                                  past || clash ? { date, start: '', end: '' } : { date },
                                );
                              }}
                            />
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
                              {times
                                .filter((t) => t !== MIDNIGHT_END)
                                .map((t) => (
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
                                  {slot.start ? `${fmtTime(t)} (${durationLabel(slot.start, t)})` : fmtTime(t)}
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
                    <button
                      type="button"
                      className="mkt-btn mkt-btn--ghost mkt-btn--sm"
                      onClick={() => addSlot(sid)}
                    >
                      + Thêm buổi
                    </button>
                  </>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      <div className="mkt-field">
        <span className="mkt-field__label">Tổng học phí ({cycleLabelDisplay})</span>
        <div className="mkt-total">
          {currency.format(total)} {cycleSuffix}
        </div>
        <span className="mkt-hint">
          {isWeekly ? (
            <>
              {hoursPerRepeat} giờ/{perRepeatUnit} · {form.slots.length} buổi/{perRepeatUnit} — Tổng:{' '}
              {hoursPerRepeat * repeats} giờ · {form.slots.length * repeats} buổi ({repeats} tuần học
              {repeatWeeksOf(form) > 1 ? ` trong ${weeksForCycle(form)} tuần` : ''}).
            </>
          ) : (
            <>
              {form.slots.length} buổi đã chọn · Tổng {hoursPerRepeat} giờ (không lặp lịch).
            </>
          )}
        </span>
      </div>

      <label className="mkt-field">
        <span className="mkt-field__label">Ghi chú / yêu cầu khác</span>
        <textarea
          rows={3}
          value={form.note}
          placeholder="Mô tả thêm về nhu cầu, lịch học mong muốn, tính cách học sinh…"
          onChange={(e) => set('note', e.target.value)}
        />
      </label>

      {conflicts.length > 0 && (
        <div className="mkt-alert mkt-alert--error">{conflicts.join('. ')}.</div>
      )}
      {extraContent}
      {touched && slotErrors.length > 0 && (
        <div className="mkt-alert mkt-alert--error">{slotErrors.join('. ')}.</div>
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
          {submitting
            ? 'Đang lưu…'
            : submitLabel ?? (isEdit ? 'Lưu thay đổi' : 'Tạo lớp')}
        </button>
      </div>
    </div>
  );
}
