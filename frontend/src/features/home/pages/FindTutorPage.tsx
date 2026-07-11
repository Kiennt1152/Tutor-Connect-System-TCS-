import { useState } from 'react';
import axios from 'axios';
import { Link } from 'react-router-dom';
import { SiteHeader } from '../components/SiteHeader';
import { SiteFooter } from '../components/SiteFooter';
import { TutorListingCard } from '../components/TutorListingCard';
import { useHome } from '../hooks/useHome';
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

export default function FindTutorPage() {
  const { status, data, reload } = useHome();
  const { user, isAuthenticated } = useAuth();
  const isClient = user?.role === 'CLIENT';
  const { subjects, grades, provinces, createRequest } = useTutorRequestForm();

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [created, setCreated] = useState<ClassResponse | null>(null);
  const [formKey, setFormKey] = useState(0);

  const tutors = data?.featuredTutors ?? [];

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
            <div className="tcs-find-hero__intro">
              <h1 className="tcs-find-title">
                <span className="tcs-find-title__icon">🎓</span>
                <span className="tcs-find-title__text">Đăng yêu cầu tìm gia sư</span>
              </h1>
              <p className="tcs-find-subtitle">
                Điền thông tin nhu cầu học tập của bạn — môn học, mục tiêu, hình thức, học phí. Gia sư
                phù hợp sẽ nhận được yêu cầu và liên hệ với bạn.
              </p>
              {isClient && (
                <div className="tcs-find-actions">
                  <Link className="tcs-btn tcs-btn--ghost tcs-find-myreq" to={APP_ROUTES.marketplace}>
                    📋 Xem yêu cầu của tôi
                  </Link>
                </div>
              )}
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
                    Yêu cầu <strong>“{created.title}”</strong> (mã #{created.classId}) đang ở trạng
                    thái nháp. Vào trang quản lý để đăng công khai cho gia sư ứng tuyển.
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
                  provinces={provinces}
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

        <section id="tutor-list" className="tcs-section tcs-section--tutors">
          <div className="tcs-container">
            <div className="tcs-section-bar">
              <div>
                <h2 className="tcs-section-bar__title">Gia sư nổi bật</h2>
                <p className="tcs-section-bar__subtitle">
                  Tham khảo các gia sư tiêu biểu trên nền tảng trong lúc chờ yêu cầu được phản hồi.
                </p>
              </div>
              {status === 'success' && tutors.length > 0 ? (
                <span className="tcs-section-bar__count">{tutors.length} gia sư</span>
              ) : null}
            </div>

            {status === 'loading' && (
              <div className="tcs-search-results__state">
                <span className="tcs-spinner" aria-hidden="true" />
                Đang tải danh sách gia sư...
              </div>
            )}

            {status === 'error' && (
              <div className="tcs-search-results__state tcs-search-results__state--error">
                Không thể tải danh sách gia sư.
                <button
                  type="button"
                  className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                  onClick={reload}
                >
                  Thử lại
                </button>
              </div>
            )}

            {status === 'success' && tutors.length === 0 && (
              <p className="tcs-empty">Chưa có gia sư nào để hiển thị.</p>
            )}

            {status === 'success' && tutors.length > 0 && (
              <div className="tcs-listing-grid">
                {tutors.map((tutor) => (
                  <TutorListingCard
                    key={tutor.id}
                    tutor={tutor}
                    isAuthenticated={isAuthenticated}
                  />
                ))}
              </div>
            )}
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  );
}
