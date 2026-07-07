import { useCallback, useEffect, useRef, useState } from 'react';
import { financeApi } from '../api/financeApi';
import type {
  DepositResponse,
  PaymentMethod,
  Transaction,
  Wallet,
  Withdrawal,
} from '../types/financeTypes';

export type FinanceStatus = 'loading' | 'success' | 'error';

export function useFinance() {
  const [status, setStatus] = useState<FinanceStatus>('loading');
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethod[]>([]);
  const [withdrawals, setWithdrawals] = useState<Withdrawal[]>([]);
  const pollRef = useRef<number | null>(null);

  const refresh = useCallback(async () => {
    try {
      const [w, txs, pms, wds] = await Promise.all([
        financeApi.getWallet(),
        financeApi.getTransactions(),
        financeApi.getPaymentMethods(),
        financeApi.getWithdrawals(),
      ]);
      setWallet(w);
      setTransactions(txs);
      setPaymentMethods(pms);
      setWithdrawals(wds);
      setStatus('success');
    } catch (error) {
      console.error('Lỗi tải ví:', error);
      setStatus('error');
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const stopPolling = useCallback(() => {
    if (pollRef.current !== null) {
      window.clearInterval(pollRef.current);
      pollRef.current = null;
    }
  }, []);

  /** Poll trạng thái tới khi giao dịch nạp chuyển SUCCESS/FAILED (SePay xác nhận), tối đa ~2 phút. */
  const watchDeposit = useCallback(
    (transactionId: number, onSettled: (settled: Transaction) => void) => {
      stopPolling();
      let elapsed = 0;
      pollRef.current = window.setInterval(async () => {
        elapsed += 3;
        try {
          const [w, txs] = await Promise.all([
            financeApi.getWallet(),
            financeApi.getTransactions(),
          ]);
          setWallet(w);
          setTransactions(txs);
          const tx = txs.find((t) => t.transactionId === transactionId);
          if (tx && tx.status !== 'PENDING') {
            stopPolling();
            onSettled(tx);
          }
        } catch (error) {
          console.error('Lỗi kiểm tra giao dịch:', error);
        }
        if (elapsed >= 120) {
          stopPolling();
        }
      }, 3000);
    },
    [stopPolling],
  );

  useEffect(() => stopPolling, [stopPolling]);

  const createDeposit = useCallback(
    (amount: number, description?: string): Promise<DepositResponse> =>
      financeApi.createDeposit(amount, description),
    [],
  );

  const addPaymentMethod = useCallback(
    async (bankName: string, accountNo: string, accountName: string) => {
      const pm = await financeApi.addPaymentMethod(bankName, accountNo, accountName);
      setPaymentMethods((prev) => [...prev, pm]);
      return pm;
    },
    [],
  );

  const deletePaymentMethod = useCallback(async (id: number) => {
    await financeApi.deletePaymentMethod(id);
    setPaymentMethods((prev) => prev.filter((p) => p.paymentMethodId !== id));
  }, []);

  const createWithdrawal = useCallback(
    async (amount: number, paymentMethodId: number): Promise<Withdrawal> => {
      const wd = await financeApi.createWithdrawal(amount, paymentMethodId);
      await refresh();
      return wd;
    },
    [refresh],
  );

  return {
    status,
    wallet,
    transactions,
    paymentMethods,
    withdrawals,
    refresh,
    createDeposit,
    watchDeposit,
    stopPolling,
    addPaymentMethod,
    deletePaymentMethod,
    createWithdrawal,
  };
}
