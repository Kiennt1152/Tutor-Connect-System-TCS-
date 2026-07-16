import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import { VerificationHeader } from '../../../shared/components/VerificationHeader';
import { centerApi } from '../api/centerApi';
import type { Reschedule, RescheduleStatus, Substitution } from '../types/centerTypes';
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
  const [subs, setSubs] = useState<Substitution[]>([]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [error, setError] = useState('');
  const [busyKey, setBusyKey] = useState('');

  const load = useCallback(() => {
    setStatus('loading');
    setError('');
    Promise.all([centerApi.getReschedules(), centerApi.getSubstitutions()])
      .then(([resched, sub]) => {
        setItems(resched.data);
        setSubs(sub.data);
        setStatus('success');
      })
      .catch((err) => {
        setError(extractError(err, 'Không tải được yêu cầu.'));
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const decide = async (r: Reschedule, approve: boolean) => {
    const key = `r:${r.classId}:${r.originalDate}`;
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

  const decideSub = async (s: Substitution, approve: boolean) => {
    const key = `s:${s.classId}:${s.date}`;
    setBusyKey(key);
    setError('');
    try {
      await centerApi.decideSubstitution(s.classId, s.date, approve);
      load();
    } catch (err) {
      setError(extractError(err, 'Không xử lý được yêu cầu.'));
    } finally {
      setBusyKey('');
    }
  };

  const pending = items.filter((i) => i.status === 'PENDING');
  const decided = items.filter((i) => i.status !== 'PENDING');
  const subPending = subs.filter((i) => i.status === 'PENDING');
  const subDecided = subs.filter((i) => i.status !== 'PENDING');
  const isEmpty = items.length === 0 && subs.length === 0;

  const renderCard = (r: Reschedule) => {
    const key = `r:${r.classId}:${r.originalDate}`;
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

  const renderSubCard = (s: Substitution) => {
    const key = `s:${s.classId}:${s.date}`;
    const st = STATUS_LABELS[s.status];
    return (
      <article className="cs-card" key={key}>
        <div className="cs-card__head">
          <div>
            <h2 className="cs-card__title">{s.className ?? `Lớp #${s.classId}`}</h2>
            <div className="cs-chips">
              {s.mainTutorName && <span className="cs-chip">👩‍🏫 {s.mainTutorName}</span>}
              {s.assistantTutorName && (
                <span className="cs-chip">🔁 Thay: {s.assistantTutorName}</span>
              )}
              <span className={`cs-attstate cs-attstate--${st.cls === 'approved' ? 'done' : 'pending'}`}>
                {st.label}
              </span>
            </div>
          </div>
          <div className="cs-times">
            <span className="cs-time">Buổi {fmt(s.date)}</span>
          </div>
        </div>
        {s.reason && <p className="cs-muted">Lý do: {s.reason}</p>}
        {s.status === 'PENDING' && (
          <div className="cs-modal__actions">
            <button
              className="cs-btn cs-btn--ghost"
              type="button"
              disabled={busyKey === key}
              onClick={() => decideSub(s, false)}
            >
              Từ chối
            </button>
            <button
              className="cs-btn cs-btn--primary"
              type="button"
              disabled={busyKey === key}
              onClick={() => decideSub(s, true)}
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
              <h1 className="cs-title">Yêu cầu đổi lịch & dạy thay</h1>
              <p className="cs-subtitle">
                Gia sư báo bận/ốm: xin dời buổi sang ngày khác, hoặc nhờ gia sư phụ dạy thay. Duyệt
                để áp dụng vào lịch lớp.
              </p>
            </div>
          </header>

          {error && <div className="cs-alert cs-alert--error">{error}</div>}
          {status === 'loading' && <div className="cs-state">Đang tải…</div>}
          {status === 'success' && isEmpty && (
            <div className="cs-empty">
              <div className="cs-empty__emoji">🗓️</div>
              <p>Chưa có yêu cầu nào.</p>
            </div>
          )}

          {status === 'success' && (pending.length > 0 || decided.length > 0) && (
            <h2 className="cs-section-title">Đổi lịch (dạy bù ngày khác)</h2>
          )}
          {status === 'success' && pending.length > 0 && (
            <>
              <h3 className="cs-section-title">Chờ duyệt ({pending.length})</h3>
              <div className="cs-list">{pending.map(renderCard)}</div>
            </>
          )}
          {status === 'success' && decided.length > 0 && (
            <>
              <h3 className="cs-section-title">Đã xử lý</h3>
              <div className="cs-list">{decided.map(renderCard)}</div>
            </>
          )}

          {status === 'success' && (subPending.length > 0 || subDecided.length > 0) && (
            <h2 className="cs-section-title">Nhờ gia sư phụ dạy thay</h2>
          )}
          {status === 'success' && subPending.length > 0 && (
            <>
              <h3 className="cs-section-title">Chờ duyệt ({subPending.length})</h3>
              <div className="cs-list">{subPending.map(renderSubCard)}</div>
            </>
          )}
          {status === 'success' && subDecided.length > 0 && (
            <>
              <h3 className="cs-section-title">Đã xử lý</h3>
              <div className="cs-list">{subDecided.map(renderSubCard)}</div>
            </>
          )}
        </div>
      </div>
    </>
  );
}
