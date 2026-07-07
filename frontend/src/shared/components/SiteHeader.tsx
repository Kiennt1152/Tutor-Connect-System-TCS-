import { Link } from 'react-router-dom';
import { AppLogo } from './AppLogo';
import { LogoutButton } from './LogoutButton';
import { useAuth } from '../auth/AuthProvider';
import { APP_ROUTES } from '../constants/routes';
import { hasAnyRole, hasRole } from '../auth/rbac';
import type { UserRole } from '../types/userRole';

const CENTER_MANAGE_ROLES: UserRole[] = ['TUTOR_CENTER'];
const WALLET_ROLES: UserRole[] = ['CLIENT', 'TUTOR', 'TUTOR_CENTER'];

const userInitials = (displayName: string | undefined, email: string) => {
  const source = displayName?.trim() || email;
  return source
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
};

/** Header chung cho các trang đã đăng nhập (trang chủ, ví…). Nav trỏ về mục trên trang chủ. */
export function SiteHeader() {
  const { user } = useAuth();

  const profilePath =
    user?.role === 'PLATFORM_ADMIN' ? APP_ROUTES.platformProfile : APP_ROUTES.profile;
  const showCenterManage = hasAnyRole(user?.role, CENTER_MANAGE_ROLES);

  return (
    <header className="tcs-header">
      <div className="tcs-container tcs-header__inner">
        <AppLogo href="/" />
        <nav className="tcs-header__nav">
          <a href="/#find-tutor">Tìm gia sư</a>
          <a href="/#classes">Tìm lớp</a>
          <a href="/#centers">Trung tâm</a>
          <a href="/#news">Tin tức</a>
          <a href="/#reviews">Đánh giá</a>
        </nav>
        <div className="tcs-header__actions">
          {user ? (
            <>
              {hasRole(user.role, 'PLATFORM_ADMIN') ? (
                <Link className="tcs-btn tcs-btn--ghost tcs-btn--header" to={APP_ROUTES.platform}>
                  Quản trị
                </Link>
              ) : null}
              {showCenterManage ? (
                <Link className="tcs-btn tcs-btn--ghost tcs-btn--header" to={APP_ROUTES.center}>
                  Quản lý trung tâm
                </Link>
              ) : null}
              {hasAnyRole(user.role, WALLET_ROLES) ? (
                <Link className="tcs-btn tcs-btn--ghost tcs-btn--header" to={APP_ROUTES.finance}>
                  Ví của tôi
                </Link>
              ) : null}
              {hasRole(user.role, 'PLATFORM_ADMIN') ? (
                <Link to={profilePath} className="tcs-home-profile-btn">
                  <span className="tcs-home-profile-btn__avatar">
                    {userInitials(user.displayName, user.email)}
                  </span>
                  <span className="tcs-home-profile-btn__label">Hồ sơ</span>
                </Link>
              ) : (
                <span className="tcs-home-profile-btn tcs-home-profile-btn--disabled" title="Sắp có">
                  <span className="tcs-home-profile-btn__avatar">
                    {userInitials(user.displayName, user.email)}
                  </span>
                  <span className="tcs-home-profile-btn__label">Hồ sơ</span>
                </span>
              )}
              <LogoutButton />
            </>
          ) : (
            <>
              <a className="tcs-btn tcs-btn--ghost tcs-btn--header" href="/login">
                Đăng nhập
              </a>
              <a className="tcs-btn tcs-btn--market tcs-btn--header" href="/register">
                Đăng ký
              </a>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
