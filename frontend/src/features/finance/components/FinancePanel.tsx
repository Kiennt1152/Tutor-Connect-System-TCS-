import { useState } from 'react';
import { useFinance } from '../hooks/useFinance';
import { VN_BANKS } from '../constants/banks';
import type {
  DepositResponse,
  PaymentMethod,
  Transaction,
  Withdrawal,
} from '../types/financeTypes';
import './FinancePanel.css';

const vnd = (value: number) =>
  new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(value) + ' ₫';

const DEPOSIT_AMOUNTS = [100000, 200000, 500000, 1000000, 2000000, 5000000];

const TX_STATUS_LABEL: Record<Transaction['status'], string> = {
  PENDING: 'Chờ thanh toán',
  SUCCESS: 'Thành công',
  FAILED: 'Thất bại',
  CANCELLED: 'Đã hủy',
};

const TYPE_LABEL: Record<Transaction['type'], string> = {
  DEPOSIT: 'Nạp tiền',
  WITHDRAWAL: 'Rút tiền',
  REFUND: 'Hoàn tiền',
  ESCROW_DEPOSIT: 'Ký quỹ',
  ESCROW_RELEASE: 'Giải ngân',
};

const WD_STATUS_LABEL: Record<Withdrawal['status'], string> = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Bị từ chối',
  COMPLETED: 'Hoàn tất',
};

const WD_STATUS_TAG: Record<Withdrawal['status'], string> = {
  PENDING: 'pending',
  APPROVED: 'success',
  REJECTED: 'failed',
  COMPLETED: 'success',
};

