import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { SiteFooter } from '../components/SiteFooter';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { hasRole } from '../../../shared/auth/rbac';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { marketplaceApi } from '../../marketplace/api/marketplaceApi';
import type {
  CenterRequestFeePayment,
  CenterSummary,
  ClassRequestPayload,
} from '../../marketplace/types/marketplaceTypes';
import { ClassRequestForm } from '../../marketplace/components/ClassRequestForm';
import { emptyForm } from '../../marketplace/mappers/marketplaceMapper';
import {
  BANK_OPTIONS,
  BankPickerDialog,
  BankSelectField,
  findBankByName,
  type BankOption,
} from '../../finance/components/BankPicker';
import { profileApi } from '../../profile/api/profileApi';
import { useTutorRequestForm } from '../hooks/useTutorRequestForm';
import './HomePage.css';
import './CentersRequest.css';

function extractError(error: unknown, fallback: string): string {
  const e = error as { response?: { data?: { message?: string } } };
  return e?.response?.data?.message ?? fallback;
}

function formatMoney(value: number | null | undefined): string {
  if (typeof value !== 'number') return '—';
  return `${new Intl.NumberFormat('vi-VN').format(value)} đ`;
}

function normalizeAccountNo(value: string): string {
  return value.trim().replace(/\s+/g, '');
}

const PAYOUT_STORAGE_PREFIX = 'tcs-center-request-payout-info:';

/**
 * Trang "Trung tâm":
 * - Mọi người: danh sách trung tâm đã xác minh.
 * - Phụ huynh (CLIENT): gửi yêu cầu mở lớp tới một trung tâm + theo dõi yêu cầu đã gửi.
 *
 * Gia sư xem/ứng tuyển tin tuyển dụng ở trang "Tin tuyển dụng" riêng (không lặp ở đây).
 */
