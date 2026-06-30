import type { Verification } from '../types/verificationTypes';

export function mapVerificationStatus(status: Verification['status']) {
  const map: Record<Verification['status'], { label: string; variant: string }> = {
    SUBMITTED: { label: 'Submitted', variant: 'info' },
    UNDER_REVIEW: { label: 'Under Review', variant: 'warn' },
    VERIFIED: { label: 'Verified', variant: 'success' },
    REJECTED: { label: 'Rejected', variant: 'danger' },
  };
  return map[status];
}
