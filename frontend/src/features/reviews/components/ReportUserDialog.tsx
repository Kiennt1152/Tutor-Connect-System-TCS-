import { useState, type FormEvent } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { reportApi, REPORT_CATEGORY_OPTIONS, type ReportCategory } from '../api/reportApi';

export function ReportUserDialog({ userId, displayName, onClose }: { userId: number; displayName: string; onClose: () => void }) {
  const [category, setCategory] = useState<ReportCategory>('OTHER');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (description.trim().length < 10) { setError('Mô tả phải có ít nhất 10 ký tự.'); return; }
    setBusy(true); setError(null);
    try { await reportApi.reportUser(userId, category, description); onClose(); }
    catch (requestError) { setError(getApiErrorMessage(requestError, 'Không gửi được báo cáo.')); }
    finally { setBusy(false); }
  }
  return <div className="msg-modal-overlay" role="presentation" onMouseDown={onClose}>
    <div className="msg-user-search-modal" role="dialog" aria-modal="true" aria-labelledby="report-user-title" onMouseDown={(e) => e.stopPropagation()}>
      <div className="msg-modal__header"><h2 id="report-user-title">Báo cáo {displayName}</h2><button className="msg-modal__close" type="button" aria-label="Đóng" onClick={onClose}>×</button></div>
      <form className="adm-form" style={{ padding: 16 }} onSubmit={(event) => void submit(event)}>
        <label className="adm-field-group">Lý do<select className="adm-field" value={category} onChange={(e) => setCategory(e.target.value as ReportCategory)}>{REPORT_CATEGORY_OPTIONS.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}</select></label>
        <label className="adm-field-group">Mô tả<textarea className="adm-field adm-field--tall" required minLength={10} value={description} onChange={(e) => setDescription(e.target.value)} /></label>
        {error && <div className="adm-alert adm-alert--error">{error}</div>}
        <div className="adm-form__footer"><button className="tcs-btn tcs-btn--danger" disabled={busy} type="submit">Gửi báo cáo</button><button className="tcs-btn tcs-btn--ghost" type="button" onClick={onClose}>Hủy</button></div>
      </form>
    </div>
  </div>;
}
