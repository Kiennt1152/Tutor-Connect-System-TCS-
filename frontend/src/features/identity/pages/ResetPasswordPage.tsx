import type { FormEvent } from 'react';
import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { imageAssets } from '../../../assets/images/ImageAssets';
import { identityApi } from '../api/identityApi';
import './PasswordPages.css';

const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d)[\x00-\x7F]{8,100}$/;

export default function ResetPasswordPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const resetToken = (location.state as { resetToken?: string } | null)?.resetToken ?? '';
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState(resetToken ? '' : 'Phiên đặt lại mật khẩu không hợp lệ.');
  const [loading, setLoading] = useState(false);

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
      navigate('/login', { replace: true, state: { message: 'Đặt lại mật khẩu thành công. Vui lòng đăng nhập.' } });
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
          <form onSubmit={submit}>
            <label className="password-field">
              <span>Mật khẩu mới</span>
              <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required autoComplete="new-password" />
            </label>
            <label className="password-field">
              <span>Xác nhận mật khẩu mới</span>
              <input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required autoComplete="new-password" />
            </label>
            {error && <div className="password-alert password-alert--error">{error}</div>}
            <button className="password-button" type="submit" disabled={loading || !resetToken}>
              {loading ? 'Đang lưu...' : 'Đặt lại mật khẩu'}
            </button>
          </form>
          <Link className="password-back" to="/login">Quay lại đăng nhập</Link>
        </section>
      </main>
    </div>
  );
}
