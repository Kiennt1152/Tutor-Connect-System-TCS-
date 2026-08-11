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
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError('');
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
      setSuccess(true);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể đặt lại mật khẩu. Vui lòng yêu cầu OTP mới.'));
    } finally {
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
          {success ? (
            <div className="password-success-state">
              <div className="password-alert password-alert--success">
                Đặt lại mật khẩu thành công! Bạn có thể đăng nhập với mật khẩu mới.
              </div>
              <Link className="password-button" to="/login" style={{ textAlign: 'center', textDecoration: 'none', display: 'block', boxSizing: 'border-box' }}>
                Đăng nhập ngay
              </Link>
            </div>
          ) : (
            <form onSubmit={submit}>
              <label className="password-field">
                <span>Mật khẩu mới</span>
                <div className="password-input-wrap">
                  <input type={showNewPassword ? 'text' : 'password'} className="password-input--with-toggle" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required autoComplete="new-password" />
                  <button
                    type="button"
                    className="password-eye"
                    tabIndex={-1}
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
                  <input type={showConfirmPassword ? 'text' : 'password'} className="password-input--with-toggle" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required autoComplete="new-password" />
                  <button
                    type="button"
                    className="password-eye"
                    tabIndex={-1}
                    aria-label={showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                    onClick={() => setShowConfirmPassword((s) => !s)}
                  >
                    <EyeIcon off={showConfirmPassword} />
                  </button>
                </div>
              </label>
              {error && <div className="password-alert password-alert--error">{error}</div>}
              <button className="password-button" type="submit" disabled={loading || !resetToken}>
                {loading ? 'Đang lưu...' : 'Đặt lại mật khẩu'}
              </button>
            </form>
          )}
          <Link className="password-back" to="/login">Quay lại đăng nhập</Link>
        </section>
      </main>
    </div>
  );
}