export default function CentersPage() {
  const { user } = useAuth();
  const isClient = hasRole(user?.role, 'CLIENT');

  const [centers, setCenters] = useState<CenterSummary[]>([]);
  const [centersLoading, setCentersLoading] = useState(true);

  useEffect(() => {
    marketplaceApi
      .listCenters()
      .then((res) => setCenters(res.data))
      .catch(() => setCenters([]))
      .finally(() => setCentersLoading(false));
  }, []);

  // ----- Modal gửi yêu cầu (dùng lại form "tìm gia sư" cho rõ ràng) -----
  const { subjects, grades } = useTutorRequestForm();
  const [target, setTarget] = useState<CenterSummary | null>(null);
  const [sending, setSending] = useState(false);
  const [modalError, setModalError] = useState('');
  // Chỉ phụ huynh đã nhập đủ CCCD mới được gửi yêu cầu.
  const [cccdComplete, setCccdComplete] = useState<boolean | null>(null);
  // Thông báo thành công (toast trong app, không dùng alert trình duyệt).
  const [notice, setNotice] = useState('');
  const [paymentRequest, setPaymentRequest] = useState<CenterRequestFeePayment | null>(null);
  const [checkingPayment, setCheckingPayment] = useState(false);
  const [payoutBankCode, setPayoutBankCode] = useState('');
  const [payoutAccountNo, setPayoutAccountNo] = useState('');
  const [payoutAccountHolderName, setPayoutAccountHolderName] = useState('');
  const [payoutPickerOpen, setPayoutPickerOpen] = useState(false);
  const [payoutLoaded, setPayoutLoaded] = useState(false);
  const payoutStorageKey = user?.userId ? `${PAYOUT_STORAGE_PREFIX}${user.userId}` : null;
  const selectedPayoutBank = useMemo(
    () => BANK_OPTIONS.find((bank) => bank.code === payoutBankCode),
    [payoutBankCode],
  );

  useEffect(() => {
    if (!isClient) return;
    profileApi
      .getMyCccd()
      .then((res) => setCccdComplete(Boolean(res.data.complete)))
      .catch(() => setCccdComplete(false));
  }, [isClient]);

  useEffect(() => {
    setPayoutLoaded(false);
    setPayoutBankCode('');
    setPayoutAccountNo('');
    setPayoutAccountHolderName('');
    if (!payoutStorageKey) {
      setPayoutLoaded(true);
      return;
    }
    try {
      const raw = window.localStorage.getItem(payoutStorageKey);
      if (raw) {
        const saved = JSON.parse(raw) as {
          bankName?: string;
          accountNo?: string;
          accountHolderName?: string;
        };
        const savedBank = findBankByName(saved.bankName);
        setPayoutBankCode(savedBank?.code ?? '');
        setPayoutAccountNo(saved.accountNo ?? '');
        setPayoutAccountHolderName(saved.accountHolderName ?? '');
      }
    } catch {
      // Bỏ qua dữ liệu localStorage hỏng.
    } finally {
      setPayoutLoaded(true);
    }
  }, [payoutStorageKey]);

  useEffect(() => {
    if (!payoutStorageKey || !payoutLoaded) return;
    window.localStorage.setItem(
      payoutStorageKey,
      JSON.stringify({
        bankName: selectedPayoutBank?.name ?? '',
        accountNo: payoutAccountNo,
        accountHolderName: payoutAccountHolderName,
      }),
    );
  }, [
    payoutAccountHolderName,
    payoutAccountNo,
    payoutLoaded,
    payoutStorageKey,
    selectedPayoutBank?.name,
  ]);

  const openModal = (center: CenterSummary) => {
    setTarget(center);
    setModalError('');
    setPaymentRequest(null);
    setCheckingPayment(false);
  };
  const closeModal = () => {
    setTarget(null);
    setPaymentRequest(null);
    setCheckingPayment(false);
    setPayoutPickerOpen(false);
    setModalError('');
  };

  // Gửi yêu cầu tới trung tâm: đính nguyên payload form vào detailsJson để trung tâm xem đủ.
  const submitRequest = async (payload: ClassRequestPayload) => {
    if (!target) return;
    const normalizedAccountNo = normalizeAccountNo(payoutAccountNo);
    if (!selectedPayoutBank) {
      setModalError('Vui lòng chọn ngân hàng nhận hoàn tiền.');
      return;
    }
    if (!/^[A-Za-z0-9]{4,50}$/.test(normalizedAccountNo)) {
      setModalError('Số tài khoản chỉ gồm chữ/số và dài từ 4 đến 50 ký tự.');
      return;
    }
    if (payoutAccountHolderName.trim().length < 2) {
      setModalError('Vui lòng nhập tên chủ tài khoản nhận hoàn tiền.');
      return;
    }
    setSending(true);
    setModalError('');
    try {
      const note =
        payload.description?.trim() || 'Yêu cầu tìm gia sư (xem thông tin chi tiết đính kèm).';
      const response = await marketplaceApi.createClassRequest(target.centerId, {
        note,
        desiredBudget: payload.budget ?? payload.tuitionFee ?? null,
        detailsJson: JSON.stringify(payload),
        refundPayoutInfo: {
          bankName: selectedPayoutBank.name,
          accountNo: normalizedAccountNo,
          accountHolderName: payoutAccountHolderName.trim().replace(/\s+/g, ' '),
        },
      });
      if (response.centerRequestFeePayment) {
        setPaymentRequest(response.centerRequestFeePayment);
        setNotice('Đã tạo mã thanh toán phí xử lý. Vui lòng chuyển khoản và quét trạng thái.');
      } else {
        setTarget(null);
        setNotice('Đã gửi yêu cầu nhờ trung tâm tìm gia sư. Theo dõi ở trang “Yêu cầu của tôi”.');
      }
      window.setTimeout(() => setNotice(''), 6000);
    } catch (err) {
      setModalError(extractError(err, 'Không gửi được yêu cầu.'));
    } finally {
      setSending(false);
    }
  };

  const handleSelectPayoutBank = (bank: BankOption) => {
    setPayoutBankCode(bank.code);
    setPayoutPickerOpen(false);
  };

  const checkPaymentStatus = async () => {
    if (!paymentRequest) return;
    setCheckingPayment(true);
    setModalError('');
    try {
      const requests = await marketplaceApi.getMyClassRequests();
      const current = requests.find((item) => item.requestId === paymentRequest.requestId);
      const latestPayment = current?.centerRequestFeePayment ?? paymentRequest;
      setPaymentRequest(latestPayment);
      if (current && current.status !== 'PAYMENT_PENDING') {
        setNotice('Thanh toán thành công. Yêu cầu đã được gửi tới trung tâm.');
        window.setTimeout(() => setNotice(''), 6000);
      } else if (latestPayment.status === 'PENDING_PAYMENT') {
        setModalError('Chưa ghi nhận thanh toán. Vui lòng kiểm tra lại sau vài giây.');
      }
    } catch (err) {
      setModalError(extractError(err, 'Không kiểm tra được trạng thái thanh toán.'));
    } finally {
      setCheckingPayment(false);
    }
  };

  return (
    <div className="tcs-page">
      <HomeNavbar />
      {notice && (
        <div className="cr-toast" role="status">
          <span className="cr-toast__icon" aria-hidden="true">✓</span>
          <span className="cr-toast__msg">{notice}</span>
          <Link className="cr-toast__link" to={APP_ROUTES.marketplace}>
            Xem
          </Link>
          <button
            type="button"
            className="cr-toast__x"
            aria-label="Đóng thông báo"
            onClick={() => setNotice('')}
          >
            ×
          </button>
        </div>
      )}
      <main>
        <section className="tcs-section tcs-section--centers">
          <div className="tcs-container">
            <div className="tcs-section-bar">
              <div>
                <h1 className="tcs-section-bar__title">Trung tâm</h1>
                <p className="tcs-section-bar__subtitle">
                  Các trung tâm gia sư đối tác — quy trình tuyển chọn và hỗ trợ chuyên nghiệp.
                </p>
              </div>
            </div>

            {/* Danh sách trung tâm đã xác minh (thật). */}
            <div className="tcs-section-bar">
              <div>
                <h2 className="tcs-recruit__title">Danh sách trung tâm</h2>
                <p className="tcs-section-bar__subtitle">
                  {isClient
                    ? 'Chọn một trung tâm và gửi yêu cầu mở lớp theo nguyện vọng của bạn.'
                    : 'Các trung tâm đã được xác minh trên nền tảng.'}
                </p>
              </div>
            </div>

            {centersLoading && (
              <div className="tcs-search-results__state">
                <span className="tcs-spinner" aria-hidden="true" />
                Đang tải danh sách trung tâm...
              </div>
            )}
            {!centersLoading && centers.length === 0 && (
              <p className="tcs-empty">Hiện chưa có trung tâm nào được xác minh.</p>
            )}
            {!centersLoading && centers.length > 0 && (
              <div className="cr-grid">
                {centers.map((center) => (
                  <article key={center.centerId} className="cr-card">
                    <h3 className="cr-card__name">{center.companyName}</h3>
                    {center.description && <p className="cr-card__desc">{center.description}</p>}
                    {center.address && <span className="cr-card__meta">📍 {center.address}</span>}
                    {isClient && (
                      <div className="cr-card__actions">
                        <button
                          type="button"
                          className="tcs-btn tcs-btn--market tcs-btn--sm"
                          onClick={() => openModal(center)}
                        >
                          Nhờ trung tâm tìm gia sư
                        </button>
                      </div>
                    )}
                  </article>
                ))}
              </div>
            )}
          </div>
        </section>
      </main>

      {/* Modal gửi yêu cầu mở lớp — dùng lại form "tìm gia sư" cho đầy đủ thông tin */}
      {target && (
        <div className="cr-overlay" role="dialog" aria-modal="true" onClick={closeModal}>
          <div
            className="cr-modal"
            style={{ maxHeight: '88vh', overflowY: 'auto', maxWidth: 720 }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="cr-modal__title">Nhờ trung tâm tìm gia sư</h3>
            <p className="cr-modal__subtitle">Gửi tới: {target.companyName}</p>

            {modalError && <p className="cr-modal__error">{modalError}</p>}

            {cccdComplete === false ? (
              <div style={{ padding: '8px 0' }}>
                <p style={{ color: '#9a3412', marginTop: 0 }}>
                  Bạn cần nhập đầy đủ <strong>thông tin CCCD</strong> trong hồ sơ trước khi gửi yêu
                  cầu tới trung tâm.
                </p>
                <div className="cr-modal__actions">
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                    onClick={closeModal}
                  >
                    Đóng
                  </button>
                  <Link className="tcs-btn tcs-btn--market tcs-btn--sm" to={APP_ROUTES.profile}>
                    Đi tới hồ sơ nhập CCCD →
                  </Link>
                </div>
              </div>
            ) : paymentRequest ? (
              <div className="cr-payment-step">
                {paymentRequest.status === 'PENDING_PAYMENT' ? (
                  <>
                    <div className="cr-payment-step__head">
                      <span className="cr-payment-step__eyebrow">Phí xử lý yêu cầu trung tâm</span>
                      <h4>Quét mã để thanh toán</h4>
                      <p>
                        Sau khi SePay ghi nhận thanh toán, yêu cầu mới được gửi vào danh sách xử lý
                        của trung tâm.
                      </p>
                    </div>
                    <div className="cr-payment-step__body">
                      <img src={paymentRequest.qrUrl} alt="QR thanh toán phí xử lý yêu cầu" />
                      <div className="cr-payment-step__info">
                        <div>
                          <span>Số tiền</span>
                          <strong>{formatMoney(paymentRequest.amount)}</strong>
                        </div>
                        <div>
                          <span>Ngân hàng</span>
                          <strong>{paymentRequest.bankName}</strong>
                        </div>
                        <div>
                          <span>Số tài khoản</span>
                          <strong>{paymentRequest.accountNumber}</strong>
                        </div>
                        <div>
                          <span>Nội dung chuyển khoản</span>
                          <strong>{paymentRequest.transferContent}</strong>
                        </div>
                        <button
                          type="button"
                          className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                          onClick={() => navigator.clipboard?.writeText(paymentRequest.transferContent)}
                        >
                          Sao chép nội dung
                        </button>
                      </div>
                    </div>
                    <div className="cr-payment-step__actions">
                      <button
                        type="button"
                        className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                        onClick={closeModal}
                      >
                        Đóng
                      </button>
                      <button
                        type="button"
                        className="tcs-btn tcs-btn--market tcs-btn--sm"
                        onClick={checkPaymentStatus}
                        disabled={checkingPayment}
                      >
                        {checkingPayment ? 'Đang quét…' : 'Quét trạng thái'}
                      </button>
                    </div>
                  </>
                ) : (
                  <div className="cr-payment-step__success">
                    <span className="cr-payment-step__success-icon">✓</span>
                    <h4>Thanh toán đã được ghi nhận</h4>
                    <p>
                      Yêu cầu đã được gửi tới trung tâm. Bạn có thể theo dõi ở trang “Yêu cầu của
                      tôi”.
                    </p>
                    <div className="cr-payment-step__actions">
                      <button
                        type="button"
                        className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                        onClick={closeModal}
                      >
                        Đóng
                      </button>
                      <Link className="tcs-btn tcs-btn--market tcs-btn--sm" to={APP_ROUTES.marketplace}>
                        Xem yêu cầu
                      </Link>
                    </div>
                  </div>
                )}
              </div>
            ) : (
              <>
                <ClassRequestForm
                  initial={emptyForm()}
                  subjects={subjects}
                  grades={grades}
                  isEdit={false}
                  submitting={sending}
                  error={modalError}
                  onSubmit={submitRequest}
                  onCancel={closeModal}
                  submitLabel="Gửi yêu cầu & tạo QR"
                  freeTextSubjects
                  extraContent={
                    <div className="cr-payout">
                      <div className="cr-payout__head">
                        <strong>Tài khoản nhận tiền phát sinh</strong>
                        <span>
                          Vui lòng nhập tài khoản thụ hưởng của quý khách để phục vụ xử lý các nhu
                          cầu phát sinh.
                        </span>
                      </div>
                      <div className="cr-payout__grid">
                        <div className="cr-field cr-field--full">
                          <span className="cr-field__label">Ngân hàng nhận hoàn tiền *</span>
                          <BankSelectField
                            id="center-request-payout-bank"
                            selectedBank={selectedPayoutBank}
                            onOpen={() => setPayoutPickerOpen(true)}
                          />
                        </div>
                        <label className="cr-field">
                          <span className="cr-field__label">Số tài khoản *</span>
                          <input
                            className="cr-input"
                            type="text"
                            value={payoutAccountNo}
                            onChange={(event) => setPayoutAccountNo(event.target.value)}
                            placeholder="Nhập số tài khoản"
                          />
                        </label>
                        <label className="cr-field">
                          <span className="cr-field__label">Tên chủ tài khoản *</span>
                          <input
                            className="cr-input"
                            type="text"
                            value={payoutAccountHolderName}
                            onChange={(event) => setPayoutAccountHolderName(event.target.value)}
                            placeholder="Nhập tên chủ tài khoản"
                          />
                        </label>
                      </div>
                    </div>
                  }
                />
                <BankPickerDialog
                  open={payoutPickerOpen}
                  selectedBankCode={payoutBankCode}
                  onSelect={handleSelectPayoutBank}
                  onClose={() => setPayoutPickerOpen(false)}
                />
              </>
            )}
          </div>
        </div>
      )}
      <SiteFooter />
    </div>
  );
}
