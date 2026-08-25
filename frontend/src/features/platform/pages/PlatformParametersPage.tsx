import type { FormEvent } from 'react';
import { useState, useEffect } from 'react';
import { ConfirmDialog, Pagination } from '../../../shared/components';
import type { SystemParameterItem, UpsertSystemParameterRequest } from '../../catalog/types/catalogTypes';
import { AdminLayout } from '../components/AdminLayout';
import { useSystemParameterList, useSystemParameterMutations } from '../hooks/useSystemParameters';

const EMPTY_FORM: UpsertSystemParameterRequest = {
  paramKey: '',
  paramValue: '',
  description: '',
};

export default function PlatformParametersPage() {
  const { status, items, errorMessage: listErrorMessage, filters, setFilters, reload } = useSystemParameterList();
  const { status: mutationStatus, errorMessage, createParameter, updateParameter, deleteParameter, reset } =
    useSystemParameterMutations();

  const [form, setForm] = useState<UpsertSystemParameterRequest>(EMPTY_FORM);
  const [selected, setSelected] = useState<SystemParameterItem | null>(null);
  const [pendingDelete, setPendingDelete] = useState<SystemParameterItem | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  useEffect(() => {
    setCurrentPage(1);
  }, [filters.keyword, filters.prefix, items.length]);

  const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
  const validCurrentPage = Math.min(currentPage, totalPages);
  const startIndex = (validCurrentPage - 1) * pageSize;
  const endIndex = Math.min(startIndex + pageSize, items.length);
  const paginatedItems = items.slice(startIndex, endIndex);

  const applyFilter = (patch: Partial<typeof filters>) => {
    setFilters((current) => ({ ...current, ...patch }));
  };

  function selectParameter(param: SystemParameterItem) {
    setSelected(param);
    reset();
    setForm({
      paramKey: param.paramKey,
      paramValue: param.paramValue,
      description: param.description ?? '',
    });
  }

  function resetForm() {
    setSelected(null);
    reset();
    setForm(EMPTY_FORM);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const ok = selected
      ? await updateParameter(selected.parameterId, form)
      : await createParameter(form);
    if (ok) {
      resetForm();
      reload();
    }
  }

  async function confirmDelete() {
    if (!pendingDelete) return;
    const ok = await deleteParameter(pendingDelete.parameterId);
    setPendingDelete(null);
    if (ok) {
      if (selected?.parameterId === pendingDelete.parameterId) resetForm();
      reload();
    }
  }

  return (
    <AdminLayout
      title="Cấu hình hệ thống"
      subtitle="Quản lý các tham số cấu hình của hệ thống (system parameters)."
    >
      <div className="adm-layout-split">
        <div className="adm-card">
          <div className="adm-toolbar">
            <input
              className="adm-field"
              placeholder="Tìm theo khóa hoặc giá trị..."
              value={filters.keyword ?? ''}
              onChange={(event) => applyFilter({ keyword: event.target.value || undefined })}
            />
            <input
              className="adm-field adm-field--fixed"
              placeholder="Lọc theo tiền tố khóa (ví dụ: SUBSTITUTION_)"
              value={filters.prefix ?? ''}
              onChange={(event) => applyFilter({ prefix: event.target.value || undefined })}
            />
            <button className="tcs-btn tcs-btn--ghost" type="button" onClick={reload}>
              Làm mới
            </button>
            <button className="tcs-btn tcs-btn--primary" type="button" onClick={resetForm}>
              Tạo tham số mới
            </button>
          </div>

          {status === 'loading' && <div className="adm-state">Đang tải tham số hệ thống…</div>}
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
              <table className="adm-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Khóa</th>
                    <th>Giá trị</th>
                    <th>Mô tả</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {items.length === 0 ? (
                    <tr>
                      <td colSpan={5}>Chưa có tham số hệ thống nào.</td>
                    </tr>
                  ) : (
                    paginatedItems.map((param) => (
                      <tr key={param.parameterId}>
                        <td>{param.parameterId}</td>
                        <td><code>{param.paramKey}</code></td>
                        <td className="adm-table__notes">{param.paramValue}</td>
                        <td className="adm-table__notes">{param.description || '—'}</td>
                        <td className="adm-table__actions">
                          <div className="adm-row-actions">
                            <button className="tcs-btn tcs-btn--ghost tcs-btn--badge" type="button" onClick={() => selectParameter(param)}>
                              Sửa
                            </button>
                            <button
                              className="tcs-btn tcs-btn--danger tcs-btn--badge"
                              type="button"
                              onClick={() => setPendingDelete(param)}
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
            <h2 className="adm-card__title">
              {selected ? `Chỉnh sửa tham số #${selected.parameterId}` : 'Tạo tham số mới'}
            </h2>
          </div>

          <form className="adm-form" onSubmit={(event) => void handleSubmit(event)}>
            <div className="adm-field-group">
              <label htmlFor="param-key">Khóa (param_key)</label>
              <input
                id="param-key"
                className="adm-field"
                value={form.paramKey}
                onChange={(event) => setForm((current) => ({ ...current, paramKey: event.target.value }))}
                placeholder="Ví dụ: SUBSTITUTION_MAX_PER_MONTH"
                maxLength={100}
                required
              />
            </div>

            <div className="adm-field-group">
              <label htmlFor="param-value">Giá trị (param_value)</label>
              <textarea
                id="param-value"
                className="adm-field adm-field--short"
                value={form.paramValue}
                onChange={(event) => setForm((current) => ({ ...current, paramValue: event.target.value }))}
                placeholder="Giá trị của tham số"
                required
              />
            </div>

            <div className="adm-field-group">
              <label htmlFor="param-description">Mô tả</label>
              <textarea
                id="param-description"
                className="adm-field adm-field--short"
                value={form.description}
                onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
                placeholder="Mô tả ý nghĩa của tham số (không bắt buộc)"
              />
            </div>

            {mutationStatus === 'error' && errorMessage && (
              <div className="adm-alert adm-alert--error">{errorMessage}</div>
            )}

            <div className="adm-form__footer">
              <button className="tcs-btn tcs-btn--primary" type="submit" disabled={mutationStatus === 'loading'}>
                {mutationStatus === 'loading' ? 'Đang lưu…' : selected ? 'Cập nhật' : 'Tạo tham số'}
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
          title="Xóa tham số hệ thống"
          message={`Xóa tham số "${pendingDelete.paramKey}"? Hành động này không thể hoàn tác.`}
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