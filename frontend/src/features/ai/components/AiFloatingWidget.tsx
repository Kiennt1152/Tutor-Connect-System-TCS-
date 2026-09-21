import React, { useState, useRef, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { aiApi } from '../api/aiApi';
import type { AiMessage } from '../types/aiTypes';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { normalizeRole, hasRole } from '../../../shared/auth/rbac';
import './AiFloatingWidget.css';

function parseBold(str: string): (string | React.ReactNode)[] {
  const parts: (string | React.ReactNode)[] = [];
  const boldRegex = /\*\*([^*]+)\*\*/g;
  let lastIndex = 0;
  let match;

  while ((match = boldRegex.exec(str)) !== null) {
    if (match.index > lastIndex) {
      parts.push(str.substring(lastIndex, match.index));
    }
    parts.push(
      <strong key={`b-${match.index}`} style={{ fontWeight: 600 }}>
        {match[1]}
      </strong>
    );
    lastIndex = boldRegex.lastIndex;
  }

  if (lastIndex < str.length) {
    parts.push(str.substring(lastIndex));
  }

  return parts;
}

function renderFormattedContent(text: string, navigate: (to: string) => void) {
  if (!text) return null;
  const lines = text.split('\n');

  return lines.map((line, lIdx) => {
    const parts: (string | React.ReactNode)[] = [];
    const linkRegex = /\[([^\]]+)\]\(([^)]+)\)/g;
    let lastIndex = 0;
    let match;

    while ((match = linkRegex.exec(line)) !== null) {
      if (match.index > lastIndex) {
        parts.push(...parseBold(line.substring(lastIndex, match.index)));
      }
      const label = match[1];
      let url = match[2];
      if (url.startsWith('/support/tickets')) {
        url = url.replace('/support/tickets', '/messaging/tickets');
      }
      parts.push(
        <a
          key={`lnk-${lIdx}-${match.index}`}
          href={url}
          style={{
            color: '#ea580c',
            textDecoration: 'underline',
            fontWeight: 600,
            cursor: 'pointer',
          }}
          onClick={(e) => {
            e.preventDefault();
            navigate(url);
          }}
        >
          {label}
        </a>
      );
      lastIndex = linkRegex.lastIndex;
    }

    if (lastIndex < line.length) {
      parts.push(...parseBold(line.substring(lastIndex)));
    }

    return (
      <div key={lIdx} style={{ minHeight: line.trim() ? undefined : '0.4rem', lineHeight: '1.4' }}>
        {parts}
      </div>
    );
  });
}

