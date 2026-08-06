import { Link } from 'react-router-dom';
import { AppLogo } from './AppLogo';
import { LogoutButton } from './LogoutButton';
import { NotificationBell } from './NotificationBell';
import { useAuth } from '../auth/AuthProvider';
import { hasAnyRole, hasRole } from '../auth/rbac';
import { APP_ROUTES } from '../constants/routes';
import type { UserRole } from '../types/userRole';

const CENTER_MANAGE_ROLES: UserRole[] = ['TUTOR_CENTER'];
const WALLET_ROLES: UserRole[] = ['TUTOR', 'TUTOR_CENTER'];
const CONTRACT_ROLES: UserRole[] = ['CLIENT', 'TUTOR', 'TUTOR_CENTER'];
const MESSAGING_ROLES: UserRole[] = ['CLIENT', 'TUTOR', 'TUTOR_CENTER', 'PLATFORM_ADMIN'];
const FEEDBACK_ROLES: UserRole[] = ['CLIENT'];
const REPUTATION_ROLES: UserRole[] = ['TUTOR'];

function userInitials(displayName: string | undefined, email: string): string {
  const source = displayName?.trim() || email;
  return source
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
}

export function VerificationHeader() {
  const { user } = useAuth();

  const profilePath =
    user?.role === 'PLATFORM_ADMIN' ? APP_ROUTES.platformProfile : APP_ROUTES.profile;
  const showCenterManage = hasAnyRole(user?.role, CENTER_MANAGE_ROLES);
  const centersHref = hasRole(user?.role, 'TUTOR') ? APP_ROUTES.recruitment : APP_ROUTES.centers;
  const showTutorSchedule = hasRole(user?.role, 'TUTOR');
  const showWallet = hasAnyRole(user?.role, WALLET_ROLES);
  const showContract = hasAnyRole(user?.role, CONTRACT_ROLES);
  const showMessaging = hasAnyRole(user?.role, MESSAGING_ROLES);
  const showFeedback = hasAnyRole(user?.role, FEEDBACK_ROLES);
  const showMyReputation = hasAnyRole(user?.role, REPUTATION_ROLES);

  return (
    <header className="tcs-header">
      <div className="tcs-container tcs-header__inner">
        <AppLogo href="/" />
        <nav className="tcs-header__nav" aria-label="Primary">
          <Link to="/#find-tutor">Tìm gia sư</Link>
          <Link to={APP_ROUTES.marketplace}>Tìm lớp</Link>
          <Link to={centersHref}>Trung tâm</Link>
          <Link to="/#news">Tin tức</Link>
          <Link to="/#reviews">Đánh giá</Link>
        </nav>
        <div className="tcs-header__actions">
          {user ? (
            <>
              <NotificationBell enabled={!!user} />
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
              {showTutorSchedule ? (
                <Link className="tcs-btn tcs-btn--ghost tcs-btn--header" to={APP_ROUTES.teaching}>
                  Lịch dạy
                </Link>
              ) : null}
              <div className="tcs-profile-menu">
                <Link to={profilePath} className="tcs-home-profile-btn" aria-haspopup="menu">
                  <span className="tcs-home-profile-btn__avatar">
                    {userInitials(user.displayName, user.email)}
                  </span>
                  <span className="tcs-home-profile-btn__label">Hồ sơ</span>
                </Link>
                <div className="tcs-profile-menu__dropdown" role="menu">
                  <Link className="tcs-profile-menu__item" to={profilePath} role="menuitem">
                    Hồ sơ của tôi
                  </Link>
                  {showWallet ? (
                    <Link className="tcs-profile-menu__item" to={APP_ROUTES.finance} role="menuitem">
                      Ví của tôi
                    </Link>
                  ) : null}
                  {showContract ? (
                    <Link className="tcs-profile-menu__item" to={APP_ROUTES.contract} role="menuitem">
                      Hợp đồng
                    </Link>
                  ) : null}
                  {showMessaging ? (
                    <Link className="tcs-profile-menu__item" to={APP_ROUTES.messaging} role="menuitem">
                      Thông báo
                    </Link>
                  ) : null}
                  {showFeedback ? (
                    <Link className="tcs-profile-menu__item" to={APP_ROUTES.feedback} role="menuitem">
                      Đánh giá của tôi
                    </Link>
                  ) : null}
                  {showMyReputation ? (
                    <Link className="tcs-profile-menu__item" to={APP_ROUTES.myReputation} role="menuitem">
                      Nhận xét về tôi
                    </Link>
                  ) : null}
                  {showCenterManage ? (
                    <Link className="tcs-profile-menu__item" to={APP_ROUTES.center} role="menuitem">
                      Quản lý trung tâm
                    </Link>
                  ) : null}
                  {hasRole(user.role, 'PLATFORM_ADMIN') ? (
                    <Link className="tcs-profile-menu__item" to={APP_ROUTES.platform} role="menuitem">
                      Bảng quản trị
                    </Link>
                  ) : null}
                </div>
              </div>
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
