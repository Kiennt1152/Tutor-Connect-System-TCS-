import type { FormEvent } from 'react';
import { useState } from 'react';
import { SubmittedApprovalsSection } from '../../profile/pages/GuardianApprovalPage';
import { ClientLayout } from '../../profile/components/ClientLayout';
import { LegalDelegationBanner } from '../../profile/components/LegalDelegationBanner';
import { useDependentLinkStatus } from '../../profile/hooks/useDependentProfile';
import { formatCurrency, useFinance } from '../hooks/useFinance';
import '../../profile/pages/DependentProfileLinkerPage.css';

export default function FinancePage() {
  const { linkStatus } = useDependentLinkStatus();
  const { status, errorMessage, wallet, paymentMethods, mutationStatus, mutationError, reload, deposit } =
    useFinance();
  const [amount, setAmount] = useState('');
  const [lastMessage, setLastMessage] = useState<string | null>(null);

  async function handleDeposit(e: FormEvent) {
    e.preventDefault();
    const parsed = Number(amount);
    if (!parsed || parsed <= 0) return;
    const result = await deposit({ amount: parsed });
    if (result?.message) {
      setLastMessage(result.message);
    }
    if (result?.pendingGuardianApproval) {
      setAmount('');
    } else if (result) {
      setAmount('');
    }
  }

  return (
    <ClientLayout title="Thanh toán" subtitle="Quản lý ví và phương thức thanh toán.">
      <LegalDelegationBanner linkStatus={linkStatus} wallet={wallet} />

      {linkStatus?.parentApprovalRequired && (
        <div className="dpl-alert dpl-alert--info">
          Học sinh có thể gửi yêu cầu nạp tiền ngay. Phụ huynh sẽ nhận thông báo qua hệ thống và email để
          xác nhận trước khi giao dịch có hiệu lực.
        </div>
      )}

      {status === 'loading' && <div className="dpl-state">Đang tải ví…</div>}

      {status === 'error' && (
        <div className="dpl-card">
          <div className="dpl-alert dpl-alert--error">{errorMessage}</div>
          <button className="tcs-btn tcs-btn--primary" type="button" onClick={reload}>
            Thử lại
          </button>
        </div>
      )}

      {status === 'success' && wallet && (
        <>
          <div className="dpl-card">
            <h2 className="dpl-section-title">
              {wallet.delegatedToParent ? 'Ví phụ huynh (ủy quyền)' : 'Ví của bạn'}
            </h2>
            <p className="dpl-wallet-balance">{formatCurrency(wallet.availableBalance)}</p>
            <p className="dpl-muted">
              Số dư khả dụng · Trạng thái: {wallet.status}
              {wallet.delegatedToParent && wallet.legalOwnerName
                ? ` · Chủ sở hữu pháp lý: ${wallet.legalOwnerName}`
                : ''}
            </p>
          </div>

          <div className="dpl-card">
            <h2 className="dpl-section-title">Nạp tiền</h2>
            {(lastMessage || wallet.message) && (
              <div className="dpl-alert dpl-alert--info">{lastMessage ?? wallet.message}</div>
            )}
            {mutationError && <div className="dpl-alert dpl-alert--error">{mutationError}</div>}
            <form className="dpl-form" onSubmit={handleDeposit}>
              <label>
                Số tiền (VND) *
                <input
                  className="dpl-field"
                  type="number"
                  min="1000"
                  step="1000"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  required
                />
              </label>
              <button
                className="tcs-btn tcs-btn--primary"
                type="submit"
                disabled={mutationStatus === 'loading'}
              >
                {mutationStatus === 'loading' ? 'Đang gửi…' : 'Gửi yêu cầu nạp tiền'}
              </button>
            </form>
          </div>

          {linkStatus?.parentApprovalRequired && <SubmittedApprovalsSection />}

          <div className="dpl-card">
            <h2 className="dpl-section-title">Phương thức thanh toán</h2>
            {paymentMethods.length === 0 ? (
              <p className="dpl-muted">Chưa có phương thức thanh toán nào.</p>
            ) : (
              <ul className="dpl-child-list">
                {paymentMethods.map((method) => (
                  <li key={method.paymentMethodId} className="dpl-child-item">
                    <div>
                      <strong>{method.type}</strong>
                      {method.provider && (
                        <span className="dpl-child-item__meta"> · {method.provider}</span>
                      )}
                    </div>
                    {method.lastFour && <span className="dpl-muted">****{method.lastFour}</span>}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      )}
    </ClientLayout>
  );
}
