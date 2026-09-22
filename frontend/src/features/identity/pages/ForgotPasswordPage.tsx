/**
 * ============================================================================
 * [UC-01] QUÊN MẬT KHẨU & YÊU CẦU ĐẶT LẠI QUA EMAIL (FORGOT PASSWORD PAGE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-08-07
 * 
 * Mô tả Use Case:
 *   - Cho phép người dùng khôi phục quyền truy cập tài khoản khi bị quên mật khẩu đăng nhập.
 *   - Gửi liên kết đặt lại mật khẩu bảo mật kèm mã token có thời hạn sử dụng giới hạn về email.
 * 
 * Chức năng chính:
 *   1. Tiếp nhận yêu cầu: Nhập địa chỉ email đăng ký tài khoản cần khôi phục.
 *   2. Kiểm tra tính hợp lệ: Xác thực định dạng email và cơ chế chống spam yêu cầu liên tục.
 *   3. Gửi email hướng dẫn: Hệ thống gửi đường dẫn kèm token bảo mật một lần tới hộp thư người dùng.
 *   4. Thông báo và điều hướng: Hiển thị hướng dẫn kiểm tra hòm thư đến hoặc quay lại đăng nhập.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Người dùng nhập địa chỉ email vào biểu mẫu quên mật khẩu.
 *   - Bước 2: Nhấn "Gửi yêu cầu", client gọi API `identityApi.forgotPassword`.
 *   - Bước 3: Backend kiểm tra, sinh mã token bí mật và gửi email hướng dẫn đặt lại mật khẩu.
 *   - Bước 4: Giao diện hiển thị màn hình xác nhận yêu cầu thành công và hướng dẫn bước tiếp theo.
 * ============================================================================
 */

import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
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

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = window.setInterval(() => {
      setCooldown((prev) => (prev <= 1 ? 0 : prev - 1));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [cooldown]);

  async function requestOtp(event?: FormEvent) {
    event?.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);
    try {
      const response = await identityApi.requestPasswordResetOtp({ email: email.trim() });
      setStep('otp');
      setMessage(response.message || 'OTP đã được gửi thành công.');
      setCooldown(response.resendCooldownSeconds ?? 60);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Email không tồn tại trong hệ thống.'));
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
