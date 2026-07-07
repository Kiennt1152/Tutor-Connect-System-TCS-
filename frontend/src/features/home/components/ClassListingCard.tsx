import type { OpenClassItem } from '../types/openClassTypes';

const currency = (value: number) =>
  new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(value);

const formatDate = (value: string | null) => {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const lessonModeLabel: Record<string, string> = {
  ONLINE: 'Online',
  OFFLINE: 'Offline',
  HYBRID: 'Kết hợp',
};

type ClassListingCardProps = {
  classItem: OpenClassItem;
  isAuthenticated: boolean;
};

export function ClassListingCard({ classItem, isAuthenticated }: ClassListingCardProps) {
  const price = classItem.budget > 0 ? classItem.budget : classItem.tuitionFee;
  const subject = classItem.subjectName?.trim() || 'Môn học';
  const description =
    classItem.description.trim().length > 160
      ? `${classItem.description.trim().slice(0, 160)}…`
      : classItem.description.trim();

  return (
    <article className="tcs-class-card">
      <div className="tcs-class-card__top">
        <div>
          <p className="tcs-class-card__subject">Môn: {subject}</p>
          <h3 className="tcs-class-card__title">{classItem.title}</h3>
        </div>
        <div className="tcs-class-card__price">
          <span className="tcs-class-card__price-value">{currency(price)} đ</span>
          <span className="tcs-class-card__price-unit">/buổi</span>
        </div>
      </div>

      <div className="tcs-class-card__tags">
        <span className="tcs-class-card__tag tcs-class-card__tag--open">Đang mở</span>
        {classItem.gradeName ? (
          <span className="tcs-class-card__tag">{classItem.gradeName}</span>
        ) : null}
        <span className="tcs-class-card__tag">
          {lessonModeLabel[classItem.lessonMode] ?? classItem.lessonMode}
        </span>
      </div>

      <div className="tcs-class-card__meta">
        <div className="tcs-class-card__row">
          <span className="tcs-class-card__label">Mã lớp</span>
          <span className="tcs-class-card__value">#{classItem.id}</span>
        </div>
        <div className="tcs-class-card__row">
          <span className="tcs-class-card__label">Số buổi</span>
          <span className="tcs-class-card__value">{classItem.numberOfSessions} buổi</span>
        </div>
        <div className="tcs-class-card__row">
          <span className="tcs-class-card__label">Người đăng</span>
          <span className="tcs-class-card__value">{classItem.creatorName}</span>
        </div>
        <div className="tcs-class-card__row">
          <span className="tcs-class-card__label">Ngày tạo</span>
          <span className="tcs-class-card__value">{formatDate(classItem.createdAt)}</span>
        </div>
      </div>

      {description ? <p className="tcs-class-card__desc">{description}</p> : null}

      <div className="tcs-class-card__foot">
        {isAuthenticated ? (
          <button type="button" className="tcs-btn tcs-btn--market" disabled title="Sắp có">
            Xem chi tiết
          </button>
        ) : (
          <a className="tcs-btn tcs-btn--market" href="/login">
            Xem chi tiết
          </a>
        )}
      </div>
    </article>
  );
}
