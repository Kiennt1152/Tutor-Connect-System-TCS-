import { Link } from 'react-router-dom';
import { AppLogo } from '../../../shared/components/AppLogo';
import { LogoutButton } from '../../../shared/components/LogoutButton';
import { NotificationBell } from '../../../shared/components/NotificationBell';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { hasAnyRole, hasRole } from '../../../shared/auth/rbac';
import type { UserRole } from '../../../shared/types/userRole';
import '../pages/HomePage.css';

const CENTER_MANAGE_ROLES: UserRole[] = ['TUTOR_CENTER'];
const WALLET_ROLES: UserRole[] = ['TUTOR', 'TUTOR_CENTER'];
const CONTRACT_ROLES: UserRole[] = ['CLIENT', 'TUTOR', 'TUTOR_CENTER'];
const MESSAGING_ROLES: UserRole[] = ['CLIENT', 'TUTOR', 'TUTOR_CENTER', 'PLATFORM_ADMIN'];
const VERIFICATION_ROLES: UserRole[] = ['CLIENT', 'TUTOR', 'TUTOR_CENTER'];
const TEACHING_ROLES: UserRole[] = ['TUTOR', 'CLIENT'];
const FEEDBACK_ROLES: UserRole[] = ['CLIENT'];
const REPUTATION_ROLES: UserRole[] = ['TUTOR'];

const userInitials = (displayName: string | undefined, email: string) => {
  const source = displayName?.trim() || email;
  return source
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
};

type SiteHeaderProps = {
  active?: 'find-tutor';
};

export function SiteHeader({ active }: SiteHeaderProps) {
  const { user } = useAuth();

  const profilePath =
    user?.role === 'PLATFORM_ADMIN' ? APP_ROUTES.platformProfile : APP_ROUTES.profile;
  const showCenterManage = hasAnyRole(user?.role, CENTER_MANAGE_ROLES);
  const showWallet = hasAnyRole(user?.role, WALLET_ROLES);
  const showContract = hasAnyRole(user?.role, CONTRACT_ROLES);
  const showMessaging = hasAnyRole(user?.role, MESSAGING_ROLES);
  const showVerification = hasAnyRole(user?.role, VERIFICATION_ROLES);
  const showTeaching = hasAnyRole(user?.role, TEACHING_ROLES);
  const showFeedback = hasAnyRole(user?.role, FEEDBACK_ROLES);
  const showMyReputation = hasAnyRole(user?.role, REPUTATION_ROLES);

  return (
    <header className="tcs-header">
      <div className="tcs-container tcs-header__inner">
        <AppLogo href="/" />
        <nav className="tcs-header__nav">
          <Link
            to={APP_ROUTES.findTutor}
            className={active === 'find-tutor' ? 'tcs-header__nav-link--active' : undefined}
          >
            Tìm gia sư
          </Link>
          <Link to={APP_ROUTES.findClass}>Tìm lớp</Link>
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
              {showTeaching ? (
                <Link className="tcs-btn tcs-btn--ghost tcs-btn--header" to={APP_ROUTES.teaching}>
                  {hasRole(user.role, 'CLIENT') ? 'Lịch học' : 'Lịch dạy'}
                  {/* Lời mời nhận lớp giờ báo ở chuông thông báo, không còn badge ở đây. */}
                </Link>
              ) : null}
              <NotificationBell enabled={!!user} />
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
                  {showVerification ? (
                    <Link className="tcs-profile-menu__item" to={APP_ROUTES.verification} role="menuitem">
                      {hasRole(user.role, 'CLIENT') ? 'Xác minh danh tính' : 'Xác minh hồ sơ'}
                    </Link>
                  ) : null}
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
                  {showCenterManage ? (
                    <Link className="tcs-profile-menu__item" to={APP_ROUTES.center} role="menuitem">
                      Quản lý trung tâm
                    </Link>
                  ) : null}
                  {showTeaching ? (
                    <Link className="tcs-profile-menu__item" to={APP_ROUTES.teaching} role="menuitem">
                      {hasRole(user.role, 'CLIENT') ? 'Lịch học' : 'Lịch dạy'}
                    </Link>
                  ) : null}
                  {hasRole(user.role, 'PLATFORM_ADMIN') ? (
                    <Link className="tcs-profile-menu__item" to={APP_ROUTES.platform} role="menuitem">
                      Bảng quản trị
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
