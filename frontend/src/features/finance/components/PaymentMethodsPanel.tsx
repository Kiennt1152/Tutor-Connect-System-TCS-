import { useState } from 'react';
import type {
  PaymentMethodInfo,
  PaymentMethodPayload,
} from '../types/financeTypes';
import { getApiErrorMessage } from '../../../shared/api/apiError';

interface Props {
  paymentMethods: PaymentMethodInfo[];
  loading: boolean;
  onLoad: () => Promise<void>;
  onCreate: (payload: PaymentMethodPayload) => Promise<PaymentMethodInfo>;
  onUpdate: (paymentMethodId: number, payload: PaymentMethodPayload) => Promise<PaymentMethodInfo>;
  onDelete: (paymentMethodId: number) => Promise<void>;
}

function displayBankName(method: PaymentMethodInfo) {
  return method.bankName || method.provider || 'Ngân hàng';
}

function displayAccount(method: PaymentMethodInfo) {
  return method.accountNoMasked || (method.lastFour ? `****${method.lastFour}` : 'Chưa có số tài khoản');
}

export function PaymentMethodsPanel({
  paymentMethods,
  loading,
  onLoad,
  onCreate,
  onUpdate,
  onDelete,
}: Props) {
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<PaymentMethodInfo | null>(null);
  const [bankName, setBankName] = useState('');
  const [accountNo, setAccountNo] = useState('');
  const [saving, setSaving] = useState(false);
  const [removingId, setRemovingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  function openCreateForm() {
    setEditing(null);
    setBankName('');
    setAccountNo('');
    setError(null);
    setSuccess(null);
    setFormOpen(true);
  }

  function openEditForm(method: PaymentMethodInfo) {
    setEditing(method);
    setBankName(displayBankName(method));
    setAccountNo('');
    setError(null);
    setSuccess(null);
    setFormOpen(true);
  }

  function closeForm() {
    if (saving) return;
    setFormOpen(false);
    setEditing(null);
    setBankName('');
    setAccountNo('');
    setError(null);
  }

  async function handleSubmit() {
    const normalizedBankName = bankName.trim();
    const normalizedAccountNo = accountNo.trim().replace(/\s+/g, '');
    if (!normalizedBankName) {
      setError('Vui lòng nhập tên ngân hàng');
      return;
    }
    if (!/^[A-Za-z0-9]{4,50}$/.test(normalizedAccountNo)) {
      setError('Số tài khoản chỉ gồm chữ/số và dài từ 4 đến 50 ký tự');
      return;
    }

    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      if (editing) {
        await onUpdate(editing.paymentMethodId, {
          bankName: normalizedBankName,
          accountNo: normalizedAccountNo,
        });
        setSuccess('Đã cập nhật tài khoản nhận tiền.');
      } else {
        await onCreate({
          bankName: normalizedBankName,
          accountNo: normalizedAccountNo,
        });
        setSuccess('Đã thêm tài khoản nhận tiền.');
      }
      setFormOpen(false);
      setEditing(null);
      setBankName('');
      setAccountNo('');
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Không thể lưu tài khoản nhận tiền.'));
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(method: PaymentMethodInfo) {
    const confirmed = window.confirm(`Xóa tài khoản ${displayBankName(method)} ${displayAccount(method)}?`);
    if (!confirmed) return;

    setRemovingId(method.paymentMethodId);
    setError(null);
    setSuccess(null);
    try {
      await onDelete(method.paymentMethodId);
      setSuccess('Đã xóa tài khoản nhận tiền.');
    } catch (err: unknown) {
      setError(getApiErrorMessage(err, 'Không thể xóa tài khoản nhận tiền.'));
    } finally {
      setRemovingId(null);
    }
  }

  return (
    <section className="payment-methods">
      <div className="payment-methods__header">
        <div>
          <h2>Tài khoản nhận tiền</h2>
          <p>Quản lý tài khoản ngân hàng dùng cho yêu cầu rút tiền.</p>
        </div>
        <div className="payment-methods__actions">
          <button className="btn btn--secondary" onClick={onLoad} disabled={loading}>
            Làm mới
          </button>
          <button className="btn btn--primary" onClick={openCreateForm}>
            Thêm tài khoản
          </button>
        </div>
      </div>

      {success && <div className="payment-methods__success">{success}</div>}
      {error && <div className="payment-methods__error">{error}</div>}

      {formOpen && (
        <div className="payment-method-form">
          <div className="payment-method-form__grid">
            <label>
              <span>Ngân hàng</span>
              <input
                className="form-input"
                value={bankName}
                onChange={(event) => setBankName(event.target.value)}
                placeholder="Ví dụ: Techcombank"
              />
            </label>
            <label>
              <span>Số tài khoản</span>
              <input
                className="form-input"
                value={accountNo}
                onChange={(event) => setAccountNo(event.target.value)}
                placeholder={editing ? 'Nhập số mới để cập nhật' : 'Nhập số tài khoản'}
              />
            </label>
          </div>
          <div className="payment-method-form__actions">
            <button className="btn btn--secondary" onClick={closeForm} disabled={saving}>
              Hủy
            </button>
            <button className="btn btn--primary" onClick={handleSubmit} disabled={saving}>
              {saving ? 'Đang lưu...' : editing ? 'Cập nhật' : 'Lưu tài khoản'}
            </button>
          </div>
        </div>
      )}

      <div className="payment-methods__list">
        {loading ? (
          <div className="payment-methods__empty">Đang tải tài khoản nhận tiền...</div>
        ) : paymentMethods.length === 0 ? (
          <div className="payment-methods__empty">Chưa có tài khoản nhận tiền nào.</div>
        ) : (
          paymentMethods.map((method) => (
            <div className="payment-method-item" key={method.paymentMethodId}>
              <div className="payment-method-item__main">
                <strong>{displayBankName(method)}</strong>
                <span>{displayAccount(method)}</span>
              </div>
              {method.isDefault && <span className="payment-method-item__default">Mặc định</span>}
              <div className="payment-method-item__actions">
                <button className="btn-link" onClick={() => openEditForm(method)}>
                  Sửa
                </button>
                <button
                  className="btn-link btn-link--danger"
                  onClick={() => void handleDelete(method)}
                  disabled={removingId === method.paymentMethodId}
                >
                  {removingId === method.paymentMethodId ? 'Đang xóa' : 'Xóa'}
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </section>
  );
}
