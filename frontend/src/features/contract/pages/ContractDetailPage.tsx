import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ClassIssueModal } from '../../dispute/components/ClassIssueModal';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { useContractDetail, useSignContract } from '../hooks/useContract';
import type { ContractStatus } from '../types/contractTypes';
import './ContractPage.css';

const STATUS_LABEL: Record<ContractStatus, { label: string; cls: string }> = {
  PENDING: { label: 'Chờ ký', cls: 'contract-status--pending' },
  DRAFT: { label: 'Chưa ký', cls: 'contract-status--draft' },
  SIGNED: { label: 'Đã ký', cls: 'contract-status--signed' },
  ACTIVE: { label: 'Đang hoạt động', cls: 'contract-status--active' },
  COMPLETED: { label: 'Hoàn thành', cls: 'contract-status--completed' },
  TERMINATED: { label: 'Đã chấm dứt', cls: 'contract-status--terminated' },
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

export default function ContractDetailPage() {
  const { contractId } = useParams<{ contractId: string }>();
  const id = Number(contractId);

  const { contract, signatures, loading, error, reload } = useContractDetail();
  const { otpSent, sendingOtp, signing, error: signError, sendOtp, sign } = useSignContract();

  const [otpInput, setOtpInput] = useState('');
  const [otpSentSuccess, setOtpSentSuccess] = useState(false);
  const [signSuccess, setSignSuccess] = useState(false);
  const [issueModalOpen, setIssueModalOpen] = useState(false);

  useEffect(() => {
    if (id) void reload(id);
  }, [id, reload]);

  useEffect(() => {
    if (signSuccess && id) {
      void reload(id);
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
  const currentUserSigned = signatures?.signatures.some((signature) => signature.isCurrentUser) ?? false;
  const allSigned = signatures?.fullySigned ?? false;
  const signRequired = contract.status === 'DRAFT' || contract.status === 'PENDING';
  const canCreateIssue = contract.classId != null;
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
            {classDetailUrl ? (
              <Link
                className="tcs-btn tcs-btn--primary"
                to={classDetailUrl}
              >
                Xem lớp liên quan
              </Link>
            ) : null}
          </div>
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
            </dl>
            {contract.termsSummary ? (
              <div className="contract-terms">
                <h3>Nội dung hợp đồng</h3>
                <p>{contract.termsSummary}</p>
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
                  {signatures.signatures.map((signature) => (
                    <div
                      key={signature.signatureId}
                      className={`contract-signature-row${signature.isCurrentUser ? ' contract-signature-row--me' : ''}`}
                    >
                      <span className="contract-signature-check">✓</span>
                      <div>
                        <strong>
                          {signature.signerName}
                          {signature.isCurrentUser ? <em>Bạn</em> : null}
                        </strong>
                        <small>{signature.signerRole} · {formatDateTime(signature.signedAt)}</small>
                      </div>
                    </div>
                  ))}

                  {Array.from({ length: signatures.totalRequired - signatures.signatures.length }).map((_, index) => (
                    <div key={`pending-${index}`} className="contract-signature-row contract-signature-row--pending">
                      <span className="contract-signature-check">—</span>
                      <div>
                        <strong>Chưa ký</strong>
                        <small>Đang chờ</small>
                      </div>
                    </div>
                  ))}
                </div>
                {allSigned ? <p className="contract-success-note">Đủ chữ ký.</p> : null}
              </div>
            ) : (
              <div className="contract-muted">Đang tải trạng thái ký...</div>
            )}
          </section>

          {signRequired && !currentUserSigned ? (
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

          {currentUserSigned && signRequired ? (
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
          <ClassIssueModal
            open={issueModalOpen}
            classId={contract.classId}
            classTitle={contract.classTitle}
            onClose={() => setIssueModalOpen(false)}
          />
        </>
      ) : null}
    </div>
  );
}
