import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../shared/auth/AuthProvider';

type AdminLayoutProps = {
  title: string;
  subtitle?: string;
  children: ReactNode;
};

export function AdminLayout({ title, subtitle, children }: AdminLayoutProps) {
  const { logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/');
  }

  return (
    <div className="adm-page">
      <header className="adm-header">
        <div className="adm-header__inner">
          <div>
            <h1 className="adm-header__title">{title}</h1>
            {subtitle && <p className="adm-header__subtitle">{subtitle}</p>}
          </div>
          {isAuthenticated && (
            <button className="tcs-btn tcs-btn--ghost" type="button" onClick={handleLogout}>
              Đăng xuất
            </button>
          )}
        </div>
      </header>
      <main className="adm-main">{children}</main>
    </div>
  );
}
