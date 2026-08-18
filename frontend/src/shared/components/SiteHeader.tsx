import { Link } from 'react-router-dom';
import { AppLogo } from './AppLogo';
import { LogoutButton } from './LogoutButton';
import { MessageIcon } from './MessageIcon';
import { NotificationBell } from './NotificationBell';
import { useAuth } from '../auth/AuthProvider';
import { APP_ROUTES } from '../constants/routes';
import { hasAnyRole, hasRole, normalizeRole } from '../auth/rbac';
import type { UserRole } from '../types/userRole';
// Không import CSS trang: mọi class header (tcs-header*, tcs-btn*, tcs-profile-menu*,
// tcs-home-profile-btn*) đều nằm trong theme global (shared/theme/*) nạp ở main.tsx.

// Vai trò được phép cho từng chức năng (khớp với route guards trong App.tsx).
const WALLET_ROLES: UserRole[] = ['TUTOR', 'TUTOR_CENTER'];
const CONTRACT_ROLES: UserRole[] = ['CLIENT', 'TUTOR', 'TUTOR_CENTER'];
const MESSAGING_ROLES: UserRole[] = ['CLIENT', 'TUTOR', 'TUTOR_CENTER', 'PLATFORM_ADMIN'];
const VERIFY_ROLES: UserRole[] = ['TUTOR', 'TUTOR_CENTER'];

/** Khóa nav để đánh dấu link đang active theo trang hiện tại. */
export type SiteHeaderNav =
  | 'find-tutor'
  | 'find-class'
  | 'find-class-tutor'
  | 'marketplace'
  | 'recruitment'
  | 'centers'
  | 'reviews';

interface NavLinkItem {
  readonly key: SiteHeaderNav;
  readonly label: string;
  readonly to: string;
}

const userInitials = (displayName: string | undefined, email: string) => {
  const source = displayName?.trim() || email;
  return source
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('');
};

/**
 * Header dùng chung cho toàn bộ trang (trừ khu vực Admin có header riêng).
 * Nav bên trái hiển thị gọn theo role; các chức năng còn lại luôn truy cập
 * được qua nút bên phải và menu "Hồ sơ" nên không role nào bị mất chức năng.
 */
function buildNavLinks(role: UserRole): NavLinkItem[] {
  // Gia sư: tìm lớp để dạy + tin tuyển dụng của trung tâm.
  if (role === 'TUTOR') {
    return [
      { key: 'find-class-tutor', label: 'Tìm lớp dạy', to: APP_ROUTES.findClass },
      { key: 'recruitment', label: 'Tin tuyển dụng', to: APP_ROUTES.recruitment },
      { key: 'centers', label: 'Trung tâm', to: APP_ROUTES.centers },
    ];
  }
  // Trung tâm: xem danh sách lớp đang mở (gồm lớp do trung tâm tạo) + trang giới thiệu công khai;
  // quản lý sâu qua nút "Quản lý trung tâm".
  if (role === 'TUTOR_CENTER') {
    return [
      { key: 'find-class', label: 'Tìm lớp', to: APP_ROUTES.classFinder },
      { key: 'centers', label: 'Trung tâm', to: APP_ROUTES.centers },
    ];
  }
  // Admin: không có nav khám phá (dùng khu vực quản trị riêng).
  if (role === 'PLATFORM_ADMIN') {
    return [];
  }
  // Phụ huynh/Học viên: khám phá + hub "Yêu cầu của tôi" (đăng lớp, xem gia sư ứng tuyển).
  if (role === 'CLIENT') {
    return [
      { key: 'find-tutor', label: 'Tìm gia sư', to: APP_ROUTES.findTutor },
      { key: 'find-class', label: 'Tìm lớp', to: APP_ROUTES.classFinder },
      { key: 'marketplace', label: 'Yêu cầu của tôi', to: APP_ROUTES.marketplace },
      { key: 'centers', label: 'Trung tâm', to: APP_ROUTES.centers },
    ];
  }
  // Khách chưa đăng nhập: bộ link khám phá.
  return [
    { key: 'find-tutor', label: 'Tìm gia sư', to: APP_ROUTES.findTutor },
    { key: 'find-class', label: 'Tìm lớp', to: APP_ROUTES.classFinder },
    { key: 'centers', label: 'Trung tâm', to: APP_ROUTES.centers },
  ];
}

type SiteHeaderProps = {
  active?: SiteHeaderNav;
};

