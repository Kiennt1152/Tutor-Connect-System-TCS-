import { useState } from 'react';
import { ConfirmDialog, Pagination } from '../../../shared/components';
import type { ConfirmDialogVariant } from '../../../shared/components';
import { AdminLayout } from '../components/AdminLayout';
import { useUpdateUserStatus } from '../hooks/usePlatformMutations';
import { useUserList } from '../hooks/useUserList';
import { platformApi } from '../api/platformApi';
import type { CreateUserApiRequest, UserRole, UserStatus } from '../types/platformTypes';

// Quản trị chỉ còn hai trạng thái: Hoạt động và Đã khóa. Tài khoản SUSPENDED cũ (nếu có)
// được coi như đã khóa: hiện nhãn "Đã khóa" và chỉ còn nút Kích hoạt.
type AdminStatus = 'ACTIVE' | 'BANNED';

function statusBadgeClass(status: UserStatus) {
  return status === 'ACTIVE' ? 'tcs-badge tcs-badge--active' : 'tcs-badge tcs-badge--banned';
}

const STATUS_ACTION_LABELS: Record<AdminStatus, string> = {
  ACTIVE: 'Kích hoạt',
  BANNED: 'Khóa',
};

type StatusDialogConfig = {
  title: string;
  confirmLabel: string;
  variant: ConfirmDialogVariant;
  describe: (name: string) => string;
};

const STATUS_DIALOG: Record<AdminStatus, StatusDialogConfig> = {
  BANNED: {
    title: 'Khóa tài khoản',
    confirmLabel: 'Khóa',
    variant: 'danger',
    describe: (name) =>
      `Khóa tài khoản "${name}"? Người dùng sẽ không thể đăng nhập và email không thể đăng ký lại.`,
  },
  ACTIVE: {
    title: 'Kích hoạt tài khoản',
    confirmLabel: 'Kích hoạt',
    variant: 'primary',
    describe: (name) => `Kích hoạt lại tài khoản "${name}"?`,
  },
};

type PendingStatusChange = {
  userId: string;
  displayName: string;
  nextStatus: AdminStatus;
};

