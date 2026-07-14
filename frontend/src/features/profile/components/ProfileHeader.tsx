import { Link } from 'react-router-dom';
import type { ProfileResponse } from '../types/profileTypes';
import { ROLE_LABEL, VERIFICATION_LABEL } from './profileConstants';

interface ProfileHeaderProps {
  profile: ProfileResponse | null;
  showVerification?: boolean;
}

export default function ProfileHeader({ profile, showVerification = false }: ProfileHeaderProps) {
  return (
    <header className="profile-header">
      <div style={{ marginBottom: '1rem' }}>
        <Link
          to="/"
          className="btn-secondary"
          style={{ display: 'inline-block', textDecoration: 'none', padding: '0.5rem 1rem' }}
        >
          &larr; Về trang chủ
        </Link>
      </div>
      <h1>Hồ sơ cá nhân</h1>
      {profile && (
        <p className="profile-role">
          Vai trò: <strong>{ROLE_LABEL[profile.role] ?? profile.role}</strong>
          {showVerification && profile.verificationStatus && (
            <>
              {' · '}
              <span
                className={`verification-badge verification-${profile.verificationStatus.toLowerCase()}`}
              >
                {VERIFICATION_LABEL[profile.verificationStatus]}
              </span>
            </>
          )}
        </p>
      )}
    </header>
  );
}
