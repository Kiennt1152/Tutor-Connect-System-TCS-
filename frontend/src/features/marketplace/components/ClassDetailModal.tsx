import { createPortal } from 'react-dom';
import { BusyConflictNotice } from './BusyConflictNotice';
import type { CatalogOption, ClassBusyConflict, ClassResponse } from '../types/marketplaceTypes';
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
  /** Gia sư: buổi của lớp trùng thời gian bận đã đăng ký. */
  readonly busyConflict?: ClassBusyConflict | null;
}

/**
 * Pop-up chi tiết tin tìm gia sư cho gia sư: thông tin lớp, học phí từng môn, lịch học và nút ứng tuyển theo trạng thái.
 */
export function ClassDetailModal({
  raw,
  subjects,
  grades,
  applied = false,
  applying = false,
  onApply,
  onClose,
  busyConflict,
}: Props) {
  // Portal ra body: hero có stacking context riêng (z-index: 1) nên modal đặt trong đó
  // sẽ bị header sticky đè lên dù z-index cao hơn.
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
          <h2 className="cdm__title">{raw.title}</h2>
        </header>

        {busyConflict && <BusyConflictNotice conflict={busyConflict} />}

        <ClassDetailPanel raw={raw} subjects={subjects} grades={grades} />

        <footer className="cdm__foot">
          <button type="button" className="tfc-btn tfc-btn--ghost" onClick={onClose}>
            Đóng
          </button>
          {onApply && (
            <button
              type="button"
              className="tfc-btn tfc-btn--primary"
              disabled={applied || applying || !!busyConflict}
              onClick={onApply}
            >
              {applied ? '✓ Đã ứng tuyển' : applying ? 'Đang gửi…' : busyConflict ? 'Trùng lịch bận' : 'Ứng tuyển'}
            </button>
          )}
        </footer>
      </div>
    </div>,
    document.body,
  );
}
