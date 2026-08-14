import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { CenterSidebar } from '../components/CenterSidebar';
import { SiteFooter } from '../../home/components/SiteFooter';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { centerApi } from '../api/centerApi';
import { CenterContractInfoSection } from '../components/CenterContractInfoSection';
import { ContractDocumentPreview } from '../components/ContractDocumentPreview';
import type { CenterContractInfo, ContractTemplate } from '../types/centerTypes';
import '../../contract/pages/ContractPage.css';
import './CenterContractTemplatesPage.css';

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
  // null = đang kiểm tra; false = chưa xác minh (không cho tạo mẫu).
  const [verified, setVerified] = useState<boolean | null>(null);
  // Thông tin BÊN A (trung tâm) để dựng bản xem trước hợp đồng.
  const [info, setInfo] = useState<CenterContractInfo | null>(null);
  // Mẫu đang mở xem trước ở cửa sổ (từ danh sách).
  const [previewTpl, setPreviewTpl] = useState<ContractTemplate | null>(null);

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
    // Kiểm tra trạng thái xác minh (để cho tạo mẫu) + lấy thông tin BÊN A cho bản xem trước.
    centerApi
      .getContractInfo()
      .then((res) => {
        setVerified(res.data.verificationStatus === 'VERIFIED');
        setInfo(res.data);
      })
      .catch(() => setVerified(false));
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
    <div className="contract-shell">
      <HomeNavbar />
      <div className="cc-shell">
      <CenterSidebar />
      <div className="cc-shell__main">
      <main className="contract-page tcs-container">
        <section className="contract-page-head">
          <div>
            <h1>Mẫu hợp đồng</h1>
            <p>
              Mẫu hệ thống dùng chung. Bạn có thể tạo mẫu riêng của trung tâm để chọn khi tuyển
              dụng hoặc tạo lớp.
            </p>
          </div>
        </section>

        {/* Thông tin BÊN A của trung tâm trên hợp đồng */}
        <CenterContractInfoSection />

        {/* Chưa xác minh -> không cho tạo mẫu, hướng dẫn đi xác minh. */}
        {verified === false && (
          <div className="cct-warn">
            <span>
              Trung tâm cần được <strong>xác minh</strong> trước khi tạo mẫu hợp đồng.
            </span>
            <Link to={APP_ROUTES.verification}>Đi tới trang xác minh →</Link>
          </div>
        )}

        {/* Form tạo/sửa + xem trước hợp đồng — chỉ khi đã xác minh */}
        {verified !== false && (
          <div className="cct-editor">
          <section className="contract-card">
            <div className="contract-card__head">
              <h2>{editingId != null ? 'Sửa mẫu' : 'Tạo mẫu mới'}</h2>
            </div>
            <div className="cct-card-body">
              {formError && <p className="cct-alert cct-alert--error">{formError}</p>}
              <div className="cct-grid">
                <div className="cct-field cct-field--full">
                  <label className="cct-label">Tên mẫu</label>
                  <input
                    className="cct-input"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    placeholder="Ví dụ: Hợp đồng dạy Toán THCS"
                  />
                </div>
                <div className="cct-field cct-field--full">
                  <label className="cct-label">Loại hợp đồng</label>
                  <select
                    className="cct-select"
                    value={form.contractType}
                    onChange={(e) =>
                      setForm({ ...form, contractType: e.target.value as 'CLASS' | 'RECRUITMENT' })
                    }
                  >
                    <option value="CLASS">{TYPE_LABEL.CLASS}</option>
                    <option value="RECRUITMENT">{TYPE_LABEL.RECRUITMENT}</option>
                  </select>
                  <p className="cct-hint">
                    Hợp đồng tuyển dụng gửi cho gia sư khi duyệt — không có Loại lớp / Số buổi / Học
                    phí.
                  </p>
                </div>
                <div className="cct-field cct-field--full">
                  <label className="cct-label">Nội dung điều khoản</label>
                  <textarea
                    className="cct-textarea"
                    value={form.content}
                    onChange={(e) => setForm({ ...form, content: e.target.value })}
                    placeholder="Nhập các điều khoản & nghĩa vụ..."
                  />
                </div>
              </div>
              <div className="cct-actions">
                <button
                  type="button"
                  className="tcs-btn tcs-btn--market tcs-btn--sm"
                  onClick={submit}
                  disabled={saving}
                >
                  {saving ? 'Đang lưu...' : editingId != null ? 'Cập nhật mẫu' : 'Tạo mẫu'}
                </button>
                {editingId != null && (
                  <button
                    type="button"
                    className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                    onClick={startCreate}
                  >
                    Hủy sửa
                  </button>
                )}
              </div>
            </div>
          </section>

          <aside className="cct-preview">
            <div className="cct-preview__bar">📄 Xem trước hợp đồng</div>
            <ContractDocumentPreview
              name={form.name}
              contractType={form.contractType}
              content={form.content}
              info={info}
            />
          </aside>
          </div>
        )}

        {/* Danh sách mẫu */}
        <section className="contract-card">
          <div className="contract-card__head">
            <h2>Danh sách mẫu</h2>
            {!loading && <span>{templates.length} mẫu</span>}
          </div>
          {loading && <div className="cct-state">Đang tải...</div>}
          {error && <p className="cct-alert cct-alert--error" style={{ margin: 'var(--space-lg)' }}>{error}</p>}
          {!loading && !error && templates.length === 0 && (
            <div className="cct-state">Chưa có mẫu hợp đồng nào.</div>
          )}
          {!loading && templates.length > 0 && (
            <div className="cct-tpl-list">
              {templates.map((t) => (
                <article key={t.templateId} className="cct-tpl">
                  <div className="cct-tpl__head">
                    <div className="cct-tpl__title">
                      <strong>{t.name}</strong>
                      <span className="cct-type-chip">{TYPE_LABEL[t.contractType ?? 'CLASS']}</span>
                      {t.system && <span className="cct-badge">Mẫu hệ thống</span>}
                      {t.defaultTemplate && (
                        <span className="cct-badge cct-badge--primary">Mặc định</span>
                      )}
                    </div>
                    <div className="cct-tpl__actions">
                      <button
                        type="button"
                        className="tcs-btn tcs-btn--ghost tcs-btn--sm"
                        onClick={() => setPreviewTpl(t)}
                      >
                        Xem trước
                      </button>
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
                  </div>
                  <div className="cct-tpl__preview">
                    <ContractDocumentPreview
                      name={t.name}
                      contractType={t.contractType ?? 'CLASS'}
                      content={t.content}
                      info={info}
                    />
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </main>
      </div>
      </div>

      {previewTpl && (
        <div
          className="cct-modal"
          role="dialog"
          aria-modal="true"
          onClick={() => setPreviewTpl(null)}
        >
          <div className="cct-modal__card" onClick={(e) => e.stopPropagation()}>
            <div className="cct-modal__head">
              <h2 className="cct-modal__title">Xem trước hợp đồng</h2>
              <button
                type="button"
                className="cct-modal__close"
                aria-label="Đóng"
                onClick={() => setPreviewTpl(null)}
              >
                ×
              </button>
            </div>
            <div className="cct-modal__body">
              <ContractDocumentPreview
                name={previewTpl.name}
                contractType={previewTpl.contractType ?? 'CLASS'}
                content={previewTpl.content}
                info={info}
              />
            </div>
          </div>
        </div>
      )}
      <SiteFooter />
    </div>
  );
}
