import { Link } from 'react-router-dom';
import { AppLogo } from './AppLogo';
import { LogoutButton } from './LogoutButton';
import { useAuth } from '../auth/AuthProvider';
import { APP_ROUTES } from '../constants/routes';
import { hasAnyRole, hasRole } from '../auth/rbac';
import type { UserRole } from '../types/userRole';

const CENTER_MANAGE_ROLES: UserRole[] = ['TUTOR_CENTER'];

function userInitials(displayName: string | undefined, email: string): string {
  const source = displayName?.trim() || email;
  return source
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
}

/**
 * Header cho trang Tutor Verification.
 * Style đồng bộ với homepage (orange theme), dùng `tcs-header` tokens.
 */
export function VerificationHeader() {
  const { user } = useAuth();

  const profilePath =
    user?.role === 'PLATFORM_ADMIN' ? APP_ROUTES.platformProfile : APP_ROUTES.profile;
  const showCenterManage = hasAnyRole(user?.role, CENTER_MANAGE_ROLES);
  const showVerification = hasRole(user?.role, 'TUTOR');

  return (
    <header className="tcs-header">
      <div className="tcs-container tcs-header__inner">
        <AppLogo href="/" />
        <nav className="tcs-header__nav" aria-label="Primary">
          <Link to={APP_ROUTES.catalog}>Tìm gia sư</Link>
          <Link to={APP_ROUTES.marketplace}>Tìm lớp</Link>
          {showCenterManage ? <Link to={APP_ROUTES.center}>Trung tâm</Link> : null}
          {showVerification ? <Link to="/tutor/schedule">Lịch dạy</Link> : null}
          {showVerification ? <Link to={APP_ROUTES.verification}>Xác minh</Link> : null}
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
              <Link to={profilePath} className="tcs-home-profile-btn">
                <span className="tcs-home-profile-btn__avatar">
                  {userInitials(user.displayName, user.email)}
                </span>
                <span className="tcs-home-profile-btn__label">Hồ sơ</span>
              </Link>
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