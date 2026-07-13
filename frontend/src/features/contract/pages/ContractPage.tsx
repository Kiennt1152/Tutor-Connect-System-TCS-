import { useCallback, useState } from 'react';
import { useParams } from 'react-router-dom';
import { ContractLayout } from '../components/ContractLayout';
import { OtpInput } from '../components/OtpInput';
import { useContract, useContractSignatures } from '../hooks/useContract';
import { useSendOtp, useSignWithOtp } from '../hooks/useContractMutations';
import { mapContract } from '../mappers/contractMapper';
import type { ContractSignatureStatus } from '../types/contractTypes';
import './ContractPage.css';

function statusBadgeClass(status: string): string {
  switch (status) {
    case 'PENDING': return 'tcs-badge tcs-badge--pending';
    case 'DRAFT': return 'tcs-badge tcs-badge--draft';
    case 'SIGNED': return 'tcs-badge tcs-badge--signed';
    case 'ACTIVE': return 'tcs-badge tcs-badge--active';
    case 'COMPLETED': return 'tcs-badge tcs-badge--completed';
    case 'TERMINATED': return 'tcs-badge tcs-badge--terminated';
    default: return 'tcs-badge tcs-badge--draft';
  }
}

function SignatureStatusIcon({ status }: { status: ContractSignatureStatus }) {
  if (status === 'SIGNED') {
    return (
      <span className="cnt-signature-status__icon" title="Đã ký">
        ✓
      </span>
    );
  }
  if (status === 'EXPIRED') {
    return (
      <span className="cnt-signature-status__icon" title="Hết hạn">
        ✗
      </span>
    );
  }
  return (
    <span className="cnt-signature-status__icon" title="Chờ ký">
      ○
    </span>
  );
}

