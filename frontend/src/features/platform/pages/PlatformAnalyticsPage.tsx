/**
 * ============================================================================
 * TRANG BÁO CÁO PHÂN TÍCH VÀ THỐNG KÊ KINH DOANH (PLATFORM ANALYTICS PAGE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp
 * Mô tả các tính năng phân tích và xuất dữ liệu:
 *   - Hiển thị bảng tổng hợp tài chính (Tổng nạp, Rút, Escrow ký quỹ, Doanh thu phí sàn).
 *   - Biểu đồ và bảng phân rã loại giao dịch (Transaction Breakdown) theo chiều IN/OUT.
 *   - Xuất file báo cáo CSV đa dạng: Danh sách người dùng, Lớp học, Doanh thu, Dòng tiền (Cashflow), Phân loại giao dịch.
 *   - Lọc dữ liệu linh hoạt theo khoảng thời gian thực tế.
 */

import { useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { AdminLayout } from '../components/AdminLayout';
import { AdminTimeFilter, type TimeFilterValue } from '../components/AdminTimeFilter';
import { platformApi } from '../api/platformApi';
import type {
  AnalyticsSummaryApiResponse,
  CenterFinancialAnalyticsApiResponse,
  TutorFinancialAnalyticsApiResponse,
  ClientFinancialAnalyticsApiResponse,
  FinancialLedgerItemApiResponse,
  PageFinancialLedgerApiResponse,
} from '../types/platformTypes';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import { Pagination } from '../../../shared/components/Pagination';
import './PlatformAnalyticsPage.css';

type AnalyticsTab = 'overview' | 'centers' | 'individuals' | 'ledger';
type IndividualSubTab = 'tutors' | 'clients';

function formatVnd(amount?: number | null): string {
  if (amount == null) return '0 ₫';
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

function formatSignedVnd(amount?: number | null, direction?: string): { text: string; color: string } {
  if (amount == null || amount === 0) {
    return { text: '0 ₫', color: '#64748b' };
  }
  const formatted = Math.abs(amount).toLocaleString('vi-VN') + ' ₫';
  if (direction === 'IN') {
    return { text: `+${formatted}`, color: '#16a34a' };
  } else if (direction === 'OUT') {
    return { text: `-${formatted}`, color: '#dc2626' };
  }
  return { text: formatted, color: '#334155' };
}

const TX_TYPE_DESCRIPTIONS: Record<string, string> = {
  DEPOSIT: 'Nạp tiền từ cổng thanh toán/ngân hàng vào ví cá nhân',
  ESCROW_DEPOSIT: 'Khách hàng ký quỹ giữ tiền học phí bảo chứng khi mở hợp đồng lớp',
  PLATFORM_FEE: 'Doanh thu phí dịch vụ sàn thu được (2% mỗi giao dịch hoàn tất)',
  WITHDRAWAL: 'Yêu cầu rút tiền từ số dư ví về tài khoản ngân hàng',
  ESCROW_RELEASE: 'Giải ngân học phí bảo chứng cho gia sư khi lớp hoàn thành',
  REFUND: 'Hoàn trả lại tiền ký quỹ cho khách hàng (khi hủy lớp hoặc xử lý sự cố)',
};

function formatDate(dateStr?: string | null): string {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    return d.toLocaleString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return dateStr;
  }
}

export default function PlatformAnalyticsPage() {
  const [searchParams] = useSearchParams();
  const [activeTab, setActiveTab] = useState<AnalyticsTab>('overview');
  const [individualSubTab, setIndividualSubTab] = useState<IndividualSubTab>('tutors');

  const [appliedFrom, setAppliedFrom] = useState(searchParams.get('from') || '');
  const [appliedTo, setAppliedTo] = useState(searchParams.get('to') || '');
  const [exportingType, setExportingType] = useState<string | null>(null);

  // Tab 1: Summary Data
  const [summaryData, setSummaryData] = useState<AnalyticsSummaryApiResponse | null>(null);
  const [summaryLoading, setSummaryLoading] = useState<boolean>(true);
  const [summaryError, setSummaryError] = useState<string | null>(null);
  const [breakdownFilter, setBreakdownFilter] = useState<'ALL' | 'IN' | 'OUT' | 'NON_ZERO'>('ALL');

  // Tab 2: Centers Data
  const [centersData, setCentersData] = useState<CenterFinancialAnalyticsApiResponse[]>([]);
  const [centersLoading, setCentersLoading] = useState<boolean>(false);
  const [centersSearch, setCentersSearch] = useState<string>('');

  // Tab 3: Individuals Data
  const [tutorsData, setTutorsData] = useState<TutorFinancialAnalyticsApiResponse[]>([]);
  const [clientsData, setClientsData] = useState<ClientFinancialAnalyticsApiResponse[]>([]);
  const [individualsLoading, setIndividualsLoading] = useState<boolean>(false);
  const [individualsSearch, setIndividualsSearch] = useState<string>('');

  // Tab 4: Ledger Data
  const [ledgerData, setLedgerData] = useState<PageFinancialLedgerApiResponse | null>(null);
  const [ledgerLoading, setLedgerLoading] = useState<boolean>(false);
  const [ledgerRole, setLedgerRole] = useState<string>('ALL');
  const [ledgerDirection, setLedgerDirection] = useState<string>('ALL');
  const [ledgerSearch, setLedgerSearch] = useState<string>('');
  const [ledgerPage, setLedgerPage] = useState<number>(0);
  const [ledgerPageSize] = useState<number>(15);

  // Load Overview Data
  const fetchSummary = useCallback(async () => {
    try {
      setSummaryLoading(true);
      setSummaryError(null);
      const res = await platformApi.getAnalyticsSummary(appliedFrom, appliedTo);
      setSummaryData(res.data);
    } catch (err) {
      setSummaryError(getApiErrorMessage(err));
    } finally {
      setSummaryLoading(false);
    }
  }, [appliedFrom, appliedTo]);

  // Load Centers Data
  const fetchCenters = useCallback(async () => {
    try {
      setCentersLoading(true);
      const res = await platformApi.getCenterAnalytics(appliedFrom, appliedTo);
      setCentersData(res.data);
    } catch (err) {
      console.error('Lỗi lấy phân tích trung tâm:', err);
    } finally {
      setCentersLoading(false);
    }
  }, [appliedFrom, appliedTo]);

  // Load Individuals Data
  const fetchIndividuals = useCallback(async () => {
    try {
      setIndividualsLoading(true);
      const [tutorsRes, clientsRes] = await Promise.all([
        platformApi.getTutorAnalytics(appliedFrom, appliedTo),
        platformApi.getClientAnalytics(appliedFrom, appliedTo),
      ]);
      setTutorsData(tutorsRes.data);
      setClientsData(clientsRes.data);
    } catch (err) {
      console.error('Lỗi lấy phân tích cá nhân:', err);
    } finally {
      setIndividualsLoading(false);
    }
  }, [appliedFrom, appliedTo]);

  // Load Ledger Data
  const fetchLedger = useCallback(async () => {
    try {
      setLedgerLoading(true);
      const res = await platformApi.getFinancialLedger({
        role: ledgerRole,
        direction: ledgerDirection,
        search: ledgerSearch,
        from: appliedFrom,
        to: appliedTo,
        page: ledgerPage,
        size: ledgerPageSize,
      });
      setLedgerData(res.data);
    } catch (err) {
      console.error('Lỗi lấy sổ cái giao dịch:', err);
    } finally {
      setLedgerLoading(false);
    }
  }, [ledgerRole, ledgerDirection, ledgerSearch, appliedFrom, appliedTo, ledgerPage, ledgerPageSize]);

  // Effect: Load active tab
  useEffect(() => {
    if (activeTab === 'overview') {
      fetchSummary();
    } else if (activeTab === 'centers') {
      fetchCenters();
    } else if (activeTab === 'individuals') {
      fetchIndividuals();
    } else if (activeTab === 'ledger') {
      fetchLedger();
    }
  }, [activeTab, fetchSummary, fetchCenters, fetchIndividuals, fetchLedger]);

  const handleTimeFilterChange = (val: TimeFilterValue) => {
    setAppliedFrom(val.from);
    setAppliedTo(val.to);
    setLedgerPage(0);
  };

  const handleExport = async (type: string) => {
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

  const filteredCenters = centersData.filter((c) => {
    if (!centersSearch.trim()) return true;
    const term = centersSearch.toLowerCase();
    return (
      c.companyName?.toLowerCase().includes(term) ||
      c.licenseNo?.toLowerCase().includes(term) ||
      c.email?.toLowerCase().includes(term) ||
      c.phone?.toLowerCase().includes(term)
    );
  });

  const filteredTutors = tutorsData.filter((t) => {
    if (!individualsSearch.trim()) return true;
    const term = individualsSearch.toLowerCase();
    return (
      t.fullName?.toLowerCase().includes(term) ||
      t.email?.toLowerCase().includes(term) ||
      t.phone?.toLowerCase().includes(term)
    );
  });

  const filteredClients = clientsData.filter((c) => {
    if (!individualsSearch.trim()) return true;
    const term = individualsSearch.toLowerCase();
    return (
      c.fullName?.toLowerCase().includes(term) ||
      c.email?.toLowerCase().includes(term) ||
      c.phone?.toLowerCase().includes(term)
    );
  });

  // Render Tab 1: Overview
  const renderOverview = () => {
    if (summaryLoading) {
      return (
        <div className="adm-analytics-loading">
          <div>Đang tổng hợp số liệu phân tích sàn...</div>
        </div>
      );
    }
    if (summaryError) {
      return <div className="adm-analytics-error">{summaryError}</div>;
    }
    if (!summaryData) return null;

    const maxMonthlyRevenue = Math.max(
      ...(summaryData.monthlyMetrics?.map((m) => m.revenue) || [1]),
      1,
    );

    const breakdownList = summaryData.transactionTypeBreakdown || [];
    const filteredBreakdown = breakdownList.filter((t) => {
      if (breakdownFilter === 'IN') return t.direction === 'IN';
      if (breakdownFilter === 'OUT') return t.direction === 'OUT';
      if (breakdownFilter === 'NON_ZERO') return (t.totalAmount ?? 0) > 0 || t.count > 0;
      return true;
    });

    const totalInAmount = breakdownList
      .filter((t) => t.direction === 'IN')
      .reduce((acc, curr) => acc + (curr.totalAmount || 0), 0);
    const totalInCount = breakdownList
      .filter((t) => t.direction === 'IN')
      .reduce((acc, curr) => acc + (curr.count || 0), 0);

    const totalOutAmount = breakdownList
      .filter((t) => t.direction === 'OUT')
      .reduce((acc, curr) => acc + (curr.totalAmount || 0), 0);
    const totalOutCount = breakdownList
      .filter((t) => t.direction === 'OUT')
      .reduce((acc, curr) => acc + (curr.count || 0), 0);

    return (
      <div className="adm-tab-pane">
        {/* KPI Row 1: Users & Ecosystem */}
        <h2 className="adm-kpi-section-title">Cơ cấu Người dùng & Hệ sinh thái</h2>
        <div className="adm-analytics-grid-4">
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Tổng người dùng</span>
            <span className="adm-analytics-card-value">{summaryData.totalUsers?.toLocaleString('vi-VN')}</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Gia sư độc lập</span>
            <span className="adm-analytics-card-value">{summaryData.totalTutors?.toLocaleString('vi-VN')}</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Phụ huynh / Học viên</span>
            <span className="adm-analytics-card-value">{summaryData.totalParents?.toLocaleString('vi-VN')}</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Trung tâm gia sư</span>
            <span className="adm-analytics-card-value">{summaryData.totalCenters?.toLocaleString('vi-VN')}</span>
          </div>
        </div>

        {/* KPI Row 2: Performance & Conversion */}
        <h2 className="adm-kpi-section-title">Hoạt động Lớp học & Vận hành</h2>
        <div className="adm-analytics-grid-4">
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Tổng lớp học</span>
            <span className="adm-analytics-card-value">{summaryData.totalClasses?.toLocaleString('vi-VN')}</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Lớp đang giảng dạy</span>
            <span className="adm-analytics-card-value">{summaryData.activeClasses?.toLocaleString('vi-VN')}</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Tỷ lệ hoàn thành hợp đồng</span>
            <span className="adm-analytics-card-value">{summaryData.contractCompletionRate?.toFixed(1)}%</span>
            <div className="adm-progress-bar-container">
              <div
                className="adm-progress-bar adm-progress-bar--green"
                style={{ width: `${Math.min(100, summaryData.contractCompletionRate ?? 0)}%` }}
              />
            </div>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Tỷ lệ xác minh hồ sơ gia sư</span>
            <span className="adm-analytics-card-value">{summaryData.verificationConversionRate?.toFixed(1)}%</span>
            <div className="adm-progress-bar-container">
              <div
                className="adm-progress-bar adm-progress-bar--green"
                style={{ width: `${Math.min(100, summaryData.verificationConversionRate ?? 0)}%` }}
              />
            </div>
          </div>
        </div>

        {/* Financial Flow Section */}
        <h2 className="adm-kpi-section-title">Dòng tiền & Doanh thu Toàn sàn</h2>
        <div className="adm-analytics-grid-4" style={{ marginBottom: '1.5rem' }}>
          <div className="adm-analytics-card adm-analytics-card--in">
            <span className="adm-analytics-card-title">Tiền Vào Toàn Sàn (IN)</span>
            <span className="adm-analytics-card-value">+{formatVnd(summaryData.moneyIn)}</span>
            <span className="adm-card-sublabel">Nạp ví & Đặt cọc ký quỹ lớp</span>
          </div>
          <div className="adm-analytics-card adm-analytics-card--out">
            <span className="adm-analytics-card-title">Tiền Ra Khỏi Sàn (OUT)</span>
            <span className="adm-analytics-card-value">-{formatVnd(summaryData.moneyOut)}</span>
            <span className="adm-card-sublabel">Đã rút về tài khoản ngân hàng</span>
          </div>
          <div
            className={`adm-analytics-card ${
              (summaryData.netMovement || 0) >= 0
                ? 'adm-analytics-card--net-positive'
                : 'adm-analytics-card--net-negative'
            }`}
          >
            <span className="adm-analytics-card-title">Dòng tiền ròng (Net Flow)</span>
            <span className="adm-analytics-card-value">{formatVnd(summaryData.netMovement)}</span>
            <span className="adm-card-sublabel">Chênh lệch tiền vào - tiền ra</span>
          </div>
          <div className="adm-analytics-card adm-analytics-card--fee">
            <span className="adm-analytics-card-title">Doanh thu Phí Sàn (2%)</span>
            <span className="adm-analytics-card-value">{formatVnd(summaryData.platformFeeRevenue)}</span>
            <span className="adm-card-sublabel">Thu thực tế từ các lớp giải ngân</span>
          </div>
        </div>

        {/* Pending Withdrawals Notice Card if any */}
        {(summaryData.pendingWithdrawals ?? 0) > 0 && (
          <div className="adm-alert-card adm-alert-card--warn" style={{ marginBottom: '1.5rem' }}>
            <div className="adm-alert-card__content">
              <strong>Yêu cầu rút tiền đang chờ xử lý:</strong> Hiện có tổng cộng{' '}
              <span className="adm-text-danger" style={{ fontWeight: 700 }}>
                {formatVnd(summaryData.pendingWithdrawals)}
              </span>{' '}
              tiền rút đang chờ quản trị viên duyệt hoặc xác nhận chuyển khoản.
            </div>
            <a href="/platform/withdrawals" className="tcs-btn tcs-btn--sm tcs-btn--primary">
              Đến trang Duyệt rút tiền →
            </a>
          </div>
        )}

        {/* Escrow Flow Section */}
        <h2 className="adm-kpi-section-title">Ký quỹ Đảm bảo Lớp học (Escrow Flow)</h2>
        <div className="adm-analytics-grid-4" style={{ marginBottom: '1.5rem' }}>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Tổng nạp ký quỹ</span>
            <span className="adm-analytics-card-value" style={{ color: '#0284c7' }}>
              {formatVnd(summaryData.escrowFlow?.deposited ?? summaryData.deposits)}
            </span>
            <span className="adm-card-sublabel">Tiền học phí phụ huynh đặt cọc</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Đã giải ngân cho gia sư</span>
            <span className="adm-analytics-card-value" style={{ color: '#16a34a' }}>
              {formatVnd(summaryData.escrowFlow?.released ?? summaryData.escrowReleased)}
            </span>
            <span className="adm-card-sublabel">Lớp hoàn thành thành công</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Đã hoàn trả khách hàng</span>
            <span className="adm-analytics-card-value" style={{ color: '#dc2626' }}>
              {formatVnd(summaryData.escrowFlow?.refunded ?? summaryData.escrowRefunded)}
            </span>
            <span className="adm-card-sublabel">Hoàn cọc khi sự cố/hủy lớp</span>
          </div>
          <div className="adm-analytics-card">
            <span className="adm-analytics-card-title">Đang giữ trong ký quỹ</span>
            <span className="adm-analytics-card-value" style={{ color: '#ca8a04' }}>
              {formatVnd(summaryData.escrowFlow?.held ?? summaryData.escrowHeld)}
            </span>
            <span className="adm-card-sublabel">Lớp học đang diễn ra an toàn</span>
          </div>
        </div>

        {/* Transaction Breakdown Table */}
        <div className="adm-analytics-table-container" style={{ marginBottom: '2rem' }}>
          <div className="adm-breakdown-header">
            <div>
              <div className="adm-table-header-title">Phân rã chi tiết giao dịch phát sinh trên sàn</div>
              <p className="adm-breakdown-subtitle">
                Phân loại theo bản chất chiều luân chuyển dòng tiền đối với Sàn: <strong>Tiền vào Sàn (INFLOW)</strong> và <strong>Tiền ra khỏi Sàn (OUTFLOW)</strong>
              </p>
            </div>
            <div className="adm-breakdown-filter-pills">
              <button
                type="button"
                className={`adm-pill-btn ${breakdownFilter === 'ALL' ? 'adm-pill-btn--active' : ''}`}
                onClick={() => setBreakdownFilter('ALL')}
              >
                Tất cả (6 danh mục)
              </button>
              <button
                type="button"
                className={`adm-pill-btn ${breakdownFilter === 'IN' ? 'adm-pill-btn--active' : ''}`}
                onClick={() => setBreakdownFilter('IN')}
              >
                Tiền vào Sàn (IN)
              </button>
              <button
                type="button"
                className={`adm-pill-btn ${breakdownFilter === 'OUT' ? 'adm-pill-btn--active' : ''}`}
                onClick={() => setBreakdownFilter('OUT')}
              >
                Tiền ra khỏi Sàn (OUT)
              </button>
              <button
                type="button"
                className={`adm-pill-btn ${breakdownFilter === 'NON_ZERO' ? 'adm-pill-btn--active' : ''}`}
                onClick={() => setBreakdownFilter('NON_ZERO')}
              >
                Chỉ loại có phát sinh (&gt; 0)
              </button>
            </div>
          </div>

          <table className="adm-analytics-table">
            <thead>
              <tr>
                <th style={{ width: '25%' }}>Loại giao dịch</th>
                <th style={{ width: '16%' }}>Phân loại dòng tiền</th>
                <th>Ý nghĩa nghiệp vụ</th>
                <th style={{ textAlign: 'right', width: '12%' }}>Số lượng</th>
                <th style={{ textAlign: 'right', width: '16%' }}>Tổng tiền (VND)</th>
              </tr>
            </thead>
            <tbody>
              {filteredBreakdown.map((tx, idx) => {
                const signed = formatSignedVnd(tx.totalAmount, tx.direction);
                return (
                  <tr key={idx}>
                    <td><strong>{tx.label}</strong></td>
                    <td>
                      <span className={`adm-direction-badge ${tx.direction === 'IN' ? 'adm-direction-badge--in' : 'adm-direction-badge--out'}`}>
                        {tx.direction === 'IN' ? 'Tiền vào sàn (IN)' : 'Tiền ra khỏi sàn (OUT)'}
                      </span>
                    </td>
                    <td style={{ fontSize: '0.85rem', color: '#475569' }}>
                      {TX_TYPE_DESCRIPTIONS[tx.type] || '—'}
                    </td>
                    <td style={{ textAlign: 'right', fontWeight: 500 }}>
                      {tx.count.toLocaleString('vi-VN')}
                    </td>
                    <td
                      style={{
                        textAlign: 'right',
                        fontWeight: 600,
                        color: signed.color,
                      }}
                    >
                      {signed.text}
                    </td>
                  </tr>
                );
              })}
              {filteredBreakdown.length === 0 && (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', color: '#64748b', padding: '1.5rem' }}>
                    Không có loại giao dịch nào phù hợp với bộ lọc hiện tại
                  </td>
                </tr>
              )}
            </tbody>
            {summaryData.transactionTypeBreakdown && summaryData.transactionTypeBreakdown.length > 0 && (
              <tfoot>
                <tr className="adm-breakdown-total-row">
                  <td colSpan={3}>
                    <strong>Tổng dòng tiền vào Sàn (INFLOW)</strong>
                    <span className="adm-sublabel-inline"> (Nạp tiền ví + Ký quỹ lớp + Phí hoa hồng sàn)</span>
                  </td>
                  <td style={{ textAlign: 'right', fontWeight: 600 }}>{totalInCount.toLocaleString('vi-VN')}</td>
                  <td style={{ textAlign: 'right', fontWeight: 700, color: totalInAmount > 0 ? '#16a34a' : '#64748b' }}>
                    {formatSignedVnd(totalInAmount, 'IN').text}
                  </td>
                </tr>
                <tr className="adm-breakdown-total-row">
                  <td colSpan={3}>
                    <strong>Tổng dòng tiền ra khỏi Sàn (OUTFLOW)</strong>
                    <span className="adm-sublabel-inline"> (Rút tiền về ngân hàng + Giải ngân học phí + Hoàn cọc)</span>
                  </td>
                  <td style={{ textAlign: 'right', fontWeight: 600 }}>{totalOutCount.toLocaleString('vi-VN')}</td>
                  <td style={{ textAlign: 'right', fontWeight: 700, color: totalOutAmount > 0 ? '#dc2626' : '#64748b' }}>
                    {formatSignedVnd(totalOutAmount, 'OUT').text}
                  </td>
                </tr>
              </tfoot>
            )}
          </table>
        </div>

        {/* Monthly Trend Chart & Table */}
        <h2 className="adm-kpi-section-title">Biểu đồ Tăng trưởng & Doanh thu (6 Tháng gần nhất)</h2>
        <div className="adm-chart-container" style={{ marginBottom: '2rem' }}>
          {summaryData.monthlyMetrics && summaryData.monthlyMetrics.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div className="adm-bar-chart">
                {summaryData.monthlyMetrics.map((m, idx) => {
                  const barHeight = Math.max(16, Math.round((m.revenue / maxMonthlyRevenue) * 120));
                  return (
                    <div key={idx} className="adm-bar-col">
                      <span className="adm-bar-value">
                        {m.revenue >= 1000000 ? `${(m.revenue / 1000000).toFixed(1)}M` : `${(m.revenue / 1000).toFixed(0)}k`}
                      </span>
                      <div className="adm-bar" style={{ height: `${barHeight}px` }} />
                      <span className="adm-bar-label">{m.month}</span>
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
                    <th style={{ textAlign: 'right' }}>Doanh thu phí sàn (VND)</th>
                  </tr>
                </thead>
                <tbody>
                  {summaryData.monthlyMetrics.map((metric, idx) => (
                    <tr key={idx}>
                      <td><strong>{metric.month}</strong></td>
                      <td style={{ textAlign: 'right' }}>+{metric.newUsers.toLocaleString('vi-VN')}</td>
                      <td style={{ textAlign: 'right' }}>+{metric.newClasses.toLocaleString('vi-VN')}</td>
                      <td style={{ textAlign: 'right', fontWeight: 600, color: 'var(--color-primary-dark)' }}>
                        {formatVnd(metric.revenue)}
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

  // Render Tab 2: Centers
  const renderCenters = () => {
    return (
      <div className="adm-tab-pane">
        <div className="adm-table-toolbar">
          <div className="adm-search-input-wrap">
            <input
              type="text"
              className="adm-search-input"
              placeholder="Tìm theo tên trung tâm, giấy phép, email, số điện thoại..."
              value={centersSearch}
              onChange={(e) => setCentersSearch(e.target.value)}
            />
          </div>
          <button
            className="tcs-btn tcs-btn--outline"
            disabled={Boolean(exportingType)}
            onClick={() => handleExport('centers')}
          >
            {exportingType === 'centers' ? 'Đang xuất...' : 'Xuất CSV Trung tâm'}
          </button>
        </div>

        <div className="adm-analytics-table-container">
          <table className="adm-analytics-table">
            <thead>
              <tr>
                <th>Trung tâm</th>
                <th>Liên hệ</th>
                <th style={{ textAlign: 'center' }}>Lớp học (Tổng/Đang/Xong)</th>
                <th style={{ textAlign: 'center' }}>Gia sư (Tổng / Mới)</th>
                <th style={{ textAlign: 'right' }}>Tiền vào (Học phí)</th>
                <th style={{ textAlign: 'right' }}>Tiền ra (Đã rút)</th>
                <th style={{ textAlign: 'right' }}>Ký quỹ giữ</th>
                <th style={{ textAlign: 'right' }}>Phí sàn nộp (2%)</th>
                <th style={{ textAlign: 'right' }}>Số dư ví</th>
              </tr>
            </thead>
            <tbody>
              {centersLoading ? (
                <tr>
                  <td colSpan={9} style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                    Đang tải dữ liệu trung tâm gia sư...
                  </td>
                </tr>
              ) : filteredCenters.length > 0 ? (
                filteredCenters.map((center) => (
                  <tr key={center.centerId}>
                    <td>
                      <div className="adm-entity-name">
                        <strong>{center.companyName}</strong>
                        {center.licenseNo && <small className="adm-badge-code">GP: {center.licenseNo}</small>}
                      </div>
                    </td>
                    <td>
                      <div className="adm-contact-col">
                        <span>{center.email || '—'}</span>
                        <small>{center.phone || '—'}</small>
                      </div>
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <span className="adm-stat-pill">
                        {center.totalClasses} / <b style={{ color: '#0284c7' }}>{center.activeClasses}</b> /{' '}
                        <b style={{ color: '#16a34a' }}>{center.completedClasses}</b>
                      </span>
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <div className="adm-tutors-metric">
                        <span>{center.totalTutors} gia sư</span>
                        {center.newTutorsInPeriod > 0 && (
                          <span className="adm-pill-new">+{center.newTutorsInPeriod} mới</span>
                        )}
                      </div>
                    </td>
                    <td style={{ textAlign: 'right', fontWeight: 600, color: 'var(--color-success)' }}>
                      +{formatVnd(center.moneyIn)}
                    </td>
                    <td style={{ textAlign: 'right', fontWeight: 600, color: 'var(--color-error)' }}>
                      -{formatVnd(center.moneyOut)}
                    </td>
                    <td style={{ textAlign: 'right', color: '#ca8a04', fontWeight: 500 }}>
                      {formatVnd(center.escrowHeld)}
                    </td>
                    <td style={{ textAlign: 'right', color: 'var(--color-primary-dark)', fontWeight: 600 }}>
                      {formatVnd(center.platformFeePaid)}
                    </td>
                    <td style={{ textAlign: 'right', fontWeight: 700 }}>
                      {formatVnd(center.walletBalance)}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={9} style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                    Không tìm thấy trung tâm gia sư nào phù hợp.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    );
  };

  // Render Tab 3: Individuals (Tutors & Clients)
  const renderIndividuals = () => {
    return (
      <div className="adm-tab-pane">
        <div className="adm-subtabs-toolbar">
          <div className="adm-subtabs">
            <button
              className={`adm-subtab ${individualSubTab === 'tutors' ? 'adm-subtab--active' : ''}`}
              onClick={() => setIndividualSubTab('tutors')}
            >
              Gia sư độc lập ({tutorsData.length})
            </button>
            <button
              className={`adm-subtab ${individualSubTab === 'clients' ? 'adm-subtab--active' : ''}`}
              onClick={() => setIndividualSubTab('clients')}
            >
              Phụ huynh / Học viên ({clientsData.length})
            </button>
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', alignItems: 'center' }}>
            <input
              type="text"
              className="adm-search-input"
              placeholder={`Tìm theo tên, email, SĐT ${individualSubTab === 'tutors' ? 'gia sư' : 'phụ huynh'}...`}
              value={individualsSearch}
              onChange={(e) => setIndividualsSearch(e.target.value)}
              style={{ width: '280px' }}
            />
            <button
              className="tcs-btn tcs-btn--outline"
              disabled={Boolean(exportingType)}
              onClick={() => handleExport(individualSubTab === 'tutors' ? 'tutors' : 'clients')}
            >
              {exportingType === (individualSubTab === 'tutors' ? 'tutors' : 'clients')
                ? 'Đang xuất...'
                : `Xuất CSV ${individualSubTab === 'tutors' ? 'Gia sư' : 'Phụ huynh'}`}
            </button>
          </div>
        </div>

        {individualSubTab === 'tutors' ? (
          <div className="adm-analytics-table-container">
            <table className="adm-analytics-table">
              <thead>
                <tr>
                  <th>Gia sư</th>
                  <th>Liên hệ</th>
                  <th style={{ textAlign: 'center' }}>Xác minh</th>
                  <th style={{ textAlign: 'center' }}>Lớp (Tổng/Đang/Xong)</th>
                  <th style={{ textAlign: 'center' }}>Lớp mới</th>
                  <th style={{ textAlign: 'right' }}>Thu nhập tích lũy</th>
                  <th style={{ textAlign: 'right' }}>Đã rút (OUT)</th>
                  <th style={{ textAlign: 'right' }}>Chờ rút</th>
                  <th style={{ textAlign: 'right' }}>Ký quỹ giữ</th>
                  <th style={{ textAlign: 'right' }}>Số dư ví</th>
                  <th style={{ textAlign: 'center' }}>Đánh giá</th>
                </tr>
              </thead>
              <tbody>
                {individualsLoading ? (
                  <tr>
                    <td colSpan={11} style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                      Đang tải danh sách gia sư...
                    </td>
                  </tr>
                ) : filteredTutors.length > 0 ? (
                  filteredTutors.map((tutor) => (
                    <tr key={tutor.tutorId}>
                      <td>
                        <strong>{tutor.fullName}</strong>
                      </td>
                      <td>
                        <div className="adm-contact-col">
                          <span>{tutor.email || '—'}</span>
                          <small>{tutor.phone || '—'}</small>
                        </div>
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        <span className={`tcs-badge ${tutor.verificationStatus === 'VERIFIED' ? 'tcs-badge--active' : 'tcs-badge--suspended'}`}>
                          {tutor.verificationStatus === 'VERIFIED' ? 'Đã duyệt' : tutor.verificationStatus}
                        </span>
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        <span className="adm-stat-pill">
                          {tutor.totalClasses} / <b style={{ color: '#0284c7' }}>{tutor.activeClasses}</b> /{' '}
                          <b style={{ color: '#16a34a' }}>{tutor.completedClasses}</b>
                        </span>
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        {tutor.newClassesInPeriod > 0 ? (
                          <span className="adm-pill-new">+{tutor.newClassesInPeriod}</span>
                        ) : (
                          '0'
                        )}
                      </td>
                      <td style={{ textAlign: 'right', fontWeight: 600, color: 'var(--color-success)' }}>
                        +{formatVnd(tutor.totalEarnings)}
                      </td>
                      <td style={{ textAlign: 'right', fontWeight: 600, color: 'var(--color-error)' }}>
                        -{formatVnd(tutor.totalWithdrawn)}
                      </td>
                      <td style={{ textAlign: 'right', color: tutor.pendingWithdrawals > 0 ? '#ea580c' : '#64748b', fontWeight: tutor.pendingWithdrawals > 0 ? 700 : 400 }}>
                        {formatVnd(tutor.pendingWithdrawals)}
                      </td>
                      <td style={{ textAlign: 'right', color: '#ca8a04', fontWeight: 500 }}>
                        {formatVnd(tutor.escrowHolding)}
                      </td>
                      <td style={{ textAlign: 'right', fontWeight: 700 }}>
                        {formatVnd(tutor.availableBalance)}
                      </td>
                      <td style={{ textAlign: 'center', fontWeight: 600, color: '#f59e0b' }}>
                        ★ {tutor.averageRating?.toFixed(1) || '5.0'}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={11} style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                      Không có dữ liệu gia sư nào.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="adm-analytics-table-container">
            <table className="adm-analytics-table">
              <thead>
                <tr>
                  <th>Khách hàng / Phụ huynh</th>
                  <th>Liên hệ</th>
                  <th style={{ textAlign: 'center' }}>Lớp đã đăng ký</th>
                  <th style={{ textAlign: 'center' }}>Lớp đang học</th>
                  <th style={{ textAlign: 'center' }}>Lớp hoàn thành</th>
                  <th style={{ textAlign: 'right' }}>Tổng tiền nạp / cọc</th>
                  <th style={{ textAlign: 'right' }}>Tổng hoàn tiền (Refund)</th>
                  <th style={{ textAlign: 'right' }}>Ký quỹ bảo vệ</th>
                  <th style={{ textAlign: 'right' }}>Số dư ví</th>
                </tr>
              </thead>
              <tbody>
                {individualsLoading ? (
                  <tr>
                    <td colSpan={9} style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                      Đang tải danh sách phụ huynh / học viên...
                    </td>
                  </tr>
                ) : filteredClients.length > 0 ? (
                  filteredClients.map((client) => (
                    <tr key={client.clientId}>
                      <td>
                        <strong>{client.fullName}</strong>
                      </td>
                      <td>
                        <div className="adm-contact-col">
                          <span>{client.email || '—'}</span>
                          <small>{client.phone || '—'}</small>
                        </div>
                      </td>
                      <td style={{ textAlign: 'center', fontWeight: 600 }}>{client.totalClassesRegistered}</td>
                      <td style={{ textAlign: 'center', color: '#0284c7', fontWeight: 600 }}>{client.activeClasses}</td>
                      <td style={{ textAlign: 'center', color: '#16a34a', fontWeight: 600 }}>{client.completedClasses}</td>
                      <td style={{ textAlign: 'right', fontWeight: 600, color: 'var(--color-success)' }}>
                        +{formatVnd(client.totalDeposited)}
                      </td>
                      <td style={{ textAlign: 'right', fontWeight: 600, color: 'var(--color-error)' }}>
                        -{formatVnd(client.totalRefunded)}
                      </td>
                      <td style={{ textAlign: 'right', color: '#ca8a04', fontWeight: 500 }}>
                        {formatVnd(client.activeEscrow)}
                      </td>
                      <td style={{ textAlign: 'right', fontWeight: 700 }}>
                        {formatVnd(client.availableBalance)}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={9} style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                      Không có dữ liệu phụ huynh nào.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    );
  };

  // Render Tab 4: Financial Ledger
  const renderLedger = () => {
    return (
      <div className="adm-tab-pane">
        <div className="adm-ledger-filters">
          <div className="adm-ledger-filter-item">
            <label>Phân loại chiều tiền:</label>
            <select
              value={ledgerDirection}
              onChange={(e) => {
                setLedgerDirection(e.target.value);
                setLedgerPage(0);
              }}
            >
              <option value="ALL">Tất cả chiều tiền (IN + OUT)</option>
              <option value="IN">Tiền vào sàn (IN)</option>
              <option value="OUT">Tiền ra khỏi sàn (OUT)</option>
            </select>
          </div>

          <div className="adm-ledger-filter-item">
            <label>Vai trò người thực hiện:</label>
            <select
              value={ledgerRole}
              onChange={(e) => {
                setLedgerRole(e.target.value);
                setLedgerPage(0);
              }}
            >
              <option value="ALL">Tất cả vai trò</option>
              <option value="TUTOR">Gia sư (Tutor)</option>
              <option value="TUTOR_CENTER">Trung tâm gia sư</option>
              <option value="CLIENT">Khách hàng / Phụ huynh</option>
              <option value="PLATFORM_ADMIN">Quản trị viên (Admin)</option>
              <option value="SYSTEM">Hệ thống tự động</option>
            </select>
          </div>

          <div className="adm-ledger-filter-item" style={{ flexGrow: 1 }}>
            <label>Tìm kiếm nhanh:</label>
            <input
              type="text"
              placeholder="Mã giao dịch, họ tên, email, nội dung..."
              value={ledgerSearch}
              onChange={(e) => {
                setLedgerSearch(e.target.value);
                setLedgerPage(0);
              }}
            />
          </div>

          <div style={{ alignSelf: 'flex-end' }}>
            <button
              className="tcs-btn tcs-btn--outline"
              disabled={Boolean(exportingType)}
              onClick={() => handleExport('ledger')}
            >
              {exportingType === 'ledger' ? 'Đang xuất...' : 'Xuất CSV Sổ cái'}
            </button>
          </div>
        </div>

        <div className="adm-analytics-table-container">
          <table className="adm-analytics-table">
            <thead>
              <tr>
                <th>Mã GD / Tham chiếu</th>
                <th>Loại giao dịch</th>
                <th>Hướng tiền</th>
                <th style={{ textAlign: 'right' }}>Số tiền (VND)</th>
                <th style={{ textAlign: 'center' }}>Trạng thái</th>
                <th>Người thực hiện</th>
                <th>Nội dung chi tiết</th>
                <th>Thời gian</th>
              </tr>
            </thead>
            <tbody>
              {ledgerLoading ? (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                    Đang tra cứu dữ liệu sổ cái thời gian thực...
                  </td>
                </tr>
              ) : ledgerData && ledgerData.content.length > 0 ? (
                ledgerData.content.map((item: FinancialLedgerItemApiResponse) => (
                  <tr key={item.transactionId}>
                    <td>
                      <div className="adm-ref-col">
                        <strong>#{item.transactionId}</strong>
                        <code>{item.referenceCode}</code>
                      </div>
                    </td>
                    <td>
                      <strong>{item.typeLabel}</strong>
                    </td>
                    <td>
                      <span className={`adm-direction-badge ${item.direction === 'IN' ? 'adm-direction-badge--in' : 'adm-direction-badge--out'}`}>
                        {item.direction === 'IN' ? 'VÀO (IN)' : 'RA (OUT)'}
                      </span>
                    </td>
                    <td
                      style={{
                        textAlign: 'right',
                        fontWeight: 700,
                        color: item.direction === 'IN' ? 'var(--color-success)' : 'var(--color-error)',
                      }}
                    >
                      {item.direction === 'IN' ? '+' : '-'}{formatVnd(item.amount)}
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <span className={`tcs-badge ${item.status === 'SUCCESS' ? 'tcs-badge--active' : item.status === 'PENDING' ? 'tcs-badge--suspended' : 'tcs-badge--banned'}`}>
                        {item.status === 'SUCCESS' ? 'Thành công' : item.status === 'PENDING' ? 'Đang chờ' : item.status}
                      </span>
                    </td>
                    <td>
                      <div className="adm-contact-col">
                        <strong>{item.actorName}</strong>
                        <small>{item.actorEmail || item.actorRole}</small>
                      </div>
                    </td>
                    <td>
                      <span className="adm-desc-text" title={item.description || ''}>
                        {item.description || '—'}
                      </span>
                    </td>
                    <td style={{ fontSize: '0.825rem', color: '#475569' }}>
                      {formatDate(item.createdAt)}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={8} style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                    Không có giao dịch nào khớp với điều kiện lọc.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {ledgerData && ledgerData.totalPages > 1 && (
          <div style={{ marginTop: '1.5rem', display: 'flex', justifyContent: 'center' }}>
            <Pagination
              current={ledgerPage + 1}
              totalPages={ledgerData.totalPages}
              onPageChange={(p) => setLedgerPage(p - 1)}
            />
          </div>
        )}
      </div>
    );
  };

  return (
    <AdminLayout
      title="Báo cáo & Phân tích Vận hành"
      subtitle="Bảng phân tích toàn diện người dùng, trung tâm gia sư, cá nhân, dòng tiền và sổ cái giao dịch TCS"
    >
      {/* Universal Time Filter Bar */}
      <AdminTimeFilter
        onChange={handleTimeFilterChange}
        extraControls={
          <div style={{ fontSize: '0.75rem', color: '#64748b' }}>
            {appliedFrom || appliedTo ? (
              <span>
                Khoảng phân tích: <strong>{appliedFrom || 'Đầu'}</strong> → <strong>{appliedTo || 'Hiện tại'}</strong>
              </span>
            ) : (
              <span>Hiển thị: <strong>30 ngày gần nhất</strong></span>
            )}
          </div>
        }
      />

      {/* Main Tab Navigation Bar */}
      <div className="adm-analytics-tabs-header">
        <button
          className={`adm-main-tab ${activeTab === 'overview' ? 'adm-main-tab--active' : ''}`}
          onClick={() => setActiveTab('overview')}
        >
          Tổng quan Sàn (Overview)
        </button>
        <button
          className={`adm-main-tab ${activeTab === 'centers' ? 'adm-main-tab--active' : ''}`}
          onClick={() => setActiveTab('centers')}
        >
          Trung tâm Gia sư (Centers)
        </button>
        <button
          className={`adm-main-tab ${activeTab === 'individuals' ? 'adm-main-tab--active' : ''}`}
          onClick={() => setActiveTab('individuals')}
        >
          Cá nhân (Gia sư & Phụ huynh)
        </button>
        <button
          className={`adm-main-tab ${activeTab === 'ledger' ? 'adm-main-tab--active' : ''}`}
          onClick={() => setActiveTab('ledger')}
        >
          Sổ cái Giao dịch Realtime
        </button>
      </div>

      {/* Overview CSV Export Action Toolbar */}
      {activeTab === 'overview' && (
        <section className="adm-analytics-controls" aria-label="Xuất báo cáo CSV" style={{ marginBottom: '1.5rem' }}>
          <div className="adm-analytics-export-actions">
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
              <span style={{ fontWeight: 600 }}>Xuất file báo cáo CSV Tổng quan (Tối đa 10.000 dòng):</span>
              <span style={{ fontSize: '0.75rem', color: '#64748b' }}>
                Trích xuất số liệu chuẩn xác theo bộ lọc thời gian đang chọn ({appliedFrom || 'Toàn thời gian'} →{' '}
                {appliedTo || 'Hiện tại'}).
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
      )}

      {/* Render Active Tab Pane */}
      {activeTab === 'overview' && renderOverview()}
      {activeTab === 'centers' && renderCenters()}
      {activeTab === 'individuals' && renderIndividuals()}
      {activeTab === 'ledger' && renderLedger()}
    </AdminLayout>
  );
}
