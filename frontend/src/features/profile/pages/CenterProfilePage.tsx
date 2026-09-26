/**
 * ============================================================================
 * [UC-08] HỒ SƠ PHÁP LÝ & TỔ CHỨC TRUNG TÂM GIA SƯ (CENTER PROFILE PAGE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-09-13
 * 
 * Mô tả Use Case:
 *   - Quản lý hồ sơ pháp lý, địa chỉ trụ sở và thông tin vận hành của Trung tâm gia sư.
 *   - Xác lập tư cách pháp nhân trên sàn phục vụ đăng tuyển gia sư và mở lớp học liên kết.
 * 
 * Chức năng chính:
 *   1. Hồ sơ doanh nghiệp: Tên trung tâm, mã số thuế, giấy phép hoạt động và người đại diện pháp luật.
 *   2. Thông tin liên hệ: Trụ sở chính, các chi nhánh đào tạo, hotline và email hỗ trợ khách hàng.
 *   3. Hồ sơ kiểm duyệt: Tải lên giấy chứng nhận đăng ký kinh doanh phục vụ phê duyệt đối tác.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Tải thông tin định danh trung tâm từ API quản lý hồ sơ.
 *   - Bước 2: Hiển thị form thông tin tổ chức, người đại diện và tài liệu pháp lý.
 *   - Bước 3: Đại diện trung tâm cập nhật thông tin và nhấn "Lưu hồ sơ".
 *   - Bước 4: Gửi dữ liệu tới backend, đồng bộ hồ sơ xác minh đối tác uy tín.
 * ============================================================================
 */

import type { FormEvent } from 'react';
import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { APP_ROUTES } from '../../../shared/constants/routes';
import type { UseProfileResult } from '../hooks/useProfile';
import { CccdVerifiedView } from '../components/CccdVerifiedView';
import { ChangePasswordPanel } from '../../identity/components/ChangePasswordPanel';
import type { ProfileResponse, ProfileVerificationStatus, UpdateProfileRequest } from '../types/profileTypes';
import './ProfilePage.css';

const VERIFICATION_LABEL: Record<ProfileVerificationStatus, string> = {
  UNDER_VERIFY: 'Đang chờ xét duyệt',
  VERIFIED: 'Đã xác minh',
  REJECTED: 'Bị từ chối',
};

interface CenterForm {
  companyName: string;
  licenseNo: string;
  address: string;
  description: string;
}

function emptyForm(): CenterForm {
  return {
    companyName: '',
    licenseNo: '',
    address: '',
    description: '',
  };
}

function fromProfile(profile: ProfileResponse | null): CenterForm {
  if (!profile) return emptyForm();
  return {
    companyName: profile.companyName ?? '',
    licenseNo: profile.licenseNo ?? '',
    address: profile.address ?? '',
    description: profile.description ?? '',
  };
}

