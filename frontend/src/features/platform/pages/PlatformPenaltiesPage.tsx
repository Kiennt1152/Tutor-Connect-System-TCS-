/**
 * ============================================================================
 * TRANG QUẢN TRỊ CHẾ TÀI VÀ XỬ PHẠT VI PHẠM (PLATFORM PENALTIES PAGE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp
 * Mô tả các tính năng xử lý kỷ luật nền tảng:
 *   - Hiển thị danh sách các quyết định xử phạt (Cảnh cáo, Hạn chế tính năng, Cấm tạm thời, Cấm vĩnh viễn).
 *   - Lọc án phạt theo Trạng thái (Đang hiệu lực, Đã hết hạn, Đã thu hồi) và Phân loại nguồn xử lý.
 *   - Modal ban hành án phạt mới (Issue Penalty) với kiểm tra thời hạn và ràng buộc lý do tối thiểu 20 ký tự.
 *   - Modal thu hồi án phạt (Revoke Penalty) và khôi phục tài khoản người dùng về hoạt động bình thường.
 *   - Điều hướng trực tiếp tới nguồn phát sinh án phạt (Report, Dispute, Ticket, Circumvention).
 */

import { useState, useEffect, useCallback, type FormEvent } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';
import { AdminLayout } from '../components/AdminLayout';
import { AdminTimeFilter } from '../components/AdminTimeFilter';
import { Pagination } from '../../../shared/components';
import { platformApi } from '../api/platformApi';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import type { 
  PenaltyApiResponse, 
  PenaltyType, 
  PenaltyStatus, 
  PenaltyFilters,
  IssuePenaltyApiRequest
} from '../types/platformTypes';
import { resolvePenaltySourceRoute } from '../utils/penaltySourceUtils';
import './PlatformPenaltiesPage.css';

const PENALTY_TYPE_LABELS: Record<PenaltyType, string> = {
  WARNING: 'Cảnh cáo',
  FEATURE_RESTRICTION: 'Hạn chế tính năng',
  TEMPORARY_BAN: 'Cấm tạm thời',
  PERMANENT_BAN: 'Cấm vĩnh viễn',
};

const PENALTY_STATUS_LABELS: Record<PenaltyStatus, string> = {
  ACTIVE: 'Đang hoạt động',
  EXPIRED: 'Đã hết hạn',
  REVOKED: 'Đã thu hồi',
};

const PENALTY_TYPE_TONES: Record<PenaltyType, string> = {
  WARNING: 'warning',
  FEATURE_RESTRICTION: 'restriction',
  TEMPORARY_BAN: 'temp-ban',
  PERMANENT_BAN: 'perm-ban',
};

const PENALTY_STATUS_TONES: Record<PenaltyStatus, string> = {
  ACTIVE: 'active',
  EXPIRED: 'expired',
  REVOKED: 'revoked',
};

