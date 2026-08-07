import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { SiteFooter } from '../../home/components/SiteFooter';
import { centerApi } from '../api/centerApi';
import { CenterContractInfoSection } from '../components/CenterContractInfoSection';
import type { ContractTemplate } from '../types/centerTypes';

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

type TemplateForm = { name: string; content: string; contractType: 'CLASS' | 'RECRUITMENT' };
const EMPTY: TemplateForm = { name: '', content: '', contractType: 'CLASS' };

const TYPE_LABEL: Record<'CLASS' | 'RECRUITMENT', string> = {
  CLASS: 'Hợp đồng học viên / dạy lớp',
  RECRUITMENT: 'Hợp đồng tuyển dụng gia sư',
};

/** Quản lý mẫu hợp đồng của trung tâm (tạo/sửa; mẫu hệ thống chỉ xem). */
export default function CenterContractTemplatesPage() {
  const [templates, setTemplates] = useState<ContractTemplate[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Form tạo/sửa. editingId = null -> tạo mới.
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState(EMPTY);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState('');

  const reload = () => {
    setLoading(true);
    centerApi
      .getContractTemplates()
      .then((res) => setTemplates(res.data))
      .catch((err) => setError(extractError(err, 'Không tải được mẫu hợp đồng.')))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    reload();
  }, []);

  const startCreate = () => {
    setEditingId(null);
    setForm(EMPTY);
    setFormError('');
  };

  const startEdit = (t: ContractTemplate) => {
    setEditingId(t.templateId);
    setForm({ name: t.name, content: t.content, contractType: t.contractType ?? 'CLASS' });
    setFormError('');
  };

  const submit = async () => {
    if (!form.name.trim() || !form.content.trim()) {
      setFormError('Vui lòng nhập tên và nội dung mẫu.');
      return;
    }
    setSaving(true);
    setFormError('');
    try {
      if (editingId != null) {
        await centerApi.updateContractTemplate(editingId, form);
      } else {
        await centerApi.createContractTemplate(form);
      }
      startCreate();
      reload();
    } catch (err) {
      setFormError(extractError(err, 'Không lưu được mẫu hợp đồng.'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <main style={{ maxWidth: 960, margin: '0 auto', padding: '24px 16px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 }}>
          <h1 style={{ margin: 0, fontSize: 22 }}>Mẫu hợp đồng</h1>
          <Link className="tcs-btn tcs-btn--ghost tcs-btn--sm" to="/center">
            ← Về quản lý lớp
          </Link>
        </div>
        <p style={{ color: '#64748b', fontSize: 14 }}>
          Mẫu hệ thống dùng chung (chỉ xem). Bạn có thể tạo mẫu riêng của trung tâm để chọn khi tạo lớp.
        </p>

        {/* Thông tin BÊN A của trung tâm trên hợp đồng */}
        <CenterContractInfoSection />

        {/* Form tạo/sửa */}
        <div style={{ border: '1px solid #e2e8f0', borderRadius: 12, padding: 16, marginBottom: 20 }}>
          <h2 style={{ marginTop: 0, fontSize: 16 }}>
            {editingId != null ? 'Sửa mẫu' : 'Tạo mẫu mới'}
          </h2>
          {formError && (
            <p style={{ background: '#fee2e2', color: '#991b1b', padding: 10, borderRadius: 8 }}>
              {formError}
            </p>
          )}
          <div style={{ marginBottom: 10 }}>
            <label style={{ display: 'block', fontSize: 13, fontWeight: 600, marginBottom: 4 }}>
              Tên mẫu
            </label>
            <input
              style={{ width: '100%', padding: 10, border: '1px solid #cbd5e1', borderRadius: 8 }}
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              placeholder="Ví dụ: Hợp đồng dạy Toán THCS"
            />
          </div>
          <div style={{ marginBottom: 10 }}>
            <label style={{ display: 'block', fontSize: 13, fontWeight: 600, marginBottom: 4 }}>
              Loại hợp đồng
            </label>
            <select
              style={{ width: '100%', padding: 10, border: '1px solid #cbd5e1', borderRadius: 8 }}
              value={form.contractType}
              onChange={(e) =>
                setForm({ ...form, contractType: e.target.value as 'CLASS' | 'RECRUITMENT' })
              }
            >
              <option value="CLASS">{TYPE_LABEL.CLASS}</option>
              <option value="RECRUITMENT">{TYPE_LABEL.RECRUITMENT}</option>
            </select>
            <p style={{ color: '#94a3b8', fontSize: 12, margin: '4px 0 0' }}>
              Hợp đồng tuyển dụng gửi cho gia sư khi duyệt — không có Loại lớp / Số buổi / Học phí.
            </p>
          </div>
          <div style={{ marginBottom: 10 }}>
            <label style={{ display: 'block', fontSize: 13, fontWeight: 600, marginBottom: 4 }}>
              Nội dung điều khoản
            </label>
            <textarea
              style={{
                width: '100%',
                minHeight: 160,
                padding: 10,
                border: '1px solid #cbd5e1',
                borderRadius: 8,
                fontFamily: 'inherit',
              }}
              value={form.content}
              onChange={(e) => setForm({ ...form, content: e.target.value })}
              placeholder="Nhập các điều khoản & nghĩa vụ..."
            />
            <p style={{ color: '#94a3b8', fontSize: 12, margin: '6px 0 0' }}>
              Chỉ nhập <strong>điều khoản & nghĩa vụ</strong>. Quốc hiệu, tiêu ngữ, tiêu đề và thông
              tin các bên sẽ được hệ thống tự thêm khi tạo hợp đồng. Có thể dùng biến tự điền:
              {form.contractType === 'RECRUITMENT' ? (
                <> <code>{'{{tenGiaSu}}'}</code>, <code>{'{{tenTrungTam}}'}</code>, <code>{'{{ngayKy}}'}</code>.</>
              ) : (
                <> <code>{'{{tenHocVien}}'}</code>, <code>{'{{tenTrungTam}}'}</code>, <code>{'{{tenLop}}'}</code>,{' '}
                  <code>{'{{monHoc}}'}</code>, <code>{'{{hocPhi}}'}</code>, <code>{'{{soBuoi}}'}</code>,{' '}
                  <code>{'{{ngayBatDau}}'}</code>, <code>{'{{ngayKetThuc}}'}</code>, <code>{'{{ngayKy}}'}</code>.</>
              )}
            </p>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button
              type="button"
              className="tcs-btn tcs-btn--market tcs-btn--sm"
              onClick={submit}
              disabled={saving}
            >
              {saving ? 'Đang lưu...' : editingId != null ? 'Cập nhật mẫu' : 'Tạo mẫu'}
            </button>
            {editingId != null && (
              <button type="button" className="tcs-btn tcs-btn--ghost tcs-btn--sm" onClick={startCreate}>
                Hủy sửa
              </button>
            )}
          </div>
        </div>

        {/* Danh sách mẫu */}
        {loading && <p>Đang tải...</p>}
        {error && <p style={{ color: '#991b1b' }}>{error}</p>}
        {!loading && templates.length === 0 && <p>Chưa có mẫu hợp đồng nào.</p>}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {templates.map((t) => (
            <div
              key={t.templateId}
              style={{ border: '1px solid #e2e8f0', borderRadius: 10, padding: 14 }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
                <strong>
                  {t.name}
                  <span style={{ color: '#0d9488', fontWeight: 400 }}>
                    {' · '}
                    {TYPE_LABEL[t.contractType ?? 'CLASS']}
                  </span>
                  {t.system && (
                    <span style={{ color: '#94a3b8', fontWeight: 400 }}> · mẫu hệ thống</span>
                  )}
                  {t.defaultTemplate && (
                    <span style={{ color: '#2563eb', fontWeight: 400 }}> · mặc định</span>
                  )}
                </strong>
                {!t.system && (
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                    onClick={() => startEdit(t)}
                  >
                    Sửa
                  </button>
                )}
              </div>
              <pre
                style={{
                  whiteSpace: 'pre-wrap',
                  fontFamily: 'inherit',
                  fontSize: 13,
                  color: '#475569',
                  margin: '8px 0 0',
                }}
              >
                {t.content}
              </pre>
            </div>
          ))}
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}
