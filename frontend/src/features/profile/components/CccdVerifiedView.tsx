import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { profileApi } from '../api/profileApi';
import type { CccdInfo } from '../types/profileTypes';
import '../pages/ProfilePage.css';

const RO: React.CSSProperties = { background: '#f1f5f9', color: '#334155' };

type Props = {
  /** Hồ sơ đã được admin xác minh (VERIFIED). */
  verified: boolean;
  /** Trung tâm -> CCCD là của người chịu trách nhiệm. */
  isCenter?: boolean;
};

/** Hiển thị CCCD ở dạng CHỈ ĐỌC cho gia sư/trung tâm (nộp khi xác minh, không sửa ở profile). */
export function CccdVerifiedView({ verified, isCenter }: Props) {
  const [info, setInfo] = useState<CccdInfo | null>(null);

  useEffect(() => {
    profileApi
      .getMyCccd()
      .then((res) => setInfo(res.data))
      .catch(() => setInfo(null));
  }, []);

  const hasCccd = Boolean(info?.cccdNumber);

  return (
    <section className="profile-section">
      <h2>
        Thông tin CCCD{isCenter ? ' (người chịu trách nhiệm)' : ''}
        <span
          style={{
            fontSize: 13,
            fontWeight: 600,
            padding: '2px 10px',
            borderRadius: 999,
            marginLeft: 8,
            background: verified && hasCccd ? '#dcfce7' : '#fef9c3',
            color: verified && hasCccd ? '#166534' : '#854d0e',
          }}
        >
          {verified && hasCccd ? '✓ Đã xác minh CCCD' : hasCccd ? 'Chờ xác minh' : 'Chưa nộp'}
        </span>
      </h2>

      {!hasCccd ? (
        <p className="profile-hint">
          Thông tin CCCD được nộp trong quá trình{' '}
          <Link to={APP_ROUTES.verification}>xác minh hồ sơ</Link>. Sau khi xác minh, thông tin sẽ hiển
          thị ở đây.
        </p>
      ) : (
        <>
          <p className="profile-hint">
            Thông tin CCCD lấy từ hồ sơ xác minh (đọc từ QR) — chỉ xem, không sửa tại đây.
          </p>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <label>
              Họ và tên
              <input value={info?.fullName ?? ''} readOnly style={RO} />
            </label>
            <label>
              Số CCCD
              <input value={info?.cccdNumber ?? ''} readOnly style={RO} />
            </label>
            <label>
              Ngày sinh
              <input value={info?.dateOfBirth ?? ''} readOnly style={RO} />
            </label>
            <label>
              Giới tính
              <input value={info?.gender ?? ''} readOnly style={RO} />
            </label>
            <label>
              Ngày cấp
              <input value={info?.issueDate ?? ''} readOnly style={RO} />
            </label>
            <label>
              Nơi cấp
              <input value={info?.issuePlace ?? ''} readOnly style={RO} />
            </label>
            <label style={{ gridColumn: '1 / -1' }}>
              Địa chỉ thường trú
              <input value={info?.permanentAddress ?? ''} readOnly style={RO} />
            </label>
          </div>
        </>
      )}
    </section>
  );
}
