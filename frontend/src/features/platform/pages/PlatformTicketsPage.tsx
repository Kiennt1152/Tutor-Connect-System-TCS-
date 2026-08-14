import { createPortal } from 'react-dom';
import { useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { AdminLayout } from '../components/AdminLayout';
import { AdminTimeFilter } from '../components/AdminTimeFilter';
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
import { IssuePenaltyModal } from '../components/IssuePenaltyModal';
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
  onUpdated: () => void;
};

function TicketDetailModal({ ticketId, onClose, onUpdated }: TicketModalProps) {
  const { status, detail, errorMessage, reload } = useAdminTicketDetail(ticketId);
  const [replyText, setReplyText] = useState('');
  const [closeNote, setCloseNote] = useState('');
  const [showClose, setShowClose] = useState(false);
  const [editCategory, setEditCategory] = useState<AdminTicketCategory | ''>('');
  const [editPriority, setEditPriority] = useState<AdminTicketPriority | ''>('');
  const [isPenaltyModalOpen, setIsPenaltyModalOpen] = useState(false);
  const [penaltySuccessMessage, setPenaltySuccessMessage] = useState<string | null>(null);

  const handleMutationSuccess = () => {
    setReplyText('');
    setCloseNote('');
    setShowClose(false);
    reload();
    onUpdated();
  };

  const mutations = useTicketMutations(handleMutationSuccess);

  const handleRespond = () => {
    if (!replyText.trim()) return;
    void mutations.respond(ticketId, { content: replyText.trim() });
  };

  const handleClose = (closeStatus: 'RESOLVED' | 'CLOSED') => {
    void mutations.closeTicket(ticketId, { status: closeStatus, adminNotes: closeNote.trim() || undefined });
  };

  const handleUpdate = () => {
    if (!detail) return;
    const payload: { category?: AdminTicketCategory; priority?: AdminTicketPriority } = {};
    const nextCategory = editCategory || detail.category;
    const nextPriority = editPriority || detail.priority;
    if (nextCategory !== detail.category) payload.category = nextCategory;
    if (nextPriority !== detail.priority) payload.priority = nextPriority;
    if (Object.keys(payload).length === 0) return;
    void mutations.updateTicket(ticketId, payload);
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
          <div style={{ padding: '2rem', textAlign: 'center', color: '#718096' }}>Đang tải...</div>
        )}

        {status === 'error' && (
          <div style={{ padding: '1.5rem' }}>
            <p style={{ color: '#991b1b' }}>{errorMessage}</p>
            <button type="button" className="tcs-btn tcs-btn--ghost" onClick={reload}>
              Thử lại
            </button>
            <button type="button" className="tcs-btn tcs-btn--ghost" onClick={onClose} style={{ marginLeft: '0.5rem' }}>
              Đóng
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
            editCategory={editCategory || detail.category}
            setEditCategory={setEditCategory}
            editPriority={editPriority || detail.priority}
            setEditPriority={setEditPriority}
            mutStatus={mutations.status}
            mutError={mutations.errorMessage}
            penaltySuccessMessage={penaltySuccessMessage}
            onOpenPenaltyModal={() => setIsPenaltyModalOpen(true)}
            onClose={onClose}
            onRespond={handleRespond}
            onUpdate={handleUpdate}
            onCloseTicket={handleClose}
          />
        )}
        {detail && (
          <IssuePenaltyModal
            isOpen={isPenaltyModalOpen}
            onClose={() => setIsPenaltyModalOpen(false)}
            onSuccess={() => {
              setPenaltySuccessMessage('Đã ban hành quyết định xử phạt thành công và lưu vết từ Ticket.');
              onUpdated();
            }}
            initialUserId={Number(detail.userId) || undefined}
            initialReason={`Xử lý từ Hỗ trợ & Khiếu nại #${detail.id}: ${detail.subject}`}
            initialEvidenceUrls={detail.evidenceUrls || undefined}
            sourceType="TICKET"
            sourceId={detail.id}
            sourceTaskId={`TICKET-${detail.id}`}
            title={`Tạo xử phạt từ Ticket #${detail.id}`}
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
  editCategory: AdminTicketCategory | '';
  setEditCategory: (value: AdminTicketCategory) => void;
  editPriority: AdminTicketPriority | '';
  setEditPriority: (value: AdminTicketPriority) => void;
  mutStatus: string;
  mutError: string | null;
  penaltySuccessMessage: string | null;
  onOpenPenaltyModal: () => void;
  onClose: () => void;
  onRespond: () => void;
  onUpdate: () => void;
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
  editCategory,
  setEditCategory,
  editPriority,
  setEditPriority,
  mutStatus,
  mutError,
  penaltySuccessMessage,
  onOpenPenaltyModal,
  onClose,
  onRespond,
  onUpdate,
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
            <button
              type="button"
              className="tcs-btn tcs-btn--sm tcs-btn--danger"
              style={{ fontSize: '0.75rem', padding: '2px 8px' }}
              onClick={onOpenPenaltyModal}
              title="Tạo quyết định xử phạt liên quan đến ticket này"
            >
              ⚖️ Tạo xử phạt
            </button>
          </div>
        </div>
        <button type="button" className="adm-ticket-modal__close" onClick={onClose} aria-label="Đóng">
          ×
        </button>
      </div>

      {penaltySuccessMessage && (
        <div style={{ padding: '8px 12px', background: '#f0fdf4', border: '1px solid #bbf7d0', color: '#166534', fontSize: '0.85rem', margin: '0 16px 12px' }}>
          {penaltySuccessMessage}
        </div>
      )}

      <div className="adm-ticket-modal__info">
        User #{detail.userId} - Tạo lúc {detail.createdAt} - Hạn SLA: {detail.dueAt || 'Chưa đặt'}
        {detail.assignedAdminId ? ` - Admin #${detail.assignedAdminId}` : ''}
        {detail.responseSlaMs != null ? ` - Phản hồi trong ${Math.round(detail.responseSlaMs / 1000 / 60)} phút` : ''}
      </div>

      <div className="adm-ticket-modal__body">
        <div className="adm-ticket-classification">
          <div className="adm-ticket-modal__section-title">Phân loại xử lý</div>
          <div className="adm-ticket-classification__grid">
            <label>
              <span>Danh mục</span>
              <select
                value={editCategory}
                disabled={isTerminated || mutStatus === 'loading'}
                onChange={(event) => setEditCategory(event.target.value as AdminTicketCategory)}
              >
                {CATEGORIES.filter((item) => item.value).map((item) => (
                  <option key={item.value} value={item.value}>{item.label}</option>
                ))}
              </select>
            </label>
            <label>
              <span>Độ ưu tiên</span>
              <select
                value={editPriority}
                disabled={isTerminated || mutStatus === 'loading'}
                onChange={(event) => setEditPriority(event.target.value as AdminTicketPriority)}
              >
                {PRIORITIES.filter((item) => item.value).map((item) => (
                  <option key={item.value} value={item.value}>{item.label}</option>
                ))}
              </select>
            </label>
            <div className="adm-ticket-classification__sla">
              <span>Hạn SLA hiện tại</span>
              <strong>{detail.dueAt || 'Chưa đặt'}</strong>
            </div>
            <button
              type="button"
              className="tcs-btn tcs-btn--primary"
              disabled={
                isTerminated
                || mutStatus === 'loading'
                || (editCategory === detail.category && editPriority === detail.priority)
              }
              onClick={onUpdate}
            >
              {mutStatus === 'loading' ? 'Đang lưu...' : 'Lưu phân loại'}
            </button>
          </div>
          {mutStatus === 'success' && !mutError && (
            <div className="adm-ticket-classification__success">Đã cập nhật ticket.</div>
          )}
          {isTerminated && (
            <div className="adm-ticket-classification__locked">Ticket đã kết thúc và chỉ có thể xem.</div>
          )}
        </div>

        <div className="adm-ticket-modal__section-title">Mô tả</div>
        <div className="adm-ticket-modal__desc">
          {detail.description}
          {detail.evidenceUrls && (
            <p style={{ marginTop: '0.4rem', fontSize: '0.8rem', color: '#718096' }}>
              Bằng chứng: {detail.evidenceUrls}
            </p>
          )}
        </div>

        <div className="adm-ticket-modal__section-title">
          Hội thoại ({detail.messages.length})
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
            Chưa có tin nhắn.
          </p>
        )}

        {!isTerminated && (
          <div className="adm-ticket-respond">
            <div className="adm-ticket-modal__section-title" style={{ marginBottom: '0.5rem' }}>
              Phản hồi
            </div>
            <textarea
              className="adm-ticket-respond__textarea"
              placeholder="Nhập nội dung phản hồi cho người dùng..."
              value={replyText}
              onChange={(e) => setReplyText(e.target.value)}
              rows={3}
              maxLength={5000}
            />

            {showClose && (
              <textarea
                className="adm-ticket-respond__textarea"
                placeholder="Ghi chú đóng ticket (tùy chọn)..."
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
                {mutStatus === 'loading' ? 'Đang gửi...' : 'Gửi phản hồi'}
              </button>
              {!showClose && (
                <button
                  type="button"
                  className="tcs-btn tcs-btn--ghost"
                  onClick={() => setShowClose(true)}
                >
                  Đóng ticket
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
                    Đã giải quyết
                  </button>
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--ghost"
                    style={{ borderColor: '#e53e3e', color: '#e53e3e' }}
                    onClick={() => onCloseTicket('CLOSED')}
                    disabled={mutStatus === 'loading'}
                  >
                    Đóng (không giải quyết)
                  </button>
                  <button type="button" className="tcs-btn tcs-btn--ghost" onClick={() => setShowClose(false)}>
                    Hủy
                  </button>
                </>
              )}
            </div>
          </div>
        )}

        {isTerminated && (
          <p style={{ color: '#718096', fontSize: '0.88rem', marginTop: '0.5rem' }}>
            Ticket này đã được đóng.
          </p>
        )}
      </div>
    </>
  );
}

