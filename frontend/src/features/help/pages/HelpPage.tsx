import { useState, useEffect, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useFaqSearch } from '../hooks/useHelp';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import './HelpPage.css';

const PAGE_SIZE = 8;

function ChevronDown({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <polyline points="6 9 12 15 18 9" />
    </svg>
  );
}

function SearchIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <circle cx="11" cy="11" r="8" />
      <line x1="21" y1="21" x2="16.65" y2="16.65" />
    </svg>
  );
}

function TicketIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <polyline points="14 2 14 8 20 8" />
      <line x1="16" y1="13" x2="8" y2="13" />
      <line x1="16" y1="17" x2="8" y2="17" />
      <line x1="10" y1="9" x2="8" y2="9" />
    </svg>
  );
}

function getVisiblePages(currentPage: number, totalPages: number): (number | 'ellipsis-start' | 'ellipsis-end')[] {
  if (totalPages <= 5) {
    return Array.from({ length: totalPages }, (_, i) => i + 1);
  }

  const pages: (number | 'ellipsis-start' | 'ellipsis-end')[] = [];

  // When at page 1 or 2: show 1, 2, 3, ..., totalPages
  if (currentPage <= 2) {
    pages.push(1, 2, 3);
    if (totalPages > 4) {
      pages.push('ellipsis-end');
    }
    pages.push(totalPages);
    return pages;
  }

  // When near the end: show ..., totalPages-2, totalPages-1, totalPages
  if (currentPage >= totalPages - 1) {
    pages.push('ellipsis-start');
    for (let i = totalPages - 2; i <= totalPages; i++) {
      pages.push(i);
    }
    return pages;
  }

  // Sliding window (e.g. at page 3 -> ... 2 3 4 ... totalPages; at page 4 -> ... 3 4 5 ... totalPages)
  pages.push('ellipsis-start');
  pages.push(currentPage - 1);
  pages.push(currentPage);
  pages.push(currentPage + 1);
  if (currentPage + 1 < totalPages) {
    pages.push('ellipsis-end');
  }
  pages.push(totalPages);

  return pages;
}

const FAQ_CATEGORIES = [
  { key: '', label: 'Tất cả chủ đề' },
  { key: 'AUTH_PROFILE', label: 'Tài khoản & Hồ sơ' },
  { key: 'VERIFICATION', label: 'Xác minh hồ sơ' },
  { key: 'MARKETPLACE', label: 'Lớp học & Tìm gia sư' },
  { key: 'TUTOR_OPS', label: 'Lịch dạy & Điểm danh' },
  { key: 'CENTER_OPS', label: 'Trung tâm gia sư' },
  { key: 'FINANCE_ESCROW', label: 'Ví tiền & Nạp rút' },
  { key: 'CONTRACT_REVIEW', label: 'Hợp đồng & Đánh giá' },
  { key: 'TRUST_SAFETY', label: 'An toàn & Khiếu nại' },
];

const CATEGORY_MAP: Record<string, string> = {
  AUTH_PROFILE: 'Tài khoản & Hồ sơ',
  VERIFICATION: 'Xác minh hồ sơ',
  MARKETPLACE: 'Lớp học & Tìm gia sư',
  TUTOR_OPS: 'Lịch dạy & Điểm danh',
  CENTER_OPS: 'Trung tâm gia sư',
  FINANCE_ESCROW: 'Ví tiền & Nạp rút',
  CONTRACT_REVIEW: 'Hợp đồng & Đánh giá',
  TRUST_SAFETY: 'An toàn & Khiếu nại',
  PLATFORM_ADMIN: 'Quản trị nền tảng',
};

