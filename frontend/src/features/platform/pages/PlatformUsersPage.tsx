import { AdminLayout } from '../components/AdminLayout';
import { useUpdateUserStatus } from '../hooks/usePlatformMutations';
import { useUserList } from '../hooks/useUserList';
import type { UserRole, UserStatus } from '../types/platformTypes';
import './PlatformUsersPage.css';

function statusBadgeClass(status: UserStatus) {
  if (status === 'ACTIVE') return 'tcs-badge tcs-badge--active';
  if (status === 'SUSPENDED') return 'tcs-badge tcs-badge--suspended';
  return 'tcs-badge tcs-badge--banned';
}

const STATUS_ACTION_LABELS: Record<UserStatus, string> = {
  ACTIVE: 'Kích hoạt',
  SUSPENDED: 'Tạm ngưng',
  BANNED: 'Khóa',
};

function confirmStatusChange(nextStatus: UserStatus) {
  if (nextStatus === 'BANNED') {
    return window.confirm('Khóa tài khoản này? Người dùng sẽ không thể đăng nhập.');
  }
  if (nextStatus === 'SUSPENDED') {
    return window.confirm('Tạm ngưng tài khoản này?');
  }
  return window.confirm('Kích hoạt lại tài khoản này?');
}

export default function PlatformUsersPage() {
  const { status, data, filters, setFilters, reload, errorMessage: listErrorMessage } = useUserList({
    page: 0,
    size: 10,
  });
  const { status: mutationStatus, errorMessage, updateStatus, reset } = useUpdateUserStatus();

  const applyFilter = (patch: Partial<typeof filters>) => {
    setFilters((current) => ({ ...current, ...patch, page: 0 }));
  };

  const handleStatusChange = async (userId: string, nextStatus: UserStatus, role: UserRole) => {
    if (role === 'PLATFORM_ADMIN') return;
    if (!confirmStatusChange(nextStatus)) return;

    reset();
    const ok = await updateStatus(userId, nextStatus);
    if (ok) reload();
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
            <option value="SUSPENDED">Tạm ngưng</option>
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
                                  onClick={() => handleStatusChange(user.id, 'ACTIVE', user.role)}
                                >
                                  Kích hoạt
                                </button>
                              )}
                              {user.status === 'ACTIVE' && (
                                <button
                                  className="tcs-btn tcs-btn--warning tcs-btn--badge"
                                  type="button"
                                  disabled={mutationStatus === 'loading'}
                                  title={STATUS_ACTION_LABELS.SUSPENDED}
                                  onClick={() => handleStatusChange(user.id, 'SUSPENDED', user.role)}
                                >
                                  Tạm ngưng
                                </button>
                              )}
                              {user.status !== 'BANNED' && (
                                <button
                                  className="tcs-btn tcs-btn--danger tcs-btn--badge"
                                  type="button"
                                  disabled={mutationStatus === 'loading'}
                                  title={STATUS_ACTION_LABELS.BANNED}
                                  onClick={() => handleStatusChange(user.id, 'BANNED', user.role)}
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

            <div className="adm-pagination">
              <span>
                Trang {data.page + 1}/{Math.max(data.totalPages, 1)} · {data.totalElements} người dùng
              </span>
              <div className="adm-pagination__actions">
                <button
                  className="tcs-btn tcs-btn--ghost"
                  type="button"
                  disabled={data.page <= 0}
                  onClick={() => setFilters((current) => ({ ...current, page: current.page - 1 }))}
                >
                  Trước
                </button>
                <button
                  className="tcs-btn tcs-btn--ghost"
                  type="button"
                  disabled={data.page + 1 >= data.totalPages}
                  onClick={() => setFilters((current) => ({ ...current, page: current.page + 1 }))}
                >
                  Sau
                </button>
              </div>
            </div>
          </>
        )}
      </div>
    </AdminLayout>
  );
}
