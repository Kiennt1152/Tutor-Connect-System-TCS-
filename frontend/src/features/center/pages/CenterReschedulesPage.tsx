import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import { VerificationHeader } from '../../../shared/components/VerificationHeader';
import { centerApi } from '../api/centerApi';
import type { Reschedule, RescheduleStatus } from '../types/centerTypes';
import './CenterSchedulePage.css';

const STATUS_LABELS: Record<RescheduleStatus, { label: string; cls: string }> = {
  PENDING: { label: 'Chờ duyệt', cls: 'pending' },
  APPROVED: { label: 'Đã duyệt', cls: 'approved' },
  REJECTED: { label: 'Từ chối', cls: 'rejected' },
};

function fmt(d: string): string {
  const [y, m, day] = d.split('-');
  return `${day}/${m}/${y}`;
}

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

export default function CenterReschedulesPage() {
  const [items, setItems] = useState<Reschedule[]>([]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [error, setError] = useState('');
  const [busyKey, setBusyKey] = useState('');

  const load = useCallback(() => {
    setStatus('loading');
    setError('');
    centerApi
      .getReschedules()
      .then((res) => {
        setItems(res.data);
        setStatus('success');
      })
      .catch((err) => {
        setError(extractError(err, 'Không tải được yêu cầu đổi lịch.'));
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const decide = async (r: Reschedule, approve: boolean) => {
    const key = `${r.classId}:${r.originalDate}`;
    setBusyKey(key);
    setError('');
    try {
      await centerApi.decideReschedule(r.classId, r.originalDate, approve);
      load();
    } catch (err) {
      setError(extractError(err, 'Không xử lý được yêu cầu.'));
    } finally {
      setBusyKey('');
    }
  };

  const pending = items.filter((i) => i.status === 'PENDING');
  const decided = items.filter((i) => i.status !== 'PENDING');

  const renderCard = (r: Reschedule) => {
    const key = `${r.classId}:${r.originalDate}`;
    const st = STATUS_LABELS[r.status];
    return (
      <article className="cs-card" key={key}>
        <div className="cs-card__head">
          <div>
            <h2 className="cs-card__title">{r.className ?? `Lớp #${r.classId}`}</h2>
            <div className="cs-chips">
              {r.tutorName && <span className="cs-chip">👩‍🏫 {r.tutorName}</span>}
              <span className={`cs-attstate cs-attstate--${st.cls === 'approved' ? 'done' : 'pending'}`}>
                {st.label}
              </span>
            </div>
          </div>
          <div className="cs-times">
            <span className="cs-time">
              {fmt(r.originalDate)} → {fmt(r.newDate)}
            </span>
            {r.newStartTime && r.newEndTime && (
              <span className="cs-time">
                🕒 {r.newStartTime}–{r.newEndTime}
              </span>
            )}
          </div>
        </div>
        {r.reason && <p className="cs-muted">Lý do: {r.reason}</p>}
        {r.status === 'PENDING' && (
          <div className="cs-modal__actions">
            <button
              className="cs-btn cs-btn--ghost"
              type="button"
              disabled={busyKey === key}
              onClick={() => decide(r, false)}
            >
              Từ chối
            </button>
            <button
              className="cs-btn cs-btn--primary"
              type="button"
              disabled={busyKey === key}
              onClick={() => decide(r, true)}
            >
              Duyệt
            </button>
          </div>
        )}
      </article>
    );
  };

  return (
    <>
      <VerificationHeader />
      <div className="cs-bg">
        <div className="cs-page">
          <div className="cs-topbar">
            <Link className="cs-back" to="/center">
              ← Lớp học của tôi
            </Link>
          </div>

          <header className="cs-header">
            <div>
              <h1 className="cs-title">Yêu cầu đổi lịch</h1>
              <p className="cs-subtitle">
                Gia sư báo ốm và xin dời buổi dạy sang ngày khác. Duyệt để áp dụng vào lịch lớp.
              </p>
            </div>
          </header>

          {error && <div className="cs-alert cs-alert--error">{error}</div>}
          {status === 'loading' && <div className="cs-state">Đang tải…</div>}
          {status === 'success' && items.length === 0 && (
            <div className="cs-empty">
              <div className="cs-empty__emoji">🗓️</div>
              <p>Chưa có yêu cầu đổi lịch nào.</p>
            </div>
          )}

          {status === 'success' && pending.length > 0 && (
            <>
              <h2 className="cs-section-title">Chờ duyệt ({pending.length})</h2>
              <div className="cs-list">{pending.map(renderCard)}</div>
            </>
          )}

          {status === 'success' && decided.length > 0 && (
            <>
              <h2 className="cs-section-title">Đã xử lý</h2>
              <div className="cs-list">{decided.map(renderCard)}</div>
            </>
          )}
        </div>
      </div>
    </>
  );
}
