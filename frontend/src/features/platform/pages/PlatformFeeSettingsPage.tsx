/**
 * ====================================================================================================
 * [UC-46 / UC-57] MÀN HÌNH CẤU HÌNH TỶ LỆ PHÍ NỀN TẢNG (PLATFORM FEE SETTINGS PAGE)
 * ====================================================================================================
 * Nghiệp vụ chính:
 * 1. Thiết lập tỷ lệ phần trăm phí dịch vụ sàn áp dụng cho lớp học gia sư tự do.
 * 2. Cấu hình chính sách ưu đãi phí nhượng quyền cho từng trung tâm gia sư đối tác.
 * 3. Mô phỏng tính toán doanh thu dự kiến khi điều chỉnh mức phí.
 * * @author Hoàng Minh Đức (mduc1011-swp)
 * @author Nguyễn Tiến Anh (tienanh6677)
 */

import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { catalogApi } from '../../catalog/api/catalogApi';
import type { SystemParameterResponse } from '../../catalog/types/catalogTypes';
import { platformApi } from '../api/platformApi';
import type { CenterFeeConfigApiResponse } from '../types/platformTypes';
import { AdminLayout } from '../components/AdminLayout';
import './PlatformFeeSettingsPage.css';

const PLATFORM_FEE_KEY = 'PLATFORM_FEE_RATE';
const DEFAULT_DESCRIPTION =
  'Tỷ lệ phí nền tảng áp dụng khi giải ngân escrow và tính phí xử lý yêu cầu trung tâm.';

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(value);

const toPercent = (paramValue: string | number | null | undefined) => {
  const parsed = Number(paramValue);
  if (!Number.isFinite(parsed)) return '2';
  return String(Number((parsed * 100).toFixed(4)));
};

const toRateValue = (percentValue: string) => {
  const normalized = percentValue.replace('%', '').replace(',', '.').trim();
  const parsed = Number(normalized);
  if (!Number.isFinite(parsed)) return null;
  return Number((parsed / 100).toFixed(6)).toString();
};

