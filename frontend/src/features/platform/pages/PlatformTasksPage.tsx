/**
 * ============================================================================
 * TRANG HÀNG ĐỢI NHIỆM VỤ TRỰC BAN KHẨN CẤP (PLATFORM TASK QUEUE PAGE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp
 * Mô tả các tính năng bảng điều khiển nhiệm vụ:
 *   - Tập hợp các nhiệm vụ cần xử lý ngay: Xác minh hồ sơ, Báo cáo vi phạm, Support Ticket, Rút tiền, Hoàn tiền, Tranh chấp.
 *   - Thống kê tổng số công việc, số task vi phạm hạn chót SLA, và tổng số tiền rủi ro đang bị treo (Money At Risk).
 *   - Sắp xếp và phân loại thông minh theo mức độ khẩn cấp (Urgent > High > Medium > Low).
 *   - Điều hướng người dùng trực tiếp tới trang và modal xử lý chi tiết tương ứng.
 */

import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { AdminLayout } from '../components/AdminLayout';
import { AdminTimeFilter } from '../components/AdminTimeFilter';
import { Pagination } from '../../../shared/components';
import { platformApi } from '../api/platformApi';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import type { TaskQueueSummaryApiResponse, TaskItemApiResponse, TaskPriority } from '../types/platformTypes';
import './PlatformTasksPage.css';

const TYPE_LABELS: Record<string, string> = {
  VERIFICATION: 'Xác minh hồ sơ',
  REPORT: 'Báo cáo vi phạm',
  SUPPORT_TICKET: 'Hỗ trợ & Khiếu nại',
  WITHDRAWAL: 'Yêu cầu rút tiền',
  REFUND_REQUEST: 'Yêu cầu hoàn tiền',
  DISPUTE: 'Tranh chấp giao dịch',
};

const PRIORITY_LABELS: Record<TaskPriority, string> = {
  URGENT: 'Khẩn cấp',
  HIGH: 'Cao',
  MEDIUM: 'Trung bình',
  LOW: 'Thấp',
};

