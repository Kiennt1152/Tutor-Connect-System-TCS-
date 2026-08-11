import { useCallback, useEffect, useState } from 'react';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { platformApi } from '../api/platformApi';
import type { AdminEscrowApiResponse, EscrowStatus } from '../types/platformTypes';

export function AdminEscrowQueue({ onSelect }: { onSelect: (item: AdminEscrowApiResponse) => void }) {
  const [items, setItems] = useState<AdminEscrowApiResponse[]>([]);
  const [status, setStatus] = useState<'' | EscrowStatus>('');
  const [keyword, setKeyword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const load = useCallback(async () => {
    try {
      const filters: Record<string, string> = { page: '0', size: '50' };
      if (status) filters.status = status;
      if (keyword.trim()) filters.reference = keyword.trim();
      setItems((await platformApi.getEscrows(filters)).data.content);
      setError(null);
    } catch (requestError) { setError(getApiErrorMessage(requestError, 'Không tải được escrow.')); }
  }, [keyword, status]);
  useEffect(() => { const id = window.setTimeout(() => void load(), 0); return () => window.clearTimeout(id); }, [load]);
  return <section className="adm-card" style={{ marginBottom: 16 }}>
    <div className="adm-toolbar"><input className="adm-field" placeholder="Mã tham chiếu" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
      <select className="adm-field" value={status} onChange={(e) => setStatus(e.target.value as '' | EscrowStatus)}><option value="">Tất cả trạng thái</option>{['PENDING','FUNDED','ON_HOLD','DISPUTED','RELEASED','REFUNDED'].map((value) => <option key={value}>{value}</option>)}</select>
      <button className="tcs-btn tcs-btn--ghost" type="button" onClick={() => void load()}>Làm mới</button></div>
    {error && <div className="adm-alert adm-alert--error">{error}</div>}
    <div className="adm-table-wrap"><table className="adm-table"><thead><tr><th>Escrow</th><th>Tham chiếu</th><th>Người trả</th><th>Người nhận</th><th>Số tiền</th><th>Trạng thái</th><th /></tr></thead><tbody>
      {items.length === 0 && <tr><td colSpan={7}>Không có escrow phù hợp.</td></tr>}
      {items.map((item) => <tr key={item.escrowId}><td>#{item.escrowId}</td><td>{item.referenceCode ?? '—'}</td><td>{item.payerEmail}</td><td>{item.beneficiaryEmail ?? '—'}</td><td>{item.amount.toLocaleString('vi-VN')} VND</td><td>{item.status}</td><td><button className="tcs-btn tcs-btn--ghost tcs-btn--badge" type="button" onClick={() => onSelect(item)}>Xử lý</button></td></tr>)}
    </tbody></table></div>
  </section>;
}
