/**
 * ====================================================================================================
 * [UC-09] MÀN HÌNH GIAO DIỆN TUTORREVIEWSPAGE
 * ====================================================================================================
 * Nghiệp vụ chính:
 * 1. Hiển thị và điều phối các chức năng nghiệp vụ của phân hệ TutorReviewsPage.
 * 2. Đảm bảo trải nghiệm người dùng tối ưu và đồng bộ dữ liệu với hệ thống Backend.
 * * @author Vũ Quốc Khánh (khanhvqhe176783)
 */
import { useEffect, useState } from 'react';
import { SiteHeader } from '../components/SiteHeader';
import { SiteFooter } from '../components/SiteFooter';
import { TutorListingCard } from '../components/TutorListingCard';
import { useTutorSearch } from '../hooks/useTutorSearch';
import { tutorSearchToFeatured } from '../mappers/tutorSearchMapper';
import { useAuth } from '../../../shared/auth/AuthProvider';
import './HomePage.css';

/** 6 gia sư mỗi trang: 2 hàng × 3 thẻ. */
const PAGE_SIZE = 6;

/** Trang công khai "Đánh giá": danh sách mọi gia sư kèm điểm sao, bấm để xem hồ sơ/đánh giá. */
export default function TutorReviewsPage() {
  const { status, results, search } = useTutorSearch();
  const { isAuthenticated } = useAuth();
  const [page, setPage] = useState(1);

  useEffect(() => {
    search({});
  }, [search]);

  // Danh sách đổi (tải lại) thì quay về trang đầu.
  useEffect(() => {
    setPage(1);
  }, [results]);

  const totalPages = Math.max(1, Math.ceil(results.length / PAGE_SIZE));
  const currentPage = Math.min(page, totalPages);
  const pagedResults = results.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

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
              <>
                <div className="tcs-listing-grid tcs-listing-grid--3col">
                  {pagedResults.map((tutor) => (
                    <TutorListingCard
                      key={tutor.id}
                      tutor={tutorSearchToFeatured(tutor)}
                      isAuthenticated={isAuthenticated}
                      showPrice={false}
                    />
                  ))}
                </div>

                {totalPages > 1 && (
                  <nav className="tcs-pagination" aria-label="Phân trang gia sư">
                    <button
                      type="button"
                      className="tcs-pagination__nav"
                      onClick={() => setPage((p) => Math.max(1, p - 1))}
                      disabled={currentPage === 1}
                    >
                      ← Trước
                    </button>
                    {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
                      <button
                        key={p}
                        type="button"
                        className={`tcs-pagination__page${p === currentPage ? ' tcs-pagination__page--active' : ''}`}
                        onClick={() => setPage(p)}
                        aria-current={p === currentPage ? 'page' : undefined}
                      >
                        {p}
                      </button>
                    ))}
                    <button
                      type="button"
                      className="tcs-pagination__nav"
                      onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                      disabled={currentPage === totalPages}
                    >
                      Sau →
                    </button>
                  </nav>
                )}
              </>
            )}
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  );
}
