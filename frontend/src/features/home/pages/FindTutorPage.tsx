import { Link } from 'react-router-dom';
import { SiteHeader } from '../components/SiteHeader';
import { SiteFooter } from '../components/SiteFooter';
import { TutorListingCard } from '../components/TutorListingCard';
import { useHome } from '../hooks/useHome';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { APP_ROUTES } from '../../../shared/constants/routes';
import './HomePage.css';
import './FindTutorPage.css';

export default function FindTutorPage() {
  const { status, data, reload } = useHome();
  const { isAuthenticated } = useAuth();

  const tutors = data?.featuredTutors ?? [];

  return (
    <div className="tcs-page">
      <SiteHeader active="find-tutor" />
      <main>
        <section id="tutor-list" className="tcs-section tcs-section--tutors">
          <div className="tcs-container">
            <div className="tcs-find-topbar">
              <Link className="tcs-find-myreq tcs-find-myreq--lg" to={APP_ROUTES.postTutorRequest}>
                📝 Đăng yêu cầu tìm gia sư
              </Link>
            </div>

            <div className="tcs-section-bar tcs-find-listbar">
              <div>
                <h2 className="tcs-section-bar__title tcs-find-listbar__title">Danh sách gia sư</h2>
                <p className="tcs-section-bar__subtitle">
                  Tham khảo các gia sư tiêu biểu trên nền tảng và xem chi tiết hồ sơ.
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
