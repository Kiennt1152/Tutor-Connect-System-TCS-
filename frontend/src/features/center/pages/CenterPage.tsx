import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import { VerificationHeader } from '../../../shared/components/VerificationHeader';
import { LocationPicker } from '../components/LocationPicker';
import { centerApi } from '../api/centerApi';
import type {
  ClassResponse,
  ClassStatus,
  LessonMode,
  RecurringType,
  SaveClassRequest,
  TutorOption,
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
const RECURRING_TYPES: RecurringType[] = ['DAILY', 'WEEKLY'];
const RECURRING_LABELS: Record<RecurringType, string> = {
  DAILY: 'Hằng ngày',
  WEEKLY: 'Hằng tuần',
  ONCE: 'Một lần',
};

function todayStr(): string {
  const d = new Date();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
}
const TODAY = todayStr();

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
  province: string;
  ward: string;
  addressDetail: string;
  lessonMode: LessonMode;
  recurringType: RecurringType;
  tuitionFee: string;
  maxStudents: string;
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
  province: '',
  ward: '',
  addressDetail: '',
  lessonMode: 'OFFLINE',
  recurringType: 'WEEKLY',
  tuitionFee: '',
  maxStudents: '',
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
  // Dữ liệu cũ 'ONCE' xem như Hằng tuần trong form.
  const recurring: RecurringType = c.recurringType === 'ONCE' ? 'WEEKLY' : c.recurringType;
  let schedule = c.schedule.map((s) => ({
    dayOfWeek: s.dayOfWeek,
    startTime: s.startTime.slice(0, 5),
    endTime: s.endTime.slice(0, 5),
  }));
  // Hằng ngày: DB lưu tiết lặp cho mọi thứ -> gộp lại theo (giờ bắt đầu–kết thúc) để hiện đúng.
  if (recurring === 'DAILY') {
    const seen = new Set<string>();
    schedule = schedule.filter((s) => {
      const key = `${s.startTime}-${s.endTime}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });
  }
  return {
    title: c.title,
    description: c.description ?? '',
    categoryName: c.categoryName ?? '',
    subjectName: c.subjectName ?? '',
    gradeChoice: gradeName === '' ? 'Lớp 1' : isKnownGrade ? gradeName : GRADE_OTHER,
    gradeCustom: isKnownGrade ? '' : gradeName,
    province: c.provinceName ?? '',
    ward: c.wardName ?? '',
    addressDetail: c.addressDetail ?? '',
    lessonMode: c.lessonMode,
    recurringType: recurring,
    tuitionFee: String(c.tuitionFee),
    maxStudents: c.maxStudents != null ? String(c.maxStudents) : '',
    startDate: c.startDate,
    endDate: c.endDate,
    schedule,
  };
}

// Tập các thứ (1=Thứ Hai..7=Chủ Nhật) xuất hiện trong khoảng ngày đã chọn.
function allowedDaysInRange(start: string, end: string): Set<number> {
  if (!start || !end || end < start) return new Set([1, 2, 3, 4, 5, 6, 7]);
  const set = new Set<number>();
  const e = new Date(`${end}T00:00:00`);
  for (let d = new Date(`${start}T00:00:00`); d <= e && set.size < 7; d.setDate(d.getDate() + 1)) {
    set.add(((d.getDay() + 6) % 7) + 1);
  }
  return set;
}

function daysInRange(start: string, end: string): number {
  const s = new Date(`${start}T00:00:00`).getTime();
  const e = new Date(`${end}T00:00:00`).getTime();
  return Math.floor((e - s) / 86400000) + 1;
}

// Số buổi tự tính: DAILY = số ngày × số tiết/ngày; WEEKLY đếm mọi lần lặp; ONCE mỗi khung một lần.
function countSessions(form: FormState): number {
  const { startDate, endDate, schedule, recurringType } = form;
  if (!startDate || !endDate || endDate < startDate || schedule.length === 0) return 0;
  if (recurringType === 'DAILY') return daysInRange(startDate, endDate) * schedule.length;
  const allowed = allowedDaysInRange(startDate, endDate);
  const valid = schedule.filter((s) => allowed.has(s.dayOfWeek));
  if (recurringType === 'ONCE') return valid.length;
  let count = 0;
  const e = new Date(`${endDate}T00:00:00`);
  for (let d = new Date(`${startDate}T00:00:00`); d <= e; d.setDate(d.getDate() + 1)) {
    const iso = ((d.getDay() + 6) % 7) + 1;
    count += valid.filter((sl) => sl.dayOfWeek === iso).length;
  }
  return count;
}

function dayLabel(value: number): string {
  return DAYS.find((d) => d.value === value)?.label ?? `Thứ ${value}`;
}

function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(-2)
    .map((w) => w[0]?.toUpperCase() ?? '')
    .join('');
}

function formatCurrency(value: number): string {
  return `${new Intl.NumberFormat('vi-VN').format(value)} đ`;
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
    provinceName: form.province,
    wardName: form.ward,
    addressDetail: form.addressDetail.trim(),
    lessonMode: form.lessonMode,
    recurringType: form.recurringType,
    numberOfSessions: countSessions(form),
    tuitionFee: num(form.tuitionFee),
    maxStudents: num(form.maxStudents),
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
  | 'province'
  | 'ward'
  | 'addressDetail'
  | 'tuitionFee'
  | 'maxStudents'
  | 'startDate'
  | 'endDate';

interface FormErrors {
  fields: Partial<Record<FieldKey, string>>;
  slots: Record<number, string>;
}

function validateForm(form: FormState, isCreate: boolean): FormErrors {
  const fields: Partial<Record<FieldKey, string>> = {};
  const slots: Record<number, string> = {};
  const daily = form.recurringType === 'DAILY';

  if (!form.title.trim()) fields.title = 'Tiêu đề là bắt buộc';
  if (!form.categoryName.trim()) fields.categoryName = 'Danh mục là bắt buộc';
  if (!form.subjectName.trim()) fields.subjectName = 'Môn học là bắt buộc';
  if (form.gradeChoice === GRADE_OTHER && !form.gradeCustom.trim())
    fields.grade = 'Vui lòng nhập khối/lớp';
  if (!form.province.trim()) fields.province = 'Vui lòng chọn Tỉnh/Thành phố';
  if (!form.ward.trim()) fields.ward = 'Vui lòng chọn Phường/Xã';
  if (!form.addressDetail.trim()) fields.addressDetail = 'Vui lòng nhập địa chỉ cụ thể';

  const fee = Number(form.tuitionFee);
  if (!form.tuitionFee.trim() || Number.isNaN(fee) || fee <= 0)
    fields.tuitionFee = 'Học phí phải là số dương';

  const maxSt = Number(form.maxStudents);
  if (!form.maxStudents.trim() || !Number.isInteger(maxSt) || maxSt <= 0)
    fields.maxStudents = 'Số học sinh tối đa phải là số nguyên dương';

  if (!form.startDate) fields.startDate = 'Ngày bắt đầu là bắt buộc';
  else if (isCreate && form.startDate < TODAY)
    fields.startDate = 'Ngày bắt đầu phải từ hôm nay trở đi';
  if (!form.endDate) fields.endDate = 'Ngày kết thúc là bắt buộc';
  else if (form.startDate && form.endDate <= form.startDate)
    fields.endDate = 'Ngày kết thúc phải sau ngày bắt đầu';

  const allowed = allowedDaysInRange(form.startDate, form.endDate);
  form.schedule.forEach((s, i) => {
    if (!s.startTime || !s.endTime || s.endTime <= s.startTime) {
      slots[i] = 'Giờ kết thúc phải sau giờ bắt đầu';
      return;
    }
    // Hằng tuần: thứ phải nằm trong khoảng ngày. Hằng ngày: không cần thứ.
    if (!daily && !allowed.has(s.dayOfWeek))
      slots[i] = 'Ngày học không nằm trong khoảng ngày bắt đầu–kết thúc';
  });

  // Chống trùng/chồng giờ: DAILY so mọi tiết; WEEKLY so trong cùng một thứ.
  for (let i = 0; i < form.schedule.length; i++) {
    if (slots[i]) continue;
    for (let j = 0; j < i; j++) {
      if (slots[j]) continue;
      const a = form.schedule[i];
      const b = form.schedule[j];
      const timeOverlap = a.startTime < b.endTime && b.startTime < a.endTime;
      if (!timeOverlap) continue;
      if (daily) {
        slots[i] = 'Tiết bị trùng/chồng giờ với tiết khác';
        break;
      }
      if (a.dayOfWeek === b.dayOfWeek) {
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

  const errors = useMemo(() => validateForm(form, editingId == null), [form, editingId]);
  const sessionCount = useMemo(() => countSessions(form), [form]);
  const allowedDays = useMemo(
    () => allowedDaysInRange(form.startDate, form.endDate),
    [form.startDate, form.endDate],
  );
  const isDaily = form.recurringType === 'DAILY';

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
      closeDetail();
      setMode('form');
    } catch (err) {
      setListError(extractError(err, 'Không mở được lớp học để chỉnh sửa.'));
    }
  };

  const publish = async (classId: number) => {
    setListError('');
    try {
      await centerApi.publishClass(classId);
      reloadList();
      if (detailData?.classId === classId) refreshDetail(classId);
    } catch (err) {
      setListError(extractError(err, 'Không đăng tải được lớp học.'));
    }
  };

  // ----- Xem chi tiết lớp -----
  const [detailData, setDetailData] = useState<ClassResponse | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');
  const [showStudents, setShowStudents] = useState(false);

  const openDetail = async (classId: number) => {
    setDetailError('');
    setDetailLoading(true);
    setShowStudents(false);
    setDetailData({ classId } as ClassResponse); // mở modal ngay, hiển thị trạng thái tải
    try {
      const res = await centerApi.getClass(classId);
      setDetailData(res.data);
    } catch (err) {
      setDetailError(extractError(err, 'Không tải được chi tiết lớp học.'));
    } finally {
      setDetailLoading(false);
    }
  };

  const refreshDetail = async (classId: number) => {
    try {
      const res = await centerApi.getClass(classId);
      setDetailData(res.data);
    } catch {
      /* giữ nguyên dữ liệu cũ nếu tải lại lỗi */
    }
  };

  const closeDetail = () => {
    setDetailData(null);
    setDetailError('');
  };

  // ----- Gán gia sư -----
  const [assignFor, setAssignFor] = useState<ClassResponse | null>(null);
  const [tutors, setTutors] = useState<TutorOption[]>([]);
  const [tutorsLoading, setTutorsLoading] = useState(false);
  const [assignError, setAssignError] = useState('');
  const [assignBusyId, setAssignBusyId] = useState<number | null>(null);

  const openAssign = async (cls: ClassResponse) => {
    setAssignFor(cls);
    setAssignError('');
    setTutorsLoading(true);
    try {
      const res = await centerApi.getTutors(cls.classId);
      setTutors(res.data);
    } catch (err) {
      setAssignError(extractError(err, 'Không tải được danh sách gia sư.'));
    } finally {
      setTutorsLoading(false);
    }
  };

  const closeAssign = () => {
    setAssignFor(null);
    setTutors([]);
    setAssignError('');
    setAssignBusyId(null);
  };

  const pickTutor = async (tutorId: number) => {
    if (!assignFor) return;
    setAssignBusyId(tutorId);
    setAssignError('');
    const targetId = assignFor.classId;
    try {
      await centerApi.assignTutor(targetId, tutorId);
      closeAssign();
      reloadList();
      if (detailData?.classId === targetId) refreshDetail(targetId);
    } catch (err) {
      setAssignError(extractError(err, 'Không gán được gia sư.'));
      setAssignBusyId(null);
    }
  };

  const removeTutor = async (classId: number) => {
    setListError('');
    try {
      await centerApi.unassignTutor(classId);
      reloadList();
      if (detailData?.classId === classId) refreshDetail(classId);
    } catch (err) {
      setListError(extractError(err, 'Không gỡ được gia sư.'));
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
      <div className="cc-bg">
      <div className="cc-page">
      <header className="cc-header">
        <h1 className="cc-title">{pageTitle}</h1>
        {mode === 'list' ? (
          <div className="cc-row-actions">
            <Link className="cc-btn cc-btn--ghost" to="/center/schedule">
              📅 Lịch hôm nay
            </Link>
            <Link className="cc-btn cc-btn--ghost" to="/center/reschedules">
              🔄 Yêu cầu đổi lịch
            </Link>
            <button className="cc-btn cc-btn--primary" type="button" onClick={openCreate}>
              + Tạo lớp mới
            </button>
          </div>
        ) : (
          <button className="cc-btn cc-btn--ghost" type="button" onClick={() => setMode('list')}>
            ← Quay lại danh sách
          </button>
        )}
      </header>

      {mode === 'list' && (
        <>
          {listError && <div className="cc-alert cc-alert--error">{listError}</div>}
          {listLoading && <div className="cc-card cc-state">Đang tải danh sách lớp học…</div>}
          {!listLoading && !listError && classes.length === 0 && (
            <div className="cc-card cc-state">
              Chưa có lớp học nào. Bấm “Tạo lớp mới” để bắt đầu.
            </div>
          )}
          {!listLoading && classes.length > 0 && (
            <div className="cc-class-grid">
              {classes.map((c) => (
                <article className="cc-class-card" key={c.classId}>
                  <div className="cc-class-card__header">
                    <h3 className="cc-class-card__title" title={c.title}>
                      {c.title}
                    </h3>
                    <div className="cc-chips">
                      {c.subjectName && <span className="cc-chip">{c.subjectName}</span>}
                      {c.gradeName && <span className="cc-chip">{c.gradeName}</span>}
                      <span className="cc-chip">{LESSON_MODE_LABELS[c.lessonMode]}</span>
                    </div>
                    <span className={`cc-badge cc-badge--${c.status.toLowerCase()}`}>
                      {STATUS_LABELS[c.status]}
                    </span>
                    <button
                      className="cc-btn cc-btn--soft cc-btn--sm cc-class-card__detailbtn"
                      type="button"
                      onClick={() => openDetail(c.classId)}
                    >
                      Xem chi tiết →
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </>
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

            <div className="cc-field cc-field--full">
              <span className="cc-label">Địa điểm *</span>
              <LocationPicker
                value={{
                  province: form.province,
                  ward: form.ward,
                  addressDetail: form.addressDetail,
                }}
                onChange={(loc) =>
                  patch({
                    province: loc.province,
                    ward: loc.ward,
                    addressDetail: loc.addressDetail,
                  })
                }
                errors={{
                  province: errors.fields.province,
                  ward: errors.fields.ward,
                  addressDetail: errors.fields.addressDetail,
                }}
                showErrors={submitted}
              />
            </div>

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
              <span className="cc-label">Số học sinh tối đa *</span>
              <input
                className={errClass('maxStudents')}
                type="number"
                min={1}
                value={form.maxStudents}
                onChange={(e) => patch({ maxStudents: e.target.value })}
                placeholder="VD: 20"
              />
              {errText('maxStudents')}
            </label>
          </div>

          <div className="cc-grid cc-grid--after-schedule">
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

            <div />

            <label className="cc-field">
              <span className="cc-label">Ngày bắt đầu *</span>
              <input
                className={errClass('startDate')}
                type="date"
                min={editingId == null ? TODAY : undefined}
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
                min={form.startDate || TODAY}
                value={form.endDate}
                onChange={(e) => patch({ endDate: e.target.value })}
              />
              {errText('endDate')}
            </label>
          </div>

          <div className="cc-schedule">
            <div className="cc-schedule__head">
              <span className="cc-label">
                {isDaily ? 'Các tiết trong ngày (ít nhất 1) *' : 'Lịch học (ít nhất 1 khung) *'}
              </span>
              <button className="cc-btn cc-btn--sm" type="button" onClick={addSlot}>
                {isDaily ? '+ Thêm tiết' : '+ Thêm khung'}
              </button>
            </div>
            {form.schedule.map((slot, index) => {
              const slotError = submitted ? errors.slots[index] : undefined;
              const timeClass = `cc-input${slotError ? ' cc-input--error' : ''}`;
              const dayOptions = DAYS.filter((d) => allowedDays.has(d.value));
              if (!allowedDays.has(slot.dayOfWeek))
                dayOptions.push({ value: slot.dayOfWeek, label: `${dayLabel(slot.dayOfWeek)} (ngoài phạm vi)` });
              return (
                <div className="cc-slot-row" key={index}>
                  <div className="cc-slot">
                    {!isDaily && (
                      <select
                        className={`cc-input${slotError ? ' cc-input--error' : ''}`}
                        value={slot.dayOfWeek}
                        onChange={(e) => updateSlot(index, { dayOfWeek: Number(e.target.value) })}
                      >
                        {dayOptions.map((d) => (
                          <option key={d.value} value={d.value}>
                            {d.label}
                          </option>
                        ))}
                      </select>
                    )}
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
            <p className="cc-hint">
              {isDaily
                ? 'Các tiết áp dụng cho mọi ngày trong khoảng đã chọn.'
                : 'Chỉ chọn được các thứ nằm trong khoảng ngày bắt đầu–kết thúc.'}
            </p>
          </div>

          <div className="cc-grid cc-grid--after-schedule">
            <label className="cc-field">
              <span className="cc-label">Số buổi học</span>
              <input className="cc-input cc-input--readonly" value={sessionCount} readOnly />
              <span className="cc-hint">Tổng số buổi theo lịch trong khoảng ngày đã chọn.</span>
            </label>
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

      {detailData && (
        <div className="cc-modal" role="dialog" aria-modal="true">
          <div className="cc-modal__backdrop" onClick={closeDetail} />
          <div className="cc-modal__card cc-modal__card--lg">
            <div className="cc-modal__head">
              <div>
                <h2 className="cc-modal__title">
                  {detailLoading ? 'Chi tiết lớp học' : detailData.title}
                </h2>
                {!detailLoading && (
                  <span className={`cc-badge cc-badge--${detailData.status.toLowerCase()}`}>
                    {STATUS_LABELS[detailData.status]}
                  </span>
                )}
              </div>
              <button
                className="cc-modal__close"
                type="button"
                onClick={closeDetail}
                aria-label="Đóng"
              >
                ×
              </button>
            </div>
            <div className="cc-modal__body">
              {detailError && <div className="cc-alert cc-alert--error">{detailError}</div>}
              {detailLoading && <div className="cc-state">Đang tải chi tiết…</div>}

              {!detailLoading && (
                <>
                  <div className="cc-chips cc-detail__chips">
                    {detailData.subjectName && (
                      <span className="cc-chip">{detailData.subjectName}</span>
                    )}
                    {detailData.gradeName && <span className="cc-chip">{detailData.gradeName}</span>}
                    <span className="cc-chip">{LESSON_MODE_LABELS[detailData.lessonMode]}</span>
                  </div>

                  {detailData.description && (
                    <div className="cc-detail__section">
                      <span className="cc-detail__label">Mô tả</span>
                      <p className="cc-detail__desc">{detailData.description}</p>
                    </div>
                  )}

                  <dl className="cc-detail__grid">
                    <div className="cc-detail__item">
                      <dt>Danh mục</dt>
                      <dd>{detailData.categoryName ?? '—'}</dd>
                    </div>
                    <div className="cc-detail__item">
                      <dt>Địa điểm</dt>
                      <dd>{detailData.locationLabel ?? detailData.locationText ?? '—'}</dd>
                    </div>
                    <div className="cc-detail__item">
                      <dt>Kiểu lặp lịch</dt>
                      <dd>{RECURRING_LABELS[detailData.recurringType]}</dd>
                    </div>
                    <div className="cc-detail__item">
                      <dt>Thời gian</dt>
                      <dd>
                        {detailData.startDate} → {detailData.endDate}
                      </dd>
                    </div>
                    <div className="cc-detail__item">
                      <dt>Số buổi</dt>
                      <dd>{detailData.numberOfSessions}</dd>
                    </div>
                    <div className="cc-detail__item">
                      <dt>Sĩ số tối đa</dt>
                      <dd>{detailData.maxStudents ?? '—'}</dd>
                    </div>
                    <div className="cc-detail__item">
                      <dt>Học phí</dt>
                      <dd className="cc-fee">{formatCurrency(detailData.tuitionFee)}</dd>
                    </div>
                  </dl>

                  <div className="cc-detail__section">
                    <span className="cc-detail__label">Lịch học</span>
                    {detailData.schedule.length === 0 ? (
                      <p className="cc-muted">Chưa có lịch học.</p>
                    ) : (
                      <ul className="cc-detail__slots">
                        {detailData.schedule.map((s, i) => (
                          <li className="cc-detail__slot" key={s.slotId ?? i}>
                            <span className="cc-detail__slotday">
                              {detailData.recurringType === 'DAILY'
                                ? 'Hằng ngày'
                                : dayLabel(s.dayOfWeek)}
                            </span>
                            <span className="cc-detail__slottime">
                              {s.startTime.slice(0, 5)} → {s.endTime.slice(0, 5)}
                            </span>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>

                  <div className="cc-detail__section">
                    <span className="cc-detail__label">Gia sư dạy</span>
                    {detailData.assignedTutorName ? (
                      <div className="cc-class-tutor__row">
                        <span className="cc-tutor-badge">
                          {initials(detailData.assignedTutorName)}
                        </span>
                        <span className="cc-tutor-name" title={detailData.assignedTutorName}>
                          {detailData.assignedTutorName}
                        </span>
                        <div className="cc-row-actions">
                          <button
                            className="cc-btn cc-btn--sm"
                            type="button"
                            onClick={() => openAssign(detailData)}
                          >
                            Đổi
                          </button>
                          <button
                            className="cc-btn cc-btn--danger cc-btn--sm"
                            type="button"
                            onClick={() => removeTutor(detailData.classId)}
                          >
                            Gỡ
                          </button>
                        </div>
                      </div>
                    ) : (
                      <button
                        className="cc-btn cc-btn--soft cc-btn--sm"
                        type="button"
                        onClick={() => openAssign(detailData)}
                      >
                        + Gán gia sư
                      </button>
                    )}
                  </div>

                  {detailData.students && detailData.students.length > 0 && (
                    <div className="cc-detail__section">
                      <button
                        type="button"
                        className="cc-detail__toggle"
                        onClick={() => setShowStudents((v) => !v)}
                        aria-expanded={showStudents}
                      >
                        <span>👥 Học sinh đã đăng ký ({detailData.students.length})</span>
                        <span className={`cc-detail__chev${showStudents ? ' is-open' : ''}`}>▾</span>
                      </button>
                      {showStudents && (
                        <ul className="cc-detail__students">
                          {detailData.students.map((st, i) => (
                            <li className="cc-detail__student" key={st.classStudentId}>
                              <span className="cc-detail__studentnum">{i + 1}</span>
                              <div className="cc-detail__studentinfo">
                                <span className="cc-detail__studentname">{st.studentName}</span>
                                {st.studentPhone && (
                                  <span className="cc-detail__studentphone">{st.studentPhone}</span>
                                )}
                              </div>
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  )}

                  <div className="cc-detail__foot">
                    {canEditStatus(detailData.status) && (
                      <button
                        className="cc-btn cc-btn--ghost"
                        type="button"
                        onClick={() => openEdit(detailData.classId)}
                      >
                        ✎ Sửa lớp học
                      </button>
                    )}
                    {detailData.status === 'DRAFT' && (
                      <button
                        className="cc-btn cc-btn--primary"
                        type="button"
                        onClick={() => publish(detailData.classId)}
                      >
                        Đăng tải
                      </button>
                    )}
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      )}

      {assignFor && (
        <div className="cc-modal" role="dialog" aria-modal="true">
          <div className="cc-modal__backdrop" onClick={closeAssign} />
          <div className="cc-modal__card">
            <div className="cc-modal__head">
              <div>
                <h2 className="cc-modal__title">Gán gia sư</h2>
                <p className="cc-modal__sub">Lớp: {assignFor.title}</p>
              </div>
              <button
                className="cc-modal__close"
                type="button"
                onClick={closeAssign}
                aria-label="Đóng"
              >
                ×
              </button>
            </div>
            <div className="cc-modal__body">
              {assignError && <div className="cc-alert cc-alert--error">{assignError}</div>}
              {tutorsLoading && <div className="cc-state">Đang tải danh sách gia sư…</div>}
              {!tutorsLoading && !assignError && tutors.length === 0 && (
                <div className="cc-state">Chưa có gia sư nào.</div>
              )}
              {!tutorsLoading && tutors.length > 0 && (
                <ul className="cc-tutor-list">
                  {tutors.map((t) => {
                    const selected = assignFor.assignedTutorId === t.tutorId;
                    const conflict = !!t.scheduleConflict && !selected;
                    return (
                      <li
                        className={`cc-tutor${conflict ? ' cc-tutor--conflict' : ''}`}
                        key={t.tutorId}
                      >
                        <div className="cc-tutor__avatar">{initials(t.fullName)}</div>
                        <div className="cc-tutor__info">
                          <div className="cc-tutor__name">
                            {t.fullName}
                            {t.verificationStatus === 'VERIFIED' && (
                              <span className="cc-tutor__verified">✓ Đã xác minh</span>
                            )}
                          </div>
                          <div className="cc-tutor__meta">
                            {t.experienceYears != null && <span>{t.experienceYears} năm KN</span>}
                            {t.ratingAvg != null && <span>★ {t.ratingAvg}</span>}
                            {t.phone && <span>{t.phone}</span>}
                          </div>
                          {conflict ? (
                            <div className="cc-tutor__conflict">
                              ⚠ Trùng lịch dạy
                              {t.conflictClassTitle ? ` với lớp "${t.conflictClassTitle}"` : ''}
                            </div>
                          ) : (
                            t.bio && <div className="cc-tutor__bio">{t.bio}</div>
                          )}
                        </div>
                        <button
                          className="cc-btn cc-btn--primary cc-btn--sm"
                          type="button"
                          disabled={assignBusyId === t.tutorId || selected || conflict}
                          onClick={() => pickTutor(t.tutorId)}
                        >
                          {selected
                            ? 'Đang dạy'
                            : conflict
                              ? 'Trùng lịch'
                              : assignBusyId === t.tutorId
                                ? 'Đang gán…'
                                : 'Chọn'}
                        </button>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
          </div>
        </div>
      )}
      </div>
      </div>
    </>
  );
}
