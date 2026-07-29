import { useState, type FormEvent } from 'react';
import type { SupportTicketCategory, SupportTicketPriority } from '../types/messagingTypes';
import { useCreateTicket, useTicketDetail, useTicketList } from '../hooks/useMessaging';
import './MessagingPanel.css';

/* ── Icons ── */
function IconBack() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="ticket-detail__back-icon" aria-hidden="true">
      <polyline points="15 18 9 12 15 6" />
    </svg>
  );
}

/* ── Ticket list ── */
type TicketListProps = {
  onSelect: (id: string) => void;
  onNew: () => void;
};

function TicketList({ onSelect, onNew }: TicketListProps) {
  const { status, items, errorMessage, reload } = useTicketList();

  return (
    <div>
      <div className="ticket-list__toolbar">
        <h2 className="ticket-list__title">Yêu cầu hỗ trợ của tôi</h2>
        <button type="button" className="tcs-btn tcs-btn--primary" onClick={onNew}>
          + Tạo yêu cầu mới
        </button>
      </div>

      {status === 'loading' && <p style={{ color: '#718096', fontSize: '0.9rem' }}>Đang tải…</p>}

      {status === 'error' && (
        <div className="ticket-list__error">
          {errorMessage ?? 'Không tải được danh sách.'}{' '}
          <button type="button" onClick={reload} style={{ background: 'none', border: 'none', cursor: 'pointer', textDecoration: 'underline', color: 'inherit' }}>
            Thử lại
          </button>
        </div>
      )}

      {status === 'success' && items.length === 0 && (
        <div className="ticket-list__empty">
          <p style={{ marginBottom: '0.75rem' }}>Bạn chưa có yêu cầu hỗ trợ nào.</p>
          <button type="button" className="tcs-btn tcs-btn--primary" onClick={onNew}>
            Tạo yêu cầu đầu tiên
          </button>
        </div>
      )}

      {status === 'success' && items.map((ticket) => (
        <article
          key={ticket.id}
          className="ticket-card"
          onClick={() => onSelect(ticket.id)}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => e.key === 'Enter' && onSelect(ticket.id)}
          aria-label={`Xem chi tiết yêu cầu: ${ticket.subject}`}
        >
          <div className="ticket-card__main">
            <p className="ticket-card__subject">{ticket.subject}</p>
            <div className="ticket-card__meta">
              <span>{ticket.categoryLabel}</span>
              <span>·</span>
              <span>Tạo lúc {ticket.createdAt}</span>
            </div>
          </div>
          <div className="ticket-card__badges">
            <span className={`tcs-badge tcs-badge--status-${ticket.statusTone}`}>{ticket.statusLabel}</span>
            <span className={`tcs-badge tcs-badge--priority-${ticket.priorityTone}`}>{ticket.priorityLabel}</span>
          </div>
        </article>
      ))}
    </div>
  );
}

/* ── Create ticket form ── */
const CATEGORIES: { value: SupportTicketCategory; label: string }[] = [
  { value: 'INQUIRY', label: 'Câu hỏi chung' },
  { value: 'BUG_REPORT', label: 'Lỗi phần mềm' },
  { value: 'SYSTEM_ERROR', label: 'Lỗi hệ thống' },
  { value: 'REPORT_USER', label: 'Báo cáo người dùng' },
  { value: 'DISPUTE', label: 'Tranh chấp' },
];

const PRIORITIES: { value: SupportTicketPriority; label: string }[] = [
  { value: 'LOW', label: 'Thấp' },
  { value: 'MEDIUM', label: 'Trung bình' },
  { value: 'HIGH', label: 'Cao' },
  { value: 'URGENT', label: 'Khẩn cấp' },
];

type CreateTicketProps = {
  onCancel: () => void;
  onCreated: () => void;
};

function CreateTicketForm({ onCancel, onCreated }: CreateTicketProps) {
  const [category, setCategory] = useState<SupportTicketCategory>('INQUIRY');
  const [subject, setSubject] = useState('');
  const [description, setDescription] = useState('');
  const [priority, setPriority] = useState<SupportTicketPriority>('LOW');

  const { status, errorMessage, submit } = useCreateTicket(onCreated);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    void submit({ category, subject: subject.trim(), description: description.trim(), priority });
  };

  return (
    <div className="create-ticket">
      <h2 className="create-ticket__title">Tạo yêu cầu hỗ trợ mới</h2>
      <form className="create-ticket__form" onSubmit={handleSubmit}>
        <div className="create-ticket__field">
          <label className="create-ticket__label create-ticket__label--required" htmlFor="ct-category">
            Danh mục
          </label>
          <select
            id="ct-category"
            className="create-ticket__select"
            value={category}
            onChange={(e) => setCategory(e.target.value as SupportTicketCategory)}
          >
            {CATEGORIES.map((c) => (
              <option key={c.value} value={c.value}>{c.label}</option>
            ))}
          </select>
        </div>

        <div className="create-ticket__field">
          <label className="create-ticket__label create-ticket__label--required" htmlFor="ct-subject">
            Tiêu đề
          </label>
          <input
            id="ct-subject"
            className="create-ticket__input"
            type="text"
            maxLength={150}
            placeholder="Mô tả ngắn về vấn đề của bạn"
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            required
          />
        </div>

        <div className="create-ticket__field">
          <label className="create-ticket__label create-ticket__label--required" htmlFor="ct-desc">
            Mô tả chi tiết
          </label>
          <textarea
            id="ct-desc"
            className="create-ticket__textarea"
            maxLength={5000}
            placeholder="Hãy mô tả chi tiết vấn đề, các bước tái hiện (nếu có)…"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            required
            rows={5}
          />
          <span className="create-ticket__hint">{description.length}/5000 ký tự</span>
        </div>

        <div className="create-ticket__field">
          <label className="create-ticket__label" htmlFor="ct-priority">
            Mức độ ưu tiên đề xuất
          </label>
          <select
            id="ct-priority"
            className="create-ticket__select"
            value={priority}
            onChange={(e) => setPriority(e.target.value as SupportTicketPriority)}
          >
            {PRIORITIES.map((p) => (
              <option key={p.value} value={p.value}>{p.label}</option>
            ))}
          </select>
          <span className="create-ticket__hint">
            Hệ thống có thể tự động nâng mức ưu tiên theo danh mục bạn chọn.
          </span>
        </div>

        {errorMessage && (
          <div className="create-ticket__error" role="alert">{errorMessage}</div>
        )}

        <div className="create-ticket__actions">
          <button type="submit" className="tcs-btn tcs-btn--primary" disabled={status === 'loading' || !subject.trim() || !description.trim()}>
            {status === 'loading' ? 'Đang gửi…' : 'Gửi yêu cầu'}
          </button>
          <button type="button" className="tcs-btn tcs-btn--ghost" onClick={onCancel} disabled={status === 'loading'}>
            Hủy
          </button>
        </div>
      </form>
    </div>
  );
}

