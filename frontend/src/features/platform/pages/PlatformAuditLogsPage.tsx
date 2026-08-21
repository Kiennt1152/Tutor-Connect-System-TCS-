import { useState, useEffect, useCallback } from 'react';
import { AdminLayout } from '../components/AdminLayout';
import { platformApi } from '../api/platformApi';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import type { AuditLogApiResponse, AuditLogFilters } from '../types/platformTypes';
import './PlatformAuditLogsPage.css';

const ACTION_LABELS: Record<string, string> = {
  // Authentication & Session
  LOGIN: 'Đăng nhập',
  LOGOUT: 'Đăng xuất',
  REGISTER: 'Đăng ký tài khoản',

  // Analytics & Exports
  EXPORT_ANALYTICS: 'Xuất báo cáo phân tích',
  SCHEDULED_REPORT_GENERATION: 'Tạo báo cáo định kỳ',

  // Users & Verification
  UPDATE_USER_STATUS: 'Cập nhật trạng thái người dùng',
  UPDATE_PROFILE: 'Cập nhật hồ sơ',
  SUBMIT_VERIFICATION: 'Nộp hồ sơ xác minh',
  REVIEW_VERIFICATION: 'Duyệt xác minh',
  CANCEL_VERIFICATION: 'Hủy hồ sơ xác minh',

  // Penalties
  ISSUE_PENALTY: 'Tạo xử phạt',
  REVOKE_PENALTY: 'Thu hồi xử phạt',

  // Announcements
  CREATE_ANNOUNCEMENT: 'Tạo thông báo',
  UPDATE_ANNOUNCEMENT: 'Sửa thông báo',
  DELETE_ANNOUNCEMENT: 'Xóa thông báo',

  // Classes & Marketplace
  CREATE_CLASS: 'Tạo lớp học',
  UPDATE_CLASS: 'Sửa lớp học',
  PUBLISH_CLASS: 'Đăng lớp học',
  APPLY_CLASS: 'Ứng tuyển lớp học',
  REGISTER_CLASS: 'Ghi danh lớp học',
  ADD_FAVORITE_TUTOR: 'Thêm gia sư yêu thích',
  REMOVE_FAVORITE_TUTOR: 'Bỏ gia sư yêu thích',

  // Center Operations
  CREATE_CENTER_CLASS: 'Tạo lớp trung tâm',
  UPDATE_CENTER_CLASS: 'Sửa lớp trung tâm',
  PUBLISH_RECRUITMENT_POST: 'Đăng tuyển dụng',
  POST_RECRUITMENT_FOR_REQUEST: 'Đăng tuyển theo yêu cầu',
  UNASSIGN_TUTOR: 'Hủy gán gia sư',
  ASSIGN_ASSISTANT: 'Gán trợ giảng',
  UNASSIGN_ASSISTANT: 'Hủy gán trợ giảng',
  DECIDE_RESCHEDULE: 'Duyệt dời lịch',
  DECIDE_SUBSTITUTION: 'Duyệt dạy thay',

  // FAQ & Catalog
  CREATE_FAQ: 'Tạo FAQ',
  UPDATE_FAQ: 'Sửa FAQ',
  DELETE_FAQ: 'Xóa FAQ',
  CREATE_CATEGORY: 'Tạo danh mục',
  UPDATE_CATEGORY: 'Sửa danh mục',
  DELETE_CATEGORY: 'Xóa danh mục',
  CREATE_SYSTEM_PARAMETER: 'Tạo tham số',
  UPDATE_SYSTEM_PARAMETER: 'Sửa tham số',
  DELETE_SYSTEM_PARAMETER: 'Xóa tham số',

  // Notification Templates
  CREATE_NOTIFICATION_TEMPLATE: 'Tạo mẫu thông báo',
  UPDATE_NOTIFICATION_TEMPLATE: 'Sửa mẫu thông báo',
  DISABLE_NOTIFICATION_TEMPLATE: 'Vô hiệu mẫu thông báo',

  // Support Tickets
  RESPOND_TICKET: 'Phản hồi ticket',
  CLOSE_TICKET: 'Đóng ticket',
  UPDATE_TICKET: 'Cập nhật ticket',
  MERGE_TICKET: 'Gộp ticket',
  SLA_BREACH_ESCALATION: 'Nâng cấp SLA',
  REDIRECT_TICKET_TO_DISPUTE: 'Chuyển sang tranh chấp',

  // Reports & Disputes & Reviews
  CREATE_REPORT: 'Tạo báo cáo',
  RESOLVE_REPORT: 'Xử lý báo cáo',
  VIEW_CIRCUMVENTION_CONVERSATION: 'Xem hội thoại lách sàn',
  REVIEW_CIRCUMVENTION: 'Xử lý lách sàn',
  VERIFY_REVIEW: 'Duyệt đánh giá',
  REJECT_REVIEW: 'Từ chối đánh giá',
  DELETE_REVIEW: 'Xóa đánh giá',
  CREATE_DISPUTE: 'Tạo tranh chấp',
  RESOLVE_DISPUTE: 'Xử lý tranh chấp',

  // Financial Controls
  UPDATE_FEE_RATE: 'Cập nhật phí sàn',
  UPDATE_REFUND_POLICY: 'Cập nhật chính sách hoàn tiền',
  UPDATE_WITHDRAWAL_CONTROLS: 'Cập nhật kiểm soát rút tiền',
};

