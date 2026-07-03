import { Link } from 'react-router-dom';
import { APP_ROUTES } from '../constants/routes';

export default function ForbiddenPage() {
  return (
    <div className="tcs-page">
      <div className="tcs-container" style={{ padding: '4rem 0', textAlign: 'center' }}>
        <h1 style={{ margin: '0 0 0.5rem', fontSize: '1.75rem' }}>Không có quyền truy cập</h1>
        <p style={{ margin: '0 0 1.5rem', color: 'var(--color-text-secondary)' }}>
          Tài khoản của bạn không được phép mở trang này.
        </p>
        <Link className="tcs-btn tcs-btn--market" to={APP_ROUTES.home}>
          Về trang chủ
        </Link>
      </div>
    </div>
  );
}
