import { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { AdminLayout } from '../components/AdminLayout';
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
  const [summary, setSummary] = useState<TaskQueueSummaryApiResponse | null>(null);
  const [tasks, setTasks] = useState<TaskItemApiResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const [selectedType, setSelectedType] = useState<string>('ALL');
  const [page, setPage] = useState(0);

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
      const res = await platformApi.getTasks({ type: selectedType, page, size: 20 });
      setTasks(res.data.content);
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [selectedType, page]);

  useEffect(() => {
    fetchSummary();
  }, [fetchSummary]);

  useEffect(() => {
    fetchTasks();
  }, [fetchTasks]);

  const handleTypeSelect = (type: string) => {
    setSelectedType(type);
    setPage(0);
  };

  const getPriorityBadgeClass = (priority: TaskPriority) => {
    return `task-priority-badge task-priority-badge--${priority.toLowerCase()}`;
  };

  const getTypeBadgeClass = (type: string) => {
    return `task-type-badge task-type-badge--${type.toLowerCase()}`;
  };

  return (
    <AdminLayout title="Hàng đợi công việc" subtitle="Quản lý và xử lý các yêu cầu, báo cáo trên hệ thống.">
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
                  <th>#</th>
                  <th>Mã</th>
                  <th>Loại</th>
                  <th>Tiêu đề & Mô tả</th>
                  <th>Ưu tiên</th>
                  <th>Thời gian tạo</th>
                  <th>Trạng thái</th>
                  <th>Hành động</th>
                </tr>
              </thead>
              <tbody>
                {tasks.length === 0 ? (
                  <tr>
                    <td colSpan={8}>Không có công việc nào cần xử lý.</td>
                  </tr>
                ) : (
                  tasks.map((item, index) => (
                    <tr key={item.taskId}>
                      <td>{index + 1}</td>
                      <td>{item.taskId}</td>
                      <td>
                        <span className={getTypeBadgeClass(item.taskType)}>
                          {TYPE_LABELS[item.taskType] || item.taskType}
                        </span>
                      </td>
                      <td>
                        <strong>{item.title}</strong>
                        <p className="adm-table__notes">{item.description}</p>
                      </td>
                      <td>
                        <span className={getPriorityBadgeClass(item.priority)}>
                          {PRIORITY_LABELS[item.priority] || item.priority}
                        </span>
                      </td>
                      <td>{new Date(item.createdAt).toLocaleString('vi-VN')}</td>
                      <td>{item.status}</td>
                      <td className="adm-table__actions">
                        <button
                          className="tcs-btn tcs-btn--sm tcs-btn--primary"
                          type="button"
                          onClick={() => navigate(item.targetRoute)}
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
      </div>
    </AdminLayout>
  );
}
