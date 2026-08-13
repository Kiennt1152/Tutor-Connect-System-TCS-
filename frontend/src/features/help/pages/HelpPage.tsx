import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useFaqSearch } from '../hooks/useHelp';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import './HelpPage.css';

function ChevronDown({ className }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
      <polyline points="6 9 12 15 18 9" />
    </svg>
  );
}

export default function HelpPage() {
  const { user } = useAuth();
  const { status, items, keyword, setKeyword, errorMessage, reload } = useFaqSearch();
  const [openFaqId, setOpenFaqId] = useState<number | null>(null);
  const [searchDraft, setSearchDraft] = useState('');

  const handleSearch = (e: FormEvent) => {
    e.preventDefault();
    setKeyword(searchDraft);
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
      </div>

      <div className="help-page__body">
        {/* FAQ accordion */}
        <section>
          <h2 className="help-faq__heading">
            Câu hỏi thường gặp
            {status === 'success' && <span className="help-faq__count">{items.length}</span>}
          </h2>

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
            <p className="help-faq__empty">
              {keyword ? `Không tìm thấy kết quả cho "${keyword}".` : 'Chưa có câu hỏi nào.'}
            </p>
          )}

          {status === 'success' && items.map((faq) => (
            <div key={faq.faqId} className="help-faq__item">
              <button
                type="button"
                className="help-faq__question-btn"
                onClick={() => setOpenFaqId(openFaqId === faq.faqId ? null : faq.faqId)}
                aria-expanded={openFaqId === faq.faqId}
              >
                <span>{faq.question}</span>
                <ChevronDown className={`help-faq__chevron${openFaqId === faq.faqId ? ' help-faq__chevron--open' : ''}`} />
              </button>
              {openFaqId === faq.faqId && (
                <div className="help-faq__answer">{faq.answer}</div>
              )}
            </div>
          ))}
        </section>

        <div className="help-page__sidebar">
          <section className="help-action-card help-action-card--primary" aria-labelledby="help-support-title">
            <div className="help-action-card__heading">
              <span className="help-action-card__icon" aria-hidden="true">🔍</span>
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
              <span className="help-action-card__icon help-action-card__icon--tickets" aria-hidden="true">▤</span>
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
