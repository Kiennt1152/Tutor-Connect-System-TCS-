import type { GuardianApprovalStatus } from '../../finance/types/financeTypes';

export type SignContractRequest = {
  tutorName: string;
  subjectName?: string;
};

export type SignContractResponse = {
  contractReference: string;
  signerName: string;
  beneficiaryMinorName?: string;
  signedByParentOnBehalf: boolean;
  signedAt?: string;
  message: string;
  pendingGuardianApproval: boolean;
  guardianApprovalId?: number;
  guardianApprovalStatus?: GuardianApprovalStatus;
};
