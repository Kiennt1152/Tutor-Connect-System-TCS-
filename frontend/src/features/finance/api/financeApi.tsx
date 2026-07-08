import axiosClient from '../../../shared/api/axiosClient';
import type {
  WalletInfo,
  TransactionPage,
  TransactionFilter,
  DepositPayload,
  TopupSessionInfo,
  TopupStatusInfo,
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

  createTopup(payload: DepositPayload): Promise<TopupSessionInfo> {
    return axiosClient.post(`${BASE}/wallet/topups`, payload).then((r) => r.data);
  },

  getTopupStatus(reference: string): Promise<TopupStatusInfo> {
    return axiosClient
      .get(`${BASE}/wallet/topups/${encodeURIComponent(reference)}`)
      .then((r) => r.data);
  },

  simulateTopupSuccess(reference: string): Promise<TopupStatusInfo> {
    return axiosClient
      .post(`${BASE}/wallet/topups/${encodeURIComponent(reference)}/simulate-success`)
      .then((r) => r.data);
  },
};