export default function AiFloatingWidget() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const role = normalizeRole(user?.role);
  const isAdmin = hasRole(role, 'PLATFORM_ADMIN');

  const [isOpen, setIsOpen] = useState(false);
  const [widgetSessionId, setWidgetSessionId] = useState<number | undefined>(undefined);
  const [messages, setMessages] = useState<AiMessage[]>([
    {
      messageId: 0,
      sessionId: 0,
      role: 'assistant',
      content: '👋 Xin chào! Tôi là trợ lý AI TCS. Bạn cần tìm gia sư hay cần hướng dẫn quy trình sử dụng sàn?',
      createdAt: new Date().toISOString(),
    },
  ]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const bodyRef = useRef<HTMLDivElement>(null);
  const isHomepage = location.pathname === APP_ROUTES.home;

  useEffect(() => {
    if (isOpen) {
      bodyRef.current?.scrollTo({ top: bodyRef.current.scrollHeight, behavior: 'smooth' });
    }
  }, [messages, isOpen]);

  // Hide widget for Platform Admin, on platform routes, or when already on full AI Assistant page
  if (isAdmin || location.pathname.startsWith('/platform') || location.pathname === APP_ROUTES.aiAssistant) {
    return null;
  }

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    const text = input.trim();
    if (!text || sending) return;

    setInput('');
    setSending(true);

    const tempUserMsg: AiMessage = {
      messageId: Date.now(),
      sessionId: widgetSessionId || 0,
      role: 'user',
      content: text,
      createdAt: new Date().toISOString(),
    };
    setMessages(prev => [...prev, tempUserMsg]);

    try {
      const resp = await aiApi.chat({ message: text, sessionId: widgetSessionId });
      setMessages(prev => [...prev, resp]);
      if (!widgetSessionId && resp.sessionId) {
        setWidgetSessionId(resp.sessionId);
      }
    } catch {
      setMessages(prev => [
        ...prev,
        {
          messageId: Date.now() + 1,
          sessionId: widgetSessionId || 0,
          role: 'assistant',
          content: '⚠️ Hệ thống AI đang bận. Vui lòng thử lại sau.',
          createdAt: new Date().toISOString(),
        },
      ]);
    } finally {
      setSending(false);
    }
  };

  const goToFullPage = () => {
    if (widgetSessionId) {
      sessionStorage.setItem('ai_current_session', String(widgetSessionId));
    }
    setIsOpen(false);
    navigate(APP_ROUTES.aiAssistant);
  };

  return (
    <>
      {isHomepage && (
        <button
          className="ai-widget-button ai-widget-button--support"
          onClick={() => {
            navigate(APP_ROUTES.help);
            window.scrollTo({ top: 0, left: 0, behavior: 'instant' });
          }}
          title="Hỗ trợ khách hàng"
        >
          <span className="ai-widget-button__icon" aria-hidden="true">☎</span>
          <span>Hỗ trợ khách hàng</span>
        </button>
      )}
      <button className="ai-widget-button" onClick={() => setIsOpen(!isOpen)} title="Trợ lý AI TCS">
        <img className="app-logo__image" alt="" src="/logo.png" style={{ width: '24px', height: '24px', borderRadius: '50%' }} />
        <span>TCS AI</span>
      </button>

      {isOpen && (
        <div className={`ai-widget-popup${isHomepage ? ' ai-widget-popup--with-support' : ''}`}>
          <header className="ai-popup-header">
            <h3>Trợ lý AI TCS</h3>
            <div className="ai-popup-actions">
              <button className="ai-popup-expand" onClick={goToFullPage}>
                Toàn màn hình
              </button>
              <button className="ai-popup-close" onClick={() => setIsOpen(false)}>
                ✕
              </button>
            </div>
          </header>

          <div className="ai-popup-body" ref={bodyRef}>
            {messages.map((m, idx) => (
              <div key={idx} style={{ alignSelf: m.role === 'user' ? 'flex-end' : 'flex-start', maxWidth: '85%' }}>
                <div
                  style={{
                    padding: '0.6rem 0.85rem',
                    borderRadius: '1rem',
                    background: m.role === 'user' ? 'linear-gradient(135deg, #ea580c, #c4612f)' : '#f9fafb',
                    color: m.role === 'user' ? '#fff' : '#1F2421',
                    border: m.role === 'assistant' ? '1px solid #E7E1D7' : 'none',
                    fontSize: '0.85rem',
                    lineHeight: '1.4',
                    borderBottomRightRadius: m.role === 'user' ? '0.2rem' : '1rem',
                    borderBottomLeftRadius: m.role === 'assistant' ? '0.2rem' : '1rem',
                  }}
                >
                  {renderFormattedContent(m.content, navigate)}
                  {m.role === 'assistant' && (m.suggestedRoute || m.content.includes('/messaging/tickets')) && (
                    <div style={{ marginTop: '0.5rem' }}>
                      <button
                        type="button"
                        style={{
                          background: '#0f172a',
                          color: '#fff',
                          border: 'none',
                          borderRadius: '4px',
                          padding: '0.35rem 0.65rem',
                          fontSize: '0.8rem',
                          cursor: 'pointer',
                          fontWeight: 600,
                          display: 'inline-block',
                        }}
                        onClick={() => {
                          let route = m.suggestedRoute || '/messaging/tickets?action=create&subject=Sự+cố+nạp+tiền+chưa+cộng+số+dư';
                          if (route.startsWith('/support/tickets')) {
                            route = route.replace('/support/tickets', '/messaging/tickets');
                          }
                          navigate(route);
                        }}
                      >
                        {(m.suggestedRoute?.includes('ticket') || m.content.includes('Ticket') || m.content.includes('ticket'))
                          ? '🎫 Gửi Ticket hỗ trợ →'
                          : m.suggestedRoute?.includes('find-tutor')
                          ? '🔍 Tìm gia sư →'
                          : m.suggestedRoute?.includes('finance')
                          ? '💳 Ví tiền →'
                          : 'Xem chi tiết →'}
                      </button>
                    </div>
                  )}
                </div>
              </div>
            ))}
            {sending && (
              <div style={{ alignSelf: 'flex-start', fontSize: '0.8rem', color: '#8b949e', fontStyle: 'italic' }}>
                🤖 AI đang tìm kiếm câu trả lời...
              </div>
            )}
          </div>

          <footer className="ai-popup-footer">
            <form onSubmit={handleSend} style={{ display: 'flex', gap: '0.5rem' }}>
              <input
                type="text"
                placeholder="Hỏi nhanh..."
                value={input}
                onChange={e => setInput(e.target.value)}
                disabled={sending}
                style={{
                  flex: 1,
                  padding: '0.45rem 0.75rem',
                  borderRadius: '9999px',
                  border: '1px solid #E7E1D7',
                  background: '#fff',
                  color: '#1F2421',
                  fontSize: '0.85rem',
                  outline: 'none',
                }}
              />
              <button
                type="submit"
                disabled={sending || !input.trim()}
                style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: '50%',
                  background: '#ea580c',
                  color: '#fff',
                  border: 'none',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                }}
              >
                ➤
              </button>
            </form>
          </footer>
        </div>
      )}
    </>
  );
}
