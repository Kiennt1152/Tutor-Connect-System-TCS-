import { Link } from 'react-router-dom';
import { AppLogo } from '../../../shared/components/AppLogo';
import { LogoutButton } from '../../../shared/components/LogoutButton';
import { MessageIcon } from '../../../shared/components/MessageIcon';
import { NotificationBell } from '../../../shared/components/NotificationBell';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { usePlatformDashboard } from '../../platform/hooks/usePlatformDashboard';
import { ADMIN_HOME_NAV, ADMIN_PRIORITY_ITEMS, ADMIN_QUICK_ACTIONS, ADMIN_STATS } from '../../platform/config/adminNavConfig';
import { AdminIcon } from '../../platform/components/AdminIcons';
import { getAuthenticatedHeroCopy } from '../config/homeQuickActions';
import './HomePage.css';
import './AdminHomePage.css';

const formatCount = (value: unknown) =>
  new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(typeof value === 'number' ? value : 0);

const userInitials = (displayName: string | undefined, email: string) => {
  const source = displayName?.trim() || email;
  return source
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
};

const ADMIN_NAV = ADMIN_HOME_NAV;

function AdminHeader() {
  const { user } = useAuth();

  return (
    <header className="tcs-header admin-home__header">
      <div className="tcs-container tcs-header__inner">
        <AppLogo href="/" />
        <nav className="tcs-header__nav admin-home__nav">
          {ADMIN_NAV.map((item) => (
            <Link key={item.to} to={item.to}>
              {item.label}
            </Link>
          ))}
        </nav>
        <div className="tcs-header__actions">
          <MessageIcon />
          <NotificationBell enabled={!!user} />
          <div className="tcs-profile-menu">
            <Link to={APP_ROUTES.platformProfile} className="tcs-home-profile-btn" aria-haspopup="menu">
              <span className="tcs-home-profile-btn__avatar">
                {userInitials(user?.displayName, user?.email ?? 'A')}
              </span>
              <span className="tcs-home-profile-btn__label">Hồ sơ</span>
              <span className="tcs-home-profile-btn__chevron" aria-hidden="true">⌄</span>
            </Link>
            <div className="tcs-profile-menu__dropdown" role="menu">
              <Link className="tcs-profile-menu__item" to={APP_ROUTES.platformProfile} role="menuitem">
                Hồ sơ của tôi
              </Link>
              <Link className="tcs-profile-menu__item" to={APP_ROUTES.platform} role="menuitem">
                Bảng quản trị
              </Link>
              <Link className="tcs-profile-menu__item" to={APP_ROUTES.platformReports} role="menuitem">
                Báo cáo hệ thống
              </Link>
            </div>
          </div>
          <LogoutButton />
        </div>
      </div>
    </header>
  );
}

export default function AdminHomePage() {
  const { user } = useAuth();
  const { status, data, reload } = usePlatformDashboard();
  const displayName = user?.displayName?.trim() || user?.email?.split('@')[0] || 'Admin';
  const copy = getAuthenticatedHeroCopy('PLATFORM_ADMIN');
  const quickActions = ADMIN_QUICK_ACTIONS;

  return (
    <div className="tcs-page admin-home">
      <AdminHeader />
      <main className="admin-home__main">
        <section className="admin-home__hero">
          <div className="tcs-container">
            <p className="admin-home__eyebrow">{copy.eyebrow}</p>
            <h1 className="admin-home__title">Xin chào, {displayName}</h1>
            <p className="admin-home__subtitle">{copy.subtitle}</p>
            <div className="admin-home__hero-actions">
              <Link className="tcs-btn tcs-btn--market" to={APP_ROUTES.platform}>
                Vào bảng điều khiển
              </Link>
            </div>
          </div>
        </section>

        {status === 'loading' && (
          <div className="tcs-container">
            <div className="admin-home__state">
              <span className="tcs-spinner" aria-hidden="true" />
              Đang tải số liệu nền tảng…
            </div>
          </div>
        )}

        {status === 'error' && (
          <div className="tcs-container">
            <div className="admin-home__state admin-home__state--error">
              <p>Không tải được số liệu dashboard.</p>
              <button type="button" className="tcs-btn tcs-btn--ghost" onClick={reload}>
                Thử lại
              </button>
            </div>
          </div>
        )}

        {status === 'success' && data && (
          <>
            {(data.pendingVerifications > 0 || data.openReports > 0) && (
              <section className="admin-home__priority">
                <div className="tcs-container">
                  <h2 className="admin-home__section-title">Cần xử lý</h2>
                  <div className="admin-home__priority-grid">
                    {ADMIN_PRIORITY_ITEMS.map((item) =>
                      data[item.key] > 0 ? (
                        <Link key={item.key} className="admin-home__priority-card" to={item.to}>
                          <span className="admin-home__priority-icon">
                            <AdminIcon name={item.icon} size="lg" />
                          </span>
                          <div>
                            <p className="admin-home__priority-label">{item.label}</p>
                            <p className="admin-home__priority-value">
                              {formatCount(data[item.key])}
                            </p>
                          </div>
                        </Link>
                      ) : null,
                    )}
                  </div>
                </div>
              </section>
            )}

            <section className="admin-home__stats">
              <div className="tcs-container">
                <div className="admin-home__stats-grid">
                  {ADMIN_STATS.map((stat) => (
                    <article
                      key={stat.key}
                      className={`admin-home__stat${stat.tone === 'warn' ? ' admin-home__stat--warn' : ''}`}
                    >
                      <span className="admin-home__stat-icon">
                        <AdminIcon name={stat.icon} size="md" />
                      </span>
                      <p className="admin-home__stat-label">{stat.label}</p>
                      <p className="admin-home__stat-value">{formatCount(data[stat.key])}</p>
                    </article>
                  ))}
                </div>
              </div>
            </section>
          </>
        )}

        <section className="admin-home__actions">
          <div className="tcs-container">
            <h2 className="admin-home__section-title">Chức năng quản trị</h2>
            <p className="admin-home__section-desc">Truy cập nhanh các module vận hành hệ thống.</p>
            <div className="admin-home__action-grid">
              {quickActions.map((action) => (
                <Link key={action.title} className="admin-home__action-card" to={action.to}>
                  <span className="admin-home__action-icon">
                    <AdminIcon name={action.icon} size="md" />
                  </span>
                  <div>
                    <h3 className="admin-home__action-title">{action.title}</h3>
                    <p className="admin-home__action-desc">{action.description}</p>
                  </div>
                </Link>
              ))}
            </div>
          </div>
        </section>
      </main>

      <footer className="tcs-footer admin-home__footer">
        <div className="tcs-container">
          <div className="admin-home__footer-inner">
            <AppLogo href="/" variant="compact" />
            <span className="admin-home__footer-note">
              © {new Date().getFullYear()} Tutor Connect System · Khu vực quản trị
            </span>
          </div>
        </div>
      </footer>
    </div>
  );
}
