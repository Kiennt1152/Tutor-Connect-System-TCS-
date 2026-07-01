export type GuardianApprovalActionType = 'DEPOSIT' | 'CONTRACT_SIGN';
export type GuardianApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export type GuardianApproval = {
  approvalId: number;
  actionType: GuardianApprovalActionType;
  status: GuardianApprovalStatus;
  amount?: number;
  description?: string;
  tutorName?: string;
  subjectName?: string;
  contractReference?: string;
  paymentTransactionId?: number;
  minorUserId: number;
  minorName: string;
  parentUserId: number;
  parentName: string;
  createdAt: string;
  resolvedAt?: string;
};
