import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useContractDetail, useSignContract } from '../hooks/useContract';
import type { ContractStatus } from '../types/contractTypes';
import { APP_ROUTES } from '../../../shared/constants/routes';

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

  if (loading) return <div className="cdetail-loading">Đang tải hợp đồng...</div>;
  if (error) return <div className="cdetail-error">{error}</div>;

  if (!contract) return null;

  const st = STATUS_LABEL[contract.status] ?? { label: contract.status, cls: '' };
  const allSigned = signatures?.hasAllSignatures ?? false;
  const signRequired = contract.status === 'DRAFT' || contract.status === 'PENDING';

  return (
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

      <style>{`
        .cdetail-page { max-width: 900px; margin: 0 auto; padding: 24px 16px; font-family: 'Segoe UI', Arial, sans-serif; }
        .cdetail-topbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
        .cdetail-back { color: #2563eb; text-decoration: none; font-weight: 600; }
        .cdetail-back:hover { text-decoration: underline; }
        .cdetail-loading, .cdetail-error { text-align: center; padding: 48px; color: #64748b; }
        .cdetail-error { color: #dc2626; }
        .cdetail-grid { display: flex; flex-direction: column; gap: 16px; }
        .cdetail-card { background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
        .cdetail-card h2 { margin: 0 0 16px; font-size: 16px; color: #1a1a2e; border-bottom: 1px solid #e2e8f0; padding-bottom: 10px; }
        .cdetail-card h3 { margin: 16px 0 8px; font-size: 14px; color: #475569; }
        .cdetail-dl { display: grid; grid-template-columns: 130px 1fr; gap: 8px 16px; }
        .cdetail-dl dt { color: #64748b; font-size: 13px; font-weight: 500; }
        .cdetail-dl dd { margin: 0; font-size: 14px; color: #1e293b; }
        .contract-no { font-family: monospace; font-weight: 700; color: #2563eb; }
        .cdetail-terms { font-size: 14px; color: #475569; line-height: 1.6; background: #f8fafc; padding: 12px; border-radius: 8px; }
        .cdetail-parties { display: flex; flex-direction: column; gap: 12px; }
        .party-card { padding: 12px; background: #f8fafc; border-radius: 8px; }
        .party-role { font-size: 11px; font-weight: 700; text-transform: uppercase; color: #94a3b8; letter-spacing: 0.5px; }
        .party-name { font-size: 15px; font-weight: 600; color: #1e293b; margin-top: 4px; }
        .party-email { font-size: 13px; color: #64748b; margin-top: 2px; }
        .sig-loading { color: #94a3b8; font-size: 14px; }
        .sig-list { display: flex; flex-direction: column; gap: 10px; }
        .sig-item { display: flex; align-items: center; gap: 12px; padding: 10px; background: #f8fafc; border-radius: 8px; }
        .sig-me { background: #eff6ff; border: 1px solid #bfdbfe; }
        .sig-pending { opacity: 0.6; }
        .sig-check { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 700; background: #d1fae5; color: #065f46; flex-shrink: 0; }
        .sig-empty { background: #f1f5f9; color: #94a3b8; }
        .sig-name { font-size: 14px; font-weight: 600; color: #1e293b; }
        .sig-name-pending { color: #94a3b8; }
        .sig-role { font-size: 12px; color: #64748b; }
        .sig-time { font-size: 12px; color: #94a3b8; }
        .sig-me-badge { margin-left: 8px; background: #2563eb; color: #fff; font-size: 10px; padding: 2px 6px; border-radius: 10px; }
        .sig-progress { margin-top: 12px; }
        .sig-progress-bar { height: 6px; background: #e2e8f0; border-radius: 3px; overflow: hidden; }
        .sig-progress-fill { height: 100%; background: #2563eb; border-radius: 3px; transition: width 0.3s; }
        .sig-progress-text { font-size: 12px; color: #64748b; margin-top: 6px; }
        .sig-done { color: #059669; font-weight: 600; }
        .sign-card { border: 2px solid #bfdbfe; }
        .sign-error { background: #fee2e2; color: #991b1b; padding: 10px 14px; border-radius: 8px; font-size: 14px; margin-bottom: 12px; }
        .sign-step p { font-size: 14px; color: #475569; line-height: 1.5; margin: 0 0 12px; }
        .otp-sent-msg { color: #065f46; }
        .otp-hint { font-size: 13px !important; color: #64748b !important; }
        .otp-input-group { display: flex; gap: 8px; }
        .otp-input { flex: 1; padding: 10px 14px; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 18px; letter-spacing: 8px; text-align: center; outline: none; }
        .otp-input:focus { border-color: #2563eb; box-shadow: 0 0 0 3px rgba(37,99,235,0.1); }
        .btn-link-resend { background: none; border: none; color: #2563eb; cursor: pointer; font-size: 13px; margin-top: 8px; padding: 0; }
        .btn-link-resend:hover { text-decoration: underline; }
        .btn-link-resend:disabled { color: #94a3b8; cursor: not-allowed; }
        .sign-done-msg { background: #d1fae5; color: #065f46; padding: 12px 16px; border-radius: 8px; font-size: 14px; }
        .sign-complete-msg { background: #d1fae5; color: #065f46; padding: 12px 16px; border-radius: 8px; font-size: 14px; }
        .status-badge { display: inline-block; padding: 4px 12px; border-radius: 20px; font-size: 13px; font-weight: 600; }
        .status-draft { background: #fef3c7; color: #92400e; }
        .status-signed { background: #d1fae5; color: #065f46; }
        .status-active { background: #dbeafe; color: #1e40af; }
        .status-completed { background: #f1f5f9; color: #475569; }
        .status-terminated { background: #fee2e2; color: #991b1b; }
        .btn { padding: 10px 20px; border-radius: 8px; font-size: 14px; cursor: pointer; border: none; font-weight: 600; }
        .btn-primary { background: #2563eb; color: #fff; }
        .btn-primary:hover { background: #1d4ed8; }
        .btn-primary:disabled { background: #93c5fd; cursor: not-allowed; }
      `}</style>
    </div>
  );
}
