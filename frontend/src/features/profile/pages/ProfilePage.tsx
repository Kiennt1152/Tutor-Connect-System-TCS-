/**
 * ============================================================================
 * [UC-08] TRANG QUẢN LÝ HỒ SƠ CÁ NHÂN & ĐIỀU PHỐI (USER PROFILE DISPATCHER)
 * ============================================================================
 * Tác giả       : mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo      : 2026-06-23
 * 
 * 1. Mục đích & Chức năng:
 *    - Là trang trung tâm điều phối giao diện hồ sơ người dùng theo vai trò (Role-Based Profile Dispatcher).
 *    - Phân tách hiển thị tương ứng: ClientProfilePage (Phụ huynh), TutorProfilePage (Gia sư), CenterProfilePage (Trung tâm).
 *    - Quản lý trạng thái loading, kiểm tra xác thực và tự động điều hướng hoàn tất hồ sơ lần đầu (First-login Onboarding).
 * 
 * 2. Luồng xử lý chính:
 *    - Bước 1: Gọi useProfile() để lấy thông tin tài khoản người dùng hiện tại từ /api/profile/me.
 *    - Bước 2: Dựa vào role (ROLE_CLIENT, ROLE_TUTOR, ROLE_TUTOR_CENTER) để render giao diện phù hợp.
 * ============================================================================
 */

import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { SiteFooter } from '../../home/components/SiteFooter';
import { useProfile } from '../hooks/useProfile';
import ClientProfilePage from './ClientProfilePage';
import TutorProfilePage from './TutorProfilePage';
import CenterProfilePage from './CenterProfilePage';
import './ProfilePage.css';

export default function ProfilePage() {
  const ctx = useProfile();
  const { profile, loading, error, reload } = ctx;

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <main className="profile-main">
        <div className="tcs-container profile-page">
          <header className="profile-header">
            <h1>Hồ sơ cá nhân</h1>
          </header>

          {loading && !profile && (
            <div className="profile-loading" style={{ textAlign: 'center', padding: '3rem 0', color: '#64748b' }}>
              <p>Đang tải thông tin hồ sơ...</p>
            </div>
          )}

          {!loading && error && !profile && (
            <div className="profile-alert error" style={{ textAlign: 'center' }}>
              <p>{error}</p>
              <button type="button" onClick={() => void reload()} className="btn-secondary" style={{ marginTop: '0.75rem' }}>
                Thử lại
              </button>
            </div>
          )}

          {profile && (
            <>
              {profile.role === 'CLIENT' && <ClientProfilePage ctx={ctx} />}
              {profile.role === 'TUTOR' && <TutorProfilePage ctx={ctx} />}
              {profile.role === 'TUTOR_CENTER' && <CenterProfilePage ctx={ctx} />}
              {profile.role !== 'CLIENT' && profile.role !== 'TUTOR' && profile.role !== 'TUTOR_CENTER' && (
                <div className="profile-alert warning">
                  <p>Hồ sơ người dùng với vai trò này hiện không được cấu hình chỉnh sửa tại trang cá nhân.</p>
                </div>
              )}
            </>
          )}
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}
