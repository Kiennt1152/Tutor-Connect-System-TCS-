import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { FileThumbnail } from '../../../shared/components/FileThumbnail';
import { VerificationHeader } from '../../../shared/components/VerificationHeader';
import { CenterSidebar } from '../components/CenterSidebar';
import { centerApi } from '../api/centerApi';
import { useCenterReportList } from '../hooks/useCenterReportList';
import { platformApi } from '../../platform/api/platformApi';
import { useDisputeReviewList } from '../../platform/hooks/useDisputeReviewList';
import { useRefundRequestList } from '../../platform/hooks/useRefundRequestList';
import {
  BANK_OPTIONS,
  BankPickerDialog,
  BankSelectField,
  type BankOption,
} from '../../finance/components/BankPicker';
import type {
  AdminDisputeReviewApiResponse,
  ClassIssueResolutionAction,
  DisputeResolutionAction,
  DisputeReviewItem,
  DisputeStatus,
  RefundRequestItem,
  RefundRequestStatus,
  ReportItem,
} from '../../platform/types/platformTypes';
import './CenterReportsPage.css';

type CenterConsoleTab = 'reports' | 'disputes' | 'refunds';

const REPORT_ACTION_OPTIONS: { value: ClassIssueResolutionAction; label: string }[] = [
  { value: 'REQUEST_MORE_INFORMATION', label: 'Yêu cầu bổ sung thông tin' },
  { value: 'CONTINUE_CLASS', label: 'Tiếp tục lớp' },
  { value: 'RESCHEDULE', label: 'Dời lịch/bù buổi' },
  { value: 'REPLACE_TUTOR', label: 'Đổi gia sư' },
  { value: 'ESCALATE_TO_DISPUTE', label: 'Chuyển thành tranh chấp' },
  { value: 'TERMINATE_CLASS', label: 'Chuyển xử lý chấm dứt lớp' },
  { value: 'CLOSE_NO_ACTION', label: 'Đóng báo cáo' },
];

const DISPUTE_ACTION_OPTIONS: { value: DisputeResolutionAction; label: string }[] = [
  { value: 'CONTINUE_CLASS', label: 'Tiếp tục lớp' },
  { value: 'APPROVE_FULL_REFUND', label: 'Hoàn tiền toàn phần' },
  { value: 'APPROVE_PARTIAL_REFUND', label: 'Chia tiền/hoàn một phần' },
  { value: 'REJECT_REFUND', label: 'Từ chối hoàn tiền' },
  { value: 'REQUEST_MORE_EVIDENCE', label: 'Yêu cầu bổ sung bằng chứng' },
];

const DISPUTE_STATUS_OPTIONS: { value: '' | DisputeStatus; label: string }[] = [
  { value: '', label: 'Tất cả tranh chấp' },
  { value: 'OPEN', label: 'Mới mở' },
  { value: 'UNDER_INVESTIGATION', label: 'Đang xem xét' },
  { value: 'WAITING', label: 'Chờ bổ sung' },
  { value: 'RESOLVED', label: 'Đã xử lý' },
];

const REFUND_STATUS_OPTIONS: { value: '' | RefundRequestStatus; label: string }[] = [
  { value: '', label: 'Tất cả hoàn tiền' },
  { value: 'PENDING', label: 'Chờ xử lý' },
  { value: 'APPROVED', label: 'Đã duyệt' },
  { value: 'COMPLETED', label: 'Đã hoàn tiền' },
  { value: 'REJECTED', label: 'Từ chối' },
];

function formatCurrency(value: number | null | undefined) {
  if (typeof value !== 'number' || Number.isNaN(value)) return '0 đ';
  return `${new Intl.NumberFormat('vi-VN').format(Math.trunc(value))} đ`;
}

function normalizeMoney(value: string) {
  return value.replace(/[^\d]/g, '');
}

function normalizeAccountNo(value: string) {
  return value.trim().replace(/\s+/g, '');
}

function moneyNumber(value: string) {
  return value ? Number(value) : 0;
}

function hasExistingRefundPayoutInfo(dispute: AdminDisputeReviewApiResponse) {
  const latestRefund = dispute.latestRefundRequest;
  const termination = dispute.terminationRequest;
  const escrow = dispute.escrow;
  return Boolean(
    (latestRefund?.bankName && latestRefund.accountNoMasked && latestRefund.accountHolderName)
      || (termination?.bankName && termination.accountNoMasked && termination.accountHolderName)
      || (escrow?.refundBankName && escrow.refundAccountNoMasked && escrow.refundAccountHolderName),
  );
}

