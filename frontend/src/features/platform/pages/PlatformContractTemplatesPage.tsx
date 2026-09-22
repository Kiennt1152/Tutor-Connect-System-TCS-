/**
 * ============================================================================
 * [BF-10] QUẢN TRỊ MẪU HỢP ĐỒNG GIẢNG DẠY SÀN (CONTRACT TEMPLATES PAGE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp (Hoàng Minh Đức - HE187354)
 * Ngày tạo: 2026-09-17
 * 
 * Mô tả Use Case:
 *   - Thiết lập và quản lý các điều khoản mẫu cho hợp đồng giảng dạy cá nhân và hợp đồng lớp trung tâm.
 *   - Đảm bảo tính pháp lý chuẩn hóa, tỷ lệ đền bù và bảo vệ quyền lợi hai bên trong giao dịch gia sư.
 * 
 * Chức năng chính:
 *   1. Quản lý danh sách mẫu hợp đồng: Phân loại theo loại hình (Lớp cá nhân 1-1, Lớp liên kết Trung tâm).
 *   2. Soạn thảo điều khoản: Tùy biến nội dung điều khoản cam kết, thời hạn thanh toán và tỷ lệ hoàn tiền.
 *   3. Quản lý biến động (Placeholders): Sử dụng các thẻ thế chỗ tự động (Tên gia sư, Học phí, Lịch học, Mã hợp đồng).
 *   4. Xem trước bản in: Hiển thị mẫu hợp đồng hoàn chỉnh trước khi ban hành áp dụng chính thức.
 * 
 * Luồng xử lý chính:
 *   - Bước 1: Quản trị viên tải danh sách mẫu hợp đồng đang được áp dụng trên sàn.
 *   - Bước 2: Chọn mẫu cần điều chỉnh nội dung điều khoản pháp lý hoặc quyền nghĩa vụ.
 *   - Bước 3: Xem trước bản mẫu với dữ liệu giả lập để kiểm tra định dạng trình bày.
 *   - Bước 4: Lưu mẫu hợp đồng mới, hệ thống kích hoạt áp dụng cho các hợp đồng phát sinh tiếp theo.
 * ============================================================================
 */

import React, { useEffect, useState } from 'react';
import { AdminLayout } from '../components/AdminLayout';
import { platformApi } from '../api/platformApi';
import { getApiErrorMessage } from '../../../shared/api/apiError';

export interface ContractTemplateItem {
  templateId: number;
  name: string;
  content: string;
  contractType: string;
  defaultTemplate: boolean;
  status: string;
  system: boolean;
}

const TYPE_LABEL: Record<string, string> = {
  PRIVATE_TUTORING: 'Hợp đồng dạy kèm 1:1 cá nhân',
  CENTER_CLASS: 'Hợp đồng dạy lớp trung tâm',
  RECRUITMENT: 'Thỏa thuận hợp tác tuyển dụng',
  SPECIALIZED_GUARANTEE: 'Hợp đồng cam kết đầu ra / luyện thi',
  CLASS: 'Hợp đồng dạy lớp / học viên',
};

const TYPE_BADGE_STYLE: Record<string, { bg: string; color: string; border: string }> = {
  PRIVATE_TUTORING: { bg: '#e0f2fe', color: '#0369a1', border: '#bae6fd' },
  CENTER_CLASS: { bg: '#dcfce7', color: '#15803d', border: '#bbf7d0' },
  RECRUITMENT: { bg: '#fef3c7', color: '#b45309', border: '#fde68a' },
  SPECIALIZED_GUARANTEE: { bg: '#f3e8ff', color: '#7e22ce', border: '#e9d5ff' },
  CLASS: { bg: '#f1f5f9', color: '#475569', border: '#e2e8f0' },
};

