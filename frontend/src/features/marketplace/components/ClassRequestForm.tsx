import { useEffect, useMemo, useState, type ClipboardEvent, type FormEvent } from 'react';
import {
  BILLING_CYCLE_OPTIONS,
  DAY_OF_WEEK_OPTIONS,
  FEE_PER_HOUR_MIN,
  FEE_PER_HOUR_STEP,
  LEARNING_GOAL_OPTIONS,
  LEARNING_GOAL_OTHER,
  LESSON_MODE_OPTIONS,
  OTHER_SUBJECT,
  REPEAT_WEEKS_OPTIONS,
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
  patternRepeats,
  repeatLabel,
  repeatWeeksOf,
  restWeeksOf,
  studyWeeksOf,
  totalBudget,
  totalHoursPerRepeat,
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

// Giờ mặc định khi bật một thứ mới cho môn chưa có khung giờ chung.
const WEEKLY_DEFAULT_START = '18:00';
const WEEKLY_DEFAULT_END = '20:00';

/** Suy ra buổi (Sáng/Chiều/Tối) từ giờ bắt đầu để giữ tương thích dữ liệu (payload, matching). */
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

/** Ngược của toMinutes: 570 → "09:30". */
function minutesToTime(mins: number): string {
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

/** Ô <input type="number"> của Chrome vẫn HIỆN chữ đã gõ trên màn hình dù value trả về rỗng,
 *  nên lọc trong onChange là quá muộn — chữ đọng lại trong ô. Chặn ngay lúc nhập. */
function blockNonDigits(e: FormEvent<HTMLInputElement>) {
  const data = (e.nativeEvent as InputEvent).data;
  if (data && /\D/.test(data)) e.preventDefault();
}


/** Mỗi buổi (thứ + Sáng/Chiều/Tối) chỉ dành cho MỘT môn.
 *  Trả về buổi → tên môn khác đã đặt trong thứ đó. */
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

/** Buổi (Sáng/Chiều/Tối) không còn mốc giờ trống nào trong ngày → khỏi cho chọn. */
function sessionFullyBusy(
  session: (typeof SESSION_OPTIONS)[number],
  busy: { start: string; end: string }[],
): boolean {
  const pool = buildTimeSlots(session.min, session.max).slice(0, -1);
  return pool.every((t) => busy.some((b) => b.start <= t && t < b.end));
}

/** Nhãn thời lượng gọn: "30 phút", "1 giờ", "1 giờ 30 phút". */
function durationLabel(start: string, end: string): string {
  if (!start || !end || end <= start) return '';
  const mins = toMinutes(end) - toMinutes(start);
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  const parts = [];
  if (h) parts.push(`${h} giờ`);
  if (m) parts.push(`${m} phút`);
  return parts.join(' ');
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

  // Đổi kiểu lịch: buổi của kiểu cũ không mang sang được (WEEKLY gắn với Thứ, CUSTOM gắn với ngày
  // cụ thể) → bỏ buổi không hợp lệ với kiểu mới, nếu không sẽ đọng lại dòng "ma" trống Thứ lẫn ngày.
  function setScheduleMode(mode: ScheduleMode) {
    setForm((prev) => {
      if (prev.scheduleMode === mode) return prev;
      const slots = prev.slots.filter((s) => (mode === 'WEEKLY' ? !!s.day : !!s.date));
      return { ...prev, scheduleMode: mode, slots };
    });
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
      // WEEKLY chọn buổi bằng vòng tròn thứ (không tạo sẵn buổi rỗng); CUSTOM tạo sẵn 1 dòng ngày.
      const slots = removing
        ? prev.slots.filter((s) => s.subjectId !== id)
        : prev.scheduleMode === 'WEEKLY'
          ? prev.slots
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
    // Ô number vẫn cho gõ '-' và 'e' → chỉ giữ chữ số, học phí không bao giờ âm.
    const digits = value.replace(/\D/g, '');
    setForm((prev) => ({
      ...prev,
      subjectFees: { ...prev.subjectFees, [subjectId]: digits },
    }));
  }

  function addSlot(subjectId: string) {
    setForm((prev) => ({ ...prev, slots: [...prev.slots, emptySlot(subjectId)] }));
  }

  // WEEKLY: bật/tắt một thứ cho môn — thêm/xóa buổi tương ứng, giờ mặc định lấy từ buổi trước đó.
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
      // Giữ khung giờ chung của môn nếu giờ đó còn trống; không thì đẩy sang khung trống gần nhất.
      const shared = prev.slots.find((s) => s.subjectId === subjectId);
      let start = shared?.start || WEEKLY_DEFAULT_START;
      let end = shared?.end || WEEKLY_DEFAULT_END;
      const overlaps = (a: string, b: string) => busy.some((r) => a < r.end && r.start < b);
      if (overlaps(start, end)) {
        const span = toMinutes(end) - toMinutes(start);
        const free = buildTimeSlots('06:00', '23:30').find(
          (t) => toMinutes(t) + span <= toMinutes('23:30') && !overlaps(t, minutesToTime(toMinutes(t) + span)),
        );
        if (!free) return prev; // cả ngày đã kín lịch của môn khác
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

  // WEEKLY: đổi giờ của riêng một buổi (một thứ) — các thứ khác giữ nguyên giờ.
  function setSlotTime(index: number, patch: { start?: string; end?: string }) {
    setForm((prev) => ({
      ...prev,
      slots: prev.slots.map((s, i) => {
        if (i !== index) return s;
        const start = patch.start ?? s.start;
        // Đổi giờ bắt đầu mà giờ kết thúc không còn hợp lệ thì xóa giờ kết thúc để chọn lại.
        let end = patch.end ?? s.end;
        if (end && start && end <= start) end = '';
        // Xóa giờ bắt đầu thì giữ nguyên buổi đang chọn: ô Buổi không có lựa chọn rỗng,
        // để session rỗng sẽ khiến nó hiện sai buổi so với dữ liệu.
        return { ...s, start, end, session: start ? sessionFromStart(start) : s.session };
      }),
    }));
  }

  // Bật/tắt một tuần trong chu kỳ (tuần học ↔ tuần nghỉ). Luôn giữ lại ít nhất một tuần học.
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

  // Chọn buổi → điền sẵn khung giờ gợi ý (vẫn sửa được).
  // Bỏ chọn buổi → xóa giờ luôn, tránh kẹt giờ của buổi cũ và báo lỗi khó hiểu.
  function setSlotSession(index: number, session: string) {
    const preset = SESSION_OPTIONS.find((s) => s.value === session);
    if (!preset) {
      updateSlot(index, { session, start: '', end: '' });
      return;
    }
    setForm((prev) => {
      const cur = prev.slots[index];
      const weekly = prev.scheduleMode === 'WEEKLY';
      // Khung giờ gợi ý chỉ được điền sẵn nếu không đè lên buổi khác cùng thứ/ngày
      // và không nằm ở quá khứ — nếu không thì để trống cho người dùng chọn từ danh sách đã lọc.
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
  const repeats = patternRepeats(form);
  const cycleName = cycleLabelOf(form); // "N tháng" hoặc nhãn cố định
  // Dùng tên gọn để khỏi lồng ngoặc: "(quý)" thay vì "(quý (3 tháng))".
  const cycleLabelDisplay = isMonth ? cycleName : cycle.short;
  const cycleSuffix = `đ / ${isMonth ? cycleName : cycle.short}`;
  // repeats = số TUẦN HỌC trong chu kỳ, nên số giờ/buổi bên dưới luôn tính cho một tuần học
  // (có tuần nghỉ thì nói rõ "tuần học" để không hiểu nhầm là mọi tuần trong lịch).
  const perRepeatUnit = repeatWeeksOf(form) === 1 ? 'tuần' : 'tuần học';
  const studyWeeks = studyWeeksOf(form);
  const restWeeks = restWeeksOf(form);

  const hoursPerRepeat = useMemo(() => totalHoursPerRepeat(form), [form]);
  const total = useMemo(() => totalBudget(form), [form]);

  const isOffline = form.lessonMode !== 'ONLINE';
  const today = new Date().toLocaleDateString('en-CA');
  // Giờ hiện tại "HH:MM" — dùng để chặn buổi học trong ngày hôm nay nhưng đã qua giờ.
  const nowHm = new Date().toTimeString().slice(0, 5);

  // Kiểm tra từng buổi: đủ thông tin (Thứ hoặc Ngày) + ngày không quá khứ + giờ hợp lệ.
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
  if (form.slots.length === 0) missing.push('Lịch học');
  // Yêu cầu bổ sung và Ghi chú là tùy chọn — để trống vẫn đăng được.

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
          <span className="mkt-field__label">Yêu cầu bổ sung (chứng chỉ, bằng cấp…)</span>
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

      {/* Tần suất lặp — chỉ có nghĩa với lịch theo Thứ */}
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
                  // Phải còn ít nhất một tuần học → không cho tắt tuần cuối cùng.
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
                      min={FEE_PER_HOUR_MIN}
                      step={FEE_PER_HOUR_STEP}
                      value={form.subjectFees[sid] ?? ''}
                      placeholder={`Từ ${currency.format(FEE_PER_HOUR_MIN)}`}
                      aria-label={`Học phí/giờ môn ${subjName(sid)}`}
                      title={`Học phí/giờ tối thiểu ${currency.format(FEE_PER_HOUR_MIN)}đ`}
                      onBeforeInput={blockNonDigits}
                      // Dán "150.000 đ" → lấy phần số (150000) thay vì chặn sạch cho người dùng hụt.
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
                    // Mỗi thứ đã chọn là một buổi riêng, sắp theo thứ tự T2 → CN.
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
                              // Cả ngày không còn khung giờ trống vì môn khác đã đặt hết → khóa thứ này.
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
                          // Giờ bắt đầu VÀ giờ kết thúc đều nằm trong đúng khung của buổi đã chọn:
                          // buổi Sáng (6h–12h) thì muộn nhất là kết thúc 12:00, không tràn sang chiều.
                          const sess = SESSION_OPTIONS.find((o) => o.value === slot.session);
                          const pool = sess ? buildTimeSlots(sess.min, sess.max) : allTimes;
                          // Các buổi của môn khác trong cùng Thứ → chặn đúng khung giờ đó,
                          // không chặn cả buổi (vẫn học chung buổi Sáng ở giờ khác được).
                          const busy = busyRangesOf(form.slots, idx, 'day', slot.day);
                          const startTimes = pool
                            .slice(0, -1) // bỏ mốc cuối để còn chỗ cho giờ kết thúc
                            .filter((t) => !busy.some((b) => b.start <= t && t < b.end));
                          // Giờ kết thúc không được tràn qua buổi bận kế tiếp (chạm sát thì được).
                          const nextBusyStart = busy
                            .map((b) => b.start)
                            .filter((s) => s >= slot.start)
                            .sort()[0];
                          const endTimes = pool.filter(
                            (t) =>
                              (!slot.start || t > slot.start) &&
                              (!nextBusyStart || t <= nextBusyStart),
                          );
                          // Lớp cũ có giờ kết thúc vắt ra ngoài khung → vẫn liệt kê để không mất giờ đã lưu.
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
                                {/* Không có lựa chọn rỗng: bật một thứ là luôn có buổi cụ thể. */}
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
                                    {slot.start ? `${t} (${durationLabel(slot.start, t)})` : t}
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
                                      slot.start && slot.end ? ` ${slot.start}–${slot.end}` : ''
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
                        // Ngày hôm nay: chỉ cho chọn các mốc giờ còn ở tương lai.
                        const dayTimes =
                          slot.date === today ? sessTimes.filter((t) => t > nowHm) : sessTimes;
                        // Các buổi khác đã đặt trong cùng ngày → không cho chọn đè lên.
                        const busy = busyRangesOf(form.slots, idx, 'date', slot.date);
                        // Giờ bắt đầu: không được rơi vào trong một khoảng đã bận.
                        const times = dayTimes.filter(
                          (t) => !busy.some((b) => b.start <= t && t < b.end),
                        );
                        // Giờ kết thúc: không được vượt qua buổi bận kế tiếp (chạm sát thì vẫn được).
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
                                // Giờ cũ đã trôi qua, hoặc ngày mới đã có buổi khác chiếm giờ đó
                                // → bỏ giờ để người dùng chọn lại từ danh sách đã lọc.
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

      {/* Tổng học phí (học phí/giờ nhập theo từng môn ở trên) */}
      <div className="mkt-field">
        <span className="mkt-field__label">Tổng học phí ước tính ({cycleLabelDisplay})</span>
        <div className="mkt-total">
          {currency.format(total)} {cycleSuffix}
        </div>
        <span className="mkt-hint">
          {hoursPerRepeat} giờ/{perRepeatUnit} · {form.slots.length} buổi/{perRepeatUnit} — Tổng:{' '}
          {hoursPerRepeat * repeats} giờ · {form.slots.length * repeats} buổi ({repeats} tuần học
          {repeatWeeksOf(form) > 1 ? ` trong ${weeksForCycle(form)} tuần` : ''}).
        </span>
      </div>

      {/* Ghi chú bổ sung */}
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
