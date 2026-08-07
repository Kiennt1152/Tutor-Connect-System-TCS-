import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { centerApi } from '../api/centerApi';
import { LocationPicker } from '../components/LocationPicker';
import { FilePreviewModal } from '../../../shared/components/FilePreviewModal';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { ChatButton } from '../../messaging/components/ChatButton';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { profileApi } from '../../profile/api/profileApi';
import type {
  ContractTemplate,
  RecruitmentApplication,
  RecruitmentApplicationStatus,
  RecruitmentPost,
  RecruitmentPostStatus,
  SaveRecruitmentPostRequest,
} from '../types/centerTypes';
import './CenterPage.css';

const STATUS_LABELS: Record<RecruitmentPostStatus, { label: string; cls: string }> = {
  DRAFT: { label: 'Nháp', cls: 'draft' },
  ACTIVE: { label: 'Đang tuyển', cls: 'active' },
  CLOSED: { label: 'Đã đóng', cls: 'closed' },
};

const APP_STATUS_LABELS: Record<RecruitmentApplicationStatus, { label: string; cls: string }> = {
  APPLIED: { label: 'Chờ duyệt', cls: 'pending' },
  SCREENING: { label: 'Đang lọc hồ sơ', cls: 'pending' },
  INTERVIEW: { label: 'Phỏng vấn', cls: 'pending' },
  PASSED: { label: 'Chờ ký hợp đồng', cls: 'pending' },
  HIRED: { label: 'Đã được nhận', cls: 'ok' },
  REJECTED: { label: 'Từ chối', cls: 'no' },
  WITHDRAWN: { label: 'Đã rút', cls: 'no' },
};

interface FormState {
  title: string;
  description: string;
  requirements: string;
  benefits: string;
  requiredExperience: string;
  maxPositions: string;
  subjectName: string;
  provinceName: string;
  wardName: string;
  addressDetail: string;
}

const EMPTY_FORM: FormState = {
  title: '',
  description: '',
  requirements: '',
  benefits: '',
  requiredExperience: '',
  maxPositions: '1',
  subjectName: '',
  provinceName: '',
  wardName: '',
  addressDetail: '',
};

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

/** Mã lỗi backend trả về (VD: "VERIFICATION_REQUIRED") để frontend điều hướng. */
function errorCode(error: unknown): string | undefined {
  if (axios.isAxiosError(error) && typeof error.response?.data?.code === 'string') {
    return error.response.data.code;
  }
  return undefined;
}

function fmtDate(value: string | null): string {
  if (!value) return '—';
  const d = new Date(value);
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
}

function initials(name: string | null): string {
  if (!name) return '?';
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p.charAt(0).toUpperCase())
    .join('');
}

