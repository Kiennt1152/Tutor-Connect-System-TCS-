/**
 * ============================================================================
 * [UC-01] THIẾT LẬP MẬT KHẨU MỚI BẰNG TOKEN (RESET PASSWORD PAGE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-07
 * 
 * Mô tả Use Case:
 *   - Tiếp nhận người dùng từ đường dẫn liên kết an toàn trong email khôi phục mật khẩu.
 *   - Xác minh tính toàn vẹn của mã token và cho phép đặt lại mật khẩu bảo mật mới.
 * 
 * Chức năng chính:
 *   1. Xác thực Token: Trích xuất và kiểm tra mã xác thực trên đường dẫn URL.
 *   2. Kiểm tra độ mạnh mật khẩu: Đảm bảo mật khẩu mới đủ độ dài và các ký tự bảo mật quy định.
 *   3. Cập nhật mật khẩu mới: Xác nhận đổi mật khẩu thành công và hủy bỏ hiệu lực của token cũ.
 *   4. Tự động điều hướng: Chuyển hướng người dùng về trang đăng nhập sau khi hoàn tất.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Người dùng nhấp vào link trong email, mở màn hình Đặt lại mật khẩu kèm tham số token.
 *   - Bước 2: Nhập mật khẩu mới và nhập lại xác nhận mật khẩu.
 *   - Bước 3: Nhấn "Cập nhật mật khẩu", gọi API `identityApi.resetPassword`.
 *   - Bước 4: Hệ thống cập nhật mật khẩu mã hóa mới vào CSDL và điều hướng về trang Đăng nhập.
 * ============================================================================
 */

import type { FormEvent } from 'react';
import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { imageAssets } from '../../../assets/images/ImageAssets';
import { identityApi } from '../api/identityApi';
import './PasswordPages.css';

const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d)[\x00-\x7F]{8,100}$/;

function EyeIcon({ off }: { off: boolean }) {
  const common = {
    width: 18,
    height: 18,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 2,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    'aria-hidden': true,
  };
  return off ? (
    <svg {...common}>
      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
      <line x1="1" y1="1" x2="23" y2="23" />
    </svg>
  ) : (
    <svg {...common}>
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

export default function ResetPasswordPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const resetToken = (location.state as { resetToken?: string } | null)?.resetToken ?? '';
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState(resetToken ? '' : 'Phiên đặt lại mật khẩu không hợp lệ.');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError('');
    setSuccess('');
    if (!PASSWORD_RULE.test(newPassword)) {
      setError('Mật khẩu phải có ít nhất 8 ký tự, gồm chữ và số, không dấu.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.');
      return;
    }
    setLoading(true);
    try {
      await identityApi.resetPassword({ token: resetToken, newPassword });
      setSuccess('Đặt lại mật khẩu thành công! Đang chuyển hướng về trang đăng nhập...');
      setTimeout(() => {
        navigate('/login', {
          replace: true,
          state: { message: 'Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.' },
        });
      }, 1500);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể đặt lại mật khẩu. Vui lòng yêu cầu OTP mới.'));
      setLoading(false);
    }
  }

  return (
    <div className="password-page">
      <header className="password-header">
        <Link to="/login" className="password-brand">
          <img src={imageAssets.logo} alt="" />
          Tutor Connect System
        </Link>
      </header>
      <main className="password-main">
        <section className="password-card">
          <p className="password-eyebrow">Mật khẩu mới</p>
          <h1>Đặt lại mật khẩu</h1>
          <p className="password-subtitle">Tạo mật khẩu mới để tiếp tục sử dụng tài khoản.</p>
          <form onSubmit={submit}>
            <label className="password-field">
              <span>Mật khẩu mới</span>
              <div className="password-input-wrap">
                <input
                  type={showNewPassword ? 'text' : 'password'}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="Nhập mật khẩu mới"
                  required
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  className="password-eye"
                  aria-label={showNewPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  onClick={() => setShowNewPassword((s) => !s)}
                >
                  <EyeIcon off={showNewPassword} />
                </button>
              </div>
            </label>
            <label className="password-field">
              <span>Xác nhận mật khẩu mới</span>
              <div className="password-input-wrap">
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Nhập lại mật khẩu mới"
                  required
                  autoComplete="new-password"
                />
                <button
                  type="button"
                  className="password-eye"
                  aria-label={showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  onClick={() => setShowConfirmPassword((s) => !s)}
                >
                  <EyeIcon off={showConfirmPassword} />
                </button>
              </div>
            </label>
            {success && <div className="password-alert password-alert--success">{success}</div>}
            {error && <div className="password-alert password-alert--error">{error}</div>}
            <button className="password-button" type="submit" disabled={loading || !resetToken || Boolean(success)}>
              {success ? 'Thành công' : loading ? 'Đang lưu...' : 'Đặt lại mật khẩu'}
            </button>
          </form>
          <Link className="password-back" to="/login">Quay lại đăng nhập</Link>
        </section>
      </main>
    </div>
  );
}
