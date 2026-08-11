import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { SiteFooter } from '../../home/components/SiteFooter';
import { marketplaceApi } from '../api/marketplaceApi';
import { useMarketplace } from '../hooks/useMarketplace';
import { ClassRequestForm } from '../components/ClassRequestForm';
import { ApplicantsPanel } from '../components/ApplicantsPanel';
import { ClassDetailPanel } from '../components/ClassDetailPanel';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { FilePreviewModal } from '../../../shared/components/FilePreviewModal';
import { WeeklyTimetable } from '../../teaching/components/WeeklyTimetable';
import { useClassLessons } from '../../teaching/hooks/useTeaching';
import { classToForm, emptyForm } from '../mappers/marketplaceMapper';
import {
  CLASS_STATUS_LABELS,
  isOtherSubject,
  type CatalogOption,
  type ClassFormValues,
  type ClassRequest,
  type ClassRequestStatus,
  type ClassRequestPayload,
  type ClassResponse,
} from '../types/marketplaceTypes';
import './MarketplacePage.css';

const currency = new Intl.NumberFormat('vi-VN');

// Nhãn trạng thái yêu cầu "nhờ trung tâm tìm".
const REQ_STATUS_LABEL: Record<ClassRequestStatus, string> = {
  PENDING: 'Đang chờ',
  SEARCHING: 'Đang tìm gia sư',
  ACCEPTED: 'Đã chấp nhận',
  REJECTED: 'Đã từ chối',
};

function isEditableClass(c: ClassResponse): boolean {
  const noApplicants = (c.applicationCount ?? 0) === 0;
  return (c.status === 'DRAFT' || c.status === 'OPEN') && noApplicants;
}

type Mode =
  | { kind: 'list' }
  | { kind: 'create' }
  | { kind: 'edit'; target: ClassResponse }
  | { kind: 'detail'; target: ClassResponse };