function statusBadgeClass(status: string | null | undefined) {
  if (status === 'RESOLVED' || status === 'COMPLETED' || status === 'RELEASED' || status === 'REFUNDED') {
    return 'tcs-badge tcs-badge--active';
  }
  if (status === 'REJECTED') {
    return 'tcs-badge tcs-badge--banned';
  }
  if (status === 'OPEN' || status === 'PENDING' || status === 'WAITING' || status === 'DISPUTED') {
    return 'tcs-badge tcs-badge--suspended';
  }
  return 'tcs-badge tcs-badge--role';
}

function InfoRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="center-report-info">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function EvidencePreviewList({ urls }: { urls: string[] }) {
  if (urls.length === 0) {
    return <p className="center-report-muted">Chưa có bằng chứng đính kèm.</p>;
  }

  return (
    <div className="center-report-links">
      {urls.map((url, index) => {
        const mimeType = evidenceMimeType(url);
        if (!mimeType) {
          return (
            <a href={url} target="_blank" rel="noreferrer" key={url}>
              {url}
            </a>
          );
        }
        return (
          <FileThumbnail
            key={url}
            src={url}
            fileName={evidenceFileName(url, index)}
            mimeType={mimeType}
            fileSize={null}
          />
        );
      })}
    </div>
  );
}

