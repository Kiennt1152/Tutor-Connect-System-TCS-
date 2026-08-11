import type {
  Contract,
  ContractApiResponse,
  ContractSignature,
  ContractSignatureApiResponse,
  ContractSignatureList,
  ContractSignatureListApiResponse,
  ContractSignatureStatus,
  ContractStatus,
  ContractSourceType,
  OtpSendResult,
  SendOtpApiResponse,
  SignWithOtpApiRequest,
} from '../types/contractTypes';

const STATUS_LABELS: Record<ContractStatus, string> = {
  PENDING: 'Chờ ký',
  DRAFT: 'Nháp',
  SIGNED: 'Đã ký',
  ACTIVE: 'Đang hoạt động',
  COMPLETED: 'Hoàn thành',
  TERMINATED: 'Đã chấm dứt',
};

const SOURCE_TYPE_LABELS: Record<ContractSourceType, string> = {
  PRIVATE: 'Lớp Gia sư',
  CENTER: 'Lớp Dạy thêm',
};

const SIGNATURE_STATUS_LABELS: Record<ContractSignatureStatus, string> = {
  PENDING: 'Chưa ký',
  SIGNED: 'Đã ký',
  EXPIRED: 'Hết hạn',
};

const formatDateTime = (value: string | null | undefined): string => {
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

const formatDate = (value: string | null | undefined): string => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date);
};

export function mapContractSignature(
  sig: ContractSignatureApiResponse,
): ContractSignature {
  return {
    id: sig.signatureId,
    partyRole: sig.partyRole,
    partyLabel: sig.partyLabel,
    signerId: sig.signerId,
    signerName: sig.signerName,
    signerEmail: sig.signerEmail,
    signatureStatus: sig.signatureStatus,
    signatureStatusLabel:
      SIGNATURE_STATUS_LABELS[sig.signatureStatus] ?? sig.signatureStatus,
    signedAt: sig.signedAt ? formatDateTime(sig.signedAt) : null,
    otpExpiresAt: sig.otpExpiresAt ? formatDateTime(sig.otpExpiresAt) : null,
    remainingOtpAttempts: sig.remainingOtpAttempts,
    isOtpExpired: sig.isOtpExpired,
  };
}

export function mapSignatureList(
  response: ContractSignatureListApiResponse,
): ContractSignatureList {
  return {
    contractId: response.contractId,
    contractNo: response.contractNo,
    hasAllSignatures: response.hasAllSignatures,
    signedCount: response.signedCount,
    requiredSignatures: response.requiredSignatures,
    signatures: response.signatures.map(mapContractSignature),
  };
}

export function mapContract(response: ContractApiResponse): Contract {
  const sourceType = response.sourceType ?? 'PRIVATE';
  return {
    id: String(response.contractId),
    contractNo: response.contractNo,
    status: response.status,
    statusLabel: STATUS_LABELS[response.status] ?? response.status,
    sourceType,
    sourceTypeLabel: SOURCE_TYPE_LABELS[sourceType] ?? sourceType,
    assignmentId: response.assignmentId ?? null,
    classId: response.classId,
    classStudentId: response.classStudentId ?? null,
    recruitmentApplicationId: response.recruitmentApplicationId ?? null,
    clientId: response.clientId ?? null,
    clientName: response.clientName ?? null,
    clientEmail: response.clientEmail ?? null,
    tutorId: response.tutorId ?? null,
    tutorName: response.tutorName ?? null,
    tutorEmail: response.tutorEmail ?? null,
    centerId: response.centerId ?? null,
    centerName: response.centerName ?? null,
    centerEmail: response.centerEmail ?? null,
    templateId: response.templateId ?? null,
    templateName: response.templateName ?? null,
    termsSummary: response.termsSummary,
    contractFileUrl: response.contractFileUrl,
    hasAllSignatures: response.hasAllSignatures ?? false,
    signedCount: response.signedCount ?? 0,
    requiredSignatures: response.requiredSignatures ?? 0,
    signedAt: response.signedAt ? formatDateTime(response.signedAt) : null,
    expiresAt: response.expiresAt ? formatDate(response.expiresAt) : null,
    confirmedAt: response.confirmedAt ? formatDateTime(response.confirmedAt) : null,
    createdAt: formatDateTime(response.createdAt),
    updatedAt: formatDateTime(response.updatedAt),
    classTitle: response.classTitle ?? null,
    classType: response.classType ?? null,
    tuitionFee: response.tuitionFee ?? null,
    lessonMode: response.lessonMode ?? null,
    numberOfSessions: response.numberOfSessions ?? null,
    tutor: response.tutor ?? null,
    client: response.client ?? null,
    center: response.center ?? null,
    escrowPayment: response.escrowPayment ?? null,
    refundPayoutInfo: response.refundPayoutInfo ?? null,
    totalSessions: response.totalSessions ?? null,
    completedSessions: response.completedSessions ?? null,
    refundAllowed: response.refundAllowed ?? true,
    refundBlockedReason: response.refundBlockedReason ?? null,
  };
}

export function buildSignWithOtpPayload(otpCode: string): SignWithOtpApiRequest {
  return { otpCode: otpCode.trim() };
}

export function mapOtpSendResult(response: SendOtpApiResponse): OtpSendResult {
  return {
    message: response.message,
    expiresInMinutes: response.expiresInMinutes,
    maxAttempts: response.maxAttempts,
  };
}

export { formatDateTime, formatDate };