export default function MarketplacePage() {
  const { user } = useAuth();
  const isClient = user?.role === 'CLIENT';
  const {
    status,
    classes,
    subjects,
    grades,
    reload,
    createClass,
    updateClass,
    publishClass,
    unpublishClass,
  } = useMarketplace();

  const [mode, setMode] = useState<Mode>({ kind: 'list' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [publishTarget, setPublishTarget] = useState<number | null>(null);
  const [publishError, setPublishError] = useState<string | null>(null);
  const [unpublishTarget, setUnpublishTarget] = useState<number | null>(null);

  // Yêu cầu "nhờ trung tâm tìm" (gửi tới trung tâm) — gộp về đây để theo dõi chung.
  const [centerRequests, setCenterRequests] = useState<ClassRequest[]>([]);
  const [chooseTarget, setChooseTarget] = useState<
    { requestId: string; tutorId: number; tutorName: string } | null
  >(null);
  const [chooseBusy, setChooseBusy] = useState(false);
  const [reqNotice, setReqNotice] = useState('');
  // Gia sư đang mở xem chứng chỉ (khoá theo requestId:tutorId) + xem trước file.
  const [candCertsOpen, setCandCertsOpen] = useState<string | null>(null);
  const [candPreview, setCandPreview] = useState<{ src: string; fileName: string } | null>(null);
  useEffect(() => {
    if (!isClient) return;
    marketplaceApi
      .getMyClassRequests()
      .then((res) => setCenterRequests(res.data))
      .catch(() => setCenterRequests([]));
  }, [isClient]);
  const cancelCenterRequest = async (requestId: string) => {
    try {
      await marketplaceApi.cancelClassRequest(requestId);
      const res = await marketplaceApi.getMyClassRequests();
      setCenterRequests(res.data);
    } catch {
      /* bỏ qua */
    }
  };
  // Phụ huynh chọn 1 gia sư đề cử -> xác nhận (ConfirmDialog) rồi materialize.
  const confirmChooseTutor = async () => {
    if (!chooseTarget) return;
    setChooseBusy(true);
    try {
      await marketplaceApi.chooseTutorForRequest(chooseTarget.requestId, chooseTarget.tutorId);
      reload();
      const res = await marketplaceApi.getMyClassRequests();
      setCenterRequests(res.data);
      setChooseTarget(null);
      setReqNotice(
        'Đã chọn gia sư. Lớp đã được tạo — gia sư sẽ nhận thông báo để nhận lớp và ký hợp đồng.',
      );
      window.setTimeout(() => setReqNotice(''), 6000);
    } catch (err) {
      setChooseTarget(null);
      setPublishError(
        axios.isAxiosError(err) && typeof err.response?.data?.message === 'string'
          ? err.response.data.message
          : 'Không chọn được gia sư. Vui lòng thử lại.',
      );
    } finally {
      setChooseBusy(false);
    }
  };

  function openEdit(target: ClassResponse) {
    setError(null);
    setMode({ kind: 'edit', target });
  }

  function openDetail(target: ClassResponse) {
    setMode({ kind: 'detail', target });
  }

  async function handleSubmit(payload: ClassRequestPayload) {
    setSubmitting(true);
    setError(null);
    try {
      if (mode.kind === 'edit') {
        await updateClass(mode.target.classId, payload);
      } else {
        await createClass(payload);
      }
      setMode({ kind: 'list' });
    } catch (err) {
      setError(extractError(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function confirmPublish() {
    const classId = publishTarget;
    setPublishTarget(null);
    if (classId == null) return;
    try {
      await publishClass(classId);
    } catch (err) {
      setPublishError(extractError(err));
    }
  }

  async function confirmUnpublish() {
    const classId = unpublishTarget;
    setUnpublishTarget(null);
    if (classId == null) return;
    try {
      await unpublishClass(classId);
      setMode((prev) =>
        prev.kind === 'detail' && prev.target.classId === classId ? { kind: 'list' } : prev,
      );
    } catch (err) {
      setPublishError(extractError(err));
    }
  }

  const initialForm: ClassFormValues =
    mode.kind === 'edit' ? classToForm(mode.target) : emptyForm();

  return (
    <div className="tcs-page mkt-page">
      <HomeNavbar />
      {reqNotice && (
        <div className="mkt-toast" role="status">
          <span className="mkt-toast__icon" aria-hidden="true">✓</span>
          <span className="mkt-toast__msg">{reqNotice}</span>
          <button
            type="button"
            className="mkt-toast__x"
            aria-label="Đóng thông báo"
            onClick={() => setReqNotice('')}
          >
            ×
          </button>
        </div>
      )}
      <main>
        <div className="tcs-container mkt-container">
          <header className="mkt-header">
            <div>
              <span className="mkt-eyebrow">Lớp học gia sư</span>
              <h1 className="mkt-title">Yêu cầu tìm gia sư của tôi</h1>
              <p className="mkt-subtitle">
                Đăng nhu cầu tìm gia sư cho con em bạn — chọn môn, lớp, mục tiêu, học phí. Gia sư
                phù hợp sẽ ứng tuyển sau khi bạn đăng lớp.
              </p>
            </div>
            {mode.kind === 'list' && isClient && (
              <Link className="mkt-btn mkt-btn--primary" to={APP_ROUTES.postTutorRequest}>
                + Tạo lớp mới
              </Link>
            )}
          </header>

          {!isClient && (
            <div className="mkt-alert mkt-alert--info">
              Chỉ tài khoản Phụ huynh / Khách hàng (Client) mới có thể tạo yêu cầu tìm gia sư.
            </div>
          )}

          {mode.kind === 'detail' ? (
            <ClassDetailScreen
              target={mode.target}
              subjects={subjects}
              grades={grades}
              onChosen={reload}
              onEdit={openEdit}
              onUnpublish={setUnpublishTarget}
              onBack={() => setMode({ kind: 'list' })}
            />
          ) : mode.kind !== 'list' ? (
            <section className="mkt-card">
              <div className="mkt-card__head">
                <h2>{mode.kind === 'edit' ? 'Chỉnh sửa lớp' : 'Tạo lớp gia sư mới'}</h2>
              </div>
              <div className="mkt-card__body">
                <ClassRequestForm
                  initial={initialForm}
                  subjects={subjects}
                  grades={grades}
                  isEdit={mode.kind === 'edit'}
                  submitting={submitting}
                  error={error}
                  onSubmit={handleSubmit}
                  onCancel={() => setMode({ kind: 'list' })}
                />
              </div>
            </section>
          ) : (
            <>
              {/* Phần 1: yêu cầu tự tìm (lớp tự đăng cho gia sư ứng tuyển) */}
              <div className="mkt-section-head mkt-section-head--first">
                <h2>Yêu cầu tự tìm</h2>
              </div>
              <p className="mkt-section-desc">Lớp bạn tự đăng để gia sư ứng tuyển trực tiếp.</p>
              <ClassList
                status={status}
                classes={classes}
                subjects={subjects}
                onEdit={openEdit}
                onPublish={setPublishTarget}
                onUnpublish={setUnpublishTarget}
                onOpenDetail={openDetail}
              />

              {/* Phần 2: yêu cầu nhờ trung tâm tìm */}
              <div className="mkt-section-head">
                <h2>Yêu cầu nhờ trung tâm tìm</h2>
                {centerRequests.length > 0 && (
                  <span className="mkt-section-head__count">{centerRequests.length}</span>
                )}
              </div>
              <p className="mkt-section-desc">
                Yêu cầu bạn gửi cho trung tâm để nhờ tìm gia sư giúp.
              </p>
              {centerRequests.length === 0 ? (
                <div className="mkt-req-empty">
                  Chưa có yêu cầu nào. Vào trang <Link to={APP_ROUTES.centers}>Trung tâm</Link> để nhờ
                  trung tâm tìm gia sư giúp bạn.
                </div>
              ) : (
                <div className="mkt-req-list">
                  {centerRequests.map((r) => (
                    <div key={r.requestId} className="mkt-req-card">
                      <div className="mkt-req-card__main">
                        <p className="mkt-req-card__note">{r.note}</p>
                        <div className="mkt-req-card__meta">
                          <span>
                            Gửi tới: <b>{r.centerName ?? '—'}</b>
                          </span>
                          {r.desiredBudget != null && (
                            <span>
                              Ngân sách: <b>{currency.format(r.desiredBudget)}đ</b>
                            </span>
                          )}
                          {r.status === 'REJECTED' && r.reason && (
                            <span className="mkt-req-card__reason">Lý do: {r.reason}</span>
                          )}
                        </div>
                        {r.status !== 'ACCEPTED' && r.candidates && r.candidates.length > 0 && (
                          <div className="mkt-req-cands">
                            <span className="mkt-req-cands__label">
                              Gia sư trung tâm đề cử — chọn 1:
                            </span>
                            {r.candidates.map((c) => {
                              const key = `${r.requestId}:${c.tutorId}`;
                              const open = candCertsOpen === key;
                              const certCount = c.certificates?.length ?? 0;
                              return (
                                <div key={c.tutorId} className="mkt-req-cand">
                                  <div className="mkt-req-cand__row">
                                    <div className="mkt-req-cand__id">
                                      <span className="mkt-req-cand__name">{c.fullName}</span>
                                      <span className="mkt-req-cand__meta">
                                        {c.experienceYears != null && `${c.experienceYears} năm KN`}
                                        {c.ratingAvg != null && ` · ⭐ ${c.ratingAvg}`}
                                      </span>
                                    </div>
                                    <div className="mkt-req-cand__btns">
                                      <button
                                        type="button"
                                        className="mkt-btn mkt-btn--ghost mkt-btn--sm"
                                        aria-expanded={open}
                                        onClick={() => setCandCertsOpen(open ? null : key)}
                                      >
                                        {open
                                          ? 'Ẩn hồ sơ ▲'
                                          : `Xem hồ sơ${certCount ? ` · ${certCount} chứng chỉ` : ''} ▼`}
                                      </button>
                                      <button
                                        type="button"
                                        className="mkt-btn mkt-btn--primary mkt-btn--sm"
                                        onClick={() =>
                                          setChooseTarget({
                                            requestId: r.requestId,
                                            tutorId: c.tutorId,
                                            tutorName: c.fullName,
                                          })
                                        }
                                      >
                                        Chọn
                                      </button>
                                    </div>
                                  </div>
                                  {open && (
                                    <div className="mkt-req-cand__certs">
                                      {certCount > 0 ? (
                                        <>
                                          <span className="mkt-req-cand__certs-label">
                                            📜 Bằng cấp / chứng chỉ đã xác minh
                                          </span>
                                          <ul className="mkt-req-cand__certs-list">
                                            {c.certificates!.map((cert) => (
                                              <li key={cert.fileUrl}>
                                                <button
                                                  type="button"
                                                  className="mkt-req-cand__cert"
                                                  onClick={() =>
                                                    setCandPreview({
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
                                        </>
                                      ) : (
                                        <span className="mkt-req-cand__certs-empty">
                                          Gia sư chưa có chứng chỉ đã xác minh.
                                        </span>
                                      )}
                                    </div>
                                  )}
                                </div>
                              );
                            })}
                          </div>
                        )}
                      </div>
                      <span className={`mkt-status mkt-status--${r.status.toLowerCase()}`}>
                        {REQ_STATUS_LABEL[r.status]}
                      </span>
                      {r.status === 'PENDING' && (
                        <div className="mkt-req-card__actions">
                          <button
                            type="button"
                            className="mkt-btn mkt-btn--ghost mkt-btn--sm"
                            onClick={() => cancelCenterRequest(r.requestId)}
                          >
                            Hủy
                          </button>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </>
          )}
        </div>
      </main>
      <SiteFooter />

      {publishTarget != null && (
        <ConfirmDialog
          title="Đăng lớp"
          message="Đăng lớp này để gia sư có thể ứng tuyển?"
          confirmLabel="Đăng lớp"
          cancelLabel="Hủy"
          onConfirm={confirmPublish}
          onClose={() => setPublishTarget(null)}
        />
      )}

      {unpublishTarget != null && (
        <ConfirmDialog
          title="Gỡ đăng lớp"
          message="Gỡ đăng sẽ đưa lớp về trạng thái Nháp và ẩn khỏi gia sư. Bạn có thể sửa rồi đăng lại. Tiếp tục?"
          confirmLabel="Gỡ đăng"
          cancelLabel="Hủy"
          onConfirm={confirmUnpublish}
          onClose={() => setUnpublishTarget(null)}
        />
      )}

      {publishError && (
        <ConfirmDialog
          title="Không thực hiện được"
          message={publishError}
          onClose={() => setPublishError(null)}
        />
      )}

      {chooseTarget && (
        <ConfirmDialog
          title="Chọn gia sư"
          message={`Chọn gia sư "${chooseTarget.tutorName}" cho yêu cầu này? Hệ thống sẽ tạo lớp và gửi lời mời nhận lớp cho gia sư.`}
          confirmLabel={chooseBusy ? 'Đang xử lý…' : 'Chọn gia sư'}
          cancelLabel="Hủy"
          onConfirm={confirmChooseTutor}
          onClose={() => setChooseTarget(null)}
        />
      )}

      <FilePreviewModal
        src={candPreview?.src ?? ''}
        fileName={candPreview?.fileName ?? ''}
        isOpen={candPreview !== null}
        onClose={() => setCandPreview(null)}
      />
    </div>
  );
}

interface ClassDetailScreenProps {
  readonly target: ClassResponse;
  readonly subjects: ReturnType<typeof useMarketplace>['subjects'];
  readonly grades: ReturnType<typeof useMarketplace>['grades'];
  readonly onChosen: () => void;
  readonly onEdit: (c: ClassResponse) => void;
  readonly onUnpublish: (classId: number) => void;
  readonly onBack: () => void;
}

function ClassDetailScreen({
  target,
  subjects,
  grades,
  onChosen,
  onEdit,
  onUnpublish,
  onBack,
}: ClassDetailScreenProps) {
  const navigate = useNavigate();
  return (
    <section className="mkt-detail">
      <div className="mkt-detail__bar">
        <button type="button" className="mkt-btn mkt-btn--ghost" onClick={onBack}>
          ← Quay lại danh sách
        </button>
        <div className="mkt-detail__bar-right">
          {target.status === 'MATCHED' && target.assignmentId != null && (
            <button
              type="button"
              className="mkt-btn mkt-btn--primary"
              onClick={() =>
                navigate(APP_ROUTES.signContract, {
                  state: { assignmentId: target.assignmentId },
                })
              }
            >
              ✍️ Ký hợp đồng
            </button>
          )}
          {isEditableClass(target) && (
            <button type="button" className="mkt-btn mkt-btn--ghost" onClick={() => onEdit(target)}>
              ✏️ Sửa lớp
            </button>
          )}
          {target.status === 'OPEN' && isEditableClass(target) && (
            <button
              type="button"
              className="mkt-btn mkt-btn--ghost"
              onClick={() => onUnpublish(target.classId)}
            >
              ↩ Gỡ đăng
            </button>
          )}
          <span className={`mkt-status mkt-status--${target.status.toLowerCase()}`}>
            {CLASS_STATUS_LABELS[target.status] ?? target.status}
          </span>
        </div>
      </div>

      <div className="mkt-detail__cols">
        <aside className="mkt-detail__col mkt-detail__col--info">
          <div className="mkt-card">
            <div className="mkt-card__head">
              <h2>{target.title}</h2>
            </div>
            <ClassDetailPanel raw={target} subjects={subjects} grades={grades} />
          </div>
        </aside>

        <div className="mkt-detail__col">
          <div className="mkt-card">
            <div className="mkt-card__head">
              <h2>Gia sư ứng tuyển</h2>
            </div>
            <div className="mkt-card__body">
              <ApplicantsPanel
                classId={target.classId}
                target={target}
                subjects={subjects}
                onChosen={onChosen}
              />
            </div>
          </div>
        </div>
      </div>

      {target.status === 'IN_PROGRESS' && <ClassTimetableCard classId={target.classId} />}
    </section>
  );
}

function ClassTimetableCard({ classId }: { readonly classId: number }) {
  const { status, lessons } = useClassLessons(classId);

  return (
    <div className="mkt-card mkt-detail__timetable">
      <div className="mkt-card__head">
        <h2>Thời khóa biểu lớp</h2>
      </div>
      <div className="mkt-card__body">
        {status === 'loading' && <p className="mkt-muted">Đang tải lịch học…</p>}
        {status === 'error' && <p className="mkt-muted">Không tải được lịch học của lớp.</p>}
        {status === 'success' &&
          (lessons.length === 0 ? (
            <p className="mkt-muted">Lớp chưa có buổi học nào.</p>
          ) : (
            <WeeklyTimetable lessons={lessons} readOnly />
          ))}
      </div>
    </div>
  );
}

interface ClassListProps {
  readonly status: ReturnType<typeof useMarketplace>['status'];
  readonly classes: ClassResponse[];
  readonly subjects: CatalogOption[];
  readonly onEdit: (c: ClassResponse) => void;
  readonly onPublish: (classId: number) => void;
  readonly onUnpublish: (classId: number) => void;
  readonly onOpenDetail: (c: ClassResponse) => void;
}

interface SubjectFeeRow {
  readonly name: string;
  readonly fee: number;
}

function subjectRowsOf(
  form: ClassFormValues,
  c: ClassResponse,
  subjects: CatalogOption[],
): SubjectFeeRow[] {
  const nameById = new Map(subjects.map((s) => [String(s.id), s.name]));
  if (c.detailsJson && form.subjectIds.length > 0) {
    return form.subjectIds.map((id) => ({
      name: isOtherSubject(id)
        ? form.subjectOthers[id]?.trim() || 'Môn khác'
        : (nameById.get(id) ?? `#${id}`),
      fee: Number(form.subjectFees[id]) || 0,
    }));
  }
  return c.subjectName ? [{ name: c.subjectName, fee: c.tuitionFee ?? 0 }] : [];
}

function fullAddressOf(form: ClassFormValues, c: ClassResponse): string {
  const parts = [form.address, form.wardName, form.districtName, form.provinceName]
    .map((s) => s.trim())
    .filter(Boolean);
  return parts.join(', ') || c.address || '';
}

function ClassList({
  status,
  classes,
  subjects,
  onEdit,
  onPublish,
  onUnpublish,
  onOpenDetail,
}: ClassListProps) {
  if (status === 'loading') {
    return <div className="mkt-state">Đang tải danh sách lớp…</div>;
  }
  if (status === 'error') {
    return <div className="mkt-alert mkt-alert--error">Không tải được danh sách lớp.</div>;
  }
  if (classes.length === 0) {
    return (
      <div className="mkt-empty">
        Bạn chưa có lớp nào. Nhấn <strong>“Tạo lớp mới”</strong> để đăng yêu cầu tìm gia sư đầu tiên.
      </div>
    );
  }

  return (
    <div className="mkt-grid">
      {classes.map((c) => {
        const form = classToForm(c);
        const subjectRows = subjectRowsOf(form, c, subjects);
        const isOnline = c.lessonMode === 'ONLINE';
        const address = fullAddressOf(form, c);
        return (
        <article key={c.classId} className="mkt-class-card">
          <div className="mkt-class-card__top">
            <span className={`mkt-status mkt-status--${c.status.toLowerCase()}`}>
              {CLASS_STATUS_LABELS[c.status] ?? c.status}
            </span>
          </div>
          <h3 className="mkt-class-card__title">{c.title}</h3>
          <dl className="mkt-class-card__meta">
            <div>
              <dt>Lớp</dt>
              <dd>{c.gradeName ?? '—'}</dd>
            </div>
            <div>
              <dt>Hình thức</dt>
              <dd>{isOnline ? 'Online' : 'Offline'}</dd>
            </div>
          </dl>
          <div className="mkt-class-card__subjects">
            <span className="mkt-class-card__subjects-label">
              {subjectRows.length > 1 ? 'Các môn & học phí/giờ' : 'Môn & học phí/giờ'}
            </span>
            {subjectRows.length > 0 ? (
              <ul className="mkt-subj-fees">
                {subjectRows.map((row, i) => (
                  <li key={i}>
                    <span className="mkt-subj-fees__name">📚 {row.name}</span>
                    <span className="mkt-subj-fees__fee">
                      {row.fee > 0 ? `${currency.format(row.fee)} đ` : '—'}
                    </span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mkt-class-card__loc">—</p>
            )}
          </div>
          {c.learningGoal && <p className="mkt-class-card__goal">🎯 {c.learningGoal}</p>}
          <p className="mkt-class-card__loc">📍 {isOnline ? 'Học Online' : address || '—'}</p>
          <div className="mkt-class-card__actions">
            {c.status === 'DRAFT' && (
              <button type="button" className="mkt-btn mkt-btn--ghost" onClick={() => onEdit(c)}>
                Sửa
              </button>
            )}
            {c.status === 'DRAFT' && (
              <button
                type="button"
                className="mkt-btn mkt-btn--primary"
                onClick={() => onPublish(c.classId)}
              >
                Đăng lớp
              </button>
            )}
            {c.status !== 'DRAFT' && isEditableClass(c) && (
              <button type="button" className="mkt-btn mkt-btn--ghost" onClick={() => onEdit(c)}>
                Sửa
              </button>
            )}
            {c.status === 'OPEN' && isEditableClass(c) && (
              <button
                type="button"
                className="mkt-btn mkt-btn--ghost"
                onClick={() => onUnpublish(c.classId)}
              >
                Gỡ đăng
              </button>
            )}
            {c.status !== 'DRAFT' && (
              <button
                type="button"
                className="mkt-btn mkt-btn--primary"
                onClick={() => onOpenDetail(c)}
              >
                Xem chi tiết &amp; gia sư ứng tuyển
                {c.applicationCount != null && c.applicationCount > 0
                  ? ` (${c.applicationCount})`
                  : ''}
              </button>
            )}
          </div>
        </article>
        );
      })}
    </div>
  );
}

function extractError(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string } | undefined;
    if (data?.message) return data.message;
  }
  if (err instanceof Error) return err.message;
  return 'Có lỗi xảy ra. Vui lòng thử lại.';
}
