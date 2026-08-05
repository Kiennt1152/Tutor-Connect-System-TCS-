import { useState } from 'react';

type StarRatingProps = {
  value: number;
  onChange?: (value: number) => void;
  readOnly?: boolean;
  size?: number;
};

export function StarRating({ value, onChange, readOnly = false, size = 28 }: StarRatingProps) {
  const [hover, setHover] = useState(0);
  const active = hover || value;

  return (
    <div className="rv-stars" role={readOnly ? 'img' : 'radiogroup'} aria-label={`${value} trên 5 sao`}>
      {[1, 2, 3, 4, 5].map((star) => {
        const filled = star <= active;
        return (
          <button
            key={star}
            type="button"
            className={`rv-star${filled ? ' rv-star--on' : ''}${readOnly ? ' rv-star--readonly' : ''}`}
            style={{ fontSize: size }}
            disabled={readOnly}
            aria-label={`${star} sao`}
            onMouseEnter={readOnly ? undefined : () => setHover(star)}
            onMouseLeave={readOnly ? undefined : () => setHover(0)}
            onClick={readOnly ? undefined : () => onChange?.(star)}
          >
            {filled ? '★' : '☆'}
          </button>
        );
      })}
    </div>
  );
}
