export interface WalletInfo {
  walletId: number;
  balance?: number;
  availableBalance: number;
  frozenBalance: number;
  status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
  updatedAt: string;
}

export interface Transaction {
  transactionId: number;
  type:
    | 'DEPOSIT'
    | 'WITHDRAWAL'
    | 'REFUND'
    | 'ESCROW_DEPOSIT'
    | 'ESCROW_RELEASE';
  status: 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';
  amount: number;
  description: string | null;
  referenceCode: string | null;
  processedAt: string | null;
  createdAt: string;
}

export interface TransactionPage {
  transactions: Transaction[];
  page: number;
  totalPages: number;
  totalElements: number;
}

export interface TransactionFilter {
  page?: number;
  size?: number;
  type?: string;
  from?: string;
  to?: string;
}

export interface DepositPayload {
  amount: number;
  description?: string;
}

export type TopupStatus =
  | 'PENDING'
  | 'SUCCESS'
  | 'EXPIRED'
  | 'FAILED'
  | 'CANCELLED'
  | string;

export interface TopupSessionInfo {
  reference: string;
  amount: number;
  status: TopupStatus;
  qrUrl: string;
  bankName: string;
  bankBin: string;
  accountNumber: string;
  accountName: string;
  transferContent: string;
  expiresAt: string;
  expiresAtMillis: number;
}

export interface TopupStatusInfo {
  reference: string;
  status: TopupStatus;
  message: string;
  wallet?: WalletInfo;
}

export interface PaymentMethodInfo {
  paymentMethodId: number;
  type: string;
  provider: string | null;
  lastFour: string | null;
  isDefault: boolean;
}

export interface WithdrawalPayload {
  amount: number;
  paymentMethodId?: number;
  bankName?: string;
  accountNo?: string;
}

export interface WithdrawalInfo {
  withdrawalId: number;
  amount: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED' | string;
  paymentMethodId: number;
  bankName: string | null;
  accountNoMasked: string;
  referenceCode: string;
  requestedAt: string;
  wallet: WalletInfo;
}
