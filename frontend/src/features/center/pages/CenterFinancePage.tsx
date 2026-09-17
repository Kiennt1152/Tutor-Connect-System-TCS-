import { useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { VerificationHeader } from '../../../shared/components/VerificationHeader';
import { CenterSidebar } from '../components/CenterSidebar';
import { centerApi } from '../api/centerApi';
import { downloadBlob } from '../../../shared/utils/download';
import type { CenterFinanceReport } from '../types/centerTypes';
import './CenterPage.css';

const C_IN = '#15803d';
const C_OUT = '#b91c1c';
const C_HOLD = '#b45309';

const STATUS_LABELS: Record<string, string> = {
  DRAFT: 'Nháp',
  OPEN: 'Đang mở',
  MATCHED: 'Đã ghép',
  ENROLLMENT_CLOSED: 'Đóng ghi danh',
  IN_PROGRESS: 'Đang diễn ra',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
  DISPUTED: 'Tranh chấp',
};

const money = (v: number | null | undefined) =>
  `${Math.round(v ?? 0).toLocaleString('vi-VN')} đ`;

/** yyyy-MM -> "Th03/2026" cho dễ đọc trên trục biểu đồ. */
function monthLabel(key: string): string {
  const [y, m] = key.split('-');
  return `Th${m}/${y}`;
}

function Card({
  label,
  value,
  hint,
  color,
}: {
  readonly label: string;
  readonly value: string;
  readonly hint?: string;
  readonly color?: string;
}) {
  return (
    <div
      style={{
        flex: '1 1 160px',
        background: '#fff',
        border: '1px solid var(--color-border, #e5e7eb)',
        borderRadius: 12,
        padding: '12px 14px',
      }}
    >
      <div style={{ fontSize: 12, color: '#6b7280', fontWeight: 600 }}>{label}</div>
      <div
        style={{
          fontSize: 20,
          fontWeight: 800,
          color: color ?? '#111827',
          fontVariantNumeric: 'tabular-nums',
        }}
      >
        {value}
      </div>
      {hint && <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 2 }}>{hint}</div>}
    </div>
  );
}

/** Cột đôi cho một tháng: tiền thu và tiền đã giải ngân, chia tỉ lệ theo tháng cao nhất. */
function MonthBar({ gross, released, max }: { gross: number; released: number; max: number }) {
  const pct = (n: number) => (max <= 0 ? 0 : Math.max(2, (n / max) * 100));
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', gap: 3, height: 72 }}>
      <div
        title={`Đã thu ${money(gross)}`}
        style={{ width: 14, height: `${pct(gross)}%`, background: C_IN, borderRadius: '3px 3px 0 0' }}
      />
      <div
        title={`Đã giải ngân ${money(released)}`}
        style={{ width: 14, height: `${pct(released)}%`, background: '#60a5fa', borderRadius: '3px 3px 0 0' }}
      />
    </div>
  );
}