/* ── Main page ── */
const STATUSES: { value: AdminTicketStatus | ''; label: string }[] = [
  { value: '', label: 'Tất cả trạng thái' },
  { value: 'OPEN', label: 'Chờ xử lý' },
  { value: 'IN_PROGRESS', label: 'Đang xử lý' },
  { value: 'IN_REVIEW', label: 'Chờ phản hồi' },
  { value: 'RESOLVED', label: 'Đã giải quyết' },
  { value: 'CLOSED', label: 'Đã đóng' },
];

const CATEGORIES: { value: AdminTicketCategory | ''; label: string }[] = [
  { value: '', label: 'Tất cả danh mục' },
  { value: 'INQUIRY', label: 'Câu hỏi chung' },
  { value: 'BUG_REPORT', label: 'Lỗi phần mềm' },
  { value: 'SYSTEM_ERROR', label: 'Lỗi hệ thống' },
  { value: 'REPORT_USER', label: 'Báo cáo người dùng' },
  { value: 'DISPUTE', label: 'Tranh chấp' },
];

const PRIORITIES: { value: AdminTicketPriority | ''; label: string }[] = [
  { value: '', label: 'Tất cả độ ưu tiên' },
  { value: 'LOW', label: 'Thấp' },
  { value: 'MEDIUM', label: 'Trung bình' },
  { value: 'HIGH', label: 'Cao' },
  { value: 'URGENT', label: 'Khẩn cấp' },
];

