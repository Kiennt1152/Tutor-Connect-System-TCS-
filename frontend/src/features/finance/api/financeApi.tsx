import axiosClient from '../../../shared/api/axiosClient';
import type {
  DepositResponse,
  PaymentMethod,
  Transaction,
  Wallet,
  Withdrawal,
} from '../types/financeTypes';

export const FINANCE_API_BASE = '/finance';

export const financeApi = {
  getWallet: () =>
    axiosClient.get<Wallet>(`${FINANCE_API_BASE}/wallet`).then((res) => res.data),

  createDeposit: (amount: number, description?: string) =>
    axiosClient
      .post<DepositResponse>(`${FINANCE_API_BASE}/wallet/deposit`, { amount, description })
      .then((res) => res.data),

  getTransactions: () =>
    axiosClient
      .get<Transaction[]>(`${FINANCE_API_BASE}/transactions`)
      .then((res) => res.data),

  getPaymentMethods: () =>
    axiosClient
      .get<PaymentMethod[]>(`${FINANCE_API_BASE}/payment-methods`)
      .then((res) => res.data),

  addPaymentMethod: (bankName: string, accountNo: string, accountName: string) =>
    axiosClient
      .post<PaymentMethod>(`${FINANCE_API_BASE}/payment-methods`, {
        bankName,
        accountNo,
        accountName,
      })
      .then((res) => res.data),

  deletePaymentMethod: (id: number) =>
    axiosClient.delete(`${FINANCE_API_BASE}/payment-methods/${id}`).then((res) => res.data),

  getWithdrawals: () =>
    axiosClient
      .get<Withdrawal[]>(`${FINANCE_API_BASE}/withdrawals`)
      .then((res) => res.data),

  createWithdrawal: (amount: number, paymentMethodId: number) =>
    axiosClient
      .post<Withdrawal>(`${FINANCE_API_BASE}/withdrawals`, { amount, paymentMethodId })
      .then((res) => res.data),
};
