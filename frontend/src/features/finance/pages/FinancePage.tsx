import { useEffect, useState } from 'react';
import '../FinancePage.css';
import { useFinance } from '../hooks/useFinance';
import { WalletBalanceCard } from '../components/WalletBalanceCard';
import { TransactionList } from '../components/TransactionList';
import { DepositModal } from '../components/DepositModal';
import type { TransactionFilter } from '../types/financeTypes';

const DEFAULT_FILTERS: TransactionFilter = { page: 0, size: 20 };

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
    deposit,
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
    <div className="finance-page">
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
          !walletError && <WalletBalanceCard loading wallet={{} as never} />
        )}
        <div className="finance-page__actions">
          <DepositModal onDeposit={deposit} />
          <button className="withdraw-btn" disabled>
            Rút tiền (sắp ra mắt)
          </button>
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
    </div>
  );
}
