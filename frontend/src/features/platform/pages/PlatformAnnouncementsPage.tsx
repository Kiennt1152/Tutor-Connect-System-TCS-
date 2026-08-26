/**
 * ============================================================================
 * TRANG QUẢN TRỊ THÔNG BÁO HỆ THỐNG (PLATFORM ANNOUNCEMENTS PAGE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp
 * Mô tả các tính năng quản trị thông báo:
 *   - Hiển thị danh sách thông báo/banner toàn nền tảng.
 *   - Tạo mới hoặc cập nhật thông báo (Tiêu đề, Nội dung, Thời hạn hiển thị, Bật/Tắt).
 *   - Phân luồng đối tượng nhận thông báo (Học viên, Gia sư, Trung tâm, Quản trị viên).
 *   - Thao tác nhanh bật/tắt (Toggle active) hoặc xóa thông báo có hộp thoại xác nhận.
 */

import type { FormEvent } from 'react';
import { useState, useEffect } from 'react';
import { ConfirmDialog, Pagination } from '../../../shared/components';
import { AdminLayout } from '../components/AdminLayout';
import { useAnnouncementList, useAnnouncementMutations } from '../hooks/useAnnouncements';
import type {
  AnnouncementItem,
  AnnouncementTargetRole,
  UpsertAnnouncementApiRequest,
} from '../types/platformTypes';

type FormState = {
  title: string;
  content: string;
  targetRole: '' | AnnouncementTargetRole;
  active: boolean;
  startsAt: string;
  endsAt: string;
};

const EMPTY_FORM: FormState = {
  title: '',
  content: '',
  targetRole: '',
  active: true,
  startsAt: '',
  endsAt: '',
};

const ROLE_LABELS: Record<AnnouncementTargetRole, string> = {
  CLIENT: 'Học viên',
  TUTOR: 'Gia sư',
  TUTOR_CENTER: 'Trung tâm',
  PLATFORM_ADMIN: 'Quản trị viên',
};

function formatDateTime(value: string | null) {
  if (!value) return '—';
  try {
    return new Date(value).toLocaleString('vi-VN');
  } catch {
    return value;
  }
}

function toInput(value: string | null) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function toPayload(form: FormState): UpsertAnnouncementApiRequest {
  return {
    title: form.title,
    content: form.content,
    targetRole: form.targetRole === '' ? null : form.targetRole,
    active: form.active,
    startsAt: form.startsAt ? new Date(form.startsAt).toISOString() : null,
    endsAt: form.endsAt ? new Date(form.endsAt).toISOString() : null,
  };
}

