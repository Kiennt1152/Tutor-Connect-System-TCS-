import type { FormEvent } from 'react';
import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../../../shared/auth/AuthProvider';
import type { UserRole } from '../types/identityTypes';
import './AuthPages.css';

export default function RegisterPage() {
  const { register, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [role, setRole] = useState<UserRole>('CLIENT');
  const [companyName, setCompanyName] = useState('');
  const [licenseNo, setLicenseNo] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await register({
        email,
        password,
        fullName,
        phone,
        role,
        companyName: role === 'TUTOR_CENTER' ? companyName : undefined,
        licenseNo: role === 'TUTOR_CENTER' ? licenseNo : undefined,
      });
      navigate('/', { replace: true });
    } catch {
      setError('Không thể đăng ký. Kiểm tra lại thông tin.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={handleSubmit}>
        <h1>Đăng ký</h1>
        {error && <p className="auth-error">{error}</p>}
        <label>
          Vai trò
          <select value={role} onChange={(e) => setRole(e.target.value as UserRole)}>
            <option value="CLIENT">Phụ huynh / Học viên</option>
            <option value="TUTOR">Gia sư</option>
            <option value="TUTOR_CENTER">Trung tâm</option>
          </select>
        </label>
        <label>
          Họ tên
          <input value={fullName} onChange={(e) => setFullName(e.target.value)} required />
        </label>
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Số điện thoại
          <input value={phone} onChange={(e) => setPhone(e.target.value)} />
        </label>
        <label>
          Mật khẩu
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={6} />
        </label>
        {role === 'TUTOR_CENTER' && (
          <>
            <label>
              Tên trung tâm
              <input value={companyName} onChange={(e) => setCompanyName(e.target.value)} required />
            </label>
            <label>
              Mã giấy phép
              <input value={licenseNo} onChange={(e) => setLicenseNo(e.target.value)} required />
            </label>
          </>
        )}
        <button type="submit" disabled={loading}>
          {loading ? 'Đang đăng ký...' : 'Đăng ký'}
        </button>
        <p>
          Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
        </p>
      </form>
    </div>
  );
}
