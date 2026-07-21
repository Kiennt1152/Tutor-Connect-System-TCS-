import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { centerApi } from '../api/centerApi';
import type {
  CenterMember,
  MembershipStatus,
  RecruitmentApplicationStatus,
} from '../types/centerTypes';
import './CenterPage.css';

const STATUS_LABELS: Record<MembershipStatus, { label: string; cls: string }> = {
  ACTIVE: { label: 'Đang hoạt động', cls: 'active' },
  INACTIVE: { label: 'Tạm ngưng', cls: 'draft' },
  TERMINATED: { label: 'Đã gỡ', cls: 'closed' },
};

const APP_STATUS_LABELS: Record<RecruitmentApplicationStatus, { label: string; cls: string }> = {
  APPLIED: { label: 'Chờ duyệt', cls: 'pending' },
  SCREENING: { label: 'Đang lọc', cls: 'pending' },
  INTERVIEW: { label: 'Phỏng vấn', cls: 'pending' },
  PASSED: { label: 'Đạt', cls: 'ok' },
  HIRED: { label: 'Đã nhận', cls: 'ok' },
  REJECTED: { label: 'Từ chối', cls: 'no' },
  WITHDRAWN: { label: 'Đã rút', cls: 'no' },
};

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

function fmtDate(value: string | null): string {
  if (!value) return '—';
  const d = new Date(value);
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
}

function initials(name: string | null): string {
  if (!name) return '?';
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p.charAt(0).toUpperCase())
    .join('');
}

export default function CenterTutorsPage() {
  const [members, setMembers] = useState<CenterMember[]>([]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState<number | null>(null);

  const load = useCallback(() => {
    setStatus('loading');
    setError('');
    centerApi
      .getMembers()
      .then((res) => {
        setMembers(res.data);
        setStatus('success');
      })
      .catch((err) => {
        setError(extractError(err, 'Không tải được danh sách gia sư.'));
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const changeStatus = async (member: CenterMember, next: MembershipStatus) => {
    setBusyId(member.membershipId);
    setError('');
    try {
      await centerApi.updateMemberStatus(member.membershipId, next);
      load();
    } catch (err) {
      setError(extractError(err, 'Không cập nhật được trạng thái.'));
    } finally {
      setBusyId(null);
    }
  };

  const activeCount = members.filter((m) => m.status === 'ACTIVE').length;

  return (
    <>
      <HomeNavbar />
      <div className="rc-bg">
        <div className="rc-page">
          <div className="rc-topbar">
            <Link className="rc-back" to="/center/recruitment">
              ← Tin tuyển dụng
            </Link>
          </div>

        <header className="rc-header">
          <div>
            <h1 className="rc-title">Gia sư của trung tâm</h1>
            <p className="rc-subtitle">
              Gia sư được thêm vào đây khi bạn duyệt (nhận) đơn ứng tuyển. Bạn có thể tạm ngưng,
              kích hoạt lại, hoặc gỡ khỏi trung tâm.
            </p>
          </div>
        </header>

        {error && <div className="rc-alert rc-alert--error">{error}</div>}
        {status === 'loading' && <div className="rc-state">Đang tải…</div>}
        {status === 'success' && members.length === 0 && (
          <div className="rc-empty">
            <div className="rc-empty__emoji">🧑‍🏫</div>
            <p>Chưa có gia sư nào. Duyệt đơn ứng tuyển để thêm gia sư vào trung tâm.</p>
          </div>
        )}

        {status === 'success' && members.length > 0 && (
          <>
            <p className="rc-count">
              {members.length} gia sư · {activeCount} đang hoạt động
            </p>
            <ul className="rc-applicants">
              {members.map((m) => {
                const st = STATUS_LABELS[m.status];
                const busy = busyId === m.membershipId;
                return (
                  <li className="rc-applicant" key={m.membershipId}>
                    <div className="rc-applicant__avatar">{initials(m.tutorName)}</div>
                    <div className="rc-applicant__info">
                      <div className="rc-applicant__name">
                        {m.tutorName}
                        {m.verificationStatus === 'VERIFIED' && (
                          <span className="rc-verified">✓ Đã xác minh</span>
                        )}
                        <span className={`rc-status rc-status--${st.cls}`}>{st.label}</span>
                      </div>
                      <div className="rc-applicant__meta">
                        {m.experienceYears != null && <span>{m.experienceYears} năm KN</span>}
                        {m.ratingAvg != null && <span>★ {m.ratingAvg}</span>}
                        {m.tutorPhone && <span>{m.tutorPhone}</span>}
                        <span>Gia nhập: {fmtDate(m.joinedAt)}</span>
                      </div>
                      {m.appliedPosts && m.appliedPosts.length > 0 && (
                        <div className="rc-applied">
                          <span className="rc-applied__label">Đã ứng tuyển:</span>
                          <div className="rc-applied__list">
                            {m.appliedPosts.map((p) => {
                              const ps = APP_STATUS_LABELS[p.applicationStatus];
                              return (
                                <span className="rc-applied__item" key={p.recruitmentId}>
                                  {p.postTitle ?? `Tin #${p.recruitmentId}`}
                                  <span className={`rc-status rc-status--${ps.cls}`}>
                                    {ps.label}
                                  </span>
                                </span>
                              );
                            })}
                          </div>
                        </div>
                      )}
                    </div>
                    <div className="rc-applicant__actions">
                      {m.status === 'ACTIVE' && (
                        <button
                          className="rc-btn rc-btn--ghost rc-btn--sm"
                          type="button"
                          disabled={busy}
                          onClick={() => changeStatus(m, 'INACTIVE')}
                        >
                          Tạm ngưng
                        </button>
                      )}
                      {m.status === 'INACTIVE' && (
                        <button
                          className="rc-btn rc-btn--primary rc-btn--sm"
                          type="button"
                          disabled={busy}
                          onClick={() => changeStatus(m, 'ACTIVE')}
                        >
                          Kích hoạt lại
                        </button>
                      )}
                      {m.status !== 'TERMINATED' && (
                        <button
                          className="rc-btn rc-btn--danger rc-btn--sm"
                          type="button"
                          disabled={busy}
                          onClick={() => changeStatus(m, 'TERMINATED')}
                        >
                          Gỡ
                        </button>
                      )}
                      {m.status === 'TERMINATED' && (
                        <button
                          className="rc-btn rc-btn--ghost rc-btn--sm"
                          type="button"
                          disabled={busy}
                          onClick={() => changeStatus(m, 'ACTIVE')}
                        >
                          Thêm lại
                        </button>
                      )}
                    </div>
                  </li>
                );
              })}
            </ul>
          </>
        )}
        </div>
      </div>
    </>
  );
}
