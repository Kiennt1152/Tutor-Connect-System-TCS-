import type { FormEvent } from 'react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { imageAssets } from '../../../assets/images/ImageAssets';
import { identityApi } from '../api/identityApi';
import './PasswordPages.css';

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [step, setStep] = useState<'email' | 'otp'>('email');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [cooldown, setCooldown] = useState(0);

  async function requestOtp(event?: FormEvent) {
    event?.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);
    try {
      const response = await identityApi.requestPasswordResetOtp({ email: email.trim() });
      setStep('otp');
      setMessage(response.message);
      setCooldown(response.resendCooldownSeconds ?? 60);
      window.setTimeout(() => setCooldown(0), (response.resendCooldownSeconds ?? 60) * 1000);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể gửi mã OTP. Vui lòng thử lại.'));
    } finally {
      setLoading(false);
    }
  }

  async function verifyOtp(event: FormEvent) {
    event.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);
    try {
      const response = await identityApi.verifyPasswordResetOtp({ email: email.trim(), code: code.trim() });
      if (!response.resetToken) throw new Error('Không nhận được quyền đặt lại mật khẩu.');
      navigate('/reset-password', { state: { resetToken: response.resetToken } });
    } catch (err) {
      setError(getApiErrorMessage(err, 'Mã OTP không hợp lệ. Vui lòng thử lại.'));
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
          <p className="password-eyebrow">Khôi phục tài khoản</p>
          <h1>Quên mật khẩu?</h1>
          <p className="password-subtitle">
            {step === 'email' ? 'Nhập email để nhận mã OTP xác nhận.' : `Mã OTP đã được gửi tới ${email.trim()}.`}
          </p>
          <form onSubmit={step === 'email' ? requestOtp : verifyOtp}>
            {step === 'email' ? (
              <label className="password-field">
                <span>Email</span>
                <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
              </label>
            ) : (
              <label className="password-field">
                <span>Mã OTP</span>
                <input
                  value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))}
                  inputMode="numeric"
                  pattern="[0-9]+"
                  placeholder="Nhập mã OTP"
                  required
                  autoFocus
                />
              </label>
            )}
            {message && <div className="password-alert password-alert--success">{message}</div>}
            {error && <div className="password-alert password-alert--error">{error}</div>}
            <button className="password-button" type="submit" disabled={loading}>
              {loading ? 'Đang xử lý...' : step === 'email' ? 'Gửi mã OTP' : 'Xác nhận OTP'}
            </button>
          </form>
          {step === 'otp' && (
            <button className="password-link-button" type="button" disabled={loading || cooldown > 0} onClick={() => void requestOtp()}>
              {cooldown > 0 ? `Gửi lại mã sau ${cooldown}s` : 'Gửi lại mã OTP'}
            </button>
          )}
          <Link className="password-back" to="/login">Quay lại đăng nhập</Link>
        </section>
      </main>
    </div>
  );
}
