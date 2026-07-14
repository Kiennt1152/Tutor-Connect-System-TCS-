import { Link } from 'react-router-dom';
import type { ProfileResponse } from '../types/profileTypes';
import { VERIFICATION_LABEL } from './profileConstants';

interface VerificationSectionProps {
  profile: ProfileResponse | null;
  onSubmit: () => void | Promise<unknown>;
}

export default function VerificationSection({ profile, onSubmit }: VerificationSectionProps) {
  if (!profile?.verificationStatus) return null;

  return (
    <section className="profile-section">
      <h2>Xác minh hồ sơ</h2>
      {profile.verificationStatus === 'VERIFIED' ? (
        <p>Hồ sơ của bạn đã được admin xác minh.</p>
      ) : (
        <>
          <p>
            Trạng thái hiện tại:{' '}
            <strong>{VERIFICATION_LABEL[profile.verificationStatus]}</strong>
          </p>
          {profile.verificationStatus === 'UNDER_VERIFY' && <p>Hồ sơ đang được xét duyệt.</p>}
          {profile.verificationStatus === 'REJECTED' && (
            <p>Hồ sơ bị từ chối. Vui lòng cập nhật thông tin rồi nộp lại.</p>
          )}
          <button type="button" className="btn-primary" onClick={() => void onSubmit()}>
            Nộp hồ sơ xác minh
          </button>
        </>
      )}
      <p className="profile-hint">
        <Link to="/profile/verification">Xem chi tiết hồ sơ xác minh</Link>
      </p>
    </section>
  );
}
