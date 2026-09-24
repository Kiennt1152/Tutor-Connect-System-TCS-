/**
 * ====================================================================================================
 * [UC-09] MÀN HÌNH GIAO DIỆN CENTERSPAGE
 * ====================================================================================================
 * Nghiệp vụ chính:
 * 1. Hiển thị và điều phối các chức năng nghiệp vụ của phân hệ CentersPage.
 * 2. Đảm bảo trải nghiệm người dùng tối ưu và đồng bộ dữ liệu với hệ thống Backend.
 * * @author Hoàng Khôi Nguyên (NguyenHK186858)
 * @author Nguyễn Tiến Anh (tienanh6677)
 */
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { PaymentQrCountdown } from '../../../shared/components/PaymentQrCountdown';
import { SiteFooter } from '../components/SiteFooter';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { hasRole } from '../../../shared/auth/rbac';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { CenterListingCard } from '../components/CenterListingCard';
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
import { normalizeName } from '../../marketplace/matching/tutorMatching';
import './HomePage.css';
import './FindTutorPage.css';
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
  const [draft, setDraft] = useState(''); // chữ đang gõ
  const [query, setQuery] = useState(''); // từ khóa đã bấm "Tìm" (dùng để lọc)

  // Tìm không phân biệt dấu theo tên, địa chỉ và mô tả — gõ "Hà Nội" hay "ha noi" đều ra.
  const filteredCenters = useMemo(() => {
    const q = normalizeName(query);
    if (!q) return centers;
    return centers.filter((center) =>
      [center.companyName, center.address, center.description].some((field) =>
        normalizeName(field).includes(q),
      ),
    );
  }, [centers, query]);

  const applySearch = (value: string) => {
    setDraft(value);
    setQuery(value);
  };

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
        setNotice('Đã tạo mã thanh toán phí xử lý. Nếu đóng màn hình, vào Yêu cầu của tôi để mở lại QR.');
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
            Yêu cầu của tôi
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
        {/* Cùng bố cục với trang "Tìm lớp": dải hero cam nhạt, khung tìm kiếm trắng, rồi danh sách. */}
        <section className="tcs-home-hero tcs-find-hero ctr-hero">
          <div className="tcs-container">
            <Link className="tcs-find-back" to={APP_ROUTES.home}>
              ← Trang chủ
            </Link>
            {/* Cụm tiêu đề + ô tìm kiếm canh giữa cho cân đối (kiểu hero canh giữa của TCS). */}
            <div className="tcs-find-hero__intro ctr-intro">
              <h1 className="tcs-find-title">
                <span className="tcs-find-title__text tcs-find-title__text--plain">
                  Trung tâm gia sư uy tín
                </span>
              </h1>
            </div>

            <div className="ctr-panel">
              <form
                className="tcs-find-search"
                role="search"
                onSubmit={(event) => {
                  event.preventDefault();
                  setQuery(draft.trim());
                }}
              >
                {/* __bar là lớp tạo hàng ngang; thiếu nó thì nút Tìm rơi xuống dòng dưới. */}
                <div className="tcs-find-search__bar">
                  <div className="tcs-find-search__field">
                    <input
                      type="text"
                      className="tcs-find-search__input"
                      placeholder="Tìm theo tên, khu vực..."
                      value={draft}
                      onChange={(event) => setDraft(event.target.value)}
                      aria-label="Tìm kiếm trung tâm"
                    />
                    {draft && (
                      <button
                        type="button"
                        className="tcs-find-search__clear"
                        aria-label="Xoá từ khoá"
                        onClick={() => {
                          setDraft('');
                          setQuery('');
                        }}
                      >
                        ✕
                      </button>
                    )}
                  </div>
                  <button type="submit" className="tcs-find-search__btn">
                    Tìm
                  </button>
                </div>
              </form>
            </div>

            <section className="ctr-results" aria-live="polite">
              <header className="ctr-results__head">
                <h2 className="ctr-results__title">
                  {query ? `Kết quả cho “${query}”` : 'Tất cả trung tâm'}
                </h2>
              </header>

              {centersLoading && (
                <div className="ctr-state">
                  <span className="tcs-spinner" aria-hidden="true" />
                  Đang tải danh sách trung tâm...
                </div>
              )}
              {!centersLoading && centers.length === 0 && (
                <div className="ctr-state">Hiện chưa có trung tâm nào được xác minh.</div>
              )}
              {!centersLoading && centers.length > 0 && filteredCenters.length === 0 && (
                <div className="ctr-state">
                  <span>
                    Không tìm thấy trung tâm khớp với “{query}”.{' '}
                    <button type="button" className="ctr-state__reset" onClick={() => applySearch('')}>
                      Xem tất cả
                    </button>
                  </span>
                </div>
              )}
              {!centersLoading && filteredCenters.length > 0 && (
                <div className="tcs-listing-grid tcs-listing-grid--fill">
                  {filteredCenters.map((center) => (
                    <CenterListingCard
                      key={center.centerId}
                      center={center}
                      action={
                        isClient ? (
                          <button
                            type="button"
                            className="tcs-btn tcs-btn--ghost"
                            onClick={() => openModal(center)}
                            title="Nhờ trung tâm tìm gia sư"
                          >
                            Nhờ tìm gia sư
                          </button>
                        ) : undefined
                      }
                    />
                  ))}
                </div>
              )}
              {isClient && (
                <div className="cr-request-tip">
                  <div>
                    <strong>Đã có yêu cầu đang xử lý?</strong>
                    <span>
                      Vào <b>Yêu cầu của tôi</b> để xem lại QR thanh toán và trạng thái
                      các yêu cầu đã gửi.
                    </span>
                  </div>
                  <Link className="tcs-btn tcs-btn--market tcs-btn--sm" to={APP_ROUTES.marketplace}>
                    Xem yêu cầu của tôi
                  </Link>
                </div>
              )}
            </section>
          </div>
        </section>
      </main>

      {/* Modal gửi yêu cầu mở lớp — dùng lại form "tìm gia sư" cho đầy đủ thông tin */}
      {(target || paymentRequest) && (
        <div className="cr-overlay" role="dialog" aria-modal="true" onClick={closeModal}>
          <div
            className="cr-modal"
            // 820px: đủ để hàng lịch học (ngày · buổi · từ – đến · ×) nằm trọn một dòng, không xuống dòng.
            style={{ maxHeight: '88vh', overflowY: 'auto', maxWidth: 820 }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="cr-modal__title">
              {paymentRequest ? 'Thanh toán phí xử lý yêu cầu' : 'Nhờ trung tâm tìm gia sư'}
            </h3>
            <p className="cr-modal__subtitle">
              {paymentRequest
                ? 'Mở lại mã QR của yêu cầu đang chờ thanh toán.'
                : `Gửi tới: ${target?.companyName ?? ''}`}
            </p>

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
                      <div className="cr-payment-step__qr-wrap">
                        <img src={paymentRequest.qrUrl} alt="QR thanh toán phí xử lý yêu cầu" />
                        <PaymentQrCountdown
                          resetKey={
                            paymentRequest.requestId
                            ?? paymentRequest.transferContent
                            ?? paymentRequest.qrUrl
                          }
                          label="Thời gian chuyển khoản còn lại"
                          expiredLabel="Mã QR đã hết 5 phút hiển thị. Vui lòng tạo lại yêu cầu nếu chưa chuyển khoản."
                        />
                      </div>
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
            ) : target ? (
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
            ) : null}
          </div>
        </div>
      )}
      <SiteFooter />
    </div>
  );
}
