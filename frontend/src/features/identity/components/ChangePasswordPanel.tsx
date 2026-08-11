import type { FormEvent } from 'react';
import { useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { identityApi } from '../api/identityApi';

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

export function ChangePasswordPanel() {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError('');
    setSuccess('');
    if (!PASSWORD_RULE.test(newPassword)) {
      setError('Mật khẩu mới phải có ít nhất 8 ký tự, gồm chữ và số, không dấu.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Mật khẩu xác nhận không khớp.');
      return;
    }
    if (newPassword === currentPassword) {
      setError('Mật khẩu mới phải khác mật khẩu hiện tại.');
      return;
    }
    setSubmitting(true);
    try {
      const response = await identityApi.changePassword({ currentPassword, newPassword });
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setSuccess(response.message);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể đổi mật khẩu. Vui lòng thử lại.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="profile-section">
      <h2>Đổi mật khẩu</h2>
      <p className="profile-hint">Xác nhận mật khẩu hiện tại trước khi tạo mật khẩu mới.</p>
      <form className="profile-password-form" onSubmit={submit}>
        <label className="password-field">
          <span>Mật khẩu hiện tại</span>
          <div className="password-input-wrap">
            <input type={showCurrentPassword ? 'text' : 'password'} className="password-input password-input--with-toggle" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} autoComplete="current-password" required />
            <button
              type="button"
              className="password-eye"
              tabIndex={-1}
              aria-label={showCurrentPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
              onClick={() => setShowCurrentPassword((s) => !s)}
            >
              <EyeIcon off={showCurrentPassword} />
            </button>
          </div>
        </label>
        <label className="password-field">
          <span>Mật khẩu mới</span>
          <div className="password-input-wrap">
            <input type={showNewPassword ? 'text' : 'password'} className="password-input password-input--with-toggle" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} autoComplete="new-password" required />
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
            <input type={showConfirmPassword ? 'text' : 'password'} className="password-input password-input--with-toggle" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} autoComplete="new-password" required />
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
        {error && <div className="profile-alert error">{error}</div>}
        {success && <div className="profile-alert success">{success}</div>}
        <div className="profile-actions">
          <button className="btn-secondary" type="submit" disabled={submitting}>
            {submitting ? 'Đang đổi mật khẩu...' : 'Đổi mật khẩu'}
          </button>
        </div>
      </form>
    </section>
  );
}
