/**
 * ============================================================================
 * [UC-08] HỒ SƠ & THIẾT LẬP TÀI KHOẢN PHỤ HUYNH (CLIENT PROFILE PAGE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-09-13
 * 
 * Mô tả Use Case:
 *   - Quản lý thông tin cá nhân và hồ sơ gia đình của Phụ huynh học sinh.
 *   - Cho phép thiết lập danh sách học sinh phụ thuộc (con cái) để thuận tiện cho việc đăng ký lớp học.
 * 
 * Chức năng chính:
 *   1. Thông tin phụ huynh: Cập nhật họ tên, số điện thoại, địa chỉ cư trú và ảnh đại diện.
 *   2. Quản lý học sinh phụ thuộc: Thêm mới, chỉnh sửa thông tin lớp học và học lực của con cái.
 *   3. Thiết lập bảo mật: Đổi mật khẩu tài khoản và quản lý phương thức nhận thông báo học tập.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Hook tải dữ liệu hồ sơ người dùng hiện tại từ API backend.
 *   - Bước 2: Hiển thị thông tin cá nhân trên biểu mẫu cho phép chỉnh sửa.
 *   - Bước 3: Người dùng cập nhật thông tin và bấm "Lưu thay đổi".
 *   - Bước 4: Gửi dữ liệu cập nhật qua API, đồng bộ lại trạng thái thông tin tài khoản.
 * ============================================================================
 */

import type { FormEvent } from 'react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { profileApi } from '../api/profileApi';
import type { UseProfileResult } from '../hooks/useProfile';
import { CccdSection } from '../components/CccdSection';
import { ChangePasswordPanel } from '../../identity/components/ChangePasswordPanel';
import type { ChildProfile, Gender, ProfileResponse, UpdateProfileRequest } from '../types/profileTypes';
import './ProfilePage.css';

interface ClientForm {
  fullName: string;
  address: string;
  dateOfBirth: string;
  gender: Gender | '';
}

function emptyForm(): ClientForm {
  return { fullName: '', address: '', dateOfBirth: '', gender: '' };
}

function fromProfile(profile: ProfileResponse | null): ClientForm {
  if (!profile) return emptyForm();
  return {
    fullName: profile.fullName ?? '',
    address: profile.address ?? '',
    dateOfBirth: profile.dateOfBirth ?? '',
    gender: profile.gender ?? '',
  };
}

function calcAge(dateOfBirth?: string | null): number | null {
  if (!dateOfBirth) return null;
  const dob = new Date(dateOfBirth);
  if (Number.isNaN(dob.getTime())) return null;
  const now = new Date();
  let age = now.getFullYear() - dob.getFullYear();
  const monthDiff = now.getMonth() - dob.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && now.getDate() < dob.getDate())) {
    age -= 1;
  }
  return age;
}

