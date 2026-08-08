import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import axios from 'axios';
import { ClassIssueModal } from '../../dispute/components/ClassIssueModal';
import '../../finance/FinancePage.css';
import { RefundRequestModal } from '../../marketplace/components/RefundRequestModal';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { contractApi } from '../api/contractApi';
import { useContractDetail, useSignContract } from '../hooks/useContract';
import { BankPickerDialog, BankSelectField, findBankByName } from '../../finance/components/BankPicker';
import type {
  ContractStatus,
  EscrowStatus,
  PaymentTransactionStatus,
} from '../types/contractTypes';
import './ContractPage.css';

const STATUS_LABEL: Record<ContractStatus, { label: string; cls: string }> = {
  PENDING: { label: 'Chờ ký', cls: 'contract-status--pending' },
  DRAFT: { label: 'Chưa ký', cls: 'contract-status--draft' },
  SIGNED: { label: 'Đã ký', cls: 'contract-status--signed' },
  ACTIVE: { label: 'Đang hoạt động', cls: 'contract-status--active' },
  COMPLETED: { label: 'Hoàn thành', cls: 'contract-status--completed' },
  TERMINATED: { label: 'Đã chấm dứt', cls: 'contract-status--terminated' },
};

const ESCROW_STATUS_LABEL: Record<EscrowStatus, { label: string; cls: string }> = {
  PENDING: { label: 'Chờ thanh toán', cls: 'contract-status--pending' },
  FUNDED: { label: 'Đã nạp escrow', cls: 'contract-status--active' },
  RELEASED: { label: 'Đã giải ngân', cls: 'contract-status--completed' },
  REFUNDED: { label: 'Đã hoàn tiền', cls: 'contract-status--completed' },
  ON_HOLD: { label: 'Đang tạm giữ', cls: 'contract-status--signed' },
  DISPUTED: { label: 'Đang tranh chấp', cls: 'contract-status--terminated' },
};

const PAYMENT_STATUS_LABEL: Record<PaymentTransactionStatus, string> = {
  PENDING: 'Chờ SePay xác nhận',
  SUCCESS: 'Đã xác nhận',
  FAILED: 'Thất bại',
  CANCELLED: 'Đã hủy',
};

const formatCurrency = (value: number | string | null) => {
  if (value == null) return '—';
  return `${Number(value).toLocaleString('vi-VN')} đ`;
};

const formatDate = (value: string | null) => {
  if (!value) return '—';
  return new Date(value).toLocaleDateString('vi-VN');
};

const formatDateTime = (value: string | null) => {
  if (!value) return '—';
  return new Date(value).toLocaleString('vi-VN');
};

const copyText = (value: string | null | undefined) => {
  if (!value) return;
  void navigator.clipboard?.writeText(value);
};

type PaymentToastTone = 'success' | 'warning' | 'error';

const getApiMessage = (error: unknown, fallback: string) => {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
};

const isEscrowPaymentConfirmed = (
  paymentStatus: PaymentTransactionStatus | null | undefined,
  escrowStatus: EscrowStatus | null | undefined,
) =>
  paymentStatus === 'SUCCESS'
  || escrowStatus === 'FUNDED'
  || escrowStatus === 'ON_HOLD'
  || escrowStatus === 'DISPUTED'
  || escrowStatus === 'RELEASED'
  || escrowStatus === 'REFUNDED';

