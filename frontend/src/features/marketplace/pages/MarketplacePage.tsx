import { useState } from 'react';
import { ClientView } from '../components/ClientView';
import { TutorView } from '../components/TutorView';
import { useMarketplace } from '../hooks/useMarketplace';
import type { TutorApplication } from '../types/marketplaceTypes';
import './MarketplacePage.css';

type ViewMode = 'CLIENT' | 'TUTOR';

export default function MarketplacePage() {
  const {
    role,

    myApplications,
    filteredMyApplications,
    myStatus,
    myError,
    statusFilter,
    setStatusFilter,

    myClasses,
    classesStatus,
    classesError,
    selectedClassId,
    setSelectedClassId,
    classApplications,
    classApplicationsStatus,
    classApplicationsError,

    mutatingId,
    actionError,
    clearActionError,
    withdrawApplication,
    reviewApplication,
  } = useMarketplace();

  // Auto-pick role; user có thể override nếu cần (vd: TUTOR_CENTER cũng vào marketplace)
  const [viewOverride, setViewOverride] = useState<ViewMode | null>(null);
  const view: ViewMode =
    viewOverride ?? (role === 'CLIENT' ? 'CLIENT' : role === 'TUTOR' ? 'TUTOR' : 'CLIENT');

  async function handleAccept(application: TutorApplication) {
    clearActionError();
    try {
      await reviewApplication(application.applicationId, { decision: 'ACCEPTED' });
    } catch {
      // banner đã hiển thị
    }
  }

  async function handleReject(application: TutorApplication) {
    clearActionError();
    try {
      await reviewApplication(application.applicationId, { decision: 'REJECTED' });
    } catch {
      // banner đã hiển thị
    }
  }

  async function handleWithdraw(application: TutorApplication) {
    clearActionError();
    try {
      await withdrawApplication(application.applicationId);
    } catch {
      // banner đã hiển thị
    }
  }

  return (
    <div className="mp-page">
      <div className="mp-container">
        <header className="mp-header">
          <h1>Marketplace — Quản lý yêu cầu lớp</h1>
          <p>
            Theo dõi và xử lý các đơn ứng tuyển giữa phụ huynh và gia sư.
          </p>
        </header>

        {role !== 'CLIENT' && role !== 'TUTOR' && (
          <div className="mp-alert mp-alert--info">
            Tài khoản của bạn không thuộc nhóm phụ huynh hoặc gia sư. Marketplace chỉ hỗ trợ hai vai trò này.
          </div>
        )}

        {(role === 'CLIENT' || role === 'TUTOR') && (
          <div className="mp-tabs" role="tablist">
            {role === 'CLIENT' && (
              <button
                type="button"
                role="tab"
                aria-selected={view === 'CLIENT'}
                className={`mp-tab ${view === 'CLIENT' ? 'mp-tab--active' : ''}`}
                onClick={() => setViewOverride('CLIENT')}
              >
                Phụ huynh — Lớp của tôi
              </button>
            )}
            {role === 'TUTOR' && (
              <button
                type="button"
                role="tab"
                aria-selected={view === 'TUTOR'}
                className={`mp-tab ${view === 'TUTOR' ? 'mp-tab--active' : ''}`}
                onClick={() => setViewOverride('TUTOR')}
              >
                Gia sư — Đơn của tôi
              </button>
            )}
          </div>
        )}

        {actionError && (
          <div className="mp-alert mp-alert--error" role="alert">
            {actionError}
          </div>
        )}

        {view === 'CLIENT' && (
          <>
            {classesStatus === 'error' && (
              <div className="mp-alert mp-alert--error">{classesError}</div>
            )}
            <ClientView
              classes={myClasses}
              selectedClassId={selectedClassId}
              onSelectClass={(id) => {
                clearActionError();
                setSelectedClassId(id);
              }}
              applications={classApplications}
              applicationsStatus={classApplicationsStatus}
              applicationsError={classApplicationsError}
              mutatingId={mutatingId}
              onAccept={handleAccept}
              onReject={handleReject}
            />
          </>
        )}

        {view === 'TUTOR' && (
          <TutorView
            applications={filteredMyApplications}
            statusFilter={statusFilter}
            onChangeFilter={setStatusFilter}
            loading={myStatus === 'loading'}
            error={myError}
            mutatingId={mutatingId}
            onWithdraw={handleWithdraw}
          />
        )}

        {/* Empty state khi role không khớp — hook đã fetch rỗng */}
        {view === 'TUTOR' &&
          myStatus === 'success' &&
          filteredMyApplications.length === 0 &&
          myApplications.length === 0 && (
            <div className="mp-alert mp-alert--info">
              Bạn có thể ứng tuyển lớp từ Marketplace. Sau khi gửi, đơn sẽ hiện ở đây.
            </div>
          )}
      </div>
    </div>
  );
}