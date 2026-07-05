type Props = {
  onCancel: () => void;
  onConfirm: () => Promise<void> | void;
  busy?: boolean;
};

export function RejectApplicationModal({ onCancel, onConfirm, busy }: Props) {
  return (
    <div className="mp-modal-backdrop" role="dialog" aria-modal="true">
      <div className="mp-modal">
        <h3>Từ chối đơn ứng tuyển</h3>
        <p style={{ margin: 0, color: 'var(--color-text-secondary)' }}>
          Bạn có chắc chắn muốn từ chối đơn ứng tuyển này? Hành động này không thể hoàn tác.
        </p>
        <div className="mp-modal__actions">
          <button
            type="button"
            className="mp-btn mp-btn--ghost"
            onClick={onCancel}
            disabled={busy}
          >
            Hủy
          </button>
          <button
            type="button"
            className="mp-btn mp-btn--danger"
            disabled={busy}
            onClick={() => void onConfirm()}
          >
            {busy ? 'Đang xử lý…' : 'Từ chối'}
          </button>
        </div>
      </div>
    </div>
  );
}