import { useState, useEffect, useCallback } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { AdminLayout } from '../components/AdminLayout';
import { usePlatformDashboard } from '../hooks/usePlatformDashboard';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { AdminIcon } from '../components/AdminIcons';
import { AdminTimeFilter, type TimeFilterValue } from '../components/AdminTimeFilter';
import { platformApi } from '../api/platformApi';
import type { AiKnowledgeStatsApiResponse } from '../types/platformTypes';
import './PlatformDashboardPage.css';

const formatCount = (value: any) =>
  new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(typeof value === 'number' ? value : 0);

export default function PlatformDashboardPage() {
  const [searchParams] = useSearchParams();
  const [from, setFrom] = useState(searchParams.get('from') || '');
  const [to, setTo] = useState(searchParams.get('to') || '');
  const [granularity, setGranularity] = useState(searchParams.get('granularity')?.toUpperCase() || 'DAY');
  const [taskFilter, setTaskFilter] = useState<'ALL' | 'SLA' | 'URGENT' | 'MONEY'>('ALL');

  const { status, data, reload } = usePlatformDashboard(from, to, granularity);
  const { user } = useAuth();
  const greetingName = user?.displayName?.trim() || user?.email?.split('@')[0] || 'Admin';

  const [lastUpdated, setLastUpdated] = useState<string>(() => new Date().toLocaleTimeString('vi-VN'));
  const [aiStats, setAiStats] = useState<AiKnowledgeStatsApiResponse | null>(null);
  const [reindexing, setReindexing] = useState(false);
  const [reindexMessage, setReindexMessage] = useState<string | null>(null);

  const fetchAiStats = useCallback(async () => {
    try {
      const res = await platformApi.getAiKnowledgeStats();
      setAiStats(res.data);
    } catch (e) {
      console.error(e);
    }
  }, []);

  useEffect(() => {
    fetchAiStats();
  }, [fetchAiStats]);

  const handleReindex = async () => {
    setReindexing(true);
    setReindexMessage(null);
    try {
      const res = await platformApi.reindexAiKnowledge();
      const s = res.data;
      setReindexMessage(`Đã đánh chỉ mục thành công: ${s.indexed} mới, ${s.updated} cập nhật, ${s.unchanged} không đổi.`);
      fetchAiStats();
    } catch (err: any) {
      setReindexMessage(err?.response?.data?.message || 'Không thể reindex AI. Vui lòng kiểm tra kết nối backend.');
    } finally {
      setReindexing(false);
    }
  };

  const handleTimeFilterChange = (val: TimeFilterValue) => {
    setFrom(val.from);
    setTo(val.to);
    setGranularity(val.granularity.toUpperCase());
    setLastUpdated(new Date().toLocaleTimeString('vi-VN'));
  };

  const handleReload = () => {
    setLastUpdated(new Date().toLocaleTimeString('vi-VN'));
    reload();
  };

  const filteredTasks = (data?.queuePreview || []).filter((task) => {
    if (taskFilter === 'SLA') return task.slaBreached;
    if (taskFilter === 'URGENT') return task.priority === 'URGENT' || task.priority === 'HIGH';
    if (taskFilter === 'MONEY') return (task.amount && task.amount > 0) || task.taskType === 'DISPUTE' || task.taskType === 'REFUND_REQUEST';
    return true;
  });

  const tutorActiveRate = data?.tutorHealth?.total ? Math.min(100, Math.round(((data.tutorHealth.active || 0) / data.tutorHealth.total) * 100)) : 0;
  const tutorVerifiedRate = data?.tutorHealth?.total ? Math.min(100, Math.round(((data.tutorHealth.verified || 0) / data.tutorHealth.total) * 100)) : 0;

  const centerActiveRate = data?.centerHealth?.total ? Math.min(100, Math.round(((data.centerHealth.active || 0) / data.centerHealth.total) * 100)) : 0;
  const centerVerifiedRate = data?.centerHealth?.total ? Math.min(100, Math.round(((data.centerHealth.verified || 0) / data.centerHealth.total) * 100)) : 0;

  return (
    <AdminLayout
      title="Trung tâm Vận hành TCS"
      subtitle="Bảng điều khiển tác vụ ưu tiên, giám sát rủi ro tài chính Escrow và sức khỏe toàn hệ thống"
    >
      {/* Controls & Filter Bar (Monochrome) */}
      <AdminTimeFilter
        onChange={handleTimeFilterChange}
        extraControls={
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.75rem' }}>
            <button type="button" className="adm-btn-mono" onClick={handleReload}>
              <AdminIcon name="check-square" size="sm" /> Làm mới
            </button>
            <span className="adm-last-updated" style={{ fontSize: '0.75rem', color: '#64748b' }}>
              Cập nhật: <strong>{lastUpdated}</strong>
            </span>
          </div>
        }
      />

      {status === 'loading' && (
        <div className="adm-state adm-state--loading">
          <span className="adm-spinner" aria-hidden="true" />
          Đang tổng hợp dữ liệu điều hành hệ thống...
        </div>
      )}

      {status === 'error' && (
        <div className="adm-card adm-error-card">
          <p className="adm-error-card__title">Không tải được dữ liệu bảng điều khiển</p>
          <p className="adm-muted">Vui lòng kiểm tra kết nối máy chủ backend và thử lại.</p>
          <button type="button" className="adm-btn-mono" onClick={reload}>
            Thử lại
          </button>
        </div>
      )}

      {status === 'success' && data && (
        <>
          {/* Welcome & Command Status Banner (Monochrome) */}
          <section className="adm-welcome-card">
            <div className="adm-welcome-card__main">
              <p className="adm-welcome-card__eyebrow">TCS Operations Command Center</p>
              <h2 className="adm-welcome-card__title">Xin chào, {greetingName}</h2>
              <p className="adm-welcome-card__desc">
                Hiện có <strong>{formatCount(data.pendingVerifications)}</strong> hồ sơ xác minh cần duyệt,{' '}
                <strong>{formatCount(data.riskSummary?.highRiskTasks || 0)}</strong> tác vụ quá hạn SLA và{' '}
                <strong>{formatCount(data.riskSummary?.activeDisputes || 0)}</strong> vụ tranh chấp đang mở.
              </p>
            </div>
            <div className="adm-welcome-card__actions">
              <Link className="adm-btn-solid" to={APP_ROUTES.platformTasks}>
                Mở hàng chờ công việc →
              </Link>
              <Link className="adm-btn-outline" to={APP_ROUTES.platformReports}>
                Xử lý Khiếu nại & Tranh chấp
              </Link>
            </div>
          </section>

          {/* Operations KPI Grid (Monochrome) */}
          <section className="adm-dashboard-section" style={{ marginTop: '1.5rem' }}>
            <h2 className="adm-dashboard-section__title">Chỉ số Rủi ro & Tác vụ Vận hành (Operations KPI)</h2>
            <div className="adm-kpi-mono-grid">
              <Link className="adm-kpi-mono-card" to={APP_ROUTES.platformTasks + "?priority=URGENT&slaBreached=true"}>
                <div className="adm-kpi-mono-card__head">
                  <AdminIcon name="flag" size="sm" />
                  <span>Quá hạn SLA / Khẩn</span>
                </div>
                <p className="adm-kpi-mono-card__value">{formatCount(data.riskSummary?.highRiskTasks)}</p>
                <p className="adm-kpi-mono-card__sub">Cần xử lý ưu tiên</p>
              </Link>

              <Link className="adm-kpi-mono-card" to={APP_ROUTES.platformReports + "?tab=disputes"}>
                <div className="adm-kpi-mono-card__head">
                  <AdminIcon name="shield" size="sm" />
                  <span>Tranh chấp Đang mở</span>
                </div>
                <p className="adm-kpi-mono-card__value">{formatCount(data.riskSummary?.activeDisputes)}</p>
                <p className="adm-kpi-mono-card__sub">Đang chờ hòa giải</p>
              </Link>

              <Link className="adm-kpi-mono-card" to={APP_ROUTES.platformReports + "?tab=reports"}>
                <div className="adm-kpi-mono-card__head">
                  <AdminIcon name="message" size="sm" />
                  <span>Báo cáo Vi phạm / Lách sàn</span>
                </div>
                <p className="adm-kpi-mono-card__value">{formatCount(data.riskSummary?.unresolvedReports)}</p>
                <p className="adm-kpi-mono-card__sub">Chờ Admin xác minh</p>
              </Link>

              <Link className="adm-kpi-mono-card" to={APP_ROUTES.platformTasks + "?type=DISPUTE"}>
                <div className="adm-kpi-mono-card__head">
                  <AdminIcon name="wallet" size="sm" />
                  <span>Tiền Escrow Rủi ro</span>
                </div>
                <p className="adm-kpi-mono-card__value">
                  {new Intl.NumberFormat('vi-VN').format(data.riskSummary?.moneyAtRisk || 0)} ₫
                </p>
                <p className="adm-kpi-mono-card__sub">Thuộc các lớp tranh chấp</p>
              </Link>
            </div>
          </section>

          {/* Workbench: Priority Inbox Queue Preview */}
          <section className="adm-dashboard-section" style={{ marginTop: '2rem' }}>
            <div className="adm-dashboard-section__head">
              <div>
                <h2 className="adm-dashboard-section__title">Priority Inbox (Hàng đợi Tác vụ Điều hành)</h2>
                <p className="adm-dashboard-section__desc">
                  Tự động sắp xếp theo mức độ ưu tiên, rủi ro tài chính và thời hạn SLA cam kết.
                </p>
              </div>
              <div className="adm-inbox-filters">
                <button
                  type="button"
                  className={`adm-filter-btn ${taskFilter === 'ALL' ? 'adm-filter-btn--active' : ''}`}
                  onClick={() => setTaskFilter('ALL')}
                >
                  Tất cả ({data.queuePreview?.length || 0})
                </button>
                <button
                  type="button"
                  className={`adm-filter-btn ${taskFilter === 'SLA' ? 'adm-filter-btn--active' : ''}`}
                  onClick={() => setTaskFilter('SLA')}
                >
                  Quá hạn SLA
                </button>
                <button
                  type="button"
                  className={`adm-filter-btn ${taskFilter === 'URGENT' ? 'adm-filter-btn--active' : ''}`}
                  onClick={() => setTaskFilter('URGENT')}
                >
                  Khẩn cấp
                </button>
                <button
                  type="button"
                  className={`adm-filter-btn ${taskFilter === 'MONEY' ? 'adm-filter-btn--active' : ''}`}
                  onClick={() => setTaskFilter('MONEY')}
                >
                  Có tiền rủi ro
                </button>
                <Link to={APP_ROUTES.platformTasks} className="adm-btn-mono adm-btn-mono--sm">
                  Toàn bộ Inbox →
                </Link>
              </div>
            </div>

            <div className="adm-table-container">
              <table className="adm-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Loại tác vụ</th>
                    <th>Tiêu đề & Nội dung</th>
                    <th>Mức độ Ưu tiên</th>
                    <th>Hạn xử lý SLA</th>
                    <th>Số tiền</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredTasks.map((task) => (
                    <tr key={task.taskId} className={task.slaBreached ? 'adm-row--alert' : ''}>
                      <td><strong>#{task.taskId}</strong></td>
                      <td>
                        <span className="adm-badge-mono">
                          {task.taskType}
                        </span>
                      </td>
                      <td>
                        <div style={{ fontWeight: 600, color: '#0f172a' }}>{task.title}</div>
                        {task.assigneeName && <div className="adm-subtext">Phụ trách: {task.assigneeName}</div>}
                      </td>
                      <td>
                        <span className={`adm-priority-tag adm-priority-tag--${task.priority.toLowerCase()}`}>
                          {task.priority}
                        </span>
                        {task.riskReason && <div className="adm-alert-subtext">[{task.riskReason}]</div>}
                      </td>
                      <td>
                        <div style={{ fontWeight: task.slaBreached ? 600 : 400 }}>
                          {task.dueAt ? new Date(task.dueAt).toLocaleString('vi-VN') : 'Không giới hạn'}
                        </div>
                        {task.slaBreached && <span className="adm-sla-tag">QUÁ HẠN</span>}
                      </td>
                      <td>
                        {task.amount ? (
                          <strong>{new Intl.NumberFormat('vi-VN').format(task.amount)} {task.currency || '₫'}</strong>
                        ) : '—'}
                      </td>
                      <td>
                        <Link className="adm-btn-mono adm-btn-mono--sm" to={task.targetRoute + (task.targetQuery || '')}>
                          Xử lý ngay
                        </Link>
                      </td>
                    </tr>
                  ))}
                  {filteredTasks.length === 0 && (
                    <tr>
                      <td colSpan={7} className="adm-table-empty">
                        Không có tác vụ nào thuộc bộ lọc này
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>

          {/* Money Flow - 4 Cards (Monochrome) */}
          <section className="adm-dashboard-section" style={{ marginTop: '2rem' }}>
            <h2 className="adm-dashboard-section__title">Dòng tiền & Escrow Ký quỹ (Money Flow)</h2>
            <div className="adm-money-flow-grid">
              <article className="adm-money-card adm-money-card--in">
                <div className="adm-money-card__content">
                  <p className="adm-money-card__label">Tổng Tiền Vào (IN)</p>
                  <p className="adm-money-card__value">
                    +{new Intl.NumberFormat('vi-VN').format((data.financialFlow?.deposits || 0) + (data.financialFlow?.escrowDeposits || 0))} ₫
                  </p>
                  <div className="adm-money-card__breakdown">
                    <span>Nạp ví: {new Intl.NumberFormat('vi-VN').format(data.financialFlow?.deposits || 0)} ₫</span>
                    <span>Ký quỹ lớp: {new Intl.NumberFormat('vi-VN').format(data.financialFlow?.escrowDeposits || 0)} ₫</span>
                  </div>
                </div>
              </article>

              <article className="adm-money-card adm-money-card--out">
                <div className="adm-money-card__content">
                  <p className="adm-money-card__label">Tổng Tiền Ra (OUT)</p>
                  <p className="adm-money-card__value">
                    -{new Intl.NumberFormat('vi-VN').format((data.financialFlow?.withdrawals || 0) + (data.financialFlow?.refunds || 0))} ₫
                  </p>
                  <div className="adm-money-card__breakdown">
                    <span>Rút ngân hàng: {new Intl.NumberFormat('vi-VN').format(data.financialFlow?.withdrawals || 0)} ₫</span>
                    <span>Hoàn học viên: {new Intl.NumberFormat('vi-VN').format(data.financialFlow?.refunds || 0)} ₫</span>
                  </div>
                </div>
              </article>

              <article className="adm-money-card adm-money-card--escrow">
                <div className="adm-money-card__content">
                  <p className="adm-money-card__label">Escrow Đang Tạm Giữ</p>
                  <p className="adm-money-card__value">
                    {new Intl.NumberFormat('vi-VN').format(data.financialFlow?.escrowHeld || 0)} ₫
                  </p>
                  <p className="adm-money-card__subtitle">{data.financialFlow?.openEscrowCount || 0} hợp đồng đang được bảo vệ</p>
                </div>
              </article>

              <article className="adm-money-card adm-money-card--fee">
                <div className="adm-money-card__content">
                  <p className="adm-money-card__label">Doanh thu Phí Nền tảng</p>
                  <p className="adm-money-card__value">
                    {new Intl.NumberFormat('vi-VN').format(data.financialFlow?.platformFeeRevenue || 0)} ₫
                  </p>
                  <p className="adm-money-card__subtitle">{data.financialFlow?.feeRate || 10}% từ {data.financialFlow?.settledCount || 0} giao dịch tất toán</p>
                </div>
              </article>
            </div>
          </section>

          {/* Ecosystem Health: Tutors, Centers & Classes with Progress Bars & Drill-down */}
          <div className="adm-ecosystem-grid" style={{ marginTop: '2rem' }}>
            {/* Tutor Health */}
            <section className="adm-dashboard-section">
              <div className="adm-dashboard-section__head">
                <h2 className="adm-dashboard-section__title">Sức khỏe Gia sư</h2>
                <Link to="/platform/users?role=TUTOR" className="adm-sublink">Xem danh sách →</Link>
              </div>
              <div className="adm-health-card">
                <div className="adm-health-metrics-row">
                  <div>
                    <span className="adm-health-label">Tổng gia sư</span>
                    <p className="adm-health-value">{formatCount(data.tutorHealth?.total)}</p>
                  </div>
                  <div>
                    <span className="adm-health-label">Hoạt động</span>
                    <p className="adm-health-value">{formatCount(data.tutorHealth?.active)}</p>
                  </div>
                  <div>
                    <span className="adm-health-label">Đã xác minh</span>
                    <p className="adm-health-value">{formatCount(data.tutorHealth?.verified)}</p>
                  </div>
                  <div>
                    <span className="adm-health-label">Mới trong kỳ</span>
                    <p className="adm-health-value">+{formatCount(data.tutorHealth?.newTutors)}</p>
                  </div>
                </div>

                <div className="adm-progress-block">
                  <div className="adm-progress-info">
                    <span>Tỷ lệ hoạt động: {tutorActiveRate}%</span>
                    <span>Tỷ lệ xác minh: {tutorVerifiedRate}%</span>
                  </div>
                  <div className="adm-progress-track">
                    <div className="adm-progress-fill" style={{ width: `${tutorActiveRate}%` }} />
                  </div>
                </div>
              </div>
            </section>
            
            {/* Center Health */}
            <section className="adm-dashboard-section">
              <div className="adm-dashboard-section__head">
                <h2 className="adm-dashboard-section__title">Sức khỏe Trung tâm</h2>
                <Link to="/platform/users?role=TUTOR_CENTER" className="adm-sublink">Xem danh sách →</Link>
              </div>
              <div className="adm-health-card">
                <div className="adm-health-metrics-row">
                  <div>
                    <span className="adm-health-label">Tổng trung tâm</span>
                    <p className="adm-health-value">{formatCount(data.centerHealth?.total)}</p>
                  </div>
                  <div>
                    <span className="adm-health-label">Hoạt động</span>
                    <p className="adm-health-value">{formatCount(data.centerHealth?.active)}</p>
                  </div>
                  <div>
                    <span className="adm-health-label">Đã xác minh</span>
                    <p className="adm-health-value">{formatCount(data.centerHealth?.verified)}</p>
                  </div>
                  <div>
                    <span className="adm-health-label">Mới trong kỳ</span>
                    <p className="adm-health-value">+{formatCount(data.centerHealth?.newCenters)}</p>
                  </div>
                </div>

                <div className="adm-progress-block">
                  <div className="adm-progress-info">
                    <span>Tỷ lệ hoạt động: {centerActiveRate}%</span>
                    <span>Tỷ lệ xác minh: {centerVerifiedRate}%</span>
                  </div>
                  <div className="adm-progress-track">
                    <div className="adm-progress-fill" style={{ width: `${centerActiveRate}%` }} />
                  </div>
                </div>
              </div>
            </section>
            
            {/* Class Health */}
            <section className="adm-dashboard-section">
              <div className="adm-dashboard-section__head">
                <h2 className="adm-dashboard-section__title">Sức khỏe Lớp Học</h2>
                <Link to="/platform/classes" className="adm-sublink">Xem danh sách →</Link>
              </div>
              <div className="adm-health-card">
                <div className="adm-health-metrics-row">
                  <div>
                    <span className="adm-health-label">Tổng lớp học</span>
                    <p className="adm-health-value">{formatCount(data.classHealth?.total)}</p>
                  </div>
                  <div>
                    <span className="adm-health-label">Đang diễn ra</span>
                    <p className="adm-health-value">{formatCount(data.classHealth?.active)}</p>
                  </div>
                  <div>
                    <span className="adm-health-label">Đã hoàn thành</span>
                    <p className="adm-health-value">{formatCount(data.classHealth?.verified)}</p>
                  </div>
                  <div>
                    <span className="adm-health-label">Đã hủy</span>
                    <p className="adm-health-value">{formatCount(data.classHealth?.newCount)}</p>
                  </div>
                </div>

                <div className="adm-progress-block">
                  <div className="adm-progress-info">
                    <span>Hoàn thành: {data.classHealth?.total ? Math.round(((data.classHealth.verified || 0) / data.classHealth.total) * 100) : 0}%</span>
                    <span>Hủy: {data.classHealth?.total ? Math.round(((data.classHealth.newCount || 0) / data.classHealth.total) * 100) : 0}%</span>
                  </div>
                  <div className="adm-progress-track">
                    <div className="adm-progress-fill" style={{ width: `${data.classHealth?.total ? Math.round(((data.classHealth.verified || 0) / data.classHealth.total) * 100) : 0}%` }} />
                  </div>
                </div>
              </div>
            </section>
          </div>

          {/* Activity Timeline Bar Chart (Monochrome SVG/CSS) */}
          {data.activityTimeline && data.activityTimeline.length > 0 && (() => {
            const maxMoney = Math.max(...data.activityTimeline.map((i: any) => Math.max(i.moneyIn || 0, i.moneyOut || 0)), 1);
            return (
              <section className="adm-dashboard-section" style={{ marginTop: '2rem' }}>
                <div className="adm-dashboard-section__head">
                  <div>
                    <h2 className="adm-dashboard-section__title">Biểu đồ Dòng tiền Hoạt động (Activity Timeline)</h2>
                    <p className="adm-dashboard-section__desc">Phân bố dòng tiền theo đơn vị {granularity === 'DAY' ? 'Ngày' : granularity === 'WEEK' ? 'Tuần' : 'Tháng'}</p>
                  </div>
                </div>
                <div className="adm-timeline-bars">
                  {data.activityTimeline.map((item: any, idx: number) => (
                    <div className="adm-timeline-row" key={idx}>
                      <span className="adm-timeline-label">{item.label}</span>
                      <div className="adm-timeline-bar-group">
                        <div className="adm-timeline-bar-wrapper">
                          <div className="adm-timeline-bar adm-timeline-bar--in" style={{ width: `${((item.moneyIn || 0) / maxMoney) * 100}%` }} />
                        </div>
                        <div className="adm-timeline-bar-wrapper">
                          <div className="adm-timeline-bar adm-timeline-bar--out" style={{ width: `${((item.moneyOut || 0) / maxMoney) * 100}%` }} />
                        </div>
                      </div>
                      <div className="adm-timeline-values">
                        <span className="adm-timeline-val--in">+{(item.moneyIn || 0).toLocaleString('vi-VN')} ₫</span>
                        <span className="adm-timeline-val--out">-{(item.moneyOut || 0).toLocaleString('vi-VN')} ₫</span>
                        <span className="adm-timeline-val--net">
                          Net: {(item.netMovement || 0).toLocaleString('vi-VN')} ₫
                        </span>
                      </div>
                    </div>
                  ))}
                  <div className="adm-timeline-legend">
                    <span><span className="adm-timeline-dot adm-timeline-dot--in" /> Tiền vào (IN - Nạp ví & Escrow)</span>
                    <span><span className="adm-timeline-dot adm-timeline-dot--out" /> Tiền ra (OUT - Rút tiền & Hoàn tiền)</span>
                  </div>
                </div>
              </section>
            );
          })()}

          {/* AI Knowledge Base Diagnostics & Reindex Control */}
          <section className="adm-dashboard-section" style={{ marginTop: '2rem', marginBottom: '2rem' }}>
            <div className="adm-dashboard-section__head">
              <div>
                <h2 className="adm-dashboard-section__title">AI Knowledge Base & RAG Index Diagnostics</h2>
                <p className="adm-dashboard-section__desc">
                  Trạng thái nguồn tri thức RAG và Tìm kiếm thông minh cho Trợ lý AI hệ thống TCS.
                </p>
              </div>
              <button
                type="button"
                className="adm-btn-solid"
                disabled={reindexing}
                onClick={handleReindex}
              >
                {reindexing ? 'Đang reindex...' : 'Đánh chỉ mục lại (Reindex All)'}
              </button>
            </div>

            {(!aiStats || aiStats.totalChunks === 0) && (
              <div className="adm-alert-box">
                [Cảnh báo] Cơ sở dữ liệu tri thức AI hiện đang trống (0 chunks). Hãy bấm <strong>"Đánh chỉ mục lại (Reindex All)"</strong> ở trên để nạp tri thức FAQ, Gia sư, Lớp học và Chính sách vào bộ nhớ RAG của AI.
              </div>
            )}

            {reindexMessage && (
              <div className="adm-success-box">
                {reindexMessage}
              </div>
            )}

            <div className="adm-kpi-mono-grid" style={{ gridTemplateColumns: 'repeat(5, 1fr)' }}>
              <div className="adm-kpi-mono-card">
                <span className="adm-kpi-mono-card__head">Tổng số Chunks</span>
                <p className="adm-kpi-mono-card__value">{formatCount(aiStats?.totalChunks || 0)}</p>
              </div>
              <div className="adm-kpi-mono-card">
                <span className="adm-kpi-mono-card__head">FAQ & Hướng dẫn</span>
                <p className="adm-kpi-mono-card__value">{formatCount(aiStats?.bySourceType?.FAQ || 0)}</p>
              </div>
              <div className="adm-kpi-mono-card">
                <span className="adm-kpi-mono-card__head">Gia sư (Active)</span>
                <p className="adm-kpi-mono-card__value">{formatCount(aiStats?.bySourceType?.TUTOR || 0)}</p>
              </div>
              <div className="adm-kpi-mono-card">
                <span className="adm-kpi-mono-card__head">Lớp học (Open)</span>
                <p className="adm-kpi-mono-card__value">{formatCount(aiStats?.bySourceType?.CLASS || 0)}</p>
              </div>
              <div className="adm-kpi-mono-card">
                <span className="adm-kpi-mono-card__head">Chính sách & Docs</span>
                <p className="adm-kpi-mono-card__value">
                  {formatCount((aiStats?.bySourceType?.POLICY || 0) + (aiStats?.bySourceType?.SYSTEM_DOC || 0))}
                </p>
              </div>
            </div>

            {aiStats?.lastIndexedAt && (
              <p className="adm-subtext" style={{ marginTop: '0.75rem' }}>
                Thời điểm đánh chỉ mục gần nhất: <strong>{new Date(aiStats.lastIndexedAt).toLocaleString('vi-VN')}</strong>
              </p>
            )}
          </section>
        </>
      )}
    </AdminLayout>
  );
}