import { useEffect, useState } from 'react';
import axios from 'axios';
import { marketplaceApi } from '../api/marketplaceApi';
import type { ApplicantResponse } from '../types/marketplaceTypes';
import { TutorDetailModal } from './TutorDetailModal';
import './applicantsModal.css';

const currency = new Intl.NumberFormat('vi-VN');

interface Props {
  readonly classId: number;
  /** Gọi lại sau khi Client chọn gia sư (để trang cha tải lại danh sách lớp). */
  readonly onChosen?: () => void;
}

/** Danh sách gia sư ứng tuyển vào một lớp (dạng panel, không phải popup). */
export function ApplicantsPanel({ classId, onChosen }: Props) {
  const [applicants, setApplicants] = useState<ApplicantResponse[]>([]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [choosingId, setChoosingId] = useState<number | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [detailApplicant, setDetailApplicant] = useState<ApplicantResponse | null>(null);

  useEffect(() => {
    setStatus('loading');
    marketplaceApi
      .listApplicants(classId)
      .then((data) => {
        setApplicants(data);
        setStatus('success');
      })
      .catch(() => setStatus('error'));
  }, [classId]);

  const alreadyChosen = applicants.some((a) => a.status === 'ACCEPTED');
  const recommended = applicants.filter((a) => a.recommended);

  async function handleChoose(applicationId: number) {
    if (!window.confirm('Chọn gia sư này cho lớp? Các ứng viên còn lại sẽ bị từ chối.')) return;
    setChoosingId(applicationId);
    setNotice(null);
    try {
      await marketplaceApi.chooseApplicant(classId, applicationId);
      // Cập nhật tại chỗ: đơn được chọn = ACCEPTED, còn lại REJECTED.
      setApplicants((list) =>
        list.map((a) => ({
          ...a,
          status: a.applicationId === applicationId ? 'ACCEPTED' : 'REJECTED',
        })),
      );
      setNotice('Đã chọn gia sư cho lớp.');
      onChosen?.();
    } catch (err) {
      setNotice(extractError(err));
    } finally {
      setChoosingId(null);
    }
  }

  return (
    <div className="apm-panel">
      {notice && <div className="apm-notice">{notice}</div>}

      {status === 'loading' && <div className="apm-state">Đang tải danh sách ứng viên…</div>}
      {status === 'error' && (
        <div className="apm-state apm-state--error">Không tải được danh sách ứng viên.</div>
      )}
      {status === 'success' && applicants.length === 0 && (
        <div className="apm-state">Chưa có gia sư nào ứng tuyển vào lớp này.</div>
      )}

      {status === 'success' && applicants.length > 0 && (
        <>
          {/* Giải thích AI + Top 5 gợi ý */}
          <div className="apm-ai">
            <div className="apm-ai__badge">AI</div>
            <p className="apm-ai__text">
              Trợ lý AI đã xếp hạng {applicants.length} ứng viên theo{' '}
              <strong>đánh giá, kinh nghiệm, mức phí và trạng thái xác minh</strong>.
              {recommended.length > 0 && (
                <>
                  {' '}
                  <strong>Top {recommended.length}</strong> phù hợp nhất được đánh dấu ⭐ để bạn dễ
                  chọn.
                </>
              )}
            </p>
          </div>

          <div className="apm-list">
            {applicants.map((a, idx) => (
              <ApplicantCard
                key={a.applicationId}
                applicant={a}
                rank={a.recommended ? idx + 1 : null}
                choosing={choosingId === a.applicationId}
                disabled={alreadyChosen || choosingId != null}
                onChoose={() => handleChoose(a.applicationId)}
                onDetail={() => setDetailApplicant(a)}
              />
            ))}
          </div>
        </>
      )}

      {detailApplicant && (
        <TutorDetailModal applicant={detailApplicant} onClose={() => setDetailApplicant(null)} />
      )}
    </div>
  );
}

interface CardProps {
  readonly applicant: ApplicantResponse;
  readonly rank: number | null;
  readonly choosing: boolean;
  readonly disabled: boolean;
  readonly onChoose: () => void;
  readonly onDetail: () => void;
}

function ApplicantCard({ applicant: a, rank, choosing, disabled, onChoose, onDetail }: CardProps) {
  const initials = a.fullName
    .split(/\s+/)
    .slice(-2)
    .map((w) => w[0])
    .join('')
    .toUpperCase();
  const rate = a.proposedRate ?? a.hourlyRate ?? 0;
  const tone = a.matchScore >= 75 ? 'high' : a.matchScore >= 45 ? 'mid' : 'low';
  const accepted = a.status === 'ACCEPTED';
  const rejected = a.status === 'REJECTED';

  return (
    <article className={`apm-card ${a.recommended ? 'is-rec' : ''} ${rejected ? 'is-rejected' : ''}`}>
      {rank != null && <span className="apm-card__rank">⭐ Top {rank}</span>}
      <div className={`apm-card__avatar apm-card__avatar--${tone}`}>{initials || '?'}</div>

      <div className="apm-card__main">
        <div className="apm-card__row">
          <h3 className="apm-card__name">
            {a.fullName}
            {a.verificationStatus === 'VERIFIED' && (
              <span className="apm-badge apm-badge--verified" title="Đã xác minh hồ sơ">
                ✓ Đã xác minh
              </span>
            )}
          </h3>
          <div className={`apm-card__score apm-card__score--${tone}`} title="Điểm AI gợi ý">
            <span className="apm-card__score-num">{a.matchScore}</span>
            <span className="apm-card__score-unit">điểm AI</span>
          </div>
        </div>

        <div className="apm-card__meta">
          <span>⭐ {a.ratingAvg != null ? Number(a.ratingAvg).toFixed(1) : '—'}/5</span>
          <span>🎓 {a.experienceYears ?? 0} năm KN</span>
          <span>💰 {rate > 0 ? `${currency.format(rate)}đ/giờ` : '—'}</span>
        </div>

        {a.bio && <p className="apm-card__bio">{a.bio}</p>}
        {a.coverLetter && <p className="apm-card__cover">“{a.coverLetter}”</p>}

        <div className="apm-card__actions">
          <button type="button" className="mkt-btn mkt-btn--ghost" onClick={onDetail}>
            Xem chi tiết gia sư
          </button>
          {accepted ? (
            <span className="apm-chip apm-chip--accepted">✓ Đã chọn gia sư này</span>
          ) : rejected ? (
            <span className="apm-chip apm-chip--rejected">Không được chọn</span>
          ) : (
            <button
              type="button"
              className="mkt-btn mkt-btn--primary"
              disabled={disabled}
              onClick={onChoose}
            >
              {choosing ? 'Đang chọn…' : 'Chọn gia sư này'}
            </button>
          )}
        </div>
      </div>
    </article>
  );
}

function extractError(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string } | undefined;
    if (data?.message) return data.message;
  }
  if (err instanceof Error) return err.message;
  return 'Có lỗi xảy ra. Vui lòng thử lại.';
}
