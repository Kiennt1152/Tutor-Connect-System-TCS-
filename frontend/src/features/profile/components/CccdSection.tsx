import { useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { profileApi } from '../api/profileApi';
import type { CccdInfo } from '../types/profileTypes';
import '../pages/ProfilePage.css';

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

// Style cho trường định danh chỉ-đọc (điền tự động từ QR).
const RO: React.CSSProperties = { background: '#f1f5f9', color: '#334155', cursor: 'not-allowed' };
const ID_TAG: React.CSSProperties = {
  fontSize: 11,
  fontWeight: 600,
  color: '#0d9488',
  marginLeft: 6,
};

const EMPTY: CccdInfo = {
  fullName: '',
  cccdNumber: '',
  dateOfBirth: '',
  gender: '',
  permanentAddress: '',
  issueDate: '',
  issuePlace: '',
  workplace: '',
  tempResidence: '',
  phone: '',
};

type CccdSectionProps = {
  /** Tiêu đề tuỳ ngữ cảnh (mặc định: dùng khi ký hợp đồng). */
  title?: string;
  /** Gọi sau khi lưu CCCD thành công (VD: để trang cha nạp lại hồ sơ). */
  onSaved?: () => void;
};

/** Thông tin CCCD (đọc QR tự điền + user xác nhận) — dùng cho khối BÊN B khi ký hợp đồng. */
export function CccdSection({
  title = 'Thông tin CCCD (dùng khi ký hợp đồng)',
  onSaved,
}: CccdSectionProps = {}) {
  const [info, setInfo] = useState<CccdInfo>(EMPTY);
  const [complete, setComplete] = useState(false);
  const [scanning, setScanning] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [msg, setMsg] = useState('');
  const [err, setErr] = useState('');
  const fileRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    profileApi
      .getMyCccd()
      .then((res) => {
        setInfo({ ...EMPTY, ...res.data });
        setComplete(Boolean(res.data.complete));
        setSaved(Boolean(res.data.cccdNumber)); // đã có dữ liệu lưu trước đó
      })
      .catch(() => {
        /* chưa có thông tin -> để trống */
      });
  }, []);

  const onPickFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (fileRef.current) fileRef.current.value = ''; // cho phép chọn lại cùng file
    if (!file) return;
    setScanning(true);
    setErr('');
    setMsg('');
    try {
      const parsed = await profileApi.scanCccd(file);
      // Chỉ đè các trường định danh đọc từ QR; giữ nơi công tác/tạm trú/ĐT user đã nhập.
      setInfo((prev) => ({
        ...prev,
        fullName: parsed.fullName ?? prev.fullName,
        cccdNumber: parsed.cccdNumber ?? prev.cccdNumber,
        dateOfBirth: parsed.dateOfBirth ?? prev.dateOfBirth,
        gender: parsed.gender ?? prev.gender,
        permanentAddress: parsed.permanentAddress ?? prev.permanentAddress,
        issueDate: parsed.issueDate ?? prev.issueDate,
        issuePlace: parsed.issuePlace ?? prev.issuePlace,
      }));
      setSaved(false); // có dữ liệu mới -> cần lưu lại
      setMsg('Đã đọc mã QR CCCD. Vui lòng kiểm tra lại thông tin rồi bấm Lưu.');
    } catch (error) {
      setErr(extractError(error, 'Không đọc được mã QR trên ảnh CCCD.'));
    } finally {
      setScanning(false);
    }
  };

  const save = async () => {
    // Bắt buộc phải có dữ liệu định danh từ QR (không cho lưu khi chưa quét được).
    if (!info.cccdNumber || !info.fullName) {
      setErr('Chưa có thông tin định danh. Vui lòng bấm "Đọc ảnh CCCD tự điền" (chụp lại rõ nếu QR mờ).');
      setMsg('');
      return;
    }
    setSaving(true);
    setErr('');
    setMsg('');
    try {
      const res = await profileApi.saveMyCccd(info);
      setInfo({ ...EMPTY, ...res.data });
      setComplete(Boolean(res.data.complete));
      setSaved(true);
      setMsg('Đã lưu thông tin CCCD.');
      onSaved?.(); // báo trang cha nạp lại hồ sơ (ngày sinh + giới tính đồng bộ từ CCCD).
    } catch (error) {
      setErr(extractError(error, 'Không lưu được thông tin CCCD.'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="profile-section">
      <h2>
        {title}{' '}
        <span
          style={{
            fontSize: 13,
            fontWeight: 600,
            padding: '2px 10px',
            borderRadius: 999,
            marginLeft: 8,
            background: complete ? '#dcfce7' : '#fef9c3',
            color: complete ? '#166534' : '#854d0e',
          }}
        >
          {complete ? 'Đã đủ' : 'Chưa đủ'}
        </span>
      </h2>
      <p className="profile-hint">
        Tải ảnh <strong>mặt trước CCCD gắn chip</strong> để hệ thống tự đọc mã QR và điền thông tin
        định danh. Các trường định danh <strong>chỉ điền tự động từ QR</strong> (không nhập tay để
        tránh gian lận) — nếu sai/thiếu, hãy <strong>chụp lại rõ hơn</strong>. Cần điền đủ mới ký được
        hợp đồng.
      </p>

      {err && <div className="profile-alert error">{err}</div>}
      {msg && <div className="profile-alert success">{msg}</div>}

      <div style={{ margin: '8px 0 16px' }}>
        <input
          ref={fileRef}
          type="file"
          accept="image/*"
          style={{ display: 'none' }}
          onChange={onPickFile}
        />
        <button
          type="button"
          className="btn-secondary"
          onClick={() => fileRef.current?.click()}
          disabled={scanning}
        >
          {scanning ? 'Đang đọc QR…' : '📷 Đọc ảnh CCCD tự điền'}
        </button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        {/* --- Trường định danh: chỉ đọc, điền tự động từ QR --- */}
        <label>
          Họ và tên <span style={ID_TAG}>tự động</span>
          <input value={info.fullName ?? ''} readOnly style={RO} placeholder="Đọc từ QR CCCD" />
        </label>
        <label>
          Số CCCD <span style={ID_TAG}>tự động</span>
          <input value={info.cccdNumber ?? ''} readOnly style={RO} placeholder="Đọc từ QR CCCD" />
        </label>
        <label>
          Ngày sinh <span style={ID_TAG}>tự động</span>
          <input value={info.dateOfBirth ?? ''} readOnly style={RO} placeholder="dd/MM/yyyy" />
        </label>
        <label>
          Giới tính <span style={ID_TAG}>tự động</span>
          <input value={info.gender ?? ''} readOnly style={RO} placeholder="Đọc từ QR CCCD" />
        </label>
        <label>
          Ngày cấp <span style={ID_TAG}>tự động</span>
          <input value={info.issueDate ?? ''} readOnly style={RO} placeholder="dd/MM/yyyy" />
        </label>
        <label>
          Nơi cấp <span style={ID_TAG}>tự động</span>
          <input value={info.issuePlace ?? ''} readOnly style={RO} placeholder="Đọc từ QR CCCD" />
        </label>
        <label style={{ gridColumn: '1 / -1' }}>
          Địa chỉ thường trú <span style={ID_TAG}>tự động</span>
          <input value={info.permanentAddress ?? ''} readOnly style={RO} placeholder="Đọc từ QR CCCD" />
        </label>
      </div>

      <div className="profile-actions" style={{ marginTop: 16 }}>
        <button
          type="button"
          className="btn-primary"
          onClick={save}
          disabled={saving || scanning || saved}
        >
          {saving ? 'Đang lưu…' : saved ? '✓ Đã lưu thông tin CCCD' : 'Lưu thông tin CCCD'}
        </button>
      </div>
    </section>
  );
}
