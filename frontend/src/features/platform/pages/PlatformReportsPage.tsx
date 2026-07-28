import { useEffect, useState, type FormEvent } from 'react';
import { AdminLayout } from '../components/AdminLayout';
import { useDisputeReviewList } from '../hooks/useDisputeReviewList';
import {
  useAppealDispute,
  useExecuteRefund,
  useExecuteSettlement,
  useResolveDispute,
} from '../hooks/usePlatformMutations';
import { useReportList } from '../hooks/useReportList';
import type {
  AdminDisputeReviewApiResponse,
  DisputeReviewItem,
  DisputeStatus,
  EscrowStatus,
  RefundRequestStatus,
  ReportStatus,
} from '../types/platformTypes';
import './PlatformReportsPage.css';

type ResolutionStatus = Exclude<DisputeStatus, 'OPEN'>;

const RESOLUTION_STATUS_OPTIONS: { value: ResolutionStatus; label: string }[] = [
  { value: 'UNDER_INVESTIGATION', label: 'Đang xem xét' },
  { value: 'WAITING', label: 'Chờ bổ sung bằng chứng' },
  { value: 'RESOLVED', label: 'Đã xử lý' },
];

function reportBadgeClass(status: ReportStatus) {
  return status === 'PENDING' ? 'tcs-badge tcs-badge--suspended' : 'tcs-badge tcs-badge--active';
}

function disputeBadgeClass(status: DisputeStatus) {
  if (status === 'RESOLVED') return 'tcs-badge tcs-badge--active';
  if (status === 'UNDER_INVESTIGATION') return 'tcs-badge tcs-badge--role';
  return 'tcs-badge tcs-badge--suspended';
}

function escrowBadgeClass(status: EscrowStatus | null) {
  if (status === 'RELEASED' || status === 'REFUNDED') return 'tcs-badge tcs-badge--active';
  if (status === 'DISPUTED' || status === 'ON_HOLD') return 'tcs-badge tcs-badge--suspended';
  if (!status) return 'tcs-badge tcs-badge--role';
  return 'tcs-badge tcs-badge--role';
}

function refundBadgeClass(status: RefundRequestStatus | null) {
  if (status === 'COMPLETED') return 'tcs-badge tcs-badge--active';
  if (status === 'REJECTED') return 'tcs-badge tcs-badge--banned';
  if (status === 'APPROVED') return 'tcs-badge tcs-badge--role';
  return 'tcs-badge tcs-badge--suspended';
}

function isEscrowSettled(status: EscrowStatus | null | undefined) {
  return status === 'RELEASED' || status === 'REFUNDED';
}

function isEscrowHeldForDispute(status: EscrowStatus | null | undefined) {
  return status === 'ON_HOLD' || status === 'DISPUTED';
}

function isEscrowSettleable(status: EscrowStatus | null | undefined) {
  return status === 'FUNDED' || status === 'ON_HOLD' || status === 'DISPUTED';
}

const normalizeDigits = (value: string) => value.replace(/[^\d]/g, '');

const formatDateTime = (value: string | null | undefined) => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
};

const formatDate = (value: string | null | undefined) => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date);
};

const formatCurrency = (value: number | null | undefined) => {
  if (typeof value !== 'number') return '—';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value);
};

function InfoRow({ label, value }: { label: string; value: string | number | null | undefined }) {
  return (
    <div className="pd-info-row">
      <span className="pd-info-row__label">{label}</span>
      <span className="pd-info-row__value">{value ?? '—'}</span>
    </div>
  );
}

