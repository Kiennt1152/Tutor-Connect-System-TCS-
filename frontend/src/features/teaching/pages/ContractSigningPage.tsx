import { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { SiteHeader } from '../../home/components/SiteHeader';
import { contractApi } from '../../contract/api/contractApi';
import { BankPickerDialog, BankSelectField, findBankByName } from '../../finance/components/BankPicker';
import '../../finance/FinancePage.css';
import { teachingApi } from '../api/teachingApi';
import type { ContractView } from '../types/teachingTypes';
import type {
  EscrowPaymentInfo,
  EscrowStatus,
  PaymentTransactionStatus,
} from '../../contract/types/contractTypes';
import { classToForm, totalBudget, weeksForCycle } from '../../marketplace/mappers/marketplaceMapper';
import { DAY_OF_WEEK_OPTIONS } from '../../marketplace/types/marketplaceTypes';
import { APP_ROUTES } from '../../../shared/constants/routes';
import './ContractSigningPage.css';

const currency = new Intl.NumberFormat('vi-VN');

function extractError(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { message?: string } | undefined;
    if (data?.message) return data.message;
  }
  if (err instanceof Error) return err.message;
  return 'Có lỗi xảy ra. Vui lòng thử lại.';
}

function fmtDate(iso: string | null): string {
  if (!iso) return '.......';
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
}

function fmtDob(iso: string | null): string {
  if (!iso) return '.................';
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
}

const hm = (t: string) => (t === '23:59' ? '00:00' : t);

const ESCROW_STATUS_LABEL: Record<EscrowStatus, { label: string; cls: string }> = {
  PENDING: { label: 'Chờ nạp', cls: 'is-pending' },
  FUNDED: { label: 'Đã nạp', cls: 'is-success' },
  ON_HOLD: { label: 'Đang giữ', cls: 'is-warning' },
  DISPUTED: { label: 'Đang tranh chấp', cls: 'is-warning' },
  RELEASED: { label: 'Đã giải ngân', cls: 'is-success' },
  REFUNDED: { label: 'Đã hoàn tiền', cls: 'is-success' },
};

