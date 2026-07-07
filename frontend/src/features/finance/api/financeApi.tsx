import axiosClient from '../../../shared/api/axiosClient';
import type {
  WalletInfo,
  TransactionPage,
  TransactionFilter,
  DepositPayload,
} from '../types/financeTypes';

const BASE = '/finance';

export const financeApi = {
  http: axiosClient,

  getWallet(): Promise<WalletInfo> {
    return axiosClient.get(`${BASE}/wallet`).then((r) => r.data);
  },

  getTransactions(params: TransactionFilter): Promise<TransactionPage> {
    return axiosClient
      .get(`${BASE}/wallet/transactions`, { params })
      .then((r) => r.data);
  },

  deposit(payload: DepositPayload): Promise<WalletInfo> {
    return axiosClient.post(`${BASE}/wallet/deposit`, payload).then((r) => r.data);
  },
};