export default function CenterFinancePage() {
  const [data, setData] = useState<CenterFinanceReport | null>(null);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [error, setError] = useState('');
  // Ngày đang gõ trong ô lọc; chỉ áp dụng khi bấm "Áp dụng".
  const [fromDraft, setFromDraft] = useState('');
  const [toDraft, setToDraft] = useState('');
  const [applied, setApplied] = useState<{ from: string; to: string }>({ from: '', to: '' });

  useEffect(() => {
    setStatus('loading');
    centerApi
      .getFinanceReport(applied.from || undefined, applied.to || undefined)
      .then((res) => {
        setData(res.data);
        setStatus('success');
      })
      .catch((err) => {
        setError(
          axios.isAxiosError(err) && typeof err.response?.data?.message === 'string'
            ? err.response.data.message
            : 'Không tải được báo cáo tài chính.',
        );
        setStatus('error');
      });
  }, [applied]);

  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState('');

  const handleExport = async () => {
    setExportError('');
    setExporting(true);
    try {
      const { blob, filename } = await centerApi.exportFinanceReport(
        applied.from || undefined,
        applied.to || undefined,
      );
      downloadBlob(blob, filename);
    } catch (err) {
      setExportError(
        axios.isAxiosError(err) && typeof err.response?.data?.message === 'string'
          ? err.response.data.message
          : 'Không xuất được báo cáo tài chính.',
      );
    } finally {
      setExporting(false);
    }
  };

  const maxMonth = useMemo(
    () =>
      (data?.months ?? []).reduce((m, r) => Math.max(m, r.gross, r.released), 0),
    [data],
  );

  const th: React.CSSProperties = {
    textAlign: 'left',
    padding: '8px 10px',
    fontSize: 12,
    color: '#6b7280',
    borderBottom: '1px solid #e5e7eb',
    whiteSpace: 'nowrap',
  };
  const td: React.CSSProperties = {
    padding: '8px 10px',
    fontSize: 13,
    borderBottom: '1px solid #f1f5f9',
  };
  const tdNum: React.CSSProperties = { ...td, textAlign: 'right', fontVariantNumeric: 'tabular-nums' };

  const s = data?.summary;

  return (
    <>
      <VerificationHeader />
      <div className="cc-area-bg">
        <div className="cc-shell">
          <CenterSidebar />
          <div className="cc-shell__main">
            <div style={{ width: '100%' }}>
              <h1 style={{ fontSize: 24, fontWeight: 800, margin: '0 0 4px' }}>Tài chính</h1>
              <p style={{ color: '#6b7280', margin: '0 0 16px', fontSize: 14 }}>
                Học phí học viên đóng được giữ ở ký quỹ, giải ngân về ví khi lớp hoàn tất, phí nền
                tảng trừ ngay lúc giải ngân.
              </p>

              {/* Bộ lọc thời gian */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'flex-end',
                  gap: 12,
                  flexWrap: 'wrap',
                  marginBottom: 16,
                }}
              >
                <label className="cc-field" style={{ maxWidth: 170 }}>
                  <span className="cc-label">Từ ngày</span>
                  <input
                    className="cc-input"
                    type="date"
                    value={fromDraft}
                    max={toDraft || undefined}
                    onChange={(e) => setFromDraft(e.target.value)}
                  />
                </label>
                <label className="cc-field" style={{ maxWidth: 170 }}>
                  <span className="cc-label">Đến ngày</span>
                  <input
                    className="cc-input"
                    type="date"
                    value={toDraft}
                    min={fromDraft || undefined}
                    onChange={(e) => setToDraft(e.target.value)}
                  />
                </label>
                <button
                  className="cc-btn cc-btn--primary"
                  type="button"
                  onClick={() => setApplied({ from: fromDraft, to: toDraft })}
                >
                  Áp dụng
                </button>
                {(applied.from || applied.to) && (
                  <button
                    className="cc-btn cc-btn--ghost"
                    type="button"
                    onClick={() => {
                      setFromDraft('');
                      setToDraft('');
                      setApplied({ from: '', to: '' });
                    }}
                  >
                    Xoá lọc
                  </button>
                )}
                {/* UC-43: xuất đúng kỳ đang xem, không phải kỳ đang gõ dở trong ô lọc. */}
                <button
                  className="cc-btn cc-btn--ghost"
                  type="button"
                  disabled={status !== 'success' || exporting}
                  onClick={handleExport}
                  style={{ marginLeft: 'auto' }}
                >
                  {exporting ? 'Đang xuất…' : '⤓ Xuất Excel'}
                </button>
              </div>

              {exportError && <div className="cc-alert cc-alert--error">{exportError}</div>}
              {status === 'loading' && <div className="cc-card cc-state">Đang tải…</div>}
              {status === 'error' && <div className="cc-alert cc-alert--error">{error}</div>}

              {status === 'success' && s && (
                <>
                  <p style={{ color: '#6b7280', fontSize: 13, margin: '0 0 8px' }}>
                    Kỳ báo cáo: <b>{s.from}</b> → <b>{s.to}</b>
                  </p>

                  <h2 style={{ fontSize: 16, fontWeight: 700, margin: '10px 0 8px' }}>
                    Phát sinh trong kỳ
                  </h2>
                  <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 8 }}>
                    <Card label="Học viên đã đóng" value={money(s.grossCollected)} color={C_IN} />
                    <Card label="Đã giải ngân về ví" value={money(s.releasedGross)} hint="trước phí" />
                    <Card label="Phí nền tảng" value={`− ${money(s.platformFee)}`} color={C_OUT} />
                    <Card
                      label="Thực nhận"
                      value={money(s.netReceived)}
                      hint="giải ngân trừ phí"
                      color={C_IN}
                    />
                    <Card label="Đã hoàn học viên" value={money(s.refunded)} color={C_OUT} />
                    <Card label="Đã rút khỏi ví" value={money(s.withdrawn)} />
                  </div>

                  <h2 style={{ fontSize: 16, fontWeight: 700, margin: '18px 0 8px' }}>
                    Số dư hiện tại
                  </h2>
                  <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
                    <Card
                      label="Đang giữ ở ký quỹ"
                      value={money(s.heldInEscrow)}
                      hint="chưa giải ngân"
                      color={C_HOLD}
                    />
                    <Card label="Số dư khả dụng" value={money(s.availableBalance)} />
                    <Card label="Số dư đang khoá" value={money(s.frozenBalance)} color={C_HOLD} />
                  </div>

                  {/* Biểu đồ theo tháng */}
                  <h2 style={{ fontSize: 16, fontWeight: 700, margin: '22px 0 8px' }}>
                    Theo tháng
                  </h2>
                  {data.months.length === 0 ? (
                    <div className="cc-card cc-state">Chưa có phát sinh nào trong kỳ này.</div>
                  ) : (
                    <div
                      style={{
                        background: '#fff',
                        border: '1px solid #e5e7eb',
                        borderRadius: 12,
                        padding: '14px 16px',
                        overflowX: 'auto',
                      }}
                    >
                      <div style={{ display: 'flex', gap: 10, marginBottom: 10, fontSize: 12 }}>
                        <span style={{ color: C_IN, fontWeight: 600 }}>■ Đã thu</span>
                        <span style={{ color: '#1d4ed8', fontWeight: 600 }}>■ Đã giải ngân</span>
                      </div>
                      <div style={{ display: 'flex', gap: 18, alignItems: 'flex-end' }}>
                        {data.months.map((m) => (
                          <div key={m.month} style={{ textAlign: 'center' }}>
                            <MonthBar gross={m.gross} released={m.released} max={maxMonth} />
                            <div style={{ fontSize: 11, color: '#6b7280', marginTop: 6 }}>
                              {monthLabel(m.month)}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Bảng theo lớp */}
                  <h2 style={{ fontSize: 16, fontWeight: 700, margin: '22px 0 8px' }}>Theo lớp</h2>
                  <div
                    style={{
                      overflowX: 'auto',
                      background: '#fff',
                      border: '1px solid #e5e7eb',
                      borderRadius: 12,
                    }}
                  >
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                      <thead>
                        <tr>
                          <th style={th}>Lớp</th>
                          <th style={th}>Môn</th>
                          <th style={th}>Trạng thái</th>
                          <th style={{ ...th, textAlign: 'right' }}>Học phí</th>
                          <th style={{ ...th, textAlign: 'center' }}>HV đã đóng</th>
                          <th style={{ ...th, textAlign: 'right', color: C_IN }}>Đã thu</th>
                          <th style={{ ...th, textAlign: 'right', color: C_HOLD }}>Đang giữ</th>
                          <th style={{ ...th, textAlign: 'right' }}>Đã giải ngân</th>
                          <th style={{ ...th, textAlign: 'right', color: C_OUT }}>Đã hoàn</th>
                        </tr>
                      </thead>
                      <tbody>
                        {data.classes.length === 0 ? (
                          <tr>
                            <td style={td} colSpan={9}>
                              Chưa có lớp nào phát sinh học phí.
                            </td>
                          </tr>
                        ) : (
                          data.classes.map((c) => (
                            <tr key={c.classId}>
                              <td style={td}>{c.title}</td>
                              <td style={td}>{c.subjectName ?? '—'}</td>
                              <td style={td}>
                                {c.status ? (STATUS_LABELS[c.status] ?? c.status) : '—'}
                              </td>
                              <td style={tdNum}>{money(c.tuitionFee)}</td>
                              <td style={{ ...td, textAlign: 'center' }}>{c.paidStudents}</td>
                              <td style={tdNum}>{money(c.gross)}</td>
                              <td style={tdNum}>{money(c.held)}</td>
                              <td style={tdNum}>{money(c.released)}</td>
                              <td style={tdNum}>{money(c.refunded)}</td>
                            </tr>
                          ))
                        )}
                      </tbody>
                    </table>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
