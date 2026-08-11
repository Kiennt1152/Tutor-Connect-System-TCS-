import { Link, NavLink } from 'react-router-dom';
import type { ReactNode } from 'react';
import { AppLogo } from '../../../shared/components/AppLogo';
import { LogoutButton } from '../../../shared/components/LogoutButton';
import { MessageIcon } from '../../../shared/components/MessageIcon';
import { NotificationBell } from '../../../shared/components/NotificationBell';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { ADMIN_NAV_GROUPS } from '../config/adminNavConfig';
import { AdminIcon, IconUser } from './AdminIcons';
import './AdminLayout.css';

type AdminLayoutProps = {
  title: string;
  subtitle?: string;
  children: ReactNode;
};

const userInitials = (displayName: string | undefined, email: string) => {
  const source = displayName?.trim() || email;
  return source
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
};

export function AdminLayout({ title, subtitle, children }: AdminLayoutProps) {
  const { user } = useAuth();

  return (
    <div className="adm-page">
      <header className="adm-topbar">
        <div className="adm-topbar__inner">
          <AppLogo href={APP_ROUTES.platform} className="adm-topbar__logo" />
          <div className="adm-topbar__actions">
            <MessageIcon />
            <NotificationBell enabled={!!user} />
            <NavLink
              to={APP_ROUTES.platformProfile}
              className={({ isActive }) =>
                `adm-profile-btn${isActive ? ' adm-profile-btn--active' : ''}`
              }
              title="Quản lý hồ sơ"
            >
              <span className="adm-profile-btn__avatar">
                {userInitials(user?.displayName, user?.email ?? 'A')}
              </span>
              <span className="adm-profile-btn__text">
                <span className="adm-profile-btn__label">Hồ sơ</span>
                <span className="adm-profile-btn__name">
                  {user?.displayName?.trim() || user?.email || 'Admin'}
                </span>
              </span>
              <IconUser className="adm-profile-btn__icon" />
            </NavLink>
            <Link to={APP_ROUTES.home} className="tcs-btn tcs-btn--ghost tcs-btn--header adm-topbar__home">
              Trang chủ
            </Link>
            <LogoutButton />
          </div>
        </div>
      </header>

      <div className="adm-shell">
        <aside className="adm-sidebar">
          {ADMIN_NAV_GROUPS.map((group, index) => (
            <div key={group.label} className="adm-sidebar__group">
              {index > 0 ? (
                <div className="adm-sidebar__divider" role="separator" aria-hidden="true" />
              ) : null}
              <p className="adm-sidebar__label">{group.label}</p>
              <nav className="adm-nav" aria-label={group.label}>
                {group.items.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) =>
                      `adm-nav__link${isActive ? ' adm-nav__link--active' : ''}`
                    }
                  >
                    <span className="adm-nav__icon">
                      <AdminIcon name={item.icon} />
                    </span>
                    {item.label}
                  </NavLink>
                ))}
              </nav>
            </div>
          ))}
        </aside>

        <div className="adm-content">
          <header className="adm-header">
            <div className="adm-header__inner">
              <h1 className="adm-header__title">{title}</h1>
              {subtitle ? <p className="adm-header__subtitle">{subtitle}</p> : null}
            </div>
          </header>
          <main className="adm-main">{children}</main>
        </div>
      </div>
    </div>
  );
}
