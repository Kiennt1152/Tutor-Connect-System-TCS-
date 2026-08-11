import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useFaqSearch } from '../hooks/useHelp';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { CreateTicketForm } from '../../messaging/components/CreateTicketForm';
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
  const [showTicketForm, setShowTicketForm] = useState(false);
  const [ticketSubmitted, setTicketSubmitted] = useState(false);

  const handleSearch = (e: FormEvent) => {
    e.preventDefault();
    setKeyword(searchDraft);
  };

  const handleTicketCreated = () => {
    setShowTicketForm(false);
    setTicketSubmitted(true);
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
          <section className="help-faq">
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

          {/* Right sidebar for CTA & Ticket Form */}
          <aside className="help-side">
            {ticketSubmitted ? (
              <div className="help-cta__box help-cta__success">
                <div style={{ fontSize: '2.5rem', marginBottom: '0.5rem' }}>✅</div>
                <h3 className="help-cta__title" style={{ color: '#166534' }}>Gửi yêu cầu thành công!</h3>
                <p className="help-cta__desc" style={{ color: '#15803d', margin: '0.5rem 0 1.5rem' }}>
                  Yêu cầu hỗ trợ của bạn đã được gửi. Đội ngũ của chúng tôi sẽ phản hồi trong 24 giờ.
                </p>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--primary"
                    onClick={() => {
                      setTicketSubmitted(false);
                      setShowTicketForm(true);
                    }}
                  >
                    Tạo yêu cầu khác
                  </button>
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--secondary"
                    onClick={() => {
                      setTicketSubmitted(false);
                      setShowTicketForm(false);
                    }}
                  >
                    Đóng
                  </button>
                </div>
              </div>
            ) : !showTicketForm ? (
              <div className="help-cta__box">
                <h3 className="help-cta__title">🔍 Không tìm thấy câu trả lời?</h3>
                <p className="help-cta__desc">
                  Gửi yêu cầu hỗ trợ và đội ngũ của chúng tôi sẽ phản hồi trong 24 giờ.
                </p>

                {user ? (
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--primary"
                    onClick={() => {
                      setTicketSubmitted(false);
                      setShowTicketForm(true);
                    }}
                  >
                    Tạo yêu cầu hỗ trợ
                  </button>
                ) : (
                  <Link
                    to={APP_ROUTES.login}
                    state={{ from: APP_ROUTES.help }}
                    className="tcs-btn tcs-btn--primary"
                  >
                    Đăng nhập để gửi yêu cầu
                  </Link>
                )}

                <div style={{ marginTop: '1rem' }}>
                  <Link to={APP_ROUTES.aiAssistant} className="help-cta__link" style={{ color: 'var(--color-primary)', fontWeight: 500 }}>
                    Hoặc chat với trợ lý AI cá nhân →
                  </Link>
                </div>
              </div>
            ) : (
              <CreateTicketForm
                onSuccess={handleTicketCreated}
                onCancel={() => setShowTicketForm(false)}
              />
            )}

            {user && (
              <div className="help-cta__box" style={{ marginTop: '1rem' }}>
                <h3 className="help-cta__title">📋 Yêu cầu hỗ trợ của tôi</h3>
                <p className="help-cta__desc">
                  Xem trạng thái và phản hồi cho các yêu cầu bạn đã gửi.
                </p>
                <Link
                  to={APP_ROUTES.messagingTickets}
                  className="tcs-btn tcs-btn--secondary"
                  style={{ display: 'inline-block', textDecoration: 'none' }}
                >
                  Xem yêu cầu của tôi →
                </Link>
              </div>
            )}
          </aside>
        </div>
      </div>
    </div>
  );
}
