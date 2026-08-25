import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { aiApi } from '../api/aiApi';
import type { AiMessage, AiSession } from '../types/aiTypes';
import { APP_ROUTES, tutorProfilePath } from '../../../shared/constants/routes';
import './AiAssistantPage.css';

export default function AiAssistantPage() {
  const navigate = useNavigate();
  const [sessions, setSessions] = useState<AiSession[]>([]);
  const [currentSessionId, setCurrentSessionId] = useState<number | undefined>(() => {
    const stored = sessionStorage.getItem('ai_current_session');
    return stored ? Number(stored) : undefined;
  });
  const [messages, setMessages] = useState<AiMessage[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);
  
  // Local UI States
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [historySearch, setHistorySearch] = useState('');
  const [expandedQueryId, setExpandedQueryId] = useState<number | null>(null);
  const [expandedSourcesId, setExpandedSourcesId] = useState<number | null>(null);
  const [copiedMessageId, setCopiedMessageId] = useState<number | null>(null);
  
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    loadSessions();
    const handleResize = () => {
      if (window.innerWidth <= 768) {
        setSidebarCollapsed(true);
      } else {
        setSidebarCollapsed(false);
      }
    };
    window.addEventListener('resize', handleResize);
    handleResize();
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  useEffect(() => {
    if (currentSessionId) {
      sessionStorage.setItem('ai_current_session', String(currentSessionId));
      // Auto-load messages if returning to the page with an active session
      if (messages.length === 0 && !loading && !sending) {
        handleSelectSession(currentSessionId);
      }
    } else {
      sessionStorage.removeItem('ai_current_session');
    }
  }, [currentSessionId]);

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
    if (window.innerWidth <= 768) setSidebarCollapsed(true);
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
    if (window.innerWidth <= 768) setSidebarCollapsed(true);
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
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
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

  const handleEditQuestion = (index: number) => {
    let userMsg = null;
    for (let i = index - 1; i >= 0; i--) {
      if (messages[i].role === 'user') {
        userMsg = messages[i];
        break;
      }
    }
    if (userMsg) {
      setInput(userMsg.content);
      if (textareaRef.current) {
        textareaRef.current.focus();
        textareaRef.current.style.height = 'auto';
        textareaRef.current.style.height = Math.min(textareaRef.current.scrollHeight, 150) + 'px';
      }
    }
  };

  const handleTextareaInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(e.target.value);
    e.target.style.height = 'auto';
    e.target.style.height = Math.min(e.target.scrollHeight, 150) + 'px';
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleCopy = (text: string, id: number) => {
    navigator.clipboard.writeText(text);
    setCopiedMessageId(id);
    setTimeout(() => setCopiedMessageId(null), 2000);
  };

  const filteredSessions = sessions.filter(s => s.title.toLowerCase().includes(historySearch.toLowerCase()));

  const promptGroups = [
    {
      title: 'Tìm gia sư',
      prompts: ['Tìm cho tôi gia sư môn Toán lớp 12 khu vực Cầu Giấy dưới 250k', 'Tôi cần tìm lớp học tiếng Anh giao tiếp cho người đi làm']
    },
    {
      title: 'Hỗ trợ thanh toán / Ticket',
      prompts: ['Tôi đã thanh toán nhưng lớp chưa cập nhật thì sao?', 'Hướng dẫn tạo ticket khiếu nại gia sư', 'Quy định thanh toán Escrow']
    },
    {
      title: 'Quản trị hệ thống',
      prompts: ['Làm sao để xem báo cáo doanh thu trên dashboard?', 'Quy trình duyệt hồ sơ gia sư như thế nào?']
    }
  ];

  return (
    <div className="ai-assistant-page">
      {/* Sidebar */}
      <aside className={`ai-sidebar ${sidebarCollapsed ? 'ai-sidebar--collapsed' : ''}`}>
        <div className="ai-sidebar-header">
          <button className="ai-toggle-sidebar-btn" onClick={() => setSidebarCollapsed(!sidebarCollapsed)} title="Thu gọn sidebar">
            ☰
          </button>
          <button className="ai-new-chat-btn" onClick={handleNewChat}>
            <span style={{ fontSize: '1.2rem', lineHeight: 1 }}>+</span> Cuộc trò chuyện mới
          </button>
        </div>
        
        <div style={{ padding: '0 1rem 0.5rem' }}>
          <div style={{ position: 'relative' }}>
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: '#5f6368' }}><circle cx="11" cy="11" r="8"></circle><line x1="21" y1="21" x2="16.65" y2="16.65"></line></svg>
            <input 
              type="text" 
              placeholder="Tìm kiếm cuộc trò chuyện..." 
              value={historySearch}
              onChange={e => setHistorySearch(e.target.value)}
              style={{ width: '100%', padding: '0.5rem 0.5rem 0.5rem 2rem', borderRadius: '8px', border: '1px solid #e3e3e3', fontSize: '0.85rem' }}
            />
          </div>
        </div>

        <div className="ai-sessions-list">
          <div className="ai-sessions-label">Gần đây</div>
          {filteredSessions.map(s => (
            <div
              key={s.sessionId}
              className={`ai-session-item ${currentSessionId === s.sessionId ? 'active' : ''}`}
              onClick={() => handleSelectSession(s.sessionId)}
            >
              <span className="ai-session-title">{s.title}</span>
              <button
                className="ai-session-delete"
                onClick={e => handleDeleteSession(e, s.sessionId)}
                title="Xóa cuộc trò chuyện"
              >
                ✕
              </button>
            </div>
          ))}
          {filteredSessions.length === 0 && (
            <div style={{ padding: '1rem', textAlign: 'center', color: '#80868b', fontSize: '0.85rem' }}>
              Không tìm thấy cuộc trò chuyện nào
            </div>
          )}
        </div>
      </aside>

      {/* Main Chat Area */}
      <main className="ai-chat-main">
        <header className="ai-chat-header">
          <div className="ai-header-title">
            {sidebarCollapsed && (
              <button className="ai-toggle-sidebar-btn" onClick={() => setSidebarCollapsed(false)} style={{ marginRight: '0.5rem' }}>
                ☰
              </button>
            )}
            <div onClick={() => navigate(APP_ROUTES.home)} style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '0.5rem', textDecoration: 'none', color: 'inherit' }} title="Về trang chủ">
              <img src="/logo.png" alt="TCS Logo" style={{ width: '2rem', height: '2rem', borderRadius: '4px' }} />
              <h2 style={{ margin: 0, fontSize: '1.25rem' }}>TCS AI Assistant</h2>
            </div>
          </div>
        </header>

        <div className="ai-messages-container">
          {loading ? (
            <div style={{ textAlign: 'center', margin: 'auto', color: '#8b949e' }}>Đang tải lịch sử hội thoại...</div>
          ) : messages.length === 0 ? (
            <div className="ai-welcome-state">
              <div className="ai-welcome-header">
                <h1>Xin chào!</h1>
                <p>Hôm nay bạn muốn TCS AI hỗ trợ gì?</p>
              </div>
              <div className="ai-prompt-groups">
                {promptGroups.map((group, idx) => (
                  <div key={idx} className="ai-prompt-group">
                    <h4>{group.title}</h4>
                    <div className="ai-prompt-starters">
                      {group.prompts.map((starter, sIdx) => (
                        <button key={sIdx} className="ai-starter-chip" onClick={() => handleSend(starter)}>
                          {starter}
                        </button>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            messages.map((m, idx) => {
              const genericSources = m.sources?.filter(s => !['TUTOR', 'CLASS', 'FAQ'].includes(s.sourceType)) || [];
              
              return (
                <div key={m.messageId || idx} className={`ai-message-row ${m.role}`}>
                  <div className="ai-avatar">
                    {m.role === 'user' ? (
                      '👤'
                    ) : (
                      <img className="ai-avatar-img" src="/logo.png" alt="AI" />
                    )}
                  </div>
                  <div className="ai-bubble-container">
                    {/* Username if needed (omitted for clean look) */}
                    <div className="ai-bubble">
                      <div style={{ whiteSpace: 'pre-wrap' }}>{m.content}</div>

                      {/* Render Generic Sources (Accordion) */}
                      {genericSources.length > 0 && (
                        <div className={`ai-accordion ${expandedSourcesId === m.messageId ? 'open' : ''}`}>
                          <div className="ai-accordion-header" onClick={() => setExpandedSourcesId(expandedSourcesId === m.messageId ? null : (m.messageId || 0))}>
                            <span>Nguồn Dữ liệu Ngữ cảnh ({genericSources.length})</span>
                            <span>{expandedSourcesId === m.messageId ? '▲' : '▼'}</span>
                          </div>
                          {expandedSourcesId === m.messageId && (
                            <div className="ai-accordion-body">
                              <div className="ai-rich-cards">
                                {genericSources.map((s, sIdx) => (
                                  <div key={sIdx} className="ai-mini-card">
                                    <div className="ai-mini-card-header">
                                      <h4 className="ai-mini-card-title">{s.title}</h4>
                                      <span className="ai-mini-card-badge">{s.sourceType} • {(s.finalScore || 0).toFixed(2)}</span>
                                    </div>
                                    <p className="ai-mini-card-snippet">{s.snippet.length > 200 ? s.snippet.substring(0, 200) + '...' : s.snippet}</p>
                                  </div>
                                ))}
                              </div>
                            </div>
                          )}
                        </div>
                      )}

                      {/* Render Tutor Cards (Top 3) */}
                      {m.referencedTutors && m.referencedTutors.length > 0 && m.intent === 'FIND_TUTOR' && (
                        <div className="ai-rich-cards">
                          <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#475569' }}>Gợi ý Gia sư phù hợp:</div>
                          {m.referencedTutors.slice(0, 3).map((t, tIdx) => (
                            <div key={tIdx} className="ai-mini-card" style={{ borderLeft: '3px solid #0f172a' }}>
                              <div className="ai-mini-card-header">
                                <h4 className="ai-mini-card-title">{t.fullName}</h4>
                                <span className="ai-mini-card-badge">{t.title || 'Gia sư'}</span>
                              </div>
                              <p className="ai-mini-card-snippet">{t.teachingAreas} • {t.hourlyRate ? t.hourlyRate.toLocaleString('vi-VN') : 'Thỏa thuận'} ₫/buổi</p>
                              <button
                                type="button"
                                className="ai-mini-card-btn"
                                onClick={() =>
                                  t.tutorId
                                    ? navigate(tutorProfilePath(), { state: { tutorId: t.tutorId } })
                                    : navigate(APP_ROUTES.findTutor)
                                }
                              >
                                Xem hồ sơ gia sư →
                              </button>
                            </div>
                          ))}
                        </div>
                      )}

                      {/* Render Class Cards (Top 3) */}
                      {m.referencedClasses && m.referencedClasses.length > 0 && (m.intent === 'FIND_CLASS' || m.intent === 'CREATE_CLASS') && (
                        <div className="ai-rich-cards">
                          <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#475569' }}>Lớp học đang mở:</div>
                          {m.referencedClasses.slice(0, 3).map((c, cIdx) => (
                            <div key={cIdx} className="ai-mini-card" style={{ borderLeft: '3px solid #334155' }}>
                              <div className="ai-mini-card-header">
                                <h4 className="ai-mini-card-title">{c.title}</h4>
                                <span className="ai-mini-card-badge">{c.subjectName} • {c.gradeLevelName}</span>
                              </div>
                              <p className="ai-mini-card-snippet">{c.tuitionFee ? c.tuitionFee.toLocaleString('vi-VN') : 'Thỏa thuận'} ₫/buổi</p>
                              <button
                                type="button"
                                className="ai-mini-card-btn"
                                style={{ background: '#0f172a' }}
                                onClick={() => navigate(APP_ROUTES.findClass)}
                              >
                                Xem lớp học →
                              </button>
                            </div>
                          ))}
                        </div>
                      )}

                      {/* Render FAQ Cards (Top 3) */}
                      {m.referencedFaqs && m.referencedFaqs.length > 0 && m.intent !== 'ADMIN_DASHBOARD' && m.intent !== 'PLATFORM_STATS' && (
                        <div className="ai-rich-cards">
                          <div style={{ fontSize: '0.85rem', fontWeight: 600, color: '#475569' }}>Bài viết hướng dẫn liên quan:</div>
                          {m.referencedFaqs.slice(0, 3).map((f, fIdx) => (
                            <div key={fIdx} className="ai-mini-card" style={{ borderLeft: '3px solid #64748b' }}>
                              <h4 className="ai-mini-card-title">{f.question}</h4>
                              <p className="ai-mini-card-snippet">{f.answer}</p>
                              <button
                                type="button"
                                className="ai-mini-card-btn"
                                style={{ background: '#334155', marginTop: '0.5rem' }}
                                onClick={() => navigate(APP_ROUTES.help)}
                              >
                                Trung tâm trợ giúp →
                              </button>
                            </div>
                          ))}
                        </div>
                      )}

                      {/* Metadata & Confidence */}
                      {m.role === 'assistant' && (
                        <>
                          <div className="ai-metadata-bar">
                            {m.confidenceLevel === 'LOW' && (
                              <span className="ai-badge ai-badge--low" title={m.evaluationNotes}>
                                ⚠️ Độ tin cậy thấp — vui lòng kiểm tra lại thông tin
                              </span>
                            )}
                            {m.sourceCount != null && m.sourceCount > 0 && (
                              <span className="ai-badge ai-badge--info">
                                📚 Dựa trên {m.sourceCount} nguồn dữ liệu
                              </span>
                            )}
                          </div>
                          
                          {/* Rewritten Query Accordion */}
                          {m.rewrittenQuery && (
                            <div className={`ai-accordion ${expandedQueryId === m.messageId ? 'open' : ''}`} style={{ marginTop: '0.5rem', background: 'transparent' }}>
                              <div 
                                className="ai-accordion-header" 
                                style={{ padding: '0.4rem 0.5rem', background: 'transparent', fontSize: '0.8rem', color: '#1a73e8' }}
                                onClick={() => setExpandedQueryId(expandedQueryId === m.messageId ? null : (m.messageId || 0))}
                              >
                                <span>🔍 AI hiểu câu hỏi là...</span>
                                <span>{expandedQueryId === m.messageId ? '▲' : '▼'}</span>
                              </div>
                              {expandedQueryId === m.messageId && (
                                <div className="ai-accordion-body" style={{ padding: '0.5rem', fontSize: '0.85rem', color: '#5f6368', background: '#f8f9fa', borderRadius: '4px' }}>
                                  "{m.rewrittenQuery}"
                                </div>
                              )}
                            </div>
                          )}
                          

                        </>
                      )}
                    </div>
                    
                    {/* Action Bar */}
                    {m.role === 'assistant' && (
                      <div className="ai-message-actions">
                        <button className="ai-action-btn" title="Copy" onClick={() => handleCopy(m.content, m.messageId || 0)} style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                          {copiedMessageId === m.messageId ? (
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12"></polyline></svg>
                          ) : (
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path></svg>
                          )}
                        </button>
                        <button className="ai-action-btn" title="Chỉnh sửa câu hỏi" onClick={() => handleEditQuestion(idx)} style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              );
            })
          )}
          {sending && (
            <div className="ai-message-row assistant">
              <div className="ai-avatar">
                <img className="ai-avatar-img" src="/logo.png" alt="AI" style={{ opacity: 0.5 }} />
              </div>
              <div className="ai-bubble-container">
                <div className="ai-bubble" style={{ color: '#5f6368' }}>
                  AI đang phân tích câu hỏi...
                </div>
              </div>
            </div>
          )}
          <div ref={messagesEndRef} style={{ height: 1 }} />
        </div>

        {/* Input Footer (Composer) */}
        <footer className="ai-input-area">
          <div style={{ width: '100%', maxWidth: '850px', position: 'relative' }}>
            <form
              className="ai-input-wrapper"
              onSubmit={e => {
                e.preventDefault();
                handleSend();
              }}
            >
              <textarea
                ref={textareaRef}
                className="ai-input-field"
                placeholder="Hỏi bất kỳ điều gì về hệ thống, tìm gia sư, hay quy định..."
                value={input}
                onChange={handleTextareaInput}
                onKeyDown={handleKeyDown}
                disabled={sending}
                rows={1}
              />
              <button type="submit" className="ai-send-btn" disabled={sending || !input.trim()}>
                ➤
              </button>
            </form>
            <div className="ai-disclaimer">
              AI có thể đưa ra thông tin chưa chính xác. Vui lòng kiểm tra lại các thông tin quan trọng.
            </div>
          </div>
        </footer>
      </main>
    </div>
  );
}
