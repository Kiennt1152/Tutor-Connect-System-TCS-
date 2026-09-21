/**
 * ====================================================================================================
 * [UC-23] MÀN HÌNH KHIẾU NẠI & TRANH CHẤP CỦA TÔI (MY DISPUTES PAGE)
 * ====================================================================================================
 * Nghiệp vụ chính:
 * 1. Hiển thị danh sách các khiếu nại tranh chấp hợp đồng do người dùng tạo hoặc liên quan.
 * 2. Theo dõi tiến độ phân xử, xem giải trình của đối phương và tải thêm chứng cứ.
 * 3. Nhận thông báo phán quyết và cập nhật số tiền hoàn/giải ngân thực tế.
 * * @author Nguyễn Tiến Anh (tienanh6677)
 */
import { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { FileThumbnail } from '../../../shared/components/FileThumbnail';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { disputeApi } from '../api/disputeApi';
import type { EvidenceUploadResponse, ParticipantDispute } from '../types/disputeTypes';
import './MyDisputesPage.css';

const labels = { OPEN: 'Mới gửi', UNDER_INVESTIGATION: 'Đang xem xét', WAITING: 'Chờ bổ sung', RESOLVED: 'Đã kết thúc' };
const errorText = (error: unknown) => axios.isAxiosError(error)
  ? error.response?.data?.message ?? 'Không thực hiện được. Vui lòng thử lại.'
  : 'Không thực hiện được. Vui lòng thử lại.';
const dateText = (text: string) => new Date(text).toLocaleString('vi-VN');

export default function MyDisputesPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const classId = (location.state as { classId?: number } | null)?.classId;
  const [items, setItems] = useState<ParticipantDispute[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    try { setItems(await disputeApi.mine(classId)); }
    catch (err) { setError(errorText(err)); }
    finally { setLoading(false); }
  }, [classId]);
  useEffect(() => { void reload(); }, [reload]);
  const selected = items.find(i => i.disputeId === selectedId) ?? items[0];
  const updated = (item: ParticipantDispute, message: string) => {
    setItems(current => current.map(i => i.disputeId === item.disputeId ? item : i));
    setNotice(message);
  };
  const goBack = () => {
    if (window.history.length > 1 && location.key !== 'default') {
      navigate(-1);
      return;
    }
    navigate(APP_ROUTES.contract);
  };
  return <>
    <HomeNavbar />
    <main className="my-disputes">
      <button className="my-disputes__back" type="button" onClick={goBack}>
        ← Quay lại
      </button>
      <header className="my-disputes__heading">
        <h1>Tranh chấp của tôi</h1>
        <button className="tcs-btn tcs-btn--ghost" onClick={() => void reload()} disabled={loading}>Làm mới</button>
      </header>
      {error && <p className="my-disputes__error" role="alert">{error}</p>}
      {notice && <p className="my-disputes__notice" role="status">{notice}</p>}
      {loading ? <p>Đang tải tranh chấp...</p> : items.length === 0 ? <p>Chưa có tranh chấp nào.</p> :
        <div className="my-disputes__layout">
          <nav className="my-disputes__list" aria-label="Danh sách tranh chấp">
            {items.map(item => <button key={item.disputeId} type="button"
              className={`my-disputes__item ${selected?.disputeId === item.disputeId ? 'is-selected' : ''}`}
              aria-current={selected?.disputeId === item.disputeId ? 'true' : undefined}
              onClick={() => { setSelectedId(item.disputeId); setNotice(''); }}>
              <strong>{item.classTitle}</strong>
              <span>{labels[item.status]}</span>
              <small>{dateText(item.createdAt)}</small>
            </button>)}
          </nav>
          {selected && <DisputeDetail key={selected.disputeId} item={selected} onUpdated={updated} />}
        </div>}
    </main>
  </>;
}

