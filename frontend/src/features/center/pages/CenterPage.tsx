import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import axios from 'axios';
import { VerificationHeader } from '../../../shared/components/VerificationHeader';
import { FilePreviewModal } from '../../../shared/components/FilePreviewModal';
import { ExpiryBadge } from '../../../shared/components/ExpiryBadge';
import { CenterSidebar } from '../components/CenterSidebar';
import { ChatButton } from '../../messaging/components/ChatButton';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { LocationPicker } from '../components/LocationPicker';
import { profileApi } from '../../profile/api/profileApi';
import { centerApi } from '../api/centerApi';
import type { ClassRequest } from '../../marketplace/types/marketplaceTypes';
import type {
  CenterMember,
  ClassResponse,
  ClassStatus,
  ContractTemplate,
  LessonMode,
  RecruitmentApplication,
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
  minStudents: string;
  originType: 'SELF' | 'EXTERNAL';
  contractTemplateId: string;
  contractContent: string;
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
  minStudents: '',
  originType: 'SELF',
  contractTemplateId: '',
  contractContent: '',
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

/** Mã lỗi backend trả về (VD: "VERIFICATION_REQUIRED") để frontend xử lý riêng. */
function errorCode(error: unknown): string | undefined {
  if (axios.isAxiosError(error) && typeof error.response?.data?.code === 'string') {
    return error.response.data.code;
  }
  return undefined;
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
    minStudents: c.minStudents != null ? String(c.minStudents) : '',
    originType: c.originType === 'EXTERNAL' ? 'EXTERNAL' : 'SELF',
    contractTemplateId: c.contractTemplateId != null ? String(c.contractTemplateId) : '',
    contractContent: c.contractContent ?? '',
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

const CENTER_REQUEST_FEE_STATUS_LABELS: Record<string, string> = {
  PENDING_PAYMENT: 'Chờ thanh toán phí',
  HELD: 'Đang giữ phí',
  REFUND_REQUESTED: 'Đang xử lý hoàn phí',
  RELEASED: 'Đã giải ngân cho trung tâm',
  REFUNDED: 'Đã hoàn phí',
  CANCELLED: 'Đã hủy',
};

const CENTER_REQUEST_FEE_STATUS_TONES: Record<string, string> = {
  PENDING_PAYMENT: 'pending',
  HELD: 'held',
  REFUND_REQUESTED: 'warning',
  RELEASED: 'released',
  REFUNDED: 'refunded',
  CANCELLED: 'cancelled',
};

function centerRequestFeeStatusLabel(status?: string | null): string {
  return status ? (CENTER_REQUEST_FEE_STATUS_LABELS[status] ?? status) : 'Chưa có phí xử lý';
}

function centerRequestFeeStatusTone(status?: string | null): string {
  return CENTER_REQUEST_FEE_STATUS_TONES[status ?? ''] ?? 'pending';
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
    // Lớp theo yêu cầu không dùng tối thiểu (không mở ghi danh).
    minStudents: form.originType === 'EXTERNAL' ? null : num(form.minStudents),
    originType: form.originType,
    contractTemplateId: form.contractTemplateId ? Number(form.contractTemplateId) : null,
    contractContent: form.contractContent,
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
  | 'minStudents'
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

  // Lớp tự tạo: nếu nhập tối thiểu thì phải là số dương và không lớn hơn tối đa.
  if (form.originType !== 'EXTERNAL' && form.minStudents.trim()) {
    const minSt = Number(form.minStudents);
    if (!Number.isInteger(minSt) || minSt <= 0)
      fields.minStudents = 'Số học sinh tối thiểu phải là số nguyên dương';
    else if (Number.isInteger(maxSt) && maxSt > 0 && minSt > maxSt)
      fields.minStudents = 'Tối thiểu không được lớn hơn tối đa';
  }

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

function splitFirst(s: string, sep: string): [string, string] {
  const i = s.indexOf(sep);
  return i < 0 ? [s, ''] : [s.slice(0, i).trim(), s.slice(i + sep.length).trim()];
}

/**
 * Trình bày phần tóm tắt yêu cầu (r.note) dạng có cấu trúc thay vì một đoạn text dài.
 * Note do phụ huynh gửi được ghép cố định: "Môn học: …" \n "Lịch học … . <môn>[phí]: <buổi>; …"
 * \n "<ghi chú tự do>". Nếu không khớp cấu trúc thì hiển thị nguyên văn (an toàn với dữ liệu cũ).
 */
function RequestNote({ note }: { note: string }) {
  const lines = (note ?? '')
    .split('\n')
    .map((l) => l.trim())
    .filter(Boolean);
  let subjects: string[] = [];
  let scheduleHeader = '';
  let scheduleRows: string[] = [];
  const extra: string[] = [];
  for (const line of lines) {
    if (line.startsWith('Môn học:')) {
      subjects = line
        .slice('Môn học:'.length)
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean);
    } else if (line.startsWith('Lịch học')) {
      const idx = line.indexOf('. ');
      if (idx >= 0) {
        scheduleHeader = line.slice(0, idx).trim();
        scheduleRows = line
          .slice(idx + 2)
          .replace(/\.\s*$/, '')
          .split(';')
          .map((s) => s.trim())
          .filter(Boolean);
      } else {
        scheduleHeader = line.replace(/\.\s*$/, '');
      }
    } else {
      extra.push(line);
    }
  }
  if (subjects.length === 0 && !scheduleHeader && scheduleRows.length === 0) {
    return <p className="cc-request__note">{note}</p>;
  }
  return (
    <div className="cc-reqnote">
      {subjects.length > 0 && (
        <div className="cc-reqnote__row">
          <span className="cc-reqnote__key">Môn học</span>
          <span className="cc-reqnote__subjects">
            {subjects.map((s) => (
              <span key={s} className="cc-chip">
                {s}
              </span>
            ))}
          </span>
        </div>
      )}
      {(scheduleHeader || scheduleRows.length > 0) && (
        <div className="cc-reqnote__row">
          <span className="cc-reqnote__key">Lịch học</span>
          <div className="cc-reqnote__sched">
            {scheduleHeader && <span className="cc-reqnote__schedhead">{scheduleHeader}</span>}
            {scheduleRows.map((row, i) => {
              const [subj, when] = splitFirst(row, ': ');
              const m = subj.match(/^(.*?)\s*\[(.*?)\]$/);
              const name = m ? m[1] : subj;
              const fee = m ? m[2] : '';
              return (
                <div key={i} className="cc-reqnote__slot">
                  <span className="cc-reqnote__slot-subj">
                    {name}
                    {fee && <span className="cc-reqnote__fee">{fee}</span>}
                  </span>
                  {when && <span className="cc-reqnote__slot-when">{when}</span>}
                </div>
              );
            })}
          </div>
        </div>
      )}
      {extra.length > 0 && (
        <div className="cc-reqnote__row">
          <span className="cc-reqnote__key">Ghi chú</span>
          <span className="cc-reqnote__extra">{extra.join(' ')}</span>
        </div>
      )}
    </div>
  );
}

export default function CenterPage() {
  const navigate = useNavigate();
  const [classes, setClasses] = useState<ClassResponse[]>([]);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState('');

  // Trạng thái xác minh trung tâm: null = đang tải, true/false = đã biết.
  const [verified, setVerified] = useState<boolean | null>(null);

  const [mode, setMode] = useState<'list' | 'form' | 'requests'>('list');
  // Đồng bộ chế độ hiển thị theo route: /center/requests -> "Yêu cầu mở lớp", còn lại -> danh sách lớp.
  const location = useLocation();
  useEffect(() => {
    setMode(location.pathname.endsWith('/requests') ? 'requests' : 'list');
  }, [location.pathname]);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  // Ô điều khoản HĐ mặc định thu gọn; bấm để mở rộng khi cần xem/sửa.
  const [contractExpanded, setContractExpanded] = useState(false);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');
  const [submitted, setSubmitted] = useState(false);

  // Yêu cầu mở lớp do phụ huynh gửi. Khi chấp nhận, tái dùng form tạo lớp bên dưới:
  // acceptingRequestId != null -> submit form sẽ gọi acceptClassRequest thay vì createClass.
  const [requests, setRequests] = useState<ClassRequest[]>([]);
  const [acceptingRequestId, setAcceptingRequestId] = useState<string | null>(null);
  // Đơn ứng tuyển (ngoài đội) theo từng yêu cầu đã đăng tin — để duyệt vào shortlist.
  const [reqApps, setReqApps] = useState<Record<string, RecruitmentApplication[]>>({});
  const [postingReqId, setPostingReqId] = useState<string | null>(null);
  const [decidingAppId, setDecidingAppId] = useState<number | null>(null);
  // Đơn đang mở xem hồ sơ + xem trước file chứng chỉ.
  const [certsOpenId, setCertsOpenId] = useState<number | null>(null);
  const [certPreview, setCertPreview] = useState<{
    src: string;
    fileName: string;
    mimeType?: string | null;
  } | null>(null);
  // Xác nhận "không tìm được gia sư" (đóng yêu cầu + báo phụ huynh) — inline, không dùng popup trình duyệt.
  const [giveUpId, setGiveUpId] = useState<string | null>(null);
  const [giveUpBusy, setGiveUpBusy] = useState(false);
  // Từ chối yêu cầu — nhập lý do inline (không dùng popup trình duyệt).
  const [rejectId, setRejectId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState('');
  const [rejectBusy, setRejectBusy] = useState(false);

  // Mẫu hợp đồng (để chọn khi tạo lớp).
  const [templates, setTemplates] = useState<ContractTemplate[]>([]);
  // Đội gia sư của trung tâm — để đề cử vào shortlist yêu cầu.
  const [members, setMembers] = useState<CenterMember[]>([]);

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

  // Bước 13b/14: trung tâm xác nhận khóa học hoàn thành (sau khi gia sư đã xác nhận) -> tất toán + đóng lớp.
  const [completingClassId, setCompletingClassId] = useState<number | null>(null);
  const confirmCenterComplete = async (classId: number) => {
    setCompletingClassId(classId);
    setListError('');
    try {
      await centerApi.completeClass(classId);
      reloadList();
    } catch (err) {
      setListError(extractError(err, 'Không xác nhận được hoàn thành lớp.'));
    } finally {
      setCompletingClassId(null);
    }
  };

  const reloadRequests = () => {
    centerApi
      .getClassRequests()
      .then((res) => setRequests(res.data))
      .catch(() => setRequests([]));
  };

  useEffect(() => {
    reloadList();
    reloadRequests();
    centerApi
      .getContractTemplates()
      .then((res) => setTemplates(res.data))
      .catch(() => setTemplates([]));
    centerApi
      .getMembers()
      .then((res) => setMembers(res.data))
      .catch(() => setMembers([]));
  }, []);

  // Yêu cầu nào đã đăng tin tuyển -> tải đơn ứng tuyển (ngoài đội) để trung tâm duyệt.
  useEffect(() => {
    requests
      .filter((r) => r.recruitmentPostId != null && r.status === 'SEARCHING')
      .forEach((r) => loadRequestApplications(r.requestId));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [requests]);

  // Tải trạng thái xác minh trung tâm để chặn tạo lớp khi chưa xác minh.
  useEffect(() => {
    let alive = true;
    profileApi.http
      .get<{ verificationStatus?: string }>('/profile/me')
      .then((res) => {
        if (alive) setVerified(res.data.verificationStatus === 'VERIFIED');
      })
      .catch(() => {
        if (alive) setVerified(null);
      });
    return () => {
      alive = false;
    };
  }, []);

  const goVerify = () =>
    navigate(APP_ROUTES.verification, {
      state: { notice: 'Trung tâm của bạn cần được xác minh trước khi tạo lớp học.' },
    });

  const openCreate = () => {
    // Mức 1: chưa xác minh -> không mở form, chuyển hướng sang trang Xác minh.
    if (verified === false) {
      goVerify();
      return;
    }
    setAcceptingRequestId(null);
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormError('');
    setSubmitted(false);
    setMode('form');
  };

  // Quay về danh sách, đồng thời huỷ trạng thái "đang chấp nhận yêu cầu".
  const backToList = () => {
    setAcceptingRequestId(null);
    setMode('list');
  };

  // Từ chối yêu cầu với lý do nhập inline (thay cho window.prompt).
  const confirmReject = async (req: ClassRequest) => {
    setRejectBusy(true);
    setListError('');
    try {
      await centerApi.rejectClassRequest(req.requestId, rejectReason.trim());
      setRejectId(null);
      setRejectReason('');
      reloadRequests();
    } catch (err) {
      setListError(extractError(err, 'Không từ chối được yêu cầu.'));
    } finally {
      setRejectBusy(false);
    }
  };

  // Trung tâm nhận tìm gia sư cho yêu cầu (PENDING -> ĐANG TÌM).
  const startSearchRequest = async (req: ClassRequest) => {
    try {
      await centerApi.startSearchClassRequest(req.requestId);
      reloadRequests();
    } catch (err) {
      setListError(extractError(err, 'Không nhận tìm được yêu cầu.'));
    }
  };

  // Đề cử / gỡ gia sư (từ đội) vào shortlist của một yêu cầu.
  const proposeTutorToRequest = async (requestId: string, tutorId: number) => {
    if (!tutorId) return;
    try {
      await centerApi.proposeTutor(requestId, tutorId);
      reloadRequests();
    } catch (err) {
      setListError(extractError(err, 'Không đề cử được gia sư.'));
    }
  };
  const removeCandidateFromRequest = async (requestId: string, tutorId: number) => {
    try {
      await centerApi.removeCandidate(requestId, tutorId);
      reloadRequests();
    } catch (err) {
      setListError(extractError(err, 'Không gỡ được gia sư.'));
    }
  };

  // Tải đơn ứng tuyển (ngoài đội) của một yêu cầu đã đăng tin.
  const loadRequestApplications = (requestId: string) => {
    centerApi
      .getRequestApplications(requestId)
      .then((res) => setReqApps((prev) => ({ ...prev, [requestId]: res.data })))
      .catch(() => setReqApps((prev) => ({ ...prev, [requestId]: [] })));
  };

  // Đăng tin tuyển gia sư NGOÀI đội cho yêu cầu (tin hiện ngay ở "Tin tuyển dụng" phía gia sư).
  const postRecruitment = async (req: ClassRequest) => {
    setPostingReqId(req.requestId);
    setListError('');
    try {
      await centerApi.postRecruitmentForRequest(req.requestId);
      reloadRequests();
    } catch (err) {
      setListError(extractError(err, 'Không đăng được tin tuyển.'));
    } finally {
      setPostingReqId(null);
    }
  };

  // Trung tâm không tìm được gia sư -> đóng yêu cầu + hệ thống báo cho phụ huynh.
  const giveUpRequest = async (req: ClassRequest) => {
    setGiveUpBusy(true);
    setListError('');
    try {
      await centerApi.giveUpClassRequest(req.requestId, '');
      setGiveUpId(null);
      reloadRequests();
    } catch (err) {
      setListError(extractError(err, 'Không đóng được yêu cầu.'));
    } finally {
      setGiveUpBusy(false);
    }
  };

  // Duyệt đơn ngoài đội: đồng ý -> đưa gia sư vào shortlist; bỏ qua -> loại đơn.
  const decideRequestApplication = async (
    requestId: string,
    appId: number,
    approve: boolean,
  ) => {
    setDecidingAppId(appId);
    setListError('');
    try {
      await centerApi.decideApplication(appId, approve);
      loadRequestApplications(requestId);
      reloadRequests();
    } catch (err) {
      setListError(extractError(err, 'Không xử lý được đơn ứng tuyển.'));
    } finally {
      setDecidingAppId(null);
    }
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
    setDetailError('');
    try {
      await centerApi.publishClass(classId);
      reloadList();
      if (detailData?.classId === classId) refreshDetail(classId);
    } catch (err) {
      const msg = extractError(err, 'Không đăng tải được lớp học.');
      setDetailError(msg);
      setListError(msg);
    }
  };

  const closeEnrollment = async (classId: number) => {
    setListError('');
    setDetailError('');
    try {
      await centerApi.closeEnrollment(classId);
      reloadList();
      if (detailData?.classId === classId) refreshDetail(classId);
    } catch (err) {
      const msg = extractError(err, 'Không đóng ghi danh được lớp học.');
      setDetailError(msg);
      setListError(msg);
    }
  };

  const activateClass = async (classId: number) => {
    setListError('');
    setDetailError('');
    try {
      await centerApi.activateClass(classId);
      reloadList();
      if (detailData?.classId === classId) refreshDetail(classId);
    } catch (err) {
      const msg = extractError(err, 'Không kích hoạt được lớp học.');
      setDetailError(msg);
      setListError(msg);
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

  // ----- Gán gia sư (chính / phụ) -----
  type AssignMode = 'main' | 'assistant';
  const [assignFor, setAssignFor] = useState<ClassResponse | null>(null);
  const [assignMode, setAssignMode] = useState<AssignMode>('main');
  const [tutors, setTutors] = useState<TutorOption[]>([]);
  const [tutorsLoading, setTutorsLoading] = useState(false);
  const [assignError, setAssignError] = useState('');
  const [assignBusyId, setAssignBusyId] = useState<number | null>(null);

  const openAssign = async (cls: ClassResponse, mode: AssignMode = 'main') => {
    setAssignFor(cls);
    setAssignMode(mode);
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
    const isAssistant = assignMode === 'assistant';
    try {
      if (isAssistant) {
        await centerApi.assignAssistant(targetId, tutorId);
      } else {
        await centerApi.assignTutor(targetId, tutorId);
      }
      closeAssign();
      reloadList();
      if (detailData?.classId === targetId) refreshDetail(targetId);
    } catch (err) {
      setAssignError(
        extractError(err, isAssistant ? 'Không gán được gia sư phụ.' : 'Không gán được gia sư.'),
      );
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

  const removeAssistant = async (classId: number) => {
    setListError('');
    try {
      await centerApi.unassignAssistant(classId);
      reloadList();
      if (detailData?.classId === classId) refreshDetail(classId);
    } catch (err) {
      setListError(extractError(err, 'Không gỡ được gia sư phụ.'));
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
      if (acceptingRequestId != null) {
        // Chấp nhận yêu cầu của phụ huynh -> tạo lớp EXTERNAL từ yêu cầu đó.
        await centerApi.acceptClassRequest(acceptingRequestId, payload);
        setAcceptingRequestId(null);
        reloadRequests();
      } else if (editingId != null) {
        await centerApi.updateClass(editingId, payload);
      } else {
        await centerApi.createClass(payload);
      }
      setMode('list');
      reloadList();
    } catch (err) {
      // Phòng thủ: nếu backend chặn vì chưa xác minh -> điều hướng sang trang Xác minh.
      if (errorCode(err) === 'VERIFICATION_REQUIRED') {
        setVerified(false);
        goVerify();
        return;
      }
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

  // BR-06 / AF-03: backend là nguồn quyết định (cờ `editable`). Chỉ suy luận tại chỗ
  // khi phản hồi chưa có cờ đó, để giao diện không bao giờ mở nút Sửa rộng hơn server.
  /**
   * BF-04: lớp đã đăng tải chỉ mở ghi danh 30 ngày. Quá hạn mà đủ sĩ số tối thiểu thì
   * tự đóng ghi danh, thiếu sĩ số thì lớp bị hủy và hoàn tiền cho học viên đã đóng.
   */
  const enrollmentCountdown = (c: ClassResponse) => {
    if (c.status !== 'OPEN' || !c.enrollmentExpiresAt) return null;
    const lastDay = c.enrollmentDeadline
      ? new Date(c.enrollmentDeadline).toLocaleDateString('vi-VN')
      : null;
    return (
      <ExpiryBadge
        expiresAt={c.enrollmentExpiresAt}
        expiredLabel="Hết hạn ghi danh"
        title={
          (lastDay ? `Ghi danh mở đến hết ngày ${lastDay}. ` : '') +
          'Quá hạn: đủ sĩ số tối thiểu thì lớp tự đóng ghi danh, ' +
          'thiếu sĩ số thì lớp bị hủy và hoàn tiền cho học viên đã thanh toán.'
        }
      />
    );
  };

  const canEditClass = (c: ClassResponse) =>
    c.editable ?? ((c.status === 'DRAFT' || c.status === 'OPEN') && c.enrolledCount === 0);

  const pageTitle = useMemo(
    () =>
      mode === 'form'
        ? editingId != null
          ? 'Chỉnh sửa lớp học'
          : acceptingRequestId != null
            ? 'Tạo lớp từ yêu cầu phụ huynh'
            : 'Tạo lớp học mới'
        : mode === 'requests'
          ? 'Yêu cầu mở lớp từ phụ huynh'
          : 'Lớp học của tôi',
    [mode, editingId, acceptingRequestId],
  );

  return (
    <>
      <VerificationHeader />
      <div className="cc-bg">
      <div className="cc-shell">
      <CenterSidebar />
      <div className="cc-page">
      <header className="cc-header">
        <h1 className="cc-title">{pageTitle}</h1>
        {mode === 'list' ? (
          <div className="cc-row-actions">
            <button className="cc-btn cc-btn--primary" type="button" onClick={openCreate}>
              Tạo lớp mới
            </button>
          </div>
        ) : mode === 'form' ? (
          <button className="cc-btn cc-btn--ghost" type="button" onClick={backToList}>
            ← Quay lại danh sách
          </button>
        ) : null}
      </header>

      {mode === 'list' && (
        <>
          {verified === false && (
            <div className="cc-alert cc-alert--warn cc-verify-banner">
              <span>
                ⚠ Trung tâm của bạn <b>chưa được xác minh</b>. Bạn cần hoàn tất xác minh trước khi
                tạo lớp học.
              </span>
              <button className="cc-btn cc-btn--primary cc-btn--sm" type="button" onClick={goVerify}>
                Đi xác minh →
              </button>
            </div>
          )}
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
                    {enrollmentCountdown(c)}
                    <button
                      className="cc-btn cc-btn--soft cc-btn--sm cc-class-card__detailbtn"
                      type="button"
                      onClick={() => openDetail(c.classId)}
                    >
                      Xem chi tiết →
                    </button>
                  </div>
                  {c.tutorCompletionConfirmed && c.status !== 'COMPLETED' && (
                    <div className="cc-class-card__complete">
                      <span>✅ Gia sư đã xác nhận hoàn thành khóa học.</span>
                      <button
                        className="cc-btn cc-btn--primary cc-btn--sm"
                        type="button"
                        disabled={completingClassId === c.classId}
                        onClick={() => confirmCenterComplete(c.classId)}
                      >
                        {completingClassId === c.classId
                          ? 'Đang xác nhận…'
                          : 'Xác nhận & đóng lớp'}
                      </button>
                    </div>
                  )}
                </article>
              ))}
            </div>
          )}
        </>
      )}

      {mode === 'requests' && (
        <>
          {listError && <div className="cc-alert cc-alert--error">{listError}</div>}
          {requests.filter((r) => r.status === 'PENDING' || r.status === 'SEARCHING').length === 0 ? (
            <div className="cc-card cc-state">
              Hiện chưa có yêu cầu mở lớp nào từ phụ huynh.
            </div>
          ) : (
            <div className="cc-card cc-request-inbox">
              <h2 className="cc-request-inbox__title">
                Yêu cầu mở lớp từ phụ huynh (
                {requests.filter((r) => r.status === 'PENDING' || r.status === 'SEARCHING').length})
              </h2>
              <div className="cc-request-list">
                {requests
                  .filter((r) => r.status === 'PENDING' || r.status === 'SEARCHING')
                  .map((r) => (
                    <div className="cc-request" key={r.requestId}>
                      {/* Đầu thẻ: trạng thái + tóm tắt yêu cầu */}
                      <div className="cc-request__head">
                        <span className={`cc-req-status cc-req-status--${r.status.toLowerCase()}`}>
                          {r.status === 'SEARCHING' ? '🔎 Đang tìm gia sư' : '🕒 Chờ nhận'}
                        </span>
                        {r.detailsJson && (
                          <span className="cc-req-tag">Có thông tin chi tiết</span>
                        )}
                      </div>
                      <RequestNote note={r.note} />
                      <div className="cc-request__meta">
                        <span>👤 {r.clientName ?? '—'}</span>
                        {r.categoryName && <span>📚 {r.categoryName}</span>}
                        {r.desiredBudget != null && (
                          <span>💰 {formatCurrency(r.desiredBudget)}</span>
                        )}
                      </div>

                      {r.centerRequestFeePayment && (
                        <div className="cc-request-fee">
                          <span className="cc-request-fee__amount">
                            Phí xử lý: {formatCurrency(r.centerRequestFeePayment.amount)}
                          </span>
                          <span
                            className={`cc-request-fee__status cc-request-fee__status--${centerRequestFeeStatusTone(
                              r.centerRequestFeePayment.status,
                            )}`}
                          >
                            {centerRequestFeeStatusLabel(r.centerRequestFeePayment.status)}
                          </span>
                          <span className="cc-request-fee__ref">
                            {r.centerRequestFeePayment.referenceCode}
                          </span>
                        </div>
                      )}

                      {r.status === 'SEARCHING' && (
                        <div className="cc-req-sources">
                          {/* Nguồn 1: gia sư trong đội */}
                          <section className="cc-req-sec">
                            <div className="cc-req-sec__head">
                              <span className="cc-req-sec__label">👥 Từ đội của bạn</span>
                              <span className="cc-req-sec__hint">
                                Đề cử {r.candidates?.length ?? 0} — phụ huynh chọn 1
                              </span>
                            </div>
                            <div className="cc-req-sec__body">
                              {r.candidates && r.candidates.length > 0 && (
                                <div className="cc-shortlist__chips">
                                  {r.candidates.map((c) => (
                                    <span key={c.tutorId} className="cc-chip cc-shortlist__chip">
                                      {c.fullName}
                                      <button
                                        type="button"
                                        className="cc-shortlist__x"
                                        aria-label="Gỡ gia sư"
                                        onClick={() =>
                                          removeCandidateFromRequest(r.requestId, c.tutorId)
                                        }
                                      >
                                        ×
                                      </button>
                                    </span>
                                  ))}
                                </div>
                              )}
                              <select
                                className="cc-input cc-shortlist__pick"
                                value=""
                                onChange={(e) => {
                                  const id = Number(e.target.value);
                                  if (id) proposeTutorToRequest(r.requestId, id);
                                }}
                              >
                                <option value="">+ Đề cử gia sư từ đội…</option>
                                {members
                                  .filter(
                                    (m) =>
                                      !(r.candidates ?? []).some((c) => c.tutorId === m.tutorId),
                                  )
                                  .map((m) => (
                                    <option key={m.tutorId} value={m.tutorId}>
                                      {m.tutorName ?? `Gia sư #${m.tutorId}`}
                                    </option>
                                  ))}
                              </select>
                            </div>
                          </section>

                          {/* Nguồn 2: tuyển gia sư ngoài đội qua tin tuyển dụng */}
                          <section className="cc-req-sec">
                            <div className="cc-req-sec__head">
                              <span className="cc-req-sec__label">📣 Tuyển ngoài đội</span>
                              {r.recruitmentPostId != null && (
                                <span className="cc-req-sec__hint cc-req-sec__hint--ok">
                                  Đã đăng tin · {reqApps[r.requestId]?.length ?? 0} đơn
                                </span>
                              )}
                            </div>
                            <div className="cc-req-sec__body">
                              {r.recruitmentPostId == null ? (
                                <button
                                  type="button"
                                  className="cc-btn cc-btn--soft cc-btn--sm cc-req-postbtn"
                                  disabled={postingReqId === r.requestId}
                                  onClick={() => postRecruitment(r)}
                                >
                                  {postingReqId === r.requestId
                                    ? 'Đang đăng…'
                                    : '＋ Đăng tin tuyển gia sư'}
                                </button>
                              ) : (reqApps[r.requestId]?.length ?? 0) === 0 ? (
                                <span className="cc-shortlist__empty">
                                  Chưa có gia sư ngoài đội ứng tuyển.
                                </span>
                              ) : (
                                <div className="cc-reqrecruit__apps">
                                  {reqApps[r.requestId]!.map((a) => {
                                    const open = certsOpenId === a.recruitmentAppId;
                                    const certCount = a.certificates?.length ?? 0;
                                    return (
                                      <div key={a.recruitmentAppId} className="cc-reqapp">
                                        <div className="cc-reqapp__top">
                                          <div className="cc-reqapp__id">
                                            <span className="cc-reqapp__name">
                                              {a.tutorName ?? `Gia sư #${a.tutorId}`}
                                            </span>
                                            {a.verificationStatus === 'VERIFIED' && (
                                              <span className="cc-reqapp__verified">
                                                ✓ Đã xác minh
                                              </span>
                                            )}
                                          </div>
                                          <button
                                            type="button"
                                            className="cc-btn cc-btn--ghost cc-btn--sm"
                                            aria-expanded={open}
                                            onClick={() =>
                                              setCertsOpenId(open ? null : a.recruitmentAppId)
                                            }
                                          >
                                            {open
                                              ? 'Ẩn hồ sơ ▲'
                                              : `Xem hồ sơ${certCount ? ` · ${certCount} chứng chỉ` : ''} ▼`}
                                          </button>
                                        </div>
                                        <div className="cc-reqapp__meta">
                                          {a.experienceYears != null && (
                                            <span>🎓 {a.experienceYears} năm KN</span>
                                          )}
                                          {a.ratingAvg != null && <span>★ {a.ratingAvg}</span>}
                                          {a.tutorPhone && <span>📞 {a.tutorPhone}</span>}
                                        </div>

                                        {open && (
                                          <div className="cc-reqapp__detail">
                                            {a.coverLetter && (
                                              <p className="cc-reqapp__letter">“{a.coverLetter}”</p>
                                            )}
                                            {certCount > 0 ? (
                                              <div className="cc-reqapp__certs">
                                                <span className="cc-reqapp__certs-label">
                                                  📜 Bằng cấp / chứng chỉ đã xác minh
                                                </span>
                                                <ul className="cc-reqapp__certs-list">
                                                  {a.certificates!.map((cert) => (
                                                    <li key={cert.fileUrl}>
                                                      <button
                                                        type="button"
                                                        className="cc-reqapp__cert"
                                                        onClick={async () => {
                                                          // Chứng chỉ là file private -> tải kèm JWT rồi tạo blob URL.
                                                          if (cert.fileId == null) {
                                                            setCertPreview({
                                                              src: cert.fileUrl,
                                                              fileName: cert.fileName,
                                                              mimeType: cert.mimeType,
                                                            });
                                                            return;
                                                          }
                                                          try {
                                                            const blob =
                                                              await centerApi.getCertificateBlob(
                                                                cert.fileId,
                                                              );
                                                            const url = URL.createObjectURL(blob);
                                                            setCertPreview((prev) => {
                                                              if (prev?.src.startsWith('blob:'))
                                                                URL.revokeObjectURL(prev.src);
                                                              return {
                                                                src: url,
                                                                fileName: cert.fileName,
                                                                mimeType: cert.mimeType ?? blob.type,
                                                              };
                                                            });
                                                          } catch {
                                                            setCertPreview({
                                                              src: cert.fileUrl,
                                                              fileName: cert.fileName,
                                                              mimeType: cert.mimeType,
                                                            });
                                                          }
                                                        }}
                                                      >
                                                        {cert.mimeType?.startsWith('image/')
                                                          ? '🖼️'
                                                          : '📄'}{' '}
                                                        {cert.fileName}
                                                      </button>
                                                    </li>
                                                  ))}
                                                </ul>
                                              </div>
                                            ) : (
                                              <span className="cc-shortlist__empty">
                                                Gia sư chưa có chứng chỉ đã xác minh.
                                              </span>
                                            )}
                                          </div>
                                        )}

                                        <div className="cc-reqapp__foot">
                                          {a.status === 'APPLIED' ? (
                                            <>
                                              <button
                                                type="button"
                                                className="cc-btn cc-btn--ghost cc-btn--sm"
                                                disabled={decidingAppId === a.recruitmentAppId}
                                                onClick={() =>
                                                  decideRequestApplication(
                                                    r.requestId,
                                                    a.recruitmentAppId,
                                                    false,
                                                  )
                                                }
                                              >
                                                Bỏ qua
                                              </button>
                                              <button
                                                type="button"
                                                className="cc-btn cc-btn--primary cc-btn--sm"
                                                disabled={decidingAppId === a.recruitmentAppId}
                                                onClick={() =>
                                                  decideRequestApplication(
                                                    r.requestId,
                                                    a.recruitmentAppId,
                                                    true,
                                                  )
                                                }
                                              >
                                                Duyệt vào danh sách
                                              </button>
                                            </>
                                          ) : (
                                            <span className="cc-chip">
                                              {a.status === 'PASSED'
                                                ? 'Đã duyệt vào danh sách'
                                                : a.status === 'REJECTED'
                                                  ? 'Đã bỏ qua'
                                                  : a.status}
                                            </span>
                                          )}
                                        </div>
                                      </div>
                                    );
                                  })}
                                </div>
                              )}
                            </div>
                          </section>
                        </div>
                      )}

                      {/* Chân thẻ: hành động */}
                      <div className="cc-request__foot">
                        {giveUpId === r.requestId ? (
                          <div className="cc-giveup">
                            <span className="cc-giveup__text">
                              Đóng yêu cầu và báo cho phụ huynh?
                            </span>
                            <div className="cc-giveup__btns">
                              <button
                                className="cc-btn cc-btn--ghost cc-btn--sm"
                                type="button"
                                disabled={giveUpBusy}
                                onClick={() => setGiveUpId(null)}
                              >
                                Huỷ
                              </button>
                              <button
                                className="cc-btn cc-btn--danger cc-btn--sm"
                                type="button"
                                disabled={giveUpBusy}
                                onClick={() => giveUpRequest(r)}
                              >
                                {giveUpBusy ? 'Đang đóng…' : 'Xác nhận đóng'}
                              </button>
                            </div>
                          </div>
                        ) : rejectId === r.requestId ? (
                          <div className="cc-reject">
                            <span className="cc-giveup__text">Từ chối yêu cầu này?</span>
                            <textarea
                              className="cc-input cc-reject__area"
                              rows={2}
                              value={rejectReason}
                              onChange={(e) => setRejectReason(e.target.value)}
                              placeholder="Lý do từ chối (tuỳ chọn) — phụ huynh sẽ thấy…"
                            />
                            <div className="cc-giveup__btns">
                              <button
                                className="cc-btn cc-btn--ghost cc-btn--sm"
                                type="button"
                                disabled={rejectBusy}
                                onClick={() => {
                                  setRejectId(null);
                                  setRejectReason('');
                                }}
                              >
                                Huỷ
                              </button>
                              <button
                                className="cc-btn cc-btn--danger cc-btn--sm"
                                type="button"
                                disabled={rejectBusy}
                                onClick={() => confirmReject(r)}
                              >
                                {rejectBusy ? 'Đang từ chối…' : 'Xác nhận từ chối'}
                              </button>
                            </div>
                          </div>
                        ) : (
                          <>
                            <ChatButton
                              contextType="CLASS_REQUEST"
                              contextId={r.requestId}
                              recipientName={r.clientName ?? undefined}
                              size="sm"
                            />
                            {r.status === 'PENDING' && (
                              <button
                                className="cc-btn cc-btn--primary cc-btn--sm"
                                type="button"
                                onClick={() => startSearchRequest(r)}
                              >
                                Nhận tìm
                              </button>
                            )}
                            {r.status === 'SEARCHING' && (
                              <button
                                className="cc-btn cc-btn--ghost cc-btn--sm"
                                type="button"
                                onClick={() => setGiveUpId(r.requestId)}
                              >
                                Không tìm được gia sư
                              </button>
                            )}
                            <button
                              className="cc-btn cc-btn--ghost cc-btn--sm cc-btn--danger-text"
                              type="button"
                              onClick={() => {
                                setRejectId(r.requestId);
                                setRejectReason('');
                              }}
                            >
                              Từ chối
                            </button>
                          </>
                        )}
                      </div>
                    </div>
                  ))}
              </div>
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
              <span className="cc-label">Loại lớp *</span>
              <select
                className="cc-input"
                value={form.originType}
                // Tạo thủ công -> khóa "Trung tâm tự tạo"; tạo từ yêu cầu phụ huynh -> khóa
                // "Theo yêu cầu ngoài". Chỉ khi sửa lớp đã có mới cho đổi loại.
                disabled={editingId == null}
                // Bị khóa -> nền xám nhạt để người dùng dễ nhận biết là cố định.
                style={
                  editingId == null
                    ? { background: '#f1f5f9', color: '#334155', cursor: 'not-allowed' }
                    : undefined
                }
                onChange={(e) => patch({ originType: e.target.value as FormState['originType'] })}
              >
                <option value="SELF">Trung tâm tự tạo (tuyển học sinh)</option>
                <option value="EXTERNAL">Theo yêu cầu ngoài (đã có học sinh)</option>
              </select>
            </label>

            <label className="cc-field">
              <span className="cc-label">Mẫu hợp đồng (học viên)</span>
              <select
                className="cc-input"
                value={form.contractTemplateId}
                onChange={(e) => {
                  const id = e.target.value;
                  const tpl = templates.find((t) => String(t.templateId) === id);
                  // Chọn mẫu -> nạp sẵn nội dung điều khoản để trung tâm sửa tiếp.
                  patch(tpl ? { contractTemplateId: id, contractContent: tpl.content } : { contractTemplateId: id });
                }}
              >
                <option value="">— Dùng mẫu mặc định / tự nhập —</option>
                {templates
                  .filter((t) => t.contractType !== 'RECRUITMENT')
                  .map((t) => (
                    <option key={t.templateId} value={String(t.templateId)}>
                      {t.name}
                      {t.system ? ' (hệ thống)' : ''}
                    </option>
                  ))}
              </select>
            </label>

            <label className="cc-field cc-field--full">
              <span
                className="cc-label"
                style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}
              >
                Nội dung điều khoản & nghĩa vụ (HĐ học viên)
                <button
                  type="button"
                  className="cc-btn cc-btn--ghost cc-btn--sm"
                  onClick={() => setContractExpanded((v) => !v)}
                  aria-expanded={contractExpanded}
                >
                  {contractExpanded ? '▴ Thu gọn' : '▾ Mở rộng để xem / sửa'}
                </button>
              </span>
              <textarea
                className="cc-input"
                rows={contractExpanded ? 16 : 3}
                style={{
                  minHeight: contractExpanded ? 340 : 72,
                  fontFamily: 'inherit',
                  lineHeight: 1.6,
                  resize: 'vertical',
                }}
                value={form.contractContent}
                onChange={(e) => patch({ contractContent: e.target.value })}
                placeholder="Nhập các điều khoản & nghĩa vụ của hợp đồng…"
              />
            </label>

            {form.originType === 'EXTERNAL' ? (
              <label className="cc-field">
                <span className="cc-label">Số lượng học sinh *</span>
                <input
                  className={errClass('maxStudents')}
                  type="number"
                  min={1}
                  value={form.maxStudents}
                  onChange={(e) => patch({ maxStudents: e.target.value })}
                  placeholder="Số học sinh có sẵn"
                />
                {errText('maxStudents')}
                <span className="cc-hint">
                  Lớp theo yêu cầu đã có sẵn học sinh — không mở ghi danh.
                </span>
              </label>
            ) : (
              <>
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

                <label className="cc-field">
                  <span className="cc-label">Số học sinh tối thiểu để đóng ghi danh</span>
                  <input
                    className={errClass('minStudents')}
                    type="number"
                    min={1}
                    value={form.minStudents}
                    onChange={(e) => patch({ minStudents: e.target.value })}
                    placeholder="Để trống = cần ≥ 1"
                  />
                  {errText('minStudents')}
                  <span className="cc-hint">Đủ số học sinh này mới đóng được ghi danh.</span>
                </label>
              </>
            )}
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
                  <span className="cc-modal__badges">
                    <span className={`cc-badge cc-badge--${detailData.status.toLowerCase()}`}>
                      {STATUS_LABELS[detailData.status]}
                    </span>
                    {enrollmentCountdown(detailData)}
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
                      <dt>Loại lớp</dt>
                      <dd>
                        {detailData.originType === 'EXTERNAL'
                          ? 'Theo yêu cầu ngoài'
                          : 'Trung tâm tự tạo'}
                      </dd>
                    </div>
                    {detailData.originType === 'EXTERNAL' ? (
                      <div className="cc-detail__item">
                        <dt>Số lượng học sinh</dt>
                        <dd>{detailData.maxStudents ?? '—'}</dd>
                      </div>
                    ) : (
                      <>
                        <div className="cc-detail__item">
                          <dt>Sĩ số tối đa</dt>
                          <dd>{detailData.maxStudents ?? '—'}</dd>
                        </div>
                        <div className="cc-detail__item">
                          <dt>Học sinh đã ghi danh</dt>
                          <dd>
                            {detailData.enrolledCount}
                            {detailData.minStudents != null
                              ? ` / tối thiểu ${detailData.minStudents}`
                              : ''}
                          </dd>
                        </div>
                      </>
                    )}
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

                  {detailData.status === 'DRAFT' ||
                  detailData.status === 'OPEN' ||
                  detailData.status === 'ENROLLMENT_CLOSED' ||
                  detailData.status === 'MATCHED' ? (
                    <>
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

                  <div className="cc-detail__section">
                    <span className="cc-detail__label">
                      Gia sư phụ (dạy thay khi gia sư chính bận/ốm)
                    </span>
                    {detailData.assistantTutorName ? (
                      <div className="cc-class-tutor__row">
                        <span className="cc-tutor-badge">
                          {initials(detailData.assistantTutorName)}
                        </span>
                        <span className="cc-tutor-name" title={detailData.assistantTutorName}>
                          {detailData.assistantTutorName}
                        </span>
                        <div className="cc-row-actions">
                          <button
                            className="cc-btn cc-btn--sm"
                            type="button"
                            onClick={() => openAssign(detailData, 'assistant')}
                          >
                            Đổi
                          </button>
                          <button
                            className="cc-btn cc-btn--danger cc-btn--sm"
                            type="button"
                            onClick={() => removeAssistant(detailData.classId)}
                          >
                            Gỡ
                          </button>
                        </div>
                      </div>
                    ) : (
                      <button
                        className="cc-btn cc-btn--soft cc-btn--sm"
                        type="button"
                        onClick={() => openAssign(detailData, 'assistant')}
                      >
                        + Gán gia sư phụ
                      </button>
                    )}
                  </div>
                    </>
                  ) : (
                    <div className="cc-detail__section">
                      <span className="cc-detail__label">Gia sư</span>
                      <p className="cc-muted">Lớp đã kết thúc — không thể thay đổi gia sư.</p>
                    </div>
                  )}

                  {detailData.students && detailData.students.length > 0 && (
                    <div className="cc-detail__section">
                      <button
                        type="button"
                        className="cc-detail__toggle"
                        onClick={() => setShowStudents((v) => !v)}
                        aria-expanded={showStudents}
                      >
                        <span>Học sinh đã đăng ký ({detailData.students.length})</span>
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
                    {canEditClass(detailData) ? (
                      <button
                        className="cc-btn cc-btn--ghost"
                        type="button"
                        onClick={() => openEdit(detailData.classId)}
                      >
                        Sửa lớp học
                      </button>
                    ) : detailData.status === 'DRAFT' || detailData.status === 'OPEN' ? (
                      <p className="cc-muted">
                        🔒{' '}
                        {detailData.editLockReason ??
                          'Đã có học viên đăng ký, không thể chỉnh sửa lớp nữa.'}
                      </p>
                    ) : null}
                    {/* Chỉ lớp "theo yêu cầu" mới đăng tin tuyển; và ẩn khi đã có gia sư. */}
                    {detailData.originType === 'EXTERNAL' && !detailData.assignedTutorId && (
                      <button
                        className="cc-btn cc-btn--ghost"
                        type="button"
                        onClick={() =>
                          navigate('/center/recruitment', {
                            state: {
                              createForClass: {
                                id: detailData.classId,
                                title: detailData.title,
                              },
                            },
                          })
                        }
                      >
                        Tạo tin tuyển dụng cho lớp này
                      </button>
                    )}
                    {detailData.status === 'DRAFT' &&
                      (() => {
                        // Lớp tự tạo: phải gán gia sư trước rồi mới mở ghi danh (đăng tải).
                        const needTutorFirst =
                          detailData.originType !== 'EXTERNAL' && !detailData.assignedTutorId;
                        return (
                          <div className="cc-publish">
                            <p className="cc-publish__hint">
                              {detailData.originType === 'EXTERNAL'
                                ? 'Lớp đã có sẵn học sinh. Đăng tải để bố trí gia sư (gán sẵn hoặc đăng tin tìm gia sư).'
                                : needTutorFirst
                                  ? '⚠ Cần gán gia sư cho lớp trước, rồi mới đăng tải để mở tuyển học sinh.'
                                  : 'Đăng tải để mở tuyển học sinh.'}
                            </p>
                            <button
                              className="cc-btn cc-btn--primary"
                              type="button"
                              disabled={needTutorFirst}
                              title={
                                needTutorFirst
                                  ? 'Cần gán gia sư cho lớp trước khi mở ghi danh'
                                  : undefined
                              }
                              onClick={() => publish(detailData.classId)}
                            >
                              {detailData.originType === 'EXTERNAL'
                                ? 'Đăng tải (bố trí gia sư)'
                                : 'Đăng tải (mở tuyển sinh)'}
                            </button>
                          </div>
                        );
                      })()}
                    {detailData.status === 'OPEN' &&
                      (() => {
                        const required = detailData.minStudents ?? 1;
                        const canClose = detailData.enrolledCount >= required;
                        return (
                          <div className="cc-publish">
                            <p className="cc-publish__hint">
                              Học sinh: <b>{detailData.enrolledCount}</b>
                              {` / tối thiểu ${required}`}
                              {detailData.maxStudents != null
                                ? ` · tối đa ${detailData.maxStudents} (đủ tối đa sẽ tự đóng)`
                                : ''}
                              {!canClose && ' — chưa đủ để đóng ghi danh.'}
                            </p>
                            <button
                              className="cc-btn cc-btn--primary"
                              type="button"
                              disabled={!canClose}
                              title={
                                canClose
                                  ? undefined
                                  : 'Cần đủ số học sinh tối thiểu mới đóng được ghi danh'
                              }
                              onClick={() => closeEnrollment(detailData.classId)}
                            >
                              Đóng ghi danh
                            </button>
                          </div>
                        );
                      })()}
                    {(detailData.status === 'MATCHED' ||
                      detailData.status === 'ENROLLMENT_CLOSED') &&
                      (() => {
                        const required = detailData.minStudents ?? 1;
                        const canActivate = detailData.enrolledCount >= required;
                        return (
                          <div className="cc-publish">
                            <p className="cc-publish__hint">
                              Học sinh: <b>{detailData.enrolledCount}</b>
                              {` / tối thiểu ${required}`}
                              {!canActivate && ' — chưa đủ để kích hoạt.'}
                            </p>
                            <button
                              className="cc-btn cc-btn--primary"
                              type="button"
                              disabled={!canActivate}
                              title={
                                canActivate ? undefined : 'Cần đủ sĩ số tối thiểu để kích hoạt lớp'
                              }
                              onClick={() => activateClass(detailData.classId)}
                            >
                              Kích hoạt lớp (bắt đầu học)
                            </button>
                          </div>
                        );
                      })()}
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
                <h2 className="cc-modal__title">
                  {assignMode === 'assistant' ? 'Gán gia sư phụ' : 'Gán gia sư'}
                </h2>
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
                    const isAssistant = assignMode === 'assistant';
                    const currentId = isAssistant
                      ? assignFor.assistantTutorId
                      : assignFor.assignedTutorId;
                    const selected = currentId === t.tutorId;
                    // Gia sư phụ phải khác gia sư chính đang dạy.
                    const isMainInAssistant =
                      isAssistant && assignFor.assignedTutorId === t.tutorId;
                    const conflict = !isAssistant && !!t.scheduleConflict && !selected;
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
                          disabled={
                            assignBusyId === t.tutorId || selected || conflict || isMainInAssistant
                          }
                          onClick={() => pickTutor(t.tutorId)}
                        >
                          {isMainInAssistant
                            ? 'Gia sư chính'
                            : selected
                              ? isAssistant
                                ? 'Gia sư phụ'
                                : 'Đang dạy'
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

      <FilePreviewModal
        src={certPreview?.src ?? ''}
        fileName={certPreview?.fileName ?? ''}
        mimeType={certPreview?.mimeType}
        isOpen={certPreview !== null}
        onClose={() =>
          setCertPreview((prev) => {
            if (prev?.src.startsWith('blob:')) URL.revokeObjectURL(prev.src);
            return null;
          })
        }
      />
      </div>
      </div>
      </div>
    </>
  );
}
