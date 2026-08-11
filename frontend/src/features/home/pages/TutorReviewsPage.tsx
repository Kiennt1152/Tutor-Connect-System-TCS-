import { useEffect } from 'react';
import { SiteHeader } from '../components/SiteHeader';
import { SiteFooter } from '../components/SiteFooter';
import { TutorListingCard } from '../components/TutorListingCard';
import { useTutorSearch } from '../hooks/useTutorSearch';
import { tutorSearchToFeatured } from '../mappers/tutorSearchMapper';
import { useAuth } from '../../../shared/auth/AuthProvider';
import './HomePage.css';

export default function TutorReviewsPage() {
  const { status, results, search } = useTutorSearch();
  const { isAuthenticated } = useAuth();

  useEffect(() => {
    search({});
  }, [search]);

  return (
    <div className="tcs-page">
      <SiteHeader active="reviews" />
      <main>
        <section className="tcs-section tcs-section--tutors">
          <div className="tcs-container">
            <div className="tcs-section-bar">
              <div>
                <h2 className="tcs-section-bar__title">Đánh giá</h2>
                <p className="tcs-section-bar__subtitle">
                  Xem điểm đánh giá của tất cả gia sư trên nền tảng — bấm “Xem hồ sơ” để đọc chi tiết
                  nhận xét và lọc theo số sao.
                </p>
              </div>
              {status === 'success' && results.length > 0 ? (
                <span className="tcs-section-bar__count">{results.length} gia sư</span>
              ) : null}
            </div>

            {status === 'loading' && (
              <div className="tcs-search-results__state">
                <span className="tcs-spinner" aria-hidden="true" />
                Đang tải danh sách gia sư…
              </div>
            )}
            {status === 'error' && (
              <p className="tcs-empty">Không tải được danh sách gia sư. Vui lòng thử lại.</p>
            )}
            {status === 'success' && results.length === 0 && (
              <p className="tcs-empty">Chưa có gia sư nào để hiển thị.</p>
            )}
            {status === 'success' && results.length > 0 && (
              <div className="tcs-listing-grid">
                {results.map((tutor) => (
                  <TutorListingCard
                    key={tutor.id}
                    tutor={tutorSearchToFeatured(tutor)}
                    isAuthenticated={isAuthenticated}
                    showPrice={false}
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
