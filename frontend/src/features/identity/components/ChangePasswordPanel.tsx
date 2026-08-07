import type { FormEvent } from 'react';
import { useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { identityApi } from '../api/identityApi';

const PASSWORD_RULE = /^(?=.*[A-Za-z])(?=.*\d)[\x00-\x7F]{8,100}$/;

export function ChangePasswordPanel() {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
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
        <label>
          Mật khẩu hiện tại
          <input type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} autoComplete="current-password" required />
        </label>
        <label>
          Mật khẩu mới
          <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} autoComplete="new-password" required />
        </label>
        <label>
          Xác nhận mật khẩu mới
          <input type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} autoComplete="new-password" required />
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