function AutomationState({ detail }: { detail: AdminDisputeReviewApiResponse }) {
  const escrowStatus = detail.escrow?.status ?? null;
  const settled = isEscrowSettled(escrowStatus);
  const held = isEscrowHeldForDispute(escrowStatus);
  const hasPendingTermination = detail.terminationRequest?.status === 'PENDING';

  let tone = 'neutral';
  let title = 'Theo dõi ngoại lệ';
  let message = 'Các luồng đủ dữ liệu sẽ tự động tất toán; admin xử lý khi có tranh chấp hoặc thiếu căn cứ.';

  if (!detail.escrow?.escrowId) {
    tone = 'warning';
    title = 'Thiếu escrow liên quan';
    message = 'Cần kiểm tra lại dữ liệu hợp đồng/lớp trước khi ra quyết định tài chính.';
  } else if (settled) {
    tone = 'success';
    title = 'Escrow đã tất toán';
    message = 'Tiền đã được giải ngân hoặc hoàn lại; tranh chấp chỉ còn phần kết luận hồ sơ.';
  } else if (held) {
    tone = 'danger';
    title = 'Cần admin quyết định';
    message = 'Escrow đang được giữ do tranh chấp nên hệ thống không tự release/refund.';
  } else if (hasPendingTermination) {
    tone = 'warning';
    title = 'Chấm dứt sớm cần duyệt';
    message = 'Yêu cầu chấm dứt sớm đang chờ quyết định trước khi tất toán tài chính.';
  }

  return (
    <section className={`pd-flow pd-flow--${tone}`}>
      <div>
        <p className="pd-flow__label">Luồng xử lý</p>
        <h3 className="pd-flow__title">{title}</h3>
      </div>
      <p className="pd-flow__message">{message}</p>
    </section>
  );
}

