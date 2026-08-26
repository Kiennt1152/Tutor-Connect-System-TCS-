import { useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { marketplaceApi } from '../api/marketplaceApi';
import { classToForm } from '../mappers/marketplaceMapper';
import {
  isOtherSubject,
  type ApplicantResponse,
  type CatalogOption,
  type ClassResponse,
} from '../types/marketplaceTypes';
import { TutorDetailModal } from './TutorDetailModal';
import { ConfirmDialog } from './ConfirmDialog';
import './applicantsModal.css';

const currency = new Intl.NumberFormat('vi-VN');

interface Props {
  readonly classId: number;
  readonly target: ClassResponse;
  readonly subjects: CatalogOption[];
  readonly onChosen?: () => void;
}

export function ApplicantsPanel({ classId, target, subjects, onChosen }: Props) {
  const [applicants, setApplicants] = useState<ApplicantResponse[]>([]);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [choosingId, setChoosingId] = useState<number | null>(null);
  const [rejectingId, setRejectingId] = useState<number | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [detailApplicant, setDetailApplicant] = useState<ApplicantResponse | null>(null);
  const [confirmAction, setConfirmAction] = useState<
    { kind: 'choose' | 'reject'; applicationId: number } | null
  >(null);
  const [rejectReason, setRejectReason] = useState('');

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

  // Khi đã chọn gia sư: chỉ hiển thị đúng gia sư đó, ẩn hẳn các ứng viên còn lại (không hiện mờ).
  const acceptedApplicant = applicants.find((a) => a.status === 'ACCEPTED');
  const visibleApplicants = acceptedApplicant
    ? [acceptedApplicant]
    : applicants.filter((a) => a.status !== 'REJECTED');
  const alreadyChosen = !!acceptedApplicant;
  const tutorAccepted = target.status === 'IN_PROGRESS';

  const subjectName = useMemo(() => {
    const form = classToForm(target);
    const m = new Map(subjects.map((s) => [String(s.id), s.name]));
    return (id: string) =>
      isOtherSubject(id) ? form.subjectOthers[id]?.trim() || 'Môn khác' : (m.get(id) ?? `#${id}`);
  }, [target, subjects]);
  const classSubjectIds = useMemo(() => classToForm(target).subjectIds, [target]);

  async function handleChoose(applicationId: number) {
    setChoosingId(applicationId);
    setNotice(null);
    try {
      await marketplaceApi.chooseApplicant(classId, applicationId);
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

  async function handleReject(applicationId: number, reason: string) {
    setRejectingId(applicationId);
    setNotice(null);
    try {
      await marketplaceApi.rejectApplicant(classId, applicationId, reason);
      setApplicants((list) => list.filter((a) => a.applicationId !== applicationId));
      setNotice('Đã từ chối gia sư và gửi thông báo kèm lý do. Lớp mở lại cho gia sư ứng tuyển.');
      onChosen?.();
    } catch (err) {
      setNotice(extractError(err));
    } finally {
      setRejectingId(null);
    }
  }

  return (
    <div className="apm-panel">
      {notice && <div className="apm-notice">{notice}</div>}

      {status === 'loading' && <div className="apm-state">Đang tải danh sách ứng viên…</div>}
      {status === 'error' && (
        <div className="apm-state apm-state--error">Không tải được danh sách ứng viên.</div>
      )}
      {status === 'success' && visibleApplicants.length === 0 && (
        <div className="apm-state">Chưa có gia sư nào ứng tuyển vào lớp này.</div>
      )}

      {status === 'success' && visibleApplicants.length > 0 && (
        <>
          {/* Giải thích AI + Top 5 gợi ý — ẩn khi đã chọn gia sư (chỉ còn 1 người). */}
          {!alreadyChosen && (
            <div className="apm-ai">
              <div className="apm-ai__badge">AI</div>
              <p className="apm-ai__text">
                Trợ lý AI đã xếp hạng {visibleApplicants.length} ứng viên theo{' '}
                <strong>đánh giá, kinh nghiệm và mức phí</strong> — mỗi tiêu chí chiếm 1/3 số điểm.
                <br />
                5 sao ăn trọn phần đánh giá;
                <br />
                5 năm kinh nghiệm ăn trọn phần kinh nghiệm;
                <br />
                báo giá bằng giá lớp ăn trọn phần mức phí, gấp đôi giá lớp thì phần này về 0.
              </p>
            </div>
          )}

          <div className="apm-list">
            {visibleApplicants.map((a, idx) => (
              <ApplicantCard
                key={a.applicationId}
                applicant={a}
                subjectName={subjectName}
                classSubjectIds={classSubjectIds}
                tutorAccepted={tutorAccepted}
                rank={!alreadyChosen && a.recommended ? idx + 1 : null}
                choosing={choosingId === a.applicationId}
                rejecting={rejectingId === a.applicationId}
                disabled={alreadyChosen || choosingId != null || rejectingId != null}
                onChoose={() => setConfirmAction({ kind: 'choose', applicationId: a.applicationId })}
                onReject={() => {
                  setRejectReason('');
                  setConfirmAction({ kind: 'reject', applicationId: a.applicationId });
                }}
                onDetail={() => setDetailApplicant(a)}
              />
            ))}
          </div>
        </>
      )}

      {detailApplicant && (
        <TutorDetailModal
          applicant={detailApplicant}
          subjectName={subjectName}
          onClose={() => setDetailApplicant(null)}
        />
      )}

      {confirmAction?.kind === 'choose' && (
        <ConfirmDialog
          title="Chọn gia sư"
          message="Chọn gia sư này cho lớp? Các ứng viên còn lại sẽ bị từ chối."
          confirmLabel="Chọn gia sư này"
          cancelLabel="Hủy"
          onConfirm={() => {
            const id = confirmAction.applicationId;
            setConfirmAction(null);
            void handleChoose(id);
          }}
          onClose={() => setConfirmAction(null)}
        />
      )}

      {confirmAction?.kind === 'reject' && (
        <div
          className="mkt-modal-overlay"
          role="dialog"
          aria-modal="true"
          aria-label="Từ chối gia sư"
          onMouseDown={(e) => {
            if (e.target === e.currentTarget) setConfirmAction(null);
          }}
        >
          <div className="mkt-modal">
            <h3 className="mkt-modal__title">Từ chối gia sư</h3>
            <p className="mkt-modal__msg">
              Gia sư sẽ nhận được thông báo kèm lý do. Vui lòng cho biết lý do từ chối:
            </p>
            <textarea
              className="apm-reason"
              rows={3}
              autoFocus
              value={rejectReason}
              placeholder="VD: Lịch dạy không phù hợp, học phí cao hơn mong muốn…"
              onChange={(e) => setRejectReason(e.target.value)}
            />
            <div className="mkt-modal__actions">
              <button
                type="button"
                className="mkt-btn mkt-btn--ghost"
                onClick={() => setConfirmAction(null)}
              >
                Hủy
              </button>
              <button
                type="button"
                className="mkt-btn mkt-btn--primary"
                disabled={!rejectReason.trim()}
                title={!rejectReason.trim() ? 'Vui lòng nhập lý do' : undefined}
                onClick={() => {
                  const id = confirmAction.applicationId;
                  const reason = rejectReason.trim();
                  setConfirmAction(null);
                  void handleReject(id, reason);
                }}
              >
                Gửi & từ chối
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

interface CardProps {
  readonly applicant: ApplicantResponse;
  readonly subjectName: (id: string) => string;
  readonly classSubjectIds: string[];
  readonly tutorAccepted: boolean;
  readonly rank: number | null;
  readonly choosing: boolean;
  readonly rejecting: boolean;
  readonly disabled: boolean;
  readonly onChoose: () => void;
  readonly onReject: () => void;
  readonly onDetail: () => void;
}

function ApplicantCard({
  applicant: a,
  subjectName,
  classSubjectIds,
  tutorAccepted,
  rank,
  choosing,
  rejecting,
  disabled,
  onChoose,
  onReject,
  onDetail,
}: CardProps) {
  const initials = a.fullName
    .split(/\s+/)
    .slice(-2)
    .map((w) => w[0])
    .join('')
    .toUpperCase();
  const perSubject = Object.entries(a.proposedRates ?? {});
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
          <div
            className={`apm-card__score apm-card__score--${tone}`}
            title="Mức độ phù hợp AI chấm trên thang 100%"
          >
            <span className="apm-card__score-num">{a.matchScore}%</span>
            <span className="apm-card__score-unit">phù hợp</span>
          </div>
        </div>

        <div className="apm-card__meta">
          <span>⭐ {a.ratingAvg != null ? Number(a.ratingAvg).toFixed(1) : '—'}/5</span>
          <span>🎓 {a.experienceYears ?? 0} năm KN</span>
          {perSubject.length === 0 && (
            <span>💰 {rate > 0 ? `${currency.format(rate)}đ/giờ` : '—'}</span>
          )}
        </div>

        {perSubject.length > 0 && (
          <ul className="apm-card__rates">
            {(classSubjectIds.length > 0 ? classSubjectIds : perSubject.map(([id]) => id)).map(
              (id) => {
                const fee = a.proposedRates?.[id];
                const teaching = fee != null;
                return (
                  <li key={id} className={`apm-rate ${teaching ? '' : 'apm-rate--off'}`}>
                    <span className="apm-rate__subject">
                      <span className={`apm-rate__mark apm-rate__mark--${teaching ? 'yes' : 'no'}`}>
                        {teaching ? '✓' : '✕'}
                      </span>
                      {subjectName(id)}
                    </span>
                    <span className="apm-rate__fee">
                      {teaching ? `${currency.format(fee)}đ/giờ` : 'Không dạy'}
                    </span>
                  </li>
                );
              },
            )}
          </ul>
        )}

        {a.bio && <p className="apm-card__bio">{a.bio}</p>}
        {a.coverLetter && <p className="apm-card__cover">“{a.coverLetter}”</p>}

        <div className="apm-card__actions">
          <button type="button" className="mkt-btn mkt-btn--ghost" onClick={onDetail}>
            Xem chi tiết gia sư
          </button>
          {accepted ? (
            tutorAccepted ? (
              <span className="apm-chip apm-chip--accepted">✓ Gia sư đã nhận lớp</span>
            ) : (
              <span
                className="apm-chip apm-chip--accepted"
                title="Gia sư cần bấm nhận lớp thì lịch học mới bắt đầu"
              >
                ✓ Đã chọn — chờ gia sư nhận lớp
              </span>
            )
          ) : rejected ? (
            <span className="apm-chip apm-chip--rejected">Đã từ chối</span>
          ) : (
            <>
              <button
                type="button"
                className="mkt-btn mkt-btn--ghost"
                disabled={disabled}
                onClick={onReject}
              >
                {rejecting ? 'Đang từ chối…' : 'Từ chối'}
              </button>
              <button
                type="button"
                className="mkt-btn mkt-btn--primary"
                disabled={disabled}
                onClick={onChoose}
              >
                {choosing ? 'Đang chọn…' : 'Chọn gia sư này'}
              </button>
            </>
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