export default function CenterRecruitmentPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [posts, setPosts] = useState<RecruitmentPost[]>([]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [listError, setListError] = useState('');

  // Lớp mà tin đang tạo/sửa gắn tới (nếu có). Null = tin tuyển chung.
  const [linkedClass, setLinkedClass] = useState<{ id: number; title: string } | null>(null);
  // Địa điểm của lớp gắn kèm (hiển thị cố định, không cho nhập tay).
  const [linkedClassLocation, setLinkedClassLocation] = useState<string | null>(null);

  const load = useCallback(() => {
    setStatus('loading');
    setListError('');
    centerApi
      .getMyPosts()
      .then((res) => {
        setPosts(res.data);
        setStatus('success');
      })
      .catch((err) => {
        setListError(extractError(err, 'Không tải được danh sách tin tuyển dụng.'));
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  // Trạng thái xác minh trung tâm: null = đang tải. Chưa xác minh thì không cho đăng tin.
  const [verified, setVerified] = useState<boolean | null>(null);
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
      state: { notice: 'Trung tâm của bạn cần được xác minh trước khi đăng tin tuyển gia sư.' },
    });

  // ----- Tạo / sửa tin -----
  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const patch = (partial: Partial<FormState>) => setForm((prev) => ({ ...prev, ...partial }));

  const openCreate = (forClass?: { id: number; title: string } | null) => {
    // Chưa xác minh -> không cho tạo tin, chuyển sang trang Xác minh.
    if (verified === false) {
      goVerify();
      return;
    }
    setEditingId(null);
    setForm(EMPTY_FORM);
    setLinkedClass(forClass ?? null);
    setFormError('');
    setFormOpen(true);
  };

  // Đến từ trang lớp học ("Tạo tin tuyển dụng cho lớp này") -> mở form gắn sẵn lớp.
  useEffect(() => {
    const st = location.state as { createForClass?: { id: number; title: string } } | null;
    if (st?.createForClass) {
      openCreate(st.createForClass);
      // Xoá state để F5 / back không mở lại form.
      navigate(location.pathname, { replace: true, state: null });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.state]);

  // Lấy địa điểm của lớp gắn kèm để hiển thị cố định (địa điểm làm việc = địa điểm lớp).
  useEffect(() => {
    if (!linkedClass) {
      setLinkedClassLocation(null);
      return;
    }
    let alive = true;
    centerApi
      .getClass(linkedClass.id)
      .then((res) => {
        if (alive) {
          setLinkedClassLocation(res.data.locationLabel ?? res.data.addressDetail ?? null);
        }
      })
      .catch(() => {
        if (alive) setLinkedClassLocation(null);
      });
    return () => {
      alive = false;
    };
  }, [linkedClass]);

  const openEdit = (post: RecruitmentPost) => {
    setEditingId(post.recruitmentId);
    setLinkedClass(
      post.classId != null ? { id: post.classId, title: post.classTitle ?? `Lớp #${post.classId}` } : null,
    );
    setForm({
      title: post.title,
      description: post.description,
      requirements: post.requirements ?? '',
      benefits: post.benefits ?? '',
      requiredExperience: post.requiredExperience != null ? String(post.requiredExperience) : '',
      maxPositions: post.maxPositions != null ? String(post.maxPositions) : '1',
      subjectName: post.subjectName ?? '',
      provinceName: post.provinceName ?? '',
      wardName: post.wardName ?? '',
      addressDetail: post.addressDetail ?? '',
    });
    setFormError('');
    setFormOpen(true);
  };

  const closeForm = () => setFormOpen(false);

  const submitForm = async () => {
    if (!form.title.trim()) {
      setFormError('Vui lòng nhập tiêu đề tin.');
      return;
    }
    if (!form.description.trim()) {
      setFormError('Vui lòng nhập mô tả công việc.');
      return;
    }
    if (form.addressDetail.trim() && !form.provinceName.trim()) {
      setFormError('Đã nhập địa chỉ thì cần chọn Tỉnh/Thành phố.');
      return;
    }
    if (form.provinceName.trim() && !form.addressDetail.trim()) {
      setFormError('Đã chọn Tỉnh/Thành phố thì cần nhập địa chỉ cụ thể.');
      return;
    }
    const payload: SaveRecruitmentPostRequest = {
      classId: linkedClass?.id ?? null,
      title: form.title.trim(),
      description: form.description.trim(),
      requirements: form.requirements.trim() || undefined,
      benefits: form.benefits.trim() || undefined,
      requiredExperience: form.requiredExperience ? Number(form.requiredExperience) : null,
      maxPositions: form.maxPositions ? Number(form.maxPositions) : null,
      subjectName: form.subjectName.trim() || undefined,
      provinceName: form.provinceName.trim() || undefined,
      wardName: form.wardName.trim() || undefined,
      addressDetail: form.addressDetail.trim() || undefined,
    };
    setSaving(true);
    setFormError('');
    try {
      if (editingId != null) {
        await centerApi.updatePost(editingId, payload);
      } else {
        await centerApi.createPost(payload);
      }
      setFormOpen(false);
      load();
    } catch (err) {
      // Chưa xác minh -> điều hướng sang trang Xác minh.
      if (errorCode(err) === 'VERIFICATION_REQUIRED') {
        setVerified(false);
        setFormOpen(false);
        goVerify();
        return;
      }
      setFormError(extractError(err, 'Không lưu được tin tuyển dụng.'));
    } finally {
      setSaving(false);
    }
  };

  // ----- Đăng / đóng tin -----
  const [busyId, setBusyId] = useState<number | null>(null);

  const publish = async (post: RecruitmentPost) => {
    setBusyId(post.recruitmentId);
    setListError('');
    try {
      await centerApi.publishPost(post.recruitmentId);
      load();
    } catch (err) {
      if (errorCode(err) === 'VERIFICATION_REQUIRED') {
        setVerified(false);
        goVerify();
        return;
      }
      setListError(extractError(err, 'Không đăng được tin.'));
    } finally {
      setBusyId(null);
    }
  };

  const closePost = async (post: RecruitmentPost) => {
    setBusyId(post.recruitmentId);
    setListError('');
    try {
      await centerApi.closePost(post.recruitmentId);
      load();
    } catch (err) {
      setListError(extractError(err, 'Không đóng được tin.'));
    } finally {
      setBusyId(null);
    }
  };

  // ----- Đơn ứng tuyển -----
  const [appsFor, setAppsFor] = useState<RecruitmentPost | null>(null);
  const [apps, setApps] = useState<RecruitmentApplication[]>([]);
  const [appsLoading, setAppsLoading] = useState(false);
  const [appsError, setAppsError] = useState('');
  const [decidingId, setDecidingId] = useState<number | null>(null);
  // Ứng viên đang mở xem chứng chỉ (null = không mở cái nào).
  const [certsOpenId, setCertsOpenId] = useState<number | null>(null);
  const toggleCerts = (appId: number) =>
    setCertsOpenId((prev) => (prev === appId ? null : appId));
  // Xem trước file chứng chỉ ngay trong trang (không nhảy sang tab khác).
  const [preview, setPreview] = useState<{ src: string; fileName: string } | null>(null);

  // BF-03: duyệt -> chọn mẫu hợp đồng (loại tuyển dụng) để gửi gia sư ký.
  const [recruitTemplates, setRecruitTemplates] = useState<ContractTemplate[]>([]);
  const [approving, setApproving] = useState<
    { app: RecruitmentApplication; templateId: number | ''; content: string } | null
  >(null);

  useEffect(() => {
    centerApi
      .getContractTemplates()
      .then((res) =>
        setRecruitTemplates(res.data.filter((t) => t.contractType === 'RECRUITMENT')),
      )
      .catch(() => setRecruitTemplates([]));
  }, []);

  const openApps = async (post: RecruitmentPost) => {
    setAppsFor(post);
    setApps([]);
    setAppsError('');
    setCertsOpenId(null);
    setAppsLoading(true);
    try {
      const res = await centerApi.getApplications(post.recruitmentId);
      setApps(res.data);
    } catch (err) {
      setAppsError(extractError(err, 'Không tải được danh sách ứng viên.'));
    } finally {
      setAppsLoading(false);
    }
  };

  const closeApps = () => setAppsFor(null);

  const decide = async (
    app: RecruitmentApplication,
    approve: boolean,
    contractTemplateId?: number,
    contractContent?: string,
  ) => {
    setDecidingId(app.recruitmentAppId);
    setAppsError('');
    try {
      await centerApi.decideApplication(
        app.recruitmentAppId,
        approve,
        contractTemplateId,
        contractContent,
      );
      // BF-03 bước 7: duyệt -> hệ thống tạo thỏa thuận hợp tác, đơn chuyển "Chờ ký hợp đồng".
      // Gia sư mới là bên ký (OTP) nên KHÔNG chuyển trung tâm sang trang Hợp đồng — ở lại đây,
      // chỉ làm mới danh sách để thấy trạng thái đơn cập nhật.
      setApproving(null);
      if (appsFor) {
        const res = await centerApi.getApplications(appsFor.recruitmentId);
        setApps(res.data);
      }
      load(); // cập nhật lại số đơn trên thẻ tin
    } catch (err) {
      setAppsError(extractError(err, 'Không xử lý được đơn ứng tuyển.'));
    } finally {
      setDecidingId(null);
    }
  };

  return (
    <>
      <HomeNavbar />
      <div className="rc-bg">
      <div className="rc-page">
        <div className="rc-topbar">
          <Link className="rc-back" to="/">
            ← Trang chủ
          </Link>
          <Link className="rc-btn rc-btn--ghost rc-btn--sm" to="/center/tutors">
            Gia sư của trung tâm
          </Link>
        </div>

        <header className="rc-header">
          <div>
            <h1 className="rc-title">Tin tuyển gia sư</h1>
            <p className="rc-subtitle">
              Tạo tin ở dạng nháp, đăng tin để gia sư tự do nhìn thấy và ứng tuyển, rồi duyệt hoặc
              từ chối từng ứng viên.
            </p>
          </div>
          <button className="rc-btn rc-btn--primary" type="button" onClick={() => openCreate()}>
            + Tạo tin tuyển dụng
          </button>
        </header>

        {verified === false && (
          <div className="rc-alert rc-alert--warn rc-verify-banner">
            <span>
              ⚠ Trung tâm của bạn <b>chưa được xác minh</b>. Cần hoàn tất xác minh trước khi đăng
              tin tuyển gia sư.
            </span>
            <button className="rc-btn rc-btn--primary rc-btn--sm" type="button" onClick={goVerify}>
              Đi xác minh →
            </button>
          </div>
        )}
        {listError && <div className="rc-alert rc-alert--error">{listError}</div>}
        {status === 'loading' && <div className="rc-state">Đang tải…</div>}
        {status === 'success' && posts.length === 0 && (
          <div className="rc-empty">
            <div className="rc-empty__emoji">📢</div>
            <p>Chưa có tin tuyển dụng nào. Bấm “Tạo tin tuyển dụng” để bắt đầu.</p>
          </div>
        )}

        {status === 'success' && posts.length > 0 && (
          <div className="rc-list">
            {posts.map((p) => {
              const st = STATUS_LABELS[p.status];
              const busy = busyId === p.recruitmentId;
              return (
                <article className="rc-card" key={p.recruitmentId}>
                  <div className="rc-card__head">
                    <div>
                      <h2 className="rc-card__title">{p.title}</h2>
                      <div className="rc-chips">
                        <span className={`rc-status rc-status--${st.cls}`}>{st.label}</span>
                        {p.classId != null && (
                          <span className="rc-chip rc-chip--class">
                            🎓 Lớp: {p.classTitle ?? `#${p.classId}`}
                          </span>
                        )}
                        {p.subjectName && <span className="rc-chip">📘 {p.subjectName}</span>}
                        {p.locationLabel && <span className="rc-chip">📍 {p.locationLabel}</span>}
                        <span className="rc-chip">👤 {p.maxPositions} vị trí</span>
                        {!!p.requiredExperience && (
                          <span className="rc-chip">🎓 ≥ {p.requiredExperience} năm KN</span>
                        )}
                      </div>
                    </div>
                    <div className="rc-card__meta">
                      <span>Tạo: {fmtDate(p.createdAt)}</span>
                      {p.publishedAt && <span>Đăng: {fmtDate(p.publishedAt)}</span>}
                      {p.closedAt && <span>Đóng: {fmtDate(p.closedAt)}</span>}
                    </div>
                  </div>

                  <p className="rc-card__desc">{p.description}</p>

                  <div className="rc-card__foot">
                    <span className="rc-count">
                      🧑‍🏫 {p.applicationCount} đơn ứng tuyển
                    </span>
                    <div className="rc-actions">
                      {p.status === 'DRAFT' && (
                        <>
                          <button
                            className="rc-btn rc-btn--ghost"
                            type="button"
                            onClick={() => openEdit(p)}
                          >
                            Sửa
                          </button>
                          <button
                            className="rc-btn rc-btn--primary"
                            type="button"
                            disabled={busy}
                            onClick={() => publish(p)}
                          >
                            {busy ? 'Đang đăng…' : 'Đăng tin'}
                          </button>
                        </>
                      )}
                      {p.status !== 'DRAFT' && (
                        <button
                          className="rc-btn rc-btn--ghost"
                          type="button"
                          onClick={() => openApps(p)}
                        >
                          Xem ứng viên ({p.applicationCount})
                        </button>
                      )}
                      {p.status === 'ACTIVE' && (
                        <button
                          className="rc-btn rc-btn--danger"
                          type="button"
                          disabled={busy}
                          onClick={() => closePost(p)}
                        >
                          {busy ? 'Đang đóng…' : 'Đóng tin'}
                        </button>
                      )}
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </div>

      {formOpen && (
        <div className="rc-modal" role="dialog" aria-modal="true">
          <div className="rc-modal__backdrop" onClick={closeForm} />
          <div className="rc-modal__card">
            <div className="rc-modal__head">
              <h2 className="rc-modal__title">
                {editingId != null ? 'Sửa tin tuyển dụng' : 'Tạo tin tuyển dụng'}
              </h2>
              <button
                className="rc-modal__close"
                type="button"
                onClick={closeForm}
                aria-label="Đóng"
              >
                ×
              </button>
            </div>
            <div className="rc-modal__body">
              {formError && <div className="rc-alert rc-alert--error">{formError}</div>}

              {linkedClass ? (
                <div className="rc-linked-class">
                  <span className="rc-linked-class__badge">🎓 Tuyển cho lớp</span>
                  <span className="rc-linked-class__title">{linkedClass.title}</span>
                  <button
                    type="button"
                    className="rc-linked-class__clear"
                    onClick={() => setLinkedClass(null)}
                    title="Bỏ gắn lớp — chuyển thành tin tuyển chung"
                  >
                    ✕
                  </button>
                </div>
              ) : null}

              <label className="rc-field">
                <span>Tiêu đề *</span>
                <input
                  type="text"
                  value={form.title}
                  onChange={(e) => patch({ title: e.target.value })}
                  placeholder="VD: Tuyển gia sư Toán lớp 9"
                />
              </label>

              <label className="rc-field">
                <span>Mô tả công việc *</span>
                <textarea
                  rows={4}
                  value={form.description}
                  onChange={(e) => patch({ description: e.target.value })}
                  placeholder="Mô tả công việc, thời gian, đối tượng học sinh…"
                />
              </label>

              <label className="rc-field">
                <span>Yêu cầu ứng viên</span>
                <textarea
                  rows={3}
                  value={form.requirements}
                  onChange={(e) => patch({ requirements: e.target.value })}
                  placeholder="VD: Sinh viên năm 3 trở lên, có kinh nghiệm dạy kèm…"
                />
              </label>

              <label className="rc-field">
                <span>Quyền lợi</span>
                <textarea
                  rows={3}
                  value={form.benefits}
                  onChange={(e) => patch({ benefits: e.target.value })}
                  placeholder="VD: Lương 200k/buổi, hỗ trợ giáo trình…"
                />
              </label>

              <div className="rc-field2">
                <label className="rc-field">
                  <span>Môn học</span>
                  <input
                    type="text"
                    value={form.subjectName}
                    onChange={(e) => patch({ subjectName: e.target.value })}
                    placeholder="VD: Toán"
                  />
                </label>
                <label className="rc-field">
                  <span>Số lượng cần tuyển</span>
                  <input
                    type="number"
                    min={1}
                    value={form.maxPositions}
                    onChange={(e) => patch({ maxPositions: e.target.value })}
                  />
                </label>
              </div>

              <label className="rc-field">
                <span>Kinh nghiệm tối thiểu (năm)</span>
                <input
                  type="number"
                  min={0}
                  value={form.requiredExperience}
                  onChange={(e) => patch({ requiredExperience: e.target.value })}
                  placeholder="0"
                />
              </label>

              {linkedClass ? (
                <div className="rc-loc">
                  <span className="rc-loc__label">Địa điểm làm việc</span>
                  <div className="rc-loc__value">
                    {linkedClassLocation ?? 'Lấy theo địa điểm của lớp'}
                  </div>
                  <span className="rc-loc__hint">
                    Tin gắn với lớp — địa điểm làm việc lấy cố định theo địa điểm của lớp.
                  </span>
                </div>
              ) : (
                <div className="rc-loc">
                  <span className="rc-loc__label">Địa điểm làm việc (tuỳ chọn)</span>
                  <LocationPicker
                    value={{
                      province: form.provinceName,
                      ward: form.wardName,
                      addressDetail: form.addressDetail,
                    }}
                    onChange={(v) =>
                      patch({
                        provinceName: v.province,
                        wardName: v.ward,
                        addressDetail: v.addressDetail,
                      })
                    }
                  />
                </div>
              )}
            </div>
            <div className="rc-modal__actions">
              <button className="rc-btn rc-btn--ghost" type="button" onClick={closeForm}>
                Huỷ
              </button>
              <button
                className="rc-btn rc-btn--primary"
                type="button"
                disabled={saving}
                onClick={submitForm}
              >
                {saving ? 'Đang lưu…' : editingId != null ? 'Lưu thay đổi' : 'Tạo tin (nháp)'}
              </button>
            </div>
          </div>
        </div>
      )}

      {appsFor && (
        <div className="rc-modal" role="dialog" aria-modal="true">
          <div className="rc-modal__backdrop" onClick={closeApps} />
          <div className="rc-modal__card">
            <div className="rc-modal__head">
              <div>
                <h2 className="rc-modal__title">Ứng viên</h2>
                <p className="rc-modal__sub">Tin: {appsFor.title}</p>
              </div>
              <button
                className="rc-modal__close"
                type="button"
                onClick={closeApps}
                aria-label="Đóng"
              >
                ×
              </button>
            </div>
            <div className="rc-modal__body">
              {appsError && <div className="rc-alert rc-alert--error">{appsError}</div>}
              {appsLoading && <div className="rc-state">Đang tải ứng viên…</div>}
              {!appsLoading && !appsError && apps.length === 0 && (
                <div className="rc-state">Chưa có ai ứng tuyển tin này.</div>
              )}
              {!appsLoading && apps.length > 0 && (
                <ul className="rc-applicants">
                  {apps.map((a) => {
                    const ast = APP_STATUS_LABELS[a.status];
                    const busy = decidingId === a.recruitmentAppId;
                    return (
                      <li className="rc-applicant" key={a.recruitmentAppId}>
                        <div className="rc-applicant__avatar">{initials(a.tutorName)}</div>
                        <div className="rc-applicant__info">
                          <div className="rc-applicant__name">
                            {a.tutorName}
                            {a.verificationStatus === 'VERIFIED' && (
                              <span className="rc-verified">✓ Đã xác minh</span>
                            )}
                          </div>
                          <div className="rc-applicant__meta">
                            {a.experienceYears != null && <span>{a.experienceYears} năm KN</span>}
                            {a.ratingAvg != null && <span>★ {a.ratingAvg}</span>}
                            {a.tutorPhone && <span>{a.tutorPhone}</span>}
                            <span>Nộp: {fmtDate(a.appliedAt)}</span>
                          </div>
                          {a.coverLetter && <p className="rc-applicant__letter">{a.coverLetter}</p>}
                          {certsOpenId === a.recruitmentAppId &&
                            a.certificates &&
                            a.certificates.length > 0 && (
                              <div className="rc-certs">
                                <span className="rc-certs__label">
                                  📜 Bằng cấp / chứng chỉ đã xác minh
                                </span>
                                <ul className="rc-certs__list">
                                  {a.certificates.map((cert) => (
                                    <li key={cert.fileUrl}>
                                      <button
                                        type="button"
                                        className="rc-certs__link"
                                        onClick={() =>
                                          setPreview({
                                            src: cert.fileUrl,
                                            fileName: cert.fileName,
                                          })
                                        }
                                      >
                                        {cert.mimeType?.startsWith('image/') ? '🖼️' : '📄'}{' '}
                                        {cert.fileName}
                                      </button>
                                    </li>
                                  ))}
                                </ul>
                              </div>
                            )}
                        </div>
                        <div className="rc-applicant__actions">
                          <ChatButton
                            contextType="RECRUITMENT"
                            contextId={a.recruitmentAppId}
                            recipientName={a.tutorName ?? undefined}
                            size="sm"
                          />
                          {a.status === 'APPLIED' ? (
                            <div className="rc-applicant__decide">
                              <button
                                className="rc-btn rc-btn--danger rc-btn--sm"
                                type="button"
                                disabled={busy}
                                onClick={() => decide(a, false)}
                              >
                                Từ chối
                              </button>
                              <button
                                className="rc-btn rc-btn--primary rc-btn--sm"
                                type="button"
                                disabled={busy}
                                onClick={() => setApproving({ app: a, templateId: '', content: '' })}
                              >
                                Duyệt
                              </button>
                            </div>
                          ) : (
                            <span className={`rc-status rc-status--${ast.cls}`}>{ast.label}</span>
                          )}
                          {a.certificates && a.certificates.length > 0 && (
                            <button
                              className="rc-btn rc-btn--ghost rc-btn--sm rc-certs__toggle"
                              type="button"
                              aria-expanded={certsOpenId === a.recruitmentAppId}
                              onClick={() => toggleCerts(a.recruitmentAppId)}
                            >
                              Chứng chỉ ({a.certificates.length}){' '}
                              {certsOpenId === a.recruitmentAppId ? '▲' : '▼'}
                            </button>
                          )}
                        </div>
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Duyệt -> chọn mẫu hợp đồng tuyển dụng gửi gia sư ký */}
      {approving && (
        <div className="rc-modal" role="dialog" aria-modal="true">
          <div className="rc-modal__backdrop" onClick={() => setApproving(null)} />
          <div className="rc-modal__card" style={{ width: 'min(560px, 100%)' }}>
            <div className="rc-modal__head">
              <div>
                <h2 className="rc-modal__title">Duyệt &amp; gửi hợp đồng</h2>
                <p className="rc-modal__sub">Gia sư: {approving.app.tutorName ?? '—'}</p>
              </div>
              <button
                type="button"
                className="rc-modal__close"
                onClick={() => setApproving(null)}
                aria-label="Đóng"
              >
                ×
              </button>
            </div>

            <div className="rc-modal__body">
              {recruitTemplates.length > 0 && (
                <label className="rc-field">
                  <span>Mẫu hợp đồng (loại tuyển dụng)</span>
                  <select
                    value={approving.templateId}
                    onChange={(e) => {
                      const id = e.target.value ? Number(e.target.value) : '';
                      const tpl = recruitTemplates.find((t) => t.templateId === id);
                      // Chọn mẫu -> nạp sẵn nội dung để center sửa tiếp.
                      setApproving({
                        ...approving,
                        templateId: id,
                        content: tpl ? tpl.content : approving.content,
                      });
                    }}
                  >
                    <option value="">— Nội dung mặc định / tự nhập —</option>
                    {recruitTemplates.map((t) => (
                      <option key={t.templateId} value={t.templateId}>
                        {t.name}
                      </option>
                    ))}
                  </select>
                </label>
              )}

              <label className="rc-field">
                <span>Nội dung điều khoản &amp; nghĩa vụ</span>
                <textarea
                  rows={8}
                  style={{ minHeight: 180, resize: 'vertical' }}
                  value={approving.content}
                  onChange={(e) => setApproving({ ...approving, content: e.target.value })}
                  placeholder={'Điều 1. ...\nĐiều 2. ...'}
                />
              </label>

            </div>

            <div className="rc-modal__actions">
              <button
                type="button"
                className="rc-btn rc-btn--ghost"
                onClick={() => setApproving(null)}
              >
                Hủy
              </button>
              <button
                type="button"
                className="rc-btn rc-btn--primary"
                disabled={decidingId === approving.app.recruitmentAppId}
                onClick={() =>
                  decide(
                    approving.app,
                    true,
                    approving.templateId === '' ? undefined : approving.templateId,
                    approving.content.trim() ? approving.content : undefined,
                  )
                }
              >
                {decidingId === approving.app.recruitmentAppId
                  ? 'Đang gửi…'
                  : 'Duyệt & gửi cho gia sư ký'}
              </button>
            </div>
          </div>
        </div>
      )}

      <FilePreviewModal
        src={preview?.src ?? ''}
        fileName={preview?.fileName ?? ''}
        isOpen={preview !== null}
        onClose={() => setPreview(null)}
      />
      </div>
    </>
  );
}
