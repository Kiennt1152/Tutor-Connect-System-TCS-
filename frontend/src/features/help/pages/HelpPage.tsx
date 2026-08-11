import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useChatbot, useFaqSearch } from '../hooks/useHelp';
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
  const { input, setInput, status: chatStatus, result, errorMessage: chatError, ask, reset } = useChatbot();
  const [openFaqId, setOpenFaqId] = useState<number | null>(null);
  const [searchDraft, setSearchDraft] = useState('');

  const handleSearch = (e: FormEvent) => {
    e.preventDefault();
    setKeyword(searchDraft);
  };

  const handleChatSubmit = (e: FormEvent) => {
    e.preventDefault();
    ask(input);
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

        {/* Chatbot panel */}
        <aside className="help-chatbot">
          <p className="help-chatbot__title">Hỏi chatbot hỗ trợ</p>
          <p className="help-chatbot__desc">Nhập câu hỏi, chatbot AI sẽ trả lời cho bạn.</p>

          {result === null ? (
            <form className="help-chatbot__form" onSubmit={handleChatSubmit}>
              <textarea
                className="help-chatbot__textarea"
                placeholder="Nhập câu hỏi của bạn…"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                rows={3}
              />
              <button
                type="submit"
                className="help-chatbot__ask-btn"
                disabled={chatStatus === 'loading' || !input.trim()}
              >
                {chatStatus === 'loading' ? 'Đang xử lý…' : 'Gửi câu hỏi'}
              </button>
              {chatError && (
                <p style={{ color: '#991b1b', fontSize: '0.83rem', margin: '0.25rem 0 0' }}>{chatError}</p>
              )}
            </form>
          ) : (
            <div>
              {result.matched ? (
                <div className="help-chatbot__result help-chatbot__result--matched">
                  <span className="help-chatbot__result-label">Câu trả lời từ AI</span>
                  <p className="help-chatbot__result-q">❓ {result.question}</p>
                  <p className="help-chatbot__result-a">💡 {result.answer}</p>
                </div>
              ) : (
                <div className="help-chatbot__result help-chatbot__result--unmatched">
                  <span className="help-chatbot__result-label">Chưa tìm được câu trả lời</span>
                  <p style={{ margin: '0 0 0.25rem' }}>{result.suggestion}</p>
                </div>
              )}

              {result.matched && (
                <div className="help-chatbot__feedback">
                  <p className="help-chatbot__feedback-text">Bạn chưa hài lòng với câu trả lời này?</p>
                  {user ? (
                    <Link to={APP_ROUTES.messagingTickets} className="help-chatbot__ticket-link">
                      Tạo yêu cầu hỗ trợ →
                    </Link>
                  ) : (
                    <Link
                      to={APP_ROUTES.login}
                      state={{ from: APP_ROUTES.messagingTickets }}
                      className="help-chatbot__ticket-link"
                    >
                      Đăng nhập để gửi yêu cầu →
                    </Link>
                  )}
                </div>
              )}

              {!result.matched && (
                user ? (
                  <Link to={APP_ROUTES.messagingTickets} className="help-chatbot__ticket-link">
                    Tạo yêu cầu hỗ trợ →
                  </Link>
                ) : (
                  <Link
                    to={APP_ROUTES.login}
                    state={{ from: APP_ROUTES.messagingTickets }}
                    className="help-chatbot__ticket-link"
                  >
                    Đăng nhập để gửi yêu cầu →
                  </Link>
                )
              )}

              <button type="button" className="help-chatbot__reset-btn" onClick={reset}>
                Đặt câu hỏi khác
              </button>
            </div>
          )}
        </aside>
      </div>
    </div>
    </div>
  );
}
