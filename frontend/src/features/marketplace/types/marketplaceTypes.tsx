export interface MarketplaceRequest {}

export interface MarketplaceResponse {}

export type TutorApplicationStatus =
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'WITHDRAWN';

export interface TutorApplication {
  applicationId: number;
  classId: number;
  classTitle: string;
  tutorId: number;
  tutorName: string;
  tutorAvatarUrl?: string | null;
  tutorRatingAvg?: number | null;
  tutorVerificationStatus?: string | null;
  proposedRate?: number | null;
  coverLetter?: string | null;
  status: TutorApplicationStatus;
  appliedAt: string;
  reviewedAt?: string | null;
}

export interface TutorApplicationReviewRequest {
  decision: 'ACCEPTED' | 'REJECTED';
}

export interface ClassSummary {
  classId: number;
  title: string;
  status: string;
  createdAt: string;
}

export const APPLICATION_STATUS_LABELS: Record<TutorApplicationStatus, string> = {
  SUBMITTED: 'Đã gửi',
  UNDER_REVIEW: 'Đang xét duyệt',
  ACCEPTED: 'Đã chấp nhận',
  REJECTED: 'Bị từ chối',
  WITHDRAWN: 'Đã rút',
};

export const APPLICATION_STATUS_TONES: Record<TutorApplicationStatus, string> = {
  SUBMITTED: 'info',
  UNDER_REVIEW: 'warning',
  ACCEPTED: 'success',
  REJECTED: 'danger',
  WITHDRAWN: 'muted',
};