const ACTION_TONES: Record<string, string> = {
  // Positive / Review / Success (Green)
  REVIEW_VERIFICATION: 'review',
  VERIFY_REVIEW: 'review',
  RESOLVE_REPORT: 'review',
  RESOLVE_DISPUTE: 'review',
  DECIDE_RESCHEDULE: 'review',
  DECIDE_SUBSTITUTION: 'review',

  // Create / Add / Register / Login (Teal / Cyan)
  CREATE_CLASS: 'create',
  CREATE_CENTER_CLASS: 'create',
  CREATE_ANNOUNCEMENT: 'create',
  CREATE_FAQ: 'create',
  CREATE_CATEGORY: 'create',
  CREATE_SYSTEM_PARAMETER: 'create',
  CREATE_NOTIFICATION_TEMPLATE: 'create',
  CREATE_REPORT: 'create',
  CREATE_DISPUTE: 'create',
  PUBLISH_CLASS: 'create',
  PUBLISH_RECRUITMENT_POST: 'create',
  POST_RECRUITMENT_FOR_REQUEST: 'create',
  REGISTER_CLASS: 'create',
  REGISTER: 'create',
  LOGIN: 'create',
  ADD_FAVORITE_TUTOR: 'create',
  ASSIGN_ASSISTANT: 'create',

  // Update / Edit / Modify / Export (Blue)
  UPDATE_USER_STATUS: 'update',
  UPDATE_PROFILE: 'update',
  UPDATE_ANNOUNCEMENT: 'update',
  UPDATE_CLASS: 'update',
  UPDATE_CENTER_CLASS: 'update',
  UPDATE_FAQ: 'update',
  UPDATE_CATEGORY: 'update',
  UPDATE_SYSTEM_PARAMETER: 'update',
  UPDATE_NOTIFICATION_TEMPLATE: 'update',
  UPDATE_TICKET: 'update',
  RESPOND_TICKET: 'update',
  CLOSE_TICKET: 'update',
  MERGE_TICKET: 'update',
  EXPORT_ANALYTICS: 'update',
  SCHEDULED_REPORT_GENERATION: 'update',
  UPDATE_FEE_RATE: 'update',
  UPDATE_REFUND_POLICY: 'update',
  UPDATE_WITHDRAWAL_CONTROLS: 'update',
  LOGOUT: 'update',

  // Warnings / Escalation / In-Progress (Amber / Orange)
  APPLY_CLASS: 'warning',
  SUBMIT_VERIFICATION: 'warning',
  REVOKE_PENALTY: 'warning',
  SLA_BREACH_ESCALATION: 'warning',
  REDIRECT_TICKET_TO_DISPUTE: 'warning',
  VIEW_CIRCUMVENTION_CONVERSATION: 'warning',
  REVIEW_CIRCUMVENTION: 'warning',

  // Danger / Deletion / Penalty (Red)
  ISSUE_PENALTY: 'danger',
  DELETE_ANNOUNCEMENT: 'danger',
  DELETE_FAQ: 'danger',
  DELETE_CATEGORY: 'danger',
  DELETE_SYSTEM_PARAMETER: 'danger',
  DELETE_REVIEW: 'danger',
  REJECT_REVIEW: 'danger',
  CANCEL_VERIFICATION: 'danger',
  DISABLE_NOTIFICATION_TEMPLATE: 'danger',
  UNASSIGN_TUTOR: 'danger',
  UNASSIGN_ASSISTANT: 'danger',
  REMOVE_FAVORITE_TUTOR: 'danger',
};

function getActionLabel(action: string): string {
  if (ACTION_LABELS[action]) return ACTION_LABELS[action];
  return action
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

function getActionTone(action: string): string {
  if (ACTION_TONES[action]) return ACTION_TONES[action];
  const upper = action.toUpperCase();
  if (upper.includes('DELETE') || upper.includes('REVOKE') || upper.includes('REJECT') || upper.includes('CANCEL') || upper.includes('DISABLE')) {
    return 'danger';
  }
  if (upper.includes('REVIEW') || upper.includes('RESOLVE') || upper.includes('VERIFY') || upper.includes('APPROVE')) {
    return 'review';
  }
  if (upper.includes('CREATE') || upper.includes('REGISTER') || upper.includes('PUBLISH') || upper.includes('ADD') || upper.includes('LOGIN')) {
    return 'create';
  }
  if (upper.includes('SLA') || upper.includes('DISPUTE') || upper.includes('REPORT') || upper.includes('WARNING')) {
    return 'warning';
  }
  if (upper.includes('UPDATE') || upper.includes('EXPORT') || upper.includes('RESPOND') || upper.includes('EDIT')) {
    return 'update';
  }
  return 'default';
}

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
                const tone = getActionTone(log.action);
                const label = getActionLabel(log.action);
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
