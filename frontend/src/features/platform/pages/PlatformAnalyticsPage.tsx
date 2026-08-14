import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { AdminLayout } from '../components/AdminLayout';
import { AdminTimeFilter, type TimeFilterValue } from '../components/AdminTimeFilter';
import { platformApi } from '../api/platformApi';
import type { AnalyticsSummaryApiResponse } from '../types/platformTypes';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import './PlatformAnalyticsPage.css';

export default function PlatformAnalyticsPage() {
  const [searchParams] = useSearchParams();
  const [data, setData] = useState<AnalyticsSummaryApiResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [exportingType, setExportingType] = useState<string | null>(null);
  const [appliedFrom, setAppliedFrom] = useState(searchParams.get('from') || '');
  const [appliedTo, setAppliedTo] = useState(searchParams.get('to') || '');

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await platformApi.getAnalyticsSummary(appliedFrom, appliedTo);
        setData(response.data);
      } catch (err) {
        setError(getApiErrorMessage(err));
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [appliedFrom, appliedTo]);

  const handleTimeFilterChange = (val: TimeFilterValue) => {
    setAppliedFrom(val.from);
    setAppliedTo(val.to);
  };

  const handleExport = async (type: 'users' | 'classes' | 'revenue' | 'cashflow' | 'transaction-breakdown') => {
    setExportingType(type);
    try {
      const response = await platformApi.exportAnalyticsCsv(type, appliedFrom, appliedTo);
      const csvBlob = response.data instanceof Blob
        ? response.data
        : new Blob([response.data], { type: 'text/csv;charset=utf-8' });
      const url = window.URL.createObjectURL(csvBlob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `tcs-analytics-${type}-${new Date().toISOString().slice(0, 10)}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      alert('Không thể tải file xuất CSV: ' + getApiErrorMessage(err));
    } finally {
      setExportingType(null);
    }
  };

  const renderContent = () => {
    if (loading) {
      return (
        <div className="adm-analytics-loading">
          <div>Đang tải dữ liệu báo cáo phân tích...</div>
        </div>
      );
    }

    if (error) {
      return <div style={{ color: 'red', padding: '1rem' }}>{error}</div>;
    }

    if (!data) return null;

    const maxMonthlyRevenue = Math.max(
      ...(data.monthlyMetrics?.map(m => m.revenue) || [1]),
      1
    );

    return (
      <div>
        {/* KPI Row 1: Users & Ecosystem */}
        <h2 className="adm-kpi-section-title">Cơ cấu Người dùng & Hệ sinh thái</h2>
        <div className="adm-analytics-grid-4">
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Tổng người dùng</span>
            <span className="adm-analytics-card-value">{data.totalUsers?.toLocaleString('vi-VN')}</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Gia sư</span>
            <span className="adm-analytics-card-value">{data.totalTutors?.toLocaleString('vi-VN')}</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Phụ huynh / Học viên</span>
            <span className="adm-analytics-card-value">{data.totalParents?.toLocaleString('vi-VN')}</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Trung tâm gia sư</span>
            <span className="adm-analytics-card-value">{data.totalCenters?.toLocaleString('vi-VN')}</span>
          </div>
        </div>

        {/* KPI Row 2: Performance & Conversion */}
        <h2 className="adm-kpi-section-title">Hoạt động & Hiệu suất Vận hành</h2>
        <div className="adm-analytics-grid-4">
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Tổng lớp học</span>
            <span className="adm-analytics-card-value">{data.totalClasses?.toLocaleString('vi-VN')}</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Lớp đang diễn ra</span>
            <span className="adm-analytics-card-value">{data.activeClasses?.toLocaleString('vi-VN')}</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Tỷ lệ hoàn thành hợp đồng</span>
            <span className="adm-analytics-card-value">{data.contractCompletionRate?.toFixed(1)}%</span>
            <div className="adm-progress-bar-container">
              <div
                className="adm-progress-bar adm-progress-bar--green"
                style={{ width: `${Math.min(100, data.contractCompletionRate ?? 0)}%` }}
              />
            </div>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Tỷ lệ duyệt hồ sơ gia sư</span>
            <span className="adm-analytics-card-value">{data.verificationConversionRate?.toFixed(1)}%</span>
            <div className="adm-progress-bar-container">
              <div
                className="adm-progress-bar adm-progress-bar--green"
                style={{ width: `${Math.min(100, data.verificationConversionRate ?? 0)}%` }}
              />
            </div>
          </div>
        </div>

        {/* Financial Flow Section with Visual Summary */}
        <h2 className="adm-kpi-section-title">Dòng tiền & Doanh thu Nền tảng</h2>
        <div className="adm-analytics-grid-4" style={{ marginBottom: '1.5rem' }}>
          <div className="adm-analytics-card adm-analytics-card--in">
            <span className="adm-analytics-card-title">Tiền Vào (IN)</span>
            <span className="adm-analytics-card-value">
              +{data.moneyIn?.toLocaleString('vi-VN') || 0} ₫
            </span>
          </div>
          <div className="adm-analytics-card adm-analytics-card--out">
            <span className="adm-analytics-card-title">Tiền Ra (OUT)</span>
            <span className="adm-analytics-card-value">
              -{data.moneyOut?.toLocaleString('vi-VN') || 0} ₫
            </span>
          </div>
          <div className={`adm-analytics-card ${(data.netMovement || 0) >= 0 ? 'adm-analytics-card--net-positive' : 'adm-analytics-card--net-negative'}`}>
            <span className="adm-analytics-card-title">Dòng tiền ròng (Net)</span>
            <span className="adm-analytics-card-value">
              {data.netMovement?.toLocaleString('vi-VN') || 0} ₫
            </span>
          </div>
          <div className="adm-analytics-card adm-analytics-card--fee">
            <span className="adm-analytics-card-title">Phí nền tảng (Revenue)</span>
            <span className="adm-analytics-card-value">
              {data.platformFeeRevenue?.toLocaleString('vi-VN') || 0} ₫
            </span>
          </div>
        </div>

        {/* Transaction Breakdown Table */}
        <div className="adm-analytics-table-container" style={{ marginBottom: '2rem' }}>
          <div style={{ padding: '1rem 1.25rem', borderBottom: '1px solid #e2e8f0', fontWeight: 600, color: '#1e293b' }}>
            Chi tiết loại giao dịch phát sinh
          </div>
          <table className="adm-analytics-table">
            <thead>
              <tr>
                <th>Loại giao dịch</th>
                <th>Phân loại</th>
                <th style={{ textAlign: 'right' }}>Số lượng</th>
                <th style={{ textAlign: 'right' }}>Tổng tiền (VND)</th>
              </tr>
            </thead>
            <tbody>
              {data.transactionTypeBreakdown?.map((tx, idx) => (
                <tr key={idx}>
                  <td><strong>{tx.label}</strong></td>
                  <td>
                    <span className={`ai-badge ${tx.direction === 'IN' ? 'ai-badge--high' : 'ai-badge--low'}`}>
                      {tx.direction === 'IN' ? 'Tiền vào' : 'Tiền ra'}
                    </span>
                  </td>
                  <td style={{ textAlign: 'right' }}>{tx.count.toLocaleString('vi-VN')}</td>
                  <td style={{ textAlign: 'right', fontWeight: 600, color: tx.direction === 'IN' ? 'var(--color-success)' : 'var(--color-error)' }}>
                    {tx.direction === 'IN' ? '+' : '-'}{tx.totalAmount.toLocaleString('vi-VN')} ₫
                  </td>
                </tr>
              ))}
              {(!data.transactionTypeBreakdown || data.transactionTypeBreakdown.length === 0) && (
                <tr>
                  <td colSpan={4} style={{ textAlign: 'center', color: '#64748b' }}>
                    Chưa có giao dịch phát sinh trong khoảng thời gian này
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Monthly Trend Chart & Table */}
        <h2 className="adm-kpi-section-title">Biểu đồ Tăng trưởng (6 Tháng gần nhất)</h2>
        <div style={{ background: '#fff', borderRadius: '0.5rem', padding: '1.5rem', boxShadow: '0 1px 3px rgba(0,0,0,0.08)', marginBottom: '2rem' }}>
          {data.monthlyMetrics && data.monthlyMetrics.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '1rem', alignItems: 'flex-end', height: '180px', paddingTop: '1rem', borderBottom: '1px solid #e2e8f0' }}>
                {data.monthlyMetrics.map((m, idx) => {
                  const barHeight = Math.max(12, Math.round((m.revenue / maxMonthlyRevenue) * 120));
                  return (
                    <div key={idx} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.5rem', height: '100%', justifyContent: 'flex-end' }}>
                      <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--color-primary-dark)' }}>
                        {m.revenue >= 1000000 ? `${(m.revenue / 1000000).toFixed(1)}M` : `${(m.revenue / 1000).toFixed(0)}k`}
                      </span>
                      <div
                        style={{
                          width: '36px',
                          height: `${barHeight}px`,
                          background: 'linear-gradient(180deg, var(--color-primary) 0%, var(--color-primary-dark) 100%)',
                          borderRadius: '6px 6px 0 0',
                          transition: 'height 0.4s ease'
                        }}
                      />
                      <span style={{ fontSize: '0.8rem', fontWeight: 500, color: '#475569' }}>{m.month}</span>
                    </div>
                  );
                })}
              </div>

              <table className="adm-analytics-table" style={{ marginTop: '1rem' }}>
                <thead>
                  <tr>
                    <th>Tháng</th>
                    <th style={{ textAlign: 'right' }}>Người dùng mới</th>
                    <th style={{ textAlign: 'right' }}>Lớp học mới</th>
                    <th style={{ textAlign: 'right' }}>Doanh thu (VND)</th>
                  </tr>
                </thead>
                <tbody>
                  {data.monthlyMetrics.map((metric, idx) => (
                    <tr key={idx}>
                      <td><strong>{metric.month}</strong></td>
                      <td style={{ textAlign: 'right' }}>+{metric.newUsers.toLocaleString('vi-VN')}</td>
                      <td style={{ textAlign: 'right' }}>+{metric.newClasses.toLocaleString('vi-VN')}</td>
                      <td style={{ textAlign: 'right', fontWeight: 600, color: 'var(--color-primary-dark)' }}>
                        {metric.revenue.toLocaleString('vi-VN')} ₫
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p style={{ textAlign: 'center', color: '#64748b', padding: '2rem 0' }}>Không có dữ liệu tăng trưởng</p>
          )}
        </div>
      </div>
    );
  };

  return (
    <AdminLayout
      title="Báo cáo & Phân tích Vận hành"
      subtitle="Bảng phân tích toàn diện người dùng, lớp học, doanh thu và dòng tiền nền tảng TCS"
    >
      <AdminTimeFilter
        onChange={handleTimeFilterChange}
        extraControls={
          <div style={{ fontSize: '0.75rem', color: '#64748b' }}>
            {appliedFrom || appliedTo ? (
              <span>Bộ lọc: <strong>{appliedFrom || 'Đầu'}</strong> → <strong>{appliedTo || 'Hiện tại'}</strong></span>
            ) : (
              <span>Hiển thị: <strong>30 ngày gần nhất</strong></span>
            )}
          </div>
        }
      />

      <section className="adm-analytics-controls" aria-label="Xuất báo cáo CSV" style={{ marginBottom: '1.5rem' }}>
        <div className="adm-analytics-export-actions">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
            <span style={{ fontWeight: 600 }}>Xuất file báo cáo CSV (Tối đa 10.000 dòng):</span>
            <span style={{ fontSize: '0.75rem', color: '#64748b' }}>
              Dữ liệu được trích xuất chính xác theo khoảng thời gian đã lọc ở trên ({appliedFrom || 'Toàn bộ'} → {appliedTo || 'Hiện tại'}).
            </span>
          </div>
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            <button disabled={Boolean(exportingType)} onClick={() => handleExport('users')}>
              {exportingType === 'users' ? 'Đang tải...' : 'CSV Người dùng'}
            </button>
            <button disabled={Boolean(exportingType)} onClick={() => handleExport('classes')}>
              {exportingType === 'classes' ? 'Đang tải...' : 'CSV Lớp học'}
            </button>
            <button disabled={Boolean(exportingType)} onClick={() => handleExport('revenue')}>
              {exportingType === 'revenue' ? 'Đang tải...' : 'CSV Doanh thu'}
            </button>
            <button disabled={Boolean(exportingType)} onClick={() => handleExport('cashflow')}>
              {exportingType === 'cashflow' ? 'Đang tải...' : 'CSV Cashflow'}
            </button>
            <button disabled={Boolean(exportingType)} onClick={() => handleExport('transaction-breakdown')}>
              {exportingType === 'transaction-breakdown' ? 'Đang tải...' : 'CSV Phân loại GD'}
            </button>
          </div>
        </div>
      </section>
      {renderContent()}
    </AdminLayout>
  );
}
