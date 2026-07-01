import type { DependentLinkStatus } from '../types/profileTypes';
import type { WalletResponse } from '../../finance/types/financeTypes';

type LegalDelegationBannerProps = {
  linkStatus?: DependentLinkStatus | null;
  wallet?: WalletResponse | null;
};

export function LegalDelegationBanner({ linkStatus, wallet }: LegalDelegationBannerProps) {
  const delegated =
    linkStatus?.legalProceduresDelegatedToParent || wallet?.delegatedToParent || false;

  if (!delegated) {
    return null;
  }

  const holderName =
    wallet?.legalOwnerName ||
    linkStatus?.legalAccountHolderName ||
    'phụ huynh liên kết';
  const beneficiaryName = wallet?.beneficiaryMinorName;

  return (
    <div className="dpl-alert dpl-alert--info" role="status">
      <strong>Thủ tục pháp lý qua tài khoản phụ huynh</strong>
      <p className="dpl-muted">
        Thanh toán và hợp đồng
        {beneficiaryName ? ` cho ${beneficiaryName}` : ''} do phụ huynh{' '}
        <strong>{holderName}</strong> xác nhận sau khi học sinh gửi yêu cầu.
      </p>
    </div>
  );
}
