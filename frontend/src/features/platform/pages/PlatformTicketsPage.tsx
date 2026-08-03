import { createPortal } from 'react-dom';
import { useState, type FormEvent } from 'react';
import { AdminLayout } from '../components/AdminLayout';
import {
  useAdminTicketDetail,
  useAdminTicketList,
  useTicketMutations,
} from '../hooks/useAdminTickets';
import type {
  AdminTicketCategory,
  AdminTicketDetail,
  AdminTicketPriority,
  AdminTicketStatus,
} from '../types/platformTypes';
import './PlatformTicketsPage.css';

/* ── Inline badge helper ── */
function TicketStatusBadge({ tone, label }: { tone: string; label: string }) {
  return <span className={`tcs-badge tcs-badge--status-${tone}`}>{label}</span>;
}
function TicketPriorityBadge({ tone, label }: { tone: string; label: string }) {
  return <span className={`tcs-badge tcs-badge--priority-${tone}`}>{label}</span>;
}

/* ── Ticket detail modal ── */
type TicketModalProps = {
  ticketId: string;
  onClose: () => void;
};

function TicketDetailModal({ ticketId, onClose }: TicketModalProps) {
  const { status, detail, errorMessage, reload } = useAdminTicketDetail(ticketId);
  const [replyText, setReplyText] = useState('');
  const [closeNote, setCloseNote] = useState('');
  const [showClose, setShowClose] = useState(false);

  const handleMutationSuccess = () => {
    setReplyText('');
    setCloseNote('');
    setShowClose(false);
    reload();
  };

  const mutations = useTicketMutations(handleMutationSuccess);

  const handleRespond = () => {
    if (!replyText.trim()) return;
    void mutations.respond(ticketId, { content: replyText.trim() });
  };

  const handleClose = (closeStatus: 'RESOLVED' | 'CLOSED') => {
    void mutations.closeTicket(ticketId, { status: closeStatus, adminNotes: closeNote.trim() || undefined });
  };

  const isTerminated = detail?.status === 'RESOLVED' || detail?.status === 'CLOSED';

  return createPortal(
    <div
      className="adm-ticket-modal-overlay"
      role="presentation"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="adm-ticket-modal" role="dialog" aria-modal="true">
        {status === 'loading' && (
          <div style={{ padding: '2rem', textAlign: 'center', color: '#718096' }}>Dang tai...</div>
        )}

        {status === 'error' && (
          <div style={{ padding: '1.5rem' }}>
            <p style={{ color: '#991b1b' }}>{errorMessage}</p>
            <button type="button" className="tcs-btn tcs-btn--ghost" onClick={reload}>
              Thu lai
            </button>
            <button type="button" className="tcs-btn tcs-btn--ghost" onClick={onClose} style={{ marginLeft: '0.5rem' }}>
              Dong
            </button>
          </div>
        )}

        {status === 'success' && detail && (
          <TicketDetailContent
            detail={detail}
            replyText={replyText}
            setReplyText={setReplyText}
            closeNote={closeNote}
            setCloseNote={setCloseNote}
            showClose={showClose}
            setShowClose={setShowClose}
            isTerminated={isTerminated}
            mutStatus={mutations.status}
            mutError={mutations.errorMessage}
            onClose={onClose}
            onRespond={handleRespond}
            onCloseTicket={handleClose}
          />
        )}
      </div>
    </div>,
    document.body,
  );
}

type ContentProps = {
  detail: AdminTicketDetail;
  replyText: string;
  setReplyText: (v: string) => void;
  closeNote: string;
  setCloseNote: (v: string) => void;
  showClose: boolean;
  setShowClose: (v: boolean) => void;
  isTerminated: boolean;
  mutStatus: string;
  mutError: string | null;
  onClose: () => void;
  onRespond: () => void;
  onCloseTicket: (s: 'RESOLVED' | 'CLOSED') => void;
};

