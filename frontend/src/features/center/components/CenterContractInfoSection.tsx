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
    <section className="contract-card" style={{ marginBottom: 'var(--space-lg)' }}>
      <div className="contract-card__head">
        <h2>Thông tin trung tâm trên hợp đồng (BÊN A)</h2>
      </div>
      <div className="cct-card-body">
        <p className="cct-hint" style={{ marginBottom: 'var(--space-md)' }}>
          Tên, địa chỉ, SĐT, email lấy từ hồ sơ trung tâm. <strong>Người đại diện</strong> lấy từ
          CCCD người đại diện pháp luật đã xác minh. Bạn chỉ cần bổ sung website + chức vụ để hiển
          thị ở khối BÊN A của mọi hợp đồng.
        </p>

        {err && <p className="cct-alert cct-alert--error">{err}</p>}
        {msg && <p className="cct-alert cct-alert--success">{msg}</p>}

        <div className="cct-grid">
          <div className="cct-field">
            <label className="cct-label">Tên trung tâm</label>
            <input className="cct-input cct-input--ro" value={info?.companyName ?? ''} readOnly />
          </div>
          <div className="cct-field">
            <label className="cct-label">Điện thoại</label>
            <input className="cct-input cct-input--ro" value={info?.phone ?? ''} readOnly />
          </div>
          <div className="cct-field cct-field--full">
            <label className="cct-label">Trụ sở / địa chỉ</label>
            <input className="cct-input cct-input--ro" value={info?.address ?? ''} readOnly />
          </div>
          <div className="cct-field">
            <label className="cct-label">Email</label>
            <input className="cct-input cct-input--ro" value={info?.email ?? ''} readOnly />
          </div>
          <div className="cct-field">
            <label className="cct-label">Website</label>
            <input
              className="cct-input"
              value={website}
              onChange={(e) => setWebsite(e.target.value)}
              placeholder="https://..."
            />
          </div>
          <div className="cct-field">
            <label className="cct-label">Người đại diện</label>
            <input
              className="cct-input cct-input--ro"
              value={info?.representativeName ?? ''}
              readOnly
              placeholder="Lấy từ CCCD người đại diện pháp luật"
            />
          </div>
          <div className="cct-field">
            <label className="cct-label">Chức vụ</label>
            <input
              className="cct-input"
              value={repPos}
              onChange={(e) => setRepPos(e.target.value)}
              placeholder="VD: Giám đốc / Chủ nhiệm"
            />
          </div>
        </div>

        <div className="cct-actions">
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
    </section>
  );
}
