import type { FormEvent } from 'react';
import { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useProfile } from '../hooks/useProfile';
import type {
  Gender,
  ProfileResponse,
  ProfileVerificationStatus,
  UpdateProfileRequest,
  UserRole,
} from '../types/profileTypes';
import './ProfilePage.css';

const VIETNAM_PHONE = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;

const VERIFICATION_LABEL: Record<ProfileVerificationStatus, string> = {
  UNDER_VERIFY: 'Đang chờ xét duyệt',
  VERIFIED: 'Đã xác minh',
  REJECTED: 'Bị từ chối',
};

const ROLE_LABEL: Record<UserRole, string> = {
  PLATFORM_ADMIN: 'Quản trị viên',
  TUTOR: 'Gia sư',
  TUTOR_CENTER: 'Trung tâm gia sư',
  CLIENT: 'Khách hàng (Phụ huynh / Học sinh)',
  UNKNOWN: 'Không xác định',
};

interface FormState {
  fullName: string;
  companyName: string;
  licenseNo: string;
  phone: string;
  address: string;
  dateOfBirth: string;
  gender: Gender | '';
  bio: string;
  experienceYears: string;
  hourlyRate: string;
  description: string;
}

function emptyForm(): FormState {
  return {
    fullName: '',
    companyName: '',
    licenseNo: '',
    phone: '',
    address: '',
    dateOfBirth: '',
    gender: '',
    bio: '',
    experienceYears: '',
    hourlyRate: '',
    description: '',
  };
}

function fromProfile(profile: ProfileResponse | null): FormState {
  if (!profile) return emptyForm();
  return {
    fullName: profile.fullName ?? '',
    companyName: profile.companyName ?? '',
    licenseNo: profile.licenseNo ?? '',
    phone: profile.phone ?? '',
    address: profile.address ?? '',
    dateOfBirth: profile.dateOfBirth ?? '',
    gender: profile.gender ?? '',
    bio: profile.bio ?? '',
    experienceYears: profile.experienceYears != null ? String(profile.experienceYears) : '',
    hourlyRate: profile.hourlyRate != null ? String(profile.hourlyRate) : '',
    description: profile.description ?? '',
  };
}

