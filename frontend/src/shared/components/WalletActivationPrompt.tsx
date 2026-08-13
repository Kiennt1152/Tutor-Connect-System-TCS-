import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { useLocation, useNavigate } from 'react-router-dom';
import { financeApi } from '../../features/finance/api/financeApi';
import { profileApi } from '../../features/profile/api/profileApi';
import { getApiErrorMessage } from '../api/apiError';
import { useAuth } from '../auth/AuthProvider';
import { APP_ROUTES } from '../constants/routes';
import type { UserRole } from '../types/userRole';
import './WalletActivationPrompt.css';

const WALLET_ROLES: UserRole[] = ['TUTOR', 'TUTOR_CENTER'];

export function WalletActivationPrompt() {
  const { user, authLoading } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  const currentRole = user?.role;
  const shouldCheckWallet =
    Boolean(user)
    && !authLoading
    && currentRole !== undefined
    && WALLET_ROLES.includes(currentRole)
    && !location.pathname.startsWith(APP_ROUTES.finance);

  useEffect(() => {
    if (!shouldCheckWallet) {
      setOpen(false);
      return;
    }

    let cancelled = false;

    async function checkVerifiedWallet() {
      try {
        const profile = await profileApi.getMyProfile();
        if (cancelled) return;

        if (profile.verificationStatus !== 'VERIFIED') {
          setOpen(false);
          return;
        }

        try {
          await financeApi.getWallet();
          if (!cancelled) {
            setOpen(false);
          }
        } catch (error: unknown) {
          if (cancelled) return;
          const message = getApiErrorMessage(error, '');
          setOpen(message.toLowerCase().includes('chưa có ví'));
        }
      } catch {
        if (!cancelled) {
          setOpen(false);
        }
      }
    }

    void checkVerifiedWallet();
    return () => {
      cancelled = true;
    };
  }, [shouldCheckWallet, user?.userId, location.pathname]);

  if (!open) {
    return null;
  }

  return createPortal(
    <div className="wallet-activation-prompt" role="presentation">
      <section
        className="wallet-activation-prompt__card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="wallet-activation-title"
      >
        <p className="wallet-activation-prompt__eyebrow">Ví nhận tiền</p>
        <h2 id="wallet-activation-title">Vui lòng tạo ví trước khi tiếp tục</h2>
        <p>
          Tài khoản của bạn đã được TCS xác minh. Hãy tạo ví để nhận tiền giải ngân escrow và
          gửi yêu cầu rút tiền khi cần.
        </p>
        <button
          className="tcs-btn tcs-btn--primary wallet-activation-prompt__action"
          type="button"
          onClick={() => navigate(APP_ROUTES.finance)}
        >
          Đi tới tạo ví
        </button>
      </section>
    </div>,
    document.body,
  );
}
