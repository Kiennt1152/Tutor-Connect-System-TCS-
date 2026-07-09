import type { ReactNode } from 'react';
import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import './ConfirmDialog.css';

export type ConfirmDialogVariant = 'primary' | 'warning' | 'danger';

type ConfirmDialogProps = {
  open: boolean;
  title: string;
  message: ReactNode;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: ConfirmDialogVariant;
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

const CONFIRM_BTN_CLASS: Record<ConfirmDialogVariant, string> = {
  primary: 'tcs-btn tcs-btn--primary',
  warning: 'tcs-btn tcs-btn--warning',
  danger: 'tcs-btn tcs-btn--danger',
};

export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Xác nhận',
  cancelLabel = 'Hủy',
  variant = 'primary',
  loading = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !loading) onCancel();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, loading, onCancel]);

  if (!open) return null;

  return createPortal(
    <div
      className="tcs-modal-overlay"
      role="presentation"
      onClick={() => {
        if (!loading) onCancel();
      }}
    >
      <div
        className="tcs-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="tcs-modal-title"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id="tcs-modal-title" className="tcs-modal__title">
          {title}
        </h2>
        <div className="tcs-modal__body">{message}</div>
        <div className="tcs-modal__actions">
          <button
            type="button"
            className="tcs-btn tcs-btn--ghost"
            onClick={onCancel}
            disabled={loading}
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            className={CONFIRM_BTN_CLASS[variant]}
            onClick={onConfirm}
            disabled={loading}
          >
            {loading ? 'Đang xử lý…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>,
    document.body,
  );
}