const PAYMENT_STATUS_LABEL: Record<PaymentTransactionStatus, string> = {
  PENDING: 'Chờ thanh toán',
  SUCCESS: 'Đã thanh toán',
  FAILED: 'Thanh toán thất bại',
  CANCELLED: 'Đã hủy',
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

const copyText = async (value?: string | null) => {
  if (!value) return;
  await navigator.clipboard?.writeText(value);
};

const DEFAULT_TERMS_B_LINES = [
  'Bảo đảm giờ học cho học viên đúng lịch; nếu nghỉ hoặc chuyển lịch học phải báo trước và dạy bù.',
  'Có giáo trình, giáo án theo đúng yêu cầu.',
  'Bảo đảm chất lượng và tiến độ học tập của học viên.',
  'Không được chuyển giao dịch vụ cho người thứ ba nếu chưa được Bên A chấp nhận.',
];

const LOCKED_DEFAULT_LINES = new Set<string>([
  ...DEFAULT_TERMS_B_LINES,
  'Bảo đảm giờ học cho học viên đúng lịch; nếu nghỉ phải báo trước và dạy bù.',
]);

function termsLines(text: string): string[] {
  return text
    .split('\n')
    .map((s) => s.trim())
    .filter(Boolean);
}

export default function ContractSigningPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const assignmentId = Number((location.state as { assignmentId?: number } | null)?.assignmentId);

  const [contract, setContract] = useState<ContractView | null>(null);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [loadError, setLoadError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [extraTermsText, setExtraTermsText] = useState<string>('');
  const [termsSaving, setTermsSaving] = useState(false);
  const [termsSaved, setTermsSaved] = useState(false);
  const [otpSent, setOtpSent] = useState(false);
  const [otp, setOtp] = useState('');
  const [otpRequesting, setOtpRequesting] = useState(false);
  const [otpMsg, setOtpMsg] = useState('');
  const [secondsLeft, setSecondsLeft] = useState(0);
  const [agreedTermsB, setAgreedTermsB] = useState(false);
  const [checkingPaymentStatus, setCheckingPaymentStatus] = useState(false);
  const [paymentReloading, setPaymentReloading] = useState(false);
  const [paymentToast, setPaymentToast] = useState<{
    tone: 'success' | 'warning' | 'error';
    message: string;
  } | null>(null);
  const [payoutBankName, setPayoutBankName] = useState('');
  const [payoutAccountNo, setPayoutAccountNo] = useState('');
  const [payoutAccountHolder, setPayoutAccountHolder] = useState('');
  const [payoutDialogOpen, setPayoutDialogOpen] = useState(false);
  const [savingPayout, setSavingPayout] = useState(false);
  const [payoutMessage, setPayoutMessage] = useState('');
  const today = new Date();

  useEffect(() => {
    if (!otpSent || secondsLeft <= 0) return;
    const t = setTimeout(() => setSecondsLeft((s) => Math.max(0, s - 1)), 1000);
    return () => clearTimeout(t);
  }, [otpSent, secondsLeft]);

  useEffect(() => {
    if (!assignmentId) {
      setLoadError('Thiếu mã lời mời nhận lớp.');
      setStatus('error');
      return;
    }
    teachingApi
      .getAssignmentContract(assignmentId)
      .then((data) => {
        setContract(data);
        const extra = termsLines(data.termsB ?? '').filter((l) => !LOCKED_DEFAULT_LINES.has(l));
        setExtraTermsText(extra.join('\n'));
        setStatus('success');
      })
      .catch((err) => {
        setLoadError(extractError(err));
        setStatus('error');
      });
  }, [assignmentId]);

  useEffect(() => {
    if (!contract) return;
    setPayoutMessage('');
    if (contract.refundPayoutInfo) {
      setPayoutBankName(contract.refundPayoutInfo.bankName ?? '');
      setPayoutAccountHolder(contract.refundPayoutInfo.accountHolderName ?? '');
      setPayoutAccountNo('');
      return;
    }
    setPayoutBankName('');
    setPayoutAccountHolder('');
    setPayoutAccountNo('');
  }, [contract]);

  const form = useMemo(
    () => (contract?.detailsJson ? classToForm({ detailsJson: contract.detailsJson } as never) : null),
    [contract?.detailsJson],
  );

  const amounts = useMemo(() => {
    if (!form) return { full: 0, monthly: 0, months: 1 };
    const full = totalBudget(form);
    const months = Math.max(1, Math.round(weeksForCycle(form) / 4));
    const monthly = months > 1 ? Math.round(full / months) : full;
    return { full, monthly, months };
  }, [form]);

  const escrowPayment = contract?.escrowPayment ?? null;
  const visibleEscrowPayment: EscrowPaymentInfo | null =
    escrowPayment && !isEscrowPaymentConfirmed(escrowPayment.paymentStatus, escrowPayment.escrowStatus)
      ? escrowPayment
      : null;
  const escrowStatus =
    escrowPayment && !isEscrowPaymentConfirmed(escrowPayment.paymentStatus, escrowPayment.escrowStatus)
      ? ESCROW_STATUS_LABEL[escrowPayment.escrowStatus]
      : null;
  const escrowPending = escrowPayment?.paymentStatus === 'PENDING' || escrowPayment?.escrowStatus === 'PENDING';
  const escrowRetryable = escrowPayment?.paymentStatus === 'FAILED' || escrowPayment?.paymentStatus === 'PENDING';

  const isTutor = contract?.myRole === 'TUTOR';
  const isClient = contract?.myRole === 'CLIENT';
  const alreadySignedByMe = isTutor ? contract?.tutorSigned : contract?.clientSigned;
  const bothSigned = !!contract?.tutorSigned && !!contract?.clientSigned;
  const myCccdMissing = isTutor ? !contract?.tutorCccd : !contract?.clientCccd;

  const dayLabel = (v: string) => DAY_OF_WEEK_OPTIONS.find((d) => d.value === v)?.label ?? v;

  const combinedTermsLines = [...DEFAULT_TERMS_B_LINES, ...termsLines(extraTermsText)];
  const combinedTermsB = combinedTermsLines.join('\n');
  const selectedPayoutBank = findBankByName(payoutBankName);

  const showPaymentToast = (tone: 'success' | 'warning' | 'error', message: string, autoClose = true) => {
    setPaymentToast({ tone, message });
    if (autoClose) {
      window.setTimeout(() => setPaymentToast(null), 4200);
    }
  };

  const handleCheckEscrowPaymentStatus = async () => {
    if (!assignmentId || checkingPaymentStatus || paymentReloading) return;
    setCheckingPaymentStatus(true);
    try {
      const latest = await teachingApi.getAssignmentContract(assignmentId);
      const latestPayment = latest.escrowPayment;
      if (latestPayment && isEscrowPaymentConfirmed(latestPayment.paymentStatus, latestPayment.escrowStatus)) {
        setPaymentReloading(true);
        showPaymentToast('success', 'Thanh toán đã được SePay xác nhận. Đang tải lại hợp đồng...', false);
        window.setTimeout(() => window.location.reload(), 1000);
        return;
      }

      const statusText = latestPayment?.paymentStatus
        ? PAYMENT_STATUS_LABEL[latestPayment.paymentStatus] ?? latestPayment.paymentStatus
        : 'chưa có giao dịch';
      showPaymentToast(
        'warning',
        `Chưa xác nhận được thanh toán. Trạng thái hiện tại: ${statusText}.`,
      );
      setContract(latest);
    } catch (err) {
      showPaymentToast('error', extractError(err));
    } finally {
      setCheckingPaymentStatus(false);
    }
  };

  async function handleSaveRefundPayout() {
    if (!contract?.contractId) return;
    const normalizedAccountNo = payoutAccountNo.trim().replace(/\s+/g, '');
    if (!payoutBankName.trim() || !normalizedAccountNo || !payoutAccountHolder.trim()) {
      setPayoutMessage('Vui lòng chọn ngân hàng, nhập số tài khoản và tên chủ tài khoản.');
      return;
    }

    setSavingPayout(true);
    setPayoutMessage('');
    try {
      await contractApi.saveRefundPayoutInfo(contract.contractId, {
        bankName: payoutBankName.trim(),
        accountNo: normalizedAccountNo,
        accountHolderName: payoutAccountHolder.trim(),
      });
      const latest = await teachingApi.getAssignmentContract(assignmentId);
      setContract(latest);
      setPayoutMessage('Đã lưu thông tin nhận hoàn tiền.');
    } catch (err) {
      setPayoutMessage(extractError(err));
    } finally {
      setSavingPayout(false);
    }
  }

  async function handleSaveTerms() {
    if (!contract) return;
    setTermsSaving(true);
    setError(null);
    try {
      await teachingApi.saveContractTerms(contract.assignmentId, combinedTermsB);
      setTermsSaved(true);
      setTimeout(() => setTermsSaved(false), 2500);
    } catch (err) {
      setError(extractError(err));
    } finally {
      setTermsSaving(false);
    }
  }

  async function handleRequestOtp() {
    if (!contract) return;
    setOtpRequesting(true);
    setError(null);
    try {
      if (isClient) {
        await teachingApi.saveContractTerms(contract.assignmentId, combinedTermsB);
      }
      await teachingApi.requestSignOtp(contract.assignmentId);
      setOtpSent(true);
      setOtp('');
      setSecondsLeft(30);
      setOtpMsg('Mã OTP đã được gửi tới email của bạn. Mã có hiệu lực trong 30 giây.');
    } catch (err) {
      setError(extractError(err));
    } finally {
      setOtpRequesting(false);
    }
  }

  async function handleSign() {
    if (!contract) return;
    if (otp.trim().length < 6) {
      setError('Vui lòng nhập mã OTP gồm 6 số đã gửi tới email của bạn.');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await teachingApi.signAssignmentContract(contract.assignmentId, otp.trim());
      // Ký xong -> tải lại hợp đồng ngay tại chỗ để văn bản phản ánh "Đã ký"
      // (thay vì chuyển trang luôn, người ký sẽ thấy chữ ký của mình được ghi nhận).
      const updated = await teachingApi.getAssignmentContract(contract.assignmentId);
      setContract(updated);
      setOtpSent(false);
      setOtp('');
      setOtpMsg('');
    } catch (err) {
      setError(extractError(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="ksign-page">
      <SiteHeader />
      <main className="tcs-container ksign-main">
        <div className="ksign-bar">
          <button type="button" className="ksign-btn ksign-btn--ghost" onClick={() => navigate(APP_ROUTES.teaching)}>
            ← Quay lại lịch dạy
          </button>
          <h1 className="ksign-h1">Ký hợp đồng làm gia sư</h1>
        </div>

        {status === 'loading' && <div className="ksign-state">Đang tải hợp đồng…</div>}
        {status === 'error' && <div className="ksign-state ksign-state--err">{loadError}</div>}

        {status === 'success' && contract && form && (
          <div className="ksign-grid">
            <article className="ksign-doc">
              <div className="ksign-doc__center">
                <p className="ksign-doc__title">CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</p>
                <p className="ksign-doc__sub">Độc lập - Tự do - Hạnh phúc</p>
                <p className="ksign-doc__hr">———</p>
                <h2 className="ksign-doc__name">HỢP ĐỒNG LÀM GIA SƯ</h2>
              </div>

              <p className="ksign-doc__p">
                Hôm nay, ngày {today.getDate()} tháng {today.getMonth() + 1} năm{' '}
                {today.getFullYear()}, chúng tôi gồm có:
              </p>

              <div className="ksign-party">
                <h3>Bên A: (Phụ huynh/Học sinh)</h3>
                <p>Họ và tên: <b>{contract.clientName || '.................'}</b></p>
                <p>Ngày sinh: {fmtDob(contract.clientDob)}</p>
                <p>Số CCCD: {contract.clientCccd || '.................'}</p>
                <p>Địa chỉ: {contract.clientAddress || '.................'}</p>
                <p>Điện thoại: {contract.clientPhone || '.................'}</p>
              </div>

              <div className="ksign-party">
                <h3>Bên B: (Gia sư)</h3>
                <p>Họ và tên: <b>{contract.tutorName || '.................'}</b></p>
                <p>Ngày sinh: {fmtDob(contract.tutorDob)}</p>
                <p>Số CCCD: {contract.tutorCccd || '.................'}</p>
                <p>Địa chỉ: {contract.tutorAddress || '.................'}</p>
                <p>Điện thoại: {contract.tutorPhone || '.................'}</p>
              </div>

              <p className="ksign-doc__p">
                Sau khi trao đổi và bàn bạc, hai bên đi đến thống nhất lập hợp đồng dịch vụ với nội
                dung và điều khoản sau:
              </p>

              <h4 className="ksign-art">Điều 1: Đối tượng hợp đồng</h4>
              <p>
                Bên A đồng ý để Bên B làm gia sư dạy kèm các môn:{' '}
                <b>{(contract.subjectNames ?? []).join(', ') || '—'}</b>
                {contract.gradeName ? ` (${contract.gradeName})` : ''} với yêu cầu sau:
              </p>
              <ul className="ksign-ul">
                <li>
                  Địa điểm học:{' '}
                  {contract.lessonMode === 'ONLINE' ? 'Học trực tuyến (Online)' : `tại ${contract.address || '.......'}`}
                </li>
                <li>
                  Thời gian học: từ {fmtDate(contract.startDate)} đến {fmtDate(contract.endDate)}
                  {` (${amounts.months} tháng)`}.
                </li>
                <li>Lịch học cụ thể theo từng môn:</li>
              </ul>
              <ul className="ksign-ul ksign-ul--sched">
                {form.subjectIds.map((sid, i) => {
                  const rows = form.slots.filter((s) => s.subjectId === sid);
                  const name = contract.subjectNames?.[i] ?? `Môn ${i + 1}`;
                  return (
                    <li key={sid}>
                      <b>{name}</b>:{' '}
                      {rows.length === 0
                        ? '—'
                        : rows
                            .map((s) =>
                              form.scheduleMode === 'WEEKLY'
                                ? `${dayLabel(s.day)} ${hm(s.start)}–${hm(s.end)}`
                                : `${s.date} ${hm(s.start)}–${hm(s.end)}`,
                            )
                            .join(' · ')}
                    </li>
                  );
                })}
              </ul>

              <h4 className="ksign-art">Điều 2: Thù lao và phương thức thanh toán</h4>
              <ul className="ksign-ul">
                {form.subjectIds.map((sid, i) => (
                  <li key={sid}>
                    {contract.subjectNames?.[i] ?? `Môn ${i + 1}`}:{' '}
                    <b>{currency.format(Number(form.subjectFees[sid]) || 0)} đồng</b> / giờ
                  </li>
                ))}
                <li>
                  Tổng học phí toàn khóa ({amounts.months} tháng):{' '}
                  <b>{currency.format(amounts.full)} đồng</b>
                  {amounts.months > 1 && (
                    <> · Mỗi tháng khoảng <b>{currency.format(amounts.monthly)} đồng</b></>
                  )}
                  .
                </li>
                <li>Đồng tiền thanh toán: đồng Việt Nam. Hình thức: chuyển khoản.</li>
              </ul>

              <h4 className="ksign-art">Điều 3: Nghĩa vụ của Bên A</h4>
              <ul className="ksign-ul">
                <li>Đôn đốc, nhắc nhở con em mình học đúng giờ.</li>
                <li>Bảo đảm địa điểm, thời gian, dụng cụ giảng dạy và phương tiện học tập.</li>
                <li>Thanh toán tiền thù lao cho Bên B đầy đủ và đúng hạn.</li>
              </ul>

              <h4 className="ksign-art">Điều 4: Nghĩa vụ của Bên B</h4>
              {isClient && !alreadySignedByMe && !myCccdMissing ? (
                <div className="ksign-terms-edit">
                  <ul className="ksign-ul">
                    {DEFAULT_TERMS_B_LINES.map((line, i) => (
                      <li key={i}>{line}</li>
                    ))}
                  </ul>
                  <p className="ksign-terms-edit__hint">
                    Bạn (Bên A) chỉ có thể <b>thêm</b> nghĩa vụ khác cho gia sư — mỗi dòng là một mục.
                  </p>
                  <textarea
                    className="ksign-terms-edit__area"
                    rows={4}
                    value={extraTermsText}
                    onChange={(e) => setExtraTermsText(e.target.value)}
                    placeholder="Thêm nghĩa vụ khác của Bên B (không bắt buộc)…"
                  />
                  <div className="ksign-terms-edit__row">
                    <button
                      type="button"
                      className="ksign-btn ksign-btn--ghost"
                      disabled={termsSaving}
                      onClick={handleSaveTerms}
                    >
                      {termsSaving ? 'Đang lưu…' : 'Lưu điều khoản'}
                    </button>
                    <button
                      type="button"
                      className="ksign-btn ksign-btn--ghost"
                      onClick={() => setExtraTermsText('')}
                    >
                      Xóa mục đã thêm
                    </button>
                    {termsSaved && <span className="ksign-terms-edit__ok">✔ Đã lưu</span>}
                  </div>
                </div>
              ) : (
                <>
                  <ul className="ksign-ul">
                    {combinedTermsLines.map((line, i) => (
                      <li key={i}>{line}</li>
                    ))}
                  </ul>
                  {isClient && myCccdMissing && (
                    <div className="ksign-alert ksign-alert--err">
                      Cập nhật CCCD trong hồ sơ để được chỉnh sửa điều khoản này.
                    </div>
                  )}
                </>
              )}

              <h4 className="ksign-art">Điều 5: Thời gian có hiệu lực</h4>
              <p>
                Hợp đồng có hiệu lực kể từ ngày hai bên ký và được lập thành hai bản, mỗi bên giữ một
                bản để theo dõi thực hiện.
              </p>

              <div className="ksign-sign-row">
                <div>
                  <b>ĐẠI DIỆN BÊN A</b>
                  <p className="ksign-sign-status">{contract.clientSigned ? '✔ Đã ký' : '(Chưa ký)'}</p>
                </div>
                <div>
                  <b>ĐẠI DIỆN BÊN B</b>
                  <p className="ksign-sign-status">{contract.tutorSigned ? '✔ Đã ký' : '(Chưa ký)'}</p>
                </div>
              </div>
            </article>

            <aside className="ksign-side">
              <div className="ksign-card">
                <h3>Xác nhận &amp; ký</h3>
                <p className="ksign-muted">
                  Bạn ký với tư cách <b>{isTutor ? 'Gia sư (Bên B)' : 'Phụ huynh/Học sinh (Bên A)'}</b>.
                </p>

                {error && <div className="ksign-alert ksign-alert--err">{error}</div>}

                {bothSigned ? (
                  escrowPayment ? (
                    isEscrowPaymentConfirmed(
                      escrowPayment.paymentStatus,
                      escrowPayment.escrowStatus,
                    ) ? (
                      <div className="ksign-alert ksign-alert--ok">
                        Thanh toán escrow đã được xác nhận. Hệ thống sẽ kích hoạt lớp.
                      </div>
                    ) : (
                      <div className="ksign-alert ksign-alert--ok">
                        Hợp đồng đã hoàn tất. Vui lòng quét mã để thanh toán escrow.
                      </div>
                    )
                  ) : (
                    <div className="ksign-alert ksign-alert--ok">
                      Hợp đồng đã hoàn tất. Đang tạo lệnh thanh toán escrow.
                    </div>
                  )
                ) : alreadySignedByMe ? (
                  <div className="ksign-alert ksign-alert--ok">
                    Bạn đã ký. Đang chờ {isTutor ? 'phụ huynh' : 'gia sư'} ký để hoàn tất.
                  </div>
                ) : isTutor && !contract.clientSigned ? (
                  <div className="ksign-alert ksign-alert--err">
                    Bên A (phụ huynh/học sinh) phải ký hợp đồng trước. Khi Bên A ký xong, bạn sẽ nhận
                    được thông báo để vào ký.
                  </div>
                ) : myCccdMissing ? (
                  <div className="ksign-alert ksign-alert--err">
                    Bạn chưa có Căn cước công dân (CCCD) trong hồ sơ nên chưa thể ký hợp đồng.{' '}
                    <Link to={APP_ROUTES.profile}>Cập nhật hồ sơ →</Link>
                  </div>
                ) : !otpSent ? (
                  <>
                    <label className="ksign-agree">
                      <input
                        type="checkbox"
                        checked={agreedTermsB}
                        onChange={(e) => setAgreedTermsB(e.target.checked)}
                      />
                      <span>
                        Tôi đã đọc và đồng ý với <b>Điều 4 – Nghĩa vụ của Bên B</b> trong hợp đồng.
                      </span>
                    </label>
                    <button
                      type="button"
                      className="ksign-btn ksign-btn--primary ksign-btn--block"
                      disabled={otpRequesting || !agreedTermsB}
                      onClick={handleRequestOtp}
                    >
                      {otpRequesting ? 'Đang gửi mã…' : 'Gửi mã OTP để ký'}
                    </button>
                    {!agreedTermsB && (
                      <p className="ksign-note">
                        Vui lòng xem kỹ và tích xác nhận Điều 4 để được gửi mã OTP.
                      </p>
                    )}
                  </>
                ) : (
                  <div className="ksign-otp">
                    {otpMsg && <div className="ksign-alert ksign-alert--ok">{otpMsg}</div>}
                    <label className="ksign-otp__label" htmlFor="ksign-otp-input">
                      Nhập mã OTP (6 số) gửi tới email của bạn
                    </label>
                    <input
                      id="ksign-otp-input"
                      className="ksign-otp__input"
                      inputMode="numeric"
                      autoComplete="one-time-code"
                      maxLength={6}
                      value={otp}
                      disabled={secondsLeft <= 0}
                      onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                      placeholder="______"
                    />
                    {secondsLeft > 0 ? (
                      <p className="ksign-otp__timer">
                        Mã hết hạn sau: <b>{secondsLeft}s</b>
                      </p>
                    ) : (
                      <p className="ksign-otp__timer ksign-otp__timer--exp">
                        Mã đã hết hạn. Vui lòng bấm "Gửi lại mã".
                      </p>
                    )}
                    <button
                      type="button"
                      className="ksign-btn ksign-btn--primary ksign-btn--block"
                      disabled={submitting || otp.trim().length < 6 || secondsLeft <= 0}
                      onClick={handleSign}
                    >
                      {submitting ? 'Đang ký…' : 'Xác nhận & ký hợp đồng'}
                    </button>
                    <button
                      type="button"
                      className="ksign-btn ksign-btn--ghost ksign-btn--block"
                      disabled={otpRequesting}
                      onClick={handleRequestOtp}
                    >
                      {otpRequesting ? 'Đang gửi…' : 'Gửi lại mã'}
                    </button>
                  </div>
                )}

                <p className="ksign-note">
                  Khi cả hai bên ký xong, hệ thống chuyển sang bước thanh toán escrow.
                </p>
              </div>

              {bothSigned && visibleEscrowPayment ? (
                contract.refundPayoutInfo == null && isClient ? (
                  <div className="ksign-card ksign-paycard">
                    <div className="ksign-paycard__head">
                      <h3>Thông tin nhận hoàn tiền</h3>
                      {escrowStatus ? (
                        <span className={`ksign-status ${escrowStatus.cls}`}>{escrowStatus.label}</span>
                      ) : null}
                    </div>
                    <div className="ksign-payout">
                      <p className="ksign-payout__intro">
                        Vui lòng nhập tài khoản thụ hưởng của quý khách để phục vụ xử lý các nhu cầu phát sinh.
                      </p>
                      {payoutMessage ? <div className="ksign-alert ksign-alert--err">{payoutMessage}</div> : null}
                      <div className="ksign-payout__field">
                        <span>Ngân hàng nhận hoàn tiền</span>
                        <BankSelectField
                          id="contract-refund-payout-bank"
                          selectedBank={selectedPayoutBank}
                          onOpen={() => setPayoutDialogOpen(true)}
                        />
                      </div>
                      <label className="ksign-payout__field">
                        <span>Tên chủ tài khoản</span>
                        <input
                          className="ksign-payout__input"
                          type="text"
                          value={payoutAccountHolder}
                          onChange={(event) => setPayoutAccountHolder(event.target.value)}
                          placeholder="Nhập tên chủ tài khoản"
                        />
                      </label>
                      <label className="ksign-payout__field">
                        <span>Số tài khoản</span>
                        <input
                          className="ksign-payout__input"
                          type="text"
                          inputMode="numeric"
                          value={payoutAccountNo}
                          onChange={(event) => setPayoutAccountNo(event.target.value.replace(/\s+/g, ''))}
                          placeholder="Nhập số tài khoản"
                        />
                      </label>
                      <button
                        type="button"
                        className="ksign-btn ksign-btn--primary ksign-btn--block"
                        onClick={handleSaveRefundPayout}
                        disabled={savingPayout}
                      >
                        {savingPayout ? 'Đang lưu...' : 'Lưu thông tin & tiếp tục'}
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="ksign-card ksign-paycard">
                    <div className="ksign-paycard__head">
                      <h3>Quét mã để thanh toán</h3>
                      {escrowStatus ? (
                        <span className={`ksign-status ${escrowStatus.cls}`}>{escrowStatus.label}</span>
                      ) : null}
                    </div>

                    <div className="ksign-escrow">
                      {visibleEscrowPayment.qrUrl ? (
                        <div className="ksign-escrow__qr">
                          <img src={visibleEscrowPayment.qrUrl} alt="Mã QR thanh toán escrow" />
                        </div>
                      ) : null}

                      <div className="ksign-escrow__details">
                        {contract.refundPayoutInfo ? (
                          <p className="ksign-escrow__saved-note">
                            Đã lưu tài khoản nhận hoàn tiền cho hợp đồng này.
                          </p>
                        ) : null}
                        <div className="ksign-escrow__row">
                          <span>Số tiền</span>
                          <strong>{currency.format(Number(visibleEscrowPayment.amount ?? 0))} đồng</strong>
                        </div>
                        <div className="ksign-escrow__row">
                          <span>Ngân hàng</span>
                          <strong>{visibleEscrowPayment.bankName ?? '—'}</strong>
                        </div>
                        <div className="ksign-escrow__row">
                          <span>Số tài khoản</span>
                          <strong>{visibleEscrowPayment.accountNumber ?? '—'}</strong>
                        </div>
                        <div className="ksign-escrow__row">
                          <span>Chủ tài khoản</span>
                          <strong>{visibleEscrowPayment.accountName ?? '—'}</strong>
                        </div>
                        <div className="ksign-escrow__code">
                          <span>Nội dung chuyển khoản</span>
                          <div>
                            <code>
                              {visibleEscrowPayment.transferContent ?? visibleEscrowPayment.referenceCode ?? '—'}
                            </code>
                            <button
                              type="button"
                              onClick={() =>
                                void copyText(
                                  visibleEscrowPayment.transferContent ?? visibleEscrowPayment.referenceCode,
                                )
                              }
                            >
                              Sao chép
                            </button>
                          </div>
                        </div>
                        <p className={`ksign-note${escrowPending ? ' ksign-note--pending' : ''}`}>
                          {escrowPending
                            ? 'Sau khi SePay xác nhận giao dịch, lớp sẽ được kích hoạt và mã QR sẽ tự ẩn.'
                            : escrowRetryable
                              ? `Giao dịch chưa thành công. Trạng thái hiện tại: ${
                                visibleEscrowPayment.paymentStatus
                                  ? PAYMENT_STATUS_LABEL[visibleEscrowPayment.paymentStatus] ?? visibleEscrowPayment.paymentStatus
                                  : '—'
                              }.`
                              : `Trạng thái giao dịch: ${
                                visibleEscrowPayment.paymentStatus
                                  ? PAYMENT_STATUS_LABEL[visibleEscrowPayment.paymentStatus] ?? visibleEscrowPayment.paymentStatus
                                  : '—'
                              }.`}
                        </p>
                        <div className="ksign-escrow__actions">
                          <button
                            type="button"
                            className="ksign-btn ksign-btn--primary ksign-btn--block"
                            onClick={handleCheckEscrowPaymentStatus}
                            disabled={checkingPaymentStatus || paymentReloading}
                          >
                            {checkingPaymentStatus || paymentReloading ? 'Đang quét...' : 'Quét trạng thái'}
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                )
              ) : bothSigned ? (
                <div className="ksign-card ksign-paycard">
                  <p className="ksign-muted">Đang tạo lệnh thanh toán escrow…</p>
                </div>
              ) : null}

              <BankPickerDialog
                open={payoutDialogOpen}
                selectedBankCode={selectedPayoutBank?.code ?? ''}
                onSelect={(bank) => {
                  setPayoutBankName(bank.name);
                  setPayoutDialogOpen(false);
                }}
                onClose={() => setPayoutDialogOpen(false)}
              />

              {paymentToast ? (
                <div className={`ksign-toast ksign-toast--${paymentToast.tone}`} role="status">
                  {paymentToast.message}
                </div>
              ) : null}
            </aside>
          </div>
        )}
      </main>
    </div>
  );
}