export default function ContractDetailPage() {
  const { contractId } = useParams<{ contractId: string }>();
  const id = Number(contractId);

  const { contract, signatures, loading, error, reload } = useContractDetail();
  const { otpSent, sendingOtp, signing, error: signError, sendOtp, sign } = useSignContract();

  const [otpInput, setOtpInput] = useState('');
  const [otpSentSuccess, setOtpSentSuccess] = useState(false);
  const [signSuccess, setSignSuccess] = useState(false);
  const [issueModalOpen, setIssueModalOpen] = useState(false);
  const [refundModalOpen, setRefundModalOpen] = useState(false);
  const [payoutBankName, setPayoutBankName] = useState('');
  const [payoutAccountNo, setPayoutAccountNo] = useState('');
  const [payoutAccountHolder, setPayoutAccountHolder] = useState('');
  const [payoutDialogOpen, setPayoutDialogOpen] = useState(false);
  const [savingPayout, setSavingPayout] = useState(false);
  const [payoutMessage, setPayoutMessage] = useState('');
  const [checkingPaymentStatus, setCheckingPaymentStatus] = useState(false);
  const [paymentReloading, setPaymentReloading] = useState(false);
  const [paymentToast, setPaymentToast] = useState<{
    tone: PaymentToastTone;
    message: string;
  } | null>(null);

  useEffect(() => {
    if (id) void reload(id);
  }, [id, reload]);

  useEffect(() => {
    if (signSuccess && id) {
      void reload(id);
      setSignSuccess(false);
    }
  }, [signSuccess, id, reload]);

  useEffect(() => {
    if (!contract) return;
    setPayoutMessage('');
    if (contract.refundPayoutInfo) {
      setPayoutBankName(contract.refundPayoutInfo.bankName ?? '');
      setPayoutAccountHolder(contract.refundPayoutInfo.accountHolderName ?? '');
      setPayoutAccountNo('');
    } else {
      setPayoutBankName('');
      setPayoutAccountHolder('');
      setPayoutAccountNo('');
    }
  }, [contract]);

  const handleSendOtp = async () => {
    if (!id) return;
    const result = await sendOtp(id);
    if (result) setOtpSentSuccess(true);
  };

  const handleSign = async () => {
    if (!id || otpInput.trim().length !== 6) return;
    const result = await sign(id, otpInput.trim());
    if (result) {
      setOtpInput('');
      setSignSuccess(true);
      setOtpSentSuccess(false);
    }
  };

  const handleSaveRefundPayout = async () => {
    if (!id || !payoutBankName.trim() || !payoutAccountNo.trim() || !payoutAccountHolder.trim()) {
      setPayoutMessage('Vui lòng chọn ngân hàng, nhập số tài khoản và tên chủ tài khoản.');
      return;
    }
    setSavingPayout(true);
    setPayoutMessage('');
    try {
      await contractApi.saveRefundPayoutInfo(id, {
        bankName: payoutBankName.trim(),
        accountNo: payoutAccountNo.trim(),
        accountHolderName: payoutAccountHolder.trim(),
      });
      setPayoutMessage('Đã lưu thông tin nhận hoàn tiền.');
      await reload(id);
    } catch (error) {
      setPayoutMessage(getApiMessage(error, 'Không lưu được thông tin nhận hoàn tiền.'));
    } finally {
      setSavingPayout(false);
    }
  };

  const showPaymentToast = (tone: PaymentToastTone, message: string, autoClose = true) => {
    setPaymentToast({ tone, message });
    if (autoClose) {
      window.setTimeout(() => setPaymentToast(null), 4200);
    }
  };

  const handleCheckEscrowPaymentStatus = async () => {
    if (!id || checkingPaymentStatus || paymentReloading) return;
    setCheckingPaymentStatus(true);
    try {
      const latest = (await contractApi.getContract(id)).data;
      const latestPayment = latest.escrowPayment;
      if (
        isEscrowPaymentConfirmed(latestPayment?.paymentStatus, latestPayment?.escrowStatus)
      ) {
        setPaymentReloading(true);
        showPaymentToast(
          'success',
          'Thanh toán đã được SePay xác nhận. Đang tải lại hợp đồng...',
          false,
        );
        window.setTimeout(() => window.location.reload(), 1000);
        return;
      }

      const statusText = latestPayment?.paymentStatus
        ? PAYMENT_STATUS_LABEL[latestPayment.paymentStatus] ?? latestPayment.paymentStatus
        : 'chưa có giao dịch';
      showPaymentToast(
        'warning',
        `Chưa ghi nhận thanh toán thành công. Trạng thái hiện tại: ${statusText}.`,
      );
    } catch (error) {
      showPaymentToast(
        error instanceof Error ? 'error' : 'warning',
        getApiMessage(error, 'Không quét được trạng thái thanh toán.'),
      );
    } finally {
      setCheckingPaymentStatus(false);
    }
  };

  if (loading) {
    return (
      <div className="contract-shell">
        <HomeNavbar />
        <main className="contract-page tcs-container">
          <div className="contract-state">Đang tải hợp đồng...</div>
        </main>
      </div>
    );
  }

  if (error) {
    return (
      <div className="contract-shell">
        <HomeNavbar />
        <main className="contract-page tcs-container">
          <div className="contract-state contract-state--error">{error}</div>
        </main>
      </div>
    );
  }

  if (!contract) return null;

  const status = STATUS_LABEL[contract.status] ?? { label: contract.status, cls: '' };
  const escrowPayment = contract.escrowPayment ?? null;
  const visibleEscrowPayment =
    escrowPayment
    && !isEscrowPaymentConfirmed(escrowPayment.paymentStatus, escrowPayment.escrowStatus)
      ? escrowPayment
      : null;
  const escrowStatus = visibleEscrowPayment
    ? ESCROW_STATUS_LABEL[visibleEscrowPayment.escrowStatus] ?? {
        label: visibleEscrowPayment.escrowStatus,
        cls: '',
      }
    : null;
  const escrowPending = visibleEscrowPayment?.paymentStatus === 'PENDING';
  const escrowRetryable =
    visibleEscrowPayment?.paymentStatus === 'FAILED' || visibleEscrowPayment?.paymentStatus === 'CANCELLED';
  const myPendingSlot =
    signatures?.signatures.some((s) => s.isCurrentUser && s.signatureStatus !== 'SIGNED') ?? false;
  const mySignedSlot =
    signatures?.signatures.some((s) => s.isCurrentUser && s.signatureStatus === 'SIGNED') ?? false;
  const allSigned = signatures?.fullySigned ?? false;
  const signRequired = contract.status === 'DRAFT' || contract.status === 'PENDING';
  const canCreateIssue = contract.classId != null;
  const selectedPayoutBank = findBankByName(payoutBankName);
  const needsRefundPayoutInfo =
    contract.sourceType === 'CENTER'
    && contract.classStudentId != null
    && Boolean(visibleEscrowPayment)
    && !contract.refundPayoutInfo;
  const canRequestRefund = contract.refundAllowed !== false;
  const classDetailUrl = contract.classId
    ? `/marketplace/classes/${contract.classId}${
        contract.classStudentId
          ? `?classStudentId=${contract.classStudentId}`
          : contract.assignmentId
            ? `?assignmentId=${contract.assignmentId}`
            : ''
      }`
    : null;
  const signedPercent = signatures?.totalRequired
    ? Math.round((signatures.signedCount / signatures.totalRequired) * 100)
    : 0;

  return (
    <div className="contract-shell">
      <HomeNavbar />
      <main className="contract-page contract-page--detail tcs-container">
        <div className="contract-detail-nav">
          <Link to={APP_ROUTES.contract} className="contract-back-link">
            Quay lại danh sách
          </Link>
          <span className={`contract-status ${status.cls}`}>{status.label}</span>
        </div>

        <section className="contract-detail-head">
          <div>
            <p className="contract-eyebrow">Chi tiết hợp đồng</p>
            <h1>{contract.classTitle ?? contract.contractNo}</h1>
            <p>
              <span className="contract-no">{contract.contractNo}</span>
              <span className="contract-dot" />
              Tạo ngày {formatDate(contract.createdAt)}
            </p>
          </div>
          <div className="contract-detail-head__actions">
            <button
              className="tcs-btn tcs-btn--ghost"
              type="button"
              disabled={!canCreateIssue}
              onClick={() => setIssueModalOpen(true)}
            >
              Báo cáo sự cố
            </button>
            <button
              className="tcs-btn tcs-btn--ghost"
              type="button"
              disabled={!canCreateIssue || !canRequestRefund}
              onClick={() => setRefundModalOpen(true)}
            >
              Yêu cầu hoàn tiền
            </button>
            {classDetailUrl ? (
              <Link
                className="tcs-btn tcs-btn--primary"
                to={classDetailUrl}
              >
                Xem lớp liên quan
              </Link>
            ) : null}
          </div>
          {!canRequestRefund ? (
            <p className="contract-muted">
              {contract.refundBlockedReason ?? 'Lớp center đã học quá 50% số buổi nên không thể yêu cầu hoàn tiền.'}
            </p>
          ) : null}
        </section>

        <div className="contract-detail-grid">
          <section className="contract-card contract-card--wide">
            <div className="contract-card__head">
              <h2>Thông tin hợp đồng</h2>
            </div>
            <dl className="contract-info-list">
              <div>
                <dt>Ngày ký</dt>
                <dd>{formatDate(contract.signedAt)}</dd>
              </div>
              {/* HĐ tuyển dụng/hợp tác gia sư: không có lớp -> ẩn các trường Loại lớp/Số buổi/Học phí. */}
              {contract.recruitmentApplicationId == null && (
                <>
                  <div>
                    <dt>Loại lớp</dt>
                    <dd>{contract.classType ?? '—'}</dd>
                  </div>
                  <div>
                    <dt>Hình thức</dt>
                    <dd>{contract.lessonMode ?? '—'}</dd>
                  </div>
                  <div>
                    <dt>Số buổi</dt>
                    <dd>{contract.numberOfSessions ?? '—'}</dd>
                  </div>
                  <div>
                    <dt>Học phí</dt>
                    <dd>{formatCurrency(contract.tuitionFee)}</dd>
                  </div>
                </>
              )}
            </dl>
            {contract.documentText || contract.termsSummary ? (
              <div className="contract-terms">
                <h3>Nội dung hợp đồng</h3>
                {/* Văn bản hợp đồng đầy đủ (quốc hiệu, BÊN A, BÊN B, điều khoản) -> giữ xuống dòng. */}
                <p style={{ whiteSpace: 'pre-wrap' }}>
                  {contract.documentText ?? contract.termsSummary}
                </p>
              </div>
            ) : null}
          </section>

          <section className="contract-card">
            <div className="contract-card__head">
              <h2>Các bên ký</h2>
            </div>
            <div className="contract-party-list">
              {contract.tutor ? (
                <div className="contract-party">
                  <span>Gia sư</span>
                  <strong>{contract.tutor.fullName}</strong>
                  <small>{contract.tutor.email}</small>
                </div>
              ) : null}
              {contract.center ? (
                <div className="contract-party">
                  <span>Trung tâm</span>
                  <strong>{contract.center.fullName}</strong>
                  <small>{contract.center.email}</small>
                </div>
              ) : null}
              {contract.client ? (
                <div className="contract-party">
                  <span>Phụ huynh / Học viên</span>
                  <strong>{contract.client.fullName}</strong>
                  <small>{contract.client.email}</small>
                </div>
              ) : null}
            </div>
          </section>

          {visibleEscrowPayment ? (
            needsRefundPayoutInfo ? (
              <section className="contract-card contract-escrow-card">
                <div className="contract-card__head">
                  <h2>Thông tin nhận hoàn tiền</h2>
                  {escrowStatus ? <span className={`contract-status ${escrowStatus.cls}`}>{escrowStatus.label}</span> : null}
                </div>
                <div className="contract-sign-form">
                  <p>
                    Vui lòng nhập tài khoản thụ hưởng của quý khách để phục vụ xử lý các nhu cầu phát sinh.
                  </p>
                  {payoutMessage ? <div className="contract-alert contract-alert--error">{payoutMessage}</div> : null}
                  <BankSelectField
                    id="contract-refund-payout-bank"
                    selectedBank={selectedPayoutBank}
                    onOpen={() => setPayoutDialogOpen(true)}
                  />
                  <input
                    type="text"
                    className="form-input"
                    placeholder="Tên chủ tài khoản"
                    value={payoutAccountHolder}
                    onChange={(event) => setPayoutAccountHolder(event.target.value)}
                  />
                  <input
                    type="text"
                    className="form-input"
                    inputMode="numeric"
                    placeholder="Số tài khoản"
                    value={payoutAccountNo}
                    onChange={(event) => setPayoutAccountNo(event.target.value.replace(/\s+/g, ''))}
                  />
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--primary"
                    onClick={handleSaveRefundPayout}
                    disabled={savingPayout}
                  >
                    {savingPayout ? 'Đang lưu...' : 'Lưu thông tin & tiếp tục'}
                  </button>
                </div>
              </section>
            ) : (
              <section className="contract-card contract-escrow-card">
                <div className="contract-card__head">
                  <h2>Quét mã để thanh toán</h2>
                  {escrowStatus ? <span className={`contract-status ${escrowStatus.cls}`}>{escrowStatus.label}</span> : null}
                </div>
                <div className="contract-escrow">
                  {visibleEscrowPayment.qrUrl ? (
                    <div className="contract-escrow__qr">
                      <img src={visibleEscrowPayment.qrUrl} alt="VietQR thanh toán escrow" />
                    </div>
                  ) : null}
                  <div className="contract-escrow__details">
                    {contract.refundPayoutInfo ? (
                      <p className="contract-escrow__note">
                        Đã lưu tài khoản nhận hoàn tiền cho lớp trung tâm.
                      </p>
                    ) : null}
                    <div className="contract-escrow__row">
                      <span>Số tiền</span>
                      <strong>{formatCurrency(visibleEscrowPayment.amount)}</strong>
                    </div>
                    <div className="contract-escrow__row">
                      <span>Ngân hàng</span>
                      <strong>{visibleEscrowPayment.bankName ?? '—'}</strong>
                    </div>
                    <div className="contract-escrow__row">
                      <span>Số tài khoản</span>
                      <strong>{visibleEscrowPayment.accountNumber ?? '—'}</strong>
                    </div>
                    <div className="contract-escrow__row">
                      <span>Chủ tài khoản</span>
                      <strong>{visibleEscrowPayment.accountName ?? '—'}</strong>
                    </div>
                    <div className="contract-escrow__code">
                      <span>Nội dung chuyển khoản</span>
                      <div>
                        <code>
                          {visibleEscrowPayment.transferContent ?? visibleEscrowPayment.referenceCode ?? '—'}
                        </code>
                        <button
                          type="button"
                          onClick={() =>
                            copyText(
                              visibleEscrowPayment.transferContent ?? visibleEscrowPayment.referenceCode,
                            )
                          }
                        >
                          Sao chép
                        </button>
                      </div>
                    </div>
                    <p className={`contract-escrow__note${escrowPending ? ' contract-escrow__note--pending' : ''}`}>
                      {escrowPending
                          ? contract.sourceType === 'CENTER'
                            ? 'Sau khi SePay xác nhận giao dịch, học viên sẽ được ghi danh chính thức. Lớp vẫn mở cho tới khi đủ sĩ số hoặc đến hạn đóng ghi danh.'
                            : 'Sau khi SePay xác nhận giao dịch, escrow sẽ chuyển sang đã nạp và lớp mới được kích hoạt.'
                          : escrowRetryable
                            ? 'Giao dịch chưa thành công. Vui lòng quét mã và chuyển khoản lại đúng nội dung.'
                            : `Trạng thái giao dịch: ${
                            visibleEscrowPayment.paymentStatus
                              ? PAYMENT_STATUS_LABEL[visibleEscrowPayment.paymentStatus] ?? visibleEscrowPayment.paymentStatus
                              : '—'
                          }.`}
                    </p>
                    <div className="contract-escrow__actions">
                      <button
                        type="button"
                        className="tcs-btn tcs-btn--primary contract-escrow__scan"
                        onClick={handleCheckEscrowPaymentStatus}
                        disabled={checkingPaymentStatus || paymentReloading}
                      >
                        {checkingPaymentStatus || paymentReloading ? 'Đang quét...' : 'Quét trạng thái'}
                      </button>
                    </div>
                  </div>
                </div>
              </section>
            )
          ) : null}

          <section className="contract-card">
            <div className="contract-card__head">
              <h2>Trạng thái ký</h2>
              {signatures ? <span>{signatures.signedCount}/{signatures.totalRequired}</span> : null}
            </div>
            {signatures ? (
              <div className="contract-signature">
                <div className="contract-progress">
                  <span style={{ width: `${signedPercent}%` }} />
                </div>
                <div className="contract-signature-list">
                  {signatures.signatures.map((signature) => {
                    const isSigned = signature.signatureStatus === 'SIGNED';
                    return (
                      <div
                        key={signature.signatureId}
                        className={`contract-signature-row${
                          signature.isCurrentUser ? ' contract-signature-row--me' : ''
                        }${isSigned ? '' : ' contract-signature-row--pending'}`}
                      >
                        <span className="contract-signature-check">{isSigned ? '✓' : '—'}</span>
                        <div>
                          <strong>
                            {signature.signerName ?? signature.partyLabel}
                            {signature.isCurrentUser ? <em>Bạn</em> : null}
                          </strong>
                          <small>
                            {signature.partyLabel} ·{' '}
                            {isSigned ? `Đã ký ${formatDateTime(signature.signedAt)}` : 'Chờ ký'}
                          </small>
                        </div>
                      </div>
                    );
                  })}
                </div>
                {allSigned ? <p className="contract-success-note">Đủ chữ ký.</p> : null}
              </div>
            ) : (
              <div className="contract-muted">Đang tải trạng thái ký...</div>
            )}
          </section>

          {signRequired && myPendingSlot ? (
            <section className="contract-card contract-card--accent">
              <div className="contract-card__head">
                <h2>Ký hợp đồng</h2>
              </div>
              {signError ? <div className="contract-alert contract-alert--error">{signError}</div> : null}

              {!otpSentSuccess ? (
                <div className="contract-sign-form">
                  <p>Hệ thống sẽ gửi mã OTP về email của bạn.</p>
                  <button
                    className="tcs-btn tcs-btn--primary"
                    type="button"
                    onClick={handleSendOtp}
                    disabled={sendingOtp}
                  >
                    {sendingOtp ? 'Đang gửi...' : 'Gửi mã OTP'}
                  </button>
                </div>
              ) : (
                <div className="contract-sign-form">
                  <p>
                    Mã OTP đã được gửi tới <strong>{otpSent?.maskedEmail}</strong>
                  </p>
                  <div className="contract-otp-row">
                    <input
                      type="text"
                      inputMode="numeric"
                      className="contract-otp-input"
                      placeholder="Nhập mã OTP"
                      value={otpInput}
                      onChange={(event) => setOtpInput(event.target.value.replace(/\D/g, '').slice(0, 6))}
                      maxLength={6}
                      autoFocus
                    />
                    <button
                      className="tcs-btn tcs-btn--primary"
                      type="button"
                      onClick={handleSign}
                      disabled={signing || otpInput.trim().length < 6}
                    >
                      {signing ? 'Đang ký...' : 'Xác nhận ký'}
                    </button>
                  </div>
                  <button
                    className="contract-link-button"
                    type="button"
                    onClick={handleSendOtp}
                    disabled={sendingOtp}
                  >
                    Gửi lại mã OTP
                  </button>
                </div>
              )}
            </section>
          ) : null}

          {mySignedSlot && !allSigned ? (
            <section className="contract-card contract-card--success">
              <h2>Đã ghi nhận chữ ký của bạn</h2>
              <p>Hợp đồng đang chờ bên còn lại ký.</p>
            </section>
          ) : null}

          {allSigned && contract.status !== 'SIGNED' && contract.status !== 'ACTIVE' ? (
            <section className="contract-card contract-card--success">
              <h2>Hoàn tất ký</h2>
              <p>Hợp đồng đã đủ chữ ký và có hiệu lực.</p>
            </section>
          ) : null}
        </div>
      </main>

      {contract.classId ? (
        <>
          <BankPickerDialog
            open={payoutDialogOpen}
            selectedBankCode={selectedPayoutBank?.code ?? ''}
            onSelect={(bank) => {
              setPayoutBankName(bank.name);
              setPayoutDialogOpen(false);
            }}
            onClose={() => setPayoutDialogOpen(false)}
          />
          <ClassIssueModal
            open={issueModalOpen}
            classId={contract.classId}
            assignmentId={contract.assignmentId}
            classStudentId={contract.classStudentId}
            classTitle={contract.classTitle}
            onClose={() => setIssueModalOpen(false)}
          />
          <RefundRequestModal
            open={refundModalOpen}
            classTitle={contract.classTitle}
            assignmentId={contract.assignmentId}
            classStudentId={contract.classStudentId}
            amountHint={contract.tuitionFee == null ? null : Number(contract.tuitionFee)}
            onClose={() => setRefundModalOpen(false)}
          />
        </>
      ) : null}
      {paymentToast ? (
        <div className={`contract-toast contract-toast--${paymentToast.tone}`} role="status">
          {paymentToast.message}
        </div>
      ) : null}
    </div>
  );
}
