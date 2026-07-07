import { Link } from 'react-router-dom';
import { AdminLayout } from '../components/AdminLayout';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { APP_ROUTES } from '../../../shared/constants/routes';
import './PlatformProfilePage.css';

const initials = (name: string, email: string) => {
  const source = name.trim() || email;
  return source
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
};

export default function PlatformProfilePage() {
  const { user } = useAuth();

  if (!user) {
    return null;
  }

  const displayName = user.displayName?.trim() || user.email;

  return (
    <AdminLayout
      title="Hồ sơ quản trị viên"
      subtitle="Quản lý thông tin tài khoản và cài đặt bảo mật."
    >
      <div className="adm-profile">
        <section className="adm-profile-hero adm-card">
          <div className="adm-profile-hero__avatar">{initials(displayName, user.email)}</div>
          <div className="adm-profile-hero__info">
            <h2 className="adm-profile-hero__name">{displayName}</h2>
            <p className="adm-profile-hero__email">{user.email}</p>
            <span className="adm-profile-hero__badge">Quản trị viên nền tảng</span>
          </div>
        </section>

        <div className="adm-profile-grid">
          <section className="adm-card adm-profile-panel">
            <h3 className="adm-profile-panel__title">Thông tin cá nhân</h3>
            <dl className="adm-profile-fields">
              <div className="adm-profile-field">
                <dt>Họ tên hiển thị</dt>
                <dd>{displayName}</dd>
              </div>
              <div className="adm-profile-field">
                <dt>Email</dt>
                <dd>{user.email}</dd>
              </div>
              <div className="adm-profile-field">
                <dt>Vai trò</dt>
                <dd>PLATFORM_ADMIN</dd>
              </div>
            </dl>
            <button type="button" className="tcs-btn tcs-btn--soft" disabled title="Sắp có">
              Chỉnh sửa thông tin
            </button>
          </section>

          <section className="adm-card adm-profile-panel">
            <h3 className="adm-profile-panel__title">Bảo mật</h3>
            <p className="adm-profile-panel__desc">
              Đổi mật khẩu và quản lý phiên đăng nhập sẽ được bổ sung trong phiên bản tiếp theo.
            </p>
            <button type="button" className="tcs-btn tcs-btn--ghost" disabled title="Sắp có">
              Đổi mật khẩu
            </button>
          </section>
        </div>

        <p className="adm-profile-back">
          <Link to={APP_ROUTES.platform}>← Quay lại tổng quan</Link>
        </p>
      </div>
    </AdminLayout>
  );
}
