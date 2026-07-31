export type ReportCategory = 'FRAUD' | 'ABUSE' | 'SPAM';

export type ReportTargetType = 'USER' | 'CLASS' | 'REVIEW';

export type ReportStatus = 'PENDING' | 'RESOLVED';

export type DisputeStatus = 'OPEN' | 'UNDER_INVESTIGATION' | 'RESOLVED' | 'WAITING';

export type EscrowStatus = 'PENDING' | 'FUNDED' | 'RELEASED' | 'REFUNDED' | 'ON_HOLD' | 'DISPUTED';

export type ClassIssueType =
  | 'TUTOR_ABSENT'
  | 'CLIENT_ABSENT'
  | 'TECHNICAL_ISSUE'
  | 'INAPPROPRIATE_BEHAVIOR'
  | 'SCHEDULE_CONFLICT'
  | 'QUALITY_ISSUE'
  | 'PAYMENT_OR_REFUND'
  | 'OTHER';

export type ClassIssueRequestedAction =
  | 'CONTINUE_CLASS'
  | 'RESCHEDULE'
  | 'REPLACE_TUTOR'
  | 'REFUND_REVIEW'
  | 'ESCALATE_DISPUTE'
  | 'TERMINATE_CLASS'
  | 'OTHER';

export interface CreateClassIssueRequest {
  classId: number;
  issueType: ClassIssueType;
  category?: ReportCategory;
  lessonRef?: string;
  occurredAt?: string;
  requestedAction: ClassIssueRequestedAction;
  description: string;
  evidenceUrls?: string;
  escrowId?: number;
  assignmentId?: number;
  classStudentId?: number;
}

export interface DisputeResponse {
  disputeId: number | null;
  disputeStatus: DisputeStatus | null;
  escalatedToDispute: boolean;
  reportId: number;
  reportStatus: ReportStatus;
  targetType: ReportTargetType;
  targetId: number;
  category: ReportCategory;
  description: string;
  evidenceUrls: string | null;
  escrowId: number | null;
  escrowStatus: EscrowStatus | null;
  createdAt: string;
}
