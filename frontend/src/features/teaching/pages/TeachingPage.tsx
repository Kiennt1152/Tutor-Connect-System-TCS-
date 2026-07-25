import { useMemo, useState } from 'react';
import { SiteHeader } from '../../home/components/SiteHeader';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { hasRole } from '../../../shared/auth/rbac';
import { WeeklyTimetable } from '../components/WeeklyTimetable';
import { LessonRequestDialog } from '../components/LessonRequestDialog';
import { useTeaching } from '../hooks/useTeaching';
import { hhmm } from '../../../shared/utils/format';
import {
  ASSIGNMENT_STATUS_LABELS,
  REQUEST_STATUS_LABELS,
  REQUEST_TYPE_LABELS,
  classOptionsFrom,
  type AssignmentResponse,
  type LessonResponse,
  type RescheduleRequestResponse,
} from '../types/teachingTypes';
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
  const {
    status,
    assignments,
    lessons,
    requests,
    notice,
    error,
    reload,
    accept,
    decline,
    attend,
    requestReschedule,
    requestExtraLesson,
    decideRequest,
    cancelRequest,
  } = useTeaching();

  // null = đóng; {mode:'EXTRA'} = thêm buổi; {mode:'RESCHEDULE', lesson} = dời buổi đó.
  const [dialog, setDialog] = useState<
    { mode: 'EXTRA' } | { mode: 'RESCHEDULE'; lesson: LessonResponse } | null
  >(null);

  const invites = assignments.filter((a) => a.status === 'PENDING');
  const active = assignments.filter((a) => a.status === 'ACTIVE');

  const openRequests = requests.filter((r) => r.status === 'PENDING');
  const historyRequests = requests.filter((r) => r.status !== 'PENDING');
  const classOptions = useMemo(() => classOptionsFrom(lessons), [lessons]);
  const pendingLessonIds = new Set(openRequests.flatMap((r) => r.lessonId ?? []));

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

        {/* Thông báo lời mời — gia sư dễ bỏ sót nếu chỉ nằm lẫn trong danh sách bên dưới. */}
        {status === 'success' && !isClient && invites.length > 0 && (
          <div className="tch-notify" role="status">
            <span className="tch-notify__icon" aria-hidden="true">
              🔔
            </span>
            <div className="tch-notify__body">
              <strong>
                Bạn có {invites.length} lời mời nhận lớp đang chờ
              </strong>
              <span>
                {invites.map((a) => a.classTitle).join(' · ')}
              </span>
            </div>
            <a className="tch-btn tch-btn--primary" href="#tch-invites">
              Xem lời mời
            </a>
          </div>
        )}

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
              <section className="tch-card" id="tch-invites">
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

            {(openRequests.length > 0 || historyRequests.length > 0) && (
              <section className="tch-card">
                <h2>
                  Yêu cầu đổi lịch / thêm buổi{' '}
                  {openRequests.length > 0 && <span className="tch-count">{openRequests.length}</span>}
                </h2>
                <ul className="tch-reqs">
                  {openRequests.map((r) => (
                    <RequestCard
                      key={r.requestId}
                      request={r}
                      onApprove={() => void decideRequest(r.requestId, true)}
                      onReject={() => {
                        const note = window.prompt('Lý do từ chối (không bắt buộc):') ?? undefined;
                        void decideRequest(r.requestId, false, note);
                      }}
                      onCancel={() => {
                        if (window.confirm('Thu hồi yêu cầu này?')) void cancelRequest(r.requestId);
                      }}
                    />
                  ))}
                </ul>
                {historyRequests.length > 0 && (
                  <details className="tch-reqs__history">
                    <summary>Yêu cầu đã xử lý ({historyRequests.length})</summary>
                    <ul className="tch-reqs">
                      {historyRequests.map((r) => (
                        <RequestCard key={r.requestId} request={r} />
                      ))}
                    </ul>
                  </details>
                )}
              </section>
            )}

            {/* --- Thời khóa biểu --- */}
            <section className="tch-card">
              <div className="tch-card__head">
                <h2>Thời khóa biểu</h2>
                {/* Cả hai bên đều xin thêm buổi được — lịch chỉ đổi khi bên kia duyệt. */}
                {classOptions.length > 0 && (
                  <button
                    className="tch-btn tch-btn--primary"
                    type="button"
                    onClick={() => setDialog({ mode: 'EXTRA' })}
                  >
                    + Thêm buổi
                  </button>
                )}
              </div>
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
                  onAttend={(id) => void attend(id)}
                  onReschedule={(lesson) => setDialog({ mode: 'RESCHEDULE', lesson })}
                  pendingLessonIds={pendingLessonIds}
                />
              )}
            </section>
          </>
        )}

        {dialog?.mode === 'RESCHEDULE' && (
          <LessonRequestDialog
            mode="RESCHEDULE"
            lesson={dialog.lesson}
            onClose={() => setDialog(null)}
            onSubmit={(payload) => requestReschedule(dialog.lesson.lessonId, payload)}
          />
        )}
        {dialog?.mode === 'EXTRA' && (
          <LessonRequestDialog
            mode="EXTRA"
            classes={classOptions}
            onClose={() => setDialog(null)}
            onSubmit={requestExtraLesson}
          />
        )}
      </main>
    </div>
  );
}

