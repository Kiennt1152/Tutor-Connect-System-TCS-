import type { CatalogOption, ClassResponse } from '../types/marketplaceTypes';
import { ClassDetailPanel } from './ClassDetailPanel';
import './tutorFindClass.css';

interface Props {
  readonly raw: ClassResponse;
  readonly subjects: CatalogOption[];
  readonly grades: CatalogOption[];
  readonly applied?: boolean;
  readonly applying?: boolean;
  readonly onApply?: () => void;
  readonly onClose: () => void;
}

export function ClassDetailModal({
  raw,
  subjects,
  grades,
  applied = false,
  applying = false,
  onApply,
  onClose,
}: Props) {
  return (
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
          <h2 className="cdm__title">{raw.title}</h2>
        </header>

        <ClassDetailPanel raw={raw} subjects={subjects} grades={grades} />

        <footer className="cdm__foot">
          <button type="button" className="tfc-btn tfc-btn--ghost" onClick={onClose}>
            Đóng
          </button>
          {onApply && (
            <button
              type="button"
              className="tfc-btn tfc-btn--primary"
              disabled={applied || applying}
              onClick={onApply}
            >
              {applied ? '✓ Đã ứng tuyển' : applying ? 'Đang gửi…' : 'Ứng tuyển nhận lớp'}
            </button>
          )}
        </footer>
      </div>
    </div>
  );
}
