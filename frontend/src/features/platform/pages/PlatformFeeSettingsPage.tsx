import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { catalogApi } from '../../catalog/api/catalogApi';
import type { SystemParameterResponse } from '../../catalog/types/catalogTypes';
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

const toPercent = (paramValue: string | null | undefined) => {
  const parsed = Number(paramValue);
  if (!Number.isFinite(parsed)) return '2';
  return String(Number((parsed * 100).toFixed(4)));
};

const toRateValue = (percentValue: string) => {
  const normalized = percentValue.replace(',', '.').trim();
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

  useEffect(() => {
    void loadParameter();
  }, []);

  const preview = useMemo(() => {
    const normalized = feePercent.replace(',', '.').trim();
    const percent = Number(normalized);
    if (!Number.isFinite(percent) || percent < 0) {
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

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setMessage('');
    setErrorMessage('');

    const normalized = feePercent.replace(',', '.').trim();
    const percent = Number(normalized);
    if (!Number.isFinite(percent) || percent < 0 || percent > 50) {
      setErrorMessage('Phí nền tảng phải nằm trong khoảng 0% đến 50%.');
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
      setMessage('Đã cập nhật phí nền tảng.');
    } catch (error: any) {
      console.error('Lỗi lưu cấu hình phí nền tảng:', error);
      setErrorMessage(error?.response?.data?.message || 'Không thể lưu cấu hình phí nền tảng.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <AdminLayout
      title="Cấu hình phí nền tảng"
      subtitle="Thiết lập tỷ lệ phí áp dụng khi hệ thống giải ngân escrow cho gia sư hoặc trung tâm."
    >
      <div className="fee-settings-grid">
        <section className="adm-card fee-settings-card">
          <div className="adm-card__head">
            <div>
              <h2 className="adm-card__title">Phí nền tảng</h2>
              <p className="fee-settings-card__hint">
                Admin nhập theo đơn vị phần trăm. Backend sẽ lưu dưới khóa <code>{PLATFORM_FEE_KEY}</code>.
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
            <form className="fee-settings-form" onSubmit={(event) => void handleSubmit(event)}>
              <label className="adm-field-group" htmlFor="platform-fee-rate">
                <span>Tỷ lệ phí nền tảng (%)</span>
                <div className="fee-settings-input">
                  <input
                    id="platform-fee-rate"
                    className="adm-field"
                    type="number"
                    min="0"
                    max="50"
                    step="0.01"
                    value={feePercent}
                    onChange={(event) => setFeePercent(event.target.value)}
                    placeholder="Ví dụ: 2"
                    required
                  />
                  <span>%</span>
                </div>
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
          <h2 className="adm-card__title">Ví dụ tính phí</h2>
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
            Phí này được trừ lúc giải ngân escrow, không trừ khi client mới chuyển khoản vào escrow.
          </p>
          <Link className="fee-settings-link" to={APP_ROUTES.platformParameters}>
            Xem cấu hình hệ thống
          </Link>
        </aside>
      </div>
    </AdminLayout>
  );
}