export function SiteHeader({ active }: SiteHeaderProps) {
  const { user } = useAuth();
  const role = normalizeRole(user?.role);
  const isGuest = !user;

  const profilePath = role === 'PLATFORM_ADMIN' ? APP_ROUTES.platformProfile : APP_ROUTES.profile;
  const showCenterManage = hasRole(role, 'TUTOR_CENTER');
  const showTeaching = hasAnyRole(role, ['TUTOR', 'CLIENT']);
  const showWallet = hasAnyRole(role, WALLET_ROLES);
  const showContract = hasAnyRole(role, CONTRACT_ROLES);
  const showMessaging = hasAnyRole(role, MESSAGING_ROLES);
  const showVerification = hasAnyRole(role, VERIFY_ROLES);
  const showFeedback = hasRole(role, 'CLIENT');
  const showMyReputation = hasRole(role, 'TUTOR');
  const isAdmin = hasRole(role, 'PLATFORM_ADMIN');

  const navLinks = buildNavLinks(role);
  // Chỉ hiển thị hai mục trang chủ (Tin tức/Đánh giá) cho khách & phụ huynh.
  const showHomeAnchors = isGuest || hasRole(role, 'CLIENT');

  return (
    <header className="tcs-header">
      <div className="tcs-container tcs-header__inner">
        <AppLogo href="/" />
        <nav className="tcs-header__nav" aria-label="Primary">
          {navLinks.map((link) => (
            <Link
              key={link.key}
              to={link.to}
              className={[
                'tcs-header__nav-link',
                active === link.key ? 'tcs-header__nav-link--active' : '',
              ].filter(Boolean).join(' ')}
            >
              {link.label}
            </Link>
          ))}
          {showHomeAnchors && (
            <>
              <a className="tcs-header__nav-link tcs-header__nav-link--secondary" href="/#news">
                Tin tức
              </a>
              {/* "Đánh giá" mở trang riêng /danh-gia (danh sách gia sư + số sao). */}
              <Link
                to={APP_ROUTES.tutorReviews}
                className={[
                  'tcs-header__nav-link',
                  'tcs-header__nav-link--secondary',
                  active === 'reviews' ? 'tcs-header__nav-link--active' : '',
                ].filter(Boolean).join(' ')}
              >
                Đánh giá
              </Link>
            </>
          )}
        </nav>
        <div className="tcs-header__actions">
          {user ? (
            <>
              {isAdmin ? (
                <Link className="tcs-btn tcs-btn--ghost tcs-btn--header tcs-header__shortcut" to={APP_ROUTES.platform}>
                  Quản trị
                </Link>
              ) : null}
              {showCenterManage ? (
                <Link className="tcs-btn tcs-btn--ghost tcs-btn--header tcs-header__shortcut" to={APP_ROUTES.center}>
                  Quản lý trung tâm
                </Link>
              ) : null}
              {showTeaching ? (
                <Link className="tcs-btn tcs-btn--ghost tcs-btn--header tcs-header__shortcut" to={APP_ROUTES.teaching}>
                  {hasRole(role, 'CLIENT') ? 'Lịch học' : 'Lịch dạy'}
                </Link>
              ) : null}
              {hasRole(role, 'TUTOR') ? (
                <Link className="tcs-btn tcs-btn--ghost tcs-btn--header tcs-header__shortcut" to="/tutor/schedule">
                  Lịch lớp trung tâm
                </Link>
              ) : null}
              {hasRole(role, 'CLIENT') ? (
                <Link className="tcs-btn tcs-btn--ghost tcs-btn--header tcs-header__shortcut" to="/client/schedule">
                  Lịch lớp trung tâm
                </Link>
              ) : null}
              {showMessaging ? <MessageIcon /> : null}
              <NotificationBell enabled={!!user} />
              <div className="tcs-profile-menu">
                <Link to={profilePath} className="tcs-home-profile-btn" aria-haspopup="menu">
                  <span className="tcs-home-profile-btn__avatar">
                    {userInitials(user.displayName, user.email)}
                  </span>
                  <span className="tcs-home-profile-btn__label">Hồ sơ</span>
                  <span className="tcs-home-profile-btn__chevron" aria-hidden="true">⌄</span>
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
                  {showVerification ? (
                    <Link
                      className="tcs-profile-menu__item"
                      to={APP_ROUTES.verification}
                      role="menuitem"
                    >
                      Xác minh hồ sơ
                    </Link>
                  ) : null}
                  {showFeedback ? (
                    <Link className="tcs-profile-menu__item" to={APP_ROUTES.feedback} role="menuitem">
                      Đánh giá của tôi
                    </Link>
                  ) : null}
                  {showMyReputation ? (
                    <Link
                      className="tcs-profile-menu__item"
                      to={APP_ROUTES.myReputation}
                      role="menuitem"
                    >
                      Nhận xét về tôi
                    </Link>
                  ) : null}
                  {isAdmin ? (
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
