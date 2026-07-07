export type WalletStatus = 'ACTIVE' | 'LOCKED' | 'CLOSED';

export type TransactionType =
  | 'DEPOSIT'
  | 'WITHDRAWAL'
  | 'REFUND'
  | 'ESCROW_DEPOSIT'
  | 'ESCROW_RELEASE';

export type TransactionStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';

export type WithdrawalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'COMPLETED';

export interface Wallet {
  walletId: number;
  availableBalance: number;
  frozenBalance: number;
  status: WalletStatus;
  updatedAt: string;
}

export interface DepositResponse {
  transactionId: number;
  referenceCode: string;
  amount: number;
  status: TransactionStatus;
  qrImageUrl: string;
  bankName: string;
  accountNo: string;
  accountName: string;
  transferContent: string;
}

export interface Transaction {
  transactionId: number;
  type: TransactionType;
  status: TransactionStatus;
  amount: number;
  description?: string;
  referenceCode?: string;
  createdAt: string;
  processedAt?: string;
}

export interface PaymentMethod {
  paymentMethodId: number;
  type: string;
  bankName: string;
  accountNo: string;
  accountName: string;
}

export interface Withdrawal {
  withdrawalId: number;
  amount: number;
  status: WithdrawalStatus;
  bankName?: string;
  accountNo?: string;
  accountName?: string;
  requestedAt: string;
  processedAt?: string;
  failureReason?: string;
  direct: boolean;
}

export interface AdminWithdrawal {
  withdrawalId: number;
  userId: number;
  userEmail: string;
  amount: number;
  status: WithdrawalStatus;
  bankName?: string;
  accountNo?: string;
  accountName?: string;
  requestedAt: string;
  processedAt?: string;
  failureReason?: string;
}