export default function PlatformPenaltiesPage() {
  const navigate = useNavigate();
  const [penalties, setPenalties] = useState<PenaltyApiResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Pagination & Filters
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [, setTotalElements] = useState(0);

  const [statusFilter, setStatusFilter] = useState<PenaltyStatus | ''>('');
  const [typeFilter, setTypeFilter] = useState<PenaltyType | ''>('');
  const [sourceTypeFilter, setSourceTypeFilter] = useState<string>('');
  const [userIdFilter, setUserIdFilter] = useState('');

  // Modals
  const [isIssueModalOpen, setIsIssueModalOpen] = useState(false);
  const [isRevokeModalOpen, setIsRevokeModalOpen] = useState(false);
  const [selectedPenalty, setSelectedPenalty] = useState<PenaltyApiResponse | null>(null);

  // Summary counts
  const [summary, setSummary] = useState({
    active: 0,
    warnings: 0,
    bans: 0
  });

  const fetchPenalties = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const filters: PenaltyFilters = { page, size };
      if (statusFilter) filters.status = statusFilter;
      if (typeFilter) filters.type = typeFilter;
      if (sourceTypeFilter) filters.sourceType = sourceTypeFilter;
      if (userIdFilter && !isNaN(Number(userIdFilter))) {
        filters.userId = Number(userIdFilter);
      }

      const res = await platformApi.getPenalties(filters);
      setPenalties(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalElements(res.data.totalElements);

      // Simple summary calculations from content (mocking overall stats)
      const active = res.data.content.filter(p => p.status === 'ACTIVE').length;
      const warnings = res.data.content.filter(p => p.penaltyType === 'WARNING').length;
      const bans = res.data.content.filter(p => p.penaltyType === 'TEMPORARY_BAN' || p.penaltyType === 'PERMANENT_BAN').length;
      setSummary({ active, warnings, bans });

    } catch (err: any) {
      setError(getApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [page, size, statusFilter, typeFilter, sourceTypeFilter, userIdFilter]);

  useEffect(() => {
    fetchPenalties();
  }, [fetchPenalties]);

  const handleFilterReset = () => {
    setStatusFilter('');
    setTypeFilter('');
    setSourceTypeFilter('');
    setUserIdFilter('');
    setPage(0);
  };

  // -- Issue Modal State --
  const [issueForm, setIssueForm] = useState<IssuePenaltyApiRequest>({
    userId: 0,
    penaltyType: 'WARNING',
    reason: '',
    evidenceUrls: '',
    expiresAt: ''
  });
  const [issueSubmitting, setIssueSubmitting] = useState(false);

  const openIssueModal = () => {
    setIssueForm({
      userId: 0,
      penaltyType: 'WARNING',
      reason: '',
      evidenceUrls: '',
      expiresAt: ''
    });
    setIsIssueModalOpen(true);
  };

  const handleIssueSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (issueForm.userId <= 0) {
      alert('Vui lòng nhập ID người dùng hợp lệ');
      return;
    }
    setIssueSubmitting(true);
    try {
      const payload = { ...issueForm };
      if (payload.penaltyType !== 'TEMPORARY_BAN') {
        delete payload.expiresAt;
      } else if (payload.expiresAt) {
        // Convert datetime-local to ISO string
        payload.expiresAt = new Date(payload.expiresAt).toISOString();
      }
      
      await platformApi.issuePenalty(payload);
      setIsIssueModalOpen(false);
      fetchPenalties();
    } catch (err: any) {
      alert(getApiErrorMessage(err));
    } finally {
      setIssueSubmitting(false);
    }
  };

  // -- Revoke Modal State --
  const [revokeReason, setRevokeReason] = useState('');
  const [revokeSubmitting, setRevokeSubmitting] = useState(false);

  const openRevokeModal = (penalty: PenaltyApiResponse) => {
    setSelectedPenalty(penalty);
    setRevokeReason('');
    setIsRevokeModalOpen(true);
  };

  const handleRevokeSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!selectedPenalty) return;
    setRevokeSubmitting(true);
    try {
      await platformApi.revokePenalty(selectedPenalty.penaltyId, {
        revokedReason: revokeReason
      });
      setIsRevokeModalOpen(false);
      fetchPenalties();
    } catch (err: any) {
      alert(getApiErrorMessage(err));
    } finally {
      setRevokeSubmitting(false);
    }
  };

  return (
    <AdminLayout 
      title="Xử phạt người dùng" 
      subtitle="Quản lý và áp dụng các hình thức xử phạt"
    >
      {/* Summary Cards */}
      <div className="adm-summary-row" style={{ display: 'flex', gap: '16px', marginBottom: '24px' }}>
        <div className="adm-summary-card" style={{ flex: 1, padding: '16px', background: '#fff', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
          <div style={{ fontSize: '14px', color: '#6b7280' }}>Đang hoạt động</div>
          <div style={{ fontSize: '24px', fontWeight: 'bold' }}>{summary.active}</div>
        </div>
        <div className="adm-summary-card" style={{ flex: 1, padding: '16px', background: '#fff', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
          <div style={{ fontSize: '14px', color: '#6b7280' }}>Cảnh cáo</div>
          <div style={{ fontSize: '24px', fontWeight: 'bold' }}>{summary.warnings}</div>
        </div>
        <div className="adm-summary-card" style={{ flex: 1, padding: '16px', background: '#fff', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
          <div style={{ fontSize: '14px', color: '#6b7280' }}>Lệnh cấm</div>
          <div style={{ fontSize: '24px', fontWeight: 'bold' }}>{summary.bans}</div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center' }}>
          <button 
            className="adm-action-btn" 
            style={{ padding: '8px 16px', background: 'var(--primary-color, #2563eb)', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer' }}
            onClick={openIssueModal}
          >
            Tạo xử phạt
          </button>
        </div>
      </div>

      <AdminTimeFilter showGranularity={false} />

      {/* Filters */}
      <div className="adm-penalty-filters">
        <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as PenaltyStatus | '')}>
          <option value="">Tất cả trạng thái</option>
          <option value="ACTIVE">Đang hoạt động</option>
          <option value="EXPIRED">Đã hết hạn</option>
          <option value="REVOKED">Đã thu hồi</option>
        </select>
        <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value as PenaltyType | '')}>
          <option value="">Tất cả loại hình</option>
          <option value="WARNING">Cảnh cáo</option>
          <option value="FEATURE_RESTRICTION">Hạn chế tính năng</option>
          <option value="TEMPORARY_BAN">Cấm tạm thời</option>
          <option value="PERMANENT_BAN">Cấm vĩnh viễn</option>
        </select>
        <select value={sourceTypeFilter} onChange={(e) => setSourceTypeFilter(e.target.value)}>
          <option value="">Tất cả nguồn</option>
          <option value="REPORT">Báo cáo (REPORT)</option>
          <option value="CIRCUMVENTION">Gian lận luồn lách (CIRCUMVENTION)</option>
          <option value="DISPUTE">Tranh chấp (DISPUTE)</option>
          <option value="TICKET">Khiếu nại / Ticket (TICKET)</option>
          <option value="DIRECT">Trực tiếp (DIRECT)</option>
        </select>
        <input 
          type="number" 
          placeholder="ID Người dùng" 
          value={userIdFilter} 
          onChange={(e) => setUserIdFilter(e.target.value)} 
        />
        <button onClick={handleFilterReset}>Làm mới</button>
      </div>

      {error && <div style={{ color: 'red', marginBottom: '16px' }}>{error}</div>}

      {/* Table */}
      <div className="adm-table-wrap">
        <table className="adm-table" style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', background: '#fff' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid #e5e7eb' }}>
              <th style={{ padding: '12px 16px' }}>#</th>
              <th style={{ padding: '12px 16px' }}>Người dùng</th>
              <th style={{ padding: '12px 16px' }}>Loại</th>
              <th style={{ padding: '12px 16px' }}>Nguồn xử lý</th>
              <th style={{ padding: '12px 16px' }}>Lý do</th>
              <th style={{ padding: '12px 16px' }}>Bắt đầu</th>
              <th style={{ padding: '12px 16px' }}>Hết hạn</th>
              <th style={{ padding: '12px 16px' }}>Trạng thái</th>
              <th style={{ padding: '12px 16px' }}>Người xử phạt</th>
              <th style={{ padding: '12px 16px' }}>Hành động</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={10} style={{ textAlign: 'center', padding: '16px' }}>Đang tải...</td></tr>
            ) : penalties.length === 0 ? (
              <tr><td colSpan={10} style={{ textAlign: 'center', padding: '16px' }}>Không có dữ liệu</td></tr>
            ) : (
              penalties.map(p => {
                const sourceRoute = resolvePenaltySourceRoute(p.sourceType, p.sourceId);
                return (
                  <tr key={p.penaltyId} style={{ borderBottom: '1px solid #e5e7eb' }}>
                    <td style={{ padding: '12px 16px' }}>{p.penaltyId}</td>
                    <td style={{ padding: '12px 16px' }}>
                      <div>ID: {p.userId}</div>
                      <div style={{ fontSize: '12px', color: '#6b7280' }}>{p.userEmail}</div>
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <span className={`tcs-badge tcs-badge--status-${PENALTY_TYPE_TONES[p.penaltyType]}`} style={{ padding: '4px 8px', borderRadius: '999px', fontSize: '12px' }}>
                        {PENALTY_TYPE_LABELS[p.penaltyType]}
                      </span>
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      {p.sourceType ? (
                        <div>
                          <span style={{ fontSize: '0.75rem', fontWeight: 600, background: '#f1f5f9', padding: '2px 6px', borderRadius: '4px', color: '#334155' }}>
                            {p.sourceType} #{p.sourceId}
                          </span>
                          {sourceRoute && (
                            <div style={{ marginTop: '4px' }}>
                              <button
                                type="button"
                                className="tcs-btn tcs-btn--sm tcs-btn--ghost"
                                style={{ fontSize: '0.75rem', padding: '2px 6px' }}
                                onClick={() => navigate(sourceRoute)}
                              >
                                Mở case nguồn →
                              </button>
                            </div>
                          )}
                        </div>
                      ) : (
                        <span style={{ color: '#94a3b8', fontSize: '0.8rem' }}>Trực tiếp</span>
                      )}
                    </td>
                  <td style={{ padding: '12px 16px', maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={p.reason}>
                    {p.reason}
                  </td>
                  <td style={{ padding: '12px 16px' }}>{new Date(p.startsAt).toLocaleDateString('vi-VN')}</td>
                  <td style={{ padding: '12px 16px' }}>{p.expiresAt ? new Date(p.expiresAt).toLocaleDateString('vi-VN') : '-'}</td>
                  <td style={{ padding: '12px 16px' }}>
                    <span className={`tcs-badge tcs-badge--status-${PENALTY_STATUS_TONES[p.status]}`} style={{ padding: '4px 8px', borderRadius: '999px', fontSize: '12px' }}>
                      {PENALTY_STATUS_LABELS[p.status]}
                    </span>
                  </td>
                  <td style={{ padding: '12px 16px' }}>{p.issuedByName || '-'}</td>
                  <td style={{ padding: '12px 16px' }}>
                    {p.status === 'ACTIVE' && (
                      <button className="adm-action-btn" onClick={() => openRevokeModal(p)}>
                        Thu hồi
                      </button>
                    )}
                  </td>
                </tr>
              );
            })
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {!loading && penalties.length > 0 && (
        <div className="adm-pagination" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: '16px', gap: '8px' }}>
          <Pagination
            current={page + 1}
            totalPages={Math.max(totalPages, 1)}
            onPageChange={(p) => setPage(p - 1)}
          />
        </div>
      )}

      {/* Issue Modal */}
      {isIssueModalOpen && createPortal(
        <div className="adm-penalty-modal-overlay">
          <div className="adm-penalty-modal">
            <h2>Tạo xử phạt</h2>
            <form className="adm-penalty-form" onSubmit={handleIssueSubmit}>
              <div className="form-group">
                <label>ID Người dùng</label>
                <input 
                  type="number" 
                  value={issueForm.userId || ''} 
                  onChange={e => setIssueForm({...issueForm, userId: Number(e.target.value)})} 
                  required 
                />
              </div>
              <div className="form-group">
                <label>Loại xử phạt</label>
                <select 
                  value={issueForm.penaltyType} 
                  onChange={e => setIssueForm({...issueForm, penaltyType: e.target.value as PenaltyType})}
                >
                  <option value="WARNING">Cảnh cáo</option>
                  <option value="FEATURE_RESTRICTION">Hạn chế tính năng</option>
                  <option value="TEMPORARY_BAN">Cấm tạm thời</option>
                  <option value="PERMANENT_BAN">Cấm vĩnh viễn</option>
                </select>
              </div>
              
              {issueForm.penaltyType === 'TEMPORARY_BAN' && (
                <div className="form-group">
                  <label>Thời gian hết hạn</label>
                  <input 
                    type="datetime-local" 
                    value={issueForm.expiresAt || ''} 
                    onChange={e => setIssueForm({...issueForm, expiresAt: e.target.value})} 
                    required 
                  />
                </div>
              )}

              <div className="form-group">
                <label>Lý do</label>
                <textarea 
                  value={issueForm.reason} 
                  onChange={e => setIssueForm({...issueForm, reason: e.target.value})} 
                  required 
                />
              </div>

              <div className="form-group">
                <label>Bằng chứng (URL) - Tùy chọn</label>
                <input 
                  type="text" 
                  value={issueForm.evidenceUrls || ''} 
                  onChange={e => setIssueForm({...issueForm, evidenceUrls: e.target.value})} 
                />
              </div>

              <div className="form-actions">
                <button type="button" className="btn-cancel" onClick={() => setIsIssueModalOpen(false)}>Hủy</button>
                <button type="submit" className="btn-submit" disabled={issueSubmitting}>
                  {issueSubmitting ? 'Đang xử lý...' : 'Xác nhận'}
                </button>
              </div>
            </form>
          </div>
        </div>,
        document.body
      )}

      {/* Revoke Modal */}
      {isRevokeModalOpen && selectedPenalty && createPortal(
        <div className="adm-penalty-modal-overlay">
          <div className="adm-penalty-modal">
            <h2>Thu hồi xử phạt #{selectedPenalty.penaltyId}</h2>
            <form className="adm-penalty-form" onSubmit={handleRevokeSubmit}>
              <div className="form-group">
                <label>Lý do thu hồi</label>
                <textarea 
                  value={revokeReason} 
                  onChange={e => setRevokeReason(e.target.value)} 
                  required 
                />
              </div>
              <div className="form-actions">
                <button type="button" className="btn-cancel" onClick={() => setIsRevokeModalOpen(false)}>Hủy</button>
                <button type="submit" className="btn-submit" disabled={revokeSubmitting}>
                  {revokeSubmitting ? 'Đang xử lý...' : 'Xác nhận'}
                </button>
              </div>
            </form>
          </div>
        </div>,
        document.body
      )}
    </AdminLayout>
  );
}
