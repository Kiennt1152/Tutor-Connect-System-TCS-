import { useEffect, useMemo, useState } from 'react';
import {
  BILLING_CYCLE_OPTIONS,
  DAY_OF_WEEK_OPTIONS,
  LEARNING_GOAL_OPTIONS,
  LEARNING_GOAL_OTHER,
  LESSON_MODE_OPTIONS,
  SESSION_OPTIONS,
  TUTOR_REQUIREMENT_OPTIONS,
  type BillingCycle,
  type CatalogOption,
  type ClassFormValues,
  type ClassRequestPayload,
  type LessonMode,
} from '../types/marketplaceTypes';
import { formToPayload } from '../mappers/marketplaceMapper';
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
    setForm((prev) => ({
      ...prev,
      districtId: value,
      districtName: name,
      wardId: '',
      wardName: '',
    }));
  }

  function handleWardChange(value: string) {
    const name = wards.find((w) => String(w.id) === value)?.name ?? '';
    setForm((prev) => ({ ...prev, wardId: value, wardName: name }));
  }

  // Bật/tắt một thứ; khi bật thì khởi tạo khung giờ rỗng cho thứ đó.
  // Không cho chọn quá số buổi/tuần đã nhập.
  function toggleDay(value: string) {
    setForm((prev) => {
      const has = prev.daysOfWeek.includes(value);
      const limit = Number(prev.sessionsPerWeek) || 0;
      if (!has && limit > 0 && prev.daysOfWeek.length >= limit) {
        return prev; // đã chọn đủ số buổi/tuần
      }
      const daysOfWeek = has
        ? prev.daysOfWeek.filter((v) => v !== value)
        : [...prev.daysOfWeek, value];
      const dayTimes = { ...prev.dayTimes };
      if (has) {
        delete dayTimes[value];
      } else {
        dayTimes[value] = { session: '', start: '', end: '' };
      }
      return { ...prev, daysOfWeek, dayTimes };
    });
  }

  // Đổi số buổi/tuần (chỉ nhận nguyên 1–7); nếu giảm thì bớt bớt các thứ dư.
  function setSessionsPerWeek(v: string) {
    if (v === '') {
      set('sessionsPerWeek', '');
      return;
    }
    const n = Number(v);
    if (!Number.isInteger(n) || n < 1 || n > 7) return;
    setForm((prev) => {
      if (prev.daysOfWeek.length <= n) {
        return { ...prev, sessionsPerWeek: v };
      }
      const kept = DAY_OF_WEEK_OPTIONS.filter((d) => prev.daysOfWeek.includes(d.value))
        .slice(0, n)
        .map((d) => d.value);
      const dayTimes: Record<string, { session: string; start: string; end: string }> = {};
      for (const k of kept) dayTimes[k] = prev.dayTimes[k] ?? { session: '', start: '', end: '' };
      return { ...prev, sessionsPerWeek: v, daysOfWeek: kept, dayTimes };
    });
  }

  function setDayTime(day: string, field: 'start' | 'end', value: string) {
    setForm((prev) => ({
      ...prev,
      dayTimes: {
        ...prev.dayTimes,
        [day]: { ...(prev.dayTimes[day] ?? { session: '', start: '', end: '' }), [field]: value },
      },
    }));
  }

  // Chọn buổi (Sáng/Chiều/Tối) → điền sẵn khung giờ gợi ý (vẫn sửa được).
  function setDaySession(day: string, session: string) {
    const preset = SESSION_OPTIONS.find((s) => s.value === session);
    setForm((prev) => ({
      ...prev,
      dayTimes: {
        ...prev.dayTimes,
        [day]: {
          session,
          start: preset ? preset.start : (prev.dayTimes[day]?.start ?? ''),
          end: preset ? preset.end : (prev.dayTimes[day]?.end ?? ''),
        },
      },
    }));
  }

  const cycle = BILLING_CYCLE_OPTIONS.find((o) => o.value === form.billingCycle) ?? BILLING_CYCLE_OPTIONS[0];

  const total = useMemo(() => {
    const fee = Number(form.feePerSession) || 0;
    const perWeek = Math.max(1, Number(form.sessionsPerWeek) || 1);
    return fee * perWeek * cycle.weeks;
  }, [form.feePerSession, form.sessionsPerWeek, cycle.weeks]);

  // Số buổi/tuần là mục tiêu; số thứ đã chọn phải khớp đúng.
  const targetDays = Number(form.sessionsPerWeek) || 0;
  const atDayLimit = targetDays > 0 && form.daysOfWeek.length >= targetDays;
  const dayCountShort =
    targetDays > 0 && form.daysOfWeek.length > 0 && form.daysOfWeek.length < targetDays;

  const isOffline = form.lessonMode !== 'ONLINE';
  const goalNeedsCustom = form.learningGoal === LEARNING_GOAL_OTHER;
  // Ngày hôm nay theo giờ địa phương (YYYY-MM-DD) — chặn chọn ngày quá khứ.
  const today = new Date().toLocaleDateString('en-CA');
  const startInPast = !!form.startDate && form.startDate < today;
  // Kiểm tra khung giờ của từng thứ đã chọn (đủ giờ + giờ kết thúc sau giờ bắt đầu).
  const dayTimeErrors: string[] = [];
  for (const opt of DAY_OF_WEEK_OPTIONS) {
    if (!form.daysOfWeek.includes(opt.value)) continue;
    const t = form.dayTimes[opt.value];
    if (!t?.start || !t?.end) {
      dayTimeErrors.push(`${opt.label}: chưa chọn đủ giờ học`);
    } else if (t.end <= t.start) {
      dayTimeErrors.push(`${opt.label}: giờ kết thúc phải sau giờ bắt đầu`);
    }
  }

  const missing: string[] = [];
  if (form.subjectIds.length === 0) missing.push('Môn học');
  if (!form.gradeId) missing.push('Lớp');
  if (!form.learningGoal) missing.push('Mục tiêu học tập');
  if (goalNeedsCustom && !form.learningGoalOther.trim()) missing.push('Mục tiêu cụ thể');
  if (isOffline) {
    if (!form.provinceId) missing.push('Tỉnh / Thành phố');
    if (!form.districtId) missing.push('Quận / Huyện');
    if (!form.wardId) missing.push('Phường / Xã');
    if (!form.address.trim()) missing.push('Địa chỉ chi tiết');
  }
  if (!form.tutorRequirementDetail.trim()) missing.push('Yêu cầu bổ sung');
  if (!form.sessionsPerWeek || Number(form.sessionsPerWeek) <= 0) missing.push('Số buổi / tuần');
  if (form.daysOfWeek.length === 0) missing.push('Ngày học trong tuần');
  if (!form.startDate) missing.push('Ngày bắt đầu');
  if (!form.feePerSession || Number(form.feePerSession) <= 0) missing.push('Học phí / buổi');
  if (!form.note.trim()) missing.push('Ghi chú / yêu cầu khác');

  function handleSubmit() {
    setTouched(true);
    if (missing.length > 0 || startInPast || dayTimeErrors.length > 0 || dayCountShort) return;
    onSubmit(formToPayload(form, subjects));
  }

  function toggleSubject(id: string) {
    setForm((prev) => ({
      ...prev,
      subjectIds: prev.subjectIds.includes(id)
        ? prev.subjectIds.filter((s) => s !== id)
        : [...prev.subjectIds, id],
    }));
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
          </div>
        )}
      </div>

      {/* Lớp */}
      <div className="mkt-form__grid">
        <label className="mkt-field">
          <span className="mkt-field__label">
            Lớp <em>*</em>
          </span>
          <select value={form.gradeId} onChange={(e) => set('gradeId', e.target.value)}>
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
      <label className="mkt-field">
        <span className="mkt-field__label">
          Mục tiêu học tập <em>*</em>
        </span>
        <select value={form.learningGoal} onChange={(e) => set('learningGoal', e.target.value)}>
          <option value="">-- Chọn mục tiêu --</option>
          {LEARNING_GOAL_OPTIONS.map((goal) => (
            <option key={goal} value={goal}>
              {goal}
            </option>
          ))}
          <option value={LEARNING_GOAL_OTHER}>{LEARNING_GOAL_OTHER}…</option>
        </select>
      </label>
      {goalNeedsCustom && (
        <label className="mkt-field">
          <span className="mkt-field__label">
            Mục tiêu cụ thể <em>*</em>
          </span>
          <input
            type="text"
            value={form.learningGoalOther}
            placeholder="VD: Luyện thi HSG Toán cấp tỉnh"
            onChange={(e) => set('learningGoalOther', e.target.value)}
          />
        </label>
      )}

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
            placeholder="VD: Có chứng chỉ IELTS 7.0, ưu tiên nữ"
            onChange={(e) => set('tutorRequirementDetail', e.target.value)}
          />
        </label>
      </div>

      {/* Lịch học */}
      <div className="mkt-form__grid mkt-form__grid--3">
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
        <label className="mkt-field">
          <span className="mkt-field__label">
            Số buổi / tuần <em>*</em>
          </span>
          <input
            type="number"
            min={1}
            max={7}
            value={form.sessionsPerWeek}
            onChange={(e) => setSessionsPerWeek(e.target.value)}
          />
        </label>
        <label className="mkt-field">
          <span className="mkt-field__label">
            Ngày bắt đầu mong muốn <em>*</em>
          </span>
          <input
            type="date"
            value={form.startDate}
            min={today}
            onChange={(e) => set('startDate', e.target.value)}
          />
        </label>
      </div>

      <div className="mkt-field">
        <span className="mkt-field__label">
          Học các thứ trong tuần <em>*</em>
          {targetDays > 0 && (
            <span className="mkt-field__hint-inline">
              {' '}
              (đã chọn {form.daysOfWeek.length}/{targetDays})
            </span>
          )}
        </span>
        <div className="mkt-checks">
          {DAY_OF_WEEK_OPTIONS.map((day) => {
            const checked = form.daysOfWeek.includes(day.value);
            return (
              <label
                key={day.value}
                className={`mkt-check${!checked && atDayLimit ? ' mkt-check--disabled' : ''}`}
              >
                <input
                  type="checkbox"
                  checked={checked}
                  disabled={!checked && atDayLimit}
                  onChange={() => toggleDay(day.value)}
                />
                <span>{day.label}</span>
              </label>
            );
          })}
        </div>
        {atDayLimit && (
          <p className="mkt-hint">
            Đã chọn đủ {targetDays} buổi/tuần. Tăng “Số buổi / tuần” nếu muốn thêm thứ.
          </p>
        )}
      </div>

      {/* Khung giờ học riêng cho từng thứ đã chọn */}
      <div className="mkt-field">
        <span className="mkt-field__label">
          Khung giờ học từng buổi <em>*</em>
        </span>
        {form.daysOfWeek.length === 0 ? (
          <p className="mkt-hint">Chọn các thứ ở trên để đặt giờ học cho từng buổi.</p>
        ) : (
          <div className="mkt-day-times">
            {DAY_OF_WEEK_OPTIONS.filter((d) => form.daysOfWeek.includes(d.value)).map((d) => (
              <div key={d.value} className="mkt-day-time">
                <span className="mkt-day-time__label">{d.label}</span>
                <select
                  className="mkt-day-time__session"
                  aria-label={`Buổi học ${d.label}`}
                  value={form.dayTimes[d.value]?.session ?? ''}
                  onChange={(e) => setDaySession(d.value, e.target.value)}
                >
                  <option value="">Buổi…</option>
                  {SESSION_OPTIONS.map((s) => (
                    <option key={s.value} value={s.value}>
                      {s.value}
                    </option>
                  ))}
                </select>
                <input
                  type="time"
                  aria-label={`Giờ bắt đầu ${d.label}`}
                  value={form.dayTimes[d.value]?.start ?? ''}
                  onChange={(e) => setDayTime(d.value, 'start', e.target.value)}
                />
                <span className="mkt-day-time__sep">–</span>
                <input
                  type="time"
                  aria-label={`Giờ kết thúc ${d.label}`}
                  value={form.dayTimes[d.value]?.end ?? ''}
                  onChange={(e) => setDayTime(d.value, 'end', e.target.value)}
                />
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Học phí */}
      <div className="mkt-form__grid">
        <label className="mkt-field">
          <span className="mkt-field__label">
            Học phí / buổi (đ) <em>*</em>
          </span>
          <input
            type="number"
            min={0}
            step={10000}
            value={form.feePerSession}
            placeholder="VD: 200000"
            onChange={(e) => set('feePerSession', e.target.value)}
          />
        </label>
        <div className="mkt-field">
          <span className="mkt-field__label">Tổng học phí ước tính ({cycle.label.toLowerCase()})</span>
          <div className="mkt-total">
            {currency.format(total)} {cycle.suffix}
          </div>
        </div>
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

      {touched && startInPast && (
        <div className="mkt-alert mkt-alert--error">
          Ngày bắt đầu không được ở trong quá khứ.
        </div>
      )}
      {touched && dayCountShort && (
        <div className="mkt-alert mkt-alert--error">
          Bạn chọn {targetDays} buổi/tuần — vui lòng chọn đủ {targetDays} thứ (đang chọn{' '}
          {form.daysOfWeek.length}).
        </div>
      )}
      {touched && dayTimeErrors.length > 0 && (
        <div className="mkt-alert mkt-alert--error">
          {dayTimeErrors.join('. ')}.
        </div>
      )}
      {touched && missing.length > 0 && (
        <div className="mkt-alert mkt-alert--error">
          Vui lòng nhập: {missing.join(', ')}.
        </div>
      )}
      {error && <div className="mkt-alert mkt-alert--error">{error}</div>}

      <div className="mkt-form__actions">
        <button type="button" className="mkt-btn mkt-btn--ghost" onClick={onCancel} disabled={submitting}>
          Hủy
        </button>
        <button type="button" className="mkt-btn mkt-btn--primary" onClick={handleSubmit} disabled={submitting}>
          {submitting ? 'Đang lưu…' : isEdit ? 'Lưu thay đổi' : 'Tạo lớp'}
        </button>
      </div>
    </div>
  );
}
