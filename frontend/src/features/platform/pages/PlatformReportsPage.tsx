import { useEffect, useState, type ChangeEvent, type FormEvent, type ReactNode } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { FileThumbnail } from '../../../shared/components/FileThumbnail';
import { disputeApi } from '../../dispute/api/disputeApi';
import type { EvidenceUploadResponse } from '../../dispute/types/disputeTypes';
import {
  BANK_OPTIONS,
  BankPickerDialog,
  BankSelectField,
  type BankOption,
} from '../../finance/components/BankPicker';
import { AdminLayout } from '../components/AdminLayout';
import { platformApi } from '../api/platformApi';
import { useDisputeReviewList } from '../hooks/useDisputeReviewList';
import {
  useAppealDispute,
  useResolveDispute,
} from '../hooks/usePlatformMutations';
import { useRefundRequestList } from '../hooks/useRefundRequestList';
import { useReportList } from '../hooks/useReportList';
import type {
  AdminDisputeReviewApiResponse,
  ClassIssueResolutionAction,
  DisputeReviewItem,
  DisputeResolutionAction,
  DisputeStatus,
  EscrowStatus,
  RefundRequestItem,
  RefundRequestStatus,
  ReportItem,
  ReportStatus,
  ReviewModerationStatus,
  ReviewReportAction,
} from '../types/platformTypes';
import './PlatformReportsPage.css';

const RESOLUTION_ACTION_OPTIONS: { value: DisputeResolutionAction; label: string }[] = [
  { value: 'CONTINUE_CLASS', label: 'Tiếp tục lớp' },
  { value: 'TERMINATE_CLASS', label: 'Chấm dứt lớp và tất toán' },
  { value: 'APPROVE_FULL_REFUND', label: 'Hoàn tiền toàn phần' },
  { value: 'APPROVE_PARTIAL_REFUND', label: 'Hoàn tiền một phần' },
  { value: 'REJECT_REFUND', label: 'Từ chối hoàn tiền' },
  { value: 'CLOSE_MUTUAL_AGREEMENT', label: 'Đóng theo thỏa thuận' },
  { value: 'REQUEST_MORE_EVIDENCE', label: 'Yêu cầu bổ sung bằng chứng' },
];

const CLASS_ISSUE_ACTION_OPTIONS: { value: ClassIssueResolutionAction; label: string }[] = [
  { value: 'REQUEST_MORE_INFORMATION', label: 'Yêu cầu bổ sung thông tin' },
  { value: 'CONTINUE_CLASS', label: 'Tiếp tục lớp' },
  { value: 'RESCHEDULE', label: 'Dời lịch/bù buổi' },
  { value: 'REPLACE_TUTOR', label: 'Đổi gia sư' },
  { value: 'ESCALATE_TO_DISPUTE', label: 'Chuyển thành tranh chấp' },
  { value: 'TERMINATE_CLASS', label: 'Chuyển xử lý chấm dứt lớp' },
  { value: 'CLOSE_NO_ACTION', label: 'Đóng báo cáo' },
];

const REVIEW_REPORT_ACTION_OPTIONS: { value: ReviewReportAction; label: string }[] = [
  { value: 'HIDE_REVIEW', label: 'Ẩn đánh giá khỏi hồ sơ gia sư' },
  { value: 'MARK_VIOLATION', label: 'Đánh dấu vi phạm' },
  { value: 'DELETE_REVIEW', label: 'Xóa đánh giá vĩnh viễn' },
  { value: 'KEEP_REVIEW', label: 'Giữ nguyên đánh giá (báo cáo không hợp lệ)' },
];

const REVIEW_MODERATION_LABELS: Record<ReviewModerationStatus, string> = {
  VISIBLE: 'Đang hiển thị',
  HIDDEN: 'Đã ẩn',
  MODERATED: 'Vi phạm',
};

const MAX_EVIDENCE_FILES = 5;
const MAX_EVIDENCE_SIZE = 10 * 1024 * 1024;
const EVIDENCE_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);

type SettlementPolicyPreset =
  | 'CUSTOM'
  | 'TUTOR_FAULT'
  | 'FORCE_MAJEURE'
  | 'CANCEL_BEFORE_START'
  | 'FIRST_SESSION_24H'
  | 'TRIAL_NOT_FIT'
  | 'NOTICE_7D'
  | 'NOTICE_3_6D'
  | 'LATE_NO_SHOW'
  | 'CENTER_CANCEL_7D'
  | 'CENTER_CANCEL_3_6D'
  | 'CENTER_CANCEL_1_2D'
  | 'CENTER_PROGRESS_UNDER_30'
  | 'CENTER_PROGRESS_30_70'
  | 'CENTER_PROGRESS_OVER_70';

