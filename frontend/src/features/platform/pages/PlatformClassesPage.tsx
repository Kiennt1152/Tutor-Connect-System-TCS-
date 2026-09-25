/**
 * ============================================================================
 * [BF-10] QUẢN TRỊ & GIÁM SÁT LỚP HỌC TOÀN NỀN TẢNG (PLATFORM CLASSES PAGE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-09-17
 * 
 * Mô tả Use Case:
 *   - Quản trị viên kiểm duyệt, giám sát toàn bộ các lớp học đăng tuyển của Phụ huynh và Trung tâm gia sư.
 *   - Đảm bảo nội dung yêu cầu lớp học đúng quy chuẩn đạo đức và chính sách vận hành của sàn.
 * 
 * Chức năng chính:
 *   1. Quản lý danh sách lớp học: Lọc theo trạng thái (Đang mở, Đã có gia sư, Đang giảng dạy, Đã đóng, Đã hủy).
 *   2. Phân loại loại hình lớp: Phân biệt lớp học cá nhân và lớp học trực thuộc Trung tâm gia sư liên kết.
 *   3. Giám sát chi tiết lớp: Xem môn học, lịch học, học phí, địa chỉ và thông tin đối tác ký hợp đồng.
 *   4. Đóng/Hủy lớp vi phạm: Can thiệp đình chỉ lớp học khi phát hiện thông tin không chuẩn mực hoặc có tranh chấp.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Quản trị viên truy vấn danh sách lớp học toàn sàn kèm bộ lọc trạng thái và từ khóa.
 *   - Bước 2: Xem xét chi tiết nội dung bài đăng tuyển lớp và các ứng viên gia sư đăng ký.
 *   - Bước 3: Kiểm tra tính hợp lệ của học phí và yêu cầu đào tạo.
 *   - Bước 4: Thực hiện đóng lớp hoặc cảnh báo tài khoản nếu phát hiện dấu hiệu vi phạm quy chế.
 * ============================================================================
 */

import { useEffect, useState, useMemo, useCallback } from 'react';
import { AdminLayout } from '../components/AdminLayout';
import { platformApi } from '../api/platformApi';
import { Pagination } from '../../../shared/components';
import { getApiErrorMessage } from '../../../shared/api/apiError';
import type { ScheduleClass } from '../../center/types/centerTypes';
import './PlatformClassesPage.css';
import '../../center/pages/CenterSchedulePage.css';

export interface ClassItem {
  classId: number;
  title: string;
  description?: string;
  creatorId?: number;
  creatorName?: string;
  subjectName?: string;
  gradeName?: string;
  learningGoal?: string;
  tutorRequirement?: string;
  locationName?: string;
  address?: string;
  lessonMode?: 'ONLINE' | 'OFFLINE';
  numberOfSessions?: number;
  totalSessions?: number;
  completedSessions?: number;
  startDate?: string;
  endDate?: string;
  tuitionFee?: number;
  budget?: number;
  status: 'OPEN' | 'MATCHED' | 'IN_PROGRESS' | 'COMPLETED' | 'CLOSED' | 'CANCELLED';
  classType?: 'PRIVATE' | 'CENTER';
  maxStudents?: number;
  enrolledCount?: number;
}

const STATUS_LABELS: Record<string, string> = {
  OPEN: 'Đang mở (Tìm gia sư)',
  MATCHED: 'Đã ghép gia sư',
  IN_PROGRESS: 'Đang học (Active)',
  COMPLETED: 'Đã kết thúc',
  CLOSED: 'Đã đóng',
  CANCELLED: 'Đã hủy',
};

const LESSON_MODE_LABELS: Record<string, string> = {
  ONLINE: 'Trực tuyến',
  OFFLINE: 'Trực tiếp',
  HYBRID: 'Kết hợp',
};

const ATT_LABELS: Record<string, { label: string; cls: string }> = {
  PRESENT: { label: 'Có mặt', cls: 'present' },
  ABSENT: { label: 'Vắng', cls: 'absent' },
  EXCUSED: { label: 'Có phép', cls: 'excused' },
};

const DAY_OF_WEEK_NAMES: Record<number, string> = {
  1: 'Thứ Hai',
  2: 'Thứ Ba',
  3: 'Thứ Tư',
  4: 'Thứ Năm',
  5: 'Thứ Sáu',
  6: 'Thứ Bảy',
  7: 'Chủ Nhật',
};

function todayStr(): string {
  const d = new Date();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
}

function fmtDate(d: string): string {
  const parts = d.split('-');
  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  }
  return d;
}

