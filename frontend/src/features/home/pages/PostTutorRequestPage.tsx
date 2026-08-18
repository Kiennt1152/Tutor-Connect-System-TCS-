import { useState } from 'react';
import axios from 'axios';
import { Link } from 'react-router-dom';
import { SiteHeader } from '../components/SiteHeader';
import { SiteFooter } from '../components/SiteFooter';
import { useTutorRequestForm } from '../hooks/useTutorRequestForm';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { ClassRequestForm } from '../../marketplace/components/ClassRequestForm';
import { emptyForm } from '../../marketplace/mappers/marketplaceMapper';
import type {
  ClassRequestPayload,
  ClassResponse,
} from '../../marketplace/types/marketplaceTypes';
import './HomePage.css';
import './FindTutorPage.css';

function extractError(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string } | undefined;
    if (data?.message) return data.message;
  }
  if (err instanceof Error) return err.message;
  return 'Có lỗi xảy ra. Vui lòng thử lại.';
}

export default function PostTutorRequestPage() {
  const { user, isAuthenticated } = useAuth();
  const isClient = user?.role === 'CLIENT';
  const { subjects, grades, createRequest } = useTutorRequestForm();

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [created, setCreated] = useState<ClassResponse | null>(null);
  const [formKey, setFormKey] = useState(0);

  async function handleSubmit(payload: ClassRequestPayload) {
    if (!isClient) {
      setError('Vui lòng đăng nhập bằng tài khoản Client (Phụ huynh/Học viên) để đăng yêu cầu.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const result = await createRequest(payload);
      setCreated(result);
    } catch (err) {
      setError(extractError(err));
    } finally {
      setSubmitting(false);
    }
  }

  function resetForm() {
    setCreated(null);
    setError(null);
    setFormKey((k) => k + 1);
  }

  return (
    <div className="tcs-page">
      <SiteHeader active="find-tutor" />
      <main>
        <section className="tcs-home-hero tcs-find-hero">
          <div className="tcs-container">
            <Link className="tcs-find-back tcs-find-back--btn" to={APP_ROUTES.findTutor}>
              ← Quay lại danh sách gia sư
            </Link>
            <div className="tcs-find-hero__intro">
              <h1 className="tcs-find-title tcs-find-title--compact">
                <span className="tcs-find-title__icon">🎓</span>
                <span className="tcs-find-title__text tcs-find-title__text--plain">Đăng yêu cầu tìm gia sư</span>
              </h1>
              <p className="tcs-find-subtitle tcs-find-subtitle--compact">
                Điền thông tin nhu cầu học tập của bạn — môn học, mục tiêu, hình thức, học phí. Gia sư
                phù hợp sẽ nhận được yêu cầu và liên hệ với bạn.
              </p>
            </div>

            <div className="tcs-find-form-card">
              {!isAuthenticated && (
                <div className="tcs-find-banner">
                  <span>
                    Bạn cần đăng nhập bằng tài khoản Client (Phụ huynh/Học viên) để đăng yêu cầu.
                  </span>
                  <Link className="tcs-btn tcs-btn--market tcs-btn--sm" to={APP_ROUTES.login}>
                    Đăng nhập
                  </Link>
                </div>
              )}
              {isAuthenticated && !isClient && (
                <div className="tcs-find-banner tcs-find-banner--warn">
                  Chỉ tài khoản Client (Phụ huynh/Học viên) mới đăng được yêu cầu tìm gia sư.
                </div>
              )}

              {created ? (
                <div className="tcs-find-success">
                  <div className="tcs-find-success__icon">✓</div>
                  <h2>Đã gửi yêu cầu tìm gia sư!</h2>
                  <p>
                    Yêu cầu <strong>“{created.title}”</strong> đang ở trạng thái nháp. Vào trang
                    quản lý để đăng công khai cho gia sư ứng tuyển.
                  </p>
                  <div className="tcs-find-success__actions">
                    <Link className="tcs-btn tcs-btn--market" to={APP_ROUTES.marketplace}>
                      Quản lý yêu cầu
                    </Link>
                    <button type="button" className="tcs-btn tcs-btn--ghost" onClick={resetForm}>
                      Đăng yêu cầu khác
                    </button>
                  </div>
                </div>
              ) : (
                <ClassRequestForm
                  key={formKey}
                  initial={emptyForm()}
                  subjects={subjects}
                  grades={grades}
                  isEdit={false}
                  submitting={submitting}
                  error={error}
                  onSubmit={handleSubmit}
                  onCancel={resetForm}
                />
              )}
            </div>
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  );
}
