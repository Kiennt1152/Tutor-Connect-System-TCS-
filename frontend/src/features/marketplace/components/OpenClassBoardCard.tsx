import { useMemo } from 'react';
import { classToForm } from '../mappers/marketplaceMapper';
import { ExpiryBadge } from './ExpiryBadge';
import {
  CLASS_STATUS_LABELS,
  isOtherSubject,
  type CatalogOption,
  type ClassResponse,
} from '../types/marketplaceTypes';

const currency = new Intl.NumberFormat('vi-VN');

interface Props {
  readonly c: ClassResponse;
  readonly subjects: CatalogOption[];
}

/**
 * Thẻ lớp hiển thị ở màn "Danh sách tin đã đăng" (/danh-sach-tin-da-dang) — dùng lại giao diện
 * thẻ lớp của màn "Yêu cầu của tôi" (mkt-class-card): trạng thái + đếm ngược 30 ngày,
 * tiêu đề, lớp/hình thức, các môn & học phí/giờ, mục tiêu, địa điểm. Chỉ để xem (không có
 * nút quản lý của chủ lớp).
 */
export function OpenClassBoardCard({ c, subjects }: Props) {
  const form = useMemo(() => classToForm(c), [c]);
  const isOnline = c.lessonMode === 'ONLINE';

  const subjectRows = useMemo(() => {
    const nameById = new Map(subjects.map((s) => [String(s.id), s.name]));
    if (c.detailsJson && form.subjectIds.length > 0) {
      return form.subjectIds.map((id) => ({
        name: isOtherSubject(id)
          ? form.subjectOthers[id]?.trim() || 'Môn khác'
          : (nameById.get(id) ?? `#${id}`),
        fee: Number(form.subjectFees[id]) || 0,
      }));
    }
    return c.subjectName ? [{ name: c.subjectName, fee: c.tuitionFee ?? 0 }] : [];
  }, [c, form, subjects]);

  const address =
    [form.address, form.wardName, form.districtName, form.provinceName]
      .map((s) => s.trim())
      .filter(Boolean)
      .join(', ') || c.address || '';

  return (
    <article className="mkt-class-card">
      <div className="mkt-class-card__top">
        <span className={`mkt-status mkt-status--${c.status.toLowerCase()}`}>
          {CLASS_STATUS_LABELS[c.status] ?? c.status}
        </span>
        {c.status === 'OPEN' && c.expiresAt && <ExpiryBadge expiresAt={c.expiresAt} />}
      </div>
      <h3 className="mkt-class-card__title">{c.title}</h3>
      <dl className="mkt-class-card__meta">
        <div>
          <dt>Lớp</dt>
          <dd>{c.gradeName ?? '—'}</dd>
        </div>
        <div>
          <dt>Hình thức</dt>
          <dd>{isOnline ? 'Online' : 'Offline'}</dd>
        </div>
      </dl>
      <div className="mkt-class-card__subjects">
        <span className="mkt-class-card__subjects-label">
          {subjectRows.length > 1 ? 'Các môn & học phí/giờ' : 'Môn & học phí/giờ'}
        </span>
        {subjectRows.length > 0 ? (
          <ul className="mkt-subj-fees">
            {subjectRows.map((row, i) => (
              <li key={i}>
                <span className="mkt-subj-fees__name">📚 {row.name}</span>
                <span className="mkt-subj-fees__fee">
                  {row.fee > 0 ? `${currency.format(row.fee)} đ` : '—'}
                </span>
              </li>
            ))}
          </ul>
        ) : (
          <p className="mkt-class-card__loc">—</p>
        )}
      </div>
      {c.learningGoal && <p className="mkt-class-card__goal">🎯 {c.learningGoal}</p>}
      <p className="mkt-class-card__loc">📍 {isOnline ? 'Học Online' : address || '—'}</p>
    </article>
  );
}