export default function PlatformTicketsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const ticketParam = searchParams.get('id') || searchParams.get('ticket');
  const selectedId = ticketParam && /^\d+$/.test(ticketParam) ? ticketParam : null;
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

  const openTicket = (ticketId: string) => {
    setSearchParams({ id: ticketId });
  };

  const closeTicket = () => {
    setSearchParams({});
  };

  return (
    <AdminLayout
      title="Quản lý yêu cầu hỗ trợ"
      subtitle="Tiếp nhận, phản hồi và đóng các yêu cầu hỗ trợ từ người dùng."
    >
      <div className="adm-summary-row">
        <article className="adm-summary-card adm-summary-card--warn">
          <p className="adm-summary-card__label">Chờ xử lý</p>
          <p className="adm-summary-card__value">{openCount}</p>
        </article>
        <article className="adm-summary-card">
          <p className="adm-summary-card__label">Đang xử lý</p>
          <p className="adm-summary-card__value">{inProgressCount}</p>
        </article>
        <article className="adm-summary-card">
          <p className="adm-summary-card__label">Tổng vé hỗ trợ</p>
          <p className="adm-summary-card__value">{data?.totalElements ?? '—'}</p>
        </article>
      </div>

      <AdminTimeFilter showGranularity={false} />

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
              placeholder="Tìm kiếm theo tiêu đề..."
              value={keywordDraft}
              onChange={(e) => setKeywordDraft(e.target.value)}
            />
            <button type="submit" className="tcs-btn tcs-btn--ghost" style={{ flexShrink: 0 }}>
              Tìm
            </button>
          </form>

          <button type="button" className="tcs-btn tcs-btn--ghost" onClick={reload}>
            Làm mới
          </button>
        </div>

        {status === 'loading' && (
          <div className="adm-state adm-state--loading">
            <span className="adm-spinner" aria-hidden="true" />
            Đang tải danh sách vé hỗ trợ...
          </div>
        )}

        {status === 'error' && (
          <div className="adm-state">
            <p>{errorMessage ?? 'Không tải được dữ liệu.'}</p>
            <button className="tcs-btn tcs-btn--market" type="button" onClick={reload}>
              Thử lại
            </button>
          </div>
        )}

        {status === 'success' && (
          <>
            <div className="adm-table-wrap adm-ticket-table-wrap">
              <table className="adm-table adm-ticket-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Người dùng</th>
                    <th>Tiêu đề</th>
                    <th>Danh mục</th>
                    <th>Độ ưu tiên</th>
                    <th>Trạng thái</th>
                    <th>SLA</th>
                    <th>Admin xử lý</th>
                    <th>Thời gian</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                    {!data || data.items.length === 0 ? (
                      <tr>
                        <td colSpan={10}>Chưa có yêu cầu hỗ trợ nào.</td>
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
                            onClick={() => openTicket(ticket.id)}
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
                  Trước
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
          onClose={closeTicket}
          onUpdated={reload}
        />
      )}
    </AdminLayout>
  );
}
