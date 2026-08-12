import { useCallback, useEffect, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { platformApi } from '../api/platformApi';
import { AdminLayout } from '../components/AdminLayout';
import type {
  CircumventionConversationApiResponse,
  CircumventionEventApiResponse,
  CircumventionStatus,
} from '../types/platformTypes';
import './PlatformCircumventionPage.css';

export default function PlatformCircumventionPage() {
  const [status, setStatus] = useState<CircumventionStatus>('PENDING');
  const [items, setItems] = useState<CircumventionEventApiResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [conversation, setConversation] = useState<CircumventionConversationApiResponse | null>(null);
  const [conversationError, setConversationError] = useState<string | null>(null);
  const [loadingConversationId, setLoadingConversationId] = useState<number | null>(null);

  const load = useCallback(async () => {
    try {
      setItems((await platformApi.getCircumventionEvents(status)).data.content);
      setError(null);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không tải được sự kiện phát hiện.'));
    }
  }, [status]);

  useEffect(() => {
    const id = window.setTimeout(() => void load(), 0);
    return () => window.clearTimeout(id);
  }, [load]);

  async function openConversation(item: CircumventionEventApiResponse) {
    setLoadingConversationId(item.eventId);
    setConversationError(null);
    try {
      setConversation((await platformApi.getCircumventionConversation(item.eventId)).data);
    } catch (requestError) {
      setConversationError(getApiErrorMessage(requestError, 'Không tải được nội dung hội thoại.'));
    } finally {
      setLoadingConversationId(null);
    }
  }

  async function review(item: CircumventionEventApiResponse, decision: 'CONFIRMED' | 'DISMISSED') {
    const note = window.prompt(decision === 'CONFIRMED' ? 'Ghi chú xác nhận' : 'Lý do bỏ qua', '') ?? '';
    setBusyId(item.eventId);
    try {
      await platformApi.reviewCircumventionEvent(item.eventId, decision, note);
      await load();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không cập nhật được sự kiện.'));
    } finally {
      setBusyId(null);
    }
  }

  return (
    <AdminLayout
      title="Phát hiện né nền tảng"
      subtitle="Duyệt dấu hiệu chia sẻ thông tin liên hệ trong chat trước khi áp dụng hình phạt."
    >
      <div className="adm-card">
        <div className="adm-toolbar">
          <select className="adm-field" value={status} onChange={(event) => setStatus(event.target.value as CircumventionStatus)}>
            <option value="PENDING">Chờ duyệt</option>
            <option value="CONFIRMED">Đã xác nhận</option>
            <option value="DISMISSED">Đã bỏ qua</option>
          </select>
          <button className="tcs-btn tcs-btn--ghost" type="button" onClick={() => void load()}>Làm mới</button>
        </div>
        {error && <div className="adm-alert adm-alert--error">{error}</div>}
        {conversationError && <div className="adm-alert adm-alert--error">{conversationError}</div>}
        <div className="adm-table-wrap">
          <table className="adm-table">
            <thead><tr><th>Người gửi</th><th>Quy tắc</th><th>Bằng chứng</th><th>Rủi ro</th><th>Thời gian</th><th>Thao tác</th></tr></thead>
            <tbody>
              {items.length === 0 && <tr><td colSpan={6}>Không có sự kiện.</td></tr>}
              {items.map((item) => (
                <tr key={item.eventId}>
                  <td>
                    {item.senderEmail}
                    <div>
                      <button
                        className="adm-circumvention-link"
                        type="button"
                        disabled={loadingConversationId === item.eventId}
                        onClick={() => void openConversation(item)}
                      >
                        {loadingConversationId === item.eventId ? 'Đang mở...' : 'Mở hội thoại'}
                      </button>
                    </div>
                  </td>
                  <td>{item.matchedRule}</td><td>{item.evidence}</td><td>{item.riskScore}/100</td>
                  <td>{new Date(item.createdAt).toLocaleString('vi-VN')}</td>
                  <td>
                    {item.status === 'PENDING' ? (
                      <div className="adm-row-actions">
                        <button className="tcs-btn tcs-btn--primary tcs-btn--badge" disabled={busyId === item.eventId} type="button" onClick={() => void review(item, 'CONFIRMED')}>Xác nhận</button>
                        <button className="tcs-btn tcs-btn--ghost tcs-btn--badge" disabled={busyId === item.eventId} type="button" onClick={() => void review(item, 'DISMISSED')}>Bỏ qua</button>
                      </div>
                    ) : item.reviewNote ?? '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {conversation && (
        <div className="adm-circumvention-overlay" role="presentation" onMouseDown={() => setConversation(null)}>
          <section className="adm-circumvention-dialog" role="dialog" aria-modal="true" aria-labelledby="circumvention-dialog-title" onMouseDown={(event) => event.stopPropagation()}>
            <header className="adm-circumvention-dialog__header">
              <div>
                <h2 id="circumvention-dialog-title">Hội thoại #{conversation.conversationId}</h2>
                <p>{conversation.conversationName || conversation.conversationType} · {conversation.participants.length} thành viên</p>
              </div>
              <button type="button" aria-label="Đóng" onClick={() => setConversation(null)}>×</button>
            </header>
            <div className="adm-circumvention-participants">
              {conversation.participants.map((participant) => <span key={participant.userId}>{participant.email}</span>)}
            </div>
            {conversation.hasMore && <p className="adm-circumvention-limit">Đang hiển thị 100 tin nhắn gần nhất.</p>}
            <div className="adm-circumvention-messages">
              {conversation.messages.map((message) => (
                <article key={message.messageId} className={`adm-circumvention-message${message.flagged ? ' adm-circumvention-message--flagged' : ''}`}>
                  <div><strong>{message.senderEmail}</strong><time>{new Date(message.sentAt).toLocaleString('vi-VN')}</time></div>
                  <p>{message.content}</p>
                  {message.flagged && <span>Tin nhắn bị phát hiện</span>}
                </article>
              ))}
            </div>
            <footer>Chế độ điều tra chỉ đọc. Hoạt động xem được ghi vào nhật ký hệ thống.</footer>
          </section>
        </div>
      )}
    </AdminLayout>
  );
}