function getStatusBadgeClass(status: string) {
  switch (status) {
    case 'IN_PROGRESS':
      return 'tcs-badge tcs-badge--active';
    case 'OPEN':
      return 'tcs-badge tcs-badge--role';
    case 'MATCHED':
      return 'tcs-badge tcs-badge--suspended';
    case 'COMPLETED':
      return 'tcs-badge tcs-badge--completed';
    default:
      return 'tcs-badge';
  }
}

export default function PlatformClassesPage() {
  const [activeTab, setActiveTab] = useState<'list' | 'schedule'>('list');

  // Tab 1: All Classes State
  const [classes, setClasses] = useState<ClassItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Tab 1 Filters
  const [keyword, setKeyword] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [modeFilter, setModeFilter] = useState('');
  const [typeFilter, setTypeFilter] = useState('');

  // Tab 1 Pagination
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Tab 2: Daily Schedule Monitor State (UC-21)
  const [scheduleDate, setScheduleDate] = useState<string>(todayStr());
  const [scheduleClasses, setScheduleClasses] = useState<ScheduleClass[]>([]);
  const [scheduleLoading, setScheduleLoading] = useState(false);
  const [scheduleError, setScheduleError] = useState<string | null>(null);
  const [scheduleKeyword, setScheduleKeyword] = useState('');
  const [expandedSchedule, setExpandedSchedule] = useState<Record<number, boolean>>({});

  // Selected Class for Modal
  const [selectedClass, setSelectedClass] = useState<ClassItem | null>(null);
  const [detailData, setDetailData] = useState<any | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  /**
   * [BF-10, UC-21] Tải danh sách lớp học toàn sàn có áp dụng bộ lọc trạng thái.
   */
  const fetchClasses = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await platformApi.getClasses(statusFilter || undefined);
      setClasses(res.data || []);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể tải danh sách lớp học.'));
    } finally {
      setLoading(false);
    }
  };

  /**
   * [UC-21] Tải lịch học và điểm danh toàn sàn theo ngày được chọn để quản trị viên giám sát.
   */
  const loadSchedule = useCallback(async (targetDate: string) => {
    try {
      setScheduleLoading(true);
      setScheduleError(null);
      const res = await platformApi.getPlatformSchedule(targetDate);
      setScheduleClasses(res.data || []);
      setExpandedSchedule({});
    } catch (err) {
      setScheduleError(getApiErrorMessage(err, 'Không thể tải lịch học toàn sàn theo ngày.'));
    } finally {
      setScheduleLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchClasses();
  }, [statusFilter]);

  useEffect(() => {
    if (activeTab === 'schedule') {
      loadSchedule(scheduleDate);
    }
  }, [activeTab, scheduleDate, loadSchedule]);

  /**
   * [UC-21] Đóng/mở chi tiết lịch học của một lớp học trên giao diện giám sát ngày.
   */
  const toggleSchedule = (classId: number) => {
    setExpandedSchedule((prev) => ({ ...prev, [classId]: !prev[classId] }));
  };

  /**
   * [BF-10] Mở modal xem thông tin chi tiết lớp học kèm hợp đồng và lịch sử điểm danh.
   */
  const handleOpenDetail = async (classId: number, baseItem?: ClassItem) => {
    if (baseItem) {
      setSelectedClass(baseItem);
    } else {
      setSelectedClass({
        classId,
        title: `Lớp học #${classId}`,
        status: 'IN_PROGRESS',
      });
    }
    setDetailLoading(true);
    setDetailData(null);
    try {
      const res = await platformApi.getClassDetail(classId);
      const data = res.data;
      setDetailData(data);
      if (data) {
        setSelectedClass({
          classId: data.classId,
          title: data.title,
          description: data.description,
          creatorId: data.creatorId,
          creatorName: data.creatorName,
          subjectName: data.subjectName,
          gradeName: data.gradeName,
          learningGoal: data.learningGoal,
          tutorRequirement: data.tutorRequirement,
          locationName: data.locationName,
          address: data.address,
          lessonMode: data.lessonMode,
          numberOfSessions: data.numberOfSessions,
          totalSessions: data.totalSessions,
          completedSessions: data.completedSessions,
          startDate: data.startDate,
          endDate: data.endDate,
          tuitionFee: data.tuitionFee,
          budget: data.budget,
          status: data.status,
          classType: data.classType,
          maxStudents: data.maxStudents,
          enrolledCount: data.enrolledCount,
        });
      }
    } catch (err) {
      console.error('Failed to load class detail:', err);
    } finally {
      setDetailLoading(false);
    }
  };

  // Filtered List for Tab 1
  const filteredClasses = useMemo(() => {
    return classes.filter((c) => {
      if (modeFilter && c.lessonMode !== modeFilter) return false;
      if (typeFilter && c.classType !== typeFilter) return false;
      if (keyword.trim()) {
        const query = keyword.trim().toLowerCase();
        const matchTitle = c.title?.toLowerCase().includes(query);
        const matchSubject = c.subjectName?.toLowerCase().includes(query);
        const matchGrade = c.gradeName?.toLowerCase().includes(query);
        const matchCreator = c.creatorName?.toLowerCase().includes(query);
        const matchId = String(c.classId).includes(query);
        if (!matchTitle && !matchSubject && !matchGrade && !matchCreator && !matchId) {
          return false;
        }
      }
      return true;
    });
  }, [classes, modeFilter, typeFilter, keyword]);

  // Tab 1 Pagination Calculations
  const totalElements = filteredClasses.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / pageSize));
  const validPage = Math.min(currentPage, totalPages);
  const startIndex = (validPage - 1) * pageSize;
  const paginatedClasses = filteredClasses.slice(startIndex, startIndex + pageSize);

  // Tab 1 Summary Metrics
  const totalCount = classes.length;
  const inProgressCount = classes.filter((c) => c.status === 'IN_PROGRESS').length;
  const openCount = classes.filter((c) => c.status === 'OPEN').length;
  const completedCount = classes.filter((c) => c.status === 'COMPLETED').length;

  // Filtered Schedule List for Tab 2
  const filteredScheduleClasses = useMemo(() => {
    if (!scheduleKeyword.trim()) return scheduleClasses;
    const q = scheduleKeyword.trim().toLowerCase();
    return scheduleClasses.filter(
      (c) =>
        c.title?.toLowerCase().includes(q) ||
        c.subjectName?.toLowerCase().includes(q) ||
        c.gradeName?.toLowerCase().includes(q) ||
        c.assignedTutorName?.toLowerCase().includes(q) ||
        String(c.classId).includes(q)
    );
  }, [scheduleClasses, scheduleKeyword]);

  return (
    <AdminLayout
      title="Giám sát lớp học"
      subtitle="Theo dõi toàn bộ các lớp học, tiến độ giảng dạy và tình trạng hoạt động trên nền tảng TCS"
    >
      <div className="pc-page">
        {/* Navigation Tabs */}
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          <button
            type="button"
            className={`tcs-btn ${activeTab === 'list' ? 'tcs-btn--primary' : 'tcs-btn--ghost'}`}
            onClick={() => setActiveTab('list')}
          >
            Danh mục tất cả lớp học ({classes.length})
          </button>
          <button
            type="button"
            className={`tcs-btn ${activeTab === 'schedule' ? 'tcs-btn--primary' : 'tcs-btn--ghost'}`}
            onClick={() => setActiveTab('schedule')}
          >
            Giám sát Lịch học theo ngày
          </button>
        </div>

        {/* ========================================================================= */}
        {/* TAB 1: DANH MỤC TẤT CẢ LỚP HỌC                                           */}
        {/* ========================================================================= */}
        {activeTab === 'list' && (
          <>
            {/* KPI Summary Cards */}
            <div className="pc-kpi-grid">
              <div className="pc-kpi-card">
                <span className="pc-kpi-title">Tổng số lớp học</span>
                <span className="pc-kpi-value">{totalCount}</span>
                <span className="pc-kpi-sub">Bao gồm toàn bộ lớp cá nhân và trung tâm</span>
              </div>
              <div className="pc-kpi-card" style={{ borderColor: '#86efac' }}>
                <span className="pc-kpi-title">Đang giảng dạy (Active)</span>
                <span className="pc-kpi-value" style={{ color: '#16a34a' }}>{inProgressCount}</span>
                <span className="pc-kpi-sub">Lớp có hợp đồng & escrow đang diễn ra</span>
              </div>
              <div className="pc-kpi-card" style={{ borderColor: '#93c5fd' }}>
                <span className="pc-kpi-title">Đang tuyển gia sư (Open)</span>
                <span className="pc-kpi-value" style={{ color: '#2563eb' }}>{openCount}</span>
                <span className="pc-kpi-sub">Lớp đang tìm kiếm gia sư phù hợp</span>
              </div>
              <div className="pc-kpi-card" style={{ borderColor: '#d1d5db' }}>
                <span className="pc-kpi-title">Đã hoàn thành</span>
                <span className="pc-kpi-value" style={{ color: '#4b5563' }}>{completedCount}</span>
                <span className="pc-kpi-sub">Đã kết thúc toàn bộ lộ trình học</span>
              </div>
            </div>

            {/* Toolbar & Filters */}
            <section className="adm-card">
              <div className="adm-toolbar" style={{ flexWrap: 'wrap', gap: '10px' }}>
                <input
                  className="adm-field"
                  style={{ minWidth: '240px' }}
                  placeholder="Tìm theo mã lớp, tiêu đề, môn, người đăng..."
                  value={keyword}
                  onChange={(e) => {
                    setKeyword(e.target.value);
                    setCurrentPage(1);
                  }}
                />

                <select
                  className="adm-field"
                  value={statusFilter}
                  onChange={(e) => {
                    setStatusFilter(e.target.value);
                    setCurrentPage(1);
                  }}
                >
                  <option value="">Tất cả trạng thái</option>
                  <option value="IN_PROGRESS">Đang học (Active)</option>
                  <option value="OPEN">Đang mở (Tuyển gia sư)</option>
                  <option value="MATCHED">Đã ghép (Chờ ký HĐ)</option>
                  <option value="COMPLETED">Đã kết thúc</option>
                  <option value="CLOSED">Đã đóng</option>
                  <option value="CANCELLED">Đã hủy</option>
                </select>

                <select
                  className="adm-field"
                  value={modeFilter}
                  onChange={(e) => {
                    setModeFilter(e.target.value);
                    setCurrentPage(1);
                  }}
                >
                  <option value="">Tất cả hình thức</option>
                  <option value="ONLINE">Trực tuyến (Online)</option>
                  <option value="OFFLINE">Tại nhà (Offline)</option>
                </select>

                <select
                  className="adm-field"
                  value={typeFilter}
                  onChange={(e) => {
                    setTypeFilter(e.target.value);
                    setCurrentPage(1);
                  }}
                >
                  <option value="">Tất cả mô hình</option>
                  <option value="PRIVATE">Gia sư 1-1 (Private)</option>
                  <option value="CENTER">Trung tâm (Center)</option>
                </select>

                <button
                  className="tcs-btn tcs-btn--ghost"
                  type="button"
                  onClick={fetchClasses}
                  disabled={loading}
                >
                  {loading ? 'Đang tải...' : 'Làm mới'}
                </button>
              </div>

              {error && <div className="adm-alert adm-alert--error">{error}</div>}

              {/* Classes Table */}
              <div className="adm-table-wrap" style={{ marginTop: '12px' }}>
                <table className="adm-table">
                  <thead>
                    <tr>
                      <th>Mã lớp</th>
                      <th>Tên lớp & Môn học</th>
                      <th>Người đăng</th>
                      <th>Hình thức & Địa điểm</th>
                      <th>Tiến độ buổi dạy</th>
                      <th>Học phí / Ngân sách</th>
                      <th>Trạng thái</th>
                      <th style={{ textAlign: 'center' }}>Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {loading ? (
                      <tr>
                        <td colSpan={8} style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                          Đang tải danh sách lớp học...
                        </td>
                      </tr>
                    ) : paginatedClasses.length === 0 ? (
                      <tr>
                        <td colSpan={8} style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                          Không tìm thấy lớp học nào phù hợp với bộ lọc.
                        </td>
                      </tr>
                    ) : (
                      paginatedClasses.map((item) => {
                        const totalSess = item.totalSessions || item.numberOfSessions || 0;
                        const compSess = item.completedSessions || 0;
                        const progressPct = totalSess > 0 ? Math.min(100, Math.round((compSess / totalSess) * 100)) : 0;

                        return (
                          <tr key={item.classId}>
                            <td><strong>#{item.classId}</strong></td>
                            <td>
                              <div style={{ fontWeight: 600, color: 'var(--color-primary-dark)' }}>
                                {item.title}
                              </div>
                              <div style={{ fontSize: '0.8rem', color: '#64748b' }}>
                                {item.subjectName || 'Chưa phân loại'} • {item.gradeName || 'Mọi cấp'}
                              </div>
                            </td>
                            <td>
                              <div>{item.creatorName || 'Người dùng ẩn danh'}</div>
                              <div style={{ fontSize: '0.75rem', color: '#64748b' }}>
                                {item.classType === 'CENTER' ? 'Trung tâm' : 'Học viên / Phụ huynh'}
                              </div>
                            </td>
                            <td>
                              <div>{item.lessonMode === 'ONLINE' ? 'Trực tuyến (Online)' : 'Tại nhà (Offline)'}</div>
                              <div style={{ fontSize: '0.75rem', color: '#64748b' }}>
                                {item.address || item.locationName || 'Toàn quốc'}
                              </div>
                            </td>
                            <td style={{ minWidth: '130px' }}>
                              <div style={{ fontSize: '0.85rem', fontWeight: 500 }}>
                                {compSess} / {totalSess} buổi ({progressPct}%)
                              </div>
                              <div className="pc-progress-bar">
                                <div className="pc-progress-fill" style={{ width: `${progressPct}%` }} />
                              </div>
                            </td>
                            <td>
                              <div style={{ fontWeight: 600 }}>
                                {item.tuitionFee ? `${item.tuitionFee.toLocaleString('vi-VN')} ₫/b` : item.budget ? `${item.budget.toLocaleString('vi-VN')} ₫` : 'Thỏa thuận'}
                              </div>
                            </td>
                            <td>
                              <span className={getStatusBadgeClass(item.status)}>
                                {STATUS_LABELS[item.status] || item.status}
                              </span>
                            </td>
                            <td style={{ textAlign: 'center' }}>
                              <button
                                className="tcs-btn tcs-btn--secondary"
                                style={{ height: '30px', padding: '0 10px', fontSize: '12px' }}
                                onClick={() => handleOpenDetail(item.classId, item)}
                              >
                                Chi tiết
                              </button>
                            </td>
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              {filteredClasses.length > 0 && (
                <div className="adm-pagination" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: '16px', gap: '8px' }}>
                  <select
                    className="adm-field adm-field--fixed"
                    style={{ width: 'auto', padding: '4px 8px', fontSize: '13px', borderRadius: '8px' }}
                    value={pageSize}
                    onChange={(e) => {
                      setPageSize(Number(e.target.value));
                      setCurrentPage(1);
                    }}
                  >
                    <option value={10}>10 / trang</option>
                    <option value={20}>20 / trang</option>
                    <option value={50}>50 / trang</option>
                  </select>
                  <Pagination
                    current={validPage}
                    totalPages={totalPages}
                    onPageChange={setCurrentPage}
                  />
                </div>
              )}
            </section>
          </>
        )}

        {/* ========================================================================= */}
        {/* TAB 2: GIÁM SÁT LỊCH HỌC THEO NGÀY (UC-21)                                */}
        {/* ========================================================================= */}
        {activeTab === 'schedule' && (
          <>
            {/* Controls Bar */}
            <section className="adm-card" style={{ padding: '16px 20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 600, fontSize: '0.9rem', color: 'var(--color-text-primary)' }}>
                    <span>Chọn ngày theo dõi:</span>
                    <input
                      type="date"
                      className="adm-field"
                      style={{ width: 'auto', padding: '6px 12px' }}
                      value={scheduleDate}
                      onChange={(e) => setScheduleDate(e.target.value)}
                    />
                  </label>
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--ghost"
                    style={{ fontSize: '13px', padding: '6px 12px' }}
                    onClick={() => setScheduleDate(todayStr())}
                  >
                    Hôm nay
                  </button>
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--ghost"
                    style={{ fontSize: '13px', padding: '6px 12px' }}
                    onClick={() => loadSchedule(scheduleDate)}
                    disabled={scheduleLoading}
                  >
                    {scheduleLoading ? 'Đang tải...' : 'Làm mới'}
                  </button>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <input
                    className="adm-field"
                    style={{ minWidth: '260px', padding: '6px 12px' }}
                    placeholder="Tìm theo tên lớp, gia sư, môn học..."
                    value={scheduleKeyword}
                    onChange={(e) => setScheduleKeyword(e.target.value)}
                  />
                </div>
              </div>
            </section>

            {/* Schedule KPI Summary */}
            <div className="pc-kpi-grid">
              <div className="pc-kpi-card">
                <span className="pc-kpi-title">Tổng ca học ngày {fmtDate(scheduleDate)}</span>
                <span className="pc-kpi-value">{scheduleClasses.length}</span>
                <span className="pc-kpi-sub">Bao gồm các lớp cá nhân và trung tâm có lịch</span>
              </div>
              <div className="pc-kpi-card" style={{ borderColor: '#86efac' }}>
                <span className="pc-kpi-title">Đã hoàn thành điểm danh</span>
                <span className="pc-kpi-value" style={{ color: '#16a34a' }}>
                  {scheduleClasses.filter((c) => c.attendanceTaken).length}
                </span>
                <span className="pc-kpi-sub">Gia sư đã xác nhận điểm danh</span>
              </div>
              <div className="pc-kpi-card" style={{ borderColor: '#fde047' }}>
                <span className="pc-kpi-title">Chưa điểm danh</span>
                <span className="pc-kpi-value" style={{ color: '#ca8a04' }}>
                  {scheduleClasses.filter((c) => !c.attendanceTaken).length}
                </span>
                <span className="pc-kpi-sub">Lớp đang học hoặc chưa chốt sĩ số</span>
              </div>
              <div className="pc-kpi-card" style={{ borderColor: '#f9a8d4' }}>
                <span className="pc-kpi-title">Có thay đổi / Đổi lịch</span>
                <span className="pc-kpi-value" style={{ color: '#db2777' }}>
                  {scheduleClasses.filter((c) => c.rescheduled).length}
                </span>
                <span className="pc-kpi-sub">Đã đổi ca dạy hoặc có gia sư dạy thay</span>
              </div>
            </div>

            {/* Schedule List */}
            {scheduleError && <div className="adm-alert adm-alert--error">{scheduleError}</div>}

            {scheduleLoading ? (
              <div className="adm-card" style={{ textAlign: 'center', padding: '3rem', color: '#64748b' }}>
                Đang tải dữ liệu lịch học toàn sàn ngày {fmtDate(scheduleDate)}...
              </div>
            ) : filteredScheduleClasses.length === 0 ? (
              <div className="cs-empty adm-card" style={{ padding: '3rem', textAlign: 'center' }}>
                <p style={{ fontWeight: 600, color: 'var(--color-text-primary)' }}>
                  Không có ca học nào vào ngày {fmtDate(scheduleDate)}.
                </p>
                <p style={{ fontSize: '0.85rem', color: '#64748b' }}>
                  {scheduleKeyword ? 'Không tìm thấy lớp học phù hợp với từ khóa.' : 'Tất cả các lớp đều không có lịch diễn ra vào ngày này.'}
                </p>
              </div>
            ) : (
              <div className="cs-list">
                {filteredScheduleClasses.map((c) => {
                  const isOpen = !!expandedSchedule[c.classId];
                  const presentCount = c.students?.filter((s) => s.status === 'PRESENT').length || 0;
                  const totalStudents = c.students?.length || 0;

                  return (
                    <article className={`cs-card${isOpen ? ' is-open' : ''}`} key={c.classId}>
                      <div className="cs-card__head">
                        <div className="cs-card__headmain">
                          <h2 className="cs-card__title" style={{ fontSize: '1.05rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <span>{c.title}</span>
                            <span style={{ fontSize: '0.8rem', color: '#64748b', fontWeight: 'normal' }}>#{c.classId}</span>
                          </h2>
                          <div className="cs-chips">
                            {c.subjectName && <span className="cs-chip">{c.subjectName}</span>}
                            {c.gradeName && <span className="cs-chip">{c.gradeName}</span>}
                            {c.lessonMode && (
                              <span className="cs-chip">
                                {LESSON_MODE_LABELS[c.lessonMode] || c.lessonMode}
                              </span>
                            )}
                            {c.rescheduled && (
                              <span className="cs-chip cs-chip--resched">{c.rescheduleNote || 'Có đổi lịch / dạy thay'}</span>
                            )}
                          </div>
                        </div>
                        <div className="cs-times">
                          {c.slots?.map((s) => (
                            <span className="cs-time" key={s.slotId}>
                              {s.startTime?.slice(0, 5)}–{s.endTime?.slice(0, 5)}
                            </span>
                          ))}
                        </div>
                      </div>

                      <div className="cs-meta">
                        <div className="cs-meta__item">
                          <span className="cs-meta__label">Gia sư phụ trách</span>
                          {c.assignedTutorName ? (
                            <span className="cs-meta__value" style={{ color: '#1e40af', fontWeight: 600 }}>
                              {c.assignedTutorName}
                            </span>
                          ) : (
                            <span className="cs-meta__value cs-meta__value--none">Chưa gán gia sư</span>
                          )}
                        </div>
                        <div className="cs-meta__item">
                          <span className="cs-meta__label">Trạng thái điểm danh</span>
                          <span className={`cs-attstate ${c.attendanceTaken ? 'cs-attstate--done' : 'cs-attstate--pending'}`}>
                            {c.attendanceTaken ? '✓ Đã điểm danh' : 'Chưa điểm danh'}
                          </span>
                        </div>
                        <div className="cs-meta__item">
                          <span className="cs-meta__label">Sĩ số lớp</span>
                          <span className="cs-meta__value">{c.studentCount} học sinh</span>
                        </div>
                        <div className="cs-meta__item" style={{ marginLeft: 'auto' }}>
                          <button
                            type="button"
                            className="tcs-btn tcs-btn--secondary"
                            style={{ height: '30px', padding: '0 12px', fontSize: '12px' }}
                            onClick={() => handleOpenDetail(c.classId)}
                          >
                            Xem hồ sơ lớp
                          </button>
                        </div>
                      </div>

                      <button
                        type="button"
                        className="cs-toggle"
                        onClick={() => toggleSchedule(c.classId)}
                        aria-expanded={isOpen}
                      >
                        <span>
                          Danh sách học sinh & điểm danh
                          {c.attendanceTaken && totalStudents > 0 && (
                            <span className="cs-toggle__count">
                              {presentCount}/{totalStudents} có mặt
                            </span>
                          )}
                        </span>
                        <span className={`cs-toggle__chev${isOpen ? ' is-open' : ''}`}>▾</span>
                      </button>

                      {isOpen && (
                        <div className="cs-students">
                          {!c.students || c.students.length === 0 ? (
                            <p className="cs-muted">Chưa có thông tin danh sách học sinh cho buổi học này.</p>
                          ) : (
                            <ul className="cs-roster">
                              {c.students.map((st) => {
                                const att = st.status ? ATT_LABELS[st.status] : null;
                                return (
                                  <li className="cs-student" key={st.classStudentId}>
                                    <div className="cs-student__info">
                                      <span className="cs-student__name">{st.studentName}</span>
                                      {st.studentPhone && (
                                        <span className="cs-student__phone">ĐT: {st.studentPhone}</span>
                                      )}
                                    </div>
                                    {att ? (
                                      <span className={`cs-attbadge cs-attbadge--${att.cls}`}>
                                        {att.label}
                                      </span>
                                    ) : (
                                      <span className="cs-attbadge cs-attbadge--none">
                                        Chưa điểm danh
                                      </span>
                                    )}
                                  </li>
                                );
                              })}
                            </ul>
                          )}
                        </div>
                      )}
                    </article>
                  );
                })}
              </div>
            )}
          </>
        )}
      </div>

      {/* Class Detail Modal (Enhanced with UC-21 Tutor & Weekly Slots) */}
      {selectedClass && (
        <div className="pc-modal-backdrop" onClick={() => setSelectedClass(null)}>
          <div className="pc-modal" onClick={(e) => e.stopPropagation()}>
            <div className="pc-modal-header">
              <div className="pc-modal-title">
                <span>Chi tiết lớp học #{selectedClass.classId}</span>
                <span className={getStatusBadgeClass(selectedClass.status)}>
                  {STATUS_LABELS[selectedClass.status] || selectedClass.status}
                </span>
              </div>
              <button className="pc-modal-close" onClick={() => setSelectedClass(null)}>
                &times;
              </button>
            </div>

            <div className="pc-modal-body">
              <div className="pc-modal-field pc-modal-field--full">
                <span className="pc-modal-label">Tiêu đề lớp học</span>
                <span className="pc-modal-val" style={{ fontSize: '1.1rem', color: 'var(--color-primary-dark)' }}>
                  {selectedClass.title}
                </span>
              </div>

              {/* Thông tin Gia sư phụ trách & Lịch học trong tuần (UC-21) */}
              <div className="pc-modal-field pc-modal-field--full" style={{ background: '#f8fafc', padding: '12px', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                <span className="pc-modal-label">Gia sư phụ trách (Active Tutor)</span>
                <span className="pc-modal-val" style={{ color: '#1e40af', fontWeight: 600, fontSize: '0.95rem' }}>
                  {detailLoading ? (
                    'Đang tải thông tin gia sư...'
                  ) : detailData?.assignedTutorName ? (
                    `${detailData.assignedTutorName} (ID: #${detailData.assignedTutorId})`
                  ) : (
                    'Chưa có gia sư chính thức'
                  )}
                </span>
              </div>

              <div className="pc-modal-field pc-modal-field--full" style={{ background: '#f8fafc', padding: '12px', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                <span className="pc-modal-label">Khung giờ học trong tuần (Weekly Slots)</span>
                {detailLoading ? (
                  <span className="pc-modal-val" style={{ color: '#64748b' }}>Đang tải lịch học...</span>
                ) : detailData?.schedule && detailData.schedule.length > 0 ? (
                  <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginTop: '6px' }}>
                    {detailData.schedule.map((slot: any) => (
                      <span
                        key={slot.slotId}
                        style={{
                          background: '#ffffff',
                          border: '1px solid #cbd5e1',
                          padding: '4px 10px',
                          borderRadius: '6px',
                          fontSize: '0.85rem',
                          fontWeight: 500,
                          color: '#334155',
                        }}
                      >
                        {DAY_OF_WEEK_NAMES[slot.dayOfWeek] || `Thứ ${slot.dayOfWeek}`}: {slot.startTime?.slice(0, 5)} - {slot.endTime?.slice(0, 5)}
                      </span>
                    ))}
                  </div>
                ) : (
                  <span className="pc-modal-val" style={{ color: '#64748b', fontSize: '0.85rem' }}>
                    Chưa thiết lập khung giờ cố định trong tuần.
                  </span>
                )}
              </div>

              <div className="pc-modal-grid">
                <div className="pc-modal-field">
                  <span className="pc-modal-label">Môn học</span>
                  <span className="pc-modal-val">{selectedClass.subjectName || 'Chưa phân loại'}</span>
                </div>
                <div className="pc-modal-field">
                  <span className="pc-modal-label">Cấp lớp</span>
                  <span className="pc-modal-val">{selectedClass.gradeName || 'Mọi cấp'}</span>
                </div>
                <div className="pc-modal-field">
                  <span className="pc-modal-label">Hình thức dạy học</span>
                  <span className="pc-modal-val">
                    {selectedClass.lessonMode === 'ONLINE' ? 'Trực tuyến (Online)' : 'Tại nhà (Offline)'}
                  </span>
                </div>
                <div className="pc-modal-field">
                  <span className="pc-modal-label">Địa chỉ học tập</span>
                  <span className="pc-modal-val">{selectedClass.address || selectedClass.locationName || 'Trực tuyến'}</span>
                </div>
                <div className="pc-modal-field">
                  <span className="pc-modal-label">Học phí / Buổi</span>
                  <span className="pc-modal-val" style={{ color: '#16a34a', fontWeight: 600 }}>
                    {selectedClass.tuitionFee ? `${selectedClass.tuitionFee.toLocaleString('vi-VN')} ₫` : 'Thỏa thuận'}
                  </span>
                </div>
                <div className="pc-modal-field">
                  <span className="pc-modal-label">Tổng ngân sách dự kiến</span>
                  <span className="pc-modal-val" style={{ fontWeight: 600 }}>
                    {selectedClass.budget ? `${selectedClass.budget.toLocaleString('vi-VN')} ₫` : 'Chưa thiết lập'}
                  </span>
                </div>
                <div className="pc-modal-field">
                  <span className="pc-modal-label">Người đăng</span>
                  <span className="pc-modal-val">{selectedClass.creatorName || 'Người dùng ẩn danh'}</span>
                </div>
                <div className="pc-modal-field">
                  <span className="pc-modal-label">Loại hình tổ chức</span>
                  <span className="pc-modal-val">
                    {selectedClass.classType === 'CENTER' ? 'Lớp của Trung tâm' : 'Lớp cá nhân (Gia sư 1-1)'}
                  </span>
                </div>
                <div className="pc-modal-field">
                  <span className="pc-modal-label">Thời gian bắt đầu</span>
                  <span className="pc-modal-val">{selectedClass.startDate || 'Chưa định ngày'}</span>
                </div>
                <div className="pc-modal-field">
                  <span className="pc-modal-label">Thời gian kết thúc</span>
                  <span className="pc-modal-val">{selectedClass.endDate || 'Chưa định ngày'}</span>
                </div>
                <div className="pc-modal-field">
                  <span className="pc-modal-label">Tiến độ buổi dạy</span>
                  <span className="pc-modal-val">
                    {selectedClass.completedSessions || 0} / {selectedClass.totalSessions || selectedClass.numberOfSessions || 0} buổi hoàn tất
                  </span>
                </div>
                <div className="pc-modal-field">
                  <span className="pc-modal-label">Học viên ghi danh</span>
                  <span className="pc-modal-val">
                    {selectedClass.enrolledCount || 0} {selectedClass.maxStudents ? `/ ${selectedClass.maxStudents}` : ''} học viên
                  </span>
                </div>
              </div>

              {selectedClass.learningGoal && (
                <div className="pc-modal-field pc-modal-field--full">
                  <span className="pc-modal-label">Mục tiêu học tập</span>
                  <span className="pc-modal-val">{selectedClass.learningGoal}</span>
                </div>
              )}

              {selectedClass.tutorRequirement && (
                <div className="pc-modal-field pc-modal-field--full">
                  <span className="pc-modal-label">Yêu cầu gia sư</span>
                  <span className="pc-modal-val">{selectedClass.tutorRequirement}</span>
                </div>
              )}

              {selectedClass.description && (
                <div className="pc-modal-field pc-modal-field--full">
                  <span className="pc-modal-label">Mô tả bổ sung</span>
                  <span className="pc-modal-val" style={{ whiteSpace: 'pre-line' }}>{selectedClass.description}</span>
                </div>
              )}
            </div>

            <div className="pc-modal-footer">
              <button
                className="tcs-btn tcs-btn--secondary"
                onClick={() => setSelectedClass(null)}
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}
