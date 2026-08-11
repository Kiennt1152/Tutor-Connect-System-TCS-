import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { platformApi } from '../api/platformApi';
import { AdminLayout } from '../components/AdminLayout';
import type { CircumventionEventApiResponse, CircumventionStatus } from '../types/platformTypes';

export default function PlatformCircumventionPage() {
  const [status, setStatus] = useState<CircumventionStatus>('PENDING');
  const [items, setItems] = useState<CircumventionEventApiResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const load = useCallback(async () => {
    try { setItems((await platformApi.getCircumventionEvents(status)).data.content); setError(null); }
    catch (requestError) { setError(getApiErrorMessage(requestError, 'Không tải được sự kiện phát hiện.')); }
  }, [status]);
  useEffect(() => { const id = window.setTimeout(() => void load(), 0); return () => window.clearTimeout(id); }, [load]);
  async function review(item: CircumventionEventApiResponse, decision: 'CONFIRMED' | 'DISMISSED') {
    const note = window.prompt(decision === 'CONFIRMED' ? 'Ghi chú xác nhận' : 'Lý do bỏ qua', '') ?? '';
    setBusyId(item.eventId);
    try { await platformApi.reviewCircumventionEvent(item.eventId, decision, note); await load(); }
    catch (requestError) { setError(getApiErrorMessage(requestError, 'Không cập nhật được sự kiện.')); }
    finally { setBusyId(null); }
  }
  return <AdminLayout title="Phát hiện né nền tảng" subtitle="Duyệt dấu hiệu chia sẻ thông tin liên hệ trong chat trước khi áp dụng hình phạt.">
    <div className="adm-card"><div className="adm-toolbar"><select className="adm-field" value={status} onChange={(e) => setStatus(e.target.value as CircumventionStatus)}><option value="PENDING">Chờ duyệt</option><option value="CONFIRMED">Đã xác nhận</option><option value="DISMISSED">Đã bỏ qua</option></select><button className="tcs-btn tcs-btn--ghost" type="button" onClick={() => void load()}>Làm mới</button></div>
      {error && <div className="adm-alert adm-alert--error">{error}</div>}
      <div className="adm-table-wrap"><table className="adm-table"><thead><tr><th>Người gửi</th><th>Quy tắc</th><th>Bằng chứng</th><th>Rủi ro</th><th>Thời gian</th><th>Thao tác</th></tr></thead><tbody>
        {items.length === 0 && <tr><td colSpan={6}>Không có sự kiện.</td></tr>}
        {items.map((item) => <tr key={item.eventId}><td>{item.senderEmail}<div><Link to={`${APP_ROUTES.messaging}?conv=${item.conversationId}`}>Mở hội thoại</Link></div></td><td>{item.matchedRule}</td><td>{item.evidence}</td><td>{item.riskScore}/100</td><td>{new Date(item.createdAt).toLocaleString('vi-VN')}</td><td>{item.status === 'PENDING' ? <div className="adm-row-actions"><button className="tcs-btn tcs-btn--primary tcs-btn--badge" disabled={busyId === item.eventId} type="button" onClick={() => void review(item, 'CONFIRMED')}>Xác nhận</button><button className="tcs-btn tcs-btn--ghost tcs-btn--badge" disabled={busyId === item.eventId} type="button" onClick={() => void review(item, 'DISMISSED')}>Bỏ qua</button></div> : item.reviewNote ?? '—'}</td></tr>)}
      </tbody></table></div>
    </div>
  </AdminLayout>;
}
