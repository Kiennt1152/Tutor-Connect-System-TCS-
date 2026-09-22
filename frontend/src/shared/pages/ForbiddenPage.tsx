/**
 * ====================================================================================================
 * [UC-09] MÀN HÌNH GIAO DIỆN FORBIDDENPAGE
 * ====================================================================================================
 * Nghiệp vụ chính:
 * 1. Hiển thị và điều phối các chức năng nghiệp vụ của phân hệ ForbiddenPage.
 * 2. Đảm bảo trải nghiệm người dùng tối ưu và đồng bộ dữ liệu với hệ thống Backend.
 * * @author Nguyễn Trung Kiên (Kiennt1152)
 */
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