const DEFAULT_SAMPLE_CONTENT = `CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM
Độc lập - Tự do - Hạnh phúc
-----------------------------------

HỢP ĐỒNG DỊCH VỤ GIẢNG DẠY & KÝ QUỸ ĐẢM BẢO

Hôm nay, ngày {{NGAY_KY}}, tại nền tảng Tutor Connect System (TCS), hai bên gồm:

BÊN A (BÊN THUÊ / PHỤ HUYNH / TRUNG TÂM):
- Đại diện: {{TEN_BEN_A}}
- Điện thoại: {{SDT_BEN_A}}
- Địa chỉ: {{DIA_CHI_BEN_A}}

BÊN B (BÊN GIẢNG DẠY / GIA SƯ):
- Họ và tên: {{TEN_BEN_B}}
- Số CCCD: {{CCCD_BEN_B}}
- Điện thoại: {{SDT_BEN_B}}

Hai bên tự nguyện thỏa thuận các điều khoản sau:
1. ĐIỀU 1: NỘI DUNG GIẢNG DẠY
Bên B nhận giảng dạy cho lớp: {{TEN_LOP}}
Hình thức giảng dạy: {{HINH_THUC}} tại {{DIA_CHI_HOC}}
Số lượng buổi học: {{SO_BUOI}} buổi.

2. ĐIỀU 2: HỌC PHÍ VÀ KÝ QUỸ ESCROW
- Học phí thỏa thuận: {{HOC_PHI}} VNĐ / khóa học.
- Học phí được nạp vào tài khoản ký quỹ (Escrow) của TCS trước khi bắt đầu buổi học đầu tiên.
- Tiền sẽ được giải ngân cho Bên B sau khi hoàn tất toàn bộ các buổi học và được Bên A xác nhận nghiệm thu.

3. ĐIỀU 3: BẢO MẬT & CHỐNG THOÁT SÀN
Hai bên cam kết không né tránh giao dịch ngoài nền tảng hoặc thanh toán trực tiếp để trốn tránh phí dịch vụ 2% của TCS. Mọi tranh chấp phát sinh sẽ được Quản trị viên TCS phân xử theo Điều khoản sử dụng.

ĐẠI DIỆN BÊN A                                   ĐẠI DIỆN BÊN B
(Ký bằng chữ ký điện tử TCS)                     (Ký bằng chữ ký điện tử TCS)`;