function DisputeDetail({ item, onUpdated }: {
  item: ParticipantDispute;
  onUpdated: (item: ParticipantDispute, message: string) => void;
}) {
  const [note, setNote] = useState('');
  const [files, setFiles] = useState<EvidenceUploadResponse[]>([]);
  const [busy, setBusy] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [withdrawOpen, setWithdrawOpen] = useState(false);
  const [reason, setReason] = useState('');

  const upload = async (selected: FileList | null) => {
    if (!selected?.length) return;
    setError('');
    const pending = Array.from(selected);
    if (files.length + pending.length > 5) { setError('Mỗi lần gửi tối đa 5 ảnh.'); return; }
    if (pending.some(f => !['image/jpeg', 'image/png', 'image/webp'].includes(f.type) || f.size > 10 * 1024 * 1024)) {
      setError('Chọn ảnh JPG, PNG hoặc WEBP, tối đa 10MB/ảnh.'); return;
    }
    setUploading(true);
    try {
      for (const file of pending) {
        const saved = await disputeApi.uploadEvidenceImage(file);
        setFiles(current => [...current, saved]);
      }
    } catch (err) { setError(errorText(err)); }
    finally { setUploading(false); }
  };

  const submit = async (withdraw = false) => {
    const text = (withdraw ? reason : note).trim();
    if (text.length < 10 || text.length > 4000) { setError('Nội dung cần từ 10 đến 4.000 ký tự.'); return; }
    setBusy(true);
    setError('');
    try {
      const result = withdraw
        ? await disputeApi.withdraw(item.disputeId, text)
        : await disputeApi.explain(item.disputeId, text, files.map(f => f.fileUrl));
      onUpdated(result, withdraw ? 'Đã rút tranh chấp.' : 'Đã gửi giải trình.');
      setNote(''); setFiles([]); setReason(''); setWithdrawOpen(false);
    } catch (err) { setError(errorText(err)); }
    finally { setBusy(false); }
  };

  return <article className="my-disputes__detail">
    <h2>{item.classTitle}</h2>
    <p className="my-disputes__status">{labels[item.status]}</p>
    <section><h3>Nội dung báo cáo</h3><p className="my-disputes__text">{item.description}</p></section>
    {item.evidenceUrls.length > 0 && <section><h3>Bằng chứng</h3><div className="my-disputes__files">
      {item.evidenceUrls.map(url => <FileThumbnail key={url} src={url} fileName={url.split('/').pop() ?? 'Ảnh bằng chứng'}
        mimeType={/\.webp$/i.test(url) ? 'image/webp' : /\.png$/i.test(url) ? 'image/png' : 'image/jpeg'}
        fileSize={null} showHoverPreview={false} />)}
    </div></section>}
    {item.updates.length > 0 && <section><h3>Giải trình và cập nhật</h3>
      {item.updates.map((update, index) => <div className="my-disputes__update" key={`${update.createdAt}-${index}`}>
        <strong>{update.author}</strong> <small>{dateText(update.createdAt)}</small>
        <p className="my-disputes__text">{update.note}</p>
      </div>)}
    </section>}
    {item.resolution && <section><h3>Kết quả xử lý</h3><p className="my-disputes__text">{item.resolution}</p></section>}
    {error && <p className="my-disputes__error" role="alert">{error}</p>}
    {item.canRespond && <section>
      <h3>Gửi giải trình</h3>
      <label className="my-disputes__field">Nội dung
        <textarea value={note} onChange={e => setNote(e.target.value)} maxLength={4000} rows={5} disabled={busy} />
      </label>
      <label className="my-disputes__field">Ảnh bổ sung
        <input type="file" accept="image/jpeg,image/png,image/webp" multiple disabled={busy || uploading}
          onChange={e => { void upload(e.target.files); e.target.value = ''; }} />
      </label>
      <div className="my-disputes__files">
        {files.map(file => <FileThumbnail key={file.fileId} src={file.fileUrl} fileId={file.fileId}
          fileName={file.fileName} mimeType={file.mimeType} fileSize={file.fileSize} showHoverPreview={false}
          actions={<button type="button" className="tcs-btn tcs-btn--ghost" disabled={busy || uploading}
            onClick={() => setFiles(current => current.filter(f => f.fileId !== file.fileId))}>Xóa</button>} />)}
      </div>
      <button className="tcs-btn tcs-btn--primary" disabled={busy || uploading} onClick={() => void submit()}>
        {uploading ? 'Đang tải ảnh...' : busy ? 'Đang gửi...' : 'Gửi giải trình'}
      </button>
    </section>}
    {item.canWithdraw && <section>
      {!withdrawOpen ? <button className="tcs-btn tcs-btn--ghost" disabled={busy || uploading} onClick={() => setWithdrawOpen(true)}>Rút tranh chấp</button> : <>
        <label className="my-disputes__field">Lý do rút tranh chấp
          <textarea value={reason} onChange={e => setReason(e.target.value)} maxLength={4000} rows={3} disabled={busy} />
        </label>
        <div className="my-disputes__actions">
          <button className="tcs-btn tcs-btn--ghost" disabled={busy} onClick={() => setWithdrawOpen(false)}>Hủy</button>
          <button className="tcs-btn tcs-btn--primary" disabled={busy || uploading} onClick={() => void submit(true)}>
            {busy ? 'Đang xử lý...' : 'Xác nhận rút tranh chấp'}
          </button>
        </div>
      </>}
    </section>}
    {!item.canWithdraw && item.status !== 'RESOLVED' && item.withdrawalBlockedReason &&
      <p>{item.withdrawalBlockedReason}</p>}
  </article>;
}
