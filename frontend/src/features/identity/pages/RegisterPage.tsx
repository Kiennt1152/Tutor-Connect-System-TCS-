import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import axios from 'axios';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { imageAssets } from '../../../assets/images/ImageAssets';
import { identityApi } from '../api/identityApi';
import type { RegisterRole } from '../types/identityTypes';
import './RegisterPage.css';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_REGEX = /^(0(3|5|7|8|9)\d{8}|\+84(3|5|7|8|9)\d{8})$/;
const PASSWORD_ASCII_REGEX = /^[\x00-\x7F]*$/;
const PASSWORD_STRENGTH_REGEX = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

const ROLE_OPTIONS: { value: RegisterRole; label: string }[] = [
  { value: 'CLIENT', label: 'Học viên / Phụ huynh' },
  { value: 'TUTOR', label: 'Gia sư' },
  { value: 'TUTOR_CENTER', label: 'Trung tâm' },
];

function extractError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as Record<string, string> | undefined;
    if (data) {
      if (typeof data.message === 'string') return data.message;
      const first = Object.values(data).find((v) => typeof v === 'string');
      if (first) return first;
    }
    if (error.code === 'ERR_NETWORK') {
      return 'Không kết nối được máy chủ. Hãy kiểm tra backend đang chạy ở cổng 8080.';
    }
  }
  return 'Đã có lỗi xảy ra. Vui lòng thử lại.';
}

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