export default function HelpPage() {
  const { user } = useAuth();
  const { status, items, keyword, setKeyword, category, setCategory, errorMessage, reload } = useFaqSearch();
  const [openFaqId, setOpenFaqId] = useState<number | null>(null);
  const [searchDraft, setSearchDraft] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

  // Reset to page 1 when search keyword, category or item list changes
  useEffect(() => {
    setCurrentPage(1);
    setOpenFaqId(null);
  }, [keyword, category, items.length]);

  const handleSearch = (e: FormEvent) => {
    e.preventDefault();
    setKeyword(searchDraft);
  };

  const handleCategorySelect = (selectedCat: string) => {
    setCategory(selectedCat);
  };

  const handleClearFilters = () => {
    setSearchDraft('');
    setKeyword('');
    setCategory('');
  };

  const totalPages = Math.max(1, Math.ceil(items.length / PAGE_SIZE));
  const startIndex = (currentPage - 1) * PAGE_SIZE;
  const endIndex = Math.min(startIndex + PAGE_SIZE, items.length);
  const paginatedItems = items.slice(startIndex, endIndex);
  const visiblePages = getVisiblePages(currentPage, totalPages);

  const handlePageChange = (newPage: number) => {
    if (newPage >= 1 && newPage <= totalPages) {
      setCurrentPage(newPage);
      setOpenFaqId(null);
      window.scrollTo({ top: 300, behavior: 'smooth' });
    }
  };

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <div className="help-page">

      {/* Hero + search */}
      <div className="help-page__hero">
        <h1 className="help-page__hero-title">Trung tâm hỗ trợ</h1>
        <p className="help-page__hero-subtitle">Tìm câu trả lời nhanh cho các câu hỏi thường gặp</p>
        <form className="help-page__search-bar" onSubmit={handleSearch}>
          <input
            className="help-page__search-input"
            type="text"
            placeholder="Nhập câu hỏi của bạn…"
            value={searchDraft}
            onChange={(e) => setSearchDraft(e.target.value)}
          />
          <button className="help-page__search-btn" type="submit">Tìm kiếm</button>
        </form>

        {/* Category Chips Bar */}
        <div className="help-category-bar" role="tablist" aria-label="Lọc theo danh mục">
          {FAQ_CATEGORIES.map((cat) => {
            const isActive = category === cat.key;
            return (
              <button
                key={cat.key}
                type="button"
                role="tab"
                aria-selected={isActive}
                className={`help-category-chip ${isActive ? 'help-category-chip--active' : ''}`}
                onClick={() => handleCategorySelect(cat.key)}
              >
                {cat.label}
              </button>
            );
          })}
        </div>
      </div>

      <div className="help-page__body">
        {/* FAQ accordion */}
        <section>
          <div className="help-faq__header-row">
            <h2 className="help-faq__heading">
              {category ? (CATEGORY_MAP[category] || category) : 'Câu hỏi thường gặp'}
              {status === 'success' && <span className="help-faq__count">{items.length}</span>}
            </h2>
            {status === 'success' && items.length > 0 && (
              <span className="help-faq__pagination-info">
                Hiển thị {startIndex + 1}–{endIndex} trong tổng số {items.length} câu
              </span>
            )}
          </div>

          {status === 'loading' && <p className="help-faq__empty">Đang tải…</p>}

          {status === 'error' && (
            <p className="help-faq__empty">
              {errorMessage ?? 'Không tải được FAQ.'}{' '}
              <button type="button" onClick={reload} style={{ textDecoration: 'underline', background: 'none', border: 'none', cursor: 'pointer', color: '#1a56db' }}>
                Thử lại
              </button>
            </p>
          )}

          {status === 'success' && items.length === 0 && (
            <div className="help-faq__empty-box">
              <p className="help-faq__empty">
                {keyword && category
                  ? `Không tìm thấy câu hỏi phù hợp cho "${keyword}" trong danh mục "${CATEGORY_MAP[category] || category}".`
                  : keyword
                  ? `Không tìm thấy kết quả cho "${keyword}".`
                  : category
                  ? `Chưa có câu hỏi nào trong danh mục "${CATEGORY_MAP[category] || category}".`
                  : 'Chưa có câu hỏi nào.'}
              </p>
              {(keyword || category) && (
                <button
                  type="button"
                  className="help-faq__clear-filter-btn"
                  onClick={handleClearFilters}
                >
                  Xóa bộ lọc tìm kiếm
                </button>
              )}
            </div>
          )}

          {status === 'success' && paginatedItems.map((faq) => (
            <div key={faq.faqId} className="help-faq__item">
              <button
                type="button"
                className="help-faq__question-btn"
                onClick={() => setOpenFaqId(openFaqId === faq.faqId ? null : faq.faqId)}
                aria-expanded={openFaqId === faq.faqId}
              >
                <div className="help-faq__question-content">
                  {faq.category && (
                    <span className="help-faq__category-tag">
                      {CATEGORY_MAP[faq.category] || faq.category}
                    </span>
                  )}
                  <span className="help-faq__question-title">{faq.question}</span>
                </div>
                <ChevronDown className={`help-faq__chevron${openFaqId === faq.faqId ? ' help-faq__chevron--open' : ''}`} />
              </button>
              {openFaqId === faq.faqId && (
                <div className="help-faq__answer">{faq.answer}</div>
              )}
            </div>
          ))}

          {/* Pagination Controls */}
          {status === 'success' && totalPages > 1 && (
            <div className="help-pagination">
              <button
                type="button"
                className="help-pagination__btn"
                disabled={currentPage === 1}
                onClick={() => handlePageChange(currentPage - 1)}
                aria-label="Trang trước"
              >
                ‹ Trước
              </button>

              <div className="help-pagination__pages">
                {visiblePages.map((item, idx) => {
                  if (item === 'ellipsis-start' || item === 'ellipsis-end') {
                    return (
                      <span key={`ellipsis-${idx}`} className="help-pagination__ellipsis">
                        …
                      </span>
                    );
                  }

                  const pageNum = item as number;
                  return (
                    <button
                      key={pageNum}
                      type="button"
                      className={`help-pagination__page-btn ${pageNum === currentPage ? 'help-pagination__page-btn--active' : ''}`}
                      onClick={() => handlePageChange(pageNum)}
                      aria-current={pageNum === currentPage ? 'page' : undefined}
                    >
                      {pageNum}
                    </button>
                  );
                })}
              </div>

              <button
                type="button"
                className="help-pagination__btn"
                disabled={currentPage === totalPages}
                onClick={() => handlePageChange(currentPage + 1)}
                aria-label="Trang sau"
              >
                Sau ›
              </button>
            </div>
          )}
        </section>

        <div className="help-page__sidebar">
          <section className="help-action-card help-action-card--primary" aria-labelledby="help-support-title">
            <div className="help-action-card__heading">
              <span className="help-action-card__icon" aria-hidden="true">
                <SearchIcon />
              </span>
              <h2 id="help-support-title">Không tìm thấy câu trả lời?</h2>
            </div>
            <p>Gửi yêu cầu hỗ trợ và đội ngũ của chúng tôi sẽ phản hồi trong 24 giờ.</p>
            {user ? (
              <Link to={APP_ROUTES.messagingTickets} className="help-action-card__button help-action-card__button--primary">
                Tạo yêu cầu hỗ trợ
              </Link>
            ) : (
              <Link
                to={APP_ROUTES.login}
                state={{ from: APP_ROUTES.messagingTickets }}
                className="help-action-card__button help-action-card__button--primary"
              >
                Tạo yêu cầu hỗ trợ
              </Link>
            )}
            <Link to={APP_ROUTES.aiAssistant} className="help-action-card__text-link">
              Hoặc chat với trợ lý AI cá nhân <span aria-hidden="true">→</span>
            </Link>
          </section>

          <section className="help-action-card" aria-labelledby="help-ticket-title">
            <div className="help-action-card__heading">
              <span className="help-action-card__icon help-action-card__icon--tickets" aria-hidden="true">
                <TicketIcon />
              </span>
              <h2 id="help-ticket-title">Yêu cầu hỗ trợ của tôi</h2>
            </div>
            <p>Theo dõi trạng thái và xem phản hồi từ đội ngũ hỗ trợ.</p>
            {user ? (
              <Link to={APP_ROUTES.messagingTickets} className="help-action-card__button help-action-card__button--secondary">
                Xem các yêu cầu <span aria-hidden="true">→</span>
              </Link>
            ) : (
              <Link
                to={APP_ROUTES.login}
                state={{ from: APP_ROUTES.messagingTickets }}
                className="help-action-card__button help-action-card__button--secondary"
              >
                Xem các yêu cầu <span aria-hidden="true">→</span>
              </Link>
            )}
          </section>
        </div>
      </div>
    </div>
    </div>
  );
}