export default function PlatformFeeSettingsPage() {
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [saving, setSaving] = useState(false);
  const [parameter, setParameter] = useState<SystemParameterResponse | null>(null);
  const [feePercent, setFeePercent] = useState('2');
  const [message, setMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  // Center Fee Overrides State
  const [centers, setCenters] = useState<CenterFeeConfigApiResponse[]>([]);
  const [loadingCenters, setLoadingCenters] = useState(false);
  const [centerSearch, setCenterSearch] = useState('');
  const [centerMessage, setCenterMessage] = useState('');
  const [centerError, setCenterError] = useState('');

  // Edit Modal State
  const [editingCenter, setEditingCenter] = useState<CenterFeeConfigApiResponse | null>(null);
  const [editPercent, setEditPercent] = useState('2');
  const [editReason, setEditReason] = useState('');
  const [savingCenterFee, setSavingCenterFee] = useState(false);
  const [modalError, setModalError] = useState('');

  /**
   * [UC-46, UC-57] Tải tỷ lệ phí dịch vụ mặc định toàn sàn từ tham số hệ thống.
   */
  const loadParameter = async () => {
    setStatus('loading');
    setErrorMessage('');
    setMessage('');
    try {
      const params = await catalogApi.getSystemParameters(PLATFORM_FEE_KEY);
      const current = params.find((item) => item.paramKey === PLATFORM_FEE_KEY) ?? null;
      setParameter(current);
      setFeePercent(toPercent(current?.paramValue));
      setStatus('success');
    } catch (error) {
      console.error('Lỗi tải cấu hình phí nền tảng:', error);
      setErrorMessage('Không tải được cấu hình phí nền tảng.');
      setStatus('error');
    }
  };

  /**
   * [UC-46] Tải danh sách cấu hình tỷ lệ phí thỏa thuận riêng của các Trung tâm gia sư.
   */
  const loadCenterFees = async () => {
    setLoadingCenters(true);
    setCenterError('');
    try {
      const response = await platformApi.getCenterFeeConfigs();
      setCenters(response.data || []);
    } catch (error) {
      console.error('Lỗi tải danh sách cấu hình phí trung tâm:', error);
      setCenterError('Không tải được danh sách phí trung tâm gia sư.');
    } finally {
      setLoadingCenters(false);
    }
  };

  useEffect(() => {
    void loadParameter();
    void loadCenterFees();
  }, []);

  const preview = useMemo(() => {
    const normalized = feePercent.replace('%', '').replace(',', '.').trim();
    const percent = Number(normalized);
    if (!Number.isFinite(percent) || percent < 0 || percent > 50) {
      return null;
    }
    const escrowAmount = 1000000;
    const feeAmount = Math.round((escrowAmount * percent) / 100);
    return {
      percent,
      escrowAmount,
      feeAmount,
      netAmount: escrowAmount - feeAmount,
    };
  }, [feePercent]);

  /**
   * [UC-46, UC-57] Lưu cập nhật tỷ lệ phí sàn mặc định toàn hệ thống.
   */
  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setMessage('');
    setErrorMessage('');

    const raw = feePercent.replace('%', '').replace(',', '.').trim();
    if (!raw) {
      setErrorMessage('Vui lòng nhập tỷ lệ phí nền tảng.');
      return;
    }

    const percent = Number(raw);
    if (!Number.isFinite(percent)) {
      setErrorMessage('Tỷ lệ phí phải là một số hợp lệ.');
      return;
    }

    if (percent < 0) {
      setErrorMessage('Tỷ lệ phí không được là số âm. Phí nền tảng phải là số dương từ 0% đến 50%.');
      return;
    }

    if (percent > 50) {
      setErrorMessage('Tỷ lệ phí vượt quá giới hạn tối đa cho phép (50%). Phí nền tảng phải nằm trong khoảng từ 0% đến 50%.');
      return;
    }

    const rateValue = toRateValue(feePercent);
    if (!rateValue) {
      setErrorMessage('Vui lòng nhập tỷ lệ phí hợp lệ.');
      return;
    }

    const payload = {
      paramKey: PLATFORM_FEE_KEY,
      paramValue: rateValue,
      description: parameter?.description || DEFAULT_DESCRIPTION,
    };

    setSaving(true);
    try {
      const saved = parameter
        ? await catalogApi.updateSystemParameter(parameter.parameterId, payload)
        : await catalogApi.createSystemParameter(payload);
      setParameter(saved);
      setFeePercent(toPercent(saved.paramValue));
      setMessage('Đã cập nhật phí nền tảng mặc định toàn hệ thống.');
      void loadCenterFees();
    } catch (error: any) {
      console.error('Lỗi lưu cấu hình phí nền tảng:', error);
      setErrorMessage(error?.response?.data?.message || 'Không thể lưu cấu hình phí nền tảng.');
    } finally {
      setSaving(false);
    }
  };

  /**
   * [UC-46] Mở modal điều chỉnh tỷ lệ phí riêng cho một Trung tâm gia sư cụ thể.
   */
  const handleOpenEditModal = (center: CenterFeeConfigApiResponse) => {
    setEditingCenter(center);
    const initialPercent = center.customFeeRate != null
      ? (center.customFeeRate * 100).toFixed(2)
      : (center.effectiveFeeRate * 100).toFixed(2);
    setEditPercent(String(Number(initialPercent)));
    setEditReason('');
    setModalError('');
  };

  /**
   * [UC-46] Đóng modal điều chỉnh tỷ lệ phí trung tâm gia sư.
   */
  const handleCloseModal = () => {
    setEditingCenter(null);
    setModalError('');
  };

  /**
   * [UC-46] Xác thực và lưu tỷ lệ phí riêng đã thỏa thuận cho Trung tâm gia sư.
   */
  const handleSaveCenterFee = async (e: FormEvent) => {
    e.preventDefault();
    if (!editingCenter) return;

    setModalError('');
    const raw = editPercent.replace('%', '').replace(',', '.').trim();
    if (!raw) {
      setModalError('Vui lòng nhập tỷ lệ phí cho trung tâm.');
      return;
    }

    const parsedPercent = Number(raw);
    if (!Number.isFinite(parsedPercent)) {
      setModalError('Tỷ lệ phí phải là một số hợp lệ.');
      return;
    }

    if (parsedPercent < 0) {
      setModalError('Tỷ lệ phí không được là số âm. Phí tùy chỉnh phải là số dương từ 0% đến 50%.');
      return;
    }

    if (parsedPercent > 50) {
      setModalError('Tỷ lệ phí vượt quá giới hạn tối đa cho phép (50%). Phí tùy chỉnh phải nằm trong khoảng từ 0% đến 50%.');
      return;
    }

    setSavingCenterFee(true);
    try {
      const rate = Number((parsedPercent / 100).toFixed(6));
      await platformApi.updateCenterFeeConfig(editingCenter.centerId, {
        customFeeRate: rate,
        reason: editReason.trim() || 'Admin cập nhật phí riêng',
      });
      setCenterMessage(`Đã cập nhật mức phí ${parsedPercent}% cho ${editingCenter.companyName}`);
      handleCloseModal();
      void loadCenterFees();
    } catch (err: any) {
      console.error('Lỗi lưu phí trung tâm:', err);
      setModalError(err?.response?.data?.message || 'Không thể lưu mức phí riêng cho trung tâm.');
    } finally {
      setSavingCenterFee(false);
    }
  };

  /**
   * [UC-46] Khôi phục mức phí của Trung tâm gia sư về mức phí chuẩn mặc định của toàn sàn.
   */
  const handleResetCenterFee = async (center: CenterFeeConfigApiResponse) => {
    if (!window.confirm(`Bạn có chắc chắn muốn khôi phục mức phí của "${center.companyName}" về mức mặc định toàn sàn (${toPercent(center.defaultPlatformFeeRate)}%)?`)) {
      return;
    }

    try {
      await platformApi.resetCenterFeeConfig(center.centerId);
      setCenterMessage(`Đã khôi phục mức phí mặc định cho ${center.companyName}`);
      void loadCenterFees();
    } catch (err: any) {
      console.error('Lỗi khôi phục phí trung tâm:', err);
      setCenterError(err?.response?.data?.message || 'Không thể khôi phục phí mặc định.');
    }
  };

  // Filtered Centers
  const filteredCenters = useMemo(() => {
    const q = centerSearch.toLowerCase().trim();
    if (!q) return centers;
    return centers.filter(
      (c) =>
        c.companyName?.toLowerCase().includes(q) ||
        c.licenseNo?.toLowerCase().includes(q) ||
        c.phone?.toLowerCase().includes(q) ||
        c.email?.toLowerCase().includes(q),
    );
  }, [centers, centerSearch]);

  // Modal Preview Calculation
  const modalPreview = useMemo(() => {
    const parsed = Number(editPercent.replace('%', '').replace(',', '.').trim());
    if (!Number.isFinite(parsed) || parsed < 0 || parsed > 50) return null;
    const sample = 2000000;
    const fee = Math.round((sample * parsed) / 100);
    return {
      sample,
      fee,
      net: sample - fee,
    };
  }, [editPercent]);

  return (
    <AdminLayout
      title="Cấu hình phí nền tảng"
      subtitle="Thiết lập tỷ lệ phí áp dụng khi hệ thống giải ngân escrow hoặc xử lý yêu cầu cho trung tâm gia sư."
    >
      <div className="fee-settings-grid">
        <section className="adm-card fee-settings-card">
          <div className="adm-card__head">
            <div>
              <h2 className="adm-card__title">Phí nền tảng mặc định toàn sàn</h2>
              <p className="fee-settings-card__hint">
                Tỷ lệ phí cơ sở áp dụng cho toàn bộ các lớp gia sư cá nhân và các trung tâm không có cấu hình riêng. Backend lưu dưới khóa <code>{PLATFORM_FEE_KEY}</code>.
              </p>
            </div>
          </div>

          {status === 'loading' && <div className="adm-state">Đang tải cấu hình phí...</div>}

          {status === 'error' && (
            <div className="adm-state">
              <p>{errorMessage}</p>
              <button className="tcs-btn tcs-btn--primary" type="button" onClick={() => void loadParameter()}>
                Thử lại
              </button>
            </div>
          )}

          {status === 'success' && (
            <form className="fee-settings-form" onSubmit={(event) => void handleSubmit(event)} noValidate>
              <label className="adm-field-group" htmlFor="platform-fee-rate">
                <span>Tỷ lệ phí mặc định (%)</span>
                <div className="fee-settings-input">
                  <input
                    id="platform-fee-rate"
                    className="adm-field"
                    type="text"
                    inputMode="decimal"
                    value={feePercent}
                    onChange={(event) => {
                      setFeePercent(event.target.value);
                      if (errorMessage) setErrorMessage('');
                      if (message) setMessage('');
                    }}
                    placeholder="Ví dụ: 2 hoặc 3.5"
                    required
                  />
                  <span>%</span>
                </div>
                {(() => {
                  const val = Number(feePercent.replace('%', '').replace(',', '.').trim());
                  if (Number.isFinite(val) && val < 0) {
                    return (
                      <div style={{ color: '#dc2626', fontSize: '0.82rem', marginTop: '4px', fontWeight: 500 }}>
                        ⚠️ Tỷ lệ phí không được là số âm (tối thiểu 0%).
                      </div>
                    );
                  }
                  if (Number.isFinite(val) && val > 50) {
                    return (
                      <div style={{ color: '#dc2626', fontSize: '0.82rem', marginTop: '4px', fontWeight: 500 }}>
                        ⚠️ Tỷ lệ phí vượt quá giới hạn tối đa cho phép (tối đa 50%).
                      </div>
                    );
                  }
                  return null;
                })()}
              </label>

              <div className="fee-settings-note">
                <strong>Đang áp dụng:</strong>{' '}
                {parameter ? `${toPercent(parameter.paramValue)}%` : 'Chưa có cấu hình, backend dùng mặc định 2%.'}
              </div>

              {message && <div className="adm-alert adm-alert--success">{message}</div>}
              {errorMessage && <div className="adm-alert adm-alert--error">{errorMessage}</div>}

              <div className="adm-form__footer">
                <button className="tcs-btn tcs-btn--primary" type="submit" disabled={saving}>
                  {saving ? 'Đang lưu...' : 'Lưu cấu hình phí'}
                </button>
                <button className="tcs-btn tcs-btn--ghost" type="button" onClick={() => void loadParameter()}>
                  Làm mới
                </button>
              </div>
            </form>
          )}
        </section>

        <aside className="adm-card fee-preview-card">
          <h2 className="adm-card__title">Ví dụ tính phí mẫu</h2>
          <div className="fee-preview-card__rows">
            <div>
              <span>Escrow mẫu</span>
              <strong>{formatCurrency(preview?.escrowAmount ?? 1000000)}</strong>
            </div>
            <div>
              <span>Phí nền tảng</span>
              <strong>{preview ? `${preview.percent}% = ${formatCurrency(preview.feeAmount)}` : '—'}</strong>
            </div>
            <div className="fee-preview-card__highlight">
              <span>Gia sư/trung tâm nhận</span>
              <strong>{preview ? formatCurrency(preview.netAmount) : '—'}</strong>
            </div>
          </div>
          <p className="fee-preview-card__hint">
            Phí này được tự động khấu trừ khi giải ngân escrow, không trừ khi client mới nạp tiền vào sàn.
          </p>
          <Link className="fee-settings-link" to={APP_ROUTES.platformParameters}>
            Xem cấu hình hệ thống &rarr;
          </Link>
        </aside>
      </div>

      {/* Cấu hình phí riêng cho từng Trung tâm Gia sư (UC-46) */}
      <section className="adm-card center-fees-section">
        <div className="center-fees-header">
          <div>
            <h2 className="adm-card__title">Cấu hình phí riêng cho từng Trung tâm Gia sư</h2>
            <p className="fee-settings-card__hint">
              Thiết lập tỷ lệ phí chiết khấu riêng cho từng đối tác trung tâm (ví dụ: ưu đãi 1.5%, 1%, hoặc 0%). Nếu không thiết lập riêng, trung tâm sẽ tự động áp dụng mức phí mặc định toàn sàn ({feePercent}%).
            </p>
          </div>
          <div className="center-fees-actions">
            <input
              type="text"
              className="adm-field center-fees-search"
              placeholder="Tìm tên trung tâm, GPKD, SĐT..."
              value={centerSearch}
              onChange={(e) => setCenterSearch(e.target.value)}
            />
            <button
              className="tcs-btn tcs-btn--ghost"
              type="button"
              onClick={() => void loadCenterFees()}
              disabled={loadingCenters}
            >
              {loadingCenters ? 'Đang tải...' : 'Làm mới'}
            </button>
          </div>
        </div>

        {centerMessage && (
          <div className="adm-alert adm-alert--success" style={{ marginBottom: '16px' }}>
            {centerMessage}
          </div>
        )}
        {centerError && (
          <div className="adm-alert adm-alert--error" style={{ marginBottom: '16px' }}>
            {centerError}
          </div>
        )}

        {loadingCenters && centers.length === 0 ? (
          <div className="adm-state">Đang tải danh sách trung tâm gia sư...</div>
        ) : filteredCenters.length === 0 ? (
          <div className="adm-state">
            {centerSearch ? 'Không tìm thấy trung tâm gia sư nào phù hợp với từ khóa.' : 'Chưa có trung tâm gia sư nào trên sàn.'}
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="center-fees-table">
              <thead>
                <tr>
                  <th>Trung tâm gia sư</th>
                  <th>Mã GPKD</th>
                  <th>Liên hệ</th>
                  <th>Tỷ lệ phí áp dụng</th>
                  <th>Loại cấu hình</th>
                  <th style={{ textAlign: 'right' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {filteredCenters.map((center) => (
                  <tr key={center.centerId}>
                    <td>
                      <div style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>
                        {center.companyName}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)' }}>
                        ID: #{center.centerId} &bull; User ID: #{center.userId || 'N/A'}
                      </div>
                    </td>
                    <td>
                      <span style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>
                        {center.licenseNo || '—'}
                      </span>
                    </td>
                    <td>
                      <div>{center.email || '—'}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)' }}>
                        {center.phone || '—'}
                      </div>
                    </td>
                    <td>
                      <span style={{ fontSize: '1.05rem', fontWeight: 700, color: center.custom ? '#047857' : 'var(--color-text-primary)' }}>
                        {center.effectiveFeeRatePercent}
                      </span>
                    </td>
                    <td>
                      {center.custom ? (
                        <span className="center-badge center-badge--custom">
                          Tùy chỉnh riêng
                        </span>
                      ) : (
                        <span className="center-badge center-badge--default">
                          Mặc định sàn ({toPercent(center.defaultPlatformFeeRate)}%)
                        </span>
                      )}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div className="center-fees-actions" style={{ justifyContent: 'flex-end' }}>
                        <button
                          type="button"
                          className="tcs-btn tcs-btn--outline tcs-btn--sm"
                          onClick={() => handleOpenEditModal(center)}
                        >
                          Điều chỉnh phí
                        </button>
                        {center.custom && (
                          <button
                            type="button"
                            className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                            style={{ color: '#b91c1c' }}
                            onClick={() => void handleResetCenterFee(center)}
                            title="Xóa mức phí riêng, quay về dùng phí sàn"
                          >
                            Khôi phục mặc định
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {/* Modal: Điều chỉnh phí riêng cho trung tâm */}
      {editingCenter && (
        <div className="center-fee-modal-overlay" onClick={handleCloseModal}>
          <div className="center-fee-modal" onClick={(e) => e.stopPropagation()}>
            <div className="center-fee-modal__header">
              <h3>Cấu hình phí riêng: {editingCenter.companyName}</h3>
              <button
                type="button"
                className="center-fee-modal__close"
                onClick={handleCloseModal}
              >
                &times;
              </button>
            </div>

            <form onSubmit={(e) => void handleSaveCenterFee(e)} noValidate>
              <div className="center-fee-modal__body">
                {modalError && <div className="adm-alert adm-alert--error">{modalError}</div>}

                <label className="adm-field-group">
                  <span>Tỷ lệ phí tùy chỉnh (%)</span>
                  <div className="fee-settings-input" style={{ maxWidth: '100%' }}>
                    <input
                      type="text"
                      inputMode="decimal"
                      className="adm-field"
                      value={editPercent}
                      onChange={(e) => {
                        setEditPercent(e.target.value);
                        if (modalError) setModalError('');
                      }}
                      placeholder="Ví dụ: 1.5"
                      required
                    />
                    <span>%</span>
                  </div>
                  {(() => {
                    const val = Number(editPercent.replace('%', '').replace(',', '.').trim());
                    if (Number.isFinite(val) && val < 0) {
                      return (
                        <div style={{ color: '#dc2626', fontSize: '0.82rem', marginTop: '4px', fontWeight: 500 }}>
                          ⚠️ Tỷ lệ phí không được là số âm (tối thiểu 0%).
                        </div>
                      );
                    }
                    if (Number.isFinite(val) && val > 50) {
                      return (
                        <div style={{ color: '#dc2626', fontSize: '0.82rem', marginTop: '4px', fontWeight: 500 }}>
                          ⚠️ Tỷ lệ phí vượt quá giới hạn tối đa cho phép (tối đa 50%).
                        </div>
                      );
                    }
                    return null;
                  })()}
                  <div className="center-fee-quick-rates">
                    <span style={{ fontSize: '0.75rem', color: 'var(--color-text-secondary)', alignSelf: 'center' }}>
                      Gợi ý:
                    </span>
                    {['0', '1', '1.5', '2', '3', '5'].map((rate) => (
                      <button
                        key={rate}
                        type="button"
                        className="center-fee-quick-btn"
                        onClick={() => setEditPercent(rate)}
                      >
                        {rate}%
                      </button>
                    ))}
                  </div>
                </label>

                <label className="adm-field-group">
                  <span>Lý do / Căn cứ điều chỉnh</span>
                  <input
                    type="text"
                    className="adm-field"
                    placeholder="Ví dụ: Đối tác VIP, Hợp đồng chiến lược Q4..."
                    value={editReason}
                    onChange={(e) => setEditReason(e.target.value)}
                  />
                </label>

                {modalPreview && (
                  <div className="fee-preview-card__rows" style={{ marginTop: '8px' }}>
                    <div style={{ background: '#f8fafc', padding: '10px 14px', borderRadius: '8px' }}>
                      <span style={{ fontSize: '0.8rem' }}>Mẫu lớp 2.000.000₫: Phí sàn thu</span>
                      <strong style={{ color: '#047857' }}>{formatCurrency(modalPreview.fee)}</strong>
                    </div>
                    <div style={{ background: '#f0fdf4', padding: '10px 14px', borderRadius: '8px', border: '1px solid #bbf7d0' }}>
                      <span style={{ fontSize: '0.8rem' }}>Trung tâm thực nhận</span>
                      <strong style={{ color: '#15803d' }}>{formatCurrency(modalPreview.net)}</strong>
                    </div>
                  </div>
                )}
              </div>

              <div className="center-fee-modal__footer">
                <button
                  type="button"
                  className="tcs-btn tcs-btn--ghost"
                  onClick={handleCloseModal}
                  disabled={savingCenterFee}
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  className="tcs-btn tcs-btn--primary"
                  disabled={savingCenterFee}
                >
                  {savingCenterFee ? 'Đang lưu...' : 'Lưu mức phí riêng'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}
