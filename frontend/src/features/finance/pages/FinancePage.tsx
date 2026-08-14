import { type MouseEvent, useEffect, useState } from 'react';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { SiteFooter } from '../../home/components/SiteFooter';
import { useAuth } from '../../../shared/auth/AuthProvider';
import '../FinancePage.css';
import { useFinance } from '../hooks/useFinance';
import { WalletBalanceCard } from '../components/WalletBalanceCard';
import { TransactionList } from '../components/TransactionList';
import { DepositModal } from '../components/DepositModal';
import { WithdrawalModal } from '../components/WithdrawalModal';
import { PaymentMethodsPanel } from '../components/PaymentMethodsPanel';
import type { TransactionFilter, WalletInfo } from '../types/financeTypes';

const DEFAULT_FILTERS: TransactionFilter = { page: 0, size: 10 };
const EMPTY_WALLET: WalletInfo = {
  walletId: 0,
  balance: 0,
  availableBalance: 0,
  frozenBalance: 0,
  status: 'ACTIVE',
  updatedAt: '',
};

export default function FinancePage() {
  const { user } = useAuth();
  const {
    wallet,
    walletLoading,
    walletError,
    fetchWallet,
    createWallet,
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
    createPaymentMethod,
    updatePaymentMethod,
    setDefaultPaymentMethod,
    deletePaymentMethod,
    createWithdrawal,
  } = useFinance();

  const [filters, setFilters] = useState<TransactionFilter>(DEFAULT_FILTERS);
  const [creatingWallet, setCreatingWallet] = useState(false);
  const [hasSeenMissingWallet, setHasSeenMissingWallet] = useState(false);
  const walletMissing = Boolean(walletError?.includes('chưa có ví'));
  const showWalletSetup = !wallet && (walletMissing || hasSeenMissingWallet);
  const canTopup = user?.role === 'TUTOR_CENTER';

  useEffect(() => {
    fetchWallet();
  }, [fetchWallet]);

  useEffect(() => {
    if (walletMissing) {
      setHasSeenMissingWallet(true);
    }
    if (wallet) {
      setHasSeenMissingWallet(false);
    }
  }, [wallet, walletMissing]);

  useEffect(() => {
    if (!wallet) return;
    fetchTransactions(DEFAULT_FILTERS);
    fetchPaymentMethods();
  }, [fetchPaymentMethods, fetchTransactions, wallet]);

  function handleFilterChange(newFilters: TransactionFilter) {
    setFilters(newFilters);
    fetchTransactionsPage(newFilters);
  }

  async function handleCreateWallet(event?: MouseEvent<HTMLButtonElement>) {
    event?.preventDefault();
    setCreatingWallet(true);
    try {
      await createWallet();
    } catch {
      // useFinance already maps and stores the API error message in walletError.
    } finally {
      setCreatingWallet(false);
    }
  }

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <main className="finance-page">
        <div className="finance-page__header">
          <h1>Ví của tôi</h1>
          <p>
            {canTopup
              ? 'Ví trung tâm dùng để nạp quỹ vận hành, nhận doanh thu/giải ngân escrow và rút tiền.'
              : 'Ví gia sư dùng để nhận lương, nhận giải ngân escrow và rút tiền.'}
          </p>
        </div>

        {walletError && !showWalletSetup && (
          <div className="alert alert--error">
            {walletError}
            <button onClick={fetchWallet}>Thử lại</button>
          </div>
        )}

        {showWalletSetup ? (
          <section className="wallet-empty">
            <div>
              <p className="wallet-empty__eyebrow">Chưa kích hoạt ví</p>
              <h2>Tạo ví nhận tiền</h2>
              <p>
                Ví sẽ được dùng để nhận tiền lương, nhận khoản giải ngân từ escrow và gửi yêu cầu rút tiền.
              </p>
              {walletError ? <p className="wallet-empty__error">{walletError}</p> : null}
            </div>
            <button
              className="btn btn--primary"
              type="button"
              onClick={handleCreateWallet}
              disabled={creatingWallet || walletLoading}
            >
              {creatingWallet ? 'Đang tạo ví...' : 'Tạo ví'}
            </button>
          </section>
        ) : (
          <>
            <div className="finance-page__balance-row">
              {wallet ? (
                <WalletBalanceCard wallet={wallet} loading={walletLoading} />
              ) : (
                !walletError && <WalletBalanceCard loading wallet={EMPTY_WALLET} />
              )}
              <div className="finance-page__actions">
                {canTopup ? (
                  <DepositModal
                    onCreateTopup={createTopup}
                    onCheckTopupStatus={checkTopupStatus}
                    onSimulateTopupSuccess={simulateTopupSuccess}
                  />
                ) : null}
                <WithdrawalModal
                  wallet={wallet}
                  paymentMethods={paymentMethods}
                  paymentMethodsLoading={paymentMethodsLoading}
                  onLoadPaymentMethods={fetchPaymentMethods}
                  onWithdraw={createWithdrawal}
                />
              </div>
            </div>

            {wallet && (
              <>
                <PaymentMethodsPanel
                  paymentMethods={paymentMethods}
                  loading={paymentMethodsLoading}
                  onLoad={fetchPaymentMethods}
                  onCreate={createPaymentMethod}
                  onUpdate={updatePaymentMethod}
                  onSetDefault={setDefaultPaymentMethod}
                  onDelete={deletePaymentMethod}
                />

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
              </>
            )}
          </>
        )}
      </main>
      <SiteFooter />
    </div>
  );
}
