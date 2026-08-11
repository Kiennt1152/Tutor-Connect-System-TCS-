import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { AdminLayout } from '../components/AdminLayout';
import { platformApi } from '../api/platformApi';
import type {
  NotificationTemplateApiResponse,
  NotificationTemplatePreviewApiResponse,
  UpsertNotificationTemplateApiRequest,
} from '../types/platformTypes';

const EMPTY: UpsertNotificationTemplateApiRequest = {
  code: '', titleTemplate: '', contentTemplate: '', channel: 'IN_APP', description: '', enabled: true,
};

export default function PlatformNotificationTemplatesPage() {
  const [items, setItems] = useState<NotificationTemplateApiResponse[]>([]);
  const [form, setForm] = useState<UpsertNotificationTemplateApiRequest>(EMPTY);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [preview, setPreview] = useState<NotificationTemplatePreviewApiResponse | null>(null);
  const [variables, setVariables] = useState('{}');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    try {
      setError(null);
      setItems((await platformApi.getNotificationTemplates()).data);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không tải được danh sách mẫu thông báo.'));
    }
  }, []);

  useEffect(() => {
    const timerId = window.setTimeout(() => void reload(), 0);
    return () => window.clearTimeout(timerId);
  }, [reload]);

  function select(item: NotificationTemplateApiResponse) {
    setSelectedId(item.templateId);
    setForm({
      code: item.code,
      titleTemplate: item.titleTemplate,
      contentTemplate: item.contentTemplate,
      channel: item.channel,
      description: item.description ?? '',
      enabled: item.enabled,
    });
    setPreview(null);
    setError(null);
  }

  function clear() {
    setSelectedId(null);
    setForm(EMPTY);
    setPreview(null);
    setError(null);
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (selectedId) await platformApi.updateNotificationTemplate(selectedId, form);
      else await platformApi.createNotificationTemplate(form);
      clear();
      await reload();
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Không lưu được mẫu thông báo.'));
    } finally { setBusy(false); }
  }

  async function showPreview() {
    setBusy(true);
    setError(null);
    try {
      const parsed = variables.trim() ? JSON.parse(variables) as Record<string, string> : {};
      setPreview((await platformApi.previewNotificationTemplate({
        titleTemplate: form.titleTemplate, contentTemplate: form.contentTemplate, variables: parsed,
      })).data);
    } catch (requestError) {
      setError(requestError instanceof SyntaxError ? 'Dữ liệu biến phải là JSON hợp lệ.' : getApiErrorMessage(requestError, 'Không xem trước được template.'));
    } finally { setBusy(false); }
  }

  async function disable(item: NotificationTemplateApiResponse) {
    if (!item.enabled || !window.confirm(`Tắt mẫu ${item.code}?`)) return;
    setBusy(true);
    try { await platformApi.disableNotificationTemplate(item.templateId); await reload(); }
    catch (requestError) { setError(getApiErrorMessage(requestError, 'Không tắt được mẫu thông báo.')); }
    finally { setBusy(false); }
  }

  return (
    <AdminLayout title="Mẫu thông báo" subtitle="Quản lý nội dung dùng lại cho thông báo trong ứng dụng và email.">
      {error && <div className="adm-alert adm-alert--error">{error}</div>}
      <div className="adm-layout-split">
        <section className="adm-card">
          <div className="adm-toolbar">
            <button className="tcs-btn tcs-btn--ghost" type="button" onClick={() => void reload()}>Làm mới</button>
            <button className="tcs-btn tcs-btn--primary" type="button" onClick={clear}>Tạo mẫu</button>
          </div>
          <div className="adm-table-wrap"><table className="adm-table"><thead><tr>
            <th>Mã</th><th>Kênh</th><th>Biến</th><th>Trạng thái</th><th>Thao tác</th>
          </tr></thead><tbody>
            {items.length === 0 && <tr><td colSpan={5}>Chưa có mẫu thông báo.</td></tr>}
            {items.map((item) => <tr key={item.templateId}>
              <td><strong>{item.code}</strong><div className="adm-table__notes">{item.description}</div></td>
              <td>{item.channel}</td><td>{item.placeholders.map((value) => `{{${value}}}`).join(', ') || 'Không có'}</td>
              <td><span className={item.enabled ? 'tcs-badge tcs-badge--active' : 'tcs-badge tcs-badge--suspended'}>{item.enabled ? 'Đang bật' : 'Đã tắt'}</span></td>
              <td><div className="adm-row-actions"><button className="tcs-btn tcs-btn--ghost tcs-btn--badge" type="button" onClick={() => select(item)}>Sửa</button>
                <button className="tcs-btn tcs-btn--danger tcs-btn--badge" type="button" disabled={!item.enabled || busy} onClick={() => void disable(item)}>Tắt</button></div></td>
            </tr>)}
          </tbody></table></div>
        </section>
        <section className="adm-card adm-card--sticky">
          <div className="adm-card__head"><h2 className="adm-card__title">{selectedId ? 'Chỉnh sửa mẫu' : 'Tạo mẫu mới'}</h2></div>
          <form className="adm-form" onSubmit={(event) => void save(event)}>
            <div className="adm-field-row"><label className="adm-field-group">Mã template<input className="adm-field" required maxLength={50} value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })} /></label>
              <label className="adm-field-group">Kênh<select className="adm-field" value={form.channel} onChange={(e) => setForm({ ...form, channel: e.target.value as 'IN_APP' | 'EMAIL' })}><option>IN_APP</option><option>EMAIL</option></select></label></div>
            <label className="adm-field-group">Mô tả<input className="adm-field" maxLength={500} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></label>
            <label className="adm-field-group">Tiêu đề<input className="adm-field" required maxLength={200} value={form.titleTemplate} onChange={(e) => setForm({ ...form, titleTemplate: e.target.value })} /></label>
            <label className="adm-field-group">Nội dung<textarea className="adm-field adm-field--tall" required value={form.contentTemplate} onChange={(e) => setForm({ ...form, contentTemplate: e.target.value })} /></label>
            <label className="adm-field-group">Biến xem trước (JSON)<textarea className="adm-field" value={variables} onChange={(e) => setVariables(e.target.value)} placeholder={'{"userName":"Nguyễn An"}'} /></label>
            <label className="adm-field-group adm-field-group--inline"><input type="checkbox" checked={form.enabled} onChange={(e) => setForm({ ...form, enabled: e.target.checked })} /> Kích hoạt</label>
            {preview && <div className="adm-alert"><strong>{preview.title}</strong><p>{preview.content}</p>{preview.unresolvedPlaceholders.length > 0 && <small>Chưa có giá trị: {preview.unresolvedPlaceholders.join(', ')}</small>}</div>}
            <div className="adm-form__footer"><button className="tcs-btn tcs-btn--primary" disabled={busy} type="submit">Lưu</button><button className="tcs-btn tcs-btn--ghost" disabled={busy || !form.titleTemplate || !form.contentTemplate} type="button" onClick={() => void showPreview()}>Xem trước</button></div>
          </form>
        </section>
      </div>
    </AdminLayout>
  );
}
