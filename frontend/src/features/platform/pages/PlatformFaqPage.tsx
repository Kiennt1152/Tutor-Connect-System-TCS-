/**
 * ============================================================================
 * TRANG QUẢN TRỊ KHO TRI THỨC VÀ HỎI ĐÁP FAQ (PLATFORM FAQ MANAGEMENT PAGE)
 * ============================================================================
 * 
 * Tác giả: mduc1011-swp
 * Mô tả các tính năng quản lý FAQ:
 *   - Quản lý bộ câu hỏi - trả lời thường gặp (FAQ Knowledge Base) phục vụ khách hàng và luồng RAG của AI.
 *   - Thêm mới, chỉnh sửa, xóa và sắp xếp thứ tự ưu tiên hiển thị (sortOrder).
 *   - Lọc danh mục câu hỏi (Chung, Phụ huynh, Gia sư, Trung tâm, Tài chính, Lớp học).
 *   - Bật/tắt trạng thái xuất bản (Published / Hidden) trên trang Trợ giúp công khai.
 */

import type { FormEvent } from 'react';
import { useState, useEffect } from 'react';
import { ConfirmDialog, Pagination } from '../../../shared/components';
import type { UpsertFaqRequest, FaqItem } from '../../catalog/types/catalogTypes';
import { AdminLayout } from '../components/AdminLayout';
import { useFaqList, useFaqMutations } from '../hooks/useFaqManagement';

const EMPTY_FORM: UpsertFaqRequest = {
  question: '',
  answer: '',
  category: '',
  sortOrder: 0,
  published: true,
};

function formatDateTime(value: string | null) {
  if (!value) return '—';
  try {
    return new Date(value).toLocaleString('vi-VN');
  } catch {
    return value;
  }
}

