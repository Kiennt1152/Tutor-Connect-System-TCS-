import type { WalletInfo } from '../types/financeTypes';
import { formatCurrency, walletStatusLabel } from '../mappers/financeMapper';

interface Props {
  wallet: WalletInfo;
  loading?: boolean;
}

export function WalletBalanceCard({ wallet, loading }: Props) {
  const availableBalance = wallet.balance ?? wallet.availableBalance;

  if (loading) {
    return (
      <div className="wallet-balance-card wallet-balance-card--loading">
        <div className="skeleton skeleton--text skeleton--lg" />
        <div className="skeleton skeleton--text skeleton--md" />
      </div>
    );
  }

  return (
    <div className="wallet-balance-card">
      <div className="wallet-balance-card__header">
        <span className="wallet-balance-card__label">Số dư khả dụng</span>
        <span className={`wallet-balance-card__status wallet-balance-card__status--${wallet.status.toLowerCase()}`}>
          {walletStatusLabel(wallet.status)}
        </span>
      </div>
      <div className="wallet-balance-card__available">
        {formatCurrency(availableBalance)}
      </div>
      {wallet.frozenBalance > 0 && (
        <div className="wallet-balance-card__frozen">
          <span className="wallet-balance-card__frozen-label">Đang bị khóa</span>
          <span className="wallet-balance-card__frozen-amount">
            {formatCurrency(wallet.frozenBalance)}
          </span>
        </div>
      )}
    </div>
  );
}
