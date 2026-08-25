import type { ReactNode } from 'react';
import { ExpiryBadge } from '../../../shared/components/ExpiryBadge';
import { money, slotLabel } from '../hooks/useClassSearch';
import { roundScore, type MatchResult } from '../matching/tutorMatching';
import './tutorFindClass.css';

interface CardProps {
  readonly result: MatchResult;
  readonly subjectName: (id: string) => string;
  readonly gradeName: (id: string) => string;
  /** Chưa lọc gì thì đừng khoe % — mọi lớp đều 100% sẽ gây hiểu nhầm. */
  readonly showScore: boolean;
  /** Nút ở đáy thẻ. Gia sư thì "Ứng tuyển", phụ huynh thì "Xem chi tiết". */
  readonly actions?: ReactNode;
}

/**
 * Thẻ một lớp trong danh sách kết quả — dùng chung cho màn gia sư tìm yêu cầu giảng dạy
 * và màn phụ huynh tìm lớp. Năm chip trên thẻ chính là 5 tiêu chí đang được chấm điểm,
 * bày sẵn ra để đối chiếu được con số % mà khỏi phải mở chi tiết.
 */
export function ClassResultCard({
  result,
  subjectName,
  gradeName,
  showScore,
  actions,
}: CardProps) {
  const { parsed, breakdown } = result;
  const c = parsed.raw;
  const pct = roundScore(breakdown.score);
  const tone = pct >= 75 ? 'high' : pct >= 45 ? 'mid' : 'low';
  const otherNames = parsed.subjectOther
    ? parsed.subjectOther.split(',').map((s) => s.trim()).filter(Boolean)
    : parsed.hasOtherSubject
      ? ['Môn khác']
      : [];
  const subjectLabel =
    parsed.subjectIds.map(subjectName).concat(otherNames).join(', ') || (c.subjectName ?? '—');
  const gradeLabel = gradeName(parsed.gradeId) || c.gradeName || '—';
  // Địa điểm chấm theo tỉnh RỒI mới tới phường/xã nên thẻ phải hiện đủ cả hai vế.
  const location =
    parsed.lessonMode === 'ONLINE'
      ? 'Học online'
      : [parsed.wardName || parsed.districtName, parsed.provinceName]
          .filter(Boolean)
          .join(', ') || c.address || c.locationName || 'Offline';
  const feeLabel =
    parsed.feePerHour <= 0
      ? ''
      : parsed.feeMin > 0 && parsed.feeMin !== parsed.feePerHour
        ? `${money(parsed.feeMin)} – ${money(parsed.feePerHour)}/giờ`
        : `${money(parsed.feePerHour)}/giờ`;
  const slotLabels = parsed.slots.map(slotLabel).filter(Boolean);
  const learningGoal = c.learningGoal?.trim() ?? '';
  const tutorRequirement = parsed.tutorRequirement?.trim() ?? '';
  const sessionCount = parsed.slots.length;
  const cycleWeeks = parsed.repeatEveryWeeks;
  const onWeeks = [...new Set(parsed.studyWeeks)]
    .filter((w) => Number.isInteger(w) && w >= 1 && w <= cycleWeeks)
    .sort((a, b) => a - b);
  const rhythm =
    cycleWeeks === 1 || onWeeks.length >= cycleWeeks || onWeeks.length === 0
      ? 'tuần'
      : onWeeks.length === 1 && onWeeks[0] === 1
        ? `${cycleWeeks} tuần`
        : `tuần học (tuần ${onWeeks.join(', ')}/${cycleWeeks})`;
  const scheduleSummary =
    sessionCount > 0
      ? parsed.scheduleMode === 'WEEKLY'
        ? `${sessionCount} buổi/${rhythm}`
        : `${sessionCount} buổi`
      : '';

  return (
    <article className={`tfc-card${showScore ? '' : ' tfc-card--noscore'}`}>
      {showScore && (
        <div className={`tfc-score tfc-score--${tone}`}>
          <span className="tfc-score__num">{pct}%</span>
          <span className="tfc-score__unit">phù hợp</span>
        </div>
      )}
      <div className="tfc-card__body">
        <div className="tfc-card__top">
          <h3 className="tfc-card__title">{c.title}</h3>
          {c.expiresAt && <ExpiryBadge expiresAt={c.expiresAt} />}
        </div>
        {/* Năm chip này chính là 5 tiêu chí đang được chấm điểm (S · E · L · P · T),
            bày sẵn ra thẻ để gia sư khỏi phải mở "Xem chi tiết" mới đối chiếu được.
            Lớp nhiều môn mỗi môn một giá thì hiện dạng khoảng "150.000đ – 200.000đ". */}
        <div className="tfc-card__meta">
          <span title="Môn học (S)">📚 {subjectLabel}</span>
          <span title="Khối lớp (E)">🎓 {gradeLabel}</span>
          <span title="Địa điểm (L)">📍 {location}</span>
          {feeLabel && <span title="Học phí (P)">💰 {feeLabel}</span>}
          {slotLabels.length > 0 && (
            <span title="Lịch học (T)">
              🕒 {slotLabels.join(' · ')}
              {scheduleSummary && ` (${scheduleSummary})`}
            </span>
          )}
        </div>

        <div className="tfc-card__info">
          {learningGoal && (
            <p className="tfc-card__info-row">
              <span className="tfc-card__ico" aria-hidden>🎯</span>
              <span>
                <strong>Mục tiêu:</strong> {learningGoal}
              </span>
            </p>
          )}
          {tutorRequirement && (
            <p className="tfc-card__info-row">
              <span className="tfc-card__ico" aria-hidden>🧑‍🏫</span>
              <span>
                <strong>Yêu cầu gia sư:</strong> {tutorRequirement}
              </span>
            </p>
          )}
          {/* Lịch học đã nằm ở chip 🕒 phía trên (kèm số buổi/tuần) nên bỏ dòng này. */}
        </div>

        {actions && <div className="tfc-card__actions">{actions}</div>}
      </div>
    </article>
  );
}
