import { useEffect, useState } from 'react';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import '../FinancePage.css';
import { useFinance } from '../hooks/useFinance';
import { WalletBalanceCard } from '../components/WalletBalanceCard';
import { TransactionList } from '../components/TransactionList';
import { DepositModal } from '../components/DepositModal';
import { WithdrawalModal } from '../components/WithdrawalModal';
import type { TransactionFilter, WalletInfo } from '../types/financeTypes';

const DEFAULT_FILTERS: TransactionFilter = { page: 0, size: 20 };
const EMPTY_WALLET: WalletInfo = {
  walletId: 0,
  balance: 0,
  availableBalance: 0,
  frozenBalance: 0,
  status: 'ACTIVE',
  updatedAt: '',
};

export default function FinancePage() {
  const {
    wallet,
    walletLoading,
    walletError,
    fetchWallet,
    transactions,
    txLoading,
    txError,
    fetchTransactions,
    fetchTransactionsPage,
    createTopup,
    checkTopupStatus,
    simulateTopupSuccess,
    paymentMethods,
    paymentMethodsLoading,
    fetchPaymentMethods,
    createWithdrawal,
  } = useFinance();

  const [filters, setFilters] = useState<TransactionFilter>(DEFAULT_FILTERS);

  useEffect(() => {
    fetchWallet();
    fetchTransactions(DEFAULT_FILTERS);
  }, [fetchWallet, fetchTransactions]);

  function handleFilterChange(newFilters: TransactionFilter) {
    setFilters(newFilters);
    fetchTransactionsPage(newFilters);
  }

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <main className="finance-page">
        <div className="finance-page__header">
          <h1>Ví của tôi</h1>
        </div>

        {walletError && (
          <div className="alert alert--error">
            {walletError}
            <button onClick={fetchWallet}>Thử lại</button>
          </div>
        )}

        <div className="finance-page__balance-row">
          {wallet ? (
            <WalletBalanceCard wallet={wallet} loading={walletLoading} />
          ) : (
            !walletError && <WalletBalanceCard loading wallet={EMPTY_WALLET} />
          )}
          <div className="finance-page__actions">
            <DepositModal
              onCreateTopup={createTopup}
              onCheckTopupStatus={checkTopupStatus}
              onSimulateTopupSuccess={simulateTopupSuccess}
            />
            <WithdrawalModal
              wallet={wallet}
              paymentMethods={paymentMethods}
              paymentMethodsLoading={paymentMethodsLoading}
              onLoadPaymentMethods={fetchPaymentMethods}
              onWithdraw={createWithdrawal}
            />
          </div>
        </div>

        <section className="finance-page__transactions">
          <h2>Lịch sử giao dịch</h2>

          {txError && (
            <div className="alert alert--error">
              {txError}
              <button onClick={() => fetchTransactionsPage(filters)}>Thử lại</button>
            </div>
          )}

          {transactions ? (
            <TransactionList
              page={transactions}
              loading={txLoading}
              filters={filters}
              onFilterChange={handleFilterChange}
            />
          ) : (
            !txError && (
              <div className="tx-list__loading">
                <div className="spinner" />
              </div>
            )
          )}
        </section>
      </main>
    </div>
  );
}