function FinancialDecisionPanel({
  detail,
  onChanged,
}: {
  detail: AdminDisputeReviewApiResponse;
  onChanged: () => void;
}) {
  const {
    status: settlementStatus,
    errorMessage: settlementErrorMessage,
    executeSettlement,
    reset: resetSettlement,
  } = useExecuteSettlement();
  const {
    status: refundStatus,
    errorMessage: refundErrorMessage,
    executeRefund,
    reset: resetRefund,
  } = useExecuteRefund();
  const [releaseAmount, setReleaseAmount] = useState('');
  const [refundAmount, setRefundAmount] = useState('');
  const [reason, setReason] = useState('');
  const [formError, setFormError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const escrow = detail.escrow;
  const escrowAmount = typeof escrow?.amount === 'number' ? escrow.amount : 0;
  const releaseAmountNumber = releaseAmount ? Number(releaseAmount) : 0;
  const refundAmountNumber = refundAmount ? Number(refundAmount) : 0;
  const totalSettlement = releaseAmountNumber + refundAmountNumber;
  const remainingAmount = escrowAmount > 0 ? escrowAmount - totalSettlement : null;
  const settled = isEscrowSettled(escrow?.status);
  const settleable = isEscrowSettleable(escrow?.status);
  const isSubmitting = settlementStatus === 'loading' || refundStatus === 'loading';

  useEffect(() => {
    const amount = typeof detail.escrow?.amount === 'number'
      ? Math.trunc(detail.escrow.amount)
      : 0;
    setReleaseAmount(amount > 0 ? String(amount) : '');
    setRefundAmount('');
    setReason(`Tất toán escrow #${detail.escrow?.escrowId ?? ''} theo quyết định tranh chấp #${detail.disputeId}`);
    setFormError('');
    setSuccessMessage('');
    resetSettlement();
    resetRefund();
  }, [detail.disputeId, detail.escrow?.escrowId, detail.escrow?.amount, resetRefund, resetSettlement]);

  const setSplit = (release: number, refund: number) => {
    setReleaseAmount(release > 0 ? String(Math.trunc(release)) : '');
    setRefundAmount(refund > 0 ? String(Math.trunc(refund)) : '');
    setFormError('');
  };

  const applyQuickAction = (mode: 'release-all' | 'refund-all' | 'half' | 'refund-30') => {
    if (escrowAmount <= 0) {
      setFormError('Escrow chưa có số tiền hợp lệ.');
      return;
    }
    if (mode === 'release-all') {
      setSplit(escrowAmount, 0);
      return;
    }
    if (mode === 'refund-all') {
      setSplit(0, escrowAmount);
      return;
    }
    if (mode === 'half') {
      const refund = Math.trunc(escrowAmount / 2);
      setSplit(escrowAmount - refund, refund);
      return;
    }
    const refund = Math.trunc(escrowAmount * 0.3);
    setSplit(escrowAmount - refund, refund);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!escrow?.escrowId) {
      setFormError('Không tìm thấy escrow để tất toán.');
      return;
    }
    if (settled) {
      setFormError('Escrow đã tất toán.');
      return;
    }
    if (!settleable) {
      setFormError('Escrow chưa ở trạng thái có thể tất toán.');
      return;
    }
    if (!Number.isFinite(releaseAmountNumber) || !Number.isFinite(refundAmountNumber)) {
      setFormError('Số tiền giải ngân hoặc hoàn tiền không hợp lệ.');
      return;
    }
    if (releaseAmountNumber < 0 || refundAmountNumber < 0) {
      setFormError('Số tiền không được âm.');
      return;
    }
    if (totalSettlement <= 0) {
      setFormError('Cần có số tiền giải ngân hoặc hoàn tiền.');
      return;
    }
    if (escrowAmount > 0 && totalSettlement !== escrowAmount) {
      setFormError('Tổng giải ngân và hoàn tiền phải bằng tổng escrow.');
      return;
    }
    if (reason.trim().length < 10) {
      setFormError('Vui lòng nhập lý do tất toán ít nhất 10 ký tự.');
      return;
    }

    setFormError('');
    setSuccessMessage('');
    const payload = {
      escrowId: escrow.escrowId,
      releaseToBeneficiary: releaseAmountNumber,
      refundToPayer: refundAmountNumber,
      reason: reason.trim(),
    };

    const result = refundAmountNumber > 0
      ? await executeRefund(payload)
      : await executeSettlement(payload);

    if (result) {
      setSuccessMessage(typeof result === 'string'
        ? result
        : `${result.message}: hoàn ${formatCurrency(result.refundToPayer)}, giải ngân ${formatCurrency(result.releaseToBeneficiary)}.`);
      onChanged();
    }
  };

  return (
    <section className="pd-section">
      <div className="pd-section__head">
        <h3 className="pd-section__title">Quyết định tài chính</h3>
        <span className={escrowBadgeClass(escrow?.status ?? null)}>{escrow?.status ?? '—'}</span>
      </div>

      {settled ? (
        <div className="adm-alert adm-alert--info pd-resolution-alert">
          Escrow đã tất toán, không cần thao tác release/refund thêm.
        </div>
      ) : (
        <form className="pd-settlement-form" onSubmit={handleSubmit}>
          <div className="pd-settlement-summary">
            <div>
              <span>Tổng escrow</span>
              <strong>{formatCurrency(escrow?.amount)}</strong>
            </div>
            <div>
              <span>Giải ngân</span>
              <strong>{formatCurrency(releaseAmountNumber)}</strong>
            </div>
            <div>
              <span>Hoàn lại</span>
              <strong>{formatCurrency(refundAmountNumber)}</strong>
            </div>
            <div className={remainingAmount === 0 ? 'pd-settlement-summary__ok' : ''}>
              <span>Còn lệch</span>
              <strong>{remainingAmount == null ? '—' : formatCurrency(remainingAmount)}</strong>
            </div>
          </div>

          <div className="pd-quick-actions">
            <button
              className="tcs-btn tcs-btn--soft tcs-btn--sm"
              type="button"
              disabled={isSubmitting || !settleable}
              onClick={() => applyQuickAction('release-all')}
            >
              Giải ngân 100%
            </button>
            <button
              className="tcs-btn tcs-btn--soft tcs-btn--sm"
              type="button"
              disabled={isSubmitting || !settleable}
              onClick={() => applyQuickAction('refund-all')}
            >
              Hoàn 100%
            </button>
            <button
              className="tcs-btn tcs-btn--soft tcs-btn--sm"
              type="button"
              disabled={isSubmitting || !settleable}
              onClick={() => applyQuickAction('half')}
            >
              Chia 50/50
            </button>
            <button
              className="tcs-btn tcs-btn--soft tcs-btn--sm"
              type="button"
              disabled={isSubmitting || !settleable}
              onClick={() => applyQuickAction('refund-30')}
            >
              Hoàn 30%
            </button>
          </div>

          <div className="pd-money-grid">
            <label className="pd-field">
              <span>Giải ngân cho tutor/trung tâm</span>
              <input
                className="adm-field"
                inputMode="numeric"
                value={releaseAmount}
                disabled={isSubmitting || !settleable}
                onChange={(event) => setReleaseAmount(normalizeDigits(event.target.value))}
              />
            </label>
            <label className="pd-field">
              <span>Hoàn lại người thanh toán</span>
              <input
                className="adm-field"
                inputMode="numeric"
                value={refundAmount}
                disabled={isSubmitting || !settleable}
                onChange={(event) => setRefundAmount(normalizeDigits(event.target.value))}
              />
            </label>
          </div>

          <label className="pd-field">
            <span>Lý do tất toán</span>
            <textarea
              className="pd-textarea pd-textarea--compact"
              rows={3}
              maxLength={1000}
              value={reason}
              disabled={isSubmitting || !settleable}
              onChange={(event) => setReason(event.target.value)}
            />
          </label>

          {!settleable && (
            <div className="adm-alert adm-alert--error">
              Escrow hiện chưa ở trạng thái có thể tất toán.
            </div>
          )}
          {formError && <div className="adm-alert adm-alert--error">{formError}</div>}
          {settlementStatus === 'error' && settlementErrorMessage && (
            <div className="adm-alert adm-alert--error">{settlementErrorMessage}</div>
          )}
          {refundStatus === 'error' && refundErrorMessage && (
            <div className="adm-alert adm-alert--error">{refundErrorMessage}</div>
          )}
          {successMessage && <div className="adm-alert adm-alert--success">{successMessage}</div>}

          <div className="pd-resolution-actions">
            <button
              className="tcs-btn tcs-btn--primary"
              type="submit"
              disabled={isSubmitting || !settleable}
            >
              {isSubmitting ? 'Đang tất toán...' : 'Thực thi tất toán'}
            </button>
          </div>
        </form>
      )}
    </section>
  );
}

