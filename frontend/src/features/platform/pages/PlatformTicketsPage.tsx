/**
 * ============================================================================
 * TRANG QUẢN LÝ VÀ TIẾP NHẬN YÊU CẦU HỖ TRỢ (PLATFORM TICKETS MANAGEMENT PAGE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp
 * Mô tả các chức năng hỗ trợ khách hàng và vận hành:
 *   - Hiển thị danh sách Support Ticket với bộ lọc đa chiều (Trạng thái, Phân loại, Mức ưu tiên, Từ khóa).
 *   - Xem chi tiết Ticket, chuỗi tin nhắn trao đổi (Thread messages), và thông tin người gửi.
 *   - Phản hồi Ticket và tự động đo lường First Response SLA.
 *   - Cập nhật phân loại danh mục (Category) và độ ưu tiên (Priority - Urgent/High/Medium/Low).
 *   - Đóng hoặc Giải quyết Ticket (RESOLVED / CLOSED) kèm ghi chú xử lý.
 *   - Gộp Ticket trùng lặp của cùng một khách hàng (Merge Tickets).
 *   - Chuyển tiếp sự cố sang luồng Xử lý Tranh chấp & Báo cáo (Redirect to Dispute & Reports).
 *   - Trực tiếp ban hành án phạt từ modal chi tiết (Issue Penalty Modal).
 */

