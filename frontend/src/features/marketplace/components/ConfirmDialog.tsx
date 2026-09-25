import { useEffect } from 'react';
import '../pages/MarketplacePage.css';

interface ConfirmDialogProps {
  readonly title: string;
  readonly message?: string;
  readonly confirmLabel?: string;
  readonly cancelLabel?: string;
  readonly onConfirm?: () => void;
  readonly onClose: () => void;
}

/** Hộp thoại xác nhận dùng chung: tiêu đề, nội dung (tuỳ chọn), nút xác nhận/huỷ; Esc để đóng. */
export function ConfirmDialog({
  title,
  message,
  confirmLabel = 'Đồng ý',
  cancelLabel = 'Hủy',
  onConfirm,
  onClose,
}: ConfirmDialogProps) {
  useEffect(() => {
    /** Bấm Esc thì đóng hộp thoại. */
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose();
    }
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onClose]);

  return (
    <div
      className="mkt-modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-label={title}
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className="mkt-modal">
        <h3 className="mkt-modal__title">{title}</h3>
        {message && <p className="mkt-modal__msg">{message}</p>}
        <div className="mkt-modal__actions">
          {onConfirm ? (
            <>
              <button type="button" className="mkt-btn mkt-btn--ghost" onClick={onClose}>
                {cancelLabel}
              </button>
              <button type="button" className="mkt-btn mkt-btn--primary" onClick={onConfirm} autoFocus>
                {confirmLabel}
              </button>
            </>
          ) : (
            <button type="button" className="mkt-btn mkt-btn--primary" onClick={onClose} autoFocus>
              OK
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