function DisputeDetail({
  detail,
  status,
  errorMessage,
  onChanged,
}: {
  detail: AdminDisputeReviewApiResponse | null;
  status: 'loading' | 'success' | 'error';
  errorMessage: string | null;
  onChanged: () => void;
}) {
  const {
    status: resolveStatus,
    errorMessage: resolveErrorMessage,
    resolveDispute,
    reset: resetResolve,
  } = useResolveDispute();
  const {
    status: appealStatus,
    errorMessage: appealErrorMessage,
    appealDispute,
    reset: resetAppeal,
  } = useAppealDispute();
  const [resolutionStatus, setResolutionStatus] = useState<ResolutionStatus>('UNDER_INVESTIGATION');
  const [resolutionNote, setResolutionNote] = useState('');
  const [appealReason, setAppealReason] = useState('');
  const [appealEvidenceUrls, setAppealEvidenceUrls] = useState('');
  const [appealFormError, setAppealFormError] = useState('');
  const [formError, setFormError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    if (!detail) return;
    setResolutionStatus(
      detail.disputeStatus === 'OPEN' ? 'UNDER_INVESTIGATION' : detail.disputeStatus,
    );
    setResolutionNote(detail.resolution ?? '');
    setAppealReason('');
    setAppealEvidenceUrls('');
    setAppealFormError('');
    setFormError('');
    setSuccessMessage('');
    resetResolve();
    resetAppeal();
  }, [detail?.disputeId, detail?.disputeStatus, detail?.resolution, resetAppeal, resetResolve]);

  const handleResolve = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!detail) return;

    const trimmedResolution = resolutionNote.trim();
    if (trimmedResolution.length < 10) {
      setFormError('Vui lòng nhập nội dung quyết định ít nhất 10 ký tự.');
      return;
    }

    setFormError('');
    setSuccessMessage('');
    const updated = await resolveDispute(String(detail.disputeId), resolutionStatus, trimmedResolution);
    if (updated) {
      setSuccessMessage('Đã lưu quyết định xử lý tranh chấp.');
      onChanged();
    }
  };

  const handleAppeal = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!detail) return;

    const trimmedReason = appealReason.trim();
    if (trimmedReason.length < 10) {
      setAppealFormError('Vui lòng nhập nội dung mở lại ít nhất 10 ký tự.');
      return;
    }

    setAppealFormError('');
    setSuccessMessage('');
    const updated = await appealDispute(String(detail.disputeId), {
      reason: trimmedReason,
      evidenceUrls: appealEvidenceUrls,
    });
    if (updated) {
      setSuccessMessage('Đã mở lại tranh chấp để tiếp tục xem xét.');
      onChanged();
    }
  };

  if (!detail) {
    return (
      <div className="pd-detail pd-detail--empty">
        <p>Chọn một tranh chấp để xem chi tiết.</p>
      </div>
    );
  }

  if (status === 'loading') {
    return (
      <div className="pd-detail">
        <div className="adm-state">Đang tải chi tiết tranh chấp…</div>
      </div>
    );
  }

  if (status === 'error') {
    return (
      <div className="pd-detail">
        <div className="adm-alert adm-alert--error">
          {errorMessage ?? 'Không tải được chi tiết tranh chấp.'}
        </div>
      </div>
    );
  }

  const appealBlockedBySettlement = isEscrowSettled(detail.escrow?.status);

  return (
    <div className="pd-detail">
      <div className="pd-detail__head">
        <div>
          <p className="pd-detail__eyebrow">Tranh chấp #{detail.disputeId}</p>
          <h2 className="pd-detail__title">{detail.tutoringClass?.title ?? 'Không xác định lớp'}</h2>
        </div>
        <span className={disputeBadgeClass(detail.disputeStatus)}>{detail.disputeStatus}</span>
      </div>

      <AutomationState detail={detail} />

      <section className="pd-section">
        <h3 className="pd-section__title">Báo cáo</h3>
        <div className="pd-info-grid">
          <InfoRow label="Mã báo cáo" value={detail.reportId ? `#${detail.reportId}` : '—'} />
          <InfoRow label="Người báo cáo" value={detail.reporterEmail ?? detail.reporterId} />
          <InfoRow label="Loại đối tượng" value={detail.targetType} />
          <InfoRow label="ID đối tượng" value={detail.targetId ? `#${detail.targetId}` : '—'} />
          <InfoRow label="Danh mục" value={detail.category} />
          <InfoRow label="Tạo lúc" value={formatDateTime(detail.reportCreatedAt)} />
        </div>
        <p className="pd-description">{detail.description ?? '—'}</p>
      </section>

      <section className="pd-section">
        <h3 className="pd-section__title">Bằng chứng</h3>
        {detail.evidenceUrlList.length === 0 ? (
          <p className="adm-muted">Không có bằng chứng đính kèm.</p>
        ) : (
          <div className="pd-evidence-list">
            {detail.evidenceUrlList.map((url) => (
              <a key={url} className="pd-evidence-link" href={url} target="_blank" rel="noreferrer">
                {url}
              </a>
            ))}
          </div>
        )}
      </section>

      <FinancialDecisionPanel detail={detail} onChanged={onChanged} />

      <section className="pd-section">
        <h3 className="pd-section__title">Quyết định xử lý</h3>
        {detail.resolution && <p className="pd-description">{detail.resolution}</p>}

        {detail.disputeStatus === 'RESOLVED' ? (
          <div className="pd-resolved-stack">
            <div className="adm-alert adm-alert--info pd-resolution-alert">
              Tranh chấp đã được chốt.
            </div>

            {appealBlockedBySettlement ? (
              <div className="adm-alert adm-alert--error pd-resolution-alert">
                Escrow đã tất toán nên không thể mở lại tự động.
              </div>
            ) : (
              <form className="pd-resolution-form pd-appeal-form" onSubmit={handleAppeal}>
                <label className="pd-field">
                  <span>Nội dung mở lại</span>
                  <textarea
                    className="pd-textarea"
                    rows={4}
                    maxLength={1000}
                    placeholder="Nhập lý do hoặc bằng chứng mới cần xem xét..."
                    value={appealReason}
                    disabled={appealStatus === 'loading'}
                    onChange={(event) => setAppealReason(event.target.value)}
                  />
                </label>

                <label className="pd-field">
                  <span>Bằng chứng bổ sung</span>
                  <textarea
                    className="pd-textarea pd-textarea--compact"
                    rows={3}
                    maxLength={1000}
                    placeholder="URL bằng chứng, mỗi dòng một mục"
                    value={appealEvidenceUrls}
                    disabled={appealStatus === 'loading'}
                    onChange={(event) => setAppealEvidenceUrls(event.target.value)}
                  />
                </label>

                {appealFormError && <div className="adm-alert adm-alert--error">{appealFormError}</div>}
                {appealStatus === 'error' && appealErrorMessage && (
                  <div className="adm-alert adm-alert--error">{appealErrorMessage}</div>
                )}
                {successMessage && <div className="adm-alert adm-alert--success">{successMessage}</div>}

                <div className="pd-resolution-actions">
                  <button
                    className="tcs-btn tcs-btn--primary"
                    type="submit"
                    disabled={appealStatus === 'loading'}
                  >
                    {appealStatus === 'loading' ? 'Đang mở lại...' : 'Mở lại tranh chấp'}
                  </button>
                </div>
              </form>
            )}
          </div>
        ) : (
          <form className="pd-resolution-form" onSubmit={handleResolve}>
            <label className="pd-field">
              <span>Trạng thái sau xử lý</span>
              <select
                className="adm-field"
                value={resolutionStatus}
                disabled={resolveStatus === 'loading'}
                onChange={(event) => setResolutionStatus(event.target.value as ResolutionStatus)}
              >
                {RESOLUTION_STATUS_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label className="pd-field">
              <span>Nội dung quyết định</span>
              <textarea
                className="pd-textarea"
                rows={4}
                maxLength={1000}
                placeholder="Nhập kết luận xử lý, yêu cầu bổ sung hoặc hướng xử lý tiếp theo..."
                value={resolutionNote}
                disabled={resolveStatus === 'loading'}
                onChange={(event) => setResolutionNote(event.target.value)}
              />
            </label>

            {formError && <div className="adm-alert adm-alert--error">{formError}</div>}
            {resolveStatus === 'error' && resolveErrorMessage && (
              <div className="adm-alert adm-alert--error">{resolveErrorMessage}</div>
            )}
            {successMessage && <div className="adm-alert adm-alert--success">{successMessage}</div>}

            <div className="pd-resolution-actions">
              <button
                className="tcs-btn tcs-btn--primary"
                type="submit"
                disabled={resolveStatus === 'loading'}
              >
                {resolveStatus === 'loading' ? 'Đang lưu...' : 'Lưu quyết định'}
              </button>
            </div>
          </form>
        )}
      </section>

      <section className="pd-section">
        <h3 className="pd-section__title">Escrow liên quan</h3>
        <div className="pd-info-grid">
          <InfoRow label="Mã escrow" value={detail.escrow?.escrowId ? `#${detail.escrow.escrowId}` : '—'} />
          <InfoRow label="Trạng thái" value={detail.escrow?.status ?? '—'} />
          <InfoRow label="Số tiền" value={formatCurrency(detail.escrow?.amount)} />
          <InfoRow label="Mã tham chiếu" value={detail.escrow?.paymentReferenceCode} />
          <InfoRow label="Người thanh toán" value={detail.escrow?.payerEmail ?? detail.escrow?.payerUserId} />
          <InfoRow label="Khóa lúc" value={formatDateTime(detail.escrow?.depositedAt)} />
        </div>
      </section>

      <section className="pd-section">
        <h3 className="pd-section__title">Hoàn tiền gần nhất</h3>
        {detail.latestRefundRequest ? (
          <>
            <div className="pd-info-grid">
              <InfoRow label="Mã yêu cầu" value={detail.latestRefundRequest.refundId ? `#${detail.latestRefundRequest.refundId}` : '—'} />
              <div className="pd-info-row">
                <span className="pd-info-row__label">Trạng thái</span>
                <span className="pd-info-row__value">
                  <span className={refundBadgeClass(detail.latestRefundRequest.status)}>
                    {detail.latestRefundRequest.status ?? '—'}
                  </span>
                </span>
              </div>
              <InfoRow label="Số tiền hoàn" value={formatCurrency(detail.latestRefundRequest.amount)} />
              <InfoRow label="Người xử lý" value={detail.latestRefundRequest.requestedByEmail ?? detail.latestRefundRequest.requestedByUserId} />
              <InfoRow label="Yêu cầu lúc" value={formatDateTime(detail.latestRefundRequest.requestedAt)} />
              <InfoRow label="Xử lý lúc" value={formatDateTime(detail.latestRefundRequest.processedAt)} />
            </div>
            <p className="pd-description">{detail.latestRefundRequest.reason ?? '—'}</p>
          </>
        ) : (
          <p className="adm-muted">Chưa có yêu cầu hoàn tiền cho escrow này.</p>
        )}
      </section>

      <section className="pd-section">
        <h3 className="pd-section__title">Lớp học</h3>
        <div className="pd-info-grid">
          <InfoRow label="Mã lớp" value={detail.tutoringClass?.classId ? `#${detail.tutoringClass.classId}` : '—'} />
          <InfoRow label="Trạng thái lớp" value={detail.tutoringClass?.status} />
          <InfoRow label="Chủ lớp" value={detail.tutoringClass?.creatorEmail ?? detail.tutoringClass?.creatorUserId} />
          <InfoRow label="Gia sư" value={detail.tutoringClass?.tutorName ?? detail.tutoringClass?.tutorEmail} />
          <InfoRow label="Assignment" value={detail.tutoringClass?.assignmentId ? `#${detail.tutoringClass.assignmentId}` : '—'} />
          <InfoRow label="Học viên" value={detail.tutoringClass?.studentName} />
        </div>
      </section>

      {detail.terminationRequest && (
        <section className="pd-section">
          <h3 className="pd-section__title">Yêu cầu chấm dứt sớm</h3>
          <div className="pd-info-grid">
            <InfoRow label="Mã yêu cầu" value={`#${detail.terminationRequest.terminationId}`} />
            <InfoRow label="Trạng thái" value={detail.terminationRequest.status} />
            <InfoRow label="Người yêu cầu" value={detail.terminationRequest.requestedByEmail ?? detail.terminationRequest.requestedByUserId} />
            <InfoRow label="Ngày hiệu lực" value={formatDate(detail.terminationRequest.effectiveDate)} />
          </div>
          <p className="pd-description">{detail.terminationRequest.reason ?? '—'}</p>
        </section>
      )}
    </div>
  );
}