export default function PlatformAnnouncementsPage() {
  const { status, items, errorMessage: listErrorMessage, reload } = useAnnouncementList();
  const { status: mutationStatus, errorMessage, createAnnouncement, updateAnnouncement, deleteAnnouncement, reset } =
    useAnnouncementMutations();

  const [form, setForm] = useState<FormState>(EMPTY_FORM);
  const [selected, setSelected] = useState<AnnouncementItem | null>(null);
  const [pendingDelete, setPendingDelete] = useState<AnnouncementItem | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  useEffect(() => {
    setCurrentPage(1);
  }, [items.length]);

  const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
  const validCurrentPage = Math.min(currentPage, totalPages);
  const startIndex = (validCurrentPage - 1) * pageSize;
  const endIndex = Math.min(startIndex + pageSize, items.length);
  const paginatedItems = items.slice(startIndex, endIndex);

  function selectAnnouncement(item: AnnouncementItem) {
    setSelected(item);
    reset();
    setForm({
      title: item.title,
      content: item.content,
      targetRole: item.targetRole ?? '',
      active: item.active,
      startsAt: toInput(item.startsAt),
      endsAt: toInput(item.endsAt),
    });
  }

  function resetForm() {
    setSelected(null);
    reset();
    setForm(EMPTY_FORM);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const ok = selected
      ? await updateAnnouncement(selected.announcementId, toPayload(form))
      : await createAnnouncement(toPayload(form));
    if (ok) {
      resetForm();
      reload();
    }
  }

  async function confirmDelete() {
    if (!pendingDelete) return;
    const ok = await deleteAnnouncement(pendingDelete.announcementId);
    setPendingDelete(null);
    if (ok) {
      if (selected?.announcementId === pendingDelete.announcementId) resetForm();
      reload();
    }
  }

  return (
    <AdminLayout title="Thông báo hệ thống" subtitle="Tạo và quản lý thông báo hiển thị cho người dùng.">
      <div className="adm-layout-split">
        <div className="adm-card">
          <div className="adm-toolbar">
            <button className="tcs-btn tcs-btn--ghost" type="button" onClick={reload}>
              Làm mới
            </button>
            <button className="tcs-btn tcs-btn--primary" type="button" onClick={resetForm}>
              Tạo thông báo mới
            </button>
          </div>

          {status === 'loading' && <div className="adm-state">Đang tải thông báo...</div>}
          {status === 'error' && (
            <div className="adm-state">
              <p>{listErrorMessage ?? 'Không tải được dữ liệu.'}</p>
              <button className="tcs-btn tcs-btn--primary" type="button" onClick={reload}>
                Thử lại
              </button>
            </div>
          )}

          {status === 'success' && (
            <div className="adm-table-wrap">
              <table className="adm-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Tiêu đề</th>
                    <th>Đối tượng</th>
                    <th>Trạng thái</th>
                    <th>Hiệu lực</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {items.length === 0 ? (
                    <tr>
                      <td colSpan={6}>Chưa có thông báo nào.</td>
                    </tr>
                  ) : (
                    paginatedItems.map((item) => (
                      <tr key={item.announcementId}>
                        <td>{item.announcementId}</td>
                        <td className="adm-table__notes">{item.title}</td>
                        <td>{item.targetRole ? ROLE_LABELS[item.targetRole] : 'Tất cả'}</td>
                        <td className="adm-table__badge">
                          <span className={item.active ? 'tcs-badge tcs-badge--active' : 'tcs-badge tcs-badge--suspended'}>
                            {item.active ? 'Đang bật' : 'Đã tắt'}
                          </span>
                        </td>
                        <td>{formatDateTime(item.startsAt)} → {formatDateTime(item.endsAt)}</td>
                        <td className="adm-table__actions">
                          <div className="adm-row-actions">
                            <button className="tcs-btn tcs-btn--ghost tcs-btn--badge" type="button" onClick={() => selectAnnouncement(item)}>
                              Sửa
                            </button>
                            <button className="tcs-btn tcs-btn--danger tcs-btn--badge" type="button" onClick={() => setPendingDelete(item)}>
                              Xóa
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}

          {status === 'success' && items.length > 0 && (
            <div className="adm-pagination" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: '16px', gap: '8px' }}>
              <select
                className="adm-field adm-field--fixed"
                style={{ width: 'auto', padding: '4px 8px', fontSize: '13px', borderRadius: '8px' }}
                value={pageSize}
                onChange={(e) => {
                  setPageSize(Number(e.target.value));
                  setCurrentPage(1);
                }}
              >
                <option value={10}>10 / trang</option>
                <option value={20}>20 / trang</option>
                <option value={50}>50 / trang</option>
              </select>
              <Pagination
                current={validCurrentPage}
                totalPages={totalPages}
                onPageChange={setCurrentPage}
              />
            </div>
          )}
        </div>

        <article className="adm-card adm-card--sticky">
          <div className="adm-card__head">
            <h2 className="adm-card__title">{selected ? `Chỉnh sửa thông báo #${selected.announcementId}` : 'Tạo thông báo mới'}</h2>
          </div>

          <form className="adm-form" onSubmit={(event) => void handleSubmit(event)}>
            <div className="adm-field-group">
              <label htmlFor="ann-title">Tiêu đề</label>
              <input
                id="ann-title"
                className="adm-field"
                value={form.title}
                onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
                placeholder="Tiêu đề thông báo..."
                maxLength={200}
                required
              />
            </div>

            <div className="adm-field-group">
              <label htmlFor="ann-content">Nội dung</label>
              <textarea
                id="ann-content"
                className="adm-field adm-field--tall"
                value={form.content}
                onChange={(event) => setForm((current) => ({ ...current, content: event.target.value }))}
                placeholder="Nội dung thông báo hiển thị cho người dùng..."
                required
              />
            </div>

            <div className="adm-field-group">
              <label htmlFor="ann-role">Đối tượng hiển thị</label>
              <select
                id="ann-role"
                className="adm-field adm-field--inline"
                value={form.targetRole}
                onChange={(event) => setForm((current) => ({ ...current, targetRole: event.target.value as FormState['targetRole'] }))}
              >
                <option value="">Tất cả vai trò</option>
                <option value="CLIENT">Học viên</option>
                <option value="TUTOR">Gia sư</option>
                <option value="TUTOR_CENTER">Trung tâm</option>
                <option value="PLATFORM_ADMIN">Quản trị viên</option>
              </select>
            </div>

            <div className="adm-field-row">
              <div className="adm-field-group">
                <label htmlFor="ann-starts">Bắt đầu hiển thị</label>
                <input
                  id="ann-starts"
                  type="datetime-local"
                  className="adm-field"
                  value={form.startsAt}
                  onChange={(event) => setForm((current) => ({ ...current, startsAt: event.target.value }))}
                />
              </div>
              <div className="adm-field-group">
                <label htmlFor="ann-ends">Kết thúc hiển thị</label>
                <input
                  id="ann-ends"
                  type="datetime-local"
                  className="adm-field"
                  value={form.endsAt}
                  onChange={(event) => setForm((current) => ({ ...current, endsAt: event.target.value }))}
                />
              </div>
            </div>

            <div className="adm-field-group adm-field-group--inline">
              <label htmlFor="ann-active">
                <input
                  id="ann-active"
                  type="checkbox"
                  checked={form.active}
                  onChange={(event) => setForm((current) => ({ ...current, active: event.target.checked }))}
                />
                Kích hoạt (hiển thị cho người dùng)
              </label>
            </div>

            {mutationStatus === 'error' && errorMessage && (
              <div className="adm-alert adm-alert--error">{errorMessage}</div>
            )}

            <div className="adm-form__footer">
              <button className="tcs-btn tcs-btn--primary" type="submit" disabled={mutationStatus === 'loading'}>
                {mutationStatus === 'loading' ? 'Đang lưu…' : selected ? 'Cập nhật' : 'Tạo thông báo'}
              </button>
              <button className="tcs-btn tcs-btn--ghost" type="button" onClick={resetForm}>
                Xóa form
              </button>
            </div>
          </form>
        </article>
      </div>

      {pendingDelete && (
        <ConfirmDialog
          open
          title="Xóa thông báo"
          message={`Xóa thông báo "${pendingDelete.title}"? Hành động này không thể hoàn tác.`}
          confirmLabel="Xóa"
          variant="danger"
          loading={mutationStatus === 'loading'}
          onConfirm={confirmDelete}
          onCancel={() => setPendingDelete(null)}
        />
      )}
    </AdminLayout>
  );
}