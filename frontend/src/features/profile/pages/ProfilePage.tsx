import { Link } from 'react-router-dom';
import { SiteHeader } from '../../home/components/SiteHeader';
import { SiteFooter } from '../../home/components/SiteFooter';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { APP_ROUTES } from '../../../shared/constants/routes';
import '../../home/pages/HomePage.css';
import './ProfilePage.css';

const ROLE_LABELS: Record<string, string> = {
  CLIENT: 'Phụ huynh / Học viên',
  TUTOR: 'Gia sư',
  TUTOR_CENTER: 'Trung tâm gia sư',
  PLATFORM_ADMIN: 'Quản trị viên',
};

const initialsOf = (name: string) =>
  name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p.charAt(0).toUpperCase())
    .join('');

export default function ProfilePage() {
  const { user } = useAuth();
  const isClient = user?.role === 'CLIENT';
  const isTutor = user?.role === 'TUTOR';
  const displayName = user?.displayName?.trim() || user?.email || 'Người dùng';

  return (
    <div className="tcs-page">
      <SiteHeader />
      <main>
        <div className="tcs-container profile-page">
          <h1 className="profile-page__title">Hồ sơ của tôi</h1>

          <div className="profile-card">
            <div className="profile-card__avatar">{initialsOf(displayName)}</div>
            <div className="profile-card__info">
              <div className="profile-card__name">{displayName}</div>
              {user?.email && <div className="profile-card__email">{user.email}</div>}
              {user?.role && (
                <span className="profile-card__role">{ROLE_LABELS[user.role] ?? user.role}</span>
              )}
            </div>
          </div>

          {isClient && (
            <div className="profile-section">
              <h2 className="profile-section__title">Yêu cầu tìm gia sư</h2>
              <p className="profile-section__desc">
                Quản lý các yêu cầu tìm gia sư bạn đã đăng — xem, chỉnh sửa và đăng công khai.
              </p>
              <Link className="tcs-btn tcs-btn--market profile-cta" to={APP_ROUTES.marketplace}>
                📋 Xem yêu cầu của tôi
              </Link>
            </div>
          )}

          {isClient && (
            <div className="profile-section">
              <h2 className="profile-section__title">Nhận xét gia sư</h2>
              <p className="profile-section__desc">
                Gửi đánh giá và phản hồi cho gia sư của các lớp học đã hoàn thành.
              </p>
              <Link className="tcs-btn tcs-btn--market profile-cta" to={APP_ROUTES.feedback}>
                ⭐ Nhận xét gia sư
              </Link>
            </div>
          )}

          {isTutor && (
            <div className="profile-section">
              <h2 className="profile-section__title">Nhận xét về tôi</h2>
              <p className="profile-section__desc">
                Xem tổng hợp đánh giá và phản hồi mà khách hàng đã gửi cho bạn.
              </p>
              <Link className="tcs-btn tcs-btn--market profile-cta" to={APP_ROUTES.myReputation}>
                ⭐ Xem nhận xét về tôi
              </Link>
            </div>
          )}
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}
