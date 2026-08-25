import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { SiteHeader } from '../components/SiteHeader';
import { SiteFooter } from '../components/SiteFooter';
import { TutorListingCard } from '../components/TutorListingCard';
import { useHome } from '../hooks/useHome';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { APP_ROUTES } from '../../../shared/constants/routes';
import './HomePage.css';
import './FindTutorPage.css';

/** Bỏ dấu tiếng Việt + hạ chữ thường để tìm kiếm không phân biệt dấu. */
const normalize = (value: string) =>
  value
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd')
    .toLowerCase()
    .trim();

const PAGE_SIZE = 6;

export default function FindTutorPage() {
  const { status, data, reload } = useHome();
  const { isAuthenticated } = useAuth();
  const [draft, setDraft] = useState(''); // chữ đang gõ
  const [query, setQuery] = useState(''); // từ khóa đã bấm "Tìm" (dùng để lọc)
  const [page, setPage] = useState(1);

  const tutors = data?.featuredTutors ?? [];

  const filteredTutors = useMemo(() => {
    const q = normalize(query);
    if (!q) return tutors;
    // Chỉ tìm theo họ và tên (khớp cả khi gõ đầy đủ họ tên lẫn chỉ gõ tên).
    return tutors.filter((tutor) => normalize(tutor.fullName).includes(q));
  }, [tutors, query]);

  const totalPages = Math.max(1, Math.ceil(filteredTutors.length / PAGE_SIZE));

  // Quay về trang 1 khi từ khóa (số kết quả) thay đổi; kẹp page trong khoảng hợp lệ.
  useEffect(() => {
    setPage(1);
  }, [query]);

  const currentPage = Math.min(page, totalPages);
  const pagedTutors = filteredTutors.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );

  return (
    <div className="tcs-page">
      <SiteHeader active="find-tutor" />
      <main>
        <section id="tutor-list" className="tcs-section tcs-section--tutors">
          <div className="tcs-container">
            <div className="tcs-find-topbar">
              <Link className="tcs-find-myreq tcs-find-myreq--lg" to={APP_ROUTES.postTutorRequest}>
                Đăng yêu cầu tìm gia sư
              </Link>
            </div>

            <form
              className="tcs-find-search"
              onSubmit={(event) => {
                event.preventDefault();
                setQuery(draft.trim());
              }}
            >
              <div className="tcs-find-search__field">
                <input
                  type="search"
                  className="tcs-find-search__input"
                  placeholder="Tìm gia sư theo tên..."
                  value={draft}
                  onChange={(event) => {
                    const value = event.target.value;
                    setDraft(value);
                    // Bấm dấu ✕ mặc định của trình duyệt (làm rỗng ô) -> reset luôn kết quả lọc.
                    if (value === '') setQuery('');
                  }}
                  aria-label="Tìm kiếm gia sư"
                />
              </div>
              <button type="submit" className="tcs-find-search__btn">
                Tìm
              </button>
            </form>

            <div className="tcs-section-bar tcs-find-listbar">
              <div>
                <h2 className="tcs-section-bar__title tcs-find-listbar__title">Danh sách gia sư</h2>
                <p className="tcs-section-bar__subtitle">
                  Tham khảo các gia sư tiêu biểu trên nền tảng và xem chi tiết hồ sơ.
                </p>
              </div>
              {status === 'success' && tutors.length > 0 ? (
                <span className="tcs-section-bar__count">
                  {query ? `${filteredTutors.length}/${tutors.length}` : tutors.length} gia sư
                </span>
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

            {status === 'success' && tutors.length > 0 && filteredTutors.length === 0 && (
              <p className="tcs-empty">Không tìm thấy gia sư khớp với "{query}".</p>
            )}

            {status === 'success' && filteredTutors.length > 0 && (
              <>
                <div className="tcs-listing-grid">
                  {pagedTutors.map((tutor) => (
                    <TutorListingCard
                      key={tutor.id}
                      tutor={tutor}
                      isAuthenticated={isAuthenticated}
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
