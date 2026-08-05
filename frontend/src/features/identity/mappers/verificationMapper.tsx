import type {
  Verification,
  VerificationDocumentType,
} from '../types/verificationTypes';

export function mapVerificationStatus(status: Verification['status']) {
  const map: Record<Verification['status'], { label: string; variant: string }> = {
    SUBMITTED: { label: 'Đã gửi', variant: 'info' },
    UNDER_REVIEW: { label: 'Đang xét duyệt', variant: 'warn' },
    VERIFIED: { label: 'Đã xác minh', variant: 'success' },
    REJECTED: { label: 'Bị từ chối', variant: 'danger' },
  };
  return map[status];
}

export function mapDocumentType(type: VerificationDocumentType): string {
  const map: Record<VerificationDocumentType, string> = {
    ID_CARD: 'CCCD/CMND mặt trước',
    DEGREE: 'CCCD/CMND mặt sau',
    CERTIFICATE: 'Bằng cấp / chứng chỉ',
    LICENSE: 'Giấy phép',
  };
  return map[type] ?? type;
}
