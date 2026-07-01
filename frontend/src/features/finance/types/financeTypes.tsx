export type WalletStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED';
export type GuardianApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export type WalletResponse = {
  walletId: number;
  availableBalance: number;
  frozenBalance: number;
  status: WalletStatus;
  updatedAt?: string;
  legalOwnerUserId?: number;
  legalOwnerName?: string;
  delegatedToParent: boolean;
  beneficiaryMinorUserId?: number;
  beneficiaryMinorName?: string;
  pendingGuardianApproval?: boolean;
  guardianApprovalId?: number;
  guardianApprovalStatus?: GuardianApprovalStatus;
  message?: string;
};

export type DepositRequest = {
  amount: number;
  description?: string;
};

export type PaymentMethodResponse = {
  paymentMethodId: number;
  type: string;
  provider?: string;
  lastFour?: string;
  isDefault: boolean;
};
