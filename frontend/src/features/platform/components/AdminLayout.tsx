import type { ReactNode } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AppLogo } from '../../../shared/components/AppLogo';
import { useAuth } from '../../../shared/auth/AuthProvider';
import './AdminLayout.css';

type AdminLayoutProps = {
  title: string;
  subtitle?: string;
  children: ReactNode;
};

export function AdminLayout({ title, subtitle, children }: AdminLayoutProps) {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="adm-page">
      <header className="adm-topbar">
        <div className="adm-topbar__inner">
          <AppLogo />
          <nav className="adm-topbar__nav">
            <Link className="adm-topbar__link adm-topbar__link--active" to="/platform/users">
              Quản lý người dùng
            </Link>
          </nav>
          <div className="adm-topbar__actions">
            <Link className="tcs-btn tcs-btn--ghost tcs-btn--sm" to="/">
              Trang chủ
            </Link>
            <button className="tcs-btn tcs-btn--ghost tcs-btn--sm" type="button" onClick={handleLogout}>
              Đăng xuất
            </button>
          </div>
        </div>
      </header>
      <header className="adm-header">
        <div className="adm-header__inner">
          <div>
            <h1 className="adm-header__title">{title}</h1>
            {subtitle && <p className="adm-header__subtitle">{subtitle}</p>}
          </div>
        </div>
      </header>
      <main className="adm-main">{children}</main>
    </div>
  );
}