function PasswordField({
  label,
  value,
  placeholder,
  onChange,
}: {
  label: string;
  value: string;
  placeholder?: string;
  onChange: (v: string) => void;
}) {
  const [show, setShow] = useState(false);
  return (
    <label className="reg-field">
      <span className="reg-label">{label}</span>
      <div className="reg-input-wrap">
        <input
          className="reg-input reg-input--toggle"
          type={show ? 'text' : 'password'}
          value={value}
          placeholder={placeholder}
          onChange={(e) => onChange(e.target.value)}
        />
        <button
          type="button"
          className="reg-eye"
          tabIndex={-1}
          aria-label={show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
          onClick={() => setShow((s) => !s)}
        >
          <EyeIcon off={show} />
        </button>
      </div>
    </label>
  );
}

function Header() {
  return (
    <header className="reg-header">
      <div className="reg-header__inner">
        <Link to="/" className="reg-logo" aria-label="Tutor Connect System">
          <img className="reg-logo__image" src={imageAssets.logo} alt="" />
          <span className="reg-logo__text">Tutor Connect System</span>
        </Link>
        <Link to="/" className="reg-header__login">
          Trang chủ
        </Link>
      </div>
    </header>
  );
}

export default function RegisterPage() {
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [role, setRole] = useState<RegisterRole>('CLIENT');
  const [email, setEmail] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [displayName, setDisplayName] = useState('');

  const [otpSent, setOtpSent] = useState(false);
  const [emailVerified, setEmailVerified] = useState(false);
  const [token, setToken] = useState<string | null>(null);
  const [otpLocked, setOtpLocked] = useState(false);
  const [cooldown, setCooldown] = useState(0);

  const [sending, setSending] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [otpMsg, setOtpMsg] = useState<{ type: 'error' | 'info'; text: string } | null>(null);
  const [formError, setFormError] = useState('');
  const [done, setDone] = useState(false);

  useEffect(() => {
    if (cooldown <= 0) return;
    const t = window.setInterval(() => setCooldown((c) => (c <= 1 ? 0 : c - 1)), 1000);
    return () => window.clearInterval(t);
  }, [cooldown]);

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  function resetVerification() {
    setOtpSent(false);
    setEmailVerified(false);
    setToken(null);
    setOtpLocked(false);
    setOtpMsg(null);
    setCooldown(0);
  }

  async function handleSendOtp() {
    if (!EMAIL_REGEX.test(email.trim())) {
      setOtpMsg({ type: 'error', text: 'Email không hợp lệ' });
      return;
    }
    setSending(true);
    setOtpMsg(null);
    try {
      const res = await identityApi.sendOtp({ email: email.trim(), role });
      setOtpSent(true);
      setOtpLocked(false);
      setCooldown(res.resendCooldownSeconds);
      setOtpMsg({ type: 'info', text: res.message });
    } catch (err) {
      setOtpMsg({ type: 'error', text: extractError(err) });
    } finally {
      setSending(false);
    }
  }

  async function handleVerifyOtp() {
    if (otpCode.length !== 6) return;
    setVerifying(true);
    setOtpMsg(null);
    try {
      const res = await identityApi.verifyOtp({ email: email.trim(), code: otpCode });
      setEmailVerified(true);
      setToken(res.verifiedEmailToken);
      // Chi giu badge "Email da duoc xac thuc", bo thong bao info trung.
      setOtpMsg(null);
    } catch (err) {
      const text = extractError(err);
      if (text.includes('quá số lần')) setOtpLocked(true);
      setOtpMsg({ type: 'error', text });
    } finally {
      setVerifying(false);
    }
  }

  function validateForm(): string | null {
    if (!emailVerified || !token) return 'Vui lòng xác thực email trước khi đăng ký.';
    if (!displayName.trim() || displayName.trim().length < 2 || displayName.trim().length > 50) {
      return 'Tên hiển thị phải từ 2 đến 50 ký tự';
    }
    if (!PHONE_REGEX.test(phone.trim())) {
      return 'Số điện thoại không hợp lệ (VD: 0901234567 hoặc +84901234567)';
    }
    if (!PASSWORD_ASCII_REGEX.test(password)) {
      return 'Mật khẩu không được chứa ký tự có dấu';
    }
    if (!PASSWORD_STRENGTH_REGEX.test(password)) {
      return 'Mật khẩu phải có ít nhất 8 ký tự, gồm cả chữ và số';
    }
    if (password !== confirmPassword) {
      return 'Mật khẩu xác nhận không khớp';
    }
    return null;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setFormError('');
    const validationError = validateForm();
    if (validationError) {
      setFormError(validationError);
      return;
    }
    setSubmitting(true);
    try {
      await identityApi.register({
        email: email.trim(),
        role,
        displayName: displayName.trim(),
        phone: phone.trim(),
        password,
        confirmPassword,
        verifiedEmailToken: token as string,
      });
      setDone(true);
    } catch (err) {
      const text = extractError(err);
      // AF-06: token xac thuc email het han/khong hop le -> xoa chi bao, bat xac thuc lai.
      if (text.includes('xác thực email')) {
        resetVerification();
        setFormError('');
        setOtpMsg({ type: 'error', text: 'Phiên xác thực email đã hết hạn. Vui lòng xác thực email lại.' });
      } else {
        setFormError(text);
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="reg-page">
      <Header />
      <main className="reg-main">
        <section className="reg-card">
            {done ? (
              <div className="reg-success">
                <div className="reg-success__icon">✓</div>
                <h1 className="reg-card__title">Đăng ký thành công!</h1>
                <p className="reg-card__sub">
                  Tài khoản <strong>{email.trim()}</strong> đã được kích hoạt. Bạn có thể đăng nhập
                  ngay bây giờ.
                </p>
                <button
                  type="button"
                  className="reg-btn reg-btn--block"
                  onClick={() => navigate('/login', { replace: true })}
                >
                  Đăng nhập
                </button>
                <Link to="/" className="reg-link reg-link--center">
                  Về trang chủ
                </Link>
              </div>
            ) : (
              <form onSubmit={handleSubmit} noValidate>
                <div className="reg-card__head">
                  <h1 className="reg-card__title">Tạo tài khoản</h1>
                  <p className="reg-card__sub">
                    Chọn loại tài khoản, xác thực email bằng OTP rồi hoàn tất thông tin.
                  </p>
                </div>

                <div className="reg-roles">
                  {ROLE_OPTIONS.map((o) => (
                    <button
                      type="button"
                      key={o.value}
                      className={`reg-role${role === o.value ? ' reg-role--active' : ''}`}
                      onClick={() => setRole(o.value)}
                    >
                      <span className="reg-role__label">{o.label}</span>
                    </button>
                  ))}
                </div>

                <label className="reg-field">
                  <span className="reg-label">Email</span>
                  <div className="reg-row">
                    <input
                      className="reg-input"
                      type="email"
                      value={email}
                      placeholder="ban@email.com"
                      readOnly={emailVerified}
                      onChange={(e) => {
                        if (emailVerified || otpSent) resetVerification();
                        setEmail(e.target.value);
                      }}
                    />
                    {emailVerified ? (
                      <button type="button" className="reg-btn reg-btn--ghost reg-row__btn" onClick={resetVerification}>
                        Đổi email
                      </button>
                    ) : (
                      <button
                        type="button"
                        className="reg-btn reg-btn--soft reg-row__btn"
                        onClick={handleSendOtp}
                        disabled={sending || cooldown > 0}
                      >
                        {sending ? 'Đang gửi…' : otpSent ? (cooldown > 0 ? `Gửi lại ${cooldown}s` : 'Gửi lại') : 'Gửi OTP'}
                      </button>
                    )}
                  </div>
                </label>

                {emailVerified ? (
                  <div className="reg-verified">
                    <span className="reg-verified__icon">✓</span> Email đã được xác thực
                  </div>
                ) : (
                  otpSent && (
                    <label className="reg-field">
                      <span className="reg-label">Mã OTP (6 chữ số)</span>
                      <div className="reg-row">
                        <input
                          className="reg-input reg-otp-input"
                          inputMode="numeric"
                          maxLength={6}
                          value={otpCode}
                          placeholder="______"
                          disabled={otpLocked}
                          onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                        />
                        <button
                          type="button"
                          className="reg-btn reg-row__btn"
                          onClick={handleVerifyOtp}
                          disabled={verifying || otpCode.length !== 6 || otpLocked}
                        >
                          {verifying ? 'Đang xác thực…' : 'Xác thực'}
                        </button>
                      </div>
                    </label>
                  )
                )}

                {otpMsg && (
                  <div className={`reg-alert ${otpMsg.type === 'error' ? 'reg-alert--error' : 'reg-alert--info'}`}>
                    {otpMsg.text}
                  </div>
                )}

                {emailVerified ? (
                  <div className="reg-fieldset">
                    <label className="reg-field">
                      <span className="reg-label">
                        {role === 'TUTOR_CENTER' ? 'Tên trung tâm' : 'Họ và tên'}
                      </span>
                      <input
                        className="reg-input"
                        value={displayName}
                        onChange={(e) => setDisplayName(e.target.value)}
                        placeholder={role === 'TUTOR_CENTER' ? 'Tên trung tâm' : 'Nguyễn Văn A'}
                      />
                    </label>
                    <label className="reg-field">
                      <span className="reg-label">Số điện thoại</span>
                      <input
                        className="reg-input"
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        placeholder="0901234567"
                      />
                    </label>
                    <div className="reg-grid2">
                      <PasswordField
                        label="Mật khẩu"
                        value={password}
                        placeholder="≥ 8 ký tự, chữ + số"
                        onChange={setPassword}
                      />
                      <PasswordField
                        label="Xác nhận mật khẩu"
                        value={confirmPassword}
                        placeholder="Nhập lại"
                        onChange={setConfirmPassword}
                      />
                    </div>
                  </div>
                ) : (
                  <p className="reg-hint">Xác thực email để tiếp tục điền thông tin đăng ký.</p>
                )}

                {formError && <div className="reg-alert reg-alert--error">{formError}</div>}

                <button type="submit" className="reg-btn reg-btn--block" disabled={!emailVerified || submitting}>
                  {submitting ? 'Đang xử lý…' : 'Đăng ký'}
                </button>

                <p className="reg-foot">
                  Đã có tài khoản? <Link to="/login" className="reg-link">Đăng nhập</Link>
                </p>
              </form>
            )}
        </section>
      </main>
    </div>
  );
}
