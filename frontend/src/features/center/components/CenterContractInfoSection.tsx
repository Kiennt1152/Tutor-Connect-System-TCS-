import { useEffect, useState } from 'react';
import axios from 'axios';
import { centerApi } from '../api/centerApi';
import type { CenterContractInfo } from '../types/centerTypes';

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

const RO: React.CSSProperties = { background: '#f1f5f9', color: '#334155' };
const LABEL: React.CSSProperties = { display: 'block', fontSize: 13, fontWeight: 600, marginBottom: 4 };
const INPUT: React.CSSProperties = {
  width: '100%',
  padding: 10,
  border: '1px solid #cbd5e1',
  borderRadius: 8,
};

/** Thông tin trung tâm hiển thị ở khối BÊN A của hợp đồng. */
export function CenterContractInfoSection() {
  const [info, setInfo] = useState<CenterContractInfo | null>(null);
  const [website, setWebsite] = useState('');
  const [repPos, setRepPos] = useState('');
  const [saving, setSaving] = useState(false);
  const [msg, setMsg] = useState('');
  const [err, setErr] = useState('');

  useEffect(() => {
    centerApi
      .getContractInfo()
      .then((res) => {
        setInfo(res.data);
        setWebsite(res.data.website ?? '');
        setRepPos(res.data.representativePosition ?? '');
      })
      .catch(() => {
        /* để trống */
      });
  }, []);

  const save = async () => {
    setSaving(true);
    setErr('');
    setMsg('');
    try {
      const res = await centerApi.saveContractInfo({
        website,
        representativePosition: repPos,
      });
      setInfo(res.data);
      setMsg('Đã lưu thông tin trung tâm cho hợp đồng.');
    } catch (error) {
      setErr(extractError(error, 'Không lưu được thông tin.'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ border: '1px solid #e2e8f0', borderRadius: 12, padding: 16, marginBottom: 20 }}>
      <h2 style={{ marginTop: 0, fontSize: 16 }}>Thông tin trung tâm trên hợp đồng (BÊN A)</h2>
      <p style={{ color: '#64748b', fontSize: 13, marginTop: 0 }}>
        Tên, địa chỉ, SĐT, email lấy từ hồ sơ trung tâm. <strong>Người đại diện</strong> lấy từ CCCD
        người đại diện pháp luật đã xác minh. Bạn chỉ cần bổ sung website + chức vụ để hiển thị ở khối
        BÊN A của mọi hợp đồng.
      </p>

      {err && <p style={{ background: '#fee2e2', color: '#991b1b', padding: 10, borderRadius: 8 }}>{err}</p>}
      {msg && <p style={{ background: '#dcfce7', color: '#166534', padding: 10, borderRadius: 8 }}>{msg}</p>}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        <div>
          <label style={LABEL}>Tên trung tâm</label>
          <input style={{ ...INPUT, ...RO }} value={info?.companyName ?? ''} readOnly />
        </div>
        <div>
          <label style={LABEL}>Điện thoại</label>
          <input style={{ ...INPUT, ...RO }} value={info?.phone ?? ''} readOnly />
        </div>
        <div style={{ gridColumn: '1 / -1' }}>
          <label style={LABEL}>Trụ sở / địa chỉ</label>
          <input style={{ ...INPUT, ...RO }} value={info?.address ?? ''} readOnly />
        </div>
        <div>
          <label style={LABEL}>Email</label>
          <input style={{ ...INPUT, ...RO }} value={info?.email ?? ''} readOnly />
        </div>
        <div>
          <label style={LABEL}>Website</label>
          <input
            style={INPUT}
            value={website}
            onChange={(e) => setWebsite(e.target.value)}
            placeholder="https://..."
          />
        </div>
        <div>
          <label style={LABEL}>Người đại diện</label>
          <input
            style={{ ...INPUT, ...RO }}
            value={info?.representativeName ?? ''}
            readOnly
            placeholder="Lấy từ CCCD người đại diện pháp luật"
          />
        </div>
        <div>
          <label style={LABEL}>Chức vụ</label>
          <input
            style={INPUT}
            value={repPos}
            onChange={(e) => setRepPos(e.target.value)}
            placeholder="VD: Giám đốc / Chủ nhiệm"
          />
        </div>
      </div>

      <div style={{ marginTop: 14 }}>
        <button
          type="button"
          className="tcs-btn tcs-btn--market tcs-btn--sm"
          onClick={save}
          disabled={saving}
        >
          {saving ? 'Đang lưu...' : 'Lưu thông tin BÊN A'}
        </button>
      </div>
    </div>
  );
}
