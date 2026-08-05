import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useContractDetail, useSignContract } from '../hooks/useContract';
import type { ContractStatus } from '../types/contractTypes';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import './ContractPage.css';

const STATUS_LABEL: Record<ContractStatus, { label: string; cls: string }> = {
  PENDING: { label: 'Chờ ký', cls: 'status-draft' },
  DRAFT: { label: 'Chưa ký', cls: 'status-draft' },
  SIGNED: { label: 'Đã ký', cls: 'status-signed' },
  ACTIVE: { label: 'Đang hoạt động', cls: 'status-active' },
  COMPLETED: { label: 'Hoàn thành', cls: 'status-completed' },
  TERMINATED: { label: 'Đã chấm dứt', cls: 'status-terminated' },
};

export default function ContractDetailPage() {
  const { contractId } = useParams<{ contractId: string }>();
  const id = Number(contractId);

  const { contract, signatures, loading, error, reload } = useContractDetail();
  const { otpSent, sendingOtp, signing, error: signError, sendOtp, sign } = useSignContract();

  const [otpInput, setOtpInput] = useState('');
  const [otpSentSuccess, setOtpSentSuccess] = useState(false);
  const [signSuccess, setSignSuccess] = useState(false);

  useEffect(() => {
    if (id) reload(id);
  }, [id, reload]);

  // Reload detail after sign
  useEffect(() => {
    if (signSuccess && id) {
      reload(id);
      setSignSuccess(false);
    }
  }, [signSuccess, id, reload]);

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

  if (loading) return (
    <div className="tcs-page">
      <HomeNavbar />
      <div className="cdetail-loading">Đang tải hợp đồng...</div>
    </div>
  );

  if (error) return (
    <div className="tcs-page">
      <HomeNavbar />
      <div className="cdetail-error">{error}</div>
    </div>
  );

  if (!contract) return null;

  const st = STATUS_LABEL[contract.status] ?? { label: contract.status, cls: '' };
  const allSigned = signatures?.hasAllSignatures ?? false;
  const signRequired = contract.status === 'DRAFT' || contract.status === 'PENDING';

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <div className="cdetail-page">
        <div className="cdetail-topbar">
          <Link to={APP_ROUTES.contract} className="cdetail-back">← Quay lại danh sách</Link>
          <span className={`status-badge ${st.cls}`}>{st.label}</span>
        </div>

        <div className="cdetail-grid">
          {/* Contract Info */}
          <section className="cdetail-card">
            <h2>Thông tin hợp đồng</h2>
            <dl className="cdetail-dl">
              <dt>Số hợp đồng</dt><dd className="contract-no">{contract.contractNo}</dd>
              <dt>Ngày tạo</dt><dd>{new Date(contract.createdAt).toLocaleDateString('vi-VN')}</dd>
              {contract.signedAt && <><dt>Ngày ký</dt><dd>{new Date(contract.signedAt).toLocaleDateString('vi-VN')}</dd></>}
              <dt>Học viên</dt><dd>{contract.clientName ?? '—'}</dd>
              <dt>Gia sư</dt><dd>{contract.tutorName ?? '—'}</dd>
            </dl>
            {contract.termsSummary && (
              <>
                <h3>Nội dung hợp đồng</h3>
                <p className="cdetail-terms">{contract.termsSummary}</p>
              </>
            )}
          </section>

          {/* Parties */}
          <section className="cdetail-card">
            <h2>Các bên ký</h2>
            <div className="cdetail-parties">
              {contract.tutorId && (
                <div className="party-card">
                  <div className="party-role">Gia sư</div>
                  <div className="party-name">{contract.tutorName}</div>
                  <div className="party-email">{contract.tutorEmail}</div>
                </div>
              )}
              {contract.centerId && (
                <div className="party-card">
                  <div className="party-role">Trung tâm</div>
                  <div className="party-name">{contract.centerName}</div>
                  <div className="party-email">{contract.centerEmail}</div>
                </div>
              )}
              {contract.clientId && (
                <div className="party-card">
                  <div className="party-role">Phụ huynh / Học viên</div>
                  <div className="party-name">{contract.clientName}</div>
                  <div className="party-email">{contract.clientEmail}</div>
                </div>
              )}
            </div>
          </section>

          {/* Signature Status */}
          <section className="cdetail-card signature-card">
            <h2>Trạng thái ký</h2>
            {signatures ? (
              <div className="sig-list">
                {signatures.signatures.map(sig => (
                  <div key={sig.signatureId} className={`sig-item`}>
                    <div className="sig-check">{'✓'}</div>
                    <div className="sig-info">
                      <div className="sig-name">
                        {sig.signerName ?? 'Chưa rõ'}
                      </div>
                      <div className="sig-role">{sig.partyLabel}</div>
                      <div className="sig-time">{sig.signedAt ? new Date(sig.signedAt).toLocaleString('vi-VN') : ''}</div>
                    </div>
                  </div>
                ))}

                {Array.from({ length: Math.max(0, signatures.requiredSignatures - signatures.signatures.length) }).map((_, i) => (
                  <div key={`pending-${i}`} className="sig-item sig-pending">
                    <div className="sig-check sig-empty">—</div>
                    <div className="sig-info">
                      <div className="sig-name sig-name-pending">Chưa ký</div>
                      <div className="sig-role">Đang chờ</div>
                    </div>
                  </div>
                ))}

                <div className="sig-progress">
                  <div className="sig-progress-bar">
                    <div
                      className="sig-progress-fill"
                      style={{ width: signatures.requiredSignatures > 0 ? `${(signatures.signedCount / signatures.requiredSignatures) * 100}%` : '0%' }}
                    />
                  </div>
                  <div className="sig-progress-text">
                    {signatures.signedCount} / {signatures.requiredSignatures} đã ký
                    {signatures.hasAllSignatures && <span className="sig-done"> — Đủ chữ ký!</span>}
                  </div>
                </div>
              </div>
            ) : <div className="sig-loading">Đang tải trạng thái ký...</div>}
          </section>

          {/* Sign Action */}
          {signRequired && (
            <section className="cdetail-card sign-card">
              <h2>Ký hợp đồng</h2>

              {signError && <div className="sign-error">{signError}</div>}

              {!otpSentSuccess ? (
                <div className="sign-step">
                  <p>Để ký hợp đồng, hệ thống sẽ gửi mã OTP về email của bạn.</p>
                  <button
                    className="btn btn-primary"
                    onClick={handleSendOtp}
                    disabled={sendingOtp}
                  >
                    {sendingOtp ? 'Đang gửi...' : 'Gửi mã OTP'}
                  </button>
                </div>
              ) : (
                <div className="sign-step">
                  <p className="otp-sent-msg">
                    {otpSent?.message ?? 'Mã OTP đã được gửi tới email của bạn'}
                  </p>
                  <p className="otp-hint">Nhập mã 6 chữ số đã nhận qua email để xác nhận ký.</p>
                  <div className="otp-input-group">
                    <input
                      type="text"
                      className="otp-input"
                      placeholder="Nhập mã OTP (6 chữ số)"
                      value={otpInput}
                      onChange={e => setOtpInput(e.target.value.replace(/\D/g, '').slice(0, 6))}
                      maxLength={6}
                      autoFocus
                    />
                    <button
                      className="btn btn-primary"
                      onClick={handleSign}
                      disabled={signing || otpInput.trim().length < 6}
                    >
                      {signing ? 'Đang ký...' : 'Xác nhận ký'}
                    </button>
                  </div>
                  <button className="btn-link-resend" onClick={handleSendOtp} disabled={sendingOtp}>
                    Gửi lại mã OTP
                  </button>
                </div>
              )}
            </section>
          )}

          {allSigned && contract.status !== 'SIGNED' && contract.status !== 'ACTIVE' && (
            <section className="cdetail-card">
              <h2>Hoàn tất ký</h2>
              <p className="sign-complete-msg">Tất cả các bên đã ký. Hợp đồng đã có hiệu lực.</p>
            </section>
          )}
        </div>
      </div>
    </div>
  );
}
