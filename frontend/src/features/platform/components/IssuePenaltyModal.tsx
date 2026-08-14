import { useState, useEffect, type FormEvent } from 'react';
import { createPortal } from 'react-dom';
import { platformApi } from '../api/platformApi';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import type { 
  PenaltyType, 
  PenaltyApiResponse, 
  IssuePenaltyApiRequest 
} from '../types/platformTypes';
import { buildPenaltySource, type PenaltySourceType } from '../utils/penaltySourceUtils';
import './IssuePenaltyModal.css';

export interface UserOption {
  label: string;
  userId: number;
}

export interface IssuePenaltyModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: (penalty: PenaltyApiResponse) => void;
  initialUserId?: number;
  initialReason?: string;
  initialEvidenceUrls?: string;
  sourceType?: PenaltySourceType;
  sourceId?: string | number;
  sourceTaskId?: string;
  title?: string;
  userOptions?: UserOption[];
}

export function IssuePenaltyModal({
  isOpen,
  onClose,
  onSuccess,
  initialUserId,
  initialReason,
  initialEvidenceUrls,
  sourceType,
  sourceId,
  sourceTaskId,
  title,
  userOptions,
}: IssuePenaltyModalProps) {
  const [userId, setUserId] = useState<number>(initialUserId || 0);
  const [penaltyType, setPenaltyType] = useState<PenaltyType>('WARNING');
  const [reason, setReason] = useState(initialReason || '');
  const [evidenceUrls, setEvidenceUrls] = useState(initialEvidenceUrls || '');
  const [expiresAt, setExpiresAt] = useState('');
  const [restrictionDetails, setRestrictionDetails] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      setUserId(initialUserId || (userOptions && userOptions.length > 0 ? userOptions[0].userId : 0));
      setReason(initialReason || '');
      setEvidenceUrls(initialEvidenceUrls || '');
      setPenaltyType('WARNING');
      setExpiresAt('');
      setRestrictionDetails('');
      setError(null);
    }
  }, [isOpen, initialUserId, initialReason, initialEvidenceUrls, userOptions]);

  if (!isOpen) return null;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!userId || userId <= 0) {
      setError('Vui lòng chọn hoặc nhập ID người dùng hợp lệ.');
      return;
    }
    if (!reason.trim()) {
      setError('Vui lòng nhập lý do xử phạt.');
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const source = sourceType && sourceId ? buildPenaltySource(sourceType, sourceId, sourceTaskId) : undefined;
      const payload: IssuePenaltyApiRequest = {
        userId,
        penaltyType,
        reason: reason.trim(),
        evidenceUrls: evidenceUrls.trim() || undefined,
        restrictionDetails: penaltyType === 'FEATURE_RESTRICTION' && restrictionDetails.trim() ? restrictionDetails.trim() : undefined,
        expiresAt: penaltyType === 'TEMPORARY_BAN' && expiresAt ? new Date(expiresAt).toISOString() : undefined,
        sourceType: source?.sourceType,
        sourceId: source?.sourceId,
        sourceTaskId: source?.sourceTaskId,
      };

      const res = await platformApi.issuePenalty(payload);
      onSuccess?.(res.data);
      onClose();
    } catch (err: any) {
      setError(getApiErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return createPortal(
    <div className="adm-penalty-modal-overlay" role="dialog" aria-modal="true">
      <div className="adm-penalty-modal">
        <div className="adm-penalty-modal__header">
          <h2 style={{ margin: 0, fontSize: '1.25rem' }}>{title || 'Tạo quyết định xử phạt'}</h2>
          <button type="button" className="btn-close" onClick={onClose} aria-label="Đóng">×</button>
        </div>

        {sourceType && sourceId && (
          <div className="adm-penalty-modal__source-badge">
            📌 Nguồn xử lý: <strong>{sourceType} #{sourceId}</strong>
          </div>
        )}

        {error && (
          <div className="adm-penalty-modal__error">
            {error}
          </div>
        )}

        <form className="adm-penalty-form" onSubmit={handleSubmit}>
          {userOptions && userOptions.length > 0 ? (
            <div className="form-group">
              <label>Đối tượng xử phạt</label>
              <select
                value={userId}
                onChange={(e) => setUserId(Number(e.target.value))}
                required
              >
                {userOptions.map((opt) => (
                  <option key={opt.userId} value={opt.userId}>
                    {opt.label} (ID #{opt.userId})
                  </option>
                ))}
              </select>
            </div>
          ) : (
            <div className="form-group">
              <label>ID Người dùng vi phạm</label>
              <input
                type="number"
                value={userId || ''}
                onChange={(e) => setUserId(Number(e.target.value))}
                placeholder="Nhập ID người dùng"
                required
              />
            </div>
          )}

          <div className="form-group">
            <label>Hình thức xử phạt</label>
            <select
              value={penaltyType}
              onChange={(e) => setPenaltyType(e.target.value as PenaltyType)}
            >
              <option value="WARNING">Cảnh cáo (Warning)</option>
              <option value="FEATURE_RESTRICTION">Hạn chế tính năng (Feature Restriction)</option>
              <option value="TEMPORARY_BAN">Khóa tài khoản tạm thời (Temporary Ban)</option>
              <option value="PERMANENT_BAN">Khóa tài khoản vĩnh viễn (Permanent Ban)</option>
            </select>
          </div>

          {penaltyType === 'FEATURE_RESTRICTION' && (
            <div className="form-group">
              <label>Mã tính năng bị khóa (JSON)</label>
              <input
                type="text"
                placeholder='["MESSAGING", "CLASS_POSTING", "WITHDRAWAL"]'
                value={restrictionDetails}
                onChange={(e) => setRestrictionDetails(e.target.value)}
                required
              />
            </div>
          )}

          {penaltyType === 'TEMPORARY_BAN' && (
            <div className="form-group">
              <label>Thời hạn hết hiệu lực</label>
              <input
                type="datetime-local"
                value={expiresAt}
                onChange={(e) => setExpiresAt(e.target.value)}
                required
              />
            </div>
          )}

          <div className="form-group">
            <label>Lý do & Căn cứ xử phạt</label>
            <textarea
              rows={3}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Nhập lý do chi tiết..."
              required
            />
          </div>

          <div className="form-group">
            <label>Bằng chứng đính kèm (URL) - Tùy chọn</label>
            <input
              type="text"
              value={evidenceUrls}
              onChange={(e) => setEvidenceUrls(e.target.value)}
              placeholder="https://..."
            />
          </div>

          <div className="form-actions">
            <button type="button" className="btn-cancel" onClick={onClose}>Hủy</button>
            <button type="submit" className="btn-submit" disabled={submitting}>
              {submitting ? 'Đang thực hiện...' : 'Xác nhận xử phạt'}
            </button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
}
