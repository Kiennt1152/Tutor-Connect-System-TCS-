import './DependentProfileGate.css';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { APP_ROUTES } from '../constants/routes';
import { useDependentLinkStatus } from '../../features/profile/hooks/useDependentProfile';

type DependentProfileGateProps = {
  children: ReactNode;
};

/**
 * Chặn CLIENT vị thành niên chưa liên kết phụ huynh khỏi màn hình thanh toán / hợp đồng.
 */
export function DependentProfileGate({ children }: DependentProfileGateProps) {
  const { status, linkStatus, errorMessage } = useDependentLinkStatus();

  if (status === 'loading' || status === 'idle') {
    return (
      <div className="dpl-gate">
        <p>Đang kiểm tra điều kiện liên kết hồ sơ…</p>
      </div>
    );
  }

  if (status === 'error') {
    return (
      <div className="dpl-gate">
        <p>{errorMessage ?? 'Không kiểm tra được trạng thái liên kết.'}</p>
        <Link className="tcs-btn tcs-btn--primary" to={APP_ROUTES.profileDependents}>
          Đến màn hình liên kết hồ sơ
        </Link>
      </div>
    );
  }

  if (linkStatus && !(linkStatus.profileLinkComplete ?? linkStatus.canProceedToPayment)) {
    return (
      <div className="dpl-gate">
        <h2>Cần hoàn tất liên kết hồ sơ</h2>
        {linkStatus.dateOfBirthMissing && (
          <p>Vui lòng cập nhật ngày sinh trước khi tiếp tục.</p>
        )}
        {linkStatus.guardianRequired && !linkStatus.guardianLinked && (
          <p>
            Tài khoản học sinh vị thành niên cần liên kết hồ sơ phụ huynh trước khi thanh toán hoặc
            tạo hợp đồng với gia sư.
          </p>
        )}
        <Link className="tcs-btn tcs-btn--primary" to={APP_ROUTES.profileDependents}>
          Liên kết hồ sơ ngay
        </Link>
      </div>
    );
  }

  return <>{children}</>;
}
