import axiosClient from '../../../shared/api/axiosClient';
import type { DepositRequest, PaymentMethodResponse, WalletResponse } from '../types/financeTypes';

export const FINANCE_API_BASE = '/finance';

export const financeApi = {
  getWallet: () => axiosClient.get<WalletResponse>(`${FINANCE_API_BASE}/wallet`),
  deposit: (body: DepositRequest) =>
    axiosClient.post<WalletResponse>(`${FINANCE_API_BASE}/wallet/deposit`, body),
  getPaymentMethods: () =>
    axiosClient.get<PaymentMethodResponse[]>(`${FINANCE_API_BASE}/payment-methods`),
};
