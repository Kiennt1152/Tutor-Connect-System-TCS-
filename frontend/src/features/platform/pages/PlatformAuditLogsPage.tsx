import { useState, useEffect, useCallback } from 'react';
import { AdminLayout } from '../components/AdminLayout';
import { platformApi } from '../api/platformApi';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import type { AuditLogApiResponse, AuditLogFilters } from '../types/platformTypes';
import './PlatformAuditLogsPage.css';

const ACTION_LABELS: Record<string, string> = {
  UPDATE_USER_STATUS: 'Cập nhật trạng thái',
  REVIEW_VERIFICATION: 'Duyệt xác minh',
  ISSUE_PENALTY: 'Tạo xử phạt',
  REVOKE_PENALTY: 'Thu hồi xử phạt',
  CREATE_ANNOUNCEMENT: 'Tạo thông báo',
  UPDATE_ANNOUNCEMENT: 'Sửa thông báo',
  DELETE_ANNOUNCEMENT: 'Xóa thông báo',
  CREATE_CLASS: 'Tạo lớp học',
  APPLY_CLASS: 'Ứng tuyển lớp học',
  UPDATE_PROFILE: 'Cập nhật hồ sơ',
  SUBMIT_VERIFICATION: 'Nộp hồ sơ xác minh',
  MERGE_TICKET: 'Gộp ticket',
  SLA_BREACH_ESCALATION: 'Nâng cấp SLA',
  REDIRECT_TICKET_TO_DISPUTE: 'Chuyển sang tranh chấp',
};

const ACTION_TONES: Record<string, string> = {
  UPDATE_USER_STATUS: 'update',
  REVIEW_VERIFICATION: 'review',
  ISSUE_PENALTY: 'danger',
  REVOKE_PENALTY: 'warning',
  CREATE_ANNOUNCEMENT: 'create',
  UPDATE_ANNOUNCEMENT: 'update',
  DELETE_ANNOUNCEMENT: 'danger',
  CREATE_CLASS: 'create',
  APPLY_CLASS: 'warning',
  UPDATE_PROFILE: 'update',
  SUBMIT_VERIFICATION: 'warning',
  MERGE_TICKET: 'update',
  SLA_BREACH_ESCALATION: 'warning',
  REDIRECT_TICKET_TO_DISPUTE: 'warning',
};

const JsonDisplay = ({ value }: { value: string | null }) => {
  const [expanded, setExpanded] = useState(false);

  if (!value) return <span>-</span>;

  let parsed = value;
  try {
    parsed = JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    // Keep as is if not valid JSON
  }

  const isLong = parsed.length > 50;
  const displayValue = expanded ? parsed : (isLong ? parsed.slice(0, 50) + '...' : parsed);

  return (
    <div>
      <div className={expanded ? 'adm-audit-json' : ''}>{displayValue}</div>
      {isLong && (
        <button
          className="adm-audit-json-toggle"
          onClick={() => setExpanded(!expanded)}
        >
          {expanded ? 'Thu gọn' : 'Xem chi tiết'}
        </button>
      )}
    </div>
  );
};

