import { CRITERIA_KEYS, criteriaShares, type MatchWeights, type TutorCriteria } from '../matching/tutorMatching';
import './formulaExplainer.css';

const LABELS: Record<keyof MatchWeights, { short: string; label: string }> = {
  subject: { short: 'S', label: 'Môn học' },
  location: { short: 'L', label: 'Địa điểm' },
  salary: { short: 'P', label: 'Học phí' },
  schedule: { short: 'T', label: 'Lịch học' },
  grade: { short: 'E', label: 'Khối lớp' },
};

/** 20 -> "20" · 33.333 -> "33,3" (một chữ số lẻ là đủ đọc). */
const pct = (n: number): string =>
  (Math.round(n * 10) / 10).toFixed(Number.isInteger(Math.round(n * 10) / 10) ? 0 : 1).replace('.', ',');

interface Props {
  readonly criteria: TutorCriteria;
  readonly defaultOpen?: boolean;
  readonly bare?: boolean;
}

export function FormulaExplainer({ criteria, defaultOpen = true, bare = false }: Props) {
  const shares = criteriaShares(criteria);
  const kept = CRITERIA_KEYS.filter((k) => criteria.weights[k] > 0);
  const maxScore = CRITERIA_KEYS.reduce((sum, k) => sum + shares[k].cap, 0);

  const body = (
    <div className="fx__body">
      <p className="fx__lead">
        <strong>100%</strong> được chia cho <strong>5 tiêu chí</strong> — mặc định mỗi tiêu chí{' '}
        <strong>20%</strong>. Kéo một thanh về <strong>0</strong> là bỏ hẳn tiêu chí đó, phần
        20% của nó chia đều cho các thanh còn lại. Mức ưu tiên <strong>1–5</strong> quyết định
        bạn lấy bao nhiêu trong phần của mình: mức 1 chỉ lấy <strong>1/5</strong> của 20% ={' '}
        <strong>4%</strong>, mức 5 lấy trọn <strong>20%</strong>.
      </p>

      {/* Tổng điểm = cộng phần thực nhận của từng tiêu chí. */}
      <div className="fx__formula" aria-label="Công thức tính độ phù hợp">
        <span className="fx__eq">Phù hợp = Σ</span>
        <span className="fx__eq">Phần<sub>i</sub></span>
        <span className="fx__eq">×</span>
        <span className="fx__frac">
          <span className="fx__num">Mức<sub>i</sub></span>
          <span className="fx__bar" />
          <span className="fx__den">5</span>
        </span>
        <span className="fx__eq">× Khớp<sub>i</sub></span>
      </div>

      {/* Bảng phần điểm đang áp dụng cho đúng bộ lọc bạn đang đặt. */}
      <table className="fx__table">
        <thead>
          <tr>
            <th>Tiêu chí</th>
            <th>Phần</th>
            <th>Mức</th>
            <th>Tối đa</th>
          </tr>
        </thead>
        <tbody>
          {CRITERIA_KEYS.map((k) => {
            const s = shares[k];
            return (
              <tr key={k} className={s.share === 0 ? 'fx__row--off' : undefined}>
                <td>
                  <span className="fx__badge">{LABELS[k].short}</span>
                  {LABELS[k].label}
                </td>
                <td>{s.share === 0 ? '—' : `${pct(s.share)}%`}</td>
                <td>
                  {s.share === 0 ? 'đã bỏ' : s.filled ? criteria.weights[k] : <em>để trống</em>}
                </td>
                <td className="fx__cap">{s.cap === 0 ? '—' : `${pct(s.cap)}%`}</td>
              </tr>
            );
          })}
        </tbody>
      </table>

      <p className="fx__foot">
        <strong>Khớp</strong> là mức đáp ứng của từng lớp, 0–100%. Thiếu môn ở phía nào cũng
        trừ như nhau: lớp cần <em>Hóa + Sinh</em> mà bạn chỉ tìm <em>Sinh</em> thì khớp môn =
        50%, và bạn tìm <em>Toán + Anh</em> mà lớp chỉ cần <em>Toán</em> thì cũng 50%. Nếu
        phần Môn học đang có trần 4% thì lớp đó nhận 2% từ tiêu chí Môn học.
        {' '}Tiêu chí bạn <strong>để trống</strong> mặc nhiên khớp 100% và giữ trọn phần của nó —
        không nêu thì không có gì để xếp mức ưu tiên, nên thanh trượt của nó không làm đổi %.
        {' '}Điểm cao nhất một lớp có thể đạt lúc này là <strong>{pct(maxScore)}%</strong>
        {kept.length < CRITERIA_KEYS.length && ` (${CRITERIA_KEYS.length - kept.length} tiêu chí đã bỏ)`}.
      </p>
    </div>
  );

  if (bare) return <div className="fx fx--bare">{body}</div>;

  return (
    <details className="fx" open={defaultOpen}>
      <summary className="fx__summary">
        <span className="fx__summary-title">Cách tính độ phù hợp</span>
        <span className="fx__summary-hint">0–100%</span>
      </summary>
      {body}
    </details>
  );
}
