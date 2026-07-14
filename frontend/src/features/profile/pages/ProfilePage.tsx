import { useProfile } from '../hooks/useProfile';
import ProfileHeader from '../components/ProfileHeader';
import ClientProfilePage from './ClientProfilePage';
import TutorProfilePage from './TutorProfilePage';
import CenterProfilePage from './CenterProfilePage';
import './ProfilePage.css';

export default function ProfilePage() {
  const ctx = useProfile();
  const { profile, loading, error } = ctx;

  if (loading && !profile) {
    return (
      <div className="profile-page">
        <ProfileHeader profile={null} />
        <p>Đang tải hồ sơ...</p>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="profile-page">
        <ProfileHeader profile={null} />
        {error && <div className="profile-alert error">{error}</div>}
      </div>
    );
  }

  switch (profile.role) {
    case 'CLIENT':
      return <ClientProfilePage ctx={ctx} />;
    case 'TUTOR':
      return <TutorProfilePage ctx={ctx} />;
    case 'TUTOR_CENTER':
      return <CenterProfilePage ctx={ctx} />;
    default:
      return (
        <div className="profile-page">
          <ProfileHeader profile={profile} />
          <div className="profile-alert error">
            Vai trò này chưa hỗ trợ chỉnh sửa hồ sơ tại đây.
          </div>
        </div>
      );
  }
}