export default function CenterProfilePage({ ctx }: { ctx: UseProfileResult }) {
  const { profile, loading, error, saving, uploadingAvatar, reload, updateProfile, uploadAvatar } = ctx;

  const [form, setForm] = useState<CenterForm>(emptyForm);
  const [initial, setInitial] = useState<CenterForm>(emptyForm);
  const [fieldError, setFieldError] = useState<Partial<Record<keyof CenterForm, string>>>({});
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

  function validate(): Partial<Record<keyof CenterForm, string>> {
    const errs: Partial<Record<keyof CenterForm, string>> = {};
    if (!form.companyName.trim()) {
      errs.companyName = 'Tên trung tâm không được để trống';
    } else if (form.companyName.trim().length < 2 || form.companyName.trim().length > 100) {
      errs.companyName = 'Tên trung tâm phải từ 2 đến 100 ký tự';
    }
    if (form.description && form.description.length > 1000) {
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

    const payload: UpdateProfileRequest = {
      companyName: form.companyName.trim(),
      licenseNo: form.licenseNo.trim(),
      address: form.address,
      description: form.description,
    };

    // BR-02: editing the company legal name of a verified profile resets verification to re-review.
    const verificationWillReset =
      profile?.verificationStatus === 'VERIFIED' &&
      form.companyName.trim() !== (initial.companyName ?? '').trim();

    const updated = await updateProfile(payload);
    if (updated) {
      setSuccess('Đã lưu thông tin trung tâm thành công');
      setInitial(fromProfile(updated));
      setFieldError({});
      if (verificationWillReset) {
        setVerificationWarning(
          'Bạn đã thay đổi tên pháp nhân trung tâm. Trạng thái xác minh đã được đặt lại về "Đang chờ xét duyệt". Vui lòng nộp lại hồ sơ.',
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
      <section className="profile-role-banner profile-role-banner--center">
        <div className="profile-role-banner__header">
          <span className="profile-role-badge-pill profile-role-badge-pill--center">
            Trung tâm gia sư
          </span>
          {profile?.verificationStatus && (
            <span className={`verification-badge verification-${profile.verificationStatus.toLowerCase()}`}>
              {VERIFICATION_LABEL[profile.verificationStatus]}
            </span>
          )}
        </div>
        <p className="profile-role-banner__subtitle">
          Quản lý thông tin pháp nhân doanh nghiệp, giấy phép kinh doanh và điều phối lớp học.
        </p>
        <div className="profile-quick-links">
          <Link to={APP_ROUTES.center} className="profile-quick-chip">
            Bảng quản trị trung tâm
          </Link>
          <Link to={APP_ROUTES.centerReports} className="profile-quick-chip">
            Báo cáo doanh thu & lớp
          </Link>
          <Link to={APP_ROUTES.centerSchedule} className="profile-quick-chip">
            Lịch dạy trung tâm
          </Link>
          <Link to={APP_ROUTES.centerRecruitment} className="profile-quick-chip">
            Đăng tuyển gia sư
          </Link>
          <Link to={APP_ROUTES.verification} className="profile-quick-chip">
            Xác minh pháp nhân
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
          {/* Logo Section */}
          <section className="profile-section">
            <h2>Logo & Hình ảnh đại diện trung tâm</h2>
            <div className="profile-avatar">
              {profile?.avatarUrl ? (
                <img src={profile.avatarUrl} alt="Logo trung tâm" />
              ) : (
                <div className="profile-avatar-placeholder">Chưa có logo</div>
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
                  {uploadingAvatar ? 'Đang tải lên...' : 'Đổi logo trung tâm'}
                </button>
                <p className="profile-hint">Hỗ trợ JPEG/PNG/WebP, tối đa 5 MB.</p>
              </div>
            </div>
          </section>

          {/* Account Section */}
          <section className="profile-section">
            <h2>Tài khoản & Liên hệ</h2>
            <label>
              Email quản trị
              <input value={profile?.email ?? ''} disabled readOnly />
            </label>
            <label>
              Số điện thoại hotline
              <input value={profile?.phone ?? ''} disabled readOnly />
              <span className="profile-hint">Lấy từ lúc đăng ký — không thể sửa.</span>
            </label>
            <label>
              Địa chỉ trụ sở chính
              <textarea
                rows={2}
                value={form.address}
                onChange={(e) => setForm({ ...form, address: e.target.value })}
                placeholder="Địa chỉ văn phòng / trụ sở của trung tâm"
              />
            </label>
          </section>

          {/* Legal Information */}
          <section className="profile-section">
            <h2>Thông tin pháp nhân trung tâm</h2>
            <label>
              Tên trung tâm (tên pháp lý trên giấy phép)
              <input
                value={form.companyName}
                onChange={(e) => setForm({ ...form, companyName: e.target.value })}
              />
              {fieldError.companyName && (
                <span className="profile-field-error">{fieldError.companyName}</span>
              )}
              <small className="profile-hint">Thay đổi trường này sẽ đặt lại trạng thái xác minh về chờ duyệt.</small>
            </label>
            <label>
              Số giấy phép kinh doanh / Mã số thuế
              <input
                value={form.licenseNo}
                onChange={(e) => setForm({ ...form, licenseNo: e.target.value })}
                placeholder="VD: 0101234567"
              />
            </label>
            <label>
              Mô tả hoạt động trung tâm
              <textarea
                rows={4}
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                maxLength={1000}
                placeholder="Giới thiệu về quy mô, các chương trình đào tạo, cơ sở vật chất..."
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

      {/* Verification Section */}
      <section className="profile-section">
        <h2>Xác minh pháp nhân doanh nghiệp</h2>
        {profile?.verificationStatus === 'VERIFIED' ? (
          <p>Hồ sơ pháp nhân của trung tâm đã được Admin phê duyệt xác thực.</p>
        ) : (
          <>
            <p>
              Trạng thái hiện tại:{' '}
              <strong>{profile?.verificationStatus ? VERIFICATION_LABEL[profile.verificationStatus] : 'Chưa nộp hồ sơ'}</strong>
            </p>
            {profile?.verificationStatus === 'UNDER_VERIFY' && (
              <p>Hồ sơ đang trong quá trình xét duyệt bởi ban quản trị nền tảng.</p>
            )}
            {profile?.verificationStatus === 'REJECTED' && (
              <p>Hồ sơ bị từ chối. Vui lòng cập nhật đầy đủ giấy phép kinh doanh rồi nộp lại.</p>
            )}
            <div className="profile-verification-actions">
              <Link to={APP_ROUTES.verification} className="btn-primary-link">
                Nộp / Cập nhật hồ sơ xác minh
              </Link>
            </div>
          </>
        )}
      </section>

      {/* Legal documents & CCCD of representative */}
      <CccdVerifiedView
        verified={profile?.verificationStatus === 'VERIFIED'}
        isCenter={true}
      />

      {/* Security & Password */}
      {profile && <ChangePasswordPanel />}
    </div>
  );
}