function TicketDetailContent({
  detail,
  replyText,
  setReplyText,
  closeNote,
  setCloseNote,
  showClose,
  setShowClose,
  isTerminated,
  mutStatus,
  mutError,
  onClose,
  onRespond,
  onCloseTicket,
}: ContentProps) {
  return (
    <>
      <div className="adm-ticket-modal__header">
        <div style={{ flex: 1 }}>
          <p className="adm-ticket-modal__subject">{detail.subject}</p>
          <div className="adm-ticket-modal__badges">
            <TicketStatusBadge tone={detail.statusTone} label={detail.statusLabel} />
            <TicketPriorityBadge tone={detail.priorityTone} label={detail.priorityLabel} />
            <span className="tcs-badge" style={{ background: '#f0f4f8', color: '#4a5568' }}>
              {detail.categoryLabel}
            </span>
            {detail.slaBreached && (
              <span className="tcs-badge" style={{ background: '#fee2e2', color: '#dc2626', fontWeight: 600 }}>
                🚨 Quá hạn SLA
              </span>
            )}
          </div>
        </div>
        <button type="button" className="adm-ticket-modal__close" onClick={onClose} aria-label="Dong">
          ×
        </button>
      </div>

      <div className="adm-ticket-modal__info">
        User #{detail.userId} - Tạo lúc {detail.createdAt} - Hạn SLA: {detail.dueAt || 'Chưa đặt'}
        {detail.assignedAdminId ? ` - Admin #${detail.assignedAdminId}` : ''}
        {detail.responseSlaMs != null ? ` - Phản hồi trong ${Math.round(detail.responseSlaMs / 1000 / 60)} phút` : ''}
      </div>

      <div className="adm-ticket-modal__body">
        <div className="adm-ticket-modal__section-title">Mo ta</div>
        <div className="adm-ticket-modal__desc">
          {detail.description}
          {detail.evidenceUrls && (
            <p style={{ marginTop: '0.4rem', fontSize: '0.8rem', color: '#718096' }}>
              Bang chung: {detail.evidenceUrls}
            </p>
          )}
        </div>

        <div className="adm-ticket-modal__section-title">
          Hoi thoai ({detail.messages.length})
        </div>
        {detail.messages.length > 0 ? (
          <div className="adm-ticket-conv">
            {detail.messages.map((msg) => (
              <div
                key={msg.id}
                className={`adm-ticket-msg ${msg.fromAdmin ? 'adm-ticket-msg--admin' : 'adm-ticket-msg--user'}`}
              >
                <div className="adm-ticket-msg__sender">{msg.senderName}</div>
                <div>{msg.content}</div>
                <div className="adm-ticket-msg__time">{msg.sentAt}</div>
              </div>
            ))}
          </div>
        ) : (
          <p style={{ color: '#718096', fontSize: '0.88rem', marginBottom: '1rem' }}>
            Chua co tin nhan.
          </p>
        )}

        {!isTerminated && (
          <div className="adm-ticket-respond">
            <div className="adm-ticket-modal__section-title" style={{ marginBottom: '0.5rem' }}>
              Phan hoi
            </div>
            <textarea
              className="adm-ticket-respond__textarea"
              placeholder="Nhap noi dung phan hoi cho nguoi dung..."
              value={replyText}
              onChange={(e) => setReplyText(e.target.value)}
              rows={3}
              maxLength={5000}
            />

            {showClose && (
              <textarea
                className="adm-ticket-respond__textarea"
                placeholder="Ghi chu dong ticket (tuy chon)..."
                value={closeNote}
                onChange={(e) => setCloseNote(e.target.value)}
                rows={2}
                maxLength={1000}
                style={{ marginTop: '0.5rem' }}
              />
            )}

            {mutError && (
              <div className="adm-ticket-respond__error">{mutError}</div>
            )}

            <div className="adm-ticket-respond__actions">
              <button
                type="button"
                className="tcs-btn tcs-btn--primary"
                onClick={onRespond}
                disabled={mutStatus === 'loading' || !replyText.trim()}
              >
                {mutStatus === 'loading' ? 'Dang gui...' : 'Gui phan hoi'}
              </button>
              {!showClose && (
                <button
                  type="button"
                  className="tcs-btn tcs-btn--ghost"
                  onClick={() => setShowClose(true)}
                >
                  Dong ticket
                </button>
              )}
              {showClose && (
                <>
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--primary"
                    style={{ background: '#16a34a' }}
                    onClick={() => onCloseTicket('RESOLVED')}
                    disabled={mutStatus === 'loading'}
                  >
                    Da giai quyet
                  </button>
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--ghost"
                    style={{ borderColor: '#e53e3e', color: '#e53e3e' }}
                    onClick={() => onCloseTicket('CLOSED')}
                    disabled={mutStatus === 'loading'}
                  >
                    Dong (khong giai quyet)
                  </button>
                  <button type="button" className="tcs-btn tcs-btn--ghost" onClick={() => setShowClose(false)}>
                    Huy
                  </button>
                </>
              )}
            </div>
          </div>
        )}

        {isTerminated && (
          <p style={{ color: '#718096', fontSize: '0.88rem', marginTop: '0.5rem' }}>
            Ticket nay da duoc dong.
          </p>
        )}
      </div>
    </>
  );
}

