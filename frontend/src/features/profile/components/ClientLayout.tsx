import type { ReactNode } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { APP_ROUTES } from '../../../shared/constants/routes';
import '../pages/DependentProfileLinkerPage.css';

type ClientLayoutProps = {
  title: string;
  subtitle?: string;
  children: ReactNode;
};

export function ClientLayout({ title, subtitle, children }: ClientLayoutProps) {
  const { logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate(APP_ROUTES.home);
  }

  return (
    <div className="client-page">
      <header className="client-header">
        <div className="client-header__inner">
          <div>
            <h1 className="client-header__title">{title}</h1>
            {subtitle && <p className="client-header__subtitle">{subtitle}</p>}
          </div>
          <nav className="client-header__nav">
            <Link to={APP_ROUTES.home}>Trang chủ</Link>
            <Link to={APP_ROUTES.profileDependents}>Liên kết hồ sơ</Link>
            <Link to={APP_ROUTES.guardianApprovals}>Xác nhận phụ huynh</Link>
            {isAuthenticated && (
              <button className="client-header__logout" type="button" onClick={handleLogout}>
                Đăng xuất
              </button>
            )}
          </nav>
        </div>
      </header>
      <main className="client-main">{children}</main>
    </div>
  );
}