export default function PlatformContractTemplatesPage() {
  const [templates, setTemplates] = useState<ContractTemplateItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filter
  const [typeFilter, setTypeFilter] = useState('');
  const [sourceFilter, setSourceFilter] = useState('');

  // Modal State
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<ContractTemplateItem | null>(null);
  const [formName, setFormName] = useState('');
  const [formType, setFormType] = useState<string>('CENTER_CLASS');
  const [formContent, setFormContent] = useState('');
  const [saving, setSaving] = useState(false);

  // Preview Modal State
  const [previewTemplate, setPreviewTemplate] = useState<ContractTemplateItem | null>(null);

  const fetchTemplates = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await platformApi.getContractTemplates();
      setTemplates(res.data || []);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Không thể tải danh sách mẫu hợp đồng.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTemplates();
  }, []);

  const handleOpenCreate = () => {
    setEditingTemplate(null);
    setFormName('');
    setFormType('CENTER_CLASS');
    setFormContent(DEFAULT_SAMPLE_CONTENT);
    setIsEditOpen(true);
  };

  const handleOpenEdit = (item: ContractTemplateItem) => {
    setEditingTemplate(item);
    setFormName(item.name);
    setFormType(item.contractType || 'CENTER_CLASS');
    setFormContent(item.content);
    setIsEditOpen(true);
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formName.trim() || !formContent.trim()) {
      alert('Vui lòng nhập đầy đủ tên mẫu và nội dung hợp đồng.');
      return;
    }
    try {
      setSaving(true);
      const payload = {
        name: formName.trim(),
        content: formContent.trim(),
        contractType: formType,
      };
      if (editingTemplate) {
        await platformApi.updateContractTemplate(editingTemplate.templateId, payload);
      } else {
        await platformApi.createContractTemplate(payload);
      }
      setIsEditOpen(false);
      await fetchTemplates();
    } catch (err) {
      alert('Lỗi lưu mẫu hợp đồng: ' + getApiErrorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (templateId: number) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa / lưu trữ mẫu hợp đồng này?')) return;
    try {
      await platformApi.deleteContractTemplate(templateId);
      await fetchTemplates();
    } catch (err) {
      alert('Lỗi xóa mẫu hợp đồng: ' + getApiErrorMessage(err));
    }
  };

  // Filter list
  const filteredTemplates = templates.filter((t) => {
    if (typeFilter && t.contractType !== typeFilter) return false;
    if (sourceFilter === 'SYSTEM' && !t.system) return false;
    if (sourceFilter === 'CENTER' && t.system) return false;
    return true;
  });

  const totalCount = templates.length;
  const systemCount = templates.filter((t) => t.system).length;
  const teachingCount = templates.filter(
    (t) => t.contractType === 'CENTER_CLASS' || t.contractType === 'PRIVATE_TUTORING' || t.contractType === 'CLASS'
  ).length;
  const recCount = templates.filter((t) => t.contractType === 'RECRUITMENT').length;
  const guaranteeCount = templates.filter((t) => t.contractType === 'SPECIALIZED_GUARANTEE').length;

  return (
    <AdminLayout
      title="Mẫu hợp đồng điện tử"
      subtitle="Quản lý các mẫu hợp đồng chuẩn hệ thống TCS và theo dõi mẫu hợp đồng do các trung tâm ban hành (UC-45)"
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
        {/* KPI Cards */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
            gap: '12px',
          }}
        >
          <div className="adm-card" style={{ padding: '16px' }}>
            <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Tổng số mẫu hợp đồng</span>
            <div style={{ fontSize: '1.75rem', fontWeight: 700, marginTop: '4px' }}>{totalCount}</div>
            <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Hệ thống & Trung tâm</span>
          </div>
          <div className="adm-card" style={{ padding: '16px', borderLeft: '4px solid #2563eb' }}>
            <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Mẫu chuẩn hệ thống</span>
            <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#2563eb', marginTop: '4px' }}>
              {systemCount}
            </div>
            <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Do sàn TCS ban hành</span>
          </div>
          <div className="adm-card" style={{ padding: '16px', borderLeft: '4px solid #16a34a' }}>
            <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Hợp đồng dạy học / lớp</span>
            <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#16a34a', marginTop: '4px' }}>
              {teachingCount}
            </div>
            <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Lớp trung tâm & kèm 1:1</span>
          </div>
          <div className="adm-card" style={{ padding: '16px', borderLeft: '4px solid #d97706' }}>
            <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Hợp tác tuyển dụng</span>
            <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#d97706', marginTop: '4px' }}>
              {recCount}
            </div>
            <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Trung tâm & Gia sư</span>
          </div>
          <div className="adm-card" style={{ padding: '16px', borderLeft: '4px solid #7c3aed' }}>
            <span style={{ fontSize: '0.8rem', color: '#64748b' }}>Cam kết đầu ra</span>
            <div style={{ fontSize: '1.75rem', fontWeight: 700, color: '#7c3aed', marginTop: '4px' }}>
              {guaranteeCount}
            </div>
            <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Luyện thi chứng chỉ</span>
          </div>
        </div>

        {/* Toolbar & Filter */}
        <section className="adm-card">
          <div
            className="adm-toolbar"
            style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: '10px' }}
          >
            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
              <select
                className="adm-field"
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value)}
              >
                <option value="">Tất cả phân loại</option>
                <option value="CENTER_CLASS">Hợp đồng dạy lớp trung tâm</option>
                <option value="PRIVATE_TUTORING">Hợp đồng dạy kèm 1:1 cá nhân</option>
                <option value="RECRUITMENT">Thỏa thuận hợp tác tuyển dụng</option>
                <option value="SPECIALIZED_GUARANTEE">Hợp đồng cam kết đầu ra / luyện thi</option>
              </select>

              <select
                className="adm-field"
                value={sourceFilter}
                onChange={(e) => setSourceFilter(e.target.value)}
              >
                <option value="">Tất cả nguồn gốc</option>
                <option value="SYSTEM">Mẫu chuẩn hệ thống TCS</option>
                <option value="CENTER">Mẫu do trung tâm tạo</option>
              </select>

              <button
                className="tcs-btn tcs-btn--ghost"
                type="button"
                onClick={fetchTemplates}
                disabled={loading}
              >
                {loading ? 'Đang tải...' : 'Làm mới'}
              </button>
            </div>

            <button
              className="tcs-btn tcs-btn--primary"
              type="button"
              onClick={handleOpenCreate}
            >
              + Thêm mẫu hợp đồng chuẩn
            </button>
          </div>

          {error && <div className="adm-alert adm-alert--error">{error}</div>}

          {/* Table */}
          <div className="adm-table-wrap" style={{ marginTop: '12px' }}>
            <table className="adm-table">
              <thead>
                <tr>
                  <th>Mã mẫu</th>
                  <th>Tên mẫu hợp đồng</th>
                  <th>Phân loại</th>
                  <th>Nguồn gốc</th>
                  <th>Trạng thái</th>
                  <th style={{ textAlign: 'center' }}>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={6} style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                      Đang tải danh sách mẫu hợp đồng...
                    </td>
                  </tr>
                ) : filteredTemplates.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ textAlign: 'center', padding: '2rem', color: '#64748b' }}>
                      Chưa có mẫu hợp đồng nào phù hợp.
                    </td>
                  </tr>
                ) : (
                  filteredTemplates.map((tpl) => (
                    <tr key={tpl.templateId}>
                      <td><strong>#{tpl.templateId}</strong></td>
                      <td>
                        <div style={{ fontWeight: 600, color: 'var(--color-primary-dark)' }}>
                          {tpl.name}
                        </div>
                        {tpl.defaultTemplate && (
                          <div style={{ fontSize: '0.75rem', color: '#16a34a', fontWeight: 600, marginTop: '2px' }}>
                            ★ Mẫu áp dụng mặc định ({TYPE_LABEL[tpl.contractType] || 'Toàn sàn'})
                          </div>
                        )}
                      </td>
                      <td>
                        <span
                          className="tcs-badge"
                          style={{
                            backgroundColor: TYPE_BADGE_STYLE[tpl.contractType]?.bg || '#f1f5f9',
                            color: TYPE_BADGE_STYLE[tpl.contractType]?.color || '#475569',
                            borderColor: TYPE_BADGE_STYLE[tpl.contractType]?.border || '#cbd5e1',
                            fontWeight: 600,
                          }}
                        >
                          {TYPE_LABEL[tpl.contractType] || tpl.contractType || 'Hợp đồng dạy lớp'}
                        </span>
                      </td>
                      <td>
                        <span className={tpl.system ? 'tcs-badge tcs-badge--active' : 'tcs-badge'}>
                          {tpl.system ? 'Hệ thống TCS' : 'Trung tâm'}
                        </span>
                      </td>
                      <td>
                        <span className="tcs-badge tcs-badge--active">
                          {tpl.status || 'ACTIVE'}
                        </span>
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        <div style={{ display: 'inline-flex', gap: '6px' }}>
                          <button
                            className="tcs-btn tcs-btn--secondary"
                            style={{ height: '30px', padding: '0 8px', fontSize: '12px' }}
                            onClick={() => setPreviewTemplate(tpl)}
                          >
                            Xem trước
                          </button>
                          <button
                            className="tcs-btn tcs-btn--secondary"
                            style={{ height: '30px', padding: '0 8px', fontSize: '12px' }}
                            onClick={() => handleOpenEdit(tpl)}
                          >
                            Sửa
                          </button>
                          <button
                            className="tcs-btn tcs-btn--ghost"
                            style={{ height: '30px', padding: '0 8px', fontSize: '12px', color: '#dc2626' }}
                            onClick={() => handleDelete(tpl.templateId)}
                          >
                            Xóa
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </section>
      </div>

      {/* Edit / Create Modal */}
      {isEditOpen && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.45)',
            backdropFilter: 'blur(2px)',
            zIndex: 999,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '1rem',
          }}
          onClick={() => !saving && setIsEditOpen(false)}
        >
          <div
            style={{
              background: '#fff',
              borderRadius: '12px',
              maxWidth: '750px',
              width: '100%',
              maxHeight: '92vh',
              overflowY: 'auto',
              boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)',
              display: 'flex',
              flexDirection: 'column',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <form onSubmit={handleSave}>
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '1.25rem 1.5rem',
                  borderBottom: '1px solid #e2e8f0',
                }}
              >
                <span style={{ fontSize: '1.2rem', fontWeight: 700 }}>
                  {editingTemplate ? `Chỉnh sửa mẫu hợp đồng #${editingTemplate.templateId}` : 'Thêm mẫu hợp đồng chuẩn mới'}
                </span>
                <button
                  type="button"
                  style={{ background: 'none', border: 'none', fontSize: '1.5rem', cursor: 'pointer' }}
                  onClick={() => !saving && setIsEditOpen(false)}
                >
                  &times;
                </button>
              </div>

              <div style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                    Tên mẫu hợp đồng *
                  </label>
                  <input
                    className="adm-field"
                    style={{ width: '100%' }}
                    placeholder="VD: Hợp đồng giảng dạy kèm 1-1 chuẩn hệ thống TCS 2026"
                    value={formName}
                    onChange={(e) => setFormName(e.target.value)}
                    required
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                    Phân loại hợp đồng
                  </label>
                  <select
                    className="adm-field"
                    style={{ width: '100%' }}
                    value={formType}
                    onChange={(e) => setFormType(e.target.value)}
                  >
                    <option value="CENTER_CLASS">Hợp đồng dạy lớp trung tâm (CENTER_CLASS)</option>
                    <option value="PRIVATE_TUTORING">Hợp đồng dạy kèm 1:1 cá nhân (PRIVATE_TUTORING)</option>
                    <option value="RECRUITMENT">Thỏa thuận hợp tác tuyển dụng (RECRUITMENT)</option>
                    <option value="SPECIALIZED_GUARANTEE">Hợp đồng cam kết đầu ra / luyện thi (SPECIALIZED_GUARANTEE)</option>
                    <option value="CLASS">Khác / Hợp đồng dạy lớp chung (CLASS)</option>
                  </select>
                </div>

                <div style={{ background: '#f8fafc', border: '1px solid #e2e8f0', borderRadius: '8px', padding: '10px 14px' }}>
                  <span style={{ fontSize: '0.8rem', fontWeight: 600, color: '#334155' }}>Các biến giữ chỗ (Placeholder tags):</span>
                  <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '4px', display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                    <code>{'{{TEN_BEN_A}}'}</code>
                    <code>{'{{TEN_BEN_B}}'}</code>
                    <code>{'{{TEN_LOP}}'}</code>
                    <code>{'{{HOC_PHI}}'}</code>
                    <code>{'{{SO_BUOI}}'}</code>
                    <code>{'{{DIA_CHI}}'}</code>
                    <code>{'{{NGAY_KY}}'}</code>
                  </div>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '4px' }}>
                    Nội dung hợp đồng *
                  </label>
                  <textarea
                    className="adm-field"
                    style={{ width: '100%', height: '320px', fontFamily: 'monospace', fontSize: '0.85rem', lineHeight: '1.4' }}
                    value={formContent}
                    onChange={(e) => setFormContent(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div
                style={{
                  padding: '1rem 1.5rem',
                  borderTop: '1px solid #e2e8f0',
                  display: 'flex',
                  justifyContent: 'flex-end',
                  gap: '8px',
                }}
              >
                <button
                  type="button"
                  className="tcs-btn tcs-btn--secondary"
                  disabled={saving}
                  onClick={() => setIsEditOpen(false)}
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  className="tcs-btn tcs-btn--primary"
                  disabled={saving}
                >
                  {saving ? 'Đang lưu...' : 'Lưu mẫu hợp đồng'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Preview Modal */}
      {previewTemplate && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.45)',
            backdropFilter: 'blur(2px)',
            zIndex: 999,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '1rem',
          }}
          onClick={() => setPreviewTemplate(null)}
        >
          <div
            style={{
              background: '#fff',
              borderRadius: '12px',
              maxWidth: '800px',
              width: '100%',
              maxHeight: '92vh',
              overflowY: 'auto',
              boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1)',
              display: 'flex',
              flexDirection: 'column',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '1.25rem 1.5rem',
                borderBottom: '1px solid #e2e8f0',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ fontSize: '1.2rem', fontWeight: 700 }}>Xem trước: {previewTemplate.name}</span>
                <span className="tcs-badge tcs-badge--role">
                  {TYPE_LABEL[previewTemplate.contractType] || previewTemplate.contractType}
                </span>
              </div>
              <button
                style={{ background: 'none', border: 'none', fontSize: '1.5rem', cursor: 'pointer' }}
                onClick={() => setPreviewTemplate(null)}
              >
                &times;
              </button>
            </div>

            <div style={{ padding: '1.5rem', background: '#f8fafc' }}>
              <div
                style={{
                  background: '#fff',
                  border: '1px solid #cbd5e1',
                  borderRadius: '6px',
                  padding: '2rem',
                  fontFamily: 'Times New Roman, serif',
                  fontSize: '0.95rem',
                  lineHeight: '1.6',
                  whiteSpace: 'pre-line',
                  boxShadow: '0 1px 3px rgba(0,0,0,0.05)',
                }}
              >
                {previewTemplate.content}
              </div>
            </div>

            <div
              style={{
                padding: '1rem 1.5rem',
                borderTop: '1px solid #e2e8f0',
                display: 'flex',
                justifyContent: 'flex-end',
              }}
            >
              <button
                className="tcs-btn tcs-btn--secondary"
                onClick={() => setPreviewTemplate(null)}
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
