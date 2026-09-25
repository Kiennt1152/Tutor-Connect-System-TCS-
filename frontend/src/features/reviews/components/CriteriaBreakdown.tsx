import { REVIEW_CRITERIA } from '../config/reviewCriteria';
import type { ReviewCriterionScore } from '../types/reviewTypes';

/** Nhãn của mức điểm trong một tiêu chí (ví dụ 5 -> "Luôn luôn đúng giờ"); không có thì "x/5". */
function levelLabel(code: string, score: number): string {
  const config = REVIEW_CRITERIA.find((c) => c.code === code);
  return config?.levels.find((l) => l.score === score)?.label ?? `${score}/5`;
}

/** Đọc JSON điểm tiêu chí thành danh sách; lỗi thì rỗng. */
function parse(criteriaJson: string | null): ReviewCriterionScore[] {
  if (!criteriaJson) return [];
  try {
    const data = JSON.parse(criteriaJson) as ReviewCriterionScore[];
    return Array.isArray(data) ? data : [];
  } catch {
    return [];
  }
}

/** Danh sách điểm từng tiêu chí của một đánh giá (câu hỏi + nhãn mức điểm). */
export function CriteriaBreakdown({ criteriaJson }: { criteriaJson: string | null }) {
  const items = parse(criteriaJson);
  if (items.length === 0) return null;

  return (
    <ul className="rv-breakdown">
      {items.map((item) => (
        <li key={item.code} className="rv-breakdown__row">
          <span className="rv-breakdown__q">{item.question}</span>
          <span className="rv-breakdown__val">
            <span className="rv-breakdown__stars" aria-hidden="true">
              {'★'.repeat(item.score)}
              {'☆'.repeat(5 - item.score)}
            </span>
            <span className="rv-breakdown__label">{levelLabel(item.code, item.score)}</span>
          </span>
        </li>
      ))}
    </ul>
  );
}