/* ── Main page ── */
const STATUSES: { value: AdminTicketStatus | ''; label: string }[] = [
  { value: '', label: 'Tat ca trang thai' },
  { value: 'OPEN', label: 'Cho xu ly' },
  { value: 'IN_PROGRESS', label: 'Dang xu ly' },
  { value: 'IN_REVIEW', label: 'Cho phan hoi' },
  { value: 'RESOLVED', label: 'Da giai quyet' },
  { value: 'CLOSED', label: 'Da dong' },
];

const CATEGORIES: { value: AdminTicketCategory | ''; label: string }[] = [
  { value: '', label: 'Tat ca danh muc' },
  { value: 'INQUIRY', label: 'Cau hoi chung' },
  { value: 'BUG_REPORT', label: 'Loi phan mem' },
  { value: 'SYSTEM_ERROR', label: 'Loi he thong' },
  { value: 'REPORT_USER', label: 'Bao cao nguoi dung' },
  { value: 'DISPUTE', label: 'Tranh chap' },
];

const PRIORITIES: { value: AdminTicketPriority | ''; label: string }[] = [
  { value: '', label: 'Tat ca do uu tien' },
  { value: 'LOW', label: 'Thap' },
  { value: 'MEDIUM', label: 'Trung binh' },
  { value: 'HIGH', label: 'Cao' },
  { value: 'URGENT', label: 'Khan cap' },
];

