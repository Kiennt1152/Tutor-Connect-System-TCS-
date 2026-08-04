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
const TEACHING_ROLES: UserRole[] = ['TUTOR', 'CLIENT'];

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
  const showTeaching = hasAnyRole(user?.role, TEACHING_ROLES);

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
