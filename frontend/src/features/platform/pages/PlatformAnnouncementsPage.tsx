import type { FormEvent } from 'react';
import { useState } from 'react';
import { ConfirmDialog } from '../../../shared/components';
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
  CLIENT: 'Hoc vien',
  TUTOR: 'Gia su',
  TUTOR_CENTER: 'Trung tam',
  PLATFORM_ADMIN: 'Quan tri vien',
};

function formatDateTime(value: string | null) {
  if (!value) return 'Tat ca';
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
    <AdminLayout title="Thong bao he thong" subtitle="Tao va quan ly thong bao hien thi cho nguoi dung.">
      <div className="adm-layout-split">
        <div className="adm-card">
          <div className="adm-toolbar">
            <button className="tcs-btn tcs-btn--ghost" type="button" onClick={reload}>
              Lam moi
            </button>
            <button className="tcs-btn tcs-btn--primary" type="button" onClick={resetForm}>
              Tao thong bao moi
            </button>
          </div>

          {status === 'loading' && <div className="adm-state">Dang tai thong bao...</div>}
          {status === 'error' && (
            <div className="adm-state">
              <p>{listErrorMessage ?? 'Khong tai duoc du lieu.'}</p>
              <button className="tcs-btn tcs-btn--primary" type="button" onClick={reload}>
                Thu lai
              </button>
            </div>
          )}

          {status === 'success' && (
            <div className="adm-table-wrap">
              <table className="adm-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Tieu de</th>
                    <th>Doi tuong</th>
                    <th>Trang thai</th>
                    <th>Hieu luc</th>
                    <th>Thao tac</th>
                  </tr>
                </thead>
                <tbody>
                  {items.length === 0 ? (
                    <tr>
                      <td colSpan={6}>Chua co thong bao nao.</td>
                    </tr>
                  ) : (
                    items.map((item) => (
                      <tr key={item.announcementId}>
                        <td>{item.announcementId}</td>
                        <td className="adm-table__notes">{item.title}</td>
                        <td>{item.targetRole ? ROLE_LABELS[item.targetRole] : 'Tat ca'}</td>
                        <td className="adm-table__badge">
                          <span className={item.active ? 'tcs-badge tcs-badge--active' : 'tcs-badge tcs-badge--suspended'}>
                            {item.active ? 'Dang bat' : 'Da tat'}
                          </span>
                        </td>
                        <td>{formatDateTime(item.startsAt)} - {formatDateTime(item.endsAt)}</td>
                        <td className="adm-table__actions">
                          <div className="adm-row-actions">
                            <button className="tcs-btn tcs-btn--ghost tcs-btn--badge" type="button" onClick={() => selectAnnouncement(item)}>
                              Sua
                            </button>
                            <button className="tcs-btn tcs-btn--danger tcs-btn--badge" type="button" onClick={() => setPendingDelete(item)}>
                              Xoa
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
        </div>

        <article className="adm-card adm-card--sticky">
          <div className="adm-card__head">
            <h2 className="adm-card__title">{selected ? `Chinh sua thong bao #${selected.announcementId}` : 'Tao thong bao moi'}</h2>
          </div>

          <form className="adm-form" onSubmit={(event) => void handleSubmit(event)}>
            <div className="adm-field-group">
              <label htmlFor="ann-title">Tieu de</label>
              <input
                id="ann-title"
                className="adm-field"
                value={form.title}
                onChange={(event) => setForm((current) => ({ ...current, title: event.target.value }))}
                maxLength={200}
                required
              />
            </div>

            <div className="adm-field-group">
              <label htmlFor="ann-content">Noi dung</label>
              <textarea
                id="ann-content"
                className="adm-field"
                value={form.content}
                onChange={(event) => setForm((current) => ({ ...current, content: event.target.value }))}
                required
              />
            </div>

            <div className="adm-field-group">
              <label htmlFor="ann-role">Doi tuong hien thi</label>
              <select
                id="ann-role"
                className="adm-field"
                value={form.targetRole}
                onChange={(event) => setForm((current) => ({ ...current, targetRole: event.target.value as FormState['targetRole'] }))}
              >
                <option value="">Tat ca vai tro</option>
                <option value="CLIENT">Hoc vien</option>
                <option value="TUTOR">Gia su</option>
                <option value="TUTOR_CENTER">Trung tam</option>
                <option value="PLATFORM_ADMIN">Quan tri vien</option>
              </select>
            </div>

            <div className="adm-field-group">
              <label htmlFor="ann-starts">Bat dau hien thi</label>
              <input
                id="ann-starts"
                type="datetime-local"
                className="adm-field"
                value={form.startsAt}
                onChange={(event) => setForm((current) => ({ ...current, startsAt: event.target.value }))}
              />
            </div>

            <div className="adm-field-group">
              <label htmlFor="ann-ends">Ket thuc hien thi</label>
              <input
                id="ann-ends"
                type="datetime-local"
                className="adm-field"
                value={form.endsAt}
                onChange={(event) => setForm((current) => ({ ...current, endsAt: event.target.value }))}
              />
            </div>

            <div className="adm-field-group adm-field-group--inline">
              <label htmlFor="ann-active">
                <input
                  id="ann-active"
                  type="checkbox"
                  checked={form.active}
                  onChange={(event) => setForm((current) => ({ ...current, active: event.target.checked }))}
                />
                Kich hoat (hien thi cho nguoi dung)
              </label>
            </div>

            {mutationStatus === 'error' && errorMessage && (
              <div className="adm-alert adm-alert--error">{errorMessage}</div>
            )}

            <div className="adm-form__footer">
              <button className="tcs-btn tcs-btn--primary" type="submit" disabled={mutationStatus === 'loading'}>
                {mutationStatus === 'loading' ? 'Dang luu...' : selected ? 'Cap nhat' : 'Tao thong bao'}
              </button>
              <button className="tcs-btn tcs-btn--ghost" type="button" onClick={resetForm}>
                Xoa form
              </button>
            </div>
          </form>
        </article>
      </div>

      {pendingDelete && (
        <ConfirmDialog
          open
          title="Xoa thong bao"
          message={`Xoa thong bao "${pendingDelete.title}"? Hanh dong nay khong the hoan tac.`}
          confirmLabel="Xoa"
          variant="danger"
          loading={mutationStatus === 'loading'}
          onConfirm={confirmDelete}
          onCancel={() => setPendingDelete(null)}
        />
      )}
    </AdminLayout>
  );
}