/* ── Ticket detail ── */
type TicketDetailProps = {
  ticketId: string;
  onBack: () => void;
};

function TicketDetailPanel({ ticketId, onBack }: TicketDetailProps) {
  const { status, detail, errorMessage, reload } = useTicketDetail(ticketId);

  if (status === 'loading') {
    return <p style={{ color: '#718096', fontSize: '0.9rem' }}>Đang tải chi tiết yêu cầu…</p>;
  }

  if (status === 'error' || !detail) {
    return (
      <div className="ticket-list__error">
        {errorMessage ?? 'Không tải được chi tiết.'}{' '}
        <button type="button" onClick={reload} style={{ background: 'none', border: 'none', cursor: 'pointer', textDecoration: 'underline', color: 'inherit' }}>
          Thử lại
        </button>
      </div>
    );
  }

  return (
    <div className="ticket-detail">
      <div className="ticket-detail__header">
        <button type="button" className="ticket-detail__back-btn" onClick={onBack} aria-label="Quay lại danh sách">
          <IconBack />
        </button>
        <div className="ticket-detail__meta">
          <p className="ticket-detail__subject">{detail.subject}</p>
          <div className="ticket-detail__badges">
            <span className={`tcs-badge tcs-badge--status-${detail.statusTone}`}>{detail.statusLabel}</span>
            <span className={`tcs-badge tcs-badge--priority-${detail.priorityTone}`}>{detail.priorityLabel}</span>
            <span className="tcs-badge" style={{ background: '#f0f4f8', color: '#4a5568' }}>{detail.categoryLabel}</span>
          </div>
        </div>
      </div>

      <div className="ticket-detail__info">
        Tạo lúc {detail.createdAt} · Cập nhật {detail.updatedAt}
        {detail.assignedAdminId && ` · Đã gán cho admin #${detail.assignedAdminId}`}
      </div>

      <div className="ticket-detail__description">
        <div className="ticket-detail__description-label">Mô tả</div>
        {detail.description}
        {detail.evidenceUrls && (
          <p style={{ marginTop: '0.5rem', fontSize: '0.83rem', color: '#718096' }}>
            Bằng chứng: {detail.evidenceUrls}
          </p>
        )}
      </div>

      <div className="ticket-conv">
        <p className="ticket-conv__title">Hội thoại ({detail.messages.length})</p>
        {detail.messages.length === 0 ? (
          <p style={{ color: '#718096', fontSize: '0.88rem' }}>Chưa có tin nhắn nào.</p>
        ) : (
          <div className="ticket-conv__messages">
            {detail.messages.map((msg) => (
              <div
                key={msg.id}
                className={`ticket-msg ${msg.fromAdmin ? 'ticket-msg--admin' : 'ticket-msg--user'}`}
              >
                <div className="ticket-msg__sender">{msg.fromAdmin ? '🛡 ' : ''}{msg.senderName}</div>
                <div>{msg.content}</div>
                <div className="ticket-msg__time">{msg.sentAt}</div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

/* ── Main panel ── */
type View = 'list' | 'create' | 'detail';

type MessagingPanelProps = {
  defaultView?: View;
};

export function MessagingPanel({ defaultView = 'list' }: MessagingPanelProps) {
  const [view, setView] = useState<View>(defaultView);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [listKey, setListKey] = useState(0);

  const handleSelect = (id: string) => {
    setSelectedId(id);
    setView('detail');
  };

  const handleCreated = () => {
    setListKey((k) => k + 1);
    setView('list');
  };

  return (
    <div>
      {view === 'list' && (
        <TicketList
          key={listKey}
          onSelect={handleSelect}
          onNew={() => setView('create')}
        />
      )}
      {view === 'create' && (
        <CreateTicketForm
          onCancel={() => setView('list')}
          onCreated={handleCreated}
        />
      )}
      {view === 'detail' && selectedId && (
        <TicketDetailPanel
          ticketId={selectedId}
          onBack={() => setView('list')}
        />
      )}
    </div>
  );
}
