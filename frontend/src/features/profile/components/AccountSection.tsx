import type { ProfileResponse } from '../types/profileTypes';

interface AccountSectionProps {
  profile: ProfileResponse | null;
  phone: string;
  address: string;
  phoneError?: string;
  addressError?: string;
  onPhoneChange: (value: string) => void;
  onAddressChange: (value: string) => void;
}

export default function AccountSection({
  profile,
  phone,
  address,
  phoneError,
  addressError,
  onPhoneChange,
  onAddressChange,
}: AccountSectionProps) {
  return (
    <section className="profile-section">
      <h2>Tài khoản</h2>
      <label>
        Email
        <input value={profile?.email ?? ''} disabled readOnly />
        <small className="profile-hint">Email và loại tài khoản không thể thay đổi tại đây.</small>
      </label>
      <label>
        Số điện thoại
        <input
          value={phone}
          onChange={(e) => onPhoneChange(e.target.value)}
          placeholder="VD: 0912345678"
        />
        {phoneError && <span className="profile-field-error">{phoneError}</span>}
      </label>
      <label>
        Địa chỉ
        <textarea rows={2} value={address} onChange={(e) => onAddressChange(e.target.value)} />
        {addressError && <span className="profile-field-error">{addressError}</span>}
      </label>
    </section>
  );
}