function evidenceMimeType(url: string) {
  const normalized = url.split(/[?#]/)[0].toLowerCase();
  if (normalized.endsWith('.jpg') || normalized.endsWith('.jpeg')) return 'image/jpeg';
  if (normalized.endsWith('.png')) return 'image/png';
  if (normalized.endsWith('.webp')) return 'image/webp';
  return null;
}

function evidenceFileName(url: string, index: number) {
  const path = url.split(/[?#]/)[0];
  const rawFileName = path.split('/').filter(Boolean).pop();
  if (!rawFileName) return `Bằng chứng ${index + 1}`;
  try {
    return decodeURIComponent(rawFileName);
  } catch {
    return rawFileName;
  }
}

function EmptyState({ children }: { children: ReactNode }) {
  return <div className="center-report-empty">{children}</div>;
}

function ReportDetail({
  report,
  onChanged,
}: {
  report: ReportItem | null;
  onChanged: () => void;
}) {
  const [action, setAction] = useState<ClassIssueResolutionAction>('REQUEST_MORE_INFORMATION');
  const [notes, setNotes] = useState('');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    setAction('REQUEST_MORE_INFORMATION');
    setNotes('');
    setMessage('');
    setError('');
  }, [report?.id]);

  if (!report) {
    return <EmptyState>Chọn một báo cáo để xem chi tiết.</EmptyState>;
  }

  const canResolve = report.status === 'PENDING';

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedNotes = notes.trim();
    if (trimmedNotes.length < 10) {
      setError('Ghi chú xử lý cần ít nhất 10 ký tự.');
      return;
    }

    setBusy(true);
    setError('');
    setMessage('');
    try {
      await centerApi.resolveReport(report.id, { action, notes: trimmedNotes });
      setMessage('Đã lưu xử lý báo cáo.');
      onChanged();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể xử lý báo cáo.'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="center-report-detail">
      <div className="center-report-detail__head">
        <div>
          <p>Báo cáo #{report.id}</p>
          <h2>{report.classTitle}</h2>
        </div>
        <span className={statusBadgeClass(report.status)}>{report.statusLabel}</span>
      </div>

      <div className="center-report-grid">
        <InfoRow label="Người báo cáo" value={report.reporterEmail} />
        <InfoRow label="Lớp" value={`${report.classTitle} (${report.classStatus})`} />
        <InfoRow label="Loại sự cố" value={report.issueTypeLabel} />
        <InfoRow label="Buổi liên quan" value={report.lessonRef} />
        <InfoRow label="Ngày xảy ra" value={report.occurredAt} />
        <InfoRow label="Mong muốn" value={report.requestedActionLabel} />
        <InfoRow label="Tạo lúc" value={report.createdAt} />
        <InfoRow
          label="Tranh chấp"
          value={report.linkedDisputeId ? `#${report.linkedDisputeId}` : 'Chưa chuyển'}
        />
      </div>

      <section className="center-report-section">
        <h3>Mô tả</h3>
        <p className="center-report-description">{report.userDescription}</p>
      </section>

      <section className="center-report-section">
        <h3>Bằng chứng</h3>
        <EvidencePreviewList urls={report.evidenceUrlList} />
      </section>

      <form className="center-report-form" onSubmit={handleSubmit}>
        <label>
          Hướng xử lý
          <select
            className="adm-field"
            value={action}
            disabled={!canResolve || busy}
            onChange={(event) => setAction(event.target.value as ClassIssueResolutionAction)}
          >
            {REPORT_ACTION_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
        <label>
          Ghi chú cho phụ huynh/gia sư
          <textarea
            className="adm-field center-report-textarea"
            value={notes}
            disabled={!canResolve || busy}
            onChange={(event) => setNotes(event.target.value)}
            placeholder="Nhập quyết định xử lý, phương án bù buổi hoặc lý do chuyển tranh chấp..."
          />
        </label>
        {error ? <div className="adm-alert adm-alert--error">{error}</div> : null}
        {message ? <div className="adm-alert adm-alert--success">{message}</div> : null}
        <button className="tcs-btn tcs-btn--market" type="submit" disabled={!canResolve || busy}>
          {busy ? 'Đang lưu...' : 'Lưu xử lý'}
        </button>
      </form>
    </section>
  );
}

function DisputeDetail({
  dispute,
  onChanged,
}: {
  dispute: AdminDisputeReviewApiResponse | null;
  onChanged: () => void;
}) {
  const [action, setAction] = useState<DisputeResolutionAction>('CONTINUE_CLASS');
  const [resolution, setResolution] = useState('');
  const [releaseAmount, setReleaseAmount] = useState('');
  const [refundAmount, setRefundAmount] = useState('');
  const [selectedBankCode, setSelectedBankCode] = useState('');
  const [bankPickerOpen, setBankPickerOpen] = useState(false);
  const [accountNo, setAccountNo] = useState('');
  const [accountHolderName, setAccountHolderName] = useState('');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const escrowAmount = typeof dispute?.escrow?.amount === 'number' ? Math.trunc(dispute.escrow.amount) : 0;
  const releaseNumber = moneyNumber(releaseAmount);
  const refundNumber = moneyNumber(refundAmount);
  const totalSettlement = releaseNumber + refundNumber;
  const financialAction =
    action === 'APPROVE_FULL_REFUND' ||
    action === 'APPROVE_PARTIAL_REFUND';
  const selectedBank = BANK_OPTIONS.find((bank) => bank.code === selectedBankCode);
  const shouldCollectRefundPayout = Boolean(
    dispute
      && financialAction
      && refundNumber > 0
      && !hasExistingRefundPayoutInfo(dispute),
  );

  useEffect(() => {
    const suggestion = dispute?.settlementSuggestion;
    const defaultAction = dispute?.terminationRequest?.status === 'PENDING'
      ? 'APPROVE_PARTIAL_REFUND'
      : 'CONTINUE_CLASS';
    setAction(defaultAction);
    setResolution(dispute?.resolution ?? '');
    if (defaultAction === 'APPROVE_PARTIAL_REFUND' && typeof suggestion?.releaseAmount === 'number') {
      setReleaseAmount(String(Math.trunc(suggestion.releaseAmount)));
      setRefundAmount(String(Math.trunc(suggestion.refundAmount ?? 0)));
    } else {
      setReleaseAmount(escrowAmount > 0 ? String(escrowAmount) : '');
      setRefundAmount('');
    }
    setSelectedBankCode('');
    setBankPickerOpen(false);
    setAccountNo('');
    setAccountHolderName('');
    setMessage('');
    setError('');
  }, [dispute?.disputeId, dispute?.resolution, dispute?.terminationRequest?.status, escrowAmount]);

  if (!dispute) {
    return <EmptyState>Chọn một tranh chấp để xem chi tiết.</EmptyState>;
  }

  const canResolve = dispute.disputeStatus !== 'RESOLVED';

  const applyActionDefaults = (nextAction: DisputeResolutionAction) => {
    setAction(nextAction);
    setError('');
    if (escrowAmount <= 0) return;
    if (nextAction === 'APPROVE_FULL_REFUND') {
      setReleaseAmount('');
      setRefundAmount(String(escrowAmount));
      return;
    }
    if (nextAction === 'APPROVE_PARTIAL_REFUND') {
      const refund = Math.trunc(escrowAmount / 2);
      setReleaseAmount(String(escrowAmount - refund));
      setRefundAmount(String(refund));
      return;
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedResolution = resolution.trim();
    if (trimmedResolution.length < 10) {
      setError('Nội dung quyết định cần ít nhất 10 ký tự.');
      return;
    }
    if (financialAction && escrowAmount > 0 && totalSettlement !== escrowAmount) {
      setError('Tổng giải ngân và hoàn tiền phải bằng tổng escrow.');
      return;
    }
    const normalizedAccountNo = normalizeAccountNo(accountNo);
    if (shouldCollectRefundPayout) {
      if (!selectedBank) {
        setError('Vui lòng chọn ngân hàng nhận hoàn tiền.');
        return;
      }
      if (!/^[A-Za-z0-9]{4,50}$/.test(normalizedAccountNo)) {
        setError('Số tài khoản chỉ gồm chữ/số và dài từ 4 đến 50 ký tự.');
        return;
      }
      if (accountHolderName.trim().length < 2) {
        setError('Vui lòng nhập tên chủ tài khoản nhận hoàn tiền.');
        return;
      }
    }

    setBusy(true);
    setError('');
    setMessage('');
    try {
      await platformApi.resolveDispute(String(dispute.disputeId), {
        action,
        status: action === 'REQUEST_MORE_EVIDENCE' ? 'WAITING' : 'RESOLVED',
        resolution: trimmedResolution,
        releaseToBeneficiary: financialAction ? releaseNumber : undefined,
        refundToPayer: financialAction ? refundNumber : undefined,
        refundPayoutInfo: shouldCollectRefundPayout && selectedBank
          ? {
              bankName: selectedBank.name,
              accountNo: normalizedAccountNo,
              accountHolderName: accountHolderName.trim().replace(/\s+/g, ' '),
            }
          : undefined,
      });
      setMessage('Đã lưu quyết định xử lý tranh chấp.');
      onChanged();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể xử lý tranh chấp.'));
    } finally {
      setBusy(false);
    }
  };

  const handleSelectBank = (bank: BankOption) => {
    setSelectedBankCode(bank.code);
    setBankPickerOpen(false);
  };

  return (
    <section className="center-report-detail">
      <div className="center-report-detail__head">
        <div>
          <p>Tranh chấp #{dispute.disputeId}</p>
          <h2>{dispute.tutoringClass?.title?.trim() || `Lớp #${dispute.targetId ?? '—'}`}</h2>
        </div>
        <span className={statusBadgeClass(dispute.disputeStatus)}>{dispute.disputeStatus}</span>
      </div>

      <div className="center-report-grid">
        <InfoRow label="Người báo cáo" value={dispute.reporterEmail || '—'} />
        <InfoRow label="Mã báo cáo" value={dispute.reportId ? `#${dispute.reportId}` : '—'} />
        <InfoRow label="Escrow" value={dispute.escrow?.escrowId ? `#${dispute.escrow.escrowId}` : '—'} />
        <InfoRow label="Trạng thái escrow" value={dispute.escrow?.status || '—'} />
        <InfoRow label="Tổng escrow" value={formatCurrency(dispute.escrow?.amount)} />
        <InfoRow label="Ngân hàng hoàn tiền đã lưu" value={dispute.escrow?.refundBankName || '—'} />
        <InfoRow label="Tài khoản hoàn tiền đã lưu" value={dispute.escrow?.refundAccountNoMasked || '—'} />
        <InfoRow label="Chủ tài khoản hoàn tiền" value={dispute.escrow?.refundAccountHolderName || '—'} />
        <InfoRow label="Tạo lúc" value={dispute.disputeCreatedAt || '—'} />
      </div>

      {dispute.settlementSuggestion ? (
        <div className="center-report-suggestion">
          Gợi ý theo tiến độ: đã học {dispute.settlementSuggestion.completedSessions ?? 0}/
          {dispute.settlementSuggestion.totalSessions ?? 0} buổi, giải ngân{' '}
          {formatCurrency(dispute.settlementSuggestion.releaseAmount)} và hoàn{' '}
          {formatCurrency(dispute.settlementSuggestion.refundAmount)}.
        </div>
      ) : null}

      <section className="center-report-section">
        <h3>Nội dung tranh chấp</h3>
        <p className="center-report-description">{dispute.description || '—'}</p>
      </section>

      <section className="center-report-section">
        <h3>Bằng chứng</h3>
        <EvidencePreviewList urls={dispute.evidenceUrlList ?? []} />
      </section>

      <form className="center-report-form" onSubmit={handleSubmit}>
        <label>
          Quyết định
          <select
            className="adm-field"
            value={action}
            disabled={!canResolve || busy}
            onChange={(event) => applyActionDefaults(event.target.value as DisputeResolutionAction)}
          >
            {DISPUTE_ACTION_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        {financialAction ? (
          <div className="center-report-money-grid">
            <div className="center-report-total">
              Tổng escrow: <strong>{formatCurrency(escrowAmount)}</strong>
            </div>
            <label>
              Giải ngân cho gia sư/trung tâm
              <input
                className="adm-field"
                inputMode="numeric"
                value={releaseAmount}
                disabled={!canResolve || busy}
                onChange={(event) => setReleaseAmount(normalizeMoney(event.target.value))}
              />
            </label>
            <label>
              Hoàn lại người thanh toán
              <input
                className="adm-field"
                inputMode="numeric"
                value={refundAmount}
                disabled={!canResolve || busy}
                onChange={(event) => setRefundAmount(normalizeMoney(event.target.value))}
              />
            </label>
            <div className={totalSettlement === escrowAmount ? 'center-report-total center-report-total--ok' : 'center-report-total'}>
              Còn lệch: <strong>{formatCurrency(escrowAmount - totalSettlement)}</strong>
            </div>
          </div>
        ) : null}

        {shouldCollectRefundPayout ? (
          <div className="center-report-payout">
            <p className="center-report-payout__title">Tài khoản nhận hoàn tiền</p>
            <label>
              Ngân hàng
              <BankSelectField
                id={`center-dispute-refund-bank-${dispute.disputeId}`}
                selectedBank={selectedBank}
                onOpen={() => setBankPickerOpen(true)}
              />
            </label>
            <div className="center-report-money-grid">
              <label>
                Số tài khoản
                <input
                  className="adm-field"
                  value={accountNo}
                  disabled={!canResolve || busy}
                  onChange={(event) => setAccountNo(event.target.value)}
                />
              </label>
              <label>
                Tên chủ tài khoản
                <input
                  className="adm-field"
                  value={accountHolderName}
                  disabled={!canResolve || busy}
                  onChange={(event) => setAccountHolderName(event.target.value)}
                />
              </label>
            </div>
          </div>
        ) : null}

        <label>
          Nội dung quyết định
          <textarea
            className="adm-field center-report-textarea"
            value={resolution}
            disabled={!canResolve || busy}
            onChange={(event) => setResolution(event.target.value)}
            placeholder="Nhập kết luận xử lý, lý do chia tiền hoặc yêu cầu bổ sung bằng chứng..."
          />
        </label>
        {error ? <div className="adm-alert adm-alert--error">{error}</div> : null}
        {message ? <div className="adm-alert adm-alert--success">{message}</div> : null}
        <button className="tcs-btn tcs-btn--market" type="submit" disabled={!canResolve || busy}>
          {busy ? 'Đang lưu...' : 'Lưu quyết định'}
        </button>
      </form>
      <BankPickerDialog
        open={bankPickerOpen}
        selectedBankCode={selectedBankCode}
        onSelect={handleSelectBank}
        onClose={() => setBankPickerOpen(false)}
      />
    </section>
  );
}

function RefundDetail({
  refund,
  onChanged,
}: {
  refund: RefundRequestItem | null;
  onChanged: () => void;
}) {
  const [amount, setAmount] = useState('');
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    setAmount(refund ? String(Math.trunc(refund.rawAmount)) : '');
    setReason('');
    setMessage('');
    setError('');
  }, [refund?.id, refund?.rawAmount]);

  if (!refund) {
    return <EmptyState>Chọn một yêu cầu hoàn tiền để xem chi tiết.</EmptyState>;
  }

  const canDecide = refund.canDecide;

  const decide = async (decision: 'approve' | 'reject') => {
    const trimmedReason = reason.trim();
    if (trimmedReason.length < 10) {
      setError('Lý do xử lý cần ít nhất 10 ký tự.');
      return;
    }
    const approvedAmount = moneyNumber(amount);
    if (decision === 'approve' && approvedAmount <= 0) {
      setError('Số tiền duyệt hoàn phải lớn hơn 0.');
      return;
    }

    setBusy(true);
    setError('');
    setMessage('');
    try {
      if (decision === 'approve') {
        await platformApi.approveRefundRequest(refund.id, {
          approvedAmount,
          reason: trimmedReason,
        });
        setMessage('Đã duyệt hoàn tiền. Chờ webhook SePay tiền ra xác nhận chuyển khoản.');
      } else {
        await platformApi.rejectRefundRequest(refund.id, { reason: trimmedReason });
        setMessage('Đã từ chối yêu cầu hoàn tiền.');
      }
      onChanged();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể xử lý yêu cầu hoàn tiền.'));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="center-report-detail">
      <div className="center-report-detail__head">
        <div>
          <p>Yêu cầu hoàn tiền #{refund.id}</p>
          <h2>{refund.classTitle}</h2>
        </div>
        <span className={statusBadgeClass(refund.status)}>{refund.statusLabel}</span>
      </div>

      <div className="center-report-grid">
        <InfoRow label="Người yêu cầu" value={refund.requester} />
        <InfoRow label="Escrow" value={`#${refund.escrowId}`} />
        <InfoRow label="Số tiền yêu cầu" value={refund.amount} />
        <InfoRow label="Tổng escrow" value={refund.escrowAmount} />
        <InfoRow label="Ngân hàng" value={refund.bankName} />
        <InfoRow label="Số tài khoản" value={refund.accountNoMasked} />
        <InfoRow label="Tên chủ tài khoản" value={refund.accountHolderName} />
        <InfoRow label="Mã chuyển khoản" value={refund.refundReferenceCode} />
        <InfoRow label="Trạng thái chuyển khoản" value={refund.transferStatus} />
      </div>

      <section className="center-report-section">
        <h3>Lý do người dùng</h3>
        <p className="center-report-description">{refund.reason}</p>
      </section>

      <form className="center-report-form" onSubmit={(event) => event.preventDefault()}>
        <label>
          Số tiền duyệt hoàn
          <input
            className="adm-field"
            inputMode="numeric"
            value={amount}
            disabled={!canDecide || busy}
            onChange={(event) => setAmount(normalizeMoney(event.target.value))}
          />
        </label>
        <label>
          Lý do xử lý
          <textarea
            className="adm-field center-report-textarea"
            value={reason}
            disabled={!canDecide || busy}
            onChange={(event) => setReason(event.target.value)}
            placeholder="Nhập lý do duyệt hoặc từ chối hoàn tiền..."
          />
        </label>
        {error ? <div className="adm-alert adm-alert--error">{error}</div> : null}
        {message ? <div className="adm-alert adm-alert--success">{message}</div> : null}
        <div className="center-report-actions">
          <button
            className="tcs-btn tcs-btn--market"
            type="button"
            disabled={!canDecide || busy}
            onClick={() => decide('approve')}
          >
            Duyệt hoàn tiền
          </button>
          <button
            className="tcs-btn tcs-btn--danger"
            type="button"
            disabled={!canDecide || busy}
            onClick={() => decide('reject')}
          >
            Từ chối
          </button>
        </div>
      </form>
    </section>
  );
}

function ReportList({
  items,
  selectedId,
  onSelect,
}: {
  items: ReportItem[];
  selectedId: string | null;
  onSelect: (id: string) => void;
}) {
  if (items.length === 0) {
    return <EmptyState>Chưa có báo cáo sự cố nào của lớp trung tâm.</EmptyState>;
  }
  return (
    <div className="center-report-list">
      {items.map((item) => (
        <button
          type="button"
          className={`center-report-list-item${item.id === selectedId ? ' center-report-list-item--active' : ''}`}
          key={item.id}
          onClick={() => onSelect(item.id)}
        >
          <span className="center-report-list-item__top">
            <strong>#{item.id}</strong>
            <span className={statusBadgeClass(item.status)}>{item.statusLabel}</span>
          </span>
          <span className="center-report-list-item__title">{item.classTitle}</span>
          <span className="center-report-list-item__meta">
            {item.issueTypeLabel} · {item.createdAt}
          </span>
        </button>
      ))}
    </div>
  );
}

function DisputeList({
  items,
  selectedId,
  onSelect,
}: {
  items: DisputeReviewItem[];
  selectedId: string | null;
  onSelect: (item: DisputeReviewItem) => void;
}) {
  if (items.length === 0) {
    return <EmptyState>Chưa có tranh chấp nào của lớp trung tâm.</EmptyState>;
  }
  return (
    <div className="center-report-list">
      {items.map((item) => (
        <button
          type="button"
          className={`center-report-list-item${item.id === selectedId ? ' center-report-list-item--active' : ''}`}
          key={item.id}
          onClick={() => onSelect(item)}
        >
          <span className="center-report-list-item__top">
            <strong>#{item.id}</strong>
            <span className={statusBadgeClass(item.status)}>{item.statusLabel}</span>
          </span>
          <span className="center-report-list-item__title">{item.classTitle}</span>
          <span className="center-report-list-item__meta">
            {item.amount} · {item.createdAt}
          </span>
        </button>
      ))}
    </div>
  );
}

function RefundList({
  items,
  selectedId,
  onSelect,
}: {
  items: RefundRequestItem[];
  selectedId: string | null;
  onSelect: (id: string) => void;
}) {
  if (items.length === 0) {
    return <EmptyState>Chưa có yêu cầu hoàn tiền nào của lớp trung tâm.</EmptyState>;
  }
  return (
    <div className="center-report-list">
      {items.map((item) => (
        <button
          type="button"
          className={`center-report-list-item${item.id === selectedId ? ' center-report-list-item--active' : ''}`}
          key={item.id}
          onClick={() => onSelect(item.id)}
        >
          <span className="center-report-list-item__top">
            <strong>#{item.id}</strong>
            <span className={statusBadgeClass(item.status)}>{item.statusLabel}</span>
          </span>
          <span className="center-report-list-item__title">{item.classTitle}</span>
          <span className="center-report-list-item__meta">
            {item.amount} · {item.requestedAt}
          </span>
        </button>
      ))}
    </div>
  );
}

export default function CenterReportsPage() {
  const [activeTab, setActiveTab] = useState<CenterConsoleTab>('reports');
  const [selectedReportId, setSelectedReportId] = useState<string | null>(null);
  const [selectedRefundId, setSelectedRefundId] = useState<string | null>(null);
  const [disputeStatus, setDisputeStatus] = useState<DisputeStatus | undefined>();
  const [refundStatus, setRefundStatus] = useState<RefundRequestStatus | undefined>();

  const reports = useCenterReportList();
  const disputes = useDisputeReviewList(disputeStatus);
  const refunds = useRefundRequestList(refundStatus);

  useEffect(() => {
    if (reports.items.length === 0) {
      setSelectedReportId(null);
      return;
    }
    if (!selectedReportId || !reports.items.some((item) => item.id === selectedReportId)) {
      setSelectedReportId(reports.items[0].id);
    }
  }, [reports.items, selectedReportId]);

  useEffect(() => {
    if (refunds.items.length === 0) {
      setSelectedRefundId(null);
      return;
    }
    if (!selectedRefundId || !refunds.items.some((item) => item.id === selectedRefundId)) {
      setSelectedRefundId(refunds.items[0].id);
    }
  }, [refunds.items, selectedRefundId]);

  const selectedReport = useMemo(
    () => reports.items.find((item) => item.id === selectedReportId) ?? null,
    [reports.items, selectedReportId],
  );
  const selectedRefund = useMemo(
    () => refunds.items.find((item) => item.id === selectedRefundId) ?? null,
    [refunds.items, selectedRefundId],
  );

  const pendingReports = reports.items.filter((item) => item.status === 'PENDING').length;
  const openDisputes = disputes.items.filter((item) => item.status !== 'RESOLVED').length;
  const pendingRefunds = refunds.items.filter((item) =>
    item.status === 'PENDING'
      || (item.status === 'APPROVED' && item.raw.transferStatus === 'PENDING')
  ).length;

  const reloadAll = () => {
    reports.reload();
    disputes.reload();
    refunds.reload();
  };

  return (
    <>
      <VerificationHeader />
      <div className="cc-area-bg">
      <div className="cc-shell">
      <CenterSidebar />
      <div className="cc-shell__main">
      <main className="center-report-page">
        <header className="center-report-header">
          <div>
            <h1>Báo cáo & tranh chấp trung tâm</h1>
            <p>
              Theo dõi sự cố, tranh chấp và yêu cầu hoàn tiền của các lớp trung tâm do bạn quản lý.
            </p>
          </div>
          <button className="tcs-btn tcs-btn--ghost" type="button" onClick={reloadAll}>
            Làm mới
          </button>
        </header>

        <section className="center-report-summary">
          <button
            type="button"
            className={`center-report-summary-card${activeTab === 'reports' ? ' center-report-summary-card--active' : ''}`}
            onClick={() => setActiveTab('reports')}
          >
            <span>Báo cáo đang mở</span>
            <strong>{pendingReports}</strong>
          </button>
          <button
            type="button"
            className={`center-report-summary-card${activeTab === 'disputes' ? ' center-report-summary-card--active' : ''}`}
            onClick={() => setActiveTab('disputes')}
          >
            <span>Tranh chấp cần xử lý</span>
            <strong>{openDisputes}</strong>
          </button>
          <button
            type="button"
            className={`center-report-summary-card${activeTab === 'refunds' ? ' center-report-summary-card--active' : ''}`}
            onClick={() => setActiveTab('refunds')}
          >
            <span>Hoàn tiền chờ xử lý/chuyển</span>
            <strong>{pendingRefunds}</strong>
          </button>
        </section>

        {activeTab === 'reports' ? (
          <section className="center-report-console">
            <div className="center-report-card">
              <div className="center-report-card__head">
                <h2>Báo cáo sự cố</h2>
                <span>{reports.items.length} báo cáo</span>
              </div>
              {reports.status === 'loading' ? <EmptyState>Đang tải báo cáo...</EmptyState> : null}
              {reports.errorMessage ? (
                <div className="adm-alert adm-alert--error">{reports.errorMessage}</div>
              ) : null}
              {reports.status === 'success' ? (
                <ReportList
                  items={reports.items}
                  selectedId={selectedReportId}
                  onSelect={setSelectedReportId}
                />
              ) : null}
            </div>
            <ReportDetail
              report={selectedReport}
              onChanged={() => {
                reports.reload();
                disputes.reload();
              }}
            />
          </section>
        ) : null}

        {activeTab === 'disputes' ? (
          <section className="center-report-console">
            <div className="center-report-card">
              <div className="center-report-card__head">
                <h2>Tranh chấp</h2>
                <select
                  className="adm-field center-report-filter"
                  value={disputeStatus ?? ''}
                  onChange={(event) =>
                    setDisputeStatus(event.target.value ? event.target.value as DisputeStatus : undefined)
                  }
                >
                  {DISPUTE_STATUS_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
              {disputes.status === 'loading' ? <EmptyState>Đang tải tranh chấp...</EmptyState> : null}
              {disputes.errorMessage ? (
                <div className="adm-alert adm-alert--error">{disputes.errorMessage}</div>
              ) : null}
              {disputes.status === 'success' ? (
                <DisputeList
                  items={disputes.items}
                  selectedId={disputes.selected?.disputeId ? String(disputes.selected.disputeId) : null}
                  onSelect={disputes.selectDispute}
                />
              ) : null}
            </div>
            {disputes.selectedStatus === 'loading' ? (
              <EmptyState>Đang tải chi tiết tranh chấp...</EmptyState>
            ) : disputes.detailErrorMessage ? (
              <div className="center-report-detail">
                <div className="adm-alert adm-alert--error">{disputes.detailErrorMessage}</div>
              </div>
            ) : (
              <DisputeDetail
                dispute={disputes.selected}
                onChanged={() => {
                  disputes.reload();
                  refunds.reload();
                  reports.reload();
                }}
              />
            )}
          </section>
        ) : null}

        {activeTab === 'refunds' ? (
          <section className="center-report-console">
            <div className="center-report-card">
              <div className="center-report-card__head">
                <h2>Yêu cầu hoàn tiền</h2>
                <select
                  className="adm-field center-report-filter"
                  value={refundStatus ?? ''}
                  onChange={(event) =>
                    setRefundStatus(event.target.value ? event.target.value as RefundRequestStatus : undefined)
                  }
                >
                  {REFUND_STATUS_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </div>
              {refunds.status === 'loading' ? <EmptyState>Đang tải yêu cầu hoàn tiền...</EmptyState> : null}
              {refunds.errorMessage ? (
                <div className="adm-alert adm-alert--error">{refunds.errorMessage}</div>
              ) : null}
              {refunds.status === 'success' ? (
                <RefundList
                  items={refunds.items}
                  selectedId={selectedRefundId}
                  onSelect={setSelectedRefundId}
                />
              ) : null}
            </div>
            <RefundDetail
              refund={selectedRefund}
              onChanged={() => {
                refunds.reload();
                disputes.reload();
              }}
            />
          </section>
        ) : null}
      </main>
      </div>
      </div>
      </div>
    </>
  );
}