export default function PlatformTicketsPage() {
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [keywordDraft, setKeywordDraft] = useState('');

  const { status, data, filters, setFilters, errorMessage, reload } = useAdminTicketList({
    page: 0,
    size: 20,
  });

  const openCount = data?.items.filter((t) => t.status === 'OPEN').length ?? 0;
  const inProgressCount = data?.items.filter((t) => t.status === 'IN_PROGRESS').length ?? 0;

  const handleKeywordSearch = (e: FormEvent) => {
    e.preventDefault();
    setFilters((f) => ({ ...f, page: 0, keyword: keywordDraft || undefined }));
  };

  return (
    <AdminLayout
      title="Quan ly yeu cau ho tro"
      subtitle="Tiep nhan, phan hoi va dong cac yeu cau ho tro tu nguoi dung."
    >
      <div className="adm-summary-row">
        <article className="adm-summary-card adm-summary-card--warn">
          <p className="adm-summary-card__label">Cho xu ly</p>
          <p className="adm-summary-card__value">{openCount}</p>
        </article>
        <article className="adm-summary-card">
          <p className="adm-summary-card__label">Dang xu ly</p>
          <p className="adm-summary-card__value">{inProgressCount}</p>
        </article>
        <article className="adm-summary-card">
          <p className="adm-summary-card__label">Tong ticket</p>
          <p className="adm-summary-card__value">{data?.totalElements ?? '—'}</p>
        </article>
      </div>

      <div className="adm-card">
        <div className="adm-ticket-filters">
          <select
            className="adm-ticket-filter__select"
            value={filters.status ?? ''}
            onChange={(e) =>
              setFilters((f) => ({
                ...f,
                page: 0,
                status: (e.target.value as AdminTicketStatus) || undefined,
              }))
            }
          >
            {STATUSES.map((s) => (
              <option key={s.value} value={s.value}>{s.label}</option>
            ))}
          </select>

          <select
            className="adm-ticket-filter__select"
            value={filters.category ?? ''}
            onChange={(e) =>
              setFilters((f) => ({
                ...f,
                page: 0,
                category: (e.target.value as AdminTicketCategory) || undefined,
              }))
            }
          >
            {CATEGORIES.map((c) => (
              <option key={c.value} value={c.value}>{c.label}</option>
            ))}
          </select>

          <select
            className="adm-ticket-filter__select"
            value={filters.priority ?? ''}
            onChange={(e) =>
              setFilters((f) => ({
                ...f,
                page: 0,
                priority: (e.target.value as AdminTicketPriority) || undefined,
              }))
            }
          >
            {PRIORITIES.map((p) => (
              <option key={p.value} value={p.value}>{p.label}</option>
            ))}
          </select>

          <form onSubmit={handleKeywordSearch} style={{ display: 'flex', gap: '0.4rem', flex: 1 }}>
            <input
              type="text"
              className="adm-ticket-filter__input"
              placeholder="Tim kiem theo tieu de..."
              value={keywordDraft}
              onChange={(e) => setKeywordDraft(e.target.value)}
            />
            <button type="submit" className="tcs-btn tcs-btn--ghost" style={{ flexShrink: 0 }}>
              Tim
            </button>
          </form>

          <button type="button" className="tcs-btn tcs-btn--ghost" onClick={reload}>
            Lam moi
          </button>
        </div>

        {status === 'loading' && (
          <div className="adm-state adm-state--loading">
            <span className="adm-spinner" aria-hidden="true" />
            Dang tai danh sach ticket...
          </div>
        )}

        {status === 'error' && (
          <div className="adm-state">
            <p>{errorMessage ?? 'Khong tai duoc du lieu.'}</p>
            <button className="tcs-btn tcs-btn--market" type="button" onClick={reload}>
              Thu lai
            </button>
          </div>
        )}

        {status === 'success' && (
          <>
            <div className="adm-table-wrap">
              <table className="adm-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Nguoi dung</th>
                    <th>Tieu de</th>
                    <th>Danh muc</th>
                    <th>Do uu tien</th>
                    <th>Trang thai</th>
                    <th>SLA</th>
                    <th>Admin xu ly</th>
                    <th>Thoi gian</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                    {!data || data.items.length === 0 ? (
                      <tr>
                        <td colSpan={10}>Chua co yeu cau ho tro nao.</td>
                      </tr>
                  ) : (
                    data.items.map((ticket) => (
                      <tr key={ticket.id}>
                        <td>#{ticket.id}</td>
                        <td>{ticket.userEmail}</td>
                        <td style={{ maxWidth: 220, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {ticket.subject}
                        </td>
                        <td>{ticket.categoryLabel}</td>
                        <td className="adm-table__badge">
                          <TicketPriorityBadge tone={ticket.priorityTone} label={ticket.priorityLabel} />
                        </td>
                        <td className="adm-table__badge">
                          <TicketStatusBadge tone={ticket.statusTone} label={ticket.statusLabel} />
                        </td>
                        <td>
                          {ticket.slaBreached ? (
                            <span className="tcs-badge" style={{ background: '#fee2e2', color: '#dc2626', fontWeight: 600, fontSize: '0.75rem' }}>
                              🚨 Quá hạn
                            </span>
                          ) : ticket.dueAt ? (
                            <span style={{ fontSize: '0.78rem', color: '#4a5568' }}>{ticket.dueAt}</span>
                          ) : (
                            '—'
                          )}
                        </td>
                        <td>{ticket.assignedAdminName}</td>
                        <td>{ticket.createdAt}</td>
                        <td>
                          <button
                            type="button"
                            className="tcs-btn tcs-btn--ghost"
                            style={{ fontSize: '0.8rem', padding: '0.3rem 0.7rem' }}
                            onClick={() => setSelectedId(ticket.id)}
                          >
                            Xem
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {data && data.totalPages > 1 && (
              <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'center', marginTop: '1rem' }}>
                <button
                  type="button"
                  className="tcs-btn tcs-btn--ghost"
                  disabled={filters.page === 0}
                  onClick={() => setFilters((f) => ({ ...f, page: f.page - 1 }))}
                >
                  Truoc
                </button>
                <span style={{ alignSelf: 'center', fontSize: '0.88rem', color: '#4a5568' }}>
                  Trang {filters.page + 1} / {data.totalPages}
                </span>
                <button
                  type="button"
                  className="tcs-btn tcs-btn--ghost"
                  disabled={filters.page >= data.totalPages - 1}
                  onClick={() => setFilters((f) => ({ ...f, page: f.page + 1 }))}
                >
                  Sau
                </button>
              </div>
            )}
          </>
        )}
      </div>

      {selectedId && (
        <TicketDetailModal
          ticketId={selectedId}
          onClose={() => setSelectedId(null)}
        />
      )}
    </AdminLayout>
  );
}