export default function PlatformTasksPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [summary, setSummary] = useState<TaskQueueSummaryApiResponse | null>(null);
  const [tasks, setTasks] = useState<TaskItemApiResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const initialType = searchParams.get('type') || 'ALL';
  const initialPriority = searchParams.get('priority') || 'ALL';
  const initialSla = searchParams.get('slaBreached') === 'true' ? true : searchParams.get('slaBreached') === 'false' ? false : undefined;

  const [selectedType, setSelectedType] = useState<string>(initialType);
  const [selectedPriority, setSelectedPriority] = useState<string>(initialPriority);
  const [slaBreachedFilter, setSlaBreachedFilter] = useState<boolean | undefined>(initialSla);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(1);

  useEffect(() => {
    const qType = searchParams.get('type') || 'ALL';
    const qPriority = searchParams.get('priority') || 'ALL';
    const qSla = searchParams.get('slaBreached') === 'true' ? true : searchParams.get('slaBreached') === 'false' ? false : undefined;
    setSelectedType(qType);
    setSelectedPriority(qPriority);
    setSlaBreachedFilter(qSla);
  }, [searchParams]);

  const updateFilters = (newType: string, newPriority: string, newSla?: boolean) => {
    const params: Record<string, string> = {};
    if (newType && newType !== 'ALL') params.type = newType;
    if (newPriority && newPriority !== 'ALL') params.priority = newPriority;
    if (newSla !== undefined) params.slaBreached = String(newSla);
    setSearchParams(params);
    setPage(0);
  };

  const fetchSummary = useCallback(async () => {
    try {
      const res = await platformApi.getTaskSummary();
      setSummary(res.data);
    } catch (err) {
      console.error(err);
    }
  }, []);

  const fetchTasks = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await platformApi.getTasks({
        type: selectedType,
        priority: selectedPriority,
        slaBreached: slaBreachedFilter,
        page,
        size: pageSize,
      });
      setTasks(res.data.content);
      setTotalPages(res.data.totalPages || 1);
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [selectedType, selectedPriority, slaBreachedFilter, page, pageSize]);

  useEffect(() => {
    fetchSummary();
  }, [fetchSummary]);

  useEffect(() => {
    fetchTasks();
  }, [fetchTasks]);

  const handleTypeSelect = (type: string) => {
    updateFilters(type, selectedPriority, slaBreachedFilter);
  };

  const handlePrioritySelect = (priority: string) => {
    updateFilters(selectedType, priority, slaBreachedFilter);
  };

  const handleSlaToggle = () => {
    const next = slaBreachedFilter === true ? undefined : true;
    updateFilters(selectedType, selectedPriority, next);
  };

  const getPriorityBadgeClass = (priority: TaskPriority) => {
    return `task-priority-badge task-priority-badge--${priority.toLowerCase()}`;
  };

  const getTypeBadgeClass = (type: string) => {
    return `task-type-badge task-type-badge--${type.toLowerCase()}`;
  };

  return (
    <AdminLayout title="Hàng đợi công việc" subtitle="Quản lý và xử lý các yêu cầu, báo cáo trên hệ thống.">
      <AdminTimeFilter showGranularity={false} />
      {summary && (
        <div className="adm-task-kpis">
          <article 
            className={`adm-summary-card ${selectedType === 'ALL' ? 'adm-task-kpi--active' : ''}`}
            onClick={() => handleTypeSelect('ALL')}
          >
            <p className="adm-summary-card__label">Tất cả công việc</p>
            <p className="adm-summary-card__value">{summary.totalPendingTasks}</p>
          </article>
          <article 
            className={`adm-summary-card ${selectedType === 'VERIFICATION' ? 'adm-task-kpi--active' : ''}`}
            onClick={() => handleTypeSelect('VERIFICATION')}
          >
            <p className="adm-summary-card__label">Xác minh hồ sơ</p>
            <p className="adm-summary-card__value">{summary.pendingVerifications}</p>
          </article>
          <article 
            className={`adm-summary-card ${selectedType === 'REPORT' ? 'adm-task-kpi--active' : ''}`}
            onClick={() => handleTypeSelect('REPORT')}
          >
            <p className="adm-summary-card__label">Báo cáo vi phạm</p>
            <p className="adm-summary-card__value">{summary.openReports}</p>
          </article>
          <article 
            className={`adm-summary-card ${selectedType === 'SUPPORT_TICKET' ? 'adm-task-kpi--active' : ''}`}
            onClick={() => handleTypeSelect('SUPPORT_TICKET')}
          >
            <p className="adm-summary-card__label">Khiếu nại & Hỗ trợ</p>
            <p className="adm-summary-card__value">{summary.openTickets}</p>
          </article>
          <article 
            className={`adm-summary-card ${selectedType === 'WITHDRAWAL' ? 'adm-task-kpi--active' : ''}`}
            onClick={() => handleTypeSelect('WITHDRAWAL')}
          >
            <p className="adm-summary-card__label">Rút tiền chờ duyệt</p>
            <p className="adm-summary-card__value">{summary.pendingWithdrawals}</p>
          </article>
          <article 
            className={`adm-summary-card ${selectedType === 'REFUND_REQUEST' ? 'adm-task-kpi--active' : ''}`}
            onClick={() => handleTypeSelect('REFUND_REQUEST')}
          >
            <p className="adm-summary-card__label">Hoàn tiền chờ duyệt</p>
            <p className="adm-summary-card__value">{summary.pendingRefunds}</p>
          </article>
          <article 
            className={`adm-summary-card ${selectedType === 'DISPUTE' ? 'adm-task-kpi--active' : ''}`}
            onClick={() => handleTypeSelect('DISPUTE')}
          >
            <p className="adm-summary-card__label">Tranh chấp giao dịch</p>
            <p className="adm-summary-card__value">{summary.openDisputes}</p>
          </article>
        </div>
      )}

      <div className="adm-card">
        <div className="adm-toolbar">
          <select 
            value={selectedType} 
            onChange={(e) => handleTypeSelect(e.target.value)}
            className="tcs-input"
          >
            <option value="ALL">Tất cả loại</option>
            <option value="VERIFICATION">Xác minh hồ sơ</option>
            <option value="REPORT">Báo cáo vi phạm</option>
            <option value="SUPPORT_TICKET">Khiếu nại & Hỗ trợ</option>
            <option value="WITHDRAWAL">Rút tiền chờ duyệt</option>
            <option value="REFUND_REQUEST">Hoàn tiền chờ duyệt</option>
            <option value="DISPUTE">Tranh chấp giao dịch</option>
          </select>

          <select
            value={selectedPriority}
            onChange={(e) => handlePrioritySelect(e.target.value)}
            className="tcs-input"
          >
            <option value="ALL">Tất cả mức ưu tiên</option>
            <option value="URGENT">Khẩn cấp</option>
            <option value="HIGH">Cao</option>
            <option value="MEDIUM">Trung bình</option>
            <option value="LOW">Thấp</option>
          </select>

          <button
            type="button"
            className={`tcs-btn ${slaBreachedFilter === true ? 'tcs-btn--primary' : 'tcs-btn--ghost'}`}
            style={{ borderColor: slaBreachedFilter === true ? '#0f172a' : undefined, background: slaBreachedFilter === true ? '#0f172a' : undefined, color: slaBreachedFilter === true ? '#fff' : undefined }}
            onClick={handleSlaToggle}
          >
            Quá hạn SLA {slaBreachedFilter === true ? '✓' : ''}
          </button>

          <button className="tcs-btn tcs-btn--ghost" type="button" onClick={() => { fetchSummary(); fetchTasks(); }}>
            Làm mới
          </button>
        </div>

        {loading && (
          <div className="adm-state adm-state--loading">
            <span className="adm-spinner" aria-hidden="true" />
            Đang tải danh sách công việc…
          </div>
        )}

        {error && (
          <div className="adm-state">
            <p>{error}</p>
            <button className="tcs-btn tcs-btn--market" type="button" onClick={fetchTasks}>
              Thử lại
            </button>
          </div>
        )}

        {!loading && !error && (
          <div className="adm-table-wrap">
            <table className="adm-table">
              <thead>
                <tr>
                  <th>Case ID</th>
                  <th>Loại</th>
                  <th>Tiêu đề & User</th>
                  <th>Ưu tiên / Risk</th>
                  <th>SLA / Due At</th>
                  <th>Số tiền</th>
                  <th>Trạng thái</th>
                  <th>Hành động</th>
                </tr>
              </thead>
              <tbody>
                {tasks.length === 0 ? (
                  <tr>
                    <td colSpan={8} style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                      Không có công việc nào cần xử lý.
                    </td>
                  </tr>
                ) : (
                  tasks.map((item) => (
                    <tr key={item.taskId} style={{ background: item.slaBreached ? '#fef2f2' : undefined }}>
                      <td><strong>{item.taskId}</strong></td>
                      <td>
                        <span className={getTypeBadgeClass(item.taskType)}>
                          {TYPE_LABELS[item.taskType] || item.taskType}
                        </span>
                      </td>
                      <td>
                        <strong style={{ display: 'block', marginBottom: '0.25rem' }}>{item.title}</strong>
                        {item.relatedEntityType && (
                          <span style={{ fontSize: '0.75rem', background: '#e2e8f0', padding: '0.125rem 0.375rem', borderRadius: '0.25rem', marginRight: '0.25rem' }}>
                            {item.relatedEntityType}: {item.relatedEntityId}
                          </span>
                        )}
                        <span className="adm-table__notes">{item.description}</span>
                      </td>
                      <td>
                        <span className={getPriorityBadgeClass(item.priority)}>
                          {PRIORITY_LABELS[item.priority] || item.priority}
                        </span>
                        {item.riskReason && (
                          <div style={{ fontSize: '0.75rem', color: '#0f172a', fontWeight: 600, marginTop: '0.25rem' }}>
                            [{item.riskReason}]
                          </div>
                        )}
                      </td>
                      <td style={{ color: item.slaBreached ? '#dc2626' : '#475569', fontWeight: item.slaBreached ? 600 : 400 }}>
                        {item.dueAt ? new Date(item.dueAt).toLocaleString('vi-VN') : '—'}
                      </td>
                      <td>
                        {item.amount ? (
                          <span style={{ fontWeight: 500 }}>{new Intl.NumberFormat('vi-VN').format(item.amount)} {item.currency || '₫'}</span>
                        ) : '—'}
                      </td>
                      <td>{item.status}</td>
                      <td className="adm-table__actions">
                        <button
                          className="tcs-btn tcs-btn--sm tcs-btn--primary"
                          type="button"
                          onClick={() => navigate(item.targetRoute + (item.targetQuery || ''))}
                        >
                          Xử lý ngay
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}

        {!loading && tasks.length > 0 && (
          <div className="adm-pagination" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: '16px', gap: '8px' }}>
            <select
              className="adm-field adm-field--fixed"
              style={{ width: 'auto', padding: '4px 8px', fontSize: '13px', borderRadius: '8px' }}
              value={pageSize}
              onChange={(e) => {
                setPageSize(Number(e.target.value));
                setPage(0);
              }}
            >
              <option value={10}>10 / trang</option>
              <option value={20}>20 / trang</option>
              <option value={50}>50 / trang</option>
            </select>
            <Pagination
              current={page + 1}
              totalPages={Math.max(totalPages, 1)}
              onPageChange={(p) => setPage(p - 1)}
            />
          </div>
        )}
      </div>
    </AdminLayout>
  );
}
