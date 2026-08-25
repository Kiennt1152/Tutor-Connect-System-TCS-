import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { PaymentQrCountdown } from '../../../shared/components/PaymentQrCountdown';
import { SiteFooter } from '../../home/components/SiteFooter';
import { marketplaceApi } from '../api/marketplaceApi';
import { useMarketplace } from '../hooks/useMarketplace';
import { ClassRequestForm } from '../components/ClassRequestForm';
import { ApplicantsPanel } from '../components/ApplicantsPanel';
import { ClassDetailPanel } from '../components/ClassDetailPanel';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { FilePreviewModal } from '../../../shared/components/FilePreviewModal';
import { WeeklyTimetable } from '../../teaching/components/WeeklyTimetable';
import { LessonRequestDialog } from '../../teaching/components/LessonRequestDialog';
import { ExpiryBadge } from '../../../shared/components/ExpiryBadge';
import { useTeaching } from '../../teaching/hooks/useTeaching';
import type { LessonResponse } from '../../teaching/types/teachingTypes';
import { classToForm, emptyForm } from '../mappers/marketplaceMapper';
import {
  CLASS_STATUS_LABELS,
  type CenterRequestFeePayment,
  isOtherSubject,
  type CatalogOption,
  type ClassFormValues,
  type ClassRequest,
  type ClassRequestStatus,
  type ClassStatus,
  type ClassRequestPayload,
  type ClassResponse,
} from '../types/marketplaceTypes';
import './MarketplacePage.css';

const currency = new Intl.NumberFormat('vi-VN');

function formatMoney(value: number | null | undefined): string {
  if (typeof value !== 'number') return '—';
  return `${currency.format(value)} đ`;
}

// Nhãn trạng thái yêu cầu "nhờ trung tâm tìm".
const REQ_STATUS_LABEL: Record<ClassRequestStatus, string> = {
  PAYMENT_PENDING: 'Chờ thanh toán',
  PENDING: 'Đang chờ',
  SEARCHING: 'Đang tìm gia sư',
  ACCEPTED: 'Đã chấp nhận',
  REJECTED: 'Đã từ chối',
  CANCELLED: 'Đã hủy',
};

/**
 * Tin đã quá hạn hiển thị: gia sư không còn tìm thấy, chủ tin chỉ xem lại được ở mục
 * "Đã hết hạn" — không sửa, không đăng lại, không gỡ đăng.
 */
function isExpiredClass(c: ClassResponse): boolean {
  return c.status === 'OPEN' && !!c.expiresAt && Date.parse(c.expiresAt) <= Date.now();
}