export default function PlatformAuditLogsPage() {
  const [logs, setLogs] = useState<AuditLogApiResponse[]>([]);
  const [, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [filters, setFilters] = useState<AuditLogFilters>({
    page: 0,
    size: 20,
  });

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await platformApi.getAuditLogs(filters);
      setLogs(res.data.content);
      setTotalElements(res.data.totalElements);
      setTotalPages(res.data.totalPages);
    } catch (err: unknown) {
      setError(getApiErrorMessage(err) || 'Không thể tải nhật ký hoạt động.');
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    // The request lifecycle updates this page's loading, error and result states.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchLogs();
  }, [fetchLogs]);

  const handleFilterChange = (key: keyof AuditLogFilters, value: string | number | undefined) => {
    setFilters((prev) => ({
      ...prev,
      [key]: value || undefined,
      page: 0, // Reset to page 0 on filter change
    }));
  };

  const handleResetFilters = () => {
    setFilters({ page: 0, size: 20 });
  };

  return (
    <AdminLayout
      title="Nhật ký hoạt động"
      subtitle="Theo dõi các thay đổi quan trọng trên hệ thống."
    >
      <div className="adm-audit-filters">
        <input
          type="number"
          placeholder="Actor ID"
          value={filters.actorId || ''}
          onChange={(e) => handleFilterChange('actorId', e.target.value ? Number(e.target.value) : '')}
        />
        <select
          value={filters.action || ''}
          onChange={(e) => handleFilterChange('action', e.target.value)}
        >
          <option value="">Tất cả Hành động</option>
          {Object.keys(ACTION_LABELS).map((action) => (
            <option key={action} value={action}>
              {ACTION_LABELS[action]}
            </option>
          ))}
        </select>
        <select
          value={filters.entityType || ''}
          onChange={(e) => handleFilterChange('entityType', e.target.value)}
        >
          <option value="">Tất cả Đối tượng</option>
          <option value="User">User</option>
          <option value="VerificationRequest">VerificationRequest</option>
          <option value="UserPenalty">UserPenalty</option>
          <option value="Announcement">Announcement</option>
        </select>
        <input
          type="datetime-local"
          value={filters.from || ''}
          onChange={(e) => handleFilterChange('from', e.target.value)}
        />
        <input
          type="datetime-local"
          value={filters.to || ''}
          onChange={(e) => handleFilterChange('to', e.target.value)}
        />
        <button
          onClick={handleResetFilters}
          className="adm-btn adm-btn-secondary"
          style={{ padding: '8px 12px', border: '1px solid #d1d5db', background: '#fff', borderRadius: '6px', cursor: 'pointer' }}
        >
          Reset
        </button>
      </div>

      {error && <div className="adm-error-message" style={{ color: 'red', marginBottom: '16px' }}>{error}</div>}

      <div className="adm-table-wrap adm-audit-table-wrap">
        <table className="adm-table adm-audit-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Thời gian</th>
              <th>Actor</th>
              <th>Hành động</th>
              <th>Đối tượng</th>
              <th>ID ĐT</th>
              <th style={{ width: '25%' }}>Giá trị cũ</th>
              <th style={{ width: '25%' }}>Giá trị mới</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '24px' }}>
                  Đang tải...
                </td>
              </tr>
            ) : logs.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', padding: '24px' }}>
                  Không tìm thấy nhật ký nào.
                </td>
              </tr>
            ) : (
              logs.map((log) => {
                const tone = ACTION_TONES[log.action] || 'default';
                const label = ACTION_LABELS[log.action] || log.action;
                return (
                  <tr key={log.auditId}>
                    <td>#{log.auditId}</td>
                    <td>{new Date(log.createdAt).toLocaleString('vi-VN')}</td>
                    <td>
                      {log.actorEmail ? (
                        <>
                          <div style={{ fontWeight: '500' }}>{log.actorEmail}</div>
                          {log.actorRole && (
                            <span className="adm-badge adm-badge-default" style={{ fontSize: '10px', margin: '4px 0', display: 'inline-block' }}>
                              {log.actorRole}
                            </span>
                          )}
                          <div style={{ fontSize: '12px', color: '#6b7280' }}>ID: {log.actorId}</div>
                        </>
                      ) : (
                        'System'
                      )}
                    </td>
                    <td>
                      <span className={`adm-badge adm-badge-${tone}`}>{label}</span>
                    </td>
                    <td>{log.entityType}</td>
                    <td>{log.entityId}</td>
                    <td><JsonDisplay value={log.oldValue} /></td>
                    <td><JsonDisplay value={log.newValue} /></td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="adm-pagination" style={{ display: 'flex', gap: '8px', marginTop: '16px', alignItems: 'center' }}>
          <button
            onClick={() => handleFilterChange('page', Math.max(0, filters.page - 1))}
            disabled={filters.page === 0}
            style={{ padding: '6px 12px' }}
          >
            Trước
          </button>
          <span>
            Trang {filters.page + 1} / {totalPages}
          </span>
          <button
            onClick={() => handleFilterChange('page', Math.min(totalPages - 1, filters.page + 1))}
            disabled={filters.page >= totalPages - 1}
            style={{ padding: '6px 12px' }}
          >
            Sau
          </button>
        </div>
      )}
    </AdminLayout>
  );
}