export function FinancePanel() {
  const {
    status,
    wallet,
    transactions,
    paymentMethods,
    withdrawals,
    createDeposit,
    watchDeposit,
    addPaymentMethod,
    deletePaymentMethod,
    createWithdrawal,
  } = useFinance();

  // Nạp tiền (inline)
  const [amount, setAmount] = useState<number>(100000);
  const [creatingDeposit, setCreatingDeposit] = useState(false);
  const [deposit, setDeposit] = useState<DepositResponse | null>(null);
  const [paid, setPaid] = useState(false);
  const [depositError, setDepositError] = useState('');

  // Tài khoản ngân hàng
  const [bankName, setBankName] = useState(VN_BANKS[0]);
  const [accNo, setAccNo] = useState('');
  const [accName, setAccName] = useState('');
  const [addingBank, setAddingBank] = useState(false);
  const [bankError, setBankError] = useState('');

  // Rút tiền (inline)
  const [wdAmount, setWdAmount] = useState<number>(0);
  const [wdMethodId, setWdMethodId] = useState<number | null>(null);
  const [submittingWd, setSubmittingWd] = useState(false);
  const [wdResult, setWdResult] = useState<Withdrawal | null>(null);
  const [wdError, setWdError] = useState('');

  async function handleCreateDeposit() {
    if (!amount || amount < 1000) {
      setDepositError('Số tiền nạp tối thiểu 1.000 ₫');
      return;
    }
    setDepositError('');
    setCreatingDeposit(true);
    try {
      const res = await createDeposit(amount);
      setDeposit(res);
      setPaid(false);
      watchDeposit(res.transactionId, () => setPaid(true));
    } catch {
      setDepositError('Không tạo được đơn nạp. Vui lòng thử lại.');
    } finally {
      setCreatingDeposit(false);
    }
  }

  function resetDeposit() {
    setDeposit(null);
    setPaid(false);
    setDepositError('');
  }

  async function handleAddBank() {
    if (!bankName) {
      setBankError('Vui lòng chọn ngân hàng');
      return;
    }
    if (!accNo.trim()) {
      setBankError('Vui lòng nhập số tài khoản');
      return;
    }
    if (accNo.trim().length < 6) {
      setBankError('Số tài khoản không hợp lệ (tối thiểu 6 chữ số)');
      return;
    }
    if (!accName.trim()) {
      setBankError('Vui lòng nhập tên chủ tài khoản');
      return;
    }
    setBankError('');
    setAddingBank(true);
    try {
      await addPaymentMethod(bankName, accNo.trim(), accName.trim());
      setAccNo('');
      setAccName('');
    } catch {
      setBankError('Không thêm được tài khoản ngân hàng.');
    } finally {
      setAddingBank(false);
    }
  }

  async function handleWithdraw() {
    const methodId = wdMethodId ?? paymentMethods[0]?.paymentMethodId ?? null;
    if (!methodId) {
      setWdError('Vui lòng thêm và chọn tài khoản ngân hàng nhận tiền');
      return;
    }
    if (!wdAmount || wdAmount < 1000) {
      setWdError('Số tiền rút tối thiểu 1.000 ₫');
      return;
    }
    if (wallet && wdAmount > wallet.availableBalance) {
      setWdError('Số dư khả dụng không đủ');
      return;
    }
    setWdError('');
    setSubmittingWd(true);
    try {
      const res = await createWithdrawal(wdAmount, methodId);
      setWdResult(res);
      setWdAmount(0);
    } catch (e) {
      const msg =
        (e as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Không gửi được yêu cầu rút.';
      setWdError(msg);
    } finally {
      setSubmittingWd(false);
    }
  }

  if (status === 'loading') {
    return <div className="fin-loading">Đang tải ví…</div>;
  }
  if (status === 'error') {
    return <div className="fin-error">Không tải được ví. Vui lòng tải lại trang.</div>;
  }

  const hasBank = paymentMethods.length > 0;
  const selectedMethod = wdMethodId ?? paymentMethods[0]?.paymentMethodId ?? '';
  // Lịch sử chỉ ghi nhận giao dịch ĐÃ THÀNH CÔNG (bỏ đơn nạp đang chờ thanh toán).
  const history = transactions.filter((t) => t.status === 'SUCCESS');

  return (
    <div className="fin">
      {/* Số dư */}
      <section className="fin-balance">
        <div>
          <p className="fin-balance__label">Số dư khả dụng</p>
          <p className="fin-balance__value">{vnd(wallet?.availableBalance ?? 0)}</p>
          {wallet && wallet.frozenBalance > 0 ? (
            <p className="fin-balance__frozen">Đang giữ (chờ duyệt rút): {vnd(wallet.frozenBalance)}</p>
          ) : null}
        </div>
      </section>

      <div className="fin-grid">
        {/* Nạp tiền — tạo bill chuyển khoản inline */}
        <section className="fin-card">
          <h2 className="fin-card__title">Nạp tiền vào ví</h2>

          {!deposit ? (
            <>
              <label className="fin-field">
                <span>Số tiền (₫)</span>
                <input
                  type="number"
                  min={1000}
                  step={1000}
                  value={amount}
                  onChange={(e) => setAmount(Number(e.target.value))}
                />
              </label>
              <div className="fin-quick">
                {DEPOSIT_AMOUNTS.map((q) => (
                  <button
                    key={q}
                    type="button"
                    className={`fin-quick__btn${amount === q ? ' fin-quick__btn--active' : ''}`}
                    onClick={() => setAmount(q)}
                  >
                    {vnd(q)}
                  </button>
                ))}
              </div>
              {depositError ? <p className="fin-modal__error">{depositError}</p> : null}
              <button
                className="fin-btn fin-btn--primary fin-btn--block"
                onClick={handleCreateDeposit}
                disabled={creatingDeposit}
              >
                {creatingDeposit ? 'Đang tạo mã…' : 'Tạo mã QR chuyển khoản'}
              </button>
            </>
          ) : paid ? (
            <div className="fin-modal__done">
              <div className="fin-check">✓</div>
              <p className="fin-modal__done-title">Nạp tiền thành công!</p>
              <p>Đã cộng {vnd(deposit.amount)} vào ví của bạn.</p>
              <button className="fin-btn fin-btn--primary fin-btn--block" onClick={resetDeposit}>
                Nạp tiếp
              </button>
            </div>
          ) : (
            <>
              <p className="fin-modal__hint">
                Mở app ngân hàng, quét mã VietQR bên dưới. <b>Giữ nguyên nội dung chuyển khoản</b> để
                hệ thống tự động ghi nhận.
              </p>
              <div className="fin-qr">
                <img src={deposit.qrImageUrl} alt="Mã QR nạp tiền" />
              </div>
              <dl className="fin-info">
                <div>
                  <dt>Ngân hàng</dt>
                  <dd>{deposit.bankName}</dd>
                </div>
                <div>
                  <dt>Số tài khoản</dt>
                  <dd>{deposit.accountNo}</dd>
                </div>
                <div>
                  <dt>Chủ tài khoản</dt>
                  <dd>{deposit.accountName}</dd>
                </div>
                <div>
                  <dt>Số tiền</dt>
                  <dd>{vnd(deposit.amount)}</dd>
                </div>
                <div>
                  <dt>Nội dung CK</dt>
                  <dd className="fin-info__ref">{deposit.transferContent}</dd>
                </div>
              </dl>
              <p className="fin-waiting">
                <span className="fin-spinner" /> Đang chờ thanh toán…
              </p>
              <button className="fin-btn fin-btn--ghost-dark fin-btn--block" onClick={resetDeposit}>
                Hủy / Tạo mã khác
              </button>
            </>
          )}
        </section>

        {/* Rút tiền inline */}
        <section className="fin-card">
          <h2 className="fin-card__title">Rút tiền</h2>
          {!hasBank ? (
            <p className="fin-card__empty">
              Bạn cần thêm tài khoản ngân hàng bên dưới trước khi rút tiền.
            </p>
          ) : (
            <>
              <label className="fin-field">
                <span>Tài khoản nhận</span>
                <select
                  value={selectedMethod}
                  onChange={(e) => setWdMethodId(Number(e.target.value))}
                >
                  {paymentMethods.map((pm) => (
                    <option key={pm.paymentMethodId} value={pm.paymentMethodId}>
                      {pm.bankName} · {pm.accountNo} · {pm.accountName}
                    </option>
                  ))}
                </select>
              </label>
              <label className="fin-field">
                <span>Số tiền (₫) — tối đa {vnd(wallet?.availableBalance ?? 0)}</span>
                <input
                  type="number"
                  min={1000}
                  step={1000}
                  value={wdAmount || ''}
                  onChange={(e) => {
                    setWdAmount(Number(e.target.value));
                    setWdResult(null);
                  }}
                />
              </label>
              {wdError ? <p className="fin-modal__error">{wdError}</p> : null}
              {wdResult ? (
                <p className={`fin-wd-result fin-wd-result--${wdResult.direct ? 'ok' : 'wait'}`}>
                  {wdResult.direct
                    ? `✓ Rút thành công ${vnd(wdResult.amount)} về ${wdResult.bankName}.`
                    : `⏳ Đã gửi yêu cầu rút ${vnd(wdResult.amount)}, đang chờ quản trị viên duyệt. Số tiền đã được tạm giữ.`}
                </p>
              ) : null}
              <button
                className="fin-btn fin-btn--primary fin-btn--block"
                onClick={handleWithdraw}
                disabled={submittingWd || !selectedMethod || !wdAmount || wdAmount < 1000}
              >
                {submittingWd ? 'Đang xử lý…' : 'Gửi yêu cầu rút'}
              </button>
            </>
          )}
        </section>
      </div>

      {/* Tài khoản ngân hàng */}
      <section className="fin-card">
        <h2 className="fin-card__title">Tài khoản ngân hàng của tôi</h2>
        {hasBank ? (
          <ul className="fin-banks">
            {paymentMethods.map((pm: PaymentMethod) => (
              <li key={pm.paymentMethodId} className="fin-bank">
                <div>
                  <p className="fin-bank__name">{pm.bankName}</p>
                  <p className="fin-bank__acc">
                    {pm.accountNo} · {pm.accountName}
                  </p>
                </div>
                <button
                  className="fin-bank__del"
                  onClick={() => deletePaymentMethod(pm.paymentMethodId)}
                  aria-label="Xóa"
                >
                  Xóa
                </button>
              </li>
            ))}
          </ul>
        ) : (
          <p className="fin-card__empty">Chưa có tài khoản nào. Thêm một tài khoản để rút tiền.</p>
        )}

        <p className="fin-bankform__hint">Tất cả các trường đều bắt buộc để có thể rút tiền.</p>
        <div className="fin-bankform">
          <select value={bankName} onChange={(e) => setBankName(e.target.value)}>
            {VN_BANKS.map((b) => (
              <option key={b} value={b}>
                {b}
              </option>
            ))}
          </select>
          <input
            placeholder="Số tài khoản *"
            inputMode="numeric"
            value={accNo}
            onChange={(e) => setAccNo(e.target.value.replace(/\D/g, ''))}
            required
          />
          <input
            placeholder="Tên chủ tài khoản *"
            value={accName}
            onChange={(e) => setAccName(e.target.value)}
            required
          />
          <button
            className="fin-btn fin-btn--primary"
            onClick={handleAddBank}
            disabled={addingBank || !accNo.trim() || !accName.trim()}
          >
            {addingBank ? 'Đang thêm…' : 'Thêm'}
          </button>
        </div>
        {bankError ? <p className="fin-modal__error">{bankError}</p> : null}
      </section>

      {/* Yêu cầu rút tiền */}
      {withdrawals.length > 0 ? (
        <section className="fin-card">
          <h2 className="fin-card__title">Yêu cầu rút tiền</h2>
          <table className="fin-table">
            <thead>
              <tr>
                <th>Tài khoản nhận</th>
                <th className="fin-table__right">Số tiền</th>
                <th>Trạng thái</th>
                <th>Thời gian</th>
              </tr>
            </thead>
            <tbody>
              {withdrawals.map((w) => (
                <tr key={w.withdrawalId}>
                  <td>
                    {w.bankName} · {w.accountNo}
                    {w.status === 'REJECTED' && w.failureReason ? (
                      <span className="fin-reason"> — {w.failureReason}</span>
                    ) : null}
                  </td>
                  <td className="fin-table__right">{vnd(w.amount)}</td>
                  <td>
                    <span className={`fin-tag fin-tag--${WD_STATUS_TAG[w.status]}`}>
                      {WD_STATUS_LABEL[w.status]}
                    </span>
                  </td>
                  <td>{new Date(w.requestedAt).toLocaleString('vi-VN')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      ) : null}

      {/* Lịch sử giao dịch */}
      <section className="fin-card">
        <h2 className="fin-card__title">Lịch sử giao dịch</h2>
        {history.length === 0 ? (
          <p className="fin-card__empty">Chưa có giao dịch thành công nào.</p>
        ) : (
          <table className="fin-table">
            <thead>
              <tr>
                <th>Loại</th>
                <th>Mã đơn</th>
                <th className="fin-table__right">Số tiền</th>
                <th>Trạng thái</th>
                <th>Thời gian</th>
              </tr>
            </thead>
            <tbody>
              {history.map((t) => (
                <tr key={t.transactionId}>
                  <td>{TYPE_LABEL[t.type]}</td>
                  <td className="fin-table__ref">{t.referenceCode ?? '—'}</td>
                  <td className="fin-table__right">{vnd(t.amount)}</td>
                  <td>
                    <span className={`fin-tag fin-tag--${t.status.toLowerCase()}`}>
                      {TX_STATUS_LABEL[t.status]}
                    </span>
                  </td>
                  <td>{new Date(t.createdAt).toLocaleString('vi-VN')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