export default function PlatformUsersPage() {
  const { status, data, filters, setFilters, reload, errorMessage: listErrorMessage } = useUserList({
    page: 0,
    size: 10,
  });
  const { status: mutationStatus, errorMessage, updateStatus, reset } = useUpdateUserStatus();
  const [pending, setPending] = useState<PendingStatusChange | null>(null);

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [createForm, setCreateForm] = useState<CreateUserApiRequest>({
    email: '',
    password: '',
    displayName: '',
    phone: '',
    role: 'CLIENT',
    status: 'ACTIVE',
  });
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreateError(null);
    if (!createForm.email.trim() || !createForm.password.trim() || !createForm.displayName.trim()) {
      setCreateError('Vui lòng điền đầy đủ các trường bắt buộc (Email, Mật khẩu, Họ và tên).');
      return;
    }
    if (createForm.password.length < 6) {
      setCreateError('Mật khẩu phải có ít nhất 6 ký tự.');
      return;
    }
    try {
      setCreateLoading(true);
      await platformApi.createUser(createForm);
      setIsCreateModalOpen(false);
      setCreateForm({
        email: '',
        password: '',
        displayName: '',
        phone: '',
        role: 'CLIENT',
        status: 'ACTIVE',
      });
      reload();
    } catch (err: any) {
      setCreateError(err?.response?.data?.message || err?.message || 'Không thể tạo người dùng mới.');
    } finally {
      setCreateLoading(false);
    }
  };

  const applyFilter = (patch: Partial<typeof filters>) => {
    setFilters((current) => ({ ...current, ...patch, page: 0 }));
  };

  const requestStatusChange = (
    userId: string,
    displayName: string,
    nextStatus: AdminStatus,
    role: UserRole,
  ) => {
    if (role === 'PLATFORM_ADMIN') return;
    reset();
    setPending({ userId, displayName, nextStatus });
  };

  const confirmStatusChange = async () => {
    if (!pending) return;
    const ok = await updateStatus(pending.userId, pending.nextStatus);
    setPending(null);
    if (ok) reload();
  };

  const cancelStatusChange = () => {
    if (mutationStatus === 'loading') return;
    setPending(null);
  };

  return (
    <AdminLayout
      title="Quản lý người dùng"
      subtitle="Xem danh sách tài khoản, lọc theo vai trò và trạng thái."
    >
      <div className="adm-card">
        {mutationStatus === 'error' && errorMessage && (
          <div className="adm-alert adm-alert--error">{errorMessage}</div>
        )}

        <div className="adm-toolbar">
          <input
            className="adm-field"
            placeholder="Tìm theo tên, email hoặc SĐT..."
            value={filters.keyword ?? ''}
            onChange={(event) => applyFilter({ keyword: event.target.value || undefined })}
          />
          <select
            className="adm-field"
            value={filters.status ?? ''}
            onChange={(event) =>
              applyFilter({
                status: (event.target.value as UserStatus) || undefined,
              })
            }
          >
            <option value="">Tất cả trạng thái</option>
            <option value="ACTIVE">Hoạt động</option>
            <option value="BANNED">Đã khóa</option>
          </select>
          <select
            className="adm-field"
            value={filters.role ?? ''}
            onChange={(event) =>
              applyFilter({
                role: (event.target.value as UserRole) || undefined,
              })
            }
          >
            <option value="">Tất cả vai trò</option>
            <option value="PLATFORM_ADMIN">Quản trị viên</option>
            <option value="TUTOR">Gia sư</option>
            <option value="TUTOR_CENTER">Trung tâm</option>
            <option value="CLIENT">Phụ huynh/Học sinh</option>
          </select>
          <button className="tcs-btn tcs-btn--ghost" type="button" onClick={reload}>
            Làm mới
          </button>
          <button
            className="tcs-btn tcs-btn--primary"
            type="button"
            style={{ marginLeft: 'auto', fontWeight: 600 }}
            onClick={() => setIsCreateModalOpen(true)}
          >
            + Thêm tài khoản
          </button>
        </div>

        {status === 'loading' && <div className="adm-state">Đang tải danh sách…</div>}
        {status === 'error' && (
          <div className="adm-state">
            <p>{listErrorMessage ?? 'Không tải được dữ liệu.'}</p>
            <button className="tcs-btn tcs-btn--primary" type="button" onClick={reload}>
              Thử lại
            </button>
          </div>
        )}

        {status === 'success' && data && (
          <>
            <div className="adm-table-wrap">
              <table className="adm-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Họ và tên</th>
                    <th>Email</th>
                    <th>Số điện thoại</th>
                    <th>Vai trò</th>
                    <th>Trạng thái</th>
                    <th>Ngày tạo</th>
                    <th>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {data.items.length === 0 ? (
                    <tr>
                      <td colSpan={8}>Chưa có người dùng nào.</td>
                    </tr>
                  ) : (
                    data.items.map((user) => (
                      <tr key={user.id}>
                        <td>{user.id}</td>
                        <td>{user.displayName}</td>
                        <td>{user.email}</td>
                        <td>{user.phone}</td>
                        <td className="adm-table__badge">
                          <span className="tcs-badge tcs-badge--role">{user.roleLabel}</span>
                        </td>
                        <td className="adm-table__badge">
                          <span className={statusBadgeClass(user.status)}>{user.statusLabel}</span>
                        </td>
                        <td>{user.createdAt}</td>
                        <td className="adm-table__actions">
                          {user.role === 'PLATFORM_ADMIN' ? (
                            <span className="adm-muted">—</span>
                          ) : (
                            <div className="adm-row-actions">
                              {user.status !== 'ACTIVE' && (
                                <button
                                  className="tcs-btn tcs-btn--success tcs-btn--badge"
                                  type="button"
                                  disabled={mutationStatus === 'loading'}
                                  title={STATUS_ACTION_LABELS.ACTIVE}
                                  onClick={() =>
                                    requestStatusChange(user.id, user.displayName, 'ACTIVE', user.role)
                                  }
                                >
                                  Kích hoạt
                                </button>
                              )}
                              {user.status === 'ACTIVE' && (
                                <button
                                  className="tcs-btn tcs-btn--danger tcs-btn--badge"
                                  type="button"
                                  disabled={mutationStatus === 'loading'}
                                  title={STATUS_ACTION_LABELS.BANNED}
                                  onClick={() =>
                                    requestStatusChange(user.id, user.displayName, 'BANNED', user.role)
                                  }
                                >
                                  Khóa
                                </button>
                              )}
                            </div>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            <div className="adm-pagination" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', marginTop: '16px', gap: '8px' }}>
              <select
                className="adm-field adm-field--fixed"
                style={{ width: 'auto', padding: '4px 8px', fontSize: '13px', borderRadius: '8px' }}
                value={filters.size}
                onChange={(e) =>
                  setFilters((current) => ({
                    ...current,
                    size: Number(e.target.value),
                    page: 0,
                  }))
                }
              >
                <option value={10}>10 / trang</option>
                <option value={20}>20 / trang</option>
                <option value={50}>50 / trang</option>
              </select>
              <Pagination
                current={data.page + 1}
                totalPages={Math.max(data.totalPages, 1)}
                onPageChange={(p) => setFilters((current) => ({ ...current, page: p - 1 }))}
              />
            </div>
          </>
        )}
      </div>

      {pending && (
        <ConfirmDialog
          open
          title={STATUS_DIALOG[pending.nextStatus].title}
          message={STATUS_DIALOG[pending.nextStatus].describe(pending.displayName)}
          confirmLabel={STATUS_DIALOG[pending.nextStatus].confirmLabel}
          variant={STATUS_DIALOG[pending.nextStatus].variant}
          loading={mutationStatus === 'loading'}
          onConfirm={confirmStatusChange}
          onCancel={cancelStatusChange}
        />
      )}

      {isCreateModalOpen && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.5)',
            backdropFilter: 'blur(2px)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '1rem',
          }}
          onClick={(e) => {
            if (e.target === e.currentTarget && !createLoading) setIsCreateModalOpen(false);
          }}
        >
          <div
            style={{
              background: '#fff',
              borderRadius: '12px',
              maxWidth: '560px',
              width: '100%',
              boxShadow: '0 20px 25px -5px rgba(0,0,0,0.15)',
              overflow: 'hidden',
            }}
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
              <h3 style={{ margin: 0, fontSize: '1.2rem', fontWeight: 700, color: '#1e293b' }}>
                Thêm tài khoản mới (UC-07)
              </h3>
              <button
                type="button"
                style={{ background: 'none', border: 'none', fontSize: '1.5rem', cursor: 'pointer', color: '#64748b' }}
                disabled={createLoading}
                onClick={() => setIsCreateModalOpen(false)}
              >
                &times;
              </button>
            </div>

            <form onSubmit={handleCreateUser} style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {createError && (
                <div style={{ padding: '10px 14px', background: '#fef2f2', border: '1px solid #fecaca', borderRadius: '8px', color: '#dc2626', fontSize: '13px' }}>
                  {createError}
                </div>
              )}

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <label style={{ fontSize: '12px', fontWeight: 600, color: '#475569' }}>
                    Email <span style={{ color: '#ef4444' }}>*</span>
                  </label>
                  <input
                    type="email"
                    required
                    className="adm-field"
                    style={{ minWidth: 'unset', width: '100%' }}
                    placeholder="user@example.com"
                    value={createForm.email}
                    onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
                  />
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <label style={{ fontSize: '12px', fontWeight: 600, color: '#475569' }}>
                    Mật khẩu <span style={{ color: '#ef4444' }}>*</span>
                  </label>
                  <input
                    type="password"
                    required
                    className="adm-field"
                    style={{ minWidth: 'unset', width: '100%' }}
                    placeholder="Tối thiểu 6 ký tự"
                    value={createForm.password}
                    onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <label style={{ fontSize: '12px', fontWeight: 600, color: '#475569' }}>
                    Họ và tên / Tên hiển thị <span style={{ color: '#ef4444' }}>*</span>
                  </label>
                  <input
                    type="text"
                    required
                    className="adm-field"
                    style={{ minWidth: 'unset', width: '100%' }}
                    placeholder="Nguyễn Văn A"
                    value={createForm.displayName}
                    onChange={(e) => setCreateForm({ ...createForm, displayName: e.target.value })}
                  />
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <label style={{ fontSize: '12px', fontWeight: 600, color: '#475569' }}>Số điện thoại</label>
                  <input
                    type="tel"
                    className="adm-field"
                    style={{ minWidth: 'unset', width: '100%' }}
                    placeholder="0912345678"
                    value={createForm.phone || ''}
                    onChange={(e) => setCreateForm({ ...createForm, phone: e.target.value })}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <label style={{ fontSize: '12px', fontWeight: 600, color: '#475569' }}>
                    Vai trò hệ thống <span style={{ color: '#ef4444' }}>*</span>
                  </label>
                  <select
                    className="adm-field"
                    style={{ minWidth: 'unset', width: '100%' }}
                    value={createForm.role}
                    onChange={(e) => setCreateForm({ ...createForm, role: e.target.value as UserRole })}
                  >
                    <option value="CLIENT">Phụ huynh / Học sinh (CLIENT)</option>
                    <option value="TUTOR">Gia sư (TUTOR)</option>
                    <option value="TUTOR_CENTER">Trung tâm gia sư (TUTOR_CENTER)</option>
                    <option value="PLATFORM_ADMIN">Quản trị viên (PLATFORM_ADMIN)</option>
                  </select>
                </div>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <label style={{ fontSize: '12px', fontWeight: 600, color: '#475569' }}>Trạng thái ban đầu</label>
                  <select
                    className="adm-field"
                    style={{ minWidth: 'unset', width: '100%' }}
                    value={createForm.status || 'ACTIVE'}
                    onChange={(e) => setCreateForm({ ...createForm, status: e.target.value as UserStatus })}
                  >
                    <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                    <option value="SUSPENDED">Tạm ngưng (SUSPENDED)</option>
                  </select>
                </div>
              </div>

              <div
                style={{
                  display: 'flex',
                  justifyContent: 'flex-end',
                  gap: '0.75rem',
                  marginTop: '0.75rem',
                  paddingTop: '1rem',
                  borderTop: '1px solid #f1f5f9',
                }}
              >
                <button
                  type="button"
                  className="tcs-btn tcs-btn--ghost"
                  disabled={createLoading}
                  onClick={() => setIsCreateModalOpen(false)}
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  className="tcs-btn tcs-btn--primary"
                  disabled={createLoading}
                >
                  {createLoading ? 'Đang tạo...' : 'Tạo tài khoản'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AdminLayout>
  );
}
