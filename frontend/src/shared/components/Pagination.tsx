import './Pagination.css';

interface PaginationProps {
  readonly current: number;
  readonly totalPages: number;
  readonly onPageChange: (page: number) => void;
  /** Nhãn cho screen reader. */
  readonly ariaLabel?: string;
}

type PageItem = number | 'ellipsis';

/** Danh sách trang hiển thị: 1 2 3 4 5 … N, tự rút gọn bằng dấu … khi nhiều trang. */
function pageItems(current: number, total: number): PageItem[] {
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1);
  }
  if (current <= 4) {
    return [1, 2, 3, 4, 5, 'ellipsis', total];
  }
  if (current >= total - 3) {
    return [1, 'ellipsis', total - 4, total - 3, total - 2, total - 1, total];
  }
  return [1, 'ellipsis', current - 1, current, current + 1, 'ellipsis', total];
}

export function Pagination({ current, totalPages, onPageChange, ariaLabel }: PaginationProps) {
  if (totalPages < 1) return null;

  const total = Math.max(1, totalPages);
  const cur = Math.max(1, Math.min(current, total));

  const go = (p: number) => {
    const next = Math.min(Math.max(1, p), total);
    if (next !== cur) onPageChange(next);
  };

  const atStart = cur <= 1;
  const atEnd = cur >= total;

  return (
    <nav className="pgn" aria-label={ariaLabel ?? 'Phân trang'}>
      <button
        type="button"
        className="pgn__btn pgn__btn--nav"
        onClick={() => go(1)}
        disabled={atStart}
        aria-label="Trang đầu"
      >
        «
      </button>
      <button
        type="button"
        className="pgn__btn pgn__btn--nav"
        onClick={() => go(cur - 1)}
        disabled={atStart}
        aria-label="Trang trước"
      >
        ‹
      </button>

      {pageItems(cur, total).map((item, idx) =>
        item === 'ellipsis' ? (
          <span key={`ellipsis-${idx}`} className="pgn__ellipsis" aria-hidden="true">
            …
          </span>
        ) : (
          <button
            key={item}
            type="button"
            className={`pgn__btn ${item === cur ? 'pgn__btn--active' : ''}`}
            onClick={() => go(item)}
            aria-current={item === cur ? 'page' : undefined}
          >
            {item}
          </button>
        )
      )}

      <button
        type="button"
        className="pgn__btn pgn__btn--nav"
        onClick={() => go(cur + 1)}
        disabled={atEnd}
        aria-label="Trang sau"
      >
        ›
      </button>
      <button
        type="button"
        className="pgn__btn pgn__btn--nav"
        onClick={() => go(total)}
        disabled={atEnd}
        aria-label="Trang cuối"
      >
        »
      </button>
    </nav>
  );
}