export default function ClientProfilePage({ ctx }: { ctx: UseProfileResult }) {
  const { profile, loading, error, saving, uploadingAvatar, reload, updateProfile, uploadAvatar } = ctx;

  const [form, setForm] = useState<ClientForm>(emptyForm);
  const [initial, setInitial] = useState<ClientForm>(emptyForm);
  const [fieldError, setFieldError] = useState<Partial<Record<keyof ClientForm, string>>>({});
  const [success, setSuccess] = useState<string | null>(null);
  const [children, setChildren] = useState<ChildProfile[]>([]);
  const [dobLockedByCccd, setDobLockedByCccd] = useState(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const age = calcAge(profile?.dateOfBirth);
  const isAdultClient = age != null && age >= 18;

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

  const handleCccdSaved = useCallback(() => {
    void reload();
    refreshCccdLock();
  }, [reload, refreshCccdLock]);

  useEffect(() => {
    if (!isAdultClient) {
      setChildren([]);
      return;
    }
    let active = true;
    profileApi
      .getMyChildren()
      .then((res) => {
        if (active) setChildren(res.data);
      })
      .catch(() => {
        if (active) setChildren([]);
      });
    return () => {
      active = false;
    };
  }, [isAdultClient]);

  function validate(): Partial<Record<keyof ClientForm, string>> {
    const errs: Partial<Record<keyof ClientForm, string>> = {};
    if (!form.fullName.trim()) {
      errs.fullName = 'Họ và tên không được để trống';
    } else if (form.fullName.trim().length < 2 || form.fullName.trim().length > 50) {
      errs.fullName = 'Họ và tên phải từ 2 đến 50 ký tự';
    }
    if (form.dateOfBirth) {
      const dob = new Date(form.dateOfBirth);
      if (Number.isNaN(dob.getTime())) errs.dateOfBirth = 'Ngày sinh không hợp lệ';
    }
    return errs;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSuccess(null);
    const errs = validate();
    setFieldError(errs);
    if (Object.keys(errs).length > 0) return;

    const payload: UpdateProfileRequest = {
      fullName: form.fullName.trim(),
      address: form.address,
      dateOfBirth: form.dateOfBirth,
      gender: form.gender || undefined,
    };

    const updated = await updateProfile(payload);
    if (updated) {
      setSuccess('Đã lưu hồ sơ học viên thành công');
      setInitial(fromProfile(updated));
      setFieldError({});
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
      <section className="profile-role-banner">
        <div className="profile-role-banner__header">
          <span className="profile-role-badge-pill profile-role-badge-pill--client">
            Học viên / Phụ huynh
          </span>
        </div>
        <p className="profile-role-banner__subtitle">
          Quản lý thông tin tài khoản cá nhân, định danh CCCD và liên kết hồ sơ học viên / con cái.
        </p>
        <div className="profile-quick-links">
          <Link to={APP_ROUTES.teaching} className="profile-quick-chip">
            Lịch học cá nhân
          </Link>
          <Link to={APP_ROUTES.clientSchedule} className="profile-quick-chip">
            Lịch lớp trung tâm
          </Link>
          <Link to={APP_ROUTES.marketplace} className="profile-quick-chip">
            Yêu cầu gia sư đã đăng
          </Link>
          <Link to={APP_ROUTES.profileDependents} className="profile-quick-chip">
            Hồ sơ con / học viên
          </Link>
          <Link to={APP_ROUTES.contract} className="profile-quick-chip">
            Hợp đồng gia sư
          </Link>
        </div>
      </section>

      {error && <div className="profile-alert error">{error}</div>}
      {success && <div className="profile-alert success">{success}</div>}

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
                placeholder="Số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố"
              />
            </label>
          </section>

          {/* Personal Info Section */}
          <section className="profile-section">
            <h2>Thông tin cá nhân</h2>
            <label>
              Họ và tên
              <input
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              />
              {fieldError.fullName && <span className="profile-field-error">{fieldError.fullName}</span>}
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
                <small className="profile-hint">Ngày sinh đã được khóa theo thông tin CCCD xác thực.</small>
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

      {/* CCCD Section */}
      <CccdSection onSaved={handleCccdSaved} />

      {/* Child Profiles Section */}
      <section className="profile-section">
        <h2>Quản lý hồ sơ con & học viên phụ thuộc</h2>
        <p>Cập nhật thông tin học tập, lớp học hoặc tạo hồ sơ con mới cho các lớp cần gia sư.</p>
        {children.length > 0 ? (
          <ul className="profile-child-list">
            {children.map((child) => (
              <li key={child.childProfileId} className="profile-child-item">
                <span className="profile-child-item__name">{child.fullName}</span>
                <Link to={APP_ROUTES.childProfile(child.childProfileId)} className="btn-link">
                  Quản lý
                </Link>
              </li>
            ))}
          </ul>
        ) : (
          <p className="profile-hint">Chưa có hồ sơ con nào.</p>
        )}
        <div className="profile-link-actions">
          <Link to={APP_ROUTES.profileDependents} className="btn-primary-link">
            Thêm / liên kết hồ sơ con
          </Link>
        </div>
      </section>

      {/* Dependent Profile Linker CTA */}
      <section className="profile-link-card">
        <div className="profile-link-card__icon" aria-hidden="true">
          🔗
        </div>
        <div className="profile-link-card__body">
          <h2>Liên kết hồ sơ & Phụ huynh</h2>
          <p>
            Liên kết hồ sơ phụ huynh và quản lý hồ sơ con cho tài khoản học sinh vị thành niên. Cần hoàn tất liên kết
            trước khi thanh toán hoặc tạo hợp đồng với gia sư.
          </p>
          <div className="profile-link-actions">
            <Link to={APP_ROUTES.profileDependents} className="btn-primary-link">
              Liên kết hồ sơ
            </Link>
            <Link to={APP_ROUTES.guardianApprovals} className="btn-link">
              Xác nhận phụ huynh
            </Link>
          </div>
        </div>
      </section>

      {/* Security & Password */}
      {profile && <ChangePasswordPanel />}
    </div>
  );
}