export default function ContractPage() {
  const { id } = useParams<{ id: string }>();
  const contractId = Number(id);

  const { status: contractStatus, data: contractData, reload: reloadContract, errorMessage: contractError } =
    useContract(contractId);
  const {
    status: sigStatus,
    data: sigData,
    reload: reloadSigs,
    errorMessage: sigError,
  } = useContractSignatures(contractId);

  const { status: sendOtpStatus, errorMessage: sendOtpError, otpInfo, sendOtp, reset: resetOtp } = useSendOtp();
  const { status: signStatus, errorMessage: signError, signWithOtp, reset: resetSign } = useSignWithOtp();

  const [otpValue, setOtpValue] = useState('');
  const [showOtpForm, setShowOtpForm] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const reloadAll = useCallback(() => {
    void reloadContract();
    void reloadSigs();
    setSuccessMessage(null);
    setShowOtpForm(false);
    setOtpValue('');
    resetOtp();
    resetSign();
  }, [reloadContract, reloadSigs, resetOtp, resetSign]);

  const handleSendOtp = async () => {
    const ok = await sendOtp(contractId);
    if (ok) {
      setShowOtpForm(true);
    }
  };

  const handleSign = async () => {
    if (otpValue.length < 6) return;
    const contract = await signWithOtp(contractId, otpValue, (updated) => {
      setSuccessMessage(
        `Ký hợp đồng thành công! Trạng thái: ${updated.statusLabel ?? updated.status}`,
      );
      void reloadAll();
    });
    if (contract) {
      void reloadAll();
    }
  };

  if (!id || isNaN(contractId) || contractId <= 0) {
    return (
      <ContractLayout title="Hợp đồng điện tử" subtitle="Mã hợp đồng không hợp lệ.">
        <div className="cnt-alert cnt-alert--error">
          Mã hợp đồng không hợp lệ. Vui lòng kiểm tra lại đường dẫn.
        </div>
      </ContractLayout>
    );
  }

  if (contractStatus === 'idle' || contractStatus === 'loading') {
    return (
      <ContractLayout title="Hợp đồng điện tử">
        <div className="cnt-state">Đang tải thông tin hợp đồng…</div>
      </ContractLayout>
    );
  }

  if (contractStatus === 'error' || !contractData) {
    return (
      <ContractLayout title="Hợp đồng điện tử">
        <div className="cnt-alert cnt-alert--error">{contractError ?? 'Không tải được hợp đồng.'}</div>
        <button className="cnt-btn cnt-btn--secondary" onClick={() => void reloadContract()}>
          Thử lại
        </button>
      </ContractLayout>
    );
  }

  const contract = mapContract(contractData);
  const canSign = contract.status === 'PENDING';

  return (
    <ContractLayout
      title="Hợp đồng điện tử"
      subtitle={`Số hợp đồng: ${contract.contractNo}`}
    >
      {/* Success banner */}
      {successMessage && (
        <div className="cnt-alert cnt-alert--success" style={{ marginBottom: 'var(--space-md)' }}>
          {successMessage}
        </div>
      )}

      {/* Contract Info Card */}
      <div className="cnt-card">
        <h2 className="cnt-card__title">Thông tin hợp đồng</h2>
        <div className="cnt-info-grid">
          <div className="cnt-info-item">
            <span className="cnt-info-item__label">Trạng thái</span>
            <span className="cnt-info-item__value">
              <span className={statusBadgeClass(contract.status)}>{contract.statusLabel}</span>
            </span>
          </div>
          <div className="cnt-info-item">
            <span className="cnt-info-item__label">Loại lớp</span>
            <span className="cnt-info-item__value">{contract.sourceTypeLabel}</span>
          </div>
          <div className="cnt-info-item">
            <span className="cnt-info-item__label">Ngày tạo</span>
            <span className="cnt-info-item__value">{contract.createdAt}</span>
          </div>
          <div className="cnt-info-item">
            <span className="cnt-info-item__label">Ngày ký</span>
            <span className="cnt-info-item__value">
              {contract.signedAt !== '—' ? contract.signedAt : '—'}
            </span>
          </div>
          <div className="cnt-info-item">
            <span className="cnt-info-item__label">Hạn ký</span>
            <span className="cnt-info-item__value">
              {contract.expiresAt !== '—' ? (
                <span style={{ color: contract.status === 'PENDING' ? '#dc2626' : undefined }}>
                  {contract.expiresAt}
                </span>
              ) : '—'}
            </span>
          </div>
          <div className="cnt-info-item">
            <span className="cnt-info-item__label">Mẫu hợp đồng</span>
            <span className="cnt-info-item__value">
              {contract.templateName ?? '—'}
            </span>
          </div>
        </div>
      </div>

      {/* Parties Info Card */}
      <div className="cnt-card">
        <h2 className="cnt-card__title">Các bên liên quan</h2>
        <div className="cnt-info-grid">
          {contract.tutorName && (
            <div className="cnt-info-item">
              <span className="cnt-info-item__label">Gia sư</span>
              <span className="cnt-info-item__value">{contract.tutorName}</span>
              <span className="cnt-info-item__value cnt-info-item__value--muted">
                {contract.tutorEmail}
              </span>
            </div>
          )}
          {contract.centerName && (
            <div className="cnt-info-item">
              <span className="cnt-info-item__label">Trung tâm</span>
              <span className="cnt-info-item__value">{contract.centerName}</span>
              <span className="cnt-info-item__value cnt-info-item__value--muted">
                {contract.centerEmail}
              </span>
            </div>
          )}
          {contract.clientName && (
            <div className="cnt-info-item">
              <span className="cnt-info-item__label">Học viên / Phụ huynh</span>
              <span className="cnt-info-item__value">{contract.clientName}</span>
              <span className="cnt-info-item__value cnt-info-item__value--muted">
                {contract.clientEmail}
              </span>
            </div>
          )}
        </div>
      </div>

      {/* Signature Status Card */}
      <div className="cnt-card">
        <h2 className="cnt-card__title">
          Trạng thái ký
          {sigData && (
            <span style={{ fontWeight: '400', fontSize: 'var(--fs-body-md)', marginLeft: '8px' }}>
              — {sigData.signedCount}/{sigData.requiredSignatures} bên đã ký
            </span>
          )}
        </h2>

        {sigStatus === 'loading' && (
          <div className="cnt-state">Đang tải trạng thái ký…</div>
        )}

        {sigError && (
          <div className="cnt-alert cnt-alert--error">{sigError}</div>
        )}

        {sigStatus === 'success' && sigData && (
          <>
            <div className="cnt-signature-progress" style={{ marginBottom: 'var(--space-md)' }}>
              <div
                style={{
                  height: '6px',
                  borderRadius: '3px',
                  background: 'var(--color-border)',
                  overflow: 'hidden',
                }}
              >
                <div
                  style={{
                    height: '100%',
                    width: `${(sigData.signedCount / sigData.requiredSignatures) * 100}%`,
                    background: sigData.hasAllSignatures ? '#22c55e' : 'var(--color-primary)',
                    transition: 'width 0.3s ease',
                  }}
                />
              </div>
            </div>

            <table className="cnt-signature-table">
              <thead>
                <tr>
                  <th>Bên ký</th>
                  <th>Email</th>
                  <th>Trạng thái</th>
                  <th>Thời điểm ký</th>
                </tr>
              </thead>
              <tbody>
                {sigData.signatures.map((sig) => (
                  <tr key={sig.id}>
                    <td>
                      <div className="cnt-signature-status">
                        <SignatureStatusIcon status={sig.signatureStatus} />
                        <strong>{sig.partyLabel}</strong>
                      </div>
                    </td>
                    <td>{sig.signerEmail ?? sig.signerName ?? '—'}</td>
                    <td>
                      <span
                        className={`tcs-badge ${
                          sig.signatureStatus === 'SIGNED'
                            ? 'tcs-badge--active'
                            : sig.signatureStatus === 'EXPIRED'
                            ? 'tcs-badge--terminated'
                            : 'tcs-badge--pending'
                        }`}
                      >
                        {sig.signatureStatusLabel}
                      </span>
                    </td>
                    <td>{sig.signedAt ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            {sigData.hasAllSignatures && contract.status === 'SIGNED' && (
              <div className="cnt-alert cnt-alert--success" style={{ marginTop: 'var(--space-md)' }}>
                Tất cả các bên đã ký. Hợp đồng đã có hiệu lực.
              </div>
            )}
          </>
        )}
      </div>

      {/* Sign with OTP Card */}
      {canSign && (
        <div className="cnt-card">
          <h2 className="cnt-card__title">Ký hợp đồng</h2>

          {sendOtpError && (
            <div className="cnt-alert cnt-alert--error">{sendOtpError}</div>
          )}

          {signError && (
            <div className="cnt-alert cnt-alert--error">{signError}</div>
          )}

          {!showOtpForm && sendOtpStatus !== 'success' && (
            <div className="cnt-otp-card">
              <p className="cnt-otp-card__desc">
                Để ký hợp đồng, hệ thống sẽ gửi mã xác nhận (OTP) gồm 6 chữ số đến email của bạn.
                Nhập mã này để hoàn tất việc ký.
              </p>
              <div className="cnt-actions">
                <button
                  className="cnt-btn cnt-btn--primary"
                  onClick={() => void handleSendOtp()}
                  disabled={sendOtpStatus === 'loading'}
                >
                  {sendOtpStatus === 'loading' ? 'Đang gửi…' : 'Gửi mã OTP'}
                </button>
              </div>
            </div>
          )}

          {(showOtpForm || sendOtpStatus === 'success') && otpInfo && (
            <div className="cnt-otp-card">
              <p className="cnt-otp-card__desc">
                Mã OTP đã được gửi đến email của bạn. Mã có hiệu lực trong{' '}
                <strong>{otpInfo.expiresInMinutes} phút</strong>. Bạn có{' '}
                <strong>{otpInfo.maxAttempts} lần</strong> thử.
              </p>

              <div className="cnt-otp-card__form">
                <div className="cnt-field-group">
                  <label className="cnt-field-group__label">Nhập mã OTP</label>
                  <OtpInput
                    value={otpValue}
                    onChange={setOtpValue}
                    disabled={signStatus === 'loading'}
                    autoFocus
                  />
                  <span className="cnt-field-group__hint">
                    Nhấn ô bất kỳ rồi dán mã từ email, hoặc nhập từng số.
                  </span>
                </div>

                <div className="cnt-actions">
                  <button
                    className="cnt-btn cnt-btn--primary"
                    onClick={() => void handleSign()}
                    disabled={otpValue.length < 6 || signStatus === 'loading'}
                  >
                    {signStatus === 'loading' ? 'Đang xác nhận…' : 'Xác nhận ký'}
                  </button>
                  <button
                    className="cnt-btn cnt-btn--secondary"
                    onClick={() => {
                      setShowOtpForm(false);
                      setOtpValue('');
                      resetOtp();
                      resetSign();
                    }}
                    disabled={signStatus === 'loading'}
                  >
                    Hủy
                  </button>
                </div>
              </div>
            </div>
          )}

          {!canSign && contract.status !== 'PENDING' && (
            <div className="cnt-alert cnt-alert--info">
              Hợp đồng này không ở trạng thái chờ ký (hiện tại: {contract.statusLabel}).
            </div>
          )}
        </div>
      )}

      {/* Terms Summary Card */}
      {contract.termsSummary && (
        <div className="cnt-card">
          <h2 className="cnt-card__title">Nội dung hợp đồng</h2>
          <div className="cnt-terms">{contract.termsSummary}</div>
          {contract.contractFileUrl && (
            <div style={{ marginTop: 'var(--space-md)' }}>
              <a
                href={contract.contractFileUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="cnt-btn cnt-btn--secondary"
              >
                Tải file hợp đồng
              </a>
            </div>
          )}
        </div>
      )}

      {/* Contract metadata */}
      <div
        style={{
          marginTop: 'var(--space-lg)',
          fontSize: 'var(--fs-body-sm)',
          color: 'var(--color-text-disabled)',
          display: 'flex',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '4px',
        }}
      >
        <span>Cập nhật lần cuối: {contract.updatedAt}</span>
        <button
          className="cnt-btn cnt-btn--secondary"
          onClick={() => void reloadAll()}
          style={{ padding: '4px 12px', fontSize: 'var(--fs-body-sm)' }}
        >
          Làm mới
        </button>
      </div>
    </ContractLayout>
  );
}
