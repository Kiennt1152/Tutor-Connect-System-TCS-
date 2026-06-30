import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useRegister } from '../hooks/useRegister';
import type { RegisterFormValues, RegisterRole } from '../types/authTypes';
import './RegisterPage.css';

const ROLE_OPTIONS: { value: RegisterRole; label: string; desc: string }[] = [
  { value: 'CLIENT', label: 'Học viên / Phụ huynh', desc: 'Tìm và thuê gia sư phù hợp' },
  { value: 'TUTOR', label: 'Gia sư', desc: 'Nhận lớp và dạy học' },
  { value: 'TUTOR_CENTER', label: 'Trung tâm gia sư', desc: 'Quản lý đội ngũ và tuyển dụng' },
];

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_REGEX = /^(0\d{9}|\+84\d{9})$/;
const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d).{8,}$/;

const INITIAL_VALUES: RegisterFormValues = {
  role: 'CLIENT',
  email: '',
  password: '',
  confirmPassword: '',
  displayName: '',
  phone: '',
};

type FieldErrors = Partial<Record<keyof RegisterFormValues, string>>;

function validate(values: RegisterFormValues): FieldErrors {
  const errors: FieldErrors = {};

  if (!values.password) {
    errors.password = 'Vui lòng nhập mật khẩu';
  } else if (!PASSWORD_REGEX.test(values.password)) {
    errors.password = 'Mật khẩu phải có ít nhất 8 ký tự, gồm cả chữ và số';
  }

  if (!values.confirmPassword) {
    errors.confirmPassword = 'Vui lòng xác nhận mật khẩu';
  } else if (values.confirmPassword !== values.password) {
    errors.confirmPassword = 'Mật khẩu xác nhận không khớp';
  }

  if (!values.displayName.trim()) {
    errors.displayName = 'Vui lòng nhập tên hiển thị';
  } else if (values.displayName.trim().length < 2 || values.displayName.trim().length > 50) {
    errors.displayName = 'Tên hiển thị phải từ 2 đến 50 ký tự';
  }

  if (!values.phone.trim()) {
    errors.phone = 'Vui lòng nhập số điện thoại';
  } else if (!PHONE_REGEX.test(values.phone.trim())) {
    errors.phone = 'Số điện thoại không hợp lệ (10 số bắt đầu bằng 0, hoặc +84)';
  }

  return errors;
}

function Header() {
  return (
    <header className="tcs-header">
      <div className="tcs-container tcs-header__inner">
        <a className="tcs-logo" href="/">
          <span className="tcs-logo__mark">TC</span>
          <span className="tcs-logo__text">Tutor Connect</span>
        </a>
        <div className="tcs-header__actions">
          <a className="tcs-btn tcs-btn--ghost" href="/login">
            Đăng nhập
          </a>
        </div>
      </div>
    </header>
  );
}