const POLICY_PRESET_GROUPS: {
  label: string;
  options: { value: Exclude<SettlementPolicyPreset, 'CUSTOM'>; label: string }[];
}[] = [
  {
    label: 'Lớp 1:1 / gia sư',
    options: [
      { value: 'TUTOR_FAULT', label: 'Lỗi TCS/gia sư - hoàn phần chưa học' },
      { value: 'FORCE_MAJEURE', label: 'Bất khả kháng - không phạt' },
      { value: 'CANCEL_BEFORE_START', label: 'Hủy trước buổi đầu - hoàn 100%' },
      { value: 'FIRST_SESSION_24H', label: 'Hủy buổi đầu <24h - tính 50% buổi đầu' },
      { value: 'TRIAL_NOT_FIT', label: 'Dừng trong giai đoạn thử - 2 buổi đầu' },
      { value: 'NOTICE_7D', label: 'Dừng báo trước >=7 ngày - theo buổi học' },
      { value: 'NOTICE_3_6D', label: 'Dừng báo trước 3-6 ngày - cộng 50% một buổi' },
      { value: 'LATE_NO_SHOW', label: 'Dừng <72h/no-show - cộng tối đa 1 buổi' },
    ],
  },
  {
    label: 'Lớp nhóm / khóa cố định',
    options: [
      { value: 'CENTER_CANCEL_7D', label: 'Hủy >=7 ngày trước khai giảng - hoàn 90%' },
      { value: 'CENTER_CANCEL_3_6D', label: 'Hủy 3-6 ngày trước khai giảng - hoàn 70%' },
      { value: 'CENTER_CANCEL_1_2D', label: 'Hủy 1-2 ngày trước khai giảng - hoàn 50%' },
      { value: 'CENTER_PROGRESS_UNDER_30', label: 'Đã học <=30% khóa - hoàn 70% phần chưa dùng' },
      { value: 'CENTER_PROGRESS_30_70', label: 'Đã học 30-70% khóa - hoàn 50% phần chưa dùng' },
      { value: 'CENTER_PROGRESS_OVER_70', label: 'Đã học >70% khóa - không hoàn tiền mặt' },
    ],
  },
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

const normalizeAccountNo = (value: string) => value.trim().replace(/\s+/g, '');

function hasExistingRefundPayoutInfo(detail: AdminDisputeReviewApiResponse) {
  const latestRefund = detail.latestRefundRequest;
  const termination = detail.terminationRequest;
  return Boolean(
    (latestRefund?.bankName && latestRefund.accountNoMasked && latestRefund.accountHolderName)
      || (termination?.bankName && termination.accountNoMasked && termination.accountHolderName),
  );
}

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

const clampCurrency = (value: number, min: number, max: number) => Math.min(max, Math.max(min, Math.trunc(value)));

const getPolicyPresetLabel = (preset: SettlementPolicyPreset) => {
  if (preset === 'CUSTOM') return 'Tự nhập số tiền';
  return POLICY_PRESET_GROUPS
    .flatMap((group) => group.options)
    .find((option) => option.value === preset)?.label ?? 'Chính sách tùy chỉnh';
};

const policyLineHasPrefix = (line: string) => {
  const trimmed = line.trim();
  return trimmed.startsWith('Căn cứ xử lý:') || trimmed.startsWith('Tính tiền:');
};

const stripPolicyLines = (value: string) => value
  .split(/\r?\n/)
  .filter((line) => !policyLineHasPrefix(line))
  .join('\n')
  .trim();

const withPeriod = (value: string) => (/[.!?]$/.test(value.trim()) ? value.trim() : `${value.trim()}.`);

const composePolicyResolution = (current: string, policyLabel: string, calculationNote: string) => {
  const existingNote = stripPolicyLines(current);
  return [
    withPeriod(`Căn cứ xử lý: ${policyLabel}`),
    withPeriod(`Tính tiền: ${calculationNote}`),
    existingNote,
  ].filter(Boolean).join('\n');
};

type SettlementPolicyContext = {
  totalSessions: number | null;
  completedSessions: number | null;
  oneSessionAmount: number | null;
  proRataReleaseAmount: number | null;
  proRataRefundAmount: number | null;
};

type SettlementPolicyResult =
  | { release: number; refund: number; note: string; warning?: string }
  | { error: string };

const getSettlementPolicyContext = (
  escrowAmount: number,
  suggestion: AdminDisputeReviewApiResponse['settlementSuggestion'],
): SettlementPolicyContext => {
  const totalSessions = Math.trunc(suggestion?.totalSessions ?? 0);
  const completedSessions = Math.trunc(suggestion?.completedSessions ?? 0);
  const hasSessionData = escrowAmount > 0 && totalSessions > 0;
  const normalizedCompletedSessions = hasSessionData
    ? Math.min(Math.max(completedSessions, 0), totalSessions)
    : null;
  const oneSessionAmount = hasSessionData ? Math.trunc(escrowAmount / totalSessions) : null;
  const suggestedReleaseAmount = typeof suggestion?.releaseAmount === 'number'
    ? Math.trunc(suggestion.releaseAmount)
    : null;
  const proRataReleaseAmount = hasSessionData
    ? clampCurrency(suggestedReleaseAmount ?? (escrowAmount * (normalizedCompletedSessions ?? 0)) / totalSessions, 0, escrowAmount)
    : null;

  return {
    totalSessions: hasSessionData ? totalSessions : null,
    completedSessions: normalizedCompletedSessions,
    oneSessionAmount,
    proRataReleaseAmount,
    proRataRefundAmount: proRataReleaseAmount == null ? null : escrowAmount - proRataReleaseAmount,
  };
};

const calculateSettlementPolicy = (
  preset: SettlementPolicyPreset,
  escrowAmount: number,
  context: SettlementPolicyContext,
): SettlementPolicyResult => {
  if (escrowAmount <= 0) {
    return { error: 'Escrow chưa có số tiền hợp lệ.' };
  }

  const splitByRelease = (release: number) => {
    const normalizedRelease = clampCurrency(release, 0, escrowAmount);
    return {
      release: normalizedRelease,
      refund: escrowAmount - normalizedRelease,
    };
  };
  const splitByRefundRate = (rate: number) => {
    const refund = clampCurrency(escrowAmount * rate, 0, escrowAmount);
    return {
      release: escrowAmount - refund,
      refund,
    };
  };
  const requireSessionData = () => {
    if (
      context.totalSessions == null ||
      context.completedSessions == null ||
      context.oneSessionAmount == null ||
      context.proRataReleaseAmount == null
    ) {
      return 'Chưa đủ dữ liệu số buổi học để tính chính sách này.';
    }
    return null;
  };

  if (preset === 'CUSTOM') {
    return { error: 'Vui lòng chọn một căn cứ xử lý cụ thể.' };
  }

  if (preset === 'CANCEL_BEFORE_START') {
    return {
      release: 0,
      refund: escrowAmount,
      note: `hủy trước buổi đầu nên hoàn 100% ${formatCurrency(escrowAmount)}`,
      warning: (context.completedSessions ?? 0) > 0 ? 'Hồ sơ đang có buổi đã học, cần kiểm tra trước khi hoàn toàn phần.' : undefined,
    };
  }

  if (preset === 'CENTER_CANCEL_7D') {
    const split = splitByRefundRate(0.9);
    return {
      ...split,
      note: `hủy >=7 ngày trước khai giảng, hoàn 90% là ${formatCurrency(split.refund)}`,
    };
  }

  if (preset === 'CENTER_CANCEL_3_6D') {
    const split = splitByRefundRate(0.7);
    return {
      ...split,
      note: `hủy 3-6 ngày trước khai giảng, hoàn 70% là ${formatCurrency(split.refund)}`,
    };
  }

  if (preset === 'CENTER_CANCEL_1_2D') {
    const split = splitByRefundRate(0.5);
    return {
      ...split,
      note: `hủy 1-2 ngày trước khai giảng, hoàn 50% là ${formatCurrency(split.refund)}`,
    };
  }

  if (preset === 'CENTER_PROGRESS_OVER_70') {
    return {
      release: escrowAmount,
      refund: 0,
      note: `khóa đã học trên 70%, không hoàn tiền mặt; giải ngân ${formatCurrency(escrowAmount)}`,
    };
  }

  const sessionDataError = requireSessionData();
  if (sessionDataError) return { error: sessionDataError };

  const completed = context.completedSessions ?? 0;
  const total = context.totalSessions ?? 0;
  const sessionAmount = context.oneSessionAmount ?? 0;
  const proRataRelease = context.proRataReleaseAmount ?? 0;
  const unusedAmount = escrowAmount - proRataRelease;

  if (preset === 'TUTOR_FAULT') {
    const split = splitByRelease(proRataRelease);
    return {
      ...split,
      note: `lỗi từ TCS/gia sư, tính ${completed}/${total} buổi hợp lệ và hoàn phần chưa học ${formatCurrency(split.refund)}`,
    };
  }

  if (preset === 'FORCE_MAJEURE') {
    const split = splitByRelease(proRataRelease);
    return {
      ...split,
      note: `bất khả kháng có chứng cứ, tính ${completed}/${total} buổi đã học và không phạt`,
    };
  }

  if (preset === 'FIRST_SESSION_24H') {
    const firstSessionFee = Math.trunc(sessionAmount * 0.5);
    const split = splitByRelease(firstSessionFee);
    return {
      ...split,
      note: `hủy buổi đầu trong 24h, tính 50% một buổi là ${formatCurrency(firstSessionFee)}`,
    };
  }

  if (preset === 'TRIAL_NOT_FIT') {
    const split = splitByRelease(proRataRelease);
    return {
      ...split,
      note: `dừng trong giai đoạn thử 2 buổi đầu, tính ${completed}/${total} buổi đã học và hoàn phần còn lại`,
      warning: completed > 2 ? 'Giai đoạn thử chỉ áp dụng cho 2 buổi đầu; hồ sơ này đang ghi nhận nhiều hơn 2 buổi.' : undefined,
    };
  }

  if (preset === 'NOTICE_7D') {
    const split = splitByRelease(proRataRelease);
    return {
      ...split,
      note: `dừng báo trước >=7 ngày, tính ${completed}/${total} buổi đã học và không phạt`,
    };
  }

  if (preset === 'NOTICE_3_6D') {
    const settlementFee = Math.trunc(sessionAmount * 0.5);
    const split = splitByRelease(proRataRelease + settlementFee);
    return {
      ...split,
      note: `dừng báo trước 3-6 ngày, tính ${completed}/${total} buổi và phí giữ lịch 50% một buổi ${formatCurrency(settlementFee)}`,
    };
  }

  if (preset === 'LATE_NO_SHOW') {
    const split = splitByRelease(proRataRelease + sessionAmount);
    return {
      ...split,
      note: `dừng <72h/no-show, tính ${completed}/${total} buổi và phí giữ lịch tối đa 1 buổi ${formatCurrency(sessionAmount)}`,
    };
  }

  if (preset === 'CENTER_PROGRESS_UNDER_30') {
    const refund = clampCurrency(unusedAmount * 0.7, 0, escrowAmount);
    return {
      release: escrowAmount - refund,
      refund,
      note: `khóa đã học <=30%, hoàn 70% phần chưa dùng ${formatCurrency(refund)}`,
    };
  }

  if (preset === 'CENTER_PROGRESS_30_70') {
    const refund = clampCurrency(unusedAmount * 0.5, 0, escrowAmount);
    return {
      release: escrowAmount - refund,
      refund,
      note: `khóa đã học 30-70%, hoàn 50% phần chưa dùng ${formatCurrency(refund)}`,
    };
  }

  return { error: 'Chính sách này chưa được hỗ trợ.' };
};

function formatAuditPayload(value: string | null | undefined) {
  if (!value) return '—';
  try {
    const parsed = JSON.parse(value) as Record<string, unknown>;
    return Object.entries(parsed)
      .map(([key, rawValue]) => {
        if (rawValue == null) return `${key}: —`;
        if (typeof rawValue === 'number' && /amount|release|refund/i.test(key)) {
          return `${key}: ${formatCurrency(rawValue)}`;
        }
        return `${key}: ${String(rawValue)}`;
      })
      .join(' · ');
  } catch {
    return value;
  }
}

function InfoRow({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="pd-info-row">
      <span className="pd-info-row__label">{label}</span>
      <span className="pd-info-row__value">{value ?? '—'}</span>
    </div>
  );
}

function EvidencePreviewList({
  urls,
  emptyText,
}: {
  urls: string[];
  emptyText: string;
}) {
  if (urls.length === 0) {
    return <p className="pd-muted">{emptyText}</p>;
  }

  return (
    <div className="pd-evidence-list">
      {urls.map((url, index) => {
        const mimeType = evidenceMimeType(url);
        if (!mimeType) {
          return (
            <a key={url} className="pd-evidence-link" href={url} target="_blank" rel="noreferrer">
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

function buildUploadedEvidenceUrls(files: EvidenceUploadResponse[]) {
  return files.map((file) => file.fileUrl).join('\n');
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

function ResolutionDecisionPanel({
  detail,
  onChanged,
}: {
  detail: AdminDisputeReviewApiResponse;
  onChanged: () => void;
}) {
  const {
    status: resolveStatus,
    errorMessage: resolveErrorMessage,
    resolveDispute,
    reset: resetResolve,
  } = useResolveDispute();
  const [action, setAction] = useState<DisputeResolutionAction>('CONTINUE_CLASS');
  const [resolutionNote, setResolutionNote] = useState('');
  const [releaseAmount, setReleaseAmount] = useState('');
  const [refundAmount, setRefundAmount] = useState('');
  const [selectedBankCode, setSelectedBankCode] = useState('');
  const [bankPickerOpen, setBankPickerOpen] = useState(false);
  const [accountNo, setAccountNo] = useState('');
  const [accountHolderName, setAccountHolderName] = useState('');
  const [policyPreset, setPolicyPreset] = useState<SettlementPolicyPreset>('CUSTOM');
  const [policyCalculationNote, setPolicyCalculationNote] = useState('');
  const [policyWarning, setPolicyWarning] = useState('');
  const [formError, setFormError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const escrow = detail.escrow;
  const escrowAmount = typeof escrow?.amount === 'number' ? Math.trunc(escrow.amount) : 0;
  const releaseAmountNumber = releaseAmount ? Number(releaseAmount) : 0;
  const refundAmountNumber = refundAmount ? Number(refundAmount) : 0;
  const totalSettlement = releaseAmountNumber + refundAmountNumber;
  const remainingAmount = escrowAmount > 0 ? escrowAmount - totalSettlement : null;
  const settleable = isEscrowSettleable(escrow?.status);
  const isSubmitting = resolveStatus === 'loading';
  const suggestion = detail.settlementSuggestion;
  const hasSuggestion =
    typeof suggestion?.releaseAmount === 'number' &&
    typeof suggestion?.refundAmount === 'number';
  const needsSplit = action === 'TERMINATE_CLASS' || action === 'APPROVE_PARTIAL_REFUND';
  const fullRefund = action === 'APPROVE_FULL_REFUND';
  const financialAction = needsSplit || fullRefund;
  const selectedBank = BANK_OPTIONS.find((bank) => bank.code === selectedBankCode);
  const shouldCollectRefundPayout = financialAction
    && refundAmountNumber > 0
    && !hasExistingRefundPayoutInfo(detail);
  const settlementContext = getSettlementPolicyContext(escrowAmount, suggestion);

  useEffect(() => {
    const defaultAction = detail.terminationRequest?.status === 'PENDING'
      ? 'TERMINATE_CLASS'
      : 'CONTINUE_CLASS';
    const amount = typeof detail.escrow?.amount === 'number'
      ? Math.trunc(detail.escrow.amount)
      : 0;
    setAction(defaultAction);
    if (defaultAction === 'TERMINATE_CLASS' && hasSuggestion) {
      setReleaseAmount(String(Math.trunc(suggestion.releaseAmount ?? 0)));
      setRefundAmount(String(Math.trunc(suggestion.refundAmount ?? 0)));
    } else {
      setReleaseAmount(amount > 0 ? String(amount) : '');
      setRefundAmount('');
    }
    setResolutionNote(detail.resolution ?? '');
    setSelectedBankCode('');
    setBankPickerOpen(false);
    setAccountNo('');
    setAccountHolderName('');
    setPolicyPreset('CUSTOM');
    setPolicyCalculationNote('');
    setPolicyWarning('');
    setFormError('');
    setSuccessMessage('');
    resetResolve();
  }, [
    detail.disputeId,
    detail.escrow?.amount,
    detail.escrow?.escrowId,
    detail.resolution,
    hasSuggestion,
    suggestion?.refundAmount,
    suggestion?.releaseAmount,
    detail.terminationRequest?.status,
    resetResolve,
  ]);

  useEffect(() => {
    if (escrowAmount <= 0) return;
    if (action === 'APPROVE_FULL_REFUND') {
      setReleaseAmount('');
      setRefundAmount(String(escrowAmount));
      return;
    }
    if (action === 'APPROVE_PARTIAL_REFUND') {
      const refund = Math.trunc(escrowAmount * 0.3);
      setReleaseAmount(String(escrowAmount - refund));
      setRefundAmount(String(refund));
      return;
    }
    if (action === 'TERMINATE_CLASS') {
      if (hasSuggestion) {
        setReleaseAmount(String(Math.trunc(suggestion.releaseAmount ?? 0)));
        setRefundAmount(String(Math.trunc(suggestion.refundAmount ?? 0)));
        return;
      }
      setReleaseAmount(String(escrowAmount));
      setRefundAmount('');
    }
  }, [action, escrowAmount, hasSuggestion, suggestion?.refundAmount, suggestion?.releaseAmount]);

  const resetPolicyPreset = () => {
    setPolicyPreset('CUSTOM');
    setPolicyCalculationNote('');
    setPolicyWarning('');
  };

  const setSplit = (release: number, refund: number) => {
    setReleaseAmount(release > 0 ? String(Math.trunc(release)) : '');
    setRefundAmount(refund > 0 ? String(Math.trunc(refund)) : '');
    setFormError('');
  };

  const applyQuickAction = (mode: 'pro-rata' | 'release-all' | 'refund-all' | 'half' | 'refund-30') => {
    resetPolicyPreset();
    if (escrowAmount <= 0) {
      setFormError('Escrow chưa có số tiền hợp lệ.');
      return;
    }
    if (mode === 'pro-rata') {
      if (!hasSuggestion) {
        setFormError('Chưa đủ dữ liệu buổi học để tính pro-rata.');
        return;
      }
      setSplit(Math.trunc(suggestion.releaseAmount ?? 0), Math.trunc(suggestion.refundAmount ?? 0));
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

  const applyPolicyPreset = (preset: SettlementPolicyPreset) => {
    setPolicyPreset(preset);
    setPolicyCalculationNote('');
    setPolicyWarning('');
    if (preset === 'CUSTOM') {
      setFormError('');
      return;
    }

    const calculation = calculateSettlementPolicy(preset, escrowAmount, settlementContext);
    if ('error' in calculation) {
      setFormError(calculation.error);
      return;
    }

    setSplit(calculation.release, calculation.refund);
    setPolicyCalculationNote(calculation.note);
    setPolicyWarning(calculation.warning ?? '');
    setResolutionNote((current) => composePolicyResolution(current, getPolicyPresetLabel(preset), calculation.note));
  };

  const validateFinancialDecision = () => {
    if (!financialAction) return true;
    if (!escrow?.escrowId) {
      setFormError('Không tìm thấy escrow để tất toán.');
      return false;
    }
    if (isEscrowSettled(escrow.status)) {
      setFormError('Escrow đã tất toán.');
      return false;
    }
    if (!settleable) {
      setFormError('Escrow chưa ở trạng thái có thể tất toán.');
      return false;
    }
    if (!Number.isFinite(releaseAmountNumber) || !Number.isFinite(refundAmountNumber)) {
      setFormError('Số tiền giải ngân hoặc hoàn tiền không hợp lệ.');
      return false;
    }
    if (releaseAmountNumber < 0 || refundAmountNumber < 0) {
      setFormError('Số tiền không được âm.');
      return false;
    }
    if (escrowAmount > 0 && totalSettlement !== escrowAmount) {
      setFormError('Tổng giải ngân và hoàn tiền phải bằng tổng escrow.');
      return false;
    }
    if (action === 'APPROVE_PARTIAL_REFUND' && refundAmountNumber <= 0) {
      setFormError('Hoàn tiền một phần cần có số tiền hoàn lớn hơn 0.');
      return false;
    }
    return true;
  };

  const handleSelectBank = (bank: BankOption) => {
    setSelectedBankCode(bank.code);
    setBankPickerOpen(false);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedResolution = resolutionNote.trim();
    if (trimmedResolution.length < 10) {
      setFormError('Vui lòng nhập nội dung quyết định ít nhất 10 ký tự.');
      return;
    }
    if (!validateFinancialDecision()) return;
    const normalizedAccountNo = normalizeAccountNo(accountNo);
    if (shouldCollectRefundPayout) {
      if (!selectedBank) {
        setFormError('Vui lòng chọn ngân hàng nhận hoàn tiền.');
        return;
      }
      if (!/^[A-Za-z0-9]{4,50}$/.test(normalizedAccountNo)) {
        setFormError('Số tài khoản chỉ gồm chữ/số và dài từ 4 đến 50 ký tự.');
        return;
      }
      if (accountHolderName.trim().length < 2) {
        setFormError('Vui lòng nhập tên chủ tài khoản nhận hoàn tiền.');
        return;
      }
    }

    setFormError('');
    setSuccessMessage('');
    const payload = {
      action,
      status: action === 'REQUEST_MORE_EVIDENCE' ? 'WAITING' as const : 'RESOLVED' as const,
      resolution: trimmedResolution,
      releaseToBeneficiary: financialAction ? releaseAmountNumber : undefined,
      refundToPayer: financialAction ? refundAmountNumber : undefined,
      refundPayoutInfo: shouldCollectRefundPayout && selectedBank
        ? {
            bankName: selectedBank.name,
            accountNo: normalizedAccountNo,
            accountHolderName: accountHolderName.trim().replace(/\s+/g, ' '),
          }
        : undefined,
    };

    const updated = await resolveDispute(String(detail.disputeId), payload);
    if (updated) {
      setSuccessMessage('Đã lưu quyết định xử lý tranh chấp.');
      onChanged();
    }
  };

  return (
    <section className="pd-section">
      <div className="pd-section__head">
        <h3 className="pd-section__title">Quyết định xử lý</h3>
        <span className={escrowBadgeClass(escrow?.status ?? null)}>{escrow?.status ?? '—'}</span>
      </div>

      <form className="pd-resolution-form" onSubmit={handleSubmit}>
        <label className="pd-field">
          <span>Loại quyết định</span>
          <select
            className="adm-field"
            value={action}
            disabled={isSubmitting}
            onChange={(event) => {
              setAction(event.target.value as DisputeResolutionAction);
              resetPolicyPreset();
            }}
          >
            {RESOLUTION_ACTION_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        {financialAction && (
          <div className="pd-settlement-form">
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

            {needsSplit && (
              <>
                {hasSuggestion ? (
                  <div className="pd-pro-rata-note">
                    <strong>Gợi ý pro-rata</strong>
                    <span>
                      Đã học {suggestion.completedSessions ?? 0}/{suggestion.totalSessions ?? 0} buổi:
                      giải ngân {formatCurrency(suggestion.releaseAmount)} và hoàn {formatCurrency(suggestion.refundAmount)}.
                    </span>
                  </div>
                ) : null}

                <div className="pd-policy-panel">
                  <label className="pd-field">
                    <span>Căn cứ xử lý đề xuất</span>
                    <select
                      className="adm-field"
                      value={policyPreset}
                      disabled={isSubmitting || !settleable}
                      onChange={(event) => applyPolicyPreset(event.target.value as SettlementPolicyPreset)}
                    >
                      <option value="CUSTOM">Tự nhập số tiền</option>
                      {POLICY_PRESET_GROUPS.map((group) => (
                        <optgroup key={group.label} label={group.label}>
                          {group.options.map((option) => (
                            <option key={option.value} value={option.value}>
                              {option.label}
                            </option>
                          ))}
                        </optgroup>
                      ))}
                    </select>
                  </label>

                  {settlementContext.totalSessions != null && (
                    <div className="pd-policy-context">
                      <span>
                        Đã học {settlementContext.completedSessions}/{settlementContext.totalSessions} buổi
                      </span>
                      <span>Đơn giá tham chiếu {formatCurrency(settlementContext.oneSessionAmount)}</span>
                    </div>
                  )}

                  {policyCalculationNote && (
                    <div className="pd-policy-result">
                      <strong>{getPolicyPresetLabel(policyPreset)}</strong>
                      <span>{policyCalculationNote}</span>
                    </div>
                  )}

                  {policyWarning && <div className="pd-policy-warning">{policyWarning}</div>}
                </div>

                <div className="pd-quick-actions">
                  <button
                    className="tcs-btn tcs-btn--soft tcs-btn--sm"
                    type="button"
                    disabled={isSubmitting || !settleable || !hasSuggestion}
                    onClick={() => applyQuickAction('pro-rata')}
                  >
                    Theo buổi học
                  </button>
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

              </>
            )}

            {shouldCollectRefundPayout ? (
              <div className="pd-payout-panel">
                <p className="pd-payout-panel__title">Tài khoản nhận hoàn tiền</p>
                <div className="pd-field">
                  <span>Ngân hàng</span>
                  <BankSelectField
                    id={`admin-dispute-refund-bank-${detail.disputeId}`}
                    selectedBank={selectedBank}
                    onOpen={() => setBankPickerOpen(true)}
                  />
                </div>
                <div className="pd-money-grid">
                  <label className="pd-field">
                    <span>Số tài khoản</span>
                    <input
                      className="adm-field"
                      value={accountNo}
                      disabled={isSubmitting}
                      onChange={(event) => setAccountNo(event.target.value)}
                    />
                  </label>
                  <label className="pd-field">
                    <span>Tên chủ tài khoản</span>
                    <input
                      className="adm-field"
                      value={accountHolderName}
                      disabled={isSubmitting}
                      onChange={(event) => setAccountHolderName(event.target.value)}
                    />
                  </label>
                </div>
              </div>
            ) : null}
          </div>
        )}

        <label className="pd-field">
          <span>Nội dung quyết định</span>
          <textarea
            className="pd-textarea"
            rows={4}
            maxLength={1000}
            placeholder="Nhập căn cứ xử lý, kết luận và ghi chú cho các bên liên quan..."
            value={resolutionNote}
            disabled={isSubmitting}
            onChange={(event) => setResolutionNote(event.target.value)}
          />
        </label>

        {formError && <div className="adm-alert adm-alert--error">{formError}</div>}
        {resolveStatus === 'error' && resolveErrorMessage && (
          <div className="adm-alert adm-alert--error">{resolveErrorMessage}</div>
        )}
        {successMessage && <div className="adm-alert adm-alert--success">{successMessage}</div>}

        <div className="pd-resolution-actions">
          <button className="tcs-btn tcs-btn--primary" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Đang lưu...' : 'Lưu quyết định'}
          </button>
        </div>
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
    status: appealStatus,
    errorMessage: appealErrorMessage,
    appealDispute,
    reset: resetAppeal,
  } = useAppealDispute();
  const [appealReason, setAppealReason] = useState('');
  const [appealEvidenceFiles, setAppealEvidenceFiles] = useState<EvidenceUploadResponse[]>([]);
  const [appealUploadingEvidence, setAppealUploadingEvidence] = useState(false);
  const [appealFormError, setAppealFormError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    if (!detail) return;
    setAppealReason('');
    setAppealEvidenceFiles([]);
    setAppealUploadingEvidence(false);
    setAppealFormError('');
    setSuccessMessage('');
    resetAppeal();
  }, [detail?.disputeId, detail?.disputeStatus, detail?.resolution, resetAppeal]);

  const handleAppealEvidenceFilesChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? []);
    event.currentTarget.value = '';
    if (files.length === 0) return;

    setAppealFormError('');
    setSuccessMessage('');

    if (appealEvidenceFiles.length + files.length > MAX_EVIDENCE_FILES) {
      setAppealFormError(`Mỗi lần mở lại chỉ nên đính kèm tối đa ${MAX_EVIDENCE_FILES} ảnh.`);
      return;
    }

    const invalidFile = files.find((file) => !EVIDENCE_IMAGE_TYPES.has(file.type));
    if (invalidFile) {
      setAppealFormError(`"${invalidFile.name}" không đúng định dạng. Vui lòng chọn ảnh JPG, PNG hoặc WEBP.`);
      return;
    }

    const oversizedFile = files.find((file) => file.size > MAX_EVIDENCE_SIZE);
    if (oversizedFile) {
      setAppealFormError(`"${oversizedFile.name}" vượt quá 10MB.`);
      return;
    }

    setAppealUploadingEvidence(true);
    try {
      const uploadedFiles = await Promise.all(files.map((file) => disputeApi.uploadEvidenceImage(file)));
      setAppealEvidenceFiles((current) => [...current, ...uploadedFiles]);
    } catch (error) {
      console.error('Lỗi tải ảnh bằng chứng:', error);
      setAppealFormError('Không thể tải ảnh bằng chứng. Vui lòng thử lại.');
    } finally {
      setAppealUploadingEvidence(false);
    }
  };

  const removeAppealEvidenceFile = (fileId: number) => {
    setAppealEvidenceFiles((current) => current.filter((file) => file.fileId !== fileId));
  };

  const handleAppeal = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!detail) return;

    const trimmedReason = appealReason.trim();
    if (trimmedReason.length < 10) {
      setAppealFormError('Vui lòng nhập nội dung mở lại ít nhất 10 ký tự.');
      return;
    }
    if (appealUploadingEvidence) {
      setAppealFormError('Vui lòng chờ tải ảnh bằng chứng xong trước khi mở lại tranh chấp.');
      return;
    }

    setAppealFormError('');
    setSuccessMessage('');
    const updated = await appealDispute(String(detail.disputeId), {
      reason: trimmedReason,
      evidenceUrls: buildUploadedEvidenceUrls(appealEvidenceFiles) || undefined,
    });
    if (updated) {
      setAppealEvidenceFiles([]);
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
  const auditTrail = detail.auditTrail ?? [];

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
        <h3 className="pd-section__title">Lịch sử xử lý</h3>
        {auditTrail.length === 0 ? (
          <p className="adm-muted">Chưa có log xử lý cho tranh chấp này.</p>
        ) : (
          <ol className="pd-timeline">
            {auditTrail.map((event, index) => (
              <li key={event.auditId ?? index} className="pd-timeline__item">
                <div className="pd-timeline__marker" aria-hidden="true" />
                <div className="pd-timeline__body">
                  <div className="pd-timeline__head">
                    <strong>{event.action ?? 'Cập nhật tranh chấp'}</strong>
                    <span>{formatDateTime(event.createdAt)}</span>
                  </div>
                  <p>
                    {event.actorEmail ?? (event.actorId ? `Người dùng #${event.actorId}` : 'Hệ thống')}
                  </p>
                  <span>{formatAuditPayload(event.newValue)}</span>
                </div>
              </li>
            ))}
          </ol>
        )}
      </section>

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
        <EvidencePreviewList
          urls={detail.evidenceUrlList}
          emptyText="Không có bằng chứng đính kèm."
        />
      </section>

      {detail.disputeStatus === 'RESOLVED' ? (
        <section className="pd-section">
          <h3 className="pd-section__title">Quyết định xử lý</h3>
          {detail.resolution && <p className="pd-description">{detail.resolution}</p>}
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
                  <span className="pd-upload">
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      multiple
                      disabled={appealStatus === 'loading' || appealUploadingEvidence}
                      onChange={handleAppealEvidenceFilesChange}
                    />
                    <strong>{appealUploadingEvidence ? 'Đang tải ảnh...' : 'Chọn ảnh bằng chứng'}</strong>
                    <small>JPG, PNG hoặc WEBP, tối đa 10MB/ảnh.</small>
                  </span>
                </label>

                {appealEvidenceFiles.length > 0 && (
                  <div className="pd-evidence-list" aria-label="Ảnh bằng chứng mở lại">
                    {appealEvidenceFiles.map((file) => (
                      <FileThumbnail
                        key={file.fileId}
                        src={file.fileUrl}
                        fileName={file.fileName}
                        mimeType={file.mimeType}
                        fileSize={file.fileSize}
                        actions={
                          <button
                            className="pd-upload-remove"
                            type="button"
                            disabled={appealStatus === 'loading' || appealUploadingEvidence}
                            onClick={() => removeAppealEvidenceFile(file.fileId)}
                          >
                            Xóa
                          </button>
                        }
                      />
                    ))}
                  </div>
                )}

                {appealFormError && <div className="adm-alert adm-alert--error">{appealFormError}</div>}
                {appealStatus === 'error' && appealErrorMessage && (
                  <div className="adm-alert adm-alert--error">{appealErrorMessage}</div>
                )}
                {successMessage && <div className="adm-alert adm-alert--success">{successMessage}</div>}

                <div className="pd-resolution-actions">
                  <button
                    className="tcs-btn tcs-btn--primary"
                    type="submit"
                    disabled={appealStatus === 'loading' || appealUploadingEvidence}
                  >
                    {appealStatus === 'loading' ? 'Đang mở lại...' : 'Mở lại tranh chấp'}
                  </button>
                </div>
              </form>
            )}
          </div>
        </section>
      ) : (
        <ResolutionDecisionPanel detail={detail} onChanged={onChanged} />
      )}

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
              <InfoRow label="Ngân hàng nhận" value={detail.latestRefundRequest.bankName ?? '—'} />
              <InfoRow label="Tài khoản nhận" value={detail.latestRefundRequest.accountNoMasked ?? '—'} />
              <InfoRow label="Tên chủ tài khoản" value={detail.latestRefundRequest.accountHolderName ?? '—'} />
              <InfoRow label="Mã chuyển khoản" value={detail.latestRefundRequest.refundReferenceCode ?? '—'} />
              <InfoRow label="Trạng thái chuyển khoản" value={detail.latestRefundRequest.transferStatus ?? '—'} />
              <InfoRow label="Người xử lý" value={detail.latestRefundRequest.requestedByEmail ?? detail.latestRefundRequest.requestedByUserId} />
              <InfoRow label="Yêu cầu lúc" value={formatDateTime(detail.latestRefundRequest.requestedAt)} />
              <InfoRow label="Xử lý lúc" value={formatDateTime(detail.latestRefundRequest.processedAt)} />
              <InfoRow label="SePay xác nhận lúc" value={formatDateTime(detail.latestRefundRequest.transferProcessedAt)} />
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
            <InfoRow label="Ngân hàng nhận" value={detail.terminationRequest.bankName ?? '—'} />
            <InfoRow label="Tài khoản nhận" value={detail.terminationRequest.accountNoMasked ?? '—'} />
            <InfoRow label="Tên chủ tài khoản" value={detail.terminationRequest.accountHolderName ?? '—'} />
            <InfoRow label="Ngày hiệu lực" value={formatDate(detail.terminationRequest.effectiveDate)} />
          </div>
          <p className="pd-description">{detail.terminationRequest.reason ?? '—'}</p>
        </section>
      )}
    </div>
  );
}

function ClassIssueReportDetail({
  detail,
  onChanged,
}: {
  detail: ReportItem | null;
  onChanged: () => void;
}) {
  const [action, setAction] = useState<ClassIssueResolutionAction>('REQUEST_MORE_INFORMATION');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    setAction('REQUEST_MORE_INFORMATION');
    setNotes('');
    setErrorMessage('');
    setSuccessMessage('');
  }, [detail?.id]);

  if (!detail) {
    return (
      <div className="pd-detail pd-detail--empty">
        <p>Chọn một báo cáo sự cố để xem chi tiết và xử lý.</p>
      </div>
    );
  }

  const canResolve = detail.status === 'PENDING';

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    if (notes.trim().length < 10) {
      setErrorMessage('Ghi chú xử lý phải có ít nhất 10 ký tự.');
      return;
    }

    setSubmitting(true);
    try {
      const response = await platformApi.resolveClassIssue(detail.id, {
        action,
        notes: notes.trim(),
      });
      const linkedDisputeId = response.data.linkedDisputeId;
      setSuccessMessage(
        linkedDisputeId
          ? `Đã chuyển báo cáo thành tranh chấp #${linkedDisputeId}.`
          : 'Đã cập nhật xử lý báo cáo sự cố.',
      );
      setNotes('');
      onChanged();
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, 'Không thể xử lý báo cáo sự cố.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="pd-detail">
      <div className="pd-detail__head">
        <div>
          <p className="pd-detail__eyebrow">Báo cáo #{detail.id}</p>
          <h2 className="pd-detail__title">{detail.classTitle}</h2>
        </div>
        <span className={reportBadgeClass(detail.status)}>{detail.statusLabel}</span>
      </div>

      <section className="pd-section">
        <h3 className="pd-section__title">Thông tin sự cố</h3>
        <div className="pd-info-grid">
          <InfoRow label="Người báo cáo" value={detail.reporterEmail} />
          <InfoRow label="Lớp" value={`${detail.classTitle} (${detail.classStatus})`} />
          <InfoRow label="Loại sự cố" value={detail.issueTypeLabel} />
          <InfoRow label="Buổi liên quan" value={detail.lessonRef} />
          <InfoRow label="Ngày xảy ra" value={detail.occurredAt} />
          <InfoRow label="Mong muốn" value={detail.requestedActionLabel} />
          <InfoRow label="Danh mục" value={detail.categoryLabel} />
          <InfoRow label="Tranh chấp" value={detail.linkedDisputeId ? `#${detail.linkedDisputeId}` : '—'} />
        </div>
        <p className="pd-description">{detail.userDescription}</p>
      </section>

      <section className="pd-section">
        <h3 className="pd-section__title">Bằng chứng</h3>
        <EvidencePreviewList
          urls={detail.evidenceUrlList}
          emptyText="Chưa có bằng chứng."
        />
      </section>

      <section className="pd-section">
        <h3 className="pd-section__title">Xử lý</h3>
        {!canResolve ? (
          <div className="adm-alert adm-alert--success">Báo cáo này đã được xử lý.</div>
        ) : (
          <form className="pd-resolution-form" onSubmit={handleSubmit}>
            <label className="pd-field">
              <span>Hành động</span>
              <select
                className="adm-field"
                value={action}
                onChange={(event) => setAction(event.target.value as ClassIssueResolutionAction)}
              >
                {CLASS_ISSUE_ACTION_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="pd-field">
              <span>Ghi chú xử lý</span>
              <textarea
                className="pd-textarea"
                value={notes}
                onChange={(event) => setNotes(event.target.value)}
                placeholder="Nhập quyết định xử lý, hướng dẫn bổ sung thông tin hoặc lý do chuyển tranh chấp..."
              />
            </label>
            {errorMessage && <div className="adm-alert adm-alert--error">{errorMessage}</div>}
            {successMessage && <div className="adm-alert adm-alert--success">{successMessage}</div>}
            <div className="pd-resolution-actions">
              <button className="tcs-btn tcs-btn--primary" type="submit" disabled={submitting}>
                {submitting ? 'Đang xử lý...' : 'Lưu xử lý'}
              </button>
            </div>
          </form>
        )}
      </section>
    </div>
  );
}

function ReviewReportDetail({
  detail,
  onChanged,
}: {
  detail: ReportItem | null;
  onChanged: () => void;
}) {
  const [action, setAction] = useState<ReviewReportAction>('HIDE_REVIEW');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const reviewMissing = detail != null && detail.reportedReview == null;

  useEffect(() => {
    // Đánh giá đã bị xóa thì chỉ còn cách đóng báo cáo.
    setAction(reviewMissing ? 'KEEP_REVIEW' : 'HIDE_REVIEW');
    setNotes('');
    setErrorMessage('');
    setSuccessMessage('');
  }, [detail?.id, reviewMissing]);

  if (!detail) {
    return (
      <div className="pd-detail pd-detail--empty">
        <p>Chọn một báo cáo đánh giá để xem nội dung bị tố cáo và xử lý.</p>
      </div>
    );
  }

  const review = detail.reportedReview;
  const canResolve = detail.status === 'PENDING';

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');
    if (notes.trim().length < 10) {
      setErrorMessage('Ghi chú xử lý phải có ít nhất 10 ký tự.');
      return;
    }
    if (action === 'DELETE_REVIEW') {
      const ok = window.confirm(
        'Xóa vĩnh viễn đánh giá này? Hành động không thể hoàn tác và điểm danh tiếng của gia sư sẽ được tính lại.',
      );
      if (!ok) return;
    }

    setSubmitting(true);
    try {
      await platformApi.resolveReviewReport(detail.id, { action, notes: notes.trim() });
      setSuccessMessage('Đã xử lý báo cáo đánh giá.');
      setNotes('');
      onChanged();
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, 'Không thể xử lý báo cáo đánh giá.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="pd-detail">
      <div className="pd-detail__head">
        <div>
          <p className="pd-detail__eyebrow">Báo cáo đánh giá #{detail.id}</p>
          <h2 className="pd-detail__title">{review?.classTitle ?? detail.classTitle}</h2>
        </div>
        <span className={reportBadgeClass(detail.status)}>{detail.statusLabel}</span>
      </div>

      <section className="pd-section">
        <h3 className="pd-section__title">Thông tin báo cáo</h3>
        <div className="pd-info-grid">
          <InfoRow label="Người báo cáo" value={detail.reporterEmail} />
          <InfoRow label="Lý do" value={detail.categoryLabel} />
          <InfoRow label="Mã đánh giá" value={`#${detail.targetId}`} />
          <InfoRow label="Gửi lúc" value={detail.createdAt} />
        </div>
        <p className="pd-description">{detail.userDescription}</p>
      </section>

      <section className="pd-section">
        <h3 className="pd-section__title">Đánh giá bị báo cáo</h3>
        {!review ? (
          <div className="adm-alert adm-alert--info">
            Đánh giá này không còn tồn tại (đã bị xóa trước đó).
          </div>
        ) : (
          <>
            <div className="pd-info-grid">
              <InfoRow
                label="Người đánh giá"
                value={
                  review.anonymous
                    ? `${review.reviewerName ?? '—'} (ẩn danh: ${review.publicDisplayName ?? '—'})`
                    : (review.reviewerName ?? review.reviewerEmail ?? '—')
                }
              />
              <InfoRow label="Gia sư bị đánh giá" value={review.tutorName ?? '—'} />
              <InfoRow label="Môn" value={review.subjectName ?? '—'} />
              <InfoRow label="Điểm" value={review.rating != null ? `${review.rating.toFixed(1)}/5` : '—'} />
              <InfoRow label="Trạng thái" value={REVIEW_MODERATION_LABELS[review.status] ?? review.status} />
              <InfoRow label="Đánh giá lúc" value={formatDateTime(review.createdAt)} />
            </div>
            <p className="pd-description">{review.comment ? `“${review.comment}”` : '(Không có nội dung)'}</p>
            {review.tutorReply ? (
              <p className="pd-description">↩ Gia sư phản hồi: {review.tutorReply}</p>
            ) : null}
          </>
        )}
      </section>

      <section className="pd-section">
        <h3 className="pd-section__title">Xử lý</h3>
        {!canResolve ? (
          <div className="adm-alert adm-alert--success">Báo cáo này đã được xử lý.</div>
        ) : (
          <form className="pd-resolution-form" onSubmit={handleSubmit}>
            <label className="pd-field">
              <span>Hành động</span>
              <select
                className="adm-field"
                value={action}
                disabled={reviewMissing}
                onChange={(event) => setAction(event.target.value as ReviewReportAction)}
              >
                {(reviewMissing
                  ? REVIEW_REPORT_ACTION_OPTIONS.filter((option) => option.value === 'KEEP_REVIEW')
                  : REVIEW_REPORT_ACTION_OPTIONS
                ).map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              {reviewMissing && (
                <span className="adm-table__sub">
                  Đánh giá không còn tồn tại nên chỉ có thể đóng báo cáo.
                </span>
              )}
            </label>
            <label className="pd-field">
              <span>Ghi chú xử lý</span>
              <textarea
                className="pd-textarea"
                rows={4}
                maxLength={2000}
                value={notes}
                onChange={(event) => setNotes(event.target.value)}
                placeholder="Nhập căn cứ xử lý để lưu vào hồ sơ báo cáo và gửi cho người báo cáo..."
              />
            </label>
            {errorMessage && <div className="adm-alert adm-alert--error">{errorMessage}</div>}
            {successMessage && <div className="adm-alert adm-alert--success">{successMessage}</div>}
            <div className="pd-resolution-actions">
              <button className="tcs-btn tcs-btn--primary" type="submit" disabled={submitting}>
                {submitting ? 'Đang xử lý...' : 'Lưu xử lý'}
              </button>
            </div>
          </form>
        )}
      </section>
    </div>
  );
}

function RefundRequestDetail({
  item,
  onChanged,
}: {
  item: RefundRequestItem | null;
  onChanged: () => void;
}) {
  const [approvedAmount, setApprovedAmount] = useState('');
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState<'approve' | 'reject' | null>(null);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  useEffect(() => {
    setApprovedAmount(item?.raw.amount ? String(Math.trunc(item.raw.amount)) : '');
    setReason('');
    setErrorMessage('');
    setSuccessMessage('');
  }, [item?.id, item?.raw.amount]);

  if (!item) {
    return <div className="pd-detail pd-detail--empty">Chọn một yêu cầu hoàn tiền để xem chi tiết.</div>;
  }

  const canDecide = item.canDecide;
  const escrowLabel = item.escrowId === '—' ? '—' : `#${item.escrowId}`;

  const handleApprove = async () => {
    setErrorMessage('');
    setSuccessMessage('');
    const parsedAmount = Number(approvedAmount);
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setErrorMessage('Số tiền hoàn được duyệt phải lớn hơn 0.');
      return;
    }

    setSubmitting('approve');
    try {
      await platformApi.approveRefundRequest(item.id, {
        approvedAmount: parsedAmount,
        reason: reason.trim() || undefined,
      });
      setSuccessMessage('Đã duyệt hoàn tiền. Nếu hoàn qua ngân hàng, hệ thống sẽ chờ SePay xác nhận tiền ra.');
      onChanged();
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, 'Không thể duyệt yêu cầu hoàn tiền.'));
    } finally {
      setSubmitting(null);
    }
  };

  const handleReject = async () => {
    setErrorMessage('');
    setSuccessMessage('');
    if (reason.trim().length < 5) {
      setErrorMessage('Vui lòng nhập lý do từ chối.');
      return;
    }

    setSubmitting('reject');
    try {
      await platformApi.rejectRefundRequest(item.id, {
        reason: reason.trim(),
      });
      setSuccessMessage('Đã từ chối yêu cầu hoàn tiền.');
      onChanged();
    } catch (error) {
      setErrorMessage(getApiErrorMessage(error, 'Không thể từ chối yêu cầu hoàn tiền.'));
    } finally {
      setSubmitting(null);
    }
  };

  return (
    <div className="pd-detail">
      <div className="pd-detail__head">
        <div>
          <p className="pd-detail__eyebrow">Yêu cầu hoàn tiền #{item.id}</p>
          <h2 className="pd-detail__title">{item.classTitle}</h2>
        </div>
        <span className={refundBadgeClass(item.status)}>{item.statusLabel}</span>
      </div>

      <section className="pd-section">
        <h3 className="pd-section__title">Thông tin yêu cầu</h3>
        <div className="pd-info-grid">
          <InfoRow label="Người yêu cầu" value={item.requester} />
          <InfoRow label="Mã escrow" value={escrowLabel} />
          <InfoRow label="Trạng thái escrow" value={<span className={escrowBadgeClass(item.escrowStatus)}>{item.escrowStatusLabel}</span>} />
          <InfoRow label="Tổng escrow" value={item.escrowAmount} />
          <InfoRow label="Số tiền yêu cầu" value={item.amount} />
          <InfoRow label="Ngân hàng nhận" value={item.bankName} />
          <InfoRow label="Tài khoản nhận" value={item.accountNoMasked} />
          <InfoRow label="Tên chủ tài khoản" value={item.accountHolderName} />
          <InfoRow label="Mã chuyển khoản" value={item.refundReferenceCode} />
          <InfoRow label="Trạng thái chuyển khoản" value={item.transferStatus} />
          <InfoRow label="Tạo lúc" value={item.requestedAt} />
          <InfoRow label="Xử lý lúc" value={item.processedAt} />
          <InfoRow label="SePay xác nhận lúc" value={formatDateTime(item.raw.transferProcessedAt)} />
        </div>
        <p className="pd-description">{item.reason}</p>
      </section>

      <section className="pd-section">
        <h3 className="pd-section__title">Quyết định tài chính</h3>
        {!canDecide ? (
          <div className="adm-alert adm-alert--success">Yêu cầu này đã được xử lý.</div>
        ) : (
          <div className="pd-resolution-form">
            <label className="pd-field">
              <span>Số tiền hoàn được duyệt</span>
              <input
                className="adm-field"
                type="text"
                inputMode="numeric"
                value={approvedAmount}
                onChange={(event) => setApprovedAmount(normalizeDigits(event.target.value))}
              />
            </label>
            <label className="pd-field">
              <span>Lý do/quyết định</span>
              <textarea
                className="pd-textarea"
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                placeholder="Nhập căn cứ duyệt hoàn hoặc lý do từ chối..."
              />
            </label>
            {errorMessage && <div className="adm-alert adm-alert--error">{errorMessage}</div>}
            {successMessage && <div className="adm-alert adm-alert--success">{successMessage}</div>}
            <div className="pd-resolution-actions">
              <button
                className="tcs-btn tcs-btn--primary"
                type="button"
                onClick={handleApprove}
                disabled={submitting != null}
              >
                {submitting === 'approve' ? 'Đang duyệt...' : 'Duyệt hoàn tiền'}
              </button>
              <button
                className="tcs-btn tcs-btn--ghost"
                type="button"
                onClick={handleReject}
                disabled={submitting != null}
              >
                {submitting === 'reject' ? 'Đang từ chối...' : 'Từ chối'}
              </button>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}

export default function PlatformReportsPage() {
  const reports = useReportList();
  const [selectedReportId, setSelectedReportId] = useState<string | null>(null);
  const [selectedReviewReportId, setSelectedReviewReportId] = useState<string | null>(null);
  const [disputeStatusFilter, setDisputeStatusFilter] = useState<DisputeStatus | undefined>();
  const [refundStatusFilter, setRefundStatusFilter] = useState<RefundRequestStatus | undefined>();
  const disputes = useDisputeReviewList(disputeStatusFilter);
  const refunds = useRefundRequestList(refundStatusFilter);
  const [selectedRefundId, setSelectedRefundId] = useState<string | null>(null);

  const openReportCount = reports.items.filter((item) => item.status === 'PENDING').length;
  const openDisputeCount = disputes.items.filter((item) => item.status !== 'RESOLVED').length;
  const pendingRefundCount = refunds.items.filter((item) =>
    item.status === 'PENDING'
      || (item.status === 'APPROVED' && item.raw.transferStatus === 'PENDING')
  ).length;
  const heldEscrowCount = disputes.items.filter((item) => isEscrowHeldForDispute(item.escrowStatus)).length;
  const classIssueReports = reports.items.filter((item) => item.targetType === 'CLASS');
  const reviewReports = reports.items.filter((item) => item.targetType === 'REVIEW');
  const openReviewReportCount = reviewReports.filter((item) => item.status === 'PENDING').length;
  const selectedReport = classIssueReports.find((item) => item.id === selectedReportId) ?? null;
  const selectedReviewReport =
    reviewReports.find((item) => item.id === selectedReviewReportId) ?? null;
  const selectedRefund = refunds.items.find((item) => item.id === selectedRefundId) ?? null;

  useEffect(() => {
    if (classIssueReports.length === 0) {
      setSelectedReportId(null);
      return;
    }
    if (!selectedReportId || !classIssueReports.some((item) => item.id === selectedReportId)) {
      setSelectedReportId(classIssueReports[0].id);
    }
  }, [classIssueReports, selectedReportId]);

  useEffect(() => {
    if (reviewReports.length === 0) {
      setSelectedReviewReportId(null);
      return;
    }
    if (!selectedReviewReportId || !reviewReports.some((item) => item.id === selectedReviewReportId)) {
      setSelectedReviewReportId(reviewReports[0].id);
    }
  }, [reviewReports, selectedReviewReportId]);

  useEffect(() => {
    if (refunds.items.length === 0) {
      setSelectedRefundId(null);
      return;
    }
    if (!selectedRefundId || !refunds.items.some((item) => item.id === selectedRefundId)) {
      setSelectedRefundId(refunds.items[0].id);
    }
  }, [refunds.items, selectedRefundId]);

  const selectDispute = (item: DisputeReviewItem) => {
    disputes.selectDispute(item);
  };

  const reloadIssueQueues = () => {
    reports.reload();
    disputes.reload();
  };

  useEffect(() => {
    const handleFocus = () => {
      reports.reload();
      disputes.reload();
      refunds.reload();
    };
    window.addEventListener('focus', handleFocus);
    return () => window.removeEventListener('focus', handleFocus);
  }, [disputes.reload, refunds.reload, reports.reload]);

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
        <article className="adm-summary-card">
          <p className="adm-summary-card__label">Hoàn tiền chờ xử lý/chuyển</p>
          <p className="adm-summary-card__value">{pendingRefundCount}</p>
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

      <section className="pd-console" aria-label="Hàng đợi báo cáo sự cố">
        <div className="adm-card pd-console__list">
          <div className="pd-card-head">
            <div>
              <h2 className="pd-card-head__title">Báo cáo sự cố lớp</h2>
              <p className="pd-card-head__meta">{classIssueReports.length} báo cáo</p>
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
            <div className="pd-dispute-list">
              {classIssueReports.length === 0 ? (
                <div className="adm-state">Chưa có báo cáo nào.</div>
              ) : (
                classIssueReports.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    className={`pd-dispute-item${
                      selectedReportId === item.id ? ' pd-dispute-item--active' : ''
                    }`}
                    onClick={() => setSelectedReportId(item.id)}
                  >
                    <span className="pd-dispute-item__top">
                      <span className="pd-dispute-item__id">#{item.id}</span>
                      <span className={reportBadgeClass(item.status)}>{item.statusLabel}</span>
                    </span>
                    <span className="pd-dispute-item__title">{item.classTitle}</span>
                    <span className="pd-dispute-item__desc">{item.userDescription}</span>
                    <span className="pd-dispute-item__meta">
                      {item.issueTypeLabel} · {item.evidenceCount} bằng chứng · {item.createdAt}
                      {item.linkedDisputeId ? (
                        <span className="tcs-badge tcs-badge--role">Tranh chấp #{item.linkedDisputeId}</span>
                      ) : null}
                    </span>
                  </button>
                ))
              )}
            </div>
          )}
        </div>

        <ClassIssueReportDetail detail={selectedReport} onChanged={reloadIssueQueues} />
      </section>

      <section className="pd-console" aria-label="Hàng đợi báo cáo đánh giá">
        <div className="adm-card pd-console__list">
          <div className="pd-card-head">
            <div>
              <h2 className="pd-card-head__title">Báo cáo đánh giá</h2>
              <p className="pd-card-head__meta">
                {reviewReports.length} báo cáo · {openReviewReportCount} chờ xử lý
              </p>
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
            <div className="pd-dispute-list">
              {reviewReports.length === 0 ? (
                <div className="adm-state">Chưa có báo cáo đánh giá nào.</div>
              ) : (
                reviewReports.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    className={`pd-dispute-item${
                      selectedReviewReportId === item.id ? ' pd-dispute-item--active' : ''
                    }`}
                    onClick={() => setSelectedReviewReportId(item.id)}
                  >
                    <span className="pd-dispute-item__top">
                      <span className="pd-dispute-item__id">#{item.id}</span>
                      <span className={reportBadgeClass(item.status)}>{item.statusLabel}</span>
                    </span>
                    <span className="pd-dispute-item__title">
                      {item.reportedReview?.tutorName
                        ? `Đánh giá gia sư ${item.reportedReview.tutorName}`
                        : `Đánh giá #${item.targetId}`}
                    </span>
                    <span className="pd-dispute-item__desc">{item.userDescription}</span>
                    <span className="pd-dispute-item__meta">
                      {item.categoryLabel} · {item.reporterEmail} · {item.createdAt}
                      {item.reportedReview ? (
                        <span className="tcs-badge tcs-badge--role">
                          {REVIEW_MODERATION_LABELS[item.reportedReview.status]
                            ?? item.reportedReview.status}
                        </span>
                      ) : (
                        <span className="tcs-badge tcs-badge--banned">Đã xóa</span>
                      )}
                    </span>
                  </button>
                ))
              )}
            </div>
          )}
        </div>

        <ReviewReportDetail detail={selectedReviewReport} onChanged={reports.reload} />
      </section>

      <section className="pd-console" aria-label="Hàng đợi yêu cầu hoàn tiền">
        <div className="adm-card pd-console__list">
          <div className="pd-card-head">
            <div>
              <h2 className="pd-card-head__title">Yêu cầu hoàn tiền</h2>
              <p className="pd-card-head__meta">{refunds.items.length} yêu cầu</p>
            </div>
            <button className="tcs-btn tcs-btn--ghost" type="button" onClick={refunds.reload}>
              Làm mới
            </button>
          </div>

          <div className="adm-toolbar">
            <select
              className="adm-field"
              value={refundStatusFilter ?? ''}
              onChange={(event) =>
                setRefundStatusFilter((event.target.value as RefundRequestStatus) || undefined)
              }
            >
              <option value="">Tất cả trạng thái</option>
              <option value="PENDING">Chờ xử lý</option>
              <option value="APPROVED">Đã duyệt</option>
              <option value="REJECTED">Từ chối</option>
              <option value="COMPLETED">Đã hoàn tiền</option>
            </select>
          </div>

          {refunds.status === 'loading' && <div className="adm-state">Đang tải yêu cầu hoàn tiền…</div>}
          {refunds.status === 'error' && (
            <div className="adm-state">
              <p>{refunds.errorMessage ?? 'Không tải được dữ liệu.'}</p>
              <button className="tcs-btn tcs-btn--primary" type="button" onClick={refunds.reload}>
                Thử lại
              </button>
            </div>
          )}

          {refunds.status === 'success' && (
            <div className="pd-dispute-list">
              {refunds.items.length === 0 ? (
                <div className="adm-state">Chưa có yêu cầu hoàn tiền nào.</div>
              ) : (
                refunds.items.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    className={`pd-dispute-item${
                      selectedRefundId === item.id ? ' pd-dispute-item--active' : ''
                    }`}
                    onClick={() => setSelectedRefundId(item.id)}
                  >
                    <span className="pd-dispute-item__top">
                      <span className="pd-dispute-item__id">#{item.id}</span>
                      <span className={refundBadgeClass(item.status)}>{item.statusLabel}</span>
                    </span>
                    <span className="pd-dispute-item__title">{item.classTitle}</span>
                    <span className="pd-dispute-item__desc">{item.reason}</span>
                    <span className="pd-dispute-item__meta">
                      {item.amount} · escrow {item.escrowId === '—' ? '—' : `#${item.escrowId}`} · {item.requestedAt}
                      <span className={escrowBadgeClass(item.escrowStatus)}>{item.escrowStatusLabel}</span>
                    </span>
                  </button>
                ))
              )}
            </div>
          )}
        </div>

        <RefundRequestDetail item={selectedRefund} onChanged={refunds.reload} />
      </section>
    </AdminLayout>
  );
}