/** Một yêu cầu đổi lịch/thêm buổi. Nút duyệt/từ chối chỉ hiện với bên phải quyết định. */
function RequestCard({
  request: r,
  onApprove,
  onReject,
  onCancel,
}: {
  readonly request: RescheduleRequestResponse;
  readonly onApprove?: () => void;
  readonly onReject?: () => void;
  readonly onCancel?: () => void;
}) {
  const isReschedule = r.requestType === 'RESCHEDULE';
  return (
    <li className={`tch-req tch-req--${r.status.toLowerCase()}`}>
      <div className="tch-req__body">
        <div className="tch-req__top">
          <span className="tch-req__type">{REQUEST_TYPE_LABELS[r.requestType]}</span>
          <span className={`tch-badge tch-badge--${r.status.toLowerCase()}`}>
            {REQUEST_STATUS_LABELS[r.status]}
          </span>
        </div>
        <h3 className="tch-req__title">{r.classTitle}</h3>
        <p className="tch-req__when">
          {isReschedule && r.oldDate ? (
            <>
              <s>
                {formatDate(r.oldDate)} ({hhmm(r.oldStartTime)}–{hhmm(r.oldEndTime)})
              </s>{' '}
              →{' '}
            </>
          ) : null}
          <strong>
            {formatDate(r.newDate)} ({hhmm(r.newStartTime)}–{hhmm(r.newEndTime)})
          </strong>
          {r.subjectName ? ` · ${r.subjectName}` : ''}
        </p>
        <p className="tch-req__meta">
          Người gửi: {r.requestedByName}
          {r.reason ? ` · Lý do: ${r.reason}` : ''}
        </p>
        {r.decidedByName && (
          <p className="tch-req__meta">
            {r.status === 'APPROVED' ? 'Đã duyệt' : 'Đã từ chối'} bởi {r.decidedByName}
            {r.decisionNote ? ` · ${r.decisionNote}` : ''}
          </p>
        )}
      </div>
      {(r.canDecide || r.canCancel) && (
        <div className="tch-req__actions">
          {r.canDecide && (
            <>
              <button className="tch-btn tch-btn--primary" type="button" onClick={onApprove}>
                Duyệt
              </button>
              <button className="tch-btn tch-btn--ghost" type="button" onClick={onReject}>
                Từ chối
              </button>
            </>
          )}
          {r.canCancel && (
            <button className="tch-btn tch-btn--ghost" type="button" onClick={onCancel}>
              Thu hồi
            </button>
          )}
        </div>
      )}
    </li>
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

