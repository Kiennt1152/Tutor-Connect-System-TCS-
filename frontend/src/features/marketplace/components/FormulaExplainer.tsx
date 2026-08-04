import type { MatchWeights } from '../matching/tutorMatching';
import './formulaExplainer.css';

const LEGEND: { short: string; label: string }[] = [
  { short: 'S', label: 'Môn & lớp' },
  { short: 'L', label: 'Địa điểm' },
  { short: 'P', label: 'Học phí' },
  { short: 'T', label: 'Lịch học' },
  { short: 'E', label: 'Trình độ' },
];

interface Props {
  readonly weights: MatchWeights;
  readonly defaultOpen?: boolean;
  readonly bare?: boolean;
}

export function FormulaExplainer({ weights, defaultOpen = true, bare = false }: Props) {
  const wSum =
    weights.subject + weights.location + weights.salary + weights.schedule + weights.experience;

  const body = (
    <div className="fx__body">
        <p className="fx__lead">
          Mỗi lớp được chấm <strong>0–100%</strong> — trung bình 5 tiêu chí, mỗi tiêu chí nhân với{' '}
          <strong>mức ưu tiên</strong> bạn kéo ở trên. Ưu tiên cao thì ảnh hưởng nhiều hơn.
        </p>

        {/* Công thức trung bình cộng có trọng số. */}
        <div className="fx__formula" aria-label="Công thức tính độ phù hợp">
          <span className="fx__eq">Phù hợp =</span>
          <span className="fx__frac">
            <span className="fx__num">
              W<sub>s</sub>·S + W<sub>l</sub>·L + W<sub>p</sub>·P + W<sub>t</sub>·T + W<sub>e</sub>·E
            </span>
            <span className="fx__bar" />
            <span className="fx__den">
              W<sub>s</sub> + W<sub>l</sub> + W<sub>p</sub> + W<sub>t</sub> + W<sub>e</sub>
            </span>
          </span>
          <span className="fx__eq">× 100</span>
        </div>

        {/* Chú thích mã tiêu chí + trọng số đang đặt. */}
        <div className="fx__legend">
          {LEGEND.map((c) => (
            <span key={c.short} className="fx__legend-item">
              <span className="fx__badge">{c.short}</span>
              {c.label}
            </span>
          ))}
        </div>

        <p className="fx__foot">
          W là mức ưu tiên (0–5) bạn đang đặt · tổng ΣW = <strong>{wSum}</strong>. Kéo một mức về{' '}
          <strong>0</strong> để bỏ tiêu chí đó khỏi công thức.
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