function isEditableClass(c: ClassResponse): boolean {
  const noApplicants = (c.applicationCount ?? 0) === 0;
  return (c.status === 'DRAFT' || c.status === 'OPEN') && noApplicants && !isExpiredClass(c);
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
  const [paymentRequest, setPaymentRequest] = useState<CenterRequestFeePayment | null>(null);
  const [checkingPayment, setCheckingPayment] = useState(false);
  const [paymentError, setPaymentError] = useState('');
  // Gia sư đang mở xem chứng chỉ (khoá theo requestId:tutorId) + xem trước file.
  const [candCertsOpen, setCandCertsOpen] = useState<string | null>(null);
  const [candPreview, setCandPreview] = useState<{
    src: string;
    fileName: string;
    mimeType?: string | null;
  } | null>(null);
  useEffect(() => {
    if (!isClient) return;
    marketplaceApi
      .getMyClassRequests()
      .then(setCenterRequests)
      .catch(() => setCenterRequests([]));
  }, [isClient]);
  const cancelCenterRequest = async (requestId: string) => {
    try {
      await marketplaceApi.cancelClassRequest(requestId);
      const requests = await marketplaceApi.getMyClassRequests();
      setCenterRequests(requests);
    } catch {
      /* bỏ qua */
    }
  };
  const openPaymentFromRequest = (request: ClassRequest) => {
    if (!request.centerRequestFeePayment) {
      setPaymentError('Không tìm thấy thông tin thanh toán của yêu cầu này.');
      return;
    }
    setPaymentRequest(request.centerRequestFeePayment);
    setPaymentError('');
    setCheckingPayment(false);
  };

  const closePaymentModal = () => {
    setPaymentRequest(null);
    setPaymentError('');
    setCheckingPayment(false);
  };

  const checkPaymentStatus = async () => {
    if (!paymentRequest) return;
    setCheckingPayment(true);
    setPaymentError('');
    try {
      const requests = await marketplaceApi.getMyClassRequests();
      const current = requests.find((item) => item.requestId === paymentRequest.requestId);
      const latestPayment = current?.centerRequestFeePayment ?? paymentRequest;
      setCenterRequests(requests);
      setPaymentRequest(latestPayment);
      if (current && current.status !== 'PAYMENT_PENDING') {
        setReqNotice('Thanh toán thành công. Yêu cầu đã được gửi tới trung tâm.');
        window.setTimeout(() => setReqNotice(''), 6000);
      } else if (latestPayment.status === 'PENDING_PAYMENT') {
        setPaymentError('Chưa ghi nhận thanh toán. Vui lòng kiểm tra lại sau vài giây.');
      }
    } catch (err) {
      setPaymentError(extractError(err));
    } finally {
      setCheckingPayment(false);
    }
  };

  // Phụ huynh chọn 1 gia sư đề cử -> xác nhận (ConfirmDialog) rồi materialize.
  const confirmChooseTutor = async () => {
    if (!chooseTarget) return;
    setChooseBusy(true);
    try {
      await marketplaceApi.chooseTutorForRequest(chooseTarget.requestId, chooseTarget.tutorId);
      reload();
      const requests = await marketplaceApi.getMyClassRequests();
      setCenterRequests(requests);
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
              <h1 className="mkt-title">Yêu cầu tìm gia sư của tôi</h1>
              <p className="mkt-subtitle">
                Đăng nhu cầu tìm gia sư cho con em bạn — chọn môn, lớp, mục tiêu, học phí. Gia sư
                phù hợp sẽ ứng tuyển sau khi bạn đăng lớp.
              </p>
            </div>
            {mode.kind === 'list' && isClient && (
              <div className="mkt-header__actions">
                <Link className="mkt-btn mkt-btn--ghost" to={APP_ROUTES.classBoard}>
                  Danh sách tin đã đăng
                </Link>
                <Link className="mkt-btn mkt-btn--primary" to={APP_ROUTES.postTutorRequest}>
                  Tạo tin mới
                </Link>
              </div>
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
              <p className="mkt-section-desc">Tin bạn tự đăng để gia sư ứng tuyển trực tiếp.</p>
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
                  {centerRequests.map((r) => {
                    const payment = r.centerRequestFeePayment;
                    const isPaymentPending =
                      r.status === 'PAYMENT_PENDING' && payment?.status === 'PENDING_PAYMENT';
                    return (
                      <div
                        key={r.requestId}
                        className={`mkt-req-card${isPaymentPending ? ' mkt-req-card--payment' : ''}`}
                      >
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
                          {isPaymentPending && payment && (
                            <div className="mkt-req-card__payment">
                              <div>
                                <strong>Đang chờ thanh toán phí xử lý</strong>
                                <span>
                                  Số tiền: <b>{formatMoney(payment.amount)}</b>. Bạn có thể đóng
                                  trang và quay lại đây để mở lại mã QR.
                                </span>
                              </div>
                              <button
                                type="button"
                                className="mkt-btn mkt-btn--primary mkt-btn--sm"
                                onClick={() => openPaymentFromRequest(r)}
                              >
                                Mở QR thanh toán
                              </button>
                            </div>
                          )}
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
                                                        mimeType: cert.mimeType,
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
                        {r.status === 'PAYMENT_PENDING' && (
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
                    );
                  })}
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
        mimeType={candPreview?.mimeType}
        isOpen={candPreview !== null}
        onClose={() => setCandPreview(null)}
      />

      {paymentRequest && (
        <div className="mkt-payment-overlay" role="dialog" aria-modal="true" onClick={closePaymentModal}>
          <div className="mkt-payment-modal" onClick={(event) => event.stopPropagation()}>
            <div className="mkt-payment-modal__top">
              <div>
                <span>Phí xử lý yêu cầu trung tâm</span>
                <h3>Quét mã để thanh toán</h3>
              </div>
              <button
                type="button"
                className="mkt-payment-modal__close"
                aria-label="Đóng mã thanh toán"
                onClick={closePaymentModal}
              >
                ×
              </button>
            </div>

            {paymentError && <p className="mkt-payment-modal__error">{paymentError}</p>}

            {paymentRequest.status === 'PENDING_PAYMENT' ? (
              <>
                <p className="mkt-payment-modal__hint">
                  Sau khi SePay ghi nhận thanh toán, yêu cầu mới được gửi vào danh sách xử lý của
                  trung tâm.
                </p>
                <div className="mkt-payment-modal__body">
                  <div className="mkt-payment-modal__qr">
                    <img src={paymentRequest.qrUrl} alt="QR thanh toán phí xử lý yêu cầu" />
                    <PaymentQrCountdown
                      resetKey={
                        paymentRequest.requestId
                        ?? paymentRequest.transferContent
                        ?? paymentRequest.qrUrl
                      }
                      label="Thời gian chuyển khoản còn lại"
                      expiredLabel="Mã QR đã hết 5 phút hiển thị. Vui lòng tạo lại yêu cầu nếu chưa chuyển khoản."
                    />
                  </div>
                  <div className="mkt-payment-modal__info">
                    <div>
                      <span>Số tiền</span>
                      <strong>{formatMoney(paymentRequest.amount)}</strong>
                    </div>
                    <div>
                      <span>Ngân hàng</span>
                      <strong>{paymentRequest.bankName}</strong>
                    </div>
                    <div>
                      <span>Số tài khoản</span>
                      <strong>{paymentRequest.accountNumber}</strong>
                    </div>
                    <div>
                      <span>Nội dung chuyển khoản</span>
                      <strong>{paymentRequest.transferContent}</strong>
                    </div>
                    <button
                      type="button"
                      className="mkt-btn mkt-btn--ghost mkt-btn--sm"
                      onClick={() => navigator.clipboard?.writeText(paymentRequest.transferContent)}
                    >
                      Sao chép nội dung
                    </button>
                  </div>
                </div>
                <div className="mkt-payment-modal__actions">
                  <button type="button" className="mkt-btn mkt-btn--ghost" onClick={closePaymentModal}>
                    Đóng
                  </button>
                  <button
                    type="button"
                    className="mkt-btn mkt-btn--primary"
                    onClick={checkPaymentStatus}
                    disabled={checkingPayment}
                  >
                    {checkingPayment ? 'Đang quét…' : 'Quét trạng thái'}
                  </button>
                </div>
              </>
            ) : (
              <div className="mkt-payment-modal__success">
                <span>✓</span>
                <h3>Thanh toán đã được ghi nhận</h3>
                <p>Yêu cầu đã được gửi tới trung tâm để xử lý.</p>
                <button type="button" className="mkt-btn mkt-btn--primary" onClick={closePaymentModal}>
                  Đóng
                </button>
              </div>
            )}
          </div>
        </div>
      )}
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
  const { status, lessons: allLessons, requests, error, requestReschedule } = useTeaching();
  const [dialogLesson, setDialogLesson] = useState<LessonResponse | null>(null);

  const lessons = allLessons.filter((l) => l.classId === classId);
  // Buổi đang có yêu cầu đổi lịch chờ duyệt -> hiện "chờ duyệt" thay vì nút đổi lịch.
  const pendingLessonIds = new Set(
    requests.filter((r) => r.status === 'PENDING').flatMap((r) => r.lessonId ?? []),
  );

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
            <WeeklyTimetable
              lessons={lessons}
              readOnly
              onReschedule={(lesson) => setDialogLesson(lesson)}
              pendingLessonIds={pendingLessonIds}
            />
          ))}
      </div>
      {dialogLesson && (
        <LessonRequestDialog
          lesson={dialogLesson}
          submitError={error}
          existingLessons={lessons}
          onClose={() => setDialogLesson(null)}
          onSubmit={async (payload) => {
            const ok = await requestReschedule(dialogLesson.lessonId, payload);
            if (ok) setDialogLesson(null);
            return ok;
          }}
        />
      )}
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
  const PAGE_SIZE = 6;
  const [page, setPage] = useState(1);
  const [statusFilter, setStatusFilter] = useState<ClassStatus | 'ALL' | 'EXPIRED'>('ALL');

  // Thứ tự ưu tiên: (0) tin đã có gia sư ứng tuyển — cần bạn xem đơn ngay,
  // (1) tin còn hạn chưa ai ứng tuyển, (2) tin hết hạn — chỉ lưu để xem lại.
  // Trong mỗi nhóm: tin mới nhất lên đầu (createdAt giảm dần, fallback classId).
  const sorted = useMemo(() => {
    const rank = (c: ClassResponse) => {
      if (isExpiredClass(c)) return 2;
      return (c.applicationCount ?? 0) > 0 ? 0 : 1;
    };
    return [...classes].sort((a, b) => {
      const ra = rank(a);
      const rb = rank(b);
      if (ra !== rb) return ra - rb;
      // Cùng nhóm "có ứng tuyển": nhiều đơn hơn lên trước.
      if (ra === 0) {
        const na = a.applicationCount ?? 0;
        const nb = b.applicationCount ?? 0;
        if (nb !== na) return nb - na;
      }
      const ta = a.createdAt ? Date.parse(a.createdAt) : 0;
      const tb = b.createdAt ? Date.parse(b.createdAt) : 0;
      if (tb !== ta) return tb - ta;
      return b.classId - a.classId;
    });
  }, [classes]);

  // Các tab lọc trạng thái: "Tất cả" + mỗi trạng thái đang có (theo thứ tự vòng đời), kèm số đếm.
  // Tin hết hạn tách ra tab riêng nên không tính vào "Đang mở".
  const statusTabs = useMemo(() => {
    const order = Object.keys(CLASS_STATUS_LABELS) as ClassStatus[];
    const counts = new Map<ClassStatus, number>();
    for (const c of sorted) {
      if (isExpiredClass(c)) continue;
      counts.set(c.status, (counts.get(c.status) ?? 0) + 1);
    }
    return order.filter((s) => counts.has(s)).map((s) => ({ status: s, count: counts.get(s) ?? 0 }));
  }, [sorted]);

  const expiredCount = useMemo(() => sorted.filter(isExpiredClass).length, [sorted]);

  const filtered = useMemo(() => {
    if (statusFilter === 'ALL') return sorted;
    if (statusFilter === 'EXPIRED') return sorted.filter(isExpiredClass);
    return sorted.filter((c) => c.status === statusFilter && !isExpiredClass(c));
  }, [sorted, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const pageItems = filtered.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  // Về trang 1 khi đổi bộ lọc hoặc số lượng tin đổi (thêm/xóa).
  useEffect(() => {
    setPage(1);
  }, [classes.length, statusFilter]);

  if (status === 'loading') {
    return <div className="mkt-state">Đang tải danh sách lớp…</div>;
  }
  if (status === 'error') {
    return <div className="mkt-alert mkt-alert--error">Không tải được danh sách lớp.</div>;
  }
  if (classes.length === 0) {
    return (
      <div className="mkt-empty">
        Bạn chưa có tin nào. Nhấn <strong>“Tạo tin mới”</strong> để đăng yêu cầu tìm gia sư đầu tiên.
      </div>
    );
  }

  return (
    <>
    <div className="mkt-filter" role="tablist" aria-label="Lọc theo trạng thái">
      <button
        type="button"
        role="tab"
        aria-selected={statusFilter === 'ALL'}
        className={`mkt-filter__tab${statusFilter === 'ALL' ? ' mkt-filter__tab--active' : ''}`}
        onClick={() => setStatusFilter('ALL')}
      >
        Tất cả ({sorted.length})
      </button>
      {statusTabs.map(({ status: s, count }) => (
        <button
          key={s}
          type="button"
          role="tab"
          aria-selected={statusFilter === s}
          className={`mkt-filter__tab${statusFilter === s ? ' mkt-filter__tab--active' : ''}`}
          onClick={() => setStatusFilter(s)}
        >
          {CLASS_STATUS_LABELS[s]} ({count})
        </button>
      ))}
      {expiredCount > 0 && (
        <button
          type="button"
          role="tab"
          aria-selected={statusFilter === 'EXPIRED'}
          className={`mkt-filter__tab${statusFilter === 'EXPIRED' ? ' mkt-filter__tab--active' : ''}`}
          onClick={() => setStatusFilter('EXPIRED')}
        >
          Đã hết hạn ({expiredCount})
        </button>
      )}
    </div>

    {pageItems.length === 0 ? (
      <div className="mkt-empty">Không có tin nào ở trạng thái này.</div>
    ) : (
    <div className="mkt-grid">
      {pageItems.map((c) => {
        const form = classToForm(c);
        const subjectRows = subjectRowsOf(form, c, subjects);
        const isOnline = c.lessonMode === 'ONLINE';
        const address = fullAddressOf(form, c);
        const expired = isExpiredClass(c);
        return (
        <article key={c.classId} className={`mkt-class-card${expired ? ' mkt-class-card--expired' : ''}`}>
          <div className="mkt-class-card__top">
            <span className={`mkt-status mkt-status--${expired ? 'expired' : c.status.toLowerCase()}`}>
              {expired ? 'Đã hết hạn' : (CLASS_STATUS_LABELS[c.status] ?? c.status)}
            </span>
            {c.status === 'OPEN' && c.expiresAt && <ExpiryBadge expiresAt={c.expiresAt} />}
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
          {expired && (
            <p className="mkt-class-card__archived">
              🔒 Tin đã hết hạn, được lưu lại để xem. Không sửa hay đăng lại được — cần tuyển tiếp
              thì tạo tin mới.
            </p>
          )}
          <div className="mkt-class-card__actions">
            {!expired && c.status === 'DRAFT' && (
              <button type="button" className="mkt-btn mkt-btn--ghost" onClick={() => onEdit(c)}>
                Sửa
              </button>
            )}
            {!expired && c.status === 'DRAFT' && (
              <button
                type="button"
                className="mkt-btn mkt-btn--primary"
                onClick={() => onPublish(c.classId)}
              >
                Đăng lớp
              </button>
            )}
            {!expired && c.status !== 'DRAFT' && isEditableClass(c) && (
              <button type="button" className="mkt-btn mkt-btn--ghost" onClick={() => onEdit(c)}>
                Sửa
              </button>
            )}
            {!expired && c.status === 'OPEN' && isEditableClass(c) && (
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
                {expired ? 'Xem lại tin' : 'Xem chi tiết & gia sư ứng tuyển'}
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
    )}

    {totalPages > 1 && (
      <nav className="mkt-pagination" aria-label="Phân trang tin đã đăng">
        <button
          type="button"
          className="mkt-pagination__nav"
          onClick={() => setPage((p) => Math.max(1, p - 1))}
          disabled={currentPage === 1}
        >
          ← Trước
        </button>
        {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
          <button
            key={p}
            type="button"
            className={`mkt-pagination__page${p === currentPage ? ' mkt-pagination__page--active' : ''}`}
            onClick={() => setPage(p)}
            aria-current={p === currentPage ? 'page' : undefined}
          >
            {p}
          </button>
        ))}
        <button
          type="button"
          className="mkt-pagination__nav"
          onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
          disabled={currentPage === totalPages}
        >
          Sau →
        </button>
      </nav>
    )}
    </>
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
