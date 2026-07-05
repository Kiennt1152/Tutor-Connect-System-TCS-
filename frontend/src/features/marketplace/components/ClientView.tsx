import { useState } from 'react';
import { ApplicationRow } from './ApplicationRow';
import { RejectApplicationModal } from './RejectApplicationModal';
import type { ClassSummary, TutorApplication } from '../types/marketplaceTypes';

type Props = {
  classes: ClassSummary[];
  selectedClassId: number | null;
  onSelectClass: (classId: number) => void;
  applications: TutorApplication[];
  applicationsStatus: 'idle' | 'loading' | 'success' | 'error';
  applicationsError: string | null;
  mutatingId: number | null;
  onAccept: (application: TutorApplication) => Promise<void> | void;
  onReject: (application: TutorApplication) => Promise<void> | void;
};

export function ClientView({
  classes,
  selectedClassId,
  onSelectClass,
  applications,
  applicationsStatus,
  applicationsError,
  mutatingId,
  onAccept,
  onReject,
}: Props) {
  const [rejecting, setRejecting] = useState<TutorApplication | null>(null);

  const selectedClass = classes.find((c) => c.classId === selectedClassId);

  return (
    <div className="mp-client">
      <aside className="mp-class-list" aria-label="Lớp của tôi">
        <h3>Lớp của tôi</h3>
        {classes.length === 0 && (
          <div className="mp-empty" style={{ padding: 'var(--space-md)' }}>
            Bạn chưa đăng lớp nào.
          </div>
        )}
        {classes.map((c) => (
          <button
            key={c.classId}
            type="button"
            className={`mp-class-item ${
              c.classId === selectedClassId ? 'mp-class-item--active' : ''
            }`}
            onClick={() => onSelectClass(c.classId)}
          >
            <span className="mp-class-item__title">{c.title}</span>
            <span className="mp-class-item__meta">Trạng thái: {c.status}</span>
          </button>
        ))}
      </aside>

      <section className="mp-application-panel">
        <header className="mp-application-panel__head">
          <h2>
            {selectedClass
              ? `Đơn ứng tuyển — ${selectedClass.title}`
              : 'Chọn một lớp để xem đơn ứng tuyển'}
          </h2>
        </header>

        {!selectedClassId && (
          <div className="mp-empty">Chọn lớp ở danh sách bên trái để bắt đầu.</div>
        )}

        {selectedClassId && applicationsStatus === 'loading' && (
          <div className="mp-empty">Đang tải đơn ứng tuyển…</div>
        )}

        {selectedClassId && applicationsStatus === 'error' && (
          <div className="mp-alert mp-alert--error">{applicationsError}</div>
        )}

        {selectedClassId &&
          applicationsStatus === 'success' &&
          applications.length === 0 && (
            <div className="mp-empty">Chưa có gia sư nào ứng tuyển lớp này.</div>
          )}

        {selectedClassId && applicationsStatus === 'success' && (
          <div className="mp-application-list">
            {applications.map((application) => (
              <ApplicationRow
                key={application.applicationId}
                application={application}
                busy={mutatingId === application.applicationId}
                showReviewActions
                onAccept={onAccept}
                onReject={(app) => setRejecting(app)}
              />
            ))}
          </div>
        )}
      </section>

      {rejecting && (
        <RejectApplicationModal
          onCancel={() => setRejecting(null)}
          onConfirm={async () => {
            try {
              await onReject(rejecting);
              setRejecting(null);
            } catch {
              // Lỗi đã được hook hiển thị ở banner
            }
          }}
          busy={mutatingId === rejecting.applicationId}
        />
      )}
    </div>
  );
}