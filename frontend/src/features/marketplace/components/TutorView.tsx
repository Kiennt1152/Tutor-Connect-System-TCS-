import { ApplicationRow } from './ApplicationRow';
import type {
  TutorApplication,
  TutorApplicationStatus,
} from '../types/marketplaceTypes';
import { APPLICATION_STATUS_LABELS } from '../types/marketplaceTypes';

type Props = {
  applications: TutorApplication[];
  statusFilter: TutorApplicationStatus | 'ALL';
  onChangeFilter: (next: TutorApplicationStatus | 'ALL') => void;
  loading: boolean;
  error: string | null;
  mutatingId: number | null;
  onWithdraw: (application: TutorApplication) => Promise<void> | void;
};

const FILTERS: Array<TutorApplicationStatus | 'ALL'> = [
  'ALL',
  'SUBMITTED',
  'ACCEPTED',
  'REJECTED',
  'WITHDRAWN',
];

export function TutorView({
  applications,
  statusFilter,
  onChangeFilter,
  loading,
  error,
  mutatingId,
  onWithdraw,
}: Props) {
  return (
    <div className="mp-application-panel">
      <header className="mp-application-panel__head">
        <h2>Đơn ứng tuyển của tôi</h2>
      </header>

      <div className="mp-chips" role="tablist">
        {FILTERS.map((filter) => (
          <button
            key={filter}
            type="button"
            role="tab"
            aria-selected={statusFilter === filter}
            className={`mp-chip ${statusFilter === filter ? 'mp-chip--active' : ''}`}
            onClick={() => onChangeFilter(filter)}
          >
            {filter === 'ALL' ? 'Tất cả' : APPLICATION_STATUS_LABELS[filter]}
          </button>
        ))}
      </div>

      {loading && <div className="mp-empty">Đang tải…</div>}
      {error && <div className="mp-alert mp-alert--error">{error}</div>}
      {!loading && !error && applications.length === 0 && (
        <div className="mp-empty">Bạn chưa gửi đơn ứng tuyển nào.</div>
      )}

      {!loading && !error && applications.length > 0 && (
        <div className="mp-application-list">
          {applications.map((application) => (
            <ApplicationRow
              key={application.applicationId}
              application={application}
              busy={mutatingId === application.applicationId}
              showWithdraw
              onWithdraw={onWithdraw}
            />
          ))}
        </div>
      )}
    </div>
  );
}