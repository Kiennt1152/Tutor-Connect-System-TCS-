export type ReportCategory = 'FRAUD' | 'ABUSE' | 'SPAM';

export type ReportTargetType = 'USER' | 'CLASS' | 'REVIEW';

export type ReportStatus = 'PENDING' | 'RESOLVED';

export type DisputeStatus = 'OPEN' | 'UNDER_INVESTIGATION' | 'RESOLVED' | 'WAITING';

export type EscrowStatus = 'PENDING' | 'FUNDED' | 'RELEASED' | 'REFUNDED' | 'ON_HOLD' | 'DISPUTED';

export interface CreateClassIssueRequest {
  classId: number;
  category: ReportCategory;
  description: string;
  evidenceUrls?: string;
  escrowId?: number;
  assignmentId?: number;
  classStudentId?: number;
}

export interface DisputeResponse {
  disputeId: number;
  disputeStatus: DisputeStatus;
  reportId: number;
  reportStatus: ReportStatus;
  targetType: ReportTargetType;
  targetId: number;
  category: ReportCategory;
  description: string;
  evidenceUrls: string | null;
  escrowId: number;
  escrowStatus: EscrowStatus;
  createdAt: string;
}
