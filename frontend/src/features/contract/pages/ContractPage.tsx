import type { FormEvent } from 'react';
import { useState } from 'react';
import { contractApi } from '../api/contractApi';
import type { SignContractResponse } from '../types/contractTypes';
import { SubmittedApprovalsSection } from '../../profile/pages/GuardianApprovalPage';
import { ClientLayout } from '../../profile/components/ClientLayout';
import { LegalDelegationBanner } from '../../profile/components/LegalDelegationBanner';
import { extractApiErrorMessage, useDependentLinkStatus } from '../../profile/hooks/useDependentProfile';
import '../../profile/pages/DependentProfileLinkerPage.css';

export default function ContractPage() {
  const { linkStatus } = useDependentLinkStatus();
  const [tutorName, setTutorName] = useState('');
  const [subjectName, setSubjectName] = useState('');
  const [status, setStatus] = useState<'idle' | 'loading' | 'error' | 'success'>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [result, setResult] = useState<SignContractResponse | null>(null);

  async function handleSign(e: FormEvent) {
    e.preventDefault();
    if (!tutorName.trim()) return;
    setStatus('loading');
    setErrorMessage(null);
    try {
      const res = await contractApi.signContract({
        tutorName: tutorName.trim(),
        subjectName: subjectName.trim() || undefined,
      });
      setResult(res.data);
      setStatus('success');
      if (!res.data.pendingGuardianApproval) {
        setTutorName('');
        setSubjectName('');
      }
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, 'Không thể gửi yêu cầu ký hợp đồng.'));
      setStatus('error');
    }
  }

  return (
    <ClientLayout title="Hợp đồng gia sư" subtitle="Tạo và ký hợp đồng với gia sư.">
      <LegalDelegationBanner linkStatus={linkStatus} />

      {linkStatus?.parentApprovalRequired && (
        <div className="dpl-alert dpl-alert--info">
          Học sinh có thể gửi yêu cầu ký hợp đồng ngay. Phụ huynh sẽ nhận thông báo qua hệ thống và email để
          xác nhận trước khi hợp đồng có hiệu lực.
        </div>
      )}

      <div className="dpl-card">
        <h2 className="dpl-section-title">Ký hợp đồng</h2>
        {errorMessage && <div className="dpl-alert dpl-alert--error">{errorMessage}</div>}
        {result && (
          <div className={`dpl-alert ${result.pendingGuardianApproval ? 'dpl-alert--info' : 'dpl-alert--info'}`}>
            <strong>{result.message}</strong>
            {result.contractReference && (
              <p className="dpl-muted">Mã hợp đồng: {result.contractReference}</p>
            )}
            {result.signedAt && (
              <p className="dpl-muted">Ký lúc {new Date(result.signedAt).toLocaleString('vi-VN')}</p>
            )}
          </div>
        )}
        <form className="dpl-form" onSubmit={handleSign}>
          <label>
            Tên gia sư *
            <input
              className="dpl-field"
              value={tutorName}
              onChange={(e) => setTutorName(e.target.value)}
              required
              disabled={status === 'loading'}
            />
          </label>
          <label>
            Môn học
            <input
              className="dpl-field"
              value={subjectName}
              onChange={(e) => setSubjectName(e.target.value)}
              disabled={status === 'loading'}
            />
          </label>
          <button className="tcs-btn tcs-btn--primary" type="submit" disabled={status === 'loading'}>
            {status === 'loading' ? 'Đang gửi…' : 'Gửi yêu cầu ký hợp đồng'}
          </button>
        </form>
      </div>

      {linkStatus?.parentApprovalRequired && <SubmittedApprovalsSection />}
    </ClientLayout>
  );
}
