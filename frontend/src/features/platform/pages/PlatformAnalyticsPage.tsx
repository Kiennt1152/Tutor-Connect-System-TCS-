import { useEffect, useState } from 'react';
import { AdminLayout } from '../components/AdminLayout';
import { platformApi } from '../api/platformApi';
import type { AnalyticsSummaryApiResponse } from '../types/platformTypes';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import './PlatformAnalyticsPage.css';

export default function PlatformAnalyticsPage() {
  const [data, setData] = useState<AnalyticsSummaryApiResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [exportingType, setExportingType] = useState<string | null>(null);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const response = await platformApi.getAnalyticsSummary(from, to);
        setData(response.data);
      } catch (err) {
        setError(getApiErrorMessage(err));
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [from, to]);

  const handleExport = async (type: 'users' | 'classes' | 'revenue') => {
    setExportingType(type);
    try {
      const response = await platformApi.exportAnalyticsCsv(type, from, to);
      const url = window.URL.createObjectURL(response.data);
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
          <div>Đang tải dữ liệu báo cáo...</div>
        </div>
      );
    }

    if (error) {
      return <div style={{ color: 'red', padding: '1rem' }}>{error}</div>;
    }

    if (!data) return null;

    return (
      <div>
        <div className="adm-analytics-header-actions" style={{ marginBottom: '2rem' }}>
          <label>Từ ngày <input type="date" value={from} max={to || undefined} onChange={(event) => setFrom(event.target.value)} /></label>
          <label>Đến ngày <input type="date" value={to} min={from || undefined} onChange={(event) => setTo(event.target.value)} /></label>
          <button disabled={exportingType === 'users'} onClick={() => handleExport('users')}>
            {exportingType === 'users' ? 'Đang tải...' : 'Tải CSV Người dùng'}
          </button>
          <button disabled={exportingType === 'classes'} onClick={() => handleExport('classes')}>
            {exportingType === 'classes' ? 'Đang tải...' : 'Tải CSV Lớp học'}
          </button>
          <button disabled={exportingType === 'revenue'} onClick={() => handleExport('revenue')}>
            {exportingType === 'revenue' ? 'Đang tải...' : 'Tải CSV Doanh thu'}
          </button>
        </div>

        <h2 className="adm-kpi-section-title">Cơ cấu Người dùng & Hệ sinh thái</h2>
        <div className="adm-analytics-grid-4">
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Tổng người dùng</span>
            <span className="adm-analytics-card-value">{data.totalUsers.toLocaleString('vi-VN')}</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Gia sư</span>
            <span className="adm-analytics-card-value">{data.totalTutors.toLocaleString('vi-VN')}</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Phụ huynh/Học viên</span>
            <span className="adm-analytics-card-value">{data.totalParents.toLocaleString('vi-VN')}</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Trung tâm</span>
            <span className="adm-analytics-card-value">{data.totalCenters.toLocaleString('vi-VN')}</span>
          </div>
        </div>

        <h2 className="adm-kpi-section-title">Hoạt động & Hiệu suất</h2>
        <div className="adm-analytics-grid-4">
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Tổng lớp học</span>
            <span className="adm-analytics-card-value">{data.totalClasses.toLocaleString('vi-VN')}</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Lớp đang diễn ra</span>
            <span className="adm-analytics-card-value">{data.activeClasses.toLocaleString('vi-VN')}</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Tỷ lệ hoàn thành hợp đồng</span>
            <span className="adm-analytics-card-value">{data.contractCompletionRate.toFixed(1)}%</span>
            <div className="adm-progress-bar-container">
              <div
                className="adm-progress-bar adm-progress-bar--green"
                style={{ width: `${Math.min(100, data.contractCompletionRate)}%` }}
              ></div>
            </div>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Tỷ lệ duyệt gia sư</span>
            <span className="adm-analytics-card-value">{data.verificationConversionRate.toFixed(1)}%</span>
            <div className="adm-progress-bar-container">
              <div
                className="adm-progress-bar adm-progress-bar--green"
                style={{ width: `${Math.min(100, data.verificationConversionRate)}%` }}
              ></div>
            </div>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Tỷ lệ khiếu nại</span>
            <span className="adm-analytics-card-value">{data.disputeRate.toFixed(1)}%</span>
            <div className="adm-progress-bar-container">
              <div
                className="adm-progress-bar"
                style={{ width: `${Math.min(100, data.disputeRate)}%`, backgroundColor: '#ef4444' }}
              ></div>
            </div>
          </div>
        </div>

        <h2 className="adm-kpi-section-title">Doanh thu & Tài chính Sàn</h2>
        <div className="adm-analytics-grid-2">
          <div className="adm-analytics-card adm-analytics-card--revenue">
            <span className="adm-analytics-card-title">Tổng giao dịch qua sàn</span>
            <span className="adm-analytics-card-value">{data.totalRevenue.toLocaleString('vi-VN')} VND</span>
          </div>
          <div className="adm-analytics-card adm-analytics-card--revenue">
            <span className="adm-analytics-card-title">
              Doanh thu phí dịch vụ ({(data.platformFeeRate * 100).toLocaleString('vi-VN')}%)
            </span>
            <span className="adm-analytics-card-value">{data.platformFeeRevenue.toLocaleString('vi-VN')} VND</span>
          </div>
        </div>
        <div className="adm-analytics-grid-4">
          <div className="adm-analytics-card"><span className="adm-analytics-card-title">Tiền nạp</span><span className="adm-analytics-card-value">{data.deposits.toLocaleString('vi-VN')} VND</span></div>
          <div className="adm-analytics-card"><span className="adm-analytics-card-title">Tiền rút</span><span className="adm-analytics-card-value">{data.withdrawals.toLocaleString('vi-VN')} VND</span></div>
          <div className="adm-analytics-card"><span className="adm-analytics-card-title">Escrow đang giữ</span><span className="adm-analytics-card-value">{data.escrowHeld.toLocaleString('vi-VN')} VND</span></div>
          <div className="adm-analytics-card"><span className="adm-analytics-card-title">Escrow đã giải ngân / hoàn</span><span className="adm-analytics-card-value">{data.escrowReleased.toLocaleString('vi-VN')} / {data.escrowRefunded.toLocaleString('vi-VN')} VND</span></div>
        </div>

        <h2 className="adm-kpi-section-title">Tăng trưởng 6 tháng gần nhất</h2>
        <div className="adm-analytics-table-container">
          <table className="adm-analytics-table">
            <thead>
              <tr>
                <th>Tháng</th>
                <th>Người dùng mới</th>
                <th>Lớp học mới</th>
                <th>Doanh thu (VND)</th>
              </tr>
            </thead>
            <tbody>
              {data.monthlyMetrics?.map((metric, idx) => (
                <tr key={idx}>
                  <td>{metric.month}</td>
                  <td>{metric.newUsers.toLocaleString('vi-VN')}</td>
                  <td>{metric.newClasses.toLocaleString('vi-VN')}</td>
                  <td style={{ fontWeight: 500 }}>{metric.revenue.toLocaleString('vi-VN')}</td>
                </tr>
              ))}
              {(!data.monthlyMetrics || data.monthlyMetrics.length === 0) && (
                <tr>
                  <td colSpan={4} style={{ textAlign: 'center', color: '#64748b' }}>
                    Không có dữ liệu
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    );
  };

  return (
    <AdminLayout
      title="Báo cáo & Phân tích"
      subtitle="Thống kê tổng quan hoạt động và doanh thu của nền tảng"
    >
      {renderContent()}
    </AdminLayout>
  );
}
