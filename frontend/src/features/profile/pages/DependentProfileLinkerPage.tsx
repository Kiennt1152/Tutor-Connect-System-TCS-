import type { FormEvent } from 'react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { ClientLayout } from '../components/ClientLayout';
import { useDependentProfile } from '../hooks/useDependentProfile';
import type { Gender } from '../types/profileTypes';
import './DependentProfileLinkerPage.css';

function formatDate(value?: string) {
  if (!value) return '—';
  return new Date(value).toLocaleDateString('vi-VN');
}

export default function DependentProfileLinkerPage() {
  const navigate = useNavigate();
  const {
    status,
    errorMessage,
    profile,
    linkStatus,
    children,
    guardian,
    grades,
    mutationStatus,
    mutationError,
    reload,
    updateProfile,
    linkGuardian,
    createChild,
  } = useDependentProfile();

  const [dateOfBirth, setDateOfBirth] = useState('');
  const [gender, setGender] = useState<Gender | ''>('');
  const [parentEmail, setParentEmail] = useState('');
  const [childFullName, setChildFullName] = useState('');
  const [childDateOfBirth, setChildDateOfBirth] = useState('');
  const [childGender, setChildGender] = useState<Gender | ''>('');
  const [childGradeId, setChildGradeId] = useState('');
  const [childSchoolName, setChildSchoolName] = useState('');

  async function handleUpdateDob(e: FormEvent) {
    e.preventDefault();
    if (!dateOfBirth) return;
    const ok = await updateProfile({
      dateOfBirth,
      gender: gender || undefined,
    });
    if (ok) reload();
  }

  async function handleLinkGuardian(e: FormEvent) {
    e.preventDefault();
    if (!parentEmail.trim()) return;
    await linkGuardian(parentEmail.trim());
  }

  async function handleCreateChild(e: FormEvent) {
    e.preventDefault();
    if (!childFullName.trim()) return;
    const ok = await createChild({
      fullName: childFullName.trim(),
      dateOfBirth: childDateOfBirth || undefined,
      gender: childGender || undefined,
      gradeId: childGradeId ? Number(childGradeId) : undefined,
      schoolName: childSchoolName || undefined,
    });
    if (ok) {
      setChildFullName('');
      setChildDateOfBirth('');
      setChildGender('');
      setChildGradeId('');
      setChildSchoolName('');
    }
  }

  const canContinue = linkStatus?.canProceedToPayment ?? false;

  return (
    <ClientLayout
      title="Liên kết hồ sơ phụ thuộc"
      subtitle="Cung cấp thông tin phụ huynh hoặc con để tối ưu hóa trải nghiệm học tập và thanh toán."
    >
      {status === 'loading' && <div className="dpl-state">Đang tải thông tin…</div>}

      {status === 'error' && (
        <div className="dpl-card">
          <div className="dpl-alert dpl-alert--error">{errorMessage}</div>
          {(errorMessage?.includes('đăng nhập') || errorMessage?.includes('máy chủ')) && (
            <p className="dpl-muted">
              {errorMessage.includes('máy chủ')
                ? 'Khởi động backend trong IntelliJ hoặc chạy mvn spring-boot:run.'
                : 'Đăng nhập bằng tài khoản CLIENT (Phụ huynh / Học viên).'}
            </p>
          )}
          <div className="dpl-actions">
            <button className="tcs-btn tcs-btn--primary" type="button" onClick={reload}>
              Thử lại
            </button>
            <Link className="tcs-btn tcs-btn--ghost" to="/login" state={{ from: APP_ROUTES.profileDependents }}>
              Đăng nhập
            </Link>
          </div>
        </div>
      )}

      {status === 'success' && linkStatus && (
        <>
          <section className="dpl-card dpl-status-banner">
            <div className="dpl-status-banner__content">
              <h2>Trạng thái liên kết</h2>
              {linkStatus.dateOfBirthMissing && (
                <p className="dpl-muted">
                  Vui lòng cập nhật ngày sinh để hệ thống xác định bạn là học sinh hay phụ huynh.
                </p>
              )}
              {linkStatus.minorAccount && (
                <p>
                  Tài khoản của bạn thuộc <strong>học sinh vị thành niên</strong>. Bạn cần liên kết
                  hồ sơ phụ huynh trước khi thanh toán hoặc tạo hợp đồng với gia sư.
                </p>
              )}
              {linkStatus.legalProceduresDelegatedToParent && (
                <p>
                  Sau khi liên kết, học sinh có thể gửi yêu cầu thanh toán và hợp đồng. Hệ thống sẽ thông báo
                  cho phụ huynh <strong>{linkStatus.legalAccountHolderName}</strong>
                  {linkStatus.legalAccountEmail ? ` (${linkStatus.legalAccountEmail})` : ''} qua ứng dụng và
                  email để xác nhận trước khi có hiệu lực.
                </p>
              )}
              {linkStatus.childrenLinkOptional && (
                <p>
                  Tài khoản của bạn thuộc <strong>phụ huynh / người lớn</strong>. Bạn có thể thêm
                  hồ sơ con (tùy chọn) để đăng ký lớp học thay mặt con.
                </p>
              )}
            </div>
            <div className="dpl-status-banner__badge">
              {canContinue ? (
                <span className="tcs-badge tcs-badge--active">Đủ điều kiện tiếp tục</span>
              ) : (
                <span className="tcs-badge tcs-badge--suspended">Cần bổ sung thông tin</span>
              )}
            </div>
          </section>

          {mutationStatus === 'error' && mutationError && (
            <div className="dpl-alert dpl-alert--error">{mutationError}</div>
          )}

          {linkStatus.dateOfBirthMissing && (
            <section className="dpl-card">
              <h2 className="dpl-section-title">Cập nhật ngày sinh</h2>
              <p className="dpl-muted">
                Xin chào {profile?.fullName ?? 'bạn'}. Hãy cung cấp ngày sinh để xác định luồng liên
                kết phù hợp.
              </p>
              <form className="dpl-form" onSubmit={handleUpdateDob}>
                <label>
                  Ngày sinh *
                  <input
                    className="dpl-field"
                    type="date"
                    value={dateOfBirth}
                    onChange={(e) => setDateOfBirth(e.target.value)}
                    required
                  />
                </label>
                <label>
                  Giới tính
                  <select
                    className="dpl-field"
                    value={gender}
                    onChange={(e) => setGender(e.target.value as Gender | '')}
                  >
                    <option value="">— Chọn —</option>
                    <option value="MALE">Nam</option>
                    <option value="FEMALE">Nữ</option>
                  </select>
                </label>
                <button
                  className="tcs-btn tcs-btn--primary"
                  type="submit"
                  disabled={mutationStatus === 'loading'}
                >
                  {mutationStatus === 'loading' ? 'Đang lưu…' : 'Lưu thông tin'}
                </button>
              </form>
            </section>
          )}

          {linkStatus.minorAccount && (
            <section className="dpl-card">
              <h2 className="dpl-section-title">Liên kết phụ huynh</h2>
              <p className="dpl-muted">
                Nhập email tài khoản phụ huynh (đã đăng ký trên TutorConnect, từ 18 tuổi trở lên).
              </p>

              {guardian ? (
                <div className="dpl-linked-card">
                  <div className="dpl-linked-card__header">
                    <span className="tcs-badge tcs-badge--active">Đã liên kết</span>
                  </div>
                  <dl className="dpl-dl">
                    <div>
                      <dt>Họ tên</dt>
                      <dd>{guardian.fullName}</dd>
                    </div>
                    <div>
                      <dt>Email</dt>
                      <dd>{guardian.email}</dd>
                    </div>
                    <div>
                      <dt>Số điện thoại</dt>
                      <dd>{guardian.phone}</dd>
                    </div>
                    <div>
                      <dt>Liên kết lúc</dt>
                      <dd>{formatDate(guardian.linkedAt)}</dd>
                    </div>
                  </dl>
                </div>
              ) : (
                <form className="dpl-form" onSubmit={handleLinkGuardian}>
                  <label>
                    Email phụ huynh *
                    <input
                      className="dpl-field"
                      type="email"
                      placeholder="phuhuynh@email.com"
                      value={parentEmail}
                      onChange={(e) => setParentEmail(e.target.value)}
                      required
                    />
                  </label>
                  <button
                    className="tcs-btn tcs-btn--primary"
                    type="submit"
                    disabled={mutationStatus === 'loading'}
                  >
                    {mutationStatus === 'loading' ? 'Đang liên kết…' : 'Liên kết phụ huynh'}
                  </button>
                </form>
              )}
            </section>
          )}

          {linkStatus.childrenLinkOptional && (
            <>
              <section className="dpl-card">
                <h2 className="dpl-section-title">Hồ sơ con (tùy chọn)</h2>
                <p className="dpl-muted">
                  Thêm thông tin con giúp bạn đăng ký lớp học và theo dõi tiến độ dễ dàng hơn. Bạn
                  có thể bỏ qua bước này và quay lại sau.
                </p>

                {children.length === 0 ? (
                  <p className="dpl-empty">Chưa có hồ sơ con nào.</p>
                ) : (
                  <ul className="dpl-child-list">
                    {children.map((child) => (
                      <li key={child.childProfileId} className="dpl-child-item">
                        <div>
                          <strong>{child.fullName}</strong>
                          {child.gradeName && (
                            <span className="dpl-child-item__meta"> · {child.gradeName}</span>
                          )}
                        </div>
                        <span className="dpl-muted">{formatDate(child.dateOfBirth)}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </section>

              <section className="dpl-card">
                <h2 className="dpl-section-title">Thêm hồ sơ con mới</h2>
                <form className="dpl-form dpl-form--grid" onSubmit={handleCreateChild}>
                  <label>
                    Họ tên con *
                    <input
                      className="dpl-field"
                      value={childFullName}
                      onChange={(e) => setChildFullName(e.target.value)}
                      required
                    />
                  </label>
                  <label>
                    Ngày sinh
                    <input
                      className="dpl-field"
                      type="date"
                      value={childDateOfBirth}
                      onChange={(e) => setChildDateOfBirth(e.target.value)}
                    />
                  </label>
                  <label>
                    Giới tính
                    <select
                      className="dpl-field"
                      value={childGender}
                      onChange={(e) => setChildGender(e.target.value as Gender | '')}
                    >
                      <option value="">— Chọn —</option>
                      <option value="MALE">Nam</option>
                      <option value="FEMALE">Nữ</option>
                    </select>
                  </label>
                  <label>
                    Khối / Lớp
                    <select
                      className="dpl-field"
                      value={childGradeId}
                      onChange={(e) => setChildGradeId(e.target.value)}
                    >
                      <option value="">— Chọn —</option>
                      {grades.map((grade) => (
                        <option key={grade.id} value={grade.id}>
                          {grade.name}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="dpl-form__full">
                    Trường học
                    <input
                      className="dpl-field"
                      value={childSchoolName}
                      onChange={(e) => setChildSchoolName(e.target.value)}
                    />
                  </label>
                  <div className="dpl-form__full">
                    <button
                      className="tcs-btn tcs-btn--primary"
                      type="submit"
                      disabled={mutationStatus === 'loading'}
                    >
                      {mutationStatus === 'loading' ? 'Đang thêm…' : 'Thêm hồ sơ con'}
                    </button>
                  </div>
                </form>
              </section>
            </>
          )}

          <section className="dpl-actions">
            {canContinue ? (
              <>
                <button
                  className="tcs-btn tcs-btn--primary"
                  type="button"
                  onClick={() => navigate(APP_ROUTES.finance)}
                >
                  Tiếp tục thanh toán
                </button>
                <button
                  className="tcs-btn tcs-btn--ghost"
                  type="button"
                  onClick={() => navigate(APP_ROUTES.contract)}
                >
                  Tạo hợp đồng với gia sư
                </button>
              </>
            ) : (
              <p className="dpl-muted dpl-actions__hint">
                Hoàn tất các bước bắt buộc ở trên để tiếp tục thanh toán hoặc tạo hợp đồng.
              </p>
            )}
            <Link className="tcs-btn tcs-btn--ghost" to={APP_ROUTES.home}>
              Về trang chủ
            </Link>
          </section>
        </>
      )}
    </ClientLayout>
  );
}