function EyeIcon({ off }: { off: boolean }) {
  return off ? (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
      <line x1="1" y1="1" x2="23" y2="23" />
    </svg>
  ) : (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

function Field({
  label,
  name,
  type = 'text',
  value,
  placeholder,
  error,
  autoComplete,
  onChange,
}: {
  label: string;
  name: keyof RegisterFormValues;
  type?: string;
  value: string;
  placeholder?: string;
  error?: string;
  autoComplete?: string;
  onChange: (name: keyof RegisterFormValues, value: string) => void;
}) {
  const [show, setShow] = useState(false);
  const isPassword = type === 'password';
  const inputType = isPassword && show ? 'text' : type;

  const input = (
    <input
      className={`auth-input${error ? ' auth-input--error' : ''}${isPassword ? ' auth-input--with-toggle' : ''}`}
      type={inputType}
      name={name}
      value={value}
      placeholder={placeholder}
      autoComplete={autoComplete}
      onChange={(event) => onChange(name, event.target.value)}
    />
  );

  return (
    <label className="auth-field">
      <span className="auth-field__label">{label}</span>
      {isPassword ? (
        <div className="auth-input-wrap">
          {input}
          <button
            type="button"
            className="auth-eye"
            tabIndex={-1}
            aria-label={show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
            onClick={() => setShow((prev) => !prev)}
          >
            <EyeIcon off={show} />
          </button>
        </div>
      ) : (
        input
      )}
      {error && <span className="auth-field__error">{error}</span>}
    </label>
  );
}

type OtpMessage = { type: 'error' | 'info'; text: string };

function EmailVerificationSection({
  email,
  emailError,
  otpSent,
  emailVerified,
  sending,
  verifying,
  otpLocked,
  message,
  resendCooldown,
  onEmailChange,
  onSendOtp,
  onVerify,
  onChangeEmail,
}: {
  email: string;
  emailError?: string;
  otpSent: boolean;
  emailVerified: boolean;
  sending: boolean;
  verifying: boolean;
  otpLocked: boolean;
  message: OtpMessage | null;
  resendCooldown: number;
  onEmailChange: (value: string) => void;
  onSendOtp: () => void;
  onVerify: (code: string) => void;
  onChangeEmail: () => void;
}) {
  const [code, setCode] = useState('');
  const [seconds, setSeconds] = useState(0);

  useEffect(() => {
    setSeconds(resendCooldown);
  }, [resendCooldown]);

  useEffect(() => {
    if (seconds <= 0) {
      return;
    }
    const timer = window.setInterval(() => {
      setSeconds((prev) => (prev <= 1 ? 0 : prev - 1));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [seconds]);

  const canSend = seconds <= 0 && !sending;

  return (
    <div className="auth-section">
      <div className="auth-field">
        <span className="auth-field__label">Email</span>
        <div className="auth-inline">
          <input
            className={`auth-input${emailError ? ' auth-input--error' : ''}`}
            type="email"
            value={email}
            placeholder="ban@email.com"
            autoComplete="email"
            readOnly={emailVerified}
            onChange={(event) => onEmailChange(event.target.value)}
          />
          {emailVerified ? (
            <button type="button" className="tcs-btn tcs-btn--ghost auth-inline__btn" onClick={onChangeEmail}>
              Đổi email
            </button>
          ) : (
            <button
              type="button"
              className="tcs-btn tcs-btn--soft auth-inline__btn"
              onClick={onSendOtp}
              disabled={!canSend}
            >
              {sending ? 'Đang gửi…' : otpSent ? (canSend ? 'Gửi lại mã' : `Gửi lại ${seconds}s`) : 'Gửi OTP'}
            </button>
          )}
        </div>
        {emailError && <span className="auth-field__error">{emailError}</span>}
      </div>

      {emailVerified ? (
        <div className="auth-verified">
          <span className="auth-verified__icon">✓</span> Email đã được xác thực
        </div>
      ) : (
        otpSent && (
          <div className="auth-field">
            <span className="auth-field__label">Mã OTP (6 chữ số)</span>
            <div className="auth-inline">
              <input
                className="auth-input auth-otp-input"
                inputMode="numeric"
                maxLength={6}
                value={code}
                placeholder="______"
                disabled={otpLocked}
                onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
              />
              <button
                type="button"
                className="tcs-btn tcs-btn--primary auth-inline__btn"
                onClick={() => onVerify(code)}
                disabled={verifying || code.length !== 6 || otpLocked}
              >
                {verifying ? 'Đang xác thực…' : 'Xác thực'}
              </button>
            </div>
            {otpLocked && (
              <span className="auth-field__error">
                Đã nhập sai quá số lần cho phép. Vui lòng bấm “Gửi lại mã” để lấy mã mới.
              </span>
            )}
          </div>
        )
      )}

      {message && (
        <div className={`auth-alert ${message.type === 'error' ? 'auth-alert--error' : 'auth-alert--info'}`}>
          {message.text}
        </div>
      )}
    </div>
  );
}

function DoneLayout({ email }: { email: string }) {
  return (
    <div className="tcs-page auth-page">
      <Header />
      <main className="auth-main">
        <div className="auth-card auth-card--narrow auth-done">
          <div className="auth-done__icon">✓</div>
          <h1 className="auth-card__title">Đăng ký thành công!</h1>
          <p className="auth-card__subtitle">
            Tài khoản <strong>{email}</strong> đã được kích hoạt. Bạn có thể đăng nhập ngay bây giờ.
          </p>
          <a className="tcs-btn tcs-btn--primary auth-submit" href="/login">
            Đăng nhập
          </a>
          <a className="auth-link auth-back" href="/">
            Về trang chủ
          </a>
        </div>
      </main>
    </div>
  );
}

function RegisterPage() {
  const reg = useRegister();
  const [values, setValues] = useState<RegisterFormValues>(INITIAL_VALUES);
  const [errors, setErrors] = useState<FieldErrors>({});
  const [touched, setTouched] = useState(false);

  useEffect(() => {
    if (touched) {
      setErrors(validate(values));
    }
  }, [values, touched]);

  const update = (name: keyof RegisterFormValues, value: string) => {
    setValues((prev) => ({ ...prev, [name]: value }));
  };

  const selectRole = (role: RegisterRole) => {
    setValues((prev) => ({ ...prev, role }));
  };

  const handleEmailChange = (value: string) => {
    if (reg.emailVerified || reg.otpSent) {
      reg.resetVerification();
    }
    setValues((prev) => ({ ...prev, email: value }));
  };

  const emailValid = EMAIL_REGEX.test(values.email.trim());
  const emailError = useMemo(() => {
    if (!touched) {
      return undefined;
    }
    if (!values.email.trim()) {
      return 'Vui lòng nhập email';
    }
    if (!emailValid) {
      return 'Email không hợp lệ';
    }
    return undefined;
  }, [touched, values.email, emailValid]);

  const handleSendOtp = () => {
    if (!emailValid) {
      setTouched(true);
      return;
    }
    void reg.sendOtp(values.email, values.role);
  };

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    setTouched(true);
    const found = validate(values);
    setErrors(found);
    if (!reg.emailVerified) {
      return;
    }
    if (Object.keys(found).length === 0) {
      void reg.register(values);
    }
  };

  if (reg.done) {
    return <DoneLayout email={reg.doneEmail} />;
  }

  return (
    <div className="tcs-page auth-page">
      <Header />
      <main className="auth-main">
        <form className="auth-card" onSubmit={handleSubmit} noValidate>
          <div className="auth-card__head">
            <h1 className="auth-card__title">Tạo tài khoản</h1>
            <p className="auth-card__subtitle">
              Chọn loại tài khoản, xác thực email bằng mã OTP rồi hoàn tất thông tin.
            </p>
          </div>

          <div className="auth-roles">
            {ROLE_OPTIONS.map((option) => (
              <button
                type="button"
                key={option.value}
                className={`auth-role${values.role === option.value ? ' auth-role--active' : ''}`}
                onClick={() => selectRole(option.value)}
              >
                <span className="auth-role__label">{option.label}</span>
                <span className="auth-role__desc">{option.desc}</span>
              </button>
            ))}
          </div>

          <EmailVerificationSection
            email={values.email}
            emailError={emailError}
            otpSent={reg.otpSent}
            emailVerified={reg.emailVerified}
            sending={reg.sending}
            verifying={reg.verifying}
            otpLocked={reg.otpLocked}
            message={reg.otpMessage}
            resendCooldown={reg.resendCooldown}
            onEmailChange={handleEmailChange}
            onSendOtp={handleSendOtp}
            onVerify={(code) => void reg.verifyOtp(values.email, code)}
            onChangeEmail={reg.resetVerification}
          />

          <fieldset className="auth-fieldset" disabled={!reg.emailVerified}>
            {!reg.emailVerified && (
              <p className="auth-hint">Vui lòng xác thực email trước khi điền thông tin bên dưới.</p>
            )}

            <div className="auth-grid">
              <Field
                label="Mật khẩu"
                name="password"
                type="password"
                value={values.password}
                placeholder="Tối thiểu 8 ký tự, gồm chữ và số"
                autoComplete="new-password"
                error={errors.password}
                onChange={update}
              />
              <Field
                label="Xác nhận mật khẩu"
                name="confirmPassword"
                type="password"
                value={values.confirmPassword}
                placeholder="Nhập lại mật khẩu"
                autoComplete="new-password"
                error={errors.confirmPassword}
                onChange={update}
              />
              <Field
                label={values.role === 'TUTOR_CENTER' ? 'Tên trung tâm' : 'Tên hiển thị'}
                name="displayName"
                value={values.displayName}
                placeholder={values.role === 'TUTOR_CENTER' ? 'Tên trung tâm gia sư' : 'Nguyễn Văn A'}
                autoComplete="name"
                error={errors.displayName}
                onChange={update}
              />
              <Field
                label="Số điện thoại"
                name="phone"
                value={values.phone}
                placeholder="0xxxxxxxxx hoặc +84xxxxxxxxx"
                autoComplete="tel"
                error={errors.phone}
                onChange={update}
              />
            </div>
          </fieldset>

          {reg.formError && <div className="auth-alert auth-alert--error">{reg.formError}</div>}

          <button
            className="tcs-btn tcs-btn--primary auth-submit"
            type="submit"
            disabled={!reg.emailVerified || reg.submitting}
          >
            {reg.submitting ? 'Đang xử lý…' : 'Đăng ký'}
          </button>

          <p className="auth-foot">
            Đã có tài khoản?{' '}
            <a className="auth-link" href="/login">
              Đăng nhập
            </a>
          </p>
        </form>
      </main>
    </div>
  );
}

export default RegisterPage;
