import { createPortal } from 'react-dom';
import type { ApplicantResponse } from '../types/marketplaceTypes';
import './tutorFindClass.css';

const currency = new Intl.NumberFormat('vi-VN');

interface Props {
  readonly applicant: ApplicantResponse;
  readonly subjectName?: (id: string) => string;
  readonly onClose: () => void;
}

export function TutorDetailModal({ applicant: a, subjectName, onClose }: Props) {
  const fullName = a.fullName?.trim() || 'Gia sư';
  const initials = fullName
    .split(/\s+/)
    .slice(-2)
    .map((w) => w[0])
    .join('')
    .toUpperCase();
  const rate = a.proposedRate ?? a.hourlyRate ?? 0;
  const perSubject = Object.entries(a.proposedRates ?? {});

  return createPortal(
    <div
      className="cdm-overlay"
      role="dialog"
      aria-modal="true"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="cdm">
        <button type="button" className="cdm__close" aria-label="Đóng" onClick={onClose}>
          ✕
        </button>

        <header className="cdm__head">
          <span className="cdm__id">Hồ sơ gia sư</span>
        </header>

        <div className="cdm__body">
          {/* Thẻ tổng quan */}
          <div className="apl-card">
            <div className="apl-card__avatar">
              {a.avatar ? <img src={a.avatar} alt={fullName} /> : <span>{initials || '?'}</span>}
            </div>
            <div className="apl-card__head">
              <h3 className="apl-card__name">{fullName}</h3>
              {a.verificationStatus === 'VERIFIED' && (
                <span className="apl-verified">✓ Đã xác minh</span>
              )}
              <div className="apl-card__sub">
                <span> {a.ratingAvg != null ? Number(a.ratingAvg).toFixed(1) : '—'}/5</span>
                <span> {a.experienceYears ?? 0} năm kinh nghiệm</span>
                <span> {rate > 0 ? `${currency.format(rate)}đ/giờ` : '—'}</span>
                <span> {a.matchScore}% phù hợp</span>
              </div>
            </div>
          </div>

          {/* Học phí đề xuất — theo từng môn nếu gia sư đã báo giá chi tiết. */}
          {(perSubject.length > 0 || a.proposedRate != null) && (
            <section className="cdm-section">
              <h3>Học phí đề xuất</h3>
              {perSubject.length > 0 ? (
                <ul className="cdm-subj-list">
                  {perSubject.map(([id, fee]) => (
                    <li key={id}>
                      <span className="cdm-subj-list__name">
                        {subjectName ? subjectName(id) : `#${id}`}
                      </span>
                      <span className="cdm-subj-list__fee">{currency.format(fee)}đ/giờ</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="cdm-note">{currency.format(a.proposedRate as number)}đ/giờ</p>
              )}
            </section>
          )}

          {/* Giới thiệu & điểm nổi bật (trường, chuyên ngành, GPA, điểm thi…) */}
          <section className="cdm-section">
            <h3>Giới thiệu &amp; điểm nổi bật</h3>
            {a.bio?.trim() ? (
              <p className="cdm-note">{a.bio.trim()}</p>
            ) : (
              <p className="cdm-muted">Gia sư chưa cập nhật phần giới thiệu.</p>
            )}
          </section>

          {/* Lời nhắn khi ứng tuyển */}
          <section className="cdm-section">
            <h3>Lời nhắn khi ứng tuyển</h3>
            {a.coverLetter?.trim() ? (
              <p className="cdm-note">“{a.coverLetter.trim()}”</p>
            ) : (
              <p className="cdm-muted">Không có lời nhắn.</p>
            )}
          </section>
        </div>

        <footer className="cdm__foot">
          <button type="button" className="tfc-btn tfc-btn--ghost" onClick={onClose}>
            Đóng
          </button>
        </footer>
      </div>
    </div>,
    document.body,
  );
}
