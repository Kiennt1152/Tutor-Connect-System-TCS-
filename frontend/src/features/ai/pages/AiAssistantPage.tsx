import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { aiApi } from '../api/aiApi';
import type { AiMessage, AiSession } from '../types/aiTypes';
import { APP_ROUTES } from '../../../shared/constants/routes';
import './AiAssistantPage.css';

export default function AiAssistantPage() {
  const navigate = useNavigate();
  const [sessions, setSessions] = useState<AiSession[]>([]);
  const [currentSessionId, setCurrentSessionId] = useState<number | undefined>(undefined);
  const [messages, setMessages] = useState<AiMessage[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    loadSessions();
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, sending]);

  const loadSessions = async () => {
    try {
      const list = await aiApi.getSessions();
      setSessions(list);
    } catch (err) {
      console.error('Failed to load chat sessions:', err);
    }
  };

  const handleSelectSession = async (sessionId: number) => {
    setCurrentSessionId(sessionId);
    setLoading(true);
    try {
      const list = await aiApi.getSessionMessages(sessionId);
      setMessages(list);
    } catch (err) {
      console.error('Failed to load session messages:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleNewChat = () => {
    setCurrentSessionId(undefined);
    setMessages([]);
    setInput('');
  };

  const handleDeleteSession = async (e: React.MouseEvent, sessionId: number) => {
    e.stopPropagation();
    if (!window.confirm('Bạn có chắc muốn xóa cuộc trò chuyện này?')) return;
    try {
      await aiApi.deleteSession(sessionId);
      setSessions(prev => prev.filter(s => s.sessionId !== sessionId));
      if (currentSessionId === sessionId) {
        handleNewChat();
      }
    } catch (err) {
      alert('Không thể xóa cuộc trò chuyện');
    }
  };

  const handleSend = async (textToSend?: string) => {
    const text = (textToSend || input).trim();
    if (!text || sending) return;

    if (!textToSend) setInput('');
    setSending(true);

    const tempUserMsg: AiMessage = {
      messageId: Date.now(),
      sessionId: currentSessionId || 0,
      role: 'user',
      content: text,
      createdAt: new Date().toISOString(),
    };
    setMessages(prev => [...prev, tempUserMsg]);

    try {
      const resp = await aiApi.chat({
        message: text,
        sessionId: currentSessionId,
      });
      setMessages(prev => [...prev, resp]);
      if (!currentSessionId && resp.sessionId) {
        setCurrentSessionId(resp.sessionId);
        loadSessions();
      }
    } catch (err) {
      const errorMsg: AiMessage = {
        messageId: Date.now() + 1,
        sessionId: currentSessionId || 0,
        role: 'assistant',
        content: '⚠️ Xin lỗi, hệ thống AI đang bận hoặc gặp gián đoạn kết nối. Vui lòng thử lại sau giây lát.',
        createdAt: new Date().toISOString(),
      };
      setMessages(prev => [...prev, errorMsg]);
    } finally {
      setSending(false);
    }
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const promptStarters = [
    '🔍 Tìm cho tôi gia sư môn Toán lớp 12 khu vực Cầu Giấy',
    '💡 Hướng dẫn quy trình đăng ký và xác minh gia sư',
    '🛡️ Bảo lãnh thanh toán Escrow bảo vệ học phí thế nào?',
    '📚 Lớp học tiếng Anh giao tiếp đang mở tuyển gia sư',
  ];

  return (
    <div className="ai-assistant-page">
      {/* Sidebar */}
      <aside className="ai-sidebar">
        <div className="ai-sidebar-header">
          <button className="ai-new-chat-btn" onClick={handleNewChat}>
            <span>✨</span> Cuộc trò chuyện mới
          </button>
        </div>
        <div className="ai-sessions-list">
          {sessions.map(s => (
            <div
              key={s.sessionId}
              className={`ai-session-item ${currentSessionId === s.sessionId ? 'active' : ''}`}
              onClick={() => handleSelectSession(s.sessionId)}
            >
              <span className="ai-session-title">💬 {s.title}</span>
              <button
                className="ai-session-delete"
                onClick={e => handleDeleteSession(e, s.sessionId)}
                title="Xóa cuộc trò chuyện"
              >
                🗑️
              </button>
            </div>
          ))}
        </div>
      </aside>

      {/* Main Chat Area */}
      <main className="ai-chat-main">
        <header className="ai-chat-header">
          <div className="ai-header-title">
            <span style={{ fontSize: '1.5rem' }}>🤖</span>
            <h2>Trợ lý AI TCS — RAG & Semantic Search</h2>
          </div>
          <div className="ai-status-badge">
            <span className="ai-status-dot"></span>
            <span>Trực tuyến 24/7 (Groq Llama-3 / Gemini)</span>
          </div>
        </header>

        <div className="ai-messages-container">
          {loading ? (
            <div style={{ textAlign: 'center', margin: 'auto', color: '#8b949e' }}>⏳ Đang tải lịch sử hội thoại...</div>
          ) : messages.length === 0 ? (
            <div className="ai-welcome-state">
              <div className="ai-welcome-icon">🧠</div>
              <h3>Chào mừng đến với Trung tâm Tư vấn AI TCS</h3>
              <p>
                Trợ lý AI được tích hợp công nghệ <strong>RAG (Retrieval-Augmented Generation)</strong> tra cứu trực tiếp cơ sở tri thức thời gian thực của hệ thống Tutor Connect System để giải đáp và gợi ý chính xác nhất.
              </p>
              <div className="ai-prompt-starters">
                {promptStarters.map((starter, idx) => (
                  <button key={idx} className="ai-starter-chip" onClick={() => handleSend(starter)}>
                    {starter}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            messages.map((m, idx) => (
              <div key={m.messageId || idx} className={`ai-message-row ${m.role}`}>
                <div className="ai-avatar">{m.role === 'user' ? '👤' : '🤖'}</div>
                <div style={{ flex: 1 }}>
                  <div className="ai-bubble">
                    <div style={{ whiteSpace: 'pre-wrap' }}>{m.content}</div>

                    {/* Render Tutor Cards */}
                    {m.referencedTutors && m.referencedTutors.length > 0 && (
                      <div className="ai-rich-cards">
                        <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#a855f7', marginTop: '0.5rem' }}>
                          👨‍🏫 Gợi ý Gia sư từ hệ thống RAG:
                        </div>
                        {m.referencedTutors.map(t => (
                          <div key={t.tutorId} className="ai-mini-card">
                            <div className="ai-mini-card-info">
                              <h4>{t.fullName} ({t.title || 'Gia sư'})</h4>
                              <p>💰 Học phí: {t.hourlyRate ? t.hourlyRate.toLocaleString('vi-VN') : '200.000'} VND/buổi • 📍 {t.teachingAreas}</p>
                            </div>
                            <button className="ai-mini-card-btn" onClick={() => navigate(APP_ROUTES.marketplace)}>
                              Xem hồ sơ →
                            </button>
                          </div>
                        ))}
                      </div>
                    )}

                    {/* Render Class Cards */}
                    {m.referencedClasses && m.referencedClasses.length > 0 && (
                      <div className="ai-rich-cards">
                        <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#60a5fa', marginTop: '0.5rem' }}>
                          📚 Lớp học đang tuyển gia sư:
                        </div>
                        {m.referencedClasses.map(c => (
                          <div key={c.classId} className="ai-mini-card">
                            <div className="ai-mini-card-info">
                              <h4>{c.title}</h4>
                              <p>Môn: {c.subjectName} ({c.gradeLevelName}) • Học phí: {c.tuitionFee ? c.tuitionFee.toLocaleString('vi-VN') : '250.000'} VND/buổi</p>
                            </div>
                            <button className="ai-mini-card-btn" onClick={() => navigate(APP_ROUTES.marketplace)}>
                              Đăng ký ngay →
                            </button>
                          </div>
                        ))}
                      </div>
                    )}

                    {/* Render FAQ Cards */}
                    {m.referencedFaqs && m.referencedFaqs.length > 0 && (
                      <div className="ai-rich-cards">
                        <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#3fb950', marginTop: '0.5rem' }}>
                          💡 Bài hướng dẫn liên quan:
                        </div>
                        {m.referencedFaqs.map(f => (
                          <div key={f.faqId} className="ai-mini-card" style={{ borderLeft: '3px solid #3fb950' }}>
                            <div className="ai-mini-card-info">
                              <h4>❓ {f.question}</h4>
                              <p>{f.answer}</p>
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))
          )}
          {sending && (
            <div className="ai-message-row assistant">
              <div className="ai-avatar">🤖</div>
              <div className="ai-bubble" style={{ fontStyle: 'italic', color: '#8b949e' }}>
                🧠 AI đang truy xuất tri thức RAG và phân tích câu trả lời...
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Input Footer */}
        <footer className="ai-input-area">
          <form
            className="ai-input-wrapper"
            onSubmit={e => {
              e.preventDefault();
              handleSend();
            }}
          >
            <input
              type="text"
              className="ai-input-field"
              placeholder="Hỏi bất kỳ điều gì về tìm gia sư, mở lớp học, quy định thanh toán Escrow..."
              value={input}
              onChange={e => setInput(e.target.value)}
              disabled={sending}
            />
            <button type="submit" className="ai-send-btn" disabled={sending || !input.trim()}>
              <span>➤</span>
            </button>
          </form>
        </footer>
      </main>
    </div>
  );
}
