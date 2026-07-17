import { SiteHeader } from '../../home/components/SiteHeader';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { hasRole } from '../../../shared/auth/rbac';
import { WeeklyTimetable } from '../components/WeeklyTimetable';
import { useTeaching } from '../hooks/useTeaching';
import { ASSIGNMENT_STATUS_LABELS, type AssignmentResponse } from '../types/teachingTypes';
import './TeachingPage.css';

const WEEKDAYS = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];

/** '2026-07-17' → 'Thứ 6, 17/07/2026'. Tự parse để khỏi lệch múi giờ. */
function formatDate(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  return `${WEEKDAYS[date.getDay()]}, ${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}/${y}`;
}

export default function TeachingPage() {
  const { user } = useAuth();
  // Cùng một màn: gia sư nhận lớp + điểm danh, Client chỉ theo dõi lịch lớp của mình.
  const isClient = hasRole(user?.role, 'CLIENT');
  const { status, assignments, lessons, notice, error, reload, accept, decline, checkIn, checkOut } =
    useTeaching();

  const invites = assignments.filter((a) => a.status === 'PENDING');
  const active = assignments.filter((a) => a.status === 'ACTIVE');

  return (
    <div className="tch-page">
      <SiteHeader />
      <main className="tcs-container tch-main">
        <div className="tch-heading">
          <h1>{isClient ? 'Lịch học của tôi' : 'Lịch dạy của tôi'}</h1>
          <p>
            {isClient
              ? 'Theo dõi thời khóa biểu các lớp bạn đã đăng, sau khi gia sư nhận lớp.'
              : 'Nhận lớp được phụ huynh chọn, xem thời khóa biểu và điểm danh khi buổi học diễn ra.'}
          </p>
        </div>

        {notice && <div className="tch-alert tch-alert--ok">{notice}</div>}
        {error && <div className="tch-alert tch-alert--err">{error}</div>}

        {status === 'loading' && <div className="tch-state">Đang tải…</div>}
        {status === 'error' && (
          <div className="tch-state">
            <p>Không tải được thời khóa biểu.</p>
            <button className="tch-btn tch-btn--primary" type="button" onClick={() => void reload()}>
              Thử lại
            </button>
          </div>
        )}

        {status === 'success' && (
          <>
            {/* --- Chờ gia sư nhận lớp --- */}
            {invites.length > 0 && (
              <section className="tch-card">
                <h2>
                  {isClient ? 'Đang chờ gia sư nhận lớp' : 'Lời mời nhận lớp'}{' '}
                  <span className="tch-count">{invites.length}</span>
                </h2>
                <ul className="tch-invites">
                  {invites.map((a) => (
                    <InviteCard
                      key={a.assignmentId}
                      assignment={a}
                      isClient={isClient}
                      onAccept={() => void accept(a.assignmentId)}
                      onDecline={() => {
                        if (window.confirm('Từ chối lớp này? Lớp sẽ được mở lại cho gia sư khác.')) {
                          void decline(a.assignmentId);
                        }
                      }}
                    />
                  ))}
                </ul>
              </section>
            )}
            {invites.length === 0 && !isClient && (
              <section className="tch-card">
                <h2>Lời mời nhận lớp</h2>
                <p className="tch-muted">
                  Chưa có lời mời nào. Khi phụ huynh chọn bạn cho một lớp, lời mời sẽ hiện ở đây.
                </p>
              </section>
            )}

            {/* --- Lớp đang chạy --- */}
            {active.length > 0 && (
              <section className="tch-card">
                <h2>{isClient ? 'Lớp đang học' : 'Lớp đang dạy'}</h2>
                <ul className="tch-classes">
                  {active.map((a) => (
                    <li key={a.assignmentId} className="tch-class">
                      <span className="tch-class__title">{a.classTitle}</span>
                      <span className="tch-class__meta">
                        {isClient && a.tutorName ? `👩‍🏫 ${a.tutorName} · ` : ''}
                        {(a.subjectNames ?? []).join(', ') || '—'} · {a.lessonCount} buổi
                      </span>
                    </li>
                  ))}
                </ul>
              </section>
            )}

            {/* --- Thời khóa biểu --- */}
            <section className="tch-card">
              <h2>Thời khóa biểu</h2>
              {lessons.length === 0 ? (
                <p className="tch-muted">
                  {isClient
                    ? 'Chưa có buổi học nào. Thời khóa biểu sẽ hiện sau khi gia sư nhận lớp.'
                    : 'Chưa có buổi học nào. Lịch sẽ được tạo ngay sau khi bạn nhận lớp.'}
                </p>
              ) : (
                <WeeklyTimetable
                  lessons={lessons}
                  readOnly={isClient}
                  onCheckIn={(id) => void checkIn(id)}
                  onCheckOut={(id) => void checkOut(id)}
                />
              )}
            </section>
          </>
        )}
      </main>
    </div>
  );
}

function InviteCard({
  assignment: a,
  onAccept,
  onDecline,
  isClient,
}: {
  readonly assignment: AssignmentResponse;
  readonly isClient: boolean;
  readonly onAccept: () => void;
  readonly onDecline: () => void;
}) {
  return (
    <li className="tch-invite">
      <div className="tch-invite__body">
        <h3 className="tch-invite__title">{a.classTitle}</h3>
        <div className="tch-invite__meta">
          {isClient && a.tutorName && <span>👩‍🏫 {a.tutorName}</span>}
          <span>📚 {(a.subjectNames ?? []).join(', ') || '—'}</span>
          {a.gradeName && <span>🎓 {a.gradeName}</span>}
          <span>📍 {a.lessonMode === 'ONLINE' ? 'Online' : (a.address ?? 'Offline')}</span>
        </div>
        {a.startDate && a.endDate && (
          <p className="tch-invite__dates">
            🗓️ Từ {formatDate(a.startDate)} đến {formatDate(a.endDate)}
          </p>
        )}
        <span className="tch-badge tch-badge--pending">
          {isClient ? 'Chờ gia sư nhận lớp' : ASSIGNMENT_STATUS_LABELS[a.status]}
        </span>
      </div>
      {/* Nhận/từ chối là quyền của gia sư — Client chỉ theo dõi. */}
      {!isClient && (
        <div className="tch-invite__actions">
          <button className="tch-btn tch-btn--primary" type="button" onClick={onAccept}>
            Nhận lớp
          </button>
          <button className="tch-btn tch-btn--ghost" type="button" onClick={onDecline}>
            Từ chối
          </button>
        </div>
      )}
    </li>
  );
}

