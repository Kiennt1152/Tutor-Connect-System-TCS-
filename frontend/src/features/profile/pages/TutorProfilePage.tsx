import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import type { UseProfileResult } from '../hooks/useProfile';
import type { Gender, ProfileResponse, UpdateProfileRequest } from '../types/profileTypes';
import ProfileHeader from '../components/ProfileHeader';
import AvatarSection from '../components/AvatarSection';
import AccountSection from '../components/AccountSection';
import VerificationSection from '../components/VerificationSection';
import {
  validateAddress,
  validateBio,
  validateDateOfBirth,
  validateExperienceYears,
  validateHourlyRate,
  validateLegalName,
  validatePhone,
} from '../components/profileValidation';
import './ProfilePage.css';

interface TutorForm {
  fullName: string;
  phone: string;
  address: string;
  dateOfBirth: string;
  gender: Gender | '';
  bio: string;
  experienceYears: string;
  hourlyRate: string;
}

function emptyForm(): TutorForm {
  return {
    fullName: '',
    phone: '',
    address: '',
    dateOfBirth: '',
    gender: '',
    bio: '',
    experienceYears: '',
    hourlyRate: '',
  };
}

function fromProfile(profile: ProfileResponse | null): TutorForm {
  if (!profile) return emptyForm();
  return {
    fullName: profile.fullName ?? '',
    phone: profile.phone ?? '',
    address: profile.address ?? '',
    dateOfBirth: profile.dateOfBirth ?? '',
    gender: profile.gender ?? '',
    bio: profile.bio ?? '',
    experienceYears: profile.experienceYears != null ? String(profile.experienceYears) : '',
    hourlyRate: profile.hourlyRate != null ? String(profile.hourlyRate) : '',
  };
}

export default function TutorProfilePage({ ctx }: { ctx: UseProfileResult }) {
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

  const [form, setForm] = useState<TutorForm>(emptyForm);
  const [initial, setInitial] = useState<TutorForm>(emptyForm);
  const [fieldError, setFieldError] = useState<Partial<Record<keyof TutorForm, string>>>({});
  const [success, setSuccess] = useState<string | null>(null);
  const [verificationWarning, setVerificationWarning] = useState<string | null>(null);

  useEffect(() => {
    if (profile) {
      const next = fromProfile(profile);
      setForm(next);
      setInitial(next);
    }
  }, [profile]);

  function validate(): Partial<Record<keyof TutorForm, string>> {
    const errs: Partial<Record<keyof TutorForm, string>> = {};
    const nameErr = validateLegalName(form.fullName);
    if (nameErr) errs.fullName = nameErr;
    const phoneErr = validatePhone(form.phone);
    if (phoneErr) errs.phone = phoneErr;
    const dobErr = validateDateOfBirth(form.dateOfBirth);
    if (dobErr) errs.dateOfBirth = dobErr;
    const addrErr = validateAddress(form.address);
    if (addrErr) errs.address = addrErr;
    const bioErr = validateBio(form.bio);
    if (bioErr) errs.bio = bioErr;
    const expErr = validateExperienceYears(form.experienceYears);
    if (expErr) errs.experienceYears = expErr;
    const rateErr = validateHourlyRate(form.hourlyRate);
    if (rateErr) errs.hourlyRate = rateErr;
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
    if (form.fullName) payload.fullName = form.fullName.trim();
    if (form.phone) payload.phone = form.phone.replace(/\s/g, '');
    if (form.address) payload.address = form.address;
    if (form.dateOfBirth) payload.dateOfBirth = form.dateOfBirth;
    if (form.gender) payload.gender = form.gender;
    if (form.bio) payload.bio = form.bio;
    if (form.experienceYears) payload.experienceYears = Number(form.experienceYears);
    if (form.hourlyRate) payload.hourlyRate = Number(form.hourlyRate);

    // BR-02: editing the legal name of a verified profile resets verification to re-review.
    const verificationWillReset =
      profile?.verificationStatus === 'VERIFIED' &&
      form.fullName.trim() !== (initial.fullName ?? '').trim();

    const updated = await updateProfile(payload);
    if (updated) {
      setSuccess('Đã lưu hồ sơ');
      setInitial(fromProfile(updated));
      setFieldError({});
      if (verificationWillReset) {
        setVerificationWarning(
          'Bạn đã thay đổi tên pháp lý. Trạng thái xác minh đã được đặt lại về "Đang chờ xét duyệt". Vui lòng nộp lại hồ sơ.',
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
            <h2>Thông tin cá nhân</h2>
            <label>
              Họ và tên (tên pháp lý)
              <input
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              />
              {fieldError.fullName && (
                <span className="profile-field-error">{fieldError.fullName}</span>
              )}
              <small className="profile-hint">
                Thay đổi trường này sẽ đặt lại trạng thái xác minh.
              </small>
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

          <section className="profile-section">
            <h2>Thông tin giảng dạy</h2>
            <label>
              Giới thiệu ngắn
              <textarea
                rows={4}
                value={form.bio}
                onChange={(e) => setForm({ ...form, bio: e.target.value })}
                maxLength={1000}
              />
              {fieldError.bio && <span className="profile-field-error">{fieldError.bio}</span>}
              <small className="profile-hint">{form.bio.length}/1000 ký tự</small>
            </label>
            <div className="profile-grid-2">
              <label>
                Số năm kinh nghiệm
                <input
                  type="number"
                  min={0}
                  max={60}
                  value={form.experienceYears}
                  onChange={(e) => setForm({ ...form, experienceYears: e.target.value })}
                />
                {fieldError.experienceYears && (
                  <span className="profile-field-error">{fieldError.experienceYears}</span>
                )}
              </label>
              <label>
                Học phí / giờ (VND)
                <input
                  type="number"
                  min={0}
                  value={form.hourlyRate}
                  onChange={(e) => setForm({ ...form, hourlyRate: e.target.value })}
                />
                {fieldError.hourlyRate && (
                  <span className="profile-field-error">{fieldError.hourlyRate}</span>
                )}
              </label>
            </div>
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
