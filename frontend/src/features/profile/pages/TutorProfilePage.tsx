import type { FormEvent } from 'react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { profileApi } from '../api/profileApi';
import type { UseProfileResult } from '../hooks/useProfile';
import { CccdVerifiedView } from '../components/CccdVerifiedView';
import { ChangePasswordPanel } from '../../identity/components/ChangePasswordPanel';
import type { Gender, ProfileResponse, ProfileVerificationStatus, UpdateProfileRequest } from '../types/profileTypes';
import './ProfilePage.css';

const VERIFICATION_LABEL: Record<ProfileVerificationStatus, string> = {
  UNDER_VERIFY: 'Đang chờ xét duyệt',
  VERIFIED: 'Đã xác minh',
  REJECTED: 'Bị từ chối',
};

interface TutorForm {
  fullName: string;
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
    address: profile.address ?? '',
    dateOfBirth: profile.dateOfBirth ?? '',
    gender: profile.gender ?? '',
    bio: profile.bio ?? '',
    experienceYears: profile.experienceYears != null ? String(profile.experienceYears) : '',
    hourlyRate: profile.hourlyRate != null ? String(profile.hourlyRate) : '',
  };
}

export default function TutorProfilePage({ ctx }: { ctx: UseProfileResult }) {
  const { profile, loading, error, saving, uploadingAvatar, reload, updateProfile, uploadAvatar } = ctx;

  const [form, setForm] = useState<TutorForm>(emptyForm);
  const [initial, setInitial] = useState<TutorForm>(emptyForm);
  const [fieldError, setFieldError] = useState<Partial<Record<keyof TutorForm, string>>>({});
  const [success, setSuccess] = useState<string | null>(null);
  const [verificationWarning, setVerificationWarning] = useState<string | null>(null);
  const [dobLockedByCccd, setDobLockedByCccd] = useState(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    if (profile) {
      const next = fromProfile(profile);
      setForm(next);
      setInitial(next);
    }
  }, [profile]);

  const refreshCccdLock = useCallback(() => {
    profileApi
      .getMyCccd()
      .then((res) => setDobLockedByCccd(Boolean(res.data.cccdNumber)))
      .catch(() => setDobLockedByCccd(false));
  }, []);

  useEffect(() => {
    refreshCccdLock();
  }, [refreshCccdLock]);

  function validate(): Partial<Record<keyof TutorForm, string>> {
    const errs: Partial<Record<keyof TutorForm, string>> = {};
    if (!form.fullName.trim()) {
      errs.fullName = 'Họ và tên không được để trống';
    } else if (form.fullName.trim().length < 2 || form.fullName.trim().length > 50) {
      errs.fullName = 'Phải từ 2 đến 50 ký tự';
    }
    if (form.bio && form.bio.length > 1000) {
      errs.bio = 'Mô tả tối đa 1000 ký tự';
    }
    if (form.experienceYears) {
      const n = Number(form.experienceYears);
      if (!Number.isInteger(n) || n < 0 || n > 60) {
        errs.experienceYears = 'Số năm kinh nghiệm không hợp lệ (0-60)';
      }
    }
    if (form.hourlyRate) {
      const n = Number(form.hourlyRate);
      if (!Number.isFinite(n) || n < 0) {
        errs.hourlyRate = 'Học phí không hợp lệ';
      }
    }
    return errs;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSuccess(null);
    setVerificationWarning(null);
    const errs = validate();
    setFieldError(errs);
    if (Object.keys(errs).length > 0) return;

    const payload: UpdateProfileRequest = {
      fullName: form.fullName.trim(),
      address: form.address,
      dateOfBirth: form.dateOfBirth,
      gender: form.gender || undefined,
      bio: form.bio,
      experienceYears: form.experienceYears ? Number(form.experienceYears) : undefined,
      hourlyRate: form.hourlyRate ? Number(form.hourlyRate) : undefined,
    };

    // BR-02: editing the legal name of a verified profile resets verification to re-review.
    const verificationWillReset =
      profile?.verificationStatus === 'VERIFIED' &&
      form.fullName.trim() !== (initial.fullName ?? '').trim();

    const updated = await updateProfile(payload);
    if (updated) {
      setSuccess('Đã lưu thông tin gia sư thành công');
      setInitial(fromProfile(updated));
      setFieldError({});
      if (verificationWillReset) {
        setVerificationWarning(
          'Bạn đã thay đổi họ tên pháp lý. Trạng thái xác minh đã được đặt lại về "Đang chờ xét duyệt". Vui lòng nộp lại hồ sơ.',
        );
      }
    }
  }

  async function handleAvatarChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setSuccess(null);
    await uploadAvatar(file);
    if (fileInputRef.current) fileInputRef.current.value = '';
  }

  const dirty = JSON.stringify(form) !== JSON.stringify(initial);

  return (
    <div className="profile-role-container">
      {/* Role Banner */}
      <section className="profile-role-banner profile-role-banner--tutor">
        <div className="profile-role-banner__header">
          <span className="profile-role-badge-pill profile-role-badge-pill--tutor">
            Gia sư đối tác
          </span>
          {profile?.verificationStatus && (
            <span className={`verification-badge verification-${profile.verificationStatus.toLowerCase()}`}>
              {VERIFICATION_LABEL[profile.verificationStatus]}
            </span>
          )}
        </div>
        <p className="profile-role-banner__subtitle">
          Quản lý hồ sơ giảng dạy, kinh nghiệm, học phí và trạng thái xác minh bằng cấp / CCCD.
        </p>
        <div className="profile-quick-links">
          <Link to={APP_ROUTES.teaching} className="profile-quick-chip">
            Lịch dạy cá nhân
          </Link>
          <Link to={APP_ROUTES.tutorSchedule} className="profile-quick-chip">
            Lịch lớp trung tâm
          </Link>
          <Link to={APP_ROUTES.myReputation} className="profile-quick-chip">
            Đánh giá & Uy tín
          </Link>
          <Link to={APP_ROUTES.contract} className="profile-quick-chip">
            Hợp đồng giảng dạy
          </Link>
          <Link to={APP_ROUTES.verification} className="profile-quick-chip">
            Xác minh KYC
          </Link>
        </div>
      </section>

      {error && <div className="profile-alert error">{error}</div>}
      {success && <div className="profile-alert success">{success}</div>}
      {verificationWarning && <div className="profile-alert warning">{verificationWarning}</div>}

      {loading && !profile ? (
        <p>Đang tải hồ sơ...</p>
      ) : (
        <form className="profile-form" onSubmit={handleSubmit}>
          {/* Avatar Section */}
          <section className="profile-section">
            <h2>Ảnh đại diện</h2>
            <div className="profile-avatar">
              {profile?.avatarUrl ? (
                <img src={profile.avatarUrl} alt="Ảnh đại diện" />
              ) : (
                <div className="profile-avatar-placeholder">Chưa có ảnh</div>
              )}
              <div>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  hidden
                  onChange={handleAvatarChange}
                />
                <button
                  type="button"
                  className="btn-secondary"
                  disabled={uploadingAvatar}
                  onClick={() => fileInputRef.current?.click()}
                >
                  {uploadingAvatar ? 'Đang tải lên...' : 'Đổi ảnh đại diện'}
                </button>
                <p className="profile-hint">Hỗ trợ JPEG/PNG/WebP, tối đa 5 MB.</p>
              </div>
            </div>
          </section>

          {/* Account Section */}
          <section className="profile-section">
            <h2>Tài khoản & Liên hệ</h2>
            <label>
              Email
              <input value={profile?.email ?? ''} disabled readOnly />
            </label>
            <label>
              Số điện thoại
              <input value={profile?.phone ?? ''} disabled readOnly />
              <span className="profile-hint">Lấy từ lúc đăng ký — không thể sửa.</span>
            </label>
            <label>
              Địa chỉ liên hệ
              <textarea
                rows={2}
                value={form.address}
                onChange={(e) => setForm({ ...form, address: e.target.value })}
                placeholder="Địa chỉ khu vực giảng dạy / sinh sống"
              />
            </label>
          </section>

          {/* Legal / Personal Info Section */}
          <section className="profile-section">
            <h2>Thông tin cá nhân & Pháp lý</h2>
            <label>
              Họ và tên (tên pháp lý theo CCCD)
              <input
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              />
              {fieldError.fullName && <span className="profile-field-error">{fieldError.fullName}</span>}
              <small className="profile-hint">Thay đổi trường này sẽ đặt lại trạng thái xác minh về chờ duyệt.</small>
            </label>
            <label>
              Ngày sinh
              <input
                type="date"
                value={form.dateOfBirth}
                onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })}
                disabled={dobLockedByCccd}
              />
              {dobLockedByCccd && (
                <small className="profile-hint">Ngày sinh đã được đồng bộ từ CCCD xác thực.</small>
              )}
              {fieldError.dateOfBirth && <span className="profile-field-error">{fieldError.dateOfBirth}</span>}
            </label>
            <label>
              Giới tính
              <select
                value={form.gender}
                onChange={(e) => setForm({ ...form, gender: e.target.value as Gender | '' })}
                disabled={dobLockedByCccd}
              >
                <option value="">-- Chọn --</option>
                <option value="MALE">Nam</option>
                <option value="FEMALE">Nữ</option>
                <option value="OTHER">Khác</option>
              </select>
              {dobLockedByCccd && (
                <small className="profile-hint">Giới tính đã được đồng bộ từ CCCD xác thực.</small>
              )}
            </label>
          </section>

          {/* Teaching Information */}
          <section className="profile-section">
            <h2>Thông tin chuyên môn & Giảng dạy</h2>
            <label>
              Giới thiệu bản thân & Năng lực giảng dạy
              <textarea
                rows={4}
                value={form.bio}
                onChange={(e) => setForm({ ...form, bio: e.target.value })}
                maxLength={1000}
                placeholder="Giới thiệu về phương pháp sư phạm, các thành tích và môn dạy thế mạnh..."
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
                  placeholder="VD: 3"
                />
                {fieldError.experienceYears && (
                  <span className="profile-field-error">{fieldError.experienceYears}</span>
                )}
              </label>
              <label>
                Học phí đề xuất / giờ (VND)
                <input
                  type="number"
                  min={0}
                  value={form.hourlyRate}
                  onChange={(e) => setForm({ ...form, hourlyRate: e.target.value })}
                  placeholder="VD: 200000"
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

      {/* Verification & KYC Section */}
      <section className="profile-section">
        <h2>Xác minh hồ sơ & Bằng cấp (KYC)</h2>
        {profile?.verificationStatus === 'VERIFIED' ? (
          <p>Hồ sơ và bằng cấp của bạn đã được Admin phê duyệt xác thực.</p>
        ) : (
          <>
            <p>
              Trạng thái hiện tại:{' '}
              <strong>{profile?.verificationStatus ? VERIFICATION_LABEL[profile.verificationStatus] : 'Chưa nộp hồ sơ'}</strong>
            </p>
            {profile?.verificationStatus === 'UNDER_VERIFY' && (
              <p>Hồ sơ đang trong quá trình xét duyệt bởi quản trị viên.</p>
            )}
            {profile?.verificationStatus === 'REJECTED' && (
              <p>Hồ sơ xác minh bị từ chối. Vui lòng cập nhật đầy đủ giấy tờ hợp lệ rồi nộp lại.</p>
            )}
            <div className="profile-verification-actions">
              <Link to={APP_ROUTES.verification} className="btn-primary-link">
                Nộp / Cập nhật hồ sơ xác minh
              </Link>
            </div>
          </>
        )}
      </section>

      {/* Verified CCCD view */}
      <CccdVerifiedView
        verified={profile?.verificationStatus === 'VERIFIED'}
        isCenter={false}
      />

      {/* Security & Password */}
      {profile && <ChangePasswordPanel />}
    </div>
  );
}
