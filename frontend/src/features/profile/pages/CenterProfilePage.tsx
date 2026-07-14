import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import type { UseProfileResult } from '../hooks/useProfile';
import type { ProfileResponse, UpdateProfileRequest } from '../types/profileTypes';
import ProfileHeader from '../components/ProfileHeader';
import AvatarSection from '../components/AvatarSection';
import AccountSection from '../components/AccountSection';
import VerificationSection from '../components/VerificationSection';
import {
  validateAddress,
  validateBio,
  validateLegalName,
  validatePhone,
} from '../components/profileValidation';
import './ProfilePage.css';

interface CenterForm {
  companyName: string;
  licenseNo: string;
  phone: string;
  address: string;
  description: string;
}

function emptyForm(): CenterForm {
  return { companyName: '', licenseNo: '', phone: '', address: '', description: '' };
}

function fromProfile(profile: ProfileResponse | null): CenterForm {
  if (!profile) return emptyForm();
  return {
    companyName: profile.companyName ?? '',
    licenseNo: profile.licenseNo ?? '',
    phone: profile.phone ?? '',
    address: profile.address ?? '',
    description: profile.description ?? '',
  };
}

export default function CenterProfilePage({ ctx }: { ctx: UseProfileResult }) {
  const {
    profile,
    loading,
    error,
    saving,
    uploadingAvatar,
    reload,
    updateProfile,
    uploadAvatar,
    submitVerification,
  } = ctx;

  const [form, setForm] = useState<CenterForm>(emptyForm);
  const [initial, setInitial] = useState<CenterForm>(emptyForm);
  const [fieldError, setFieldError] = useState<Partial<Record<keyof CenterForm, string>>>({});
  const [success, setSuccess] = useState<string | null>(null);
  const [verificationWarning, setVerificationWarning] = useState<string | null>(null);

  useEffect(() => {
    if (profile) {
      const next = fromProfile(profile);
      setForm(next);
      setInitial(next);
    }
  }, [profile]);

  function validate(): Partial<Record<keyof CenterForm, string>> {
    const errs: Partial<Record<keyof CenterForm, string>> = {};
    const nameErr = validateLegalName(form.companyName);
    if (nameErr) errs.companyName = nameErr;
    const phoneErr = validatePhone(form.phone);
    if (phoneErr) errs.phone = phoneErr;
    const addrErr = validateAddress(form.address);
    if (addrErr) errs.address = addrErr;
    const descErr = validateBio(form.description);
    if (descErr) errs.description = descErr;
    return errs;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSuccess(null);
    setVerificationWarning(null);
    const errs = validate();
    setFieldError(errs);
    if (Object.keys(errs).length > 0) return;

    const payload: UpdateProfileRequest = {};
    if (form.companyName) payload.companyName = form.companyName.trim();
    if (form.licenseNo) payload.licenseNo = form.licenseNo.trim();
    if (form.phone) payload.phone = form.phone.replace(/\s/g, '');
    if (form.address) payload.address = form.address;
    if (form.description) payload.description = form.description;

    // BR-02: editing the legal name of a verified profile resets verification to re-review.
    const verificationWillReset =
      profile?.verificationStatus === 'VERIFIED' &&
      form.companyName.trim() !== (initial.companyName ?? '').trim();

    const updated = await updateProfile(payload);
    if (updated) {
      setSuccess('Đã lưu hồ sơ');
      setInitial(fromProfile(updated));
      setFieldError({});
      if (verificationWillReset) {
        setVerificationWarning(
          'Bạn đã thay đổi tên trung tâm. Trạng thái xác minh đã được đặt lại về "Đang chờ xét duyệt". Vui lòng nộp lại hồ sơ.',
        );
      }
    }
  }

  async function handleSubmitVerification() {
    const ok = await submitVerification();
    if (ok) setSuccess('Đã nộp hồ sơ xác minh, vui lòng chờ admin xét duyệt');
  }

  const dirty = JSON.stringify(form) !== JSON.stringify(initial);

  return (
    <div className="profile-page">
      <ProfileHeader profile={profile} showVerification />

      {error && <div className="profile-alert error">{error}</div>}
      {success && <div className="profile-alert success">{success}</div>}
      {verificationWarning && <div className="profile-alert warning">{verificationWarning}</div>}

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
            <h2>Thông tin trung tâm</h2>
            <label>
              Tên trung tâm (tên pháp lý)
              <input
                value={form.companyName}
                onChange={(e) => setForm({ ...form, companyName: e.target.value })}
              />
              {fieldError.companyName && (
                <span className="profile-field-error">{fieldError.companyName}</span>
              )}
              <small className="profile-hint">
                Thay đổi trường này sẽ đặt lại trạng thái xác minh.
              </small>
            </label>
            <label>
              Số giấy phép kinh doanh
              <input
                value={form.licenseNo}
                onChange={(e) => setForm({ ...form, licenseNo: e.target.value })}
              />
            </label>
            <label>
              Mô tả trung tâm
              <textarea
                rows={4}
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                maxLength={1000}
              />
              {fieldError.description && (
                <span className="profile-field-error">{fieldError.description}</span>
              )}
              <small className="profile-hint">{form.description.length}/1000 ký tự</small>
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

      <VerificationSection profile={profile} onSubmit={handleSubmitVerification} />
    </div>
  );
}