export default function PlatformFaqPage() {
  const { status, items, errorMessage: listErrorMessage, filters, setFilters, reload } = useFaqList();
  const { status: mutationStatus, errorMessage, createFaq, updateFaq, deleteFaq, reset } = useFaqMutations();

  const [form, setForm] = useState<UpsertFaqRequest>(EMPTY_FORM);
  const [selectedFaq, setSelectedFaq] = useState<FaqItem | null>(null);
  const [pendingDelete, setPendingDelete] = useState<FaqItem | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Reset to page 1 when filters or items change
  useEffect(() => {
    setCurrentPage(1);
  }, [filters.keyword, filters.category, items.length]);

  const applyFilter = (patch: Partial<typeof filters>) => {
    setFilters((current) => ({ ...current, ...patch }));
  };

  const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
  const validCurrentPage = Math.min(currentPage, totalPages);
  const startIndex = (validCurrentPage - 1) * pageSize;
  const endIndex = Math.min(startIndex + pageSize, items.length);
  const paginatedItems = items.slice(startIndex, endIndex);

  function selectFaq(faq: FaqItem) {
    setSelectedFaq(faq);
    reset();
    setForm({
      question: faq.question,
      answer: faq.answer,
      category: faq.category ?? '',
      sortOrder: faq.sortOrder,
      published: faq.published,
    });
  }

  function resetForm() {
    setSelectedFaq(null);
    reset();
    setForm(EMPTY_FORM);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const ok = selectedFaq
      ? await updateFaq(selectedFaq.faqId, form)
      : await createFaq(form);
    if (ok) {
      resetForm();
      reload();
    }
  }

  async function confirmDelete() {
    if (!pendingDelete) return;
    const ok = await deleteFaq(pendingDelete.faqId);
    setPendingDelete(null);
    if (ok) {
      if (selectedFaq?.faqId === pendingDelete.faqId) resetForm();
      reload();
    }
  }

  return (
    <AdminLayout
      title="Quản lý FAQ"
      subtitle="Quản lý bộ câu hỏi thường gặp hiển thị trên trang Trợ giúp và dùng cho chatbot."
    >
      <div className="adm-layout-split">
        <div className="adm-card">
          <div className="adm-toolbar">
            <input
              className="adm-field"
              placeholder="Tìm theo câu hỏi hoặc câu trả lời..."
              value={filters.keyword ?? ''}
              onChange={(event) => applyFilter({ keyword: event.target.value || undefined })}
            />
            <input
              className="adm-field adm-field--fixed"
              placeholder="Danh mục (ví dụ: TICKET, PAYMENT...)"
              value={filters.category ?? ''}
              onChange={(event) => applyFilter({ category: event.target.value || undefined })}
            />
            <button className="tcs-btn tcs-btn--ghost" type="button" onClick={reload}>
              Làm mới
            </button>
            <button className="tcs-btn tcs-btn--primary" type="button" onClick={resetForm}>
              Tạo FAQ mới
            </button>
          </div>

          {status === 'loading' && <div className="adm-state">Đang tải danh sách FAQ…</div>}
          {status === 'error' && (
            <div className="adm-state">
              <p>{listErrorMessage ?? 'Không tải được dữ liệu.'}</p>
              <button className="tcs-btn tcs-btn--primary" type="button" onClick={reload}>
                Thử lại
              </button>
            </div>
          )}

          {status === 'success' && (
            <div className="adm-table-wrap">
              <table className="adm-table adm-table--faq">
                <thead>
                  <tr>
                    <th style={{ width: '45px' }}>ID</th>
                    <th style={{ minWidth: '180px' }}>Câu hỏi</th>
                    <th>Danh mục</th>
                    <th style={{ width: '65px', textAlign: 'center' }}>Thứ tự</th>
                    <th>Trạng thái</th>
                    <th>Cập nhật</th>
                    <th style={{ width: '110px' }}>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {items.length === 0 ? (
                    <tr>
                      <td colSpan={7}>Chưa có câu hỏi thường gặp nào.</td>
                    </tr>
                  ) : (
                    paginatedItems.map((faq) => (
                      <tr key={faq.faqId}>
                        <td>{faq.faqId}</td>
                        <td className="adm-table__question">
                          <div className="adm-table__question-text">{faq.question}</div>
                        </td>
                        <td>
                          <span className="tcs-badge" style={{ background: '#f1f5f9', color: '#475569', fontWeight: 600 }}>
                            {faq.category || '—'}
                          </span>
                        </td>
                        <td style={{ textAlign: 'center' }}>{faq.sortOrder}</td>
                        <td className="adm-table__badge">
                          <span className={faq.published ? 'tcs-badge tcs-badge--active' : 'tcs-badge tcs-badge--suspended'}>
                            {faq.published ? 'Đã xuất bản' : 'Bản nháp'}
                          </span>
                        </td>
                        <td style={{ whiteSpace: 'nowrap' }}>{formatDateTime(faq.updatedAt)}</td>
                        <td className="adm-table__actions">
                          <div className="adm-row-actions">
                            <button className="tcs-btn tcs-btn--ghost tcs-btn--badge" type="button" onClick={() => selectFaq(faq)}>
                              Sửa
                            </button>
                            <button
                              className="tcs-btn tcs-btn--danger tcs-btn--badge"
                              type="button"
                              onClick={() => setPendingDelete(faq)}
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
          )}

          {status === 'success' && items.length > 0 && (
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
                current={validCurrentPage}
                totalPages={totalPages}
                onPageChange={setCurrentPage}
              />
            </div>
          )}
        </div>

        <article className="adm-card adm-card--sticky">
          <div className="adm-card__head">
            <h2 className="adm-card__title">{selectedFaq ? `Chỉnh sửa FAQ #${selectedFaq.faqId}` : 'Tạo FAQ mới'}</h2>
          </div>

          <form className="adm-form" onSubmit={(event) => void handleSubmit(event)}>
            <div className="adm-field-group">
              <label htmlFor="faq-question">Câu hỏi</label>
              <textarea
                id="faq-question"
                className="adm-field adm-field--short"
                value={form.question}
                onChange={(event) => setForm((current) => ({ ...current, question: event.target.value }))}
                placeholder="Ví dụ: Làm sao để tạo yêu cầu hỗ trợ?"
                required
              />
            </div>

            <div className="adm-field-group">
              <label htmlFor="faq-answer">Câu trả lời</label>
              <textarea
                id="faq-answer"
                className="adm-field adm-field--tall"
                value={form.answer}
                onChange={(event) => setForm((current) => ({ ...current, answer: event.target.value }))}
                placeholder="Nội dung trả lời hiển thị cho người dùng"
                required
              />
            </div>

            <div className="adm-field-row">
              <div className="adm-field-group">
                <label htmlFor="faq-category">Danh mục</label>
                <input
                  id="faq-category"
                  className="adm-field"
                  value={form.category}
                  onChange={(event) => setForm((current) => ({ ...current, category: event.target.value }))}
                  placeholder="Ví dụ: TICKET, PAYMENT..."
                />
              </div>

              <div className="adm-field-group">
                <label htmlFor="faq-sort-order">Thứ tự hiển thị</label>
                <input
                  id="faq-sort-order"
                  type="number"
                  className="adm-field"
                  value={form.sortOrder}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, sortOrder: Number(event.target.value) || 0 }))
                  }
                />
              </div>
            </div>

            <div className="adm-field-group adm-field-group--inline">
              <label htmlFor="faq-published">
                <input
                  id="faq-published"
                  type="checkbox"
                  checked={form.published}
                  onChange={(event) => setForm((current) => ({ ...current, published: event.target.checked }))}
                />
                Xuất bản (hiển thị trên trang Trợ giúp)
              </label>
            </div>

            {mutationStatus === 'error' && errorMessage && (
              <div className="adm-alert adm-alert--error">{errorMessage}</div>
            )}

            <div className="adm-form__footer">
              <button className="tcs-btn tcs-btn--primary" type="submit" disabled={mutationStatus === 'loading'}>
                {mutationStatus === 'loading' ? 'Đang lưu…' : selectedFaq ? 'Cập nhật FAQ' : 'Tạo FAQ'}
              </button>
              <button className="tcs-btn tcs-btn--ghost" type="button" onClick={resetForm}>
                Xóa form
              </button>
            </div>
          </form>
        </article>
      </div>

      {pendingDelete && (
        <ConfirmDialog
          open
          title="Xóa câu hỏi thường gặp"
          message={`Xóa câu hỏi "${pendingDelete.question}"? Hành động này không thể hoàn tác.`}
          confirmLabel="Xóa"
          variant="danger"
          loading={mutationStatus === 'loading'}
          onConfirm={confirmDelete}
          onCancel={() => setPendingDelete(null)}
        />
      )}
    </AdminLayout>
  );
}