export default function ProfilePage() {
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
  } = useProfile();

  const [form, setForm] = useState<FormState>(emptyForm);
  const [initial, setInitial] = useState<FormState>(emptyForm);
  const [fieldError, setFieldError] = useState<Partial<Record<keyof FormState, string>>>({});
  const [success, setSuccess] = useState<string | null>(null);
  const [verificationWarning, setVerificationWarning] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    if (profile) {
      const next = fromProfile(profile);
      setForm(next);
      setInitial(next);
    }
  }, [profile]);

  const role = profile?.role;
  const isClient = role === 'CLIENT';
  const isTutor = role === 'TUTOR';
  const isCenter = role === 'TUTOR_CENTER';

  const verificationLinkedField = useMemo(() => {
    if (isTutor) return { key: 'fullName' as const, label: 'Họ và tên (tên pháp lý)' };
    if (isCenter) return { key: 'companyName' as const, label: 'Tên trung tâm (tên pháp lý)' };
    return null;
  }, [isTutor, isCenter]);

  function validate(): Partial<Record<keyof FormState, string>> {
    const errs: Partial<Record<keyof FormState, string>> = {};

    if (verificationLinkedField) {
      const v = form[verificationLinkedField.key]?.trim() ?? '';
      if (v.length < 2 || v.length > 50) {
        errs[verificationLinkedField.key] = 'Phải từ 2 đến 50 ký tự';
      }
    }

    if (form.phone && !VIETNAM_PHONE.test(form.phone.replace(/\s/g, ''))) {
      errs.phone = 'Số điện thoại không hợp lệ (10 số, đầu 0 hoặc +84)';
    }

    if (isClient && form.dateOfBirth) {
      const dob = new Date(form.dateOfBirth);
      if (Number.isNaN(dob.getTime())) errs.dateOfBirth = 'Ngày sinh không hợp lệ';
    }

    if (isTutor) {
      if (form.bio && form.bio.length > 1000) errs.bio = 'Mô tả tối đa 1000 ký tự';
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
    }

    if (isCenter && form.description && form.description.length > 1000) {
      errs.description = 'Mô tả tối đa 1000 ký tự';
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

    const payload: UpdateProfileRequest = {};
    if (isClient || isTutor) {
      if (form.fullName) payload.fullName = form.fullName.trim();
    }
    if (isCenter) {
      if (form.companyName) payload.companyName = form.companyName.trim();
      if (form.licenseNo) payload.licenseNo = form.licenseNo.trim();
      if (form.description) payload.description = form.description;
    }
    if (form.phone) payload.phone = form.phone.replace(/\s/g, '');
    if (form.address) payload.address = form.address;
    if (form.dateOfBirth) payload.dateOfBirth = form.dateOfBirth;
    if (form.gender) payload.gender = form.gender;
    if (isTutor) {
      if (form.bio) payload.bio = form.bio;
      if (form.experienceYears) payload.experienceYears = Number(form.experienceYears);
      if (form.hourlyRate) payload.hourlyRate = Number(form.hourlyRate);
    }

    // BR-02: editing a verification-linked field resets verification to require re-review.
    const verificationWillReset =
      verificationLinkedField != null
      && profile?.verificationStatus === 'VERIFIED'
      && form[verificationLinkedField.key].trim() !== (initial[verificationLinkedField.key] ?? '').trim();

    const updated = await updateProfile(payload);
    if (updated) {
      setSuccess('Đã lưu hồ sơ');
      setInitial(fromProfile(updated));
      setFieldError({});
      if (verificationWillReset) {
        setVerificationWarning(
          'Bạn đã thay đổi trường gắn với xác minh. Trạng thái xác minh đã được đặt lại về "Đang chờ xét duyệt". Vui lòng nộp lại hồ sơ.',
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

  async function handleSubmitVerification() {
    const ok = await submitVerification();
    if (ok) setSuccess('Đã nộp hồ sơ xác minh, vui lòng chờ admin xét duyệt');
  }

  const dirty = JSON.stringify(form) !== JSON.stringify(initial);
  const errs = fieldError;

  return (
    <div className="profile-page">
      <header className="profile-header">
        <h1>Hồ sơ cá nhân</h1>
        {profile && (
          <p className="profile-role">
            Vai trò: <strong>{ROLE_LABEL[profile.role] ?? profile.role}</strong>
            {profile.verificationStatus && !isClient && (
              <>
                {' · '}
                <span className={`verification-badge verification-${profile.verificationStatus.toLowerCase()}`}>
                  {VERIFICATION_LABEL[profile.verificationStatus]}
                </span>
              </>
            )}
          </p>
        )}
      </header>

      {error && <div className="profile-alert error">{error}</div>}
      {success && <div className="profile-alert success">{success}</div>}
      {verificationWarning && <div className="profile-alert warning">{verificationWarning}</div>}

      {loading && !profile ? (
        <p>Đang tải hồ sơ...</p>
      ) : (
        <form className="profile-form" onSubmit={handleSubmit}>
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

          <section className="profile-section">
            <h2>Tài khoản</h2>
            <label>
              Email
              <input value={profile?.email ?? ''} disabled readOnly />
            </label>
            <label>
              Số điện thoại
              <input
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
                placeholder="VD: 0912345678"
              />
              {errs.phone && <span className="profile-field-error">{errs.phone}</span>}
            </label>
            <label>
              Địa chỉ
              <textarea
                rows={2}
                value={form.address}
                onChange={(e) => setForm({ ...form, address: e.target.value })}
              />
            </label>
          </section>

          {(isClient || isTutor) && (
            <section className="profile-section">
              <h2>Thông tin cá nhân</h2>
              <label>
                {verificationLinkedField && verificationLinkedField.key === 'fullName'
                  ? verificationLinkedField.label
                  : 'Họ và tên'}
                <input
                  value={form.fullName}
                  onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                  disabled={!isClient && !isTutor}
                />
                {errs.fullName && <span className="profile-field-error">{errs.fullName}</span>}
                {verificationLinkedField?.key === 'fullName' && (
                  <small className="profile-hint">
                    Thay đổi trường này sẽ đặt lại trạng thái xác minh.
                  </small>
                )}
              </label>
              <label>
                Ngày sinh
                <input
                  type="date"
                  value={form.dateOfBirth}
                  onChange={(e) => setForm({ ...form, dateOfBirth: e.target.value })}
                />
                {errs.dateOfBirth && <span className="profile-field-error">{errs.dateOfBirth}</span>}
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
          )}

          {isTutor && (
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
                {errs.bio && <span className="profile-field-error">{errs.bio}</span>}
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
                  {errs.experienceYears && <span className="profile-field-error">{errs.experienceYears}</span>}
                </label>
                <label>
                  Học phí / giờ (VND)
                  <input
                    type="number"
                    min={0}
                    value={form.hourlyRate}
                    onChange={(e) => setForm({ ...form, hourlyRate: e.target.value })}
                  />
                  {errs.hourlyRate && <span className="profile-field-error">{errs.hourlyRate}</span>}
                </label>
              </div>
            </section>
          )}

          {isCenter && (
            <section className="profile-section">
              <h2>Thông tin trung tâm</h2>
              <label>
                {verificationLinkedField && verificationLinkedField.key === 'companyName'
                  ? verificationLinkedField.label
                  : 'Tên trung tâm'}
                <input
                  value={form.companyName}
                  onChange={(e) => setForm({ ...form, companyName: e.target.value })}
                />
                {errs.companyName && <span className="profile-field-error">{errs.companyName}</span>}
                {verificationLinkedField?.key === 'companyName' && (
                  <small className="profile-hint">
                    Thay đổi trường này sẽ đặt lại trạng thái xác minh.
                  </small>
                )}
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
                <small className="profile-hint">{form.description.length}/1000 ký tự</small>
              </label>
            </section>
          )}

          <div className="profile-actions">
            <button type="submit" className="btn-primary" disabled={saving || !dirty}>
              {saving ? 'Đang lưu...' : 'Lưu thay đổi'}
            </button>
            <button type="button" className="btn-secondary" disabled={saving || !dirty}
                    onClick={() => { setForm(initial); setFieldError({}); }}>
              Hủy
            </button>
            <button type="button" onClick={() => void reload()} className="btn-link">
              Tải lại
            </button>
          </div>
        </form>
      )}

      {(isTutor || isCenter) && profile?.verificationStatus && (
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
              {profile.verificationStatus === 'UNDER_VERIFY' && (
                <p>Hồ sơ đang được xét duyệt.</p>
              )}
              {profile.verificationStatus === 'REJECTED' && (
                <p>Hồ sơ bị từ chối. Vui lòng cập nhật thông tin rồi nộp lại.</p>
              )}
              <button type="button" className="btn-primary" onClick={handleSubmitVerification}>
                Nộp hồ sơ xác minh
              </button>
            </>
          )}
          <p className="profile-hint">
            <Link to="/profile/verification">Xem chi tiết hồ sơ xác minh</Link>
          </p>
        </section>
      )}
    </div>
  );
}