export default function PlatformReportsPage() {
  const reports = useReportList();
  const [disputeStatusFilter, setDisputeStatusFilter] = useState<DisputeStatus | undefined>();
  const disputes = useDisputeReviewList(disputeStatusFilter);

  const openReportCount = reports.items.filter((item) => item.status === 'PENDING').length;
  const openDisputeCount = disputes.items.filter((item) => item.status !== 'RESOLVED').length;
  const heldEscrowCount = disputes.items.filter((item) => isEscrowHeldForDispute(item.escrowStatus)).length;

  const selectDispute = (item: DisputeReviewItem) => {
    disputes.selectDispute(item);
  };

  return (
    <AdminLayout
      title="Báo cáo & tranh chấp"
      subtitle="Theo dõi báo cáo vi phạm, tranh chấp lớp học và bằng chứng liên quan."
    >
      <div className="adm-summary-row">
        <article className="adm-summary-card adm-summary-card--warn">
          <p className="adm-summary-card__label">Cần admin can thiệp</p>
          <p className="adm-summary-card__value">{openDisputeCount}</p>
        </article>
        <article className="adm-summary-card">
          <p className="adm-summary-card__label">Escrow đang giữ</p>
          <p className="adm-summary-card__value">{heldEscrowCount}</p>
        </article>
        <article className="adm-summary-card">
          <p className="adm-summary-card__label">Báo cáo đang mở</p>
          <p className="adm-summary-card__value">{openReportCount}</p>
        </article>
      </div>

      <section className="pd-console" aria-label="Danh sách tranh chấp">
        <div className="adm-card pd-console__list">
          <div className="pd-card-head">
            <div>
              <h2 className="pd-card-head__title">Tranh chấp</h2>
              <p className="pd-card-head__meta">{disputes.items.length} hồ sơ</p>
            </div>
            <button className="tcs-btn tcs-btn--ghost" type="button" onClick={disputes.reload}>
              Làm mới
            </button>
          </div>

          <div className="adm-toolbar">
            <select
              className="adm-field"
              value={disputeStatusFilter ?? ''}
              onChange={(event) =>
                setDisputeStatusFilter((event.target.value as DisputeStatus) || undefined)
              }
            >
              <option value="">Tất cả trạng thái</option>
              <option value="OPEN">Mới mở</option>
              <option value="UNDER_INVESTIGATION">Đang xem xét</option>
              <option value="WAITING">Chờ bổ sung</option>
              <option value="RESOLVED">Đã xử lý</option>
            </select>
          </div>

          {disputes.status === 'loading' && <div className="adm-state">Đang tải danh sách tranh chấp…</div>}
          {disputes.status === 'error' && (
            <div className="adm-state">
              <p>{disputes.errorMessage ?? 'Không tải được dữ liệu.'}</p>
              <button className="tcs-btn tcs-btn--primary" type="button" onClick={disputes.reload}>
                Thử lại
              </button>
            </div>
          )}

          {disputes.status === 'success' && (
            <div className="pd-dispute-list">
              {disputes.items.length === 0 ? (
                <div className="adm-state">Chưa có tranh chấp nào.</div>
              ) : (
                disputes.items.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    className={`pd-dispute-item${
                      disputes.selected?.disputeId === item.raw.disputeId ? ' pd-dispute-item--active' : ''
                    }`}
                    onClick={() => selectDispute(item)}
                  >
                    <span className="pd-dispute-item__top">
                      <span className="pd-dispute-item__id">#{item.id}</span>
                      <span className={disputeBadgeClass(item.status)}>{item.statusLabel}</span>
                    </span>
                    <span className="pd-dispute-item__title">{item.classTitle}</span>
                    <span className="pd-dispute-item__desc">{item.description}</span>
                    <span className="pd-dispute-item__meta">
                      {item.amount} · {item.evidenceCount} bằng chứng · {item.createdAt}
                      <span className={escrowBadgeClass(item.escrowStatus)}>{item.escrowStatusLabel}</span>
                    </span>
                  </button>
                ))
              )}
            </div>
          )}
        </div>

        <DisputeDetail
          detail={disputes.selected}
          status={disputes.selectedStatus}
          errorMessage={disputes.detailErrorMessage}
          onChanged={disputes.reload}
        />
      </section>

      <div className="adm-card pd-report-card">
        <div className="pd-card-head">
          <div>
            <h2 className="pd-card-head__title">Báo cáo thường</h2>
            <p className="pd-card-head__meta">{reports.items.length} báo cáo</p>
          </div>
          <button className="tcs-btn tcs-btn--ghost" type="button" onClick={reports.reload}>
            Làm mới
          </button>
        </div>

        {reports.status === 'loading' && (
          <div className="adm-state adm-state--loading">Đang tải danh sách báo cáo…</div>
        )}

        {reports.status === 'error' && (
          <div className="adm-state">
            <p>{reports.errorMessage ?? 'Không tải được dữ liệu.'}</p>
            <button className="tcs-btn tcs-btn--market" type="button" onClick={reports.reload}>
              Thử lại
            </button>
          </div>
        )}

        {reports.status === 'success' && (
          <div className="adm-table-wrap">
            <table className="adm-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Người báo cáo</th>
                  <th>Đối tượng</th>
                  <th>ID đối tượng</th>
                  <th>Danh mục</th>
                  <th>Mô tả</th>
                  <th>Trạng thái</th>
                  <th>Thời gian</th>
                </tr>
              </thead>
              <tbody>
                {reports.items.length === 0 ? (
                  <tr>
                    <td colSpan={8}>Chưa có báo cáo nào.</td>
                  </tr>
                ) : (
                  reports.items.map((item) => (
                    <tr key={item.id}>
                      <td>{item.id}</td>
                      <td>#{item.reporterId}</td>
                      <td>{item.targetTypeLabel}</td>
                      <td>{item.targetId}</td>
                      <td>{item.categoryLabel}</td>
                      <td className="adm-table__notes">{item.description}</td>
                      <td className="adm-table__badge">
                        <span className={reportBadgeClass(item.status)}>{item.statusLabel}</span>
                      </td>
                      <td>{item.createdAt}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </AdminLayout>
  );
}
