import React, { useState, useRef, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { aiApi } from '../api/aiApi';
import type { AiMessage } from '../types/aiTypes';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { normalizeRole, hasRole } from '../../../shared/auth/rbac';
import './AiFloatingWidget.css';

export default function AiFloatingWidget() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();
  const role = normalizeRole(user?.role);
  const isAdmin = hasRole(role, 'PLATFORM_ADMIN');

  const [isOpen, setIsOpen] = useState(false);
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
      sessionId: 0,
      role: 'user',
      content: text,
      createdAt: new Date().toISOString(),
    };
    setMessages(prev => [...prev, tempUserMsg]);

    try {
      const resp = await aiApi.chat({ message: text });
      setMessages(prev => [...prev, resp]);
    } catch {
      setMessages(prev => [
        ...prev,
        {
          messageId: Date.now() + 1,
          sessionId: 0,
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
    setIsOpen(false);
    navigate(APP_ROUTES.aiAssistant);
  };

  return (
    <>
      {isHomepage && (
        <button
          className="ai-widget-button ai-widget-button--support"
          onClick={() => navigate(APP_ROUTES.help)}
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
                  {m.content}
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
