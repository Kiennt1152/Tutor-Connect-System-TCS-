import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import type { UseProfileResult } from '../hooks/useProfile';
import type { Gender, ProfileResponse, UpdateProfileRequest } from '../types/profileTypes';
import ProfileHeader from '../components/ProfileHeader';
import AvatarSection from '../components/AvatarSection';
import AccountSection from '../components/AccountSection';
import {
  validateAddress,
  validateDateOfBirth,
  validatePersonName,
  validatePhone,
} from '../components/profileValidation';
import './ProfilePage.css';

interface ClientForm {
  fullName: string;
  phone: string;
  address: string;
  dateOfBirth: string;
  gender: Gender | '';
}

function emptyForm(): ClientForm {
  return { fullName: '', phone: '', address: '', dateOfBirth: '', gender: '' };
}

function fromProfile(profile: ProfileResponse | null): ClientForm {
  if (!profile) return emptyForm();
  return {
    fullName: profile.fullName ?? '',
    phone: profile.phone ?? '',
    address: profile.address ?? '',
    dateOfBirth: profile.dateOfBirth ?? '',
    gender: profile.gender ?? '',
  };
}

export default function ClientProfilePage({ ctx }: { ctx: UseProfileResult }) {
  const { profile, loading, error, saving, uploadingAvatar, reload, updateProfile, uploadAvatar } =
    ctx;

  const [form, setForm] = useState<ClientForm>(emptyForm);
  const [initial, setInitial] = useState<ClientForm>(emptyForm);
  const [fieldError, setFieldError] = useState<Partial<Record<keyof ClientForm, string>>>({});
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    if (profile) {
      const next = fromProfile(profile);
      setForm(next);
      setInitial(next);
    }
  }, [profile]);

  function validate(): Partial<Record<keyof ClientForm, string>> {
    const errs: Partial<Record<keyof ClientForm, string>> = {};
    const nameErr = validatePersonName(form.fullName);
    if (nameErr) errs.fullName = nameErr;
    const phoneErr = validatePhone(form.phone);
    if (phoneErr) errs.phone = phoneErr;
    const dobErr = validateDateOfBirth(form.dateOfBirth);
    if (dobErr) errs.dateOfBirth = dobErr;
    const addrErr = validateAddress(form.address);
    if (addrErr) errs.address = addrErr;
    return errs;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSuccess(null);
    const errs = validate();
    setFieldError(errs);
    if (Object.keys(errs).length > 0) return;

    const payload: UpdateProfileRequest = {};
    if (form.fullName) payload.fullName = form.fullName.trim();
    if (form.phone) payload.phone = form.phone.replace(/\s/g, '');
    if (form.address) payload.address = form.address;
    if (form.dateOfBirth) payload.dateOfBirth = form.dateOfBirth;
    if (form.gender) payload.gender = form.gender;

    const updated = await updateProfile(payload);
    if (updated) {
      setSuccess('Đã lưu hồ sơ');
      setInitial(fromProfile(updated));
      setFieldError({});
    }
  }

  const dirty = JSON.stringify(form) !== JSON.stringify(initial);

  return (
    <div className="profile-page">
      <ProfileHeader profile={profile} />

      {error && <div className="profile-alert error">{error}</div>}
      {success && <div className="profile-alert success">{success}</div>}

      {loading && !profile ? (
        <p>Đang tải hồ sơ...</p>
      ) : (
        <form className="profile-form" onSubmit={handleSubmit}>
          <AvatarSection profile={profile} uploading={uploadingAvatar} onUpload={uploadAvatar} />

          <AccountSection
            profile={profile}
            phone={form.phone}
            address={form.address}
            phoneError={fieldError.phone}
            addressError={fieldError.address}
            onPhoneChange={(v) => setForm({ ...form, phone: v })}
            onAddressChange={(v) => setForm({ ...form, address: v })}
          />

          <section className="profile-section">
            <h2>Thông tin cá nhân</h2>
            <label>
              Họ và tên
              <input
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              />
              {fieldError.fullName && (
                <span className="profile-field-error">{fieldError.fullName}</span>
              )}
            </label>
            <label>
              Ngày sinh
              <input
                type="date"
                value={form.dateOfBirth}
                onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })}
              />
              {fieldError.dateOfBirth && (
                <span className="profile-field-error">{fieldError.dateOfBirth}</span>
              )}
            </label>
            <label>
              Giới tính
              <select
                value={form.gender}
                onChange={(e) => setForm({ ...form, gender: e.target.value as Gender | '' })}
              >
                <option value="">-- Chọn --</option>
                <option value="MALE">Nam</option>
                <option value="FEMALE">Nữ</option>
                <option value="OTHER">Khác</option>
              </select>
            </label>
          </section>

          <div className="profile-actions">
            <button type="submit" className="btn-primary" disabled={saving || !dirty}>
              {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
            </button>
            <button
              type="button"
              className="btn-secondary"
              disabled={saving || !dirty}
              onClick={() => {
                setForm(initial);
                setFieldError({});
              }}
            >
              Hủy
            </button>
            <button type="button" onClick={() => void reload()} className="btn-link">
              Tải lại
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
