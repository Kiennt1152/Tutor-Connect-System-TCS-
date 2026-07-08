import { useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { VerificationHeader } from '../../../shared/components/VerificationHeader';
import { centerApi } from '../api/centerApi';
import type {
  ClassResponse,
  ClassStatus,
  LessonMode,
  RecurringType,
  SaveClassRequest,
} from '../types/centerTypes';
import './CenterPage.css';

const DAYS: { value: number; label: string }[] = [
  { value: 1, label: 'Thứ Hai' },
  { value: 2, label: 'Thứ Ba' },
  { value: 3, label: 'Thứ Tư' },
  { value: 4, label: 'Thứ Năm' },
  { value: 5, label: 'Thứ Sáu' },
  { value: 6, label: 'Thứ Bảy' },
  { value: 7, label: 'Chủ Nhật' },
];

// Danh sách khối/lớp cố định 1–12, thêm lựa chọn "Khác" để tự nhập.
const GRADE_OPTIONS: string[] = Array.from({ length: 12 }, (_, i) => `Lớp ${i + 1}`);
const GRADE_OTHER = 'Khác';

const STATUS_LABELS: Record<ClassStatus, string> = {
  DRAFT: 'Nháp',
  OPEN: 'Đang mở',
  MATCHED: 'Đã ghép',
  ENROLLMENT_CLOSED: 'Đóng ghi danh',
  IN_PROGRESS: 'Đang diễn ra',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
  DISPUTED: 'Tranh chấp',
};

const LESSON_MODES: LessonMode[] = ['ONLINE', 'OFFLINE', 'HYBRID'];
const LESSON_MODE_LABELS: Record<LessonMode, string> = {
  ONLINE: 'Trực tuyến',
  OFFLINE: 'Trực tiếp',
  HYBRID: 'Kết hợp',
};
const RECURRING_TYPES: RecurringType[] = ['ONCE', 'WEEKLY'];
const RECURRING_LABELS: Record<RecurringType, string> = {
  ONCE: 'Một lần',
  WEEKLY: 'Hằng tuần',
};

interface SlotForm {
  dayOfWeek: number;
  startTime: string;
  endTime: string;
}

interface FormState {
  title: string;
  description: string;
  categoryName: string;
  subjectName: string;
  gradeChoice: string; // một trong GRADE_OPTIONS hoặc GRADE_OTHER
  gradeCustom: string; // dùng khi gradeChoice === GRADE_OTHER
  locationText: string;
  lessonMode: LessonMode;
  recurringType: RecurringType;
  numberOfSessions: string;
  tuitionFee: string;
  startDate: string;
  endDate: string;
  schedule: SlotForm[];
}

const EMPTY_FORM: FormState = {
  title: '',
  description: '',
  categoryName: '',
  subjectName: '',
  gradeChoice: 'Lớp 1',
  gradeCustom: '',
  locationText: '',
  lessonMode: 'OFFLINE',
  recurringType: 'WEEKLY',
  numberOfSessions: '1',
  tuitionFee: '',
  startDate: '',
  endDate: '',
  schedule: [{ dayOfWeek: 1, startTime: '18:00', endTime: '20:00' }],
};

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

function toFormState(c: ClassResponse): FormState {
  const gradeName = c.gradeName ?? '';
  const isKnownGrade = GRADE_OPTIONS.includes(gradeName);
  return {
    title: c.title,
    description: c.description ?? '',
    categoryName: c.categoryName ?? '',
    subjectName: c.subjectName ?? '',
    gradeChoice: gradeName === '' ? 'Lớp 1' : isKnownGrade ? gradeName : GRADE_OTHER,
    gradeCustom: isKnownGrade ? '' : gradeName,
    locationText: c.locationText ?? '',
    lessonMode: c.lessonMode,
    recurringType: c.recurringType,
    numberOfSessions: String(c.numberOfSessions),
    tuitionFee: String(c.tuitionFee),
    startDate: c.startDate,
    endDate: c.endDate,
    schedule: c.schedule.map((s) => ({
      dayOfWeek: s.dayOfWeek,
      startTime: s.startTime.slice(0, 5),
      endTime: s.endTime.slice(0, 5),
    })),
  };
}

function buildPayload(form: FormState): SaveClassRequest {
  const num = (v: string) => (v.trim() === '' ? null : Number(v));
  const gradeName = form.gradeChoice === GRADE_OTHER ? form.gradeCustom.trim() : form.gradeChoice;
  return {
    title: form.title.trim(),
    description: form.description.trim() || undefined,
    categoryName: form.categoryName.trim(),
    subjectName: form.subjectName.trim(),
    gradeName,
    locationText: form.locationText.trim(),
    lessonMode: form.lessonMode,
    recurringType: form.recurringType,
    numberOfSessions: num(form.numberOfSessions),
    tuitionFee: num(form.tuitionFee),
    startDate: form.startDate || null,
    endDate: form.endDate || null,
    schedule: form.schedule.map((s) => ({
      dayOfWeek: s.dayOfWeek,
      startTime: s.startTime,
      endTime: s.endTime,
    })),
  };
}

type FieldKey =
  | 'title'
  | 'categoryName'
  | 'subjectName'
  | 'grade'
  | 'locationText'
  | 'numberOfSessions'
  | 'tuitionFee'
  | 'startDate'
  | 'endDate';

interface FormErrors {
  fields: Partial<Record<FieldKey, string>>;
  slots: Record<number, string>;
}

function validateForm(form: FormState): FormErrors {
  const fields: Partial<Record<FieldKey, string>> = {};
  const slots: Record<number, string> = {};

  if (!form.title.trim()) fields.title = 'Tiêu đề là bắt buộc';
  if (!form.categoryName.trim()) fields.categoryName = 'Danh mục là bắt buộc';
  if (!form.subjectName.trim()) fields.subjectName = 'Môn học là bắt buộc';
  if (form.gradeChoice === GRADE_OTHER && !form.gradeCustom.trim())
    fields.grade = 'Vui lòng nhập khối/lớp';
  if (!form.locationText.trim()) fields.locationText = 'Địa điểm là bắt buộc';

  const sessions = Number(form.numberOfSessions);
  if (!form.numberOfSessions.trim() || !Number.isInteger(sessions) || sessions <= 0)
    fields.numberOfSessions = 'Số buổi học phải là số nguyên dương';

  const fee = Number(form.tuitionFee);
  if (!form.tuitionFee.trim() || Number.isNaN(fee) || fee <= 0)
    fields.tuitionFee = 'Học phí phải là số dương';

  if (!form.startDate) fields.startDate = 'Ngày bắt đầu là bắt buộc';
  if (!form.endDate) fields.endDate = 'Ngày kết thúc là bắt buộc';
  else if (form.startDate && form.endDate <= form.startDate)
    fields.endDate = 'Ngày kết thúc phải sau ngày bắt đầu';

  form.schedule.forEach((s, i) => {
    if (!s.startTime || !s.endTime || s.endTime <= s.startTime)
      slots[i] = 'Giờ kết thúc phải sau giờ bắt đầu';
  });

  // Không cho hai khung trùng/chồng giờ trong cùng một ngày.
  for (let i = 0; i < form.schedule.length; i++) {
    if (slots[i]) continue;
    for (let j = 0; j < i; j++) {
      if (slots[j]) continue;
      const a = form.schedule[i];
      const b = form.schedule[j];
      if (a.dayOfWeek === b.dayOfWeek && a.startTime < b.endTime && b.startTime < a.endTime) {
        slots[i] = 'Khung lịch bị trùng/chồng giờ với khung khác cùng ngày';
        break;
      }
    }
  }

  return { fields, slots };
}

function hasErrors(e: FormErrors): boolean {
  return Object.keys(e.fields).length > 0 || Object.keys(e.slots).length > 0;
}

export default function CenterPage() {
  const [classes, setClasses] = useState<ClassResponse[]>([]);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState('');

  const [mode, setMode] = useState<'list' | 'form'>('list');
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const errors = useMemo(() => validateForm(form), [form]);

  const reloadList = () => {
    setListLoading(true);
    setListError('');
    centerApi
      .getMyClasses()
      .then((res) => setClasses(res.data))
      .catch((err) => setListError(extractError(err, 'Không tải được danh sách lớp học.')))
      .finally(() => setListLoading(false));
  };

  useEffect(() => {
    reloadList();
  }, []);

  const openCreate = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setSubmitted(false);
    setMode('form');
  };

  const openEdit = async (classId: number) => {
    setFormError('');
    setSubmitted(false);
    try {
      const res = await centerApi.getClass(classId);
      setForm(toFormState(res.data));
      setEditingId(classId);
      setMode('form');
    } catch (err) {
      setListError(extractError(err, 'Không mở được lớp học để chỉnh sửa.'));
    }
  };

  const patch = (partial: Partial<FormState>) => setForm((prev) => ({ ...prev, ...partial }));

  const addSlot = () =>
    patch({ schedule: [...form.schedule, { dayOfWeek: 1, startTime: '18:00', endTime: '20:00' }] });
  const removeSlot = (index: number) =>
    patch({ schedule: form.schedule.filter((_, i) => i !== index) });
  const updateSlot = (index: number, partial: Partial<SlotForm>) =>
    patch({
      schedule: form.schedule.map((s, i) => (i === index ? { ...s, ...partial } : s)),
    });

  const handleSubmit = async () => {
    setSubmitted(true);
    setFormError('');
    if (hasErrors(errors)) return; // dừng lại, lỗi hiển thị ngay dưới từng field
    setSaving(true);
    try {
      const payload = buildPayload(form);
      if (editingId != null) {
        await centerApi.updateClass(editingId, payload);
      } else {
        await centerApi.createClass(payload);
      }
      setMode('list');
      reloadList();
    } catch (err) {
      setFormError(extractError(err, 'Không lưu được lớp học.'));
    } finally {
      setSaving(false);
    }
  };

  const errClass = (key: FieldKey) =>
    `cc-input${submitted && errors.fields[key] ? ' cc-input--error' : ''}`;
  const errText = (key: FieldKey) =>
    submitted && errors.fields[key] ? (
      <span className="cc-error">{errors.fields[key]}</span>
    ) : null;

  const canEditStatus = (status: ClassStatus) => status === 'DRAFT' || status === 'OPEN';

  const pageTitle = useMemo(
    () =>
      mode === 'form'
        ? editingId != null
          ? 'Chỉnh sửa lớp học'
          : 'Tạo lớp học mới'
        : 'Lớp học của tôi',
    [mode, editingId],
  );

  return (
    <>
      <VerificationHeader />
      <div className="cc-page">
      <header className="cc-header">
        <h1 className="cc-title">{pageTitle}</h1>
        {mode === 'list' ? (
          <button className="cc-btn cc-btn--primary" type="button" onClick={openCreate}>
            + Tạo lớp mới
          </button>
        ) : (
          <button className="cc-btn cc-btn--ghost" type="button" onClick={() => setMode('list')}>
            ← Quay lại danh sách
          </button>
        )}
      </header>

      {mode === 'list' && (
        <section className="cc-card">
          {listLoading && <div className="cc-state">Đang tải danh sách lớp học…</div>}
          {listError && <div className="cc-alert cc-alert--error">{listError}</div>}
          {!listLoading && !listError && classes.length === 0 && (
            <div className="cc-state">Chưa có lớp học nào. Bấm “Tạo lớp mới” để bắt đầu.</div>
          )}
          {!listLoading && classes.length > 0 && (
            <div className="cc-table-wrap">
              <table className="cc-table">
                <thead>
                  <tr>
                    <th>Tiêu đề</th>
                    <th>Môn</th>
                    <th>Khối</th>
                    <th>Hình thức</th>
                    <th>Bắt đầu</th>
                    <th>Kết thúc</th>
                    <th>Trạng thái</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {classes.map((c) => (
                    <tr key={c.classId}>
                      <td>{c.title}</td>
                      <td>{c.subjectName ?? '—'}</td>
                      <td>{c.gradeName ?? '—'}</td>
                      <td>{LESSON_MODE_LABELS[c.lessonMode]}</td>
                      <td>{c.startDate}</td>
                      <td>{c.endDate}</td>
                      <td>
                        <span className={`cc-badge cc-badge--${c.status.toLowerCase()}`}>
                          {STATUS_LABELS[c.status]}
                        </span>
                      </td>
                      <td>
                        {canEditStatus(c.status) ? (
                          <button
                            className="cc-btn cc-btn--sm"
                            type="button"
                            onClick={() => openEdit(c.classId)}
                          >
                            Sửa
                          </button>
                        ) : (
                          <span className="cc-muted">Không sửa được</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}

      {mode === 'form' && (
        <section className="cc-card">
          {formError && <div className="cc-alert cc-alert--error">{formError}</div>}

          <div className="cc-grid">
            <label className="cc-field cc-field--full">
              <span className="cc-label">Tiêu đề *</span>
              <input
                className={errClass('title')}
                value={form.title}
                onChange={(e) => patch({ title: e.target.value })}
                placeholder="VD: Toán nâng cao lớp 9"
              />
              {errText('title')}
            </label>

            <label className="cc-field cc-field--full">
              <span className="cc-label">Mô tả</span>
              <textarea
                className="cc-input"
                rows={3}
                value={form.description}
                onChange={(e) => patch({ description: e.target.value })}
                placeholder="Mô tả nội dung, mục tiêu lớp học…"
              />
            </label>

            <label className="cc-field">
              <span className="cc-label">Danh mục *</span>
              <input
                className={errClass('categoryName')}
                value={form.categoryName}
                onChange={(e) => patch({ categoryName: e.target.value })}
                placeholder="VD: Luyện thi, Ngoại ngữ…"
              />
              {errText('categoryName')}
            </label>

            <label className="cc-field">
              <span className="cc-label">Môn học *</span>
              <input
                className={errClass('subjectName')}
                value={form.subjectName}
                onChange={(e) => patch({ subjectName: e.target.value })}
                placeholder="VD: Toán, Tiếng Anh…"
              />
              {errText('subjectName')}
            </label>

            <label className="cc-field">
              <span className="cc-label">Khối/Lớp *</span>
              <select
                className="cc-input"
                value={form.gradeChoice}
                onChange={(e) => patch({ gradeChoice: e.target.value })}
              >
                {GRADE_OPTIONS.map((g) => (
                  <option key={g} value={g}>
                    {g}
                  </option>
                ))}
                <option value={GRADE_OTHER}>Khác…</option>
              </select>
            </label>

            {form.gradeChoice === GRADE_OTHER && (
              <label className="cc-field">
                <span className="cc-label">Khối/Lớp (tự nhập) *</span>
                <input
                  className={errClass('grade')}
                  value={form.gradeCustom}
                  onChange={(e) => patch({ gradeCustom: e.target.value })}
                  placeholder="VD: Mầm non, Đại học…"
                />
                {errText('grade')}
              </label>
            )}

            <label className="cc-field cc-field--full">
              <span className="cc-label">Địa điểm *</span>
              <input
                className={errClass('locationText')}
                value={form.locationText}
                onChange={(e) => patch({ locationText: e.target.value })}
                placeholder="VD: 123 Lê Lợi, Quận 1, TP.HCM"
              />
              {errText('locationText')}
            </label>

            <label className="cc-field">
              <span className="cc-label">Hình thức học *</span>
              <select
                className="cc-input"
                value={form.lessonMode}
                onChange={(e) => patch({ lessonMode: e.target.value as LessonMode })}
              >
                {LESSON_MODES.map((m) => (
                  <option key={m} value={m}>
                    {LESSON_MODE_LABELS[m]}
                  </option>
                ))}
              </select>
            </label>

            <label className="cc-field">
              <span className="cc-label">Kiểu lặp lịch *</span>
              <select
                className="cc-input"
                value={form.recurringType}
                onChange={(e) => patch({ recurringType: e.target.value as RecurringType })}
              >
                {RECURRING_TYPES.map((r) => (
                  <option key={r} value={r}>
                    {RECURRING_LABELS[r]}
                  </option>
                ))}
              </select>
            </label>

            <label className="cc-field">
              <span className="cc-label">Số buổi học *</span>
              <input
                className={errClass('numberOfSessions')}
                type="number"
                min={1}
                value={form.numberOfSessions}
                onChange={(e) => patch({ numberOfSessions: e.target.value })}
              />
              {errText('numberOfSessions')}
            </label>

            <label className="cc-field">
              <span className="cc-label">Học phí (VND) *</span>
              <input
                className={errClass('tuitionFee')}
                type="number"
                min={0}
                value={form.tuitionFee}
                onChange={(e) => patch({ tuitionFee: e.target.value })}
                placeholder="VD: 500000"
              />
              {errText('tuitionFee')}
            </label>

            <label className="cc-field">
              <span className="cc-label">Ngày bắt đầu *</span>
              <input
                className={errClass('startDate')}
                type="date"
                value={form.startDate}
                onChange={(e) => patch({ startDate: e.target.value })}
              />
              {errText('startDate')}
            </label>

            <label className="cc-field">
              <span className="cc-label">Ngày kết thúc *</span>
              <input
                className={errClass('endDate')}
                type="date"
                value={form.endDate}
                onChange={(e) => patch({ endDate: e.target.value })}
              />
              {errText('endDate')}
            </label>
          </div>

          <div className="cc-schedule">
            <div className="cc-schedule__head">
              <span className="cc-label">Lịch học (ít nhất 1 khung) *</span>
              <button className="cc-btn cc-btn--sm" type="button" onClick={addSlot}>
                + Thêm khung
              </button>
            </div>
            {form.schedule.map((slot, index) => {
              const slotError = submitted ? errors.slots[index] : undefined;
              const timeClass = `cc-input${slotError ? ' cc-input--error' : ''}`;
              return (
                <div className="cc-slot-row" key={index}>
                  <div className="cc-slot">
                    <select
                      className="cc-input"
                      value={slot.dayOfWeek}
                      onChange={(e) => updateSlot(index, { dayOfWeek: Number(e.target.value) })}
                    >
                      {DAYS.map((d) => (
                        <option key={d.value} value={d.value}>
                          {d.label}
                        </option>
                      ))}
                    </select>
                    <input
                      className={timeClass}
                      type="time"
                      value={slot.startTime}
                      onChange={(e) => updateSlot(index, { startTime: e.target.value })}
                    />
                    <span className="cc-slot__sep">→</span>
                    <input
                      className={timeClass}
                      type="time"
                      value={slot.endTime}
                      onChange={(e) => updateSlot(index, { endTime: e.target.value })}
                    />
                    <button
                      className="cc-btn cc-btn--danger cc-btn--sm"
                      type="button"
                      onClick={() => removeSlot(index)}
                      disabled={form.schedule.length <= 1}
                    >
                      Xóa
                    </button>
                  </div>
                  {slotError && <span className="cc-error">{slotError}</span>}
                </div>
              );
            })}
          </div>

          <div className="cc-form-foot">
            <button className="cc-btn cc-btn--ghost" type="button" onClick={() => setMode('list')}>
              Hủy
            </button>
            <button
              className="cc-btn cc-btn--primary"
              type="button"
              onClick={handleSubmit}
              disabled={saving}
            >
              {saving ? 'Đang lưu…' : editingId != null ? 'Lưu thay đổi' : 'Tạo lớp học'}
            </button>
          </div>
        </section>
      )}
      </div>
    </>
  );
}
