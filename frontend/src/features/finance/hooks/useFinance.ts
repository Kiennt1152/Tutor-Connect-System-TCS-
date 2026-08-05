import { useCallback, useState } from 'react';
import { financeApi } from '../api/financeApi';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import type {
  WalletInfo,
  TransactionPage,
  TransactionFilter,
  DepositPayload,
  TopupSessionInfo,
  TopupStatusInfo,
  PaymentMethodInfo,
  PaymentMethodPayload,
  WithdrawalPayload,
  WithdrawalInfo,
} from '../types/financeTypes';

export function useFinance() {
  const [wallet, setWallet] = useState<WalletInfo | null>(null);
  const [walletLoading, setWalletLoading] = useState(false);
  const [walletError, setWalletError] = useState<string | null>(null);

  const [transactions, setTransactions] = useState<TransactionPage | null>(null);
  const [txLoading, setTxLoading] = useState(false);
  const [txError, setTxError] = useState<string | null>(null);

  const [paymentMethods, setPaymentMethods] = useState<PaymentMethodInfo[]>([]);
  const [paymentMethodsLoading, setPaymentMethodsLoading] = useState(false);

  // ── Wallet ──────────────────────────────────────────────────────────────

  const fetchWallet = useCallback(async () => {
    setWalletLoading(true);
    setWalletError(null);
    try {
      const data = await financeApi.getWallet();
      setWallet(data);
    } catch (err: unknown) {
      setWalletError(getApiErrorMessage(err, 'Không thể tải thông tin ví'));
    } finally {
      setWalletLoading(false);
    }
  }, []);

  // ── Transactions ────────────────────────────────────────────────────────

  const fetchTransactions = useCallback(async (filters: TransactionFilter = {}) => {
    setTxLoading(true);
    setTxError(null);
    try {
      const data = await financeApi.getTransactions({
        page: 0,
        size: 20,
        ...filters,
      });
      setTransactions(data);
    } catch (err: unknown) {
      setTxError(
        getApiErrorMessage(err, 'Không thể tải lịch sử giao dịch')
      );
    } finally {
      setTxLoading(false);
    }
  }, []);

  const fetchTransactionsPage = useCallback(
    async (filters: TransactionFilter) => {
      setTxLoading(true);
      setTxError(null);
      try {
        const data = await financeApi.getTransactions(filters);
        setTransactions(data);
      } catch (err: unknown) {
        setTxError(
          getApiErrorMessage(err, 'Không thể tải trang giao dịch')
        );
      } finally {
        setTxLoading(false);
      }
    },
    []
  );

  // ── Deposit ─────────────────────────────────────────────────────────────

  const deposit = useCallback(async (payload: DepositPayload): Promise<boolean> => {
    try {
      const updated = await financeApi.deposit(payload);
      setWallet(updated);
      await fetchTransactions({ page: 0, size: 20 });
      return true;
    } catch {
      return false;
    }
  }, [fetchTransactions]);

  const createTopup = useCallback(async (
    payload: DepositPayload
  ): Promise<TopupSessionInfo> => {
    return financeApi.createTopup(payload);
  }, []);

  const checkTopupStatus = useCallback(async (
    reference: string
  ): Promise<TopupStatusInfo> => {
    const data = await financeApi.getTopupStatus(reference);
    if (data.wallet) {
      setWallet(data.wallet);
      await fetchTransactions({ page: 0, size: 20 });
    }
    return data;
  }, [fetchTransactions]);

  const simulateTopupSuccess = useCallback(async (
    reference: string
  ): Promise<TopupStatusInfo> => {
    const data = await financeApi.simulateTopupSuccess(reference);
    if (data.wallet) {
      setWallet(data.wallet);
      await fetchTransactions({ page: 0, size: 20 });
    }
    return data;
  }, [fetchTransactions]);

  // ── Payment methods & withdrawals ───────────────────────────────────────

  const fetchPaymentMethods = useCallback(async () => {
    setPaymentMethodsLoading(true);
    try {
      const data = await financeApi.getPaymentMethods();
      setPaymentMethods(data);
    } finally {
      setPaymentMethodsLoading(false);
    }
  }, []);

  const createPaymentMethod = useCallback(async (
    payload: PaymentMethodPayload
  ): Promise<PaymentMethodInfo> => {
    const data = await financeApi.createPaymentMethod(payload);
    await fetchPaymentMethods();
    return data;
  }, [fetchPaymentMethods]);

  const updatePaymentMethod = useCallback(async (
    paymentMethodId: number,
    payload: PaymentMethodPayload
  ): Promise<PaymentMethodInfo> => {
    const data = await financeApi.updatePaymentMethod(paymentMethodId, payload);
    await fetchPaymentMethods();
    return data;
  }, [fetchPaymentMethods]);

  const deletePaymentMethod = useCallback(async (
    paymentMethodId: number
  ): Promise<void> => {
    await financeApi.deletePaymentMethod(paymentMethodId);
    await fetchPaymentMethods();
  }, [fetchPaymentMethods]);

  const createWithdrawal = useCallback(async (
    payload: WithdrawalPayload
  ): Promise<WithdrawalInfo> => {
    const data = await financeApi.createWithdrawal(payload);
    setWallet(data.wallet);
    await fetchTransactions({ page: 0, size: 20 });
    await fetchPaymentMethods();
    return data;
  }, [fetchPaymentMethods, fetchTransactions]);

  return {
    // wallet
    wallet,
    walletLoading,
    walletError,
    fetchWallet,
    // transactions
    transactions,
    txLoading,
    txError,
    fetchTransactions,
    fetchTransactionsPage,
    // deposit
    deposit,
    createTopup,
    checkTopupStatus,
    simulateTopupSuccess,
    // withdrawals
    paymentMethods,
    paymentMethodsLoading,
    fetchPaymentMethods,
    createPaymentMethod,
    updatePaymentMethod,
    deletePaymentMethod,
    createWithdrawal,
  };
}
