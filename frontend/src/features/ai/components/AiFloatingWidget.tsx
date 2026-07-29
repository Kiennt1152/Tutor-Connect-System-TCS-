import React, { useState, useRef, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { aiApi } from '../api/aiApi';
import type { AiMessage } from '../types/aiTypes';
import { APP_ROUTES } from '../../../shared/constants/routes';
import './AiFloatingWidget.css';

export default function AiFloatingWidget() {
  const navigate = useNavigate();
  const location = useLocation();
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

  useEffect(() => {
    if (isOpen) {
      bodyRef.current?.scrollTo({ top: bodyRef.current.scrollHeight, behavior: 'smooth' });
    }
  }, [messages, isOpen]);

  // Hide widget when already on full AI Assistant page
  if (location.pathname === APP_ROUTES.aiAssistant) {
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
    } catch (err) {
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
      <button className="ai-widget-button" onClick={() => setIsOpen(!isOpen)} title="Trợ lý AI TCS">
        <span style={{ fontSize: '1.2rem' }}>🤖</span>
        <span>AI Hỗ trợ 24/7</span>
      </button>

      {isOpen && (
        <div className="ai-widget-popup">
          <header className="ai-popup-header">
            <h3><span>🧠</span> Trợ lý AI TCS (RAG)</h3>
            <div className="ai-popup-actions">
              <button className="ai-popup-expand" onClick={goToFullPage}>
                🚀 Toàn màn hình
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
                    background: m.role === 'user' ? 'linear-gradient(135deg, #2563eb, #4f46e5)' : '#21262d',
                    color: '#fff',
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
                🤖 AI đang tra cứu RAG...
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
                  border: '1px solid #30363d',
                  background: '#0d1117',
                  color: '#fff',
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
                  background: '#a855f7',
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