import { createPortal } from 'react-dom';
import { useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import { AdminLayout } from '../components/AdminLayout';
import { AdminTimeFilter } from '../components/AdminTimeFilter';
import { Pagination } from '../../../shared/components';
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

/* ── Inline badge helper: Hiển thị nhãn trạng thái ticket ── */
function TicketStatusBadge({ tone, label }: { tone: string; label: string }) {
  return <span className={`tcs-badge tcs-badge--status-${tone}`}>{label}</span>;
}

/* ── Inline badge helper: Hiển thị nhãn độ ưu tiên ticket ── */
function TicketPriorityBadge({ tone, label }: { tone: string; label: string }) {
  return <span className={`tcs-badge tcs-badge--priority-${tone}`}>{label}</span>;
}

/* ── Modal chi tiết và xử lý Ticket của Admin ── */
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
  const [showMerge, setShowMerge] = useState(false);
  const [showDisputeTransfer, setShowDisputeTransfer] = useState(false);
  const [targetTicketId, setTargetTicketId] = useState('');
  const [mergeReason, setMergeReason] = useState('');
  const [disputeClassId, setDisputeClassId] = useState('');
  const [disputeNotes, setDisputeNotes] = useState('');
  const [editCategory, setEditCategory] = useState<AdminTicketCategory | ''>('');
  const [editPriority, setEditPriority] = useState<AdminTicketPriority | ''>('');
  const [isPenaltyModalOpen, setIsPenaltyModalOpen] = useState(false);
  const [penaltySuccessMessage, setPenaltySuccessMessage] = useState<string | null>(null);

  const handleMutationSuccess = () => {
    setReplyText('');
    setCloseNote('');
    setShowClose(false);
    setShowMerge(false);
    setShowDisputeTransfer(false);
    setTargetTicketId('');
    setMergeReason('');
    setDisputeClassId('');
    setDisputeNotes('');
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

  const handleMerge = () => {
    const targetIdNum = parseInt(targetTicketId.trim(), 10);
    if (isNaN(targetIdNum) || targetIdNum <= 0) return;
    void mutations.mergeTicket(ticketId, {
      targetTicketId: targetIdNum,
      reason: mergeReason.trim() || undefined,
    });
  };

  const handleRedirectDispute = () => {
    const classIdNum = disputeClassId.trim() ? parseInt(disputeClassId.trim(), 10) : undefined;
    void mutations.redirectTicketToDispute(ticketId, {
      targetClassId: classIdNum && !isNaN(classIdNum) ? classIdNum : undefined,
      notes: disputeNotes.trim() || undefined,
    });
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
            showMerge={showMerge}
            setShowMerge={setShowMerge}
            showDisputeTransfer={showDisputeTransfer}
            setShowDisputeTransfer={setShowDisputeTransfer}
            targetTicketId={targetTicketId}
            setTargetTicketId={setTargetTicketId}
            mergeReason={mergeReason}
            setMergeReason={setMergeReason}
            disputeClassId={disputeClassId}
            setDisputeClassId={setDisputeClassId}
            disputeNotes={disputeNotes}
            setDisputeNotes={setDisputeNotes}
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
            onMergeTicket={handleMerge}
            onRedirectDispute={handleRedirectDispute}
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
  showMerge: boolean;
  setShowMerge: (v: boolean) => void;
  showDisputeTransfer: boolean;
  setShowDisputeTransfer: (v: boolean) => void;
  targetTicketId: string;
  setTargetTicketId: (v: string) => void;
  mergeReason: string;
  setMergeReason: (v: string) => void;
  disputeClassId: string;
  setDisputeClassId: (v: string) => void;
  disputeNotes: string;
  setDisputeNotes: (v: string) => void;
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
  onMergeTicket: () => void;
  onRedirectDispute: () => void;
};

function TicketDetailContent({
  detail,
  replyText,
  setReplyText,
  closeNote,
  setCloseNote,
  showClose,
  setShowClose,
  showMerge,
  setShowMerge,
  showDisputeTransfer,
  setShowDisputeTransfer,
  targetTicketId,
  setTargetTicketId,
  mergeReason,
  setMergeReason,
  disputeClassId,
  setDisputeClassId,
  disputeNotes,
  setDisputeNotes,
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
  onMergeTicket,
  onRedirectDispute,
}: ContentProps) {
  const currentCategory = editCategory || detail.category;
  const isDispute = currentCategory === 'DISPUTE';

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
            {detail.category === 'DISPUTE' && (
              <a
                href="/platform/reports"
                className="tcs-badge"
                style={{ background: '#fef3c7', color: '#92400e', border: '1px solid #fcd34d', textDecoration: 'none', fontWeight: 600 }}
                title="Mở trang Xử lý Báo cáo & Tranh chấp"
              >
                Mở trang Báo cáo & Tranh chấp
              </a>
            )}
            {detail.slaBreached && (
              <span className="tcs-badge" style={{ background: '#fee2e2', color: '#dc2626', fontWeight: 600 }}>
                Quá hạn SLA
              </span>
            )}
            <button
              type="button"
              className="tcs-btn tcs-btn--sm tcs-btn--danger"
              style={{ fontSize: '0.75rem', padding: '2px 8px' }}
              onClick={onOpenPenaltyModal}
              title="Tạo quyết định xử phạt liên quan đến ticket này"
            >
              Tạo xử phạt
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
              Phản hồi & Xử lý
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
                placeholder="Ghi chú đóng / từ chối ticket (tùy chọn)..."
                value={closeNote}
                onChange={(e) => setCloseNote(e.target.value)}
                rows={2}
                maxLength={1000}
                style={{ marginTop: '0.5rem' }}
              />
            )}

            {showMerge && (
              <div style={{ background: '#eff6ff', border: '1px solid #bfdbfe', borderRadius: '8px', padding: '12px', marginTop: '0.75rem' }}>
                <div style={{ fontWeight: 600, color: '#1e40af', marginBottom: '6px', fontSize: '0.9rem' }}>
                  Gộp Ticket trùng lặp (Merge Ticket)
                </div>
                <p style={{ fontSize: '0.8rem', color: '#3b82f6', marginBottom: '8px' }}>
                  Ticket #{detail.id} sẽ được đóng với trạng thái CLOSED và toàn bộ nội dung sẽ được gộp vào Ticket đích.
                </p>
                <input
                  type="number"
                  placeholder="Nhập mã Ticket gốc cần gộp vào (ví dụ: 1)..."
                  value={targetTicketId}
                  onChange={(e) => setTargetTicketId(e.target.value)}
                  style={{ width: '100%', marginBottom: '8px', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                />
                <textarea
                  className="adm-ticket-respond__textarea"
                  placeholder="Lý do gộp ticket (tùy chọn)..."
                  value={mergeReason}
                  onChange={(e) => setMergeReason(e.target.value)}
                  rows={2}
                  maxLength={500}
                />
                <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--primary"
                    style={{ background: '#2563eb' }}
                    onClick={onMergeTicket}
                    disabled={mutStatus === 'loading' || !targetTicketId.trim()}
                  >
                    {mutStatus === 'loading' ? 'Đang gộp...' : 'Xác nhận gộp Ticket'}
                  </button>
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--ghost"
                    onClick={() => setShowMerge(false)}
                  >
                    Hủy
                  </button>
                </div>
              </div>
            )}

            {showDisputeTransfer && (
              <div style={{ background: '#fffbeb', border: '1px solid #fde68a', borderRadius: '8px', padding: '12px', marginTop: '0.75rem' }}>
                <div style={{ fontWeight: 600, color: '#92400e', marginBottom: '6px', fontSize: '0.9rem' }}>
                  Chuyển tiếp sang luồng Tranh chấp & Báo cáo sự cố
                </div>
                <p style={{ fontSize: '0.8rem', color: '#b45309', marginBottom: '8px' }}>
                  Ticket sẽ được cập nhật sang danh mục DISPUTE, nâng mức ưu tiên lên HIGH và tự động tạo báo cáo sự cố trong trang /platform/reports.
                </p>
                <input
                  type="number"
                  placeholder="Mã lớp học liên quan (tùy chọn)..."
                  value={disputeClassId}
                  onChange={(e) => setDisputeClassId(e.target.value)}
                  style={{ width: '100%', marginBottom: '8px', padding: '8px 12px', borderRadius: '6px', border: '1px solid #cbd5e1' }}
                />
                <textarea
                  className="adm-ticket-respond__textarea"
                  placeholder="Ghi chú bàn giao chuyển tiếp sang Tranh chấp (tùy chọn)..."
                  value={disputeNotes}
                  onChange={(e) => setDisputeNotes(e.target.value)}
                  rows={2}
                  maxLength={500}
                />
                <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--primary"
                    style={{ background: '#d97706' }}
                    onClick={onRedirectDispute}
                    disabled={mutStatus === 'loading'}
                  >
                    {mutStatus === 'loading' ? 'Đang chuyển...' : 'Xác nhận sang tranh chấp'}
                  </button>
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--ghost"
                    onClick={() => setShowDisputeTransfer(false)}
                  >
                    Hủy
                  </button>
                </div>
              </div>
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
              {!showClose && !showMerge && !showDisputeTransfer && (
                <>
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--ghost"
                    onClick={() => {
                      setShowClose(true);
                      setShowMerge(false);
                      setShowDisputeTransfer(false);
                    }}
                  >
                    Đóng ticket
                  </button>
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--ghost"
                    style={{ borderColor: '#3b82f6', color: '#2563eb' }}
                    onClick={() => {
                      setShowMerge(true);
                      setShowClose(false);
                      setShowDisputeTransfer(false);
                    }}
                    title="Gộp ticket trùng lặp vào một ticket gốc"
                  >
                    Gộp Ticket
                  </button>
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--ghost"
                    style={{
                      borderColor: isDispute ? '#d97706' : '#cbd5e1',
                      color: isDispute ? '#b45309' : '#94a3b8',
                      cursor: isDispute ? 'pointer' : 'not-allowed',
                    }}
                    disabled={!isDispute || mutStatus === 'loading'}
                    onClick={() => {
                      setShowDisputeTransfer(true);
                      setShowClose(false);
                      setShowMerge(false);
                    }}
                    title={isDispute ? 'Chuyển ticket này sang luồng Xử lý Tranh chấp' : 'Chỉ khả dụng khi ticket thuộc danh mục Tranh chấp'}
                  >
                    Chuyển sang Tranh chấp
                  </button>
                </>
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
                    Đóng (Từ chối)
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

            {data && data.items.length > 0 && (
              <div className="adm-pagination" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: '16px', gap: '8px' }}>
                <select
                  className="adm-field adm-field--fixed"
                  style={{ width: 'auto', padding: '4px 8px', fontSize: '13px', borderRadius: '8px' }}
                  value={filters.size}
                  onChange={(e) =>
                    setFilters((f) => ({
                      ...f,
                      size: Number(e.target.value),
                      page: 0,
                    }))
                  }
                >
                  <option value={10}>10 / trang</option>
                  <option value={20}>20 / trang</option>
                  <option value={50}>50 / trang</option>
                </select>
                <Pagination
                  current={filters.page + 1}
                  totalPages={Math.max(data.totalPages, 1)}
                  onPageChange={(p) => setFilters((f) => ({ ...f, page: p - 1 }))}
                />
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
