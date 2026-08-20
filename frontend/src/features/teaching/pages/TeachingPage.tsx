import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { SiteHeader } from '../../home/components/SiteHeader';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { hasRole } from '../../../shared/auth/rbac';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { WeeklyTimetable } from '../components/WeeklyTimetable';
import { LessonRequestDialog } from '../components/LessonRequestDialog';
import { ClassDetailModal } from '../components/ClassDetailModal';
import { ConfirmDialog } from '../../marketplace/components/ConfirmDialog';
import { ReviewFormModal } from '../../reviews/components/ReviewFormModal';
import { reviewApi } from '../../reviews/api/reviewApi';
import type { ReviewableAssignment } from '../../reviews/types/reviewTypes';
import { useTeaching } from '../hooks/useTeaching';
import { hhmmDisplay, toIsoDate } from '../../../shared/utils/format';
import {
  CLASS_STATUS_LABELS,
  REQUEST_STATUS_LABELS,
  type AssignmentResponse,
  type LessonResponse,
  type RescheduleRequestResponse,
} from '../types/teachingTypes';
import './TeachingPage.css';

const WEEKDAYS = ['Chủ nhật', 'Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7'];

const CLASS_STATUS_TONES: Record<string, string> = {
  OPEN: 'info',
  MATCHED: 'warning',
  IN_PROGRESS: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'muted',
  DISPUTED: 'danger',
  PENDING: 'warning',
  CLOSED: 'muted',
};

function classStatusTone(status?: string | null): string {
  return CLASS_STATUS_TONES[status ?? ''] ?? 'muted';
}

function classStatusLabel(status?: string | null): string {
  return status ? (CLASS_STATUS_LABELS[status] ?? status) : 'Chưa rõ trạng thái';
}

function formatDate(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  return `${WEEKDAYS[date.getDay()]}, ${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}/${y}`;
}

export default function TeachingPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const isClient = hasRole(user?.role, 'CLIENT');
  const {
    status,
    assignments,
    lessons,
    requests,
    notice,
    error,
    reload,
    decline,
    attend,
    requestReschedule,
    decideRequest,
    cancelRequest,
    confirmCompletion,
  } = useTeaching();

  const [dialog, setDialog] = useState<{ mode: 'RESCHEDULE'; lesson: LessonResponse } | null>(null);
  const [detailClassId, setDetailClassId] = useState<number | null>(null);
  const [reviewables, setReviewables] = useState<ReviewableAssignment[]>([]);
  const [activeReview, setActiveReview] = useState<ReviewableAssignment | null>(null);
  const [reviewNotice, setReviewNotice] = useState<string | null>(null);
  const loadReviewables = () => {
    if (!isClient) return;
    reviewApi
      .getReviewable()
      .then((r) => setReviewables(r.data))
      .catch(() => setReviewables([]));
  };
  useEffect(() => {
    loadReviewables();
  }, [isClient]);

  function openReviewByClass(classId: number) {
    const match = reviewables.find((a) => a.classId === classId);
    if (!match) {
      setReviewNotice('Chưa có buổi học nào diễn ra để đánh giá gia sư.');
      return;
    }
    if (!match.reviewable) {
      setReviewNotice('Bạn đã đánh giá đủ số buổi đã diễn ra của lớp này.');
      return;
    }
    setActiveReview(match);
  }

  const openReview = (lesson: LessonResponse) => openReviewByClass(lesson.classId);

  const [confirmAction, setConfirmAction] = useState<
    | { kind: 'decline'; assignmentId: number }
    | { kind: 'cancelReq'; requestId: number }
    | { kind: 'complete'; classId: number; classTitle: string }
    | null
  >(null);

  const invites = assignments.filter((a) => a.status === 'PENDING');
  const active = assignments.filter((a) => a.status === 'ACTIVE');

  // Chỉ hiển thị yêu cầu đổi lịch (tính năng "thêm buổi" đã bỏ).
  const rescheduleRequests = requests.filter((r) => r.requestType === 'RESCHEDULE');
  const openRequests = rescheduleRequests.filter((r) => r.status === 'PENDING');
  const historyRequests = rescheduleRequests.filter((r) => r.status !== 'PENDING');
  const pendingLessonIds = new Set(openRequests.flatMap((r) => r.lessonId ?? []));

  // Quota đánh giá tính theo lớp: mỗi buổi đã diễn ra cho phép 1 đánh giá.
  // Số đánh giá đã gửi được gán cho các buổi đã diễn ra sớm nhất của lớp đó.
  const reviewedLessonIds = useMemo(() => {
    const today = toIsoDate(new Date());
    const ids = new Set<number>();
    for (const a of reviewables) {
      if (a.reviewsSubmitted <= 0) continue;
      lessons
        .filter(
          (l) =>
            l.classId === a.classId && l.lessonDate <= today && l.attendanceStatus === 'COMPLETED',
        )
        .sort((x, y) =>
          x.lessonDate === y.lessonDate
            ? x.sequenceNo - y.sequenceNo
            : x.lessonDate.localeCompare(y.lessonDate),
        )
        .slice(0, a.reviewsSubmitted)
        .forEach((l) => ids.add(l.lessonId));
    }
    return ids;
  }, [reviewables, lessons]);

  const detailAssignment =
    detailClassId != null ? (assignments.find((a) => a.classId === detailClassId) ?? null) : null;
  const detailLessons =
    detailClassId != null ? lessons.filter((l) => l.classId === detailClassId) : [];
  const detailTitle =
    detailAssignment?.classTitle ?? detailLessons[0]?.classTitle ?? 'Chi tiết lớp';

  return (
    <div className="tch-page">
      <SiteHeader />
      <main className="tcs-container tch-main">
        <div className="tch-heading">
          <h1>{isClient ? 'Lịch học cá nhân' : 'Lịch dạy cá nhân'}</h1>
          <p>
            {isClient
              ? 'Theo dõi thời khóa biểu các lớp bạn đã đăng, sau khi gia sư nhận lớp.'
              : 'Nhận lớp được phụ huynh chọn, xem thời khóa biểu và điểm danh khi buổi học diễn ra.'}
          </p>
        </div>

        {notice && <div className="tch-alert tch-alert--ok">{notice}</div>}
        {error && <div className="tch-alert tch-alert--err">{error}</div>}

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
                      onSign={() =>
                        navigate(APP_ROUTES.signContract, {
                          state: { assignmentId: a.assignmentId },
                        })
                      }
                      onDecline={() =>
                        setConfirmAction({ kind: 'decline', assignmentId: a.assignmentId })
                      }
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

            {active.length > 0 && (
              <section className="tch-card">
                <h2>{isClient ? 'Lớp đang học' : 'Lớp đang dạy'}</h2>
                <ul className="tch-classes">
                  {active.map((a) => (
                    <li key={a.assignmentId} className="tch-class">
                      <div className="tch-class__info">
                        <div className="tch-class__topline">
                          <span className="tch-class__title">{a.classTitle}</span>
                          <span
                            className={`tch-class-status tch-class-status--${classStatusTone(
                              a.classStatus,
                            )}`}
                          >
                            {classStatusLabel(a.classStatus)}
                          </span>
                        </div>
                        <span className="tch-class__meta">
                          {isClient && a.tutorName ? `👩‍🏫 ${a.tutorName} · ` : ''}
                          {(a.subjectNames ?? []).join(', ') || '—'} · {a.lessonCount} buổi
                        </span>
                      </div>
                      <div className="tch-class__action">
                        <CompletionCell
                          assignment={a}
                          onTutorConfirm={() =>
                            setConfirmAction({
                              kind: 'complete',
                              classId: a.classId,
                              classTitle: a.classTitle,
                            })
                          }
                          onClientReview={() => openReviewByClass(a.classId)}
                        />
                      </div>
                    </li>
                  ))}
                </ul>
              </section>
            )}

            {(openRequests.length > 0 || historyRequests.length > 0) && (
              <section className="tch-card">
                <h2>
                  Yêu cầu đổi lịch{' '}
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
                      onCancel={() =>
                        setConfirmAction({ kind: 'cancelReq', requestId: r.requestId })
                      }
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

            <section className="tch-card">
              <div className="tch-card__head">
                <h2>Thời khóa biểu</h2>
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
                  onAttend={(id, present) => void attend(id, present)}
                  onReschedule={(lesson) => setDialog({ mode: 'RESCHEDULE', lesson })}
                  onOpenDetail={(lesson) => setDetailClassId(lesson.classId)}
                  onReview={isClient ? openReview : undefined}
                  pendingLessonIds={pendingLessonIds}
                  reviewedLessonIds={reviewedLessonIds}
                />
              )}
            </section>
          </>
        )}

        {dialog?.mode === 'RESCHEDULE' && (
          <LessonRequestDialog
            lesson={dialog.lesson}
            onClose={() => setDialog(null)}
            submitError={error}
            existingLessons={lessons}
            onSubmit={(payload) => requestReschedule(dialog.lesson.lessonId, payload)}
          />
        )}
        {detailClassId != null && (
          <ClassDetailModal
            assignment={detailAssignment}
            lessons={detailLessons}
            classTitle={detailTitle}
            isClient={isClient}
            onClose={() => setDetailClassId(null)}
          />
        )}

        {activeReview && (
          <ReviewFormModal
            assignment={activeReview}
            onClose={() => setActiveReview(null)}
            onSubmitted={() => {
              setActiveReview(null);
              loadReviewables();
              // Sau khi đánh giá, nếu gia sư đã yêu cầu hoàn thành thì lớp đã đóng -> làm mới danh sách.
              void reload();
            }}
          />
        )}

        {reviewNotice && (
          <ConfirmDialog
            title="Đánh giá gia sư"
            message={reviewNotice}
            onClose={() => setReviewNotice(null)}
          />
        )}

        {confirmAction?.kind === 'decline' && (
          <ConfirmDialog
            title="Từ chối lớp"
            message="Từ chối lớp này? Lớp sẽ được mở lại cho gia sư khác."
            confirmLabel="Từ chối"
            cancelLabel="Hủy"
            onConfirm={() => {
              const id = confirmAction.assignmentId;
              setConfirmAction(null);
              void decline(id);
            }}
            onClose={() => setConfirmAction(null)}
          />
        )}

        {confirmAction?.kind === 'cancelReq' && (
          <ConfirmDialog
            title="Thu hồi yêu cầu"
            message="Thu hồi yêu cầu này?"
            confirmLabel="Thu hồi"
            cancelLabel="Hủy"
            onConfirm={() => {
              const id = confirmAction.requestId;
              setConfirmAction(null);
              void cancelRequest(id);
            }}
            onClose={() => setConfirmAction(null)}
          />
        )}

        {confirmAction?.kind === 'complete' && (
          <ConfirmDialog
            title="Hoàn thành lớp"
            message={`Đánh dấu lớp "${confirmAction.classTitle}" đã hoàn thành và mời học viên đánh giá gia sư? Lớp sẽ đóng và học phí escrow được giải ngân cho bạn sau khi học viên đánh giá.`}
            confirmLabel="Hoàn thành lớp"
            cancelLabel="Hủy"
            onConfirm={() => {
              const id = confirmAction.classId;
              setConfirmAction(null);
              void confirmCompletion(id);
            }}
            onClose={() => setConfirmAction(null)}
          />
        )}
      </main>
    </div>
  );
}

/** Ô hành động "Hoàn thành lớp" theo trạng thái của người dùng hiện tại. */
function CompletionCell({
  assignment: a,
  onTutorConfirm,
  onClientReview,
}: {
  readonly assignment: AssignmentResponse;
  readonly onTutorConfirm: () => void;
  readonly onClientReview: () => void;
}) {
  // Lớp đã hoàn thành thì không bao giờ hiện nút đánh giá, kể cả khi completionState chưa cập nhật kịp
  if (a.classStatus === 'COMPLETED') {
    return <span className="tch-badge tch-badge--done">✓ Đã hoàn thành</span>;
  }
  switch (a.completionState) {
    case 'COMPLETED':
      return <span className="tch-badge tch-badge--done">✓ Đã hoàn thành</span>;
    case 'TUTOR_CAN_CONFIRM':
      return (
        <button className="tch-btn tch-btn--primary" type="button" onClick={onTutorConfirm}>
          Hoàn thành lớp
        </button>
      );
    case 'TUTOR_BLOCKED':
      return (
        <span className="tch-class__hint" title={a.completionBlockedReason ?? undefined}>
          Chưa đủ điều kiện hoàn thành
        </span>
      );
    case 'TUTOR_WAITING':
      return <span className="tch-badge tch-badge--pending">Chờ học viên đánh giá</span>;
    case 'CLIENT_WAITING_TUTOR':
      return <span className="tch-class__hint">Chờ gia sư hoàn thành lớp</span>;
    case 'CLIENT_MUST_REVIEW':
      return (
        <button className="tch-btn tch-btn--primary" type="button" onClick={onClientReview}>
          Đánh giá gia sư để hoàn thành
        </button>
      );
    default:
      return null;
  }
}

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
  return (
    <li className={`tch-req tch-req--${r.status.toLowerCase()}`}>
      <div className="tch-req__body">
        <div className="tch-req__top">
          <span className="tch-req__type">Đổi lịch</span>
          <span className={`tch-badge tch-badge--${r.status.toLowerCase()}`}>
            {REQUEST_STATUS_LABELS[r.status]}
          </span>
        </div>
        <h3 className="tch-req__title">{r.classTitle}</h3>
        <p className="tch-req__when">
          {r.oldDate ? (
            <>
              <s>
                {formatDate(r.oldDate)} ({hhmmDisplay(r.oldStartTime)}–{hhmmDisplay(r.oldEndTime)})
              </s>{' '}
              →{' '}
            </>
          ) : null}
          <strong>
            {formatDate(r.newDate)} ({hhmmDisplay(r.newStartTime)}–{hhmmDisplay(r.newEndTime)})
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
  onSign,
  onDecline,
  isClient,
}: {
  readonly assignment: AssignmentResponse;
  readonly isClient: boolean;
  readonly onSign: () => void;
  readonly onDecline: () => void;
}) {
  const iSigned = isClient ? !!a.clientSignedAt : !!a.tutorSignedAt;
  const otherSigned = isClient ? !!a.tutorSignedAt : !!a.clientSignedAt;
  const bothSigned = !!a.clientSignedAt && !!a.tutorSignedAt;
  let badge: string;
  if (bothSigned) {
    badge = isClient
      ? 'Đã ký — chờ thanh toán'
      : 'Đã ký — chờ phụ huynh thanh toán';
  } else if (iSigned) {
    badge = otherSigned
      ? 'Đã ký — hoàn tất'
      : `Đã ký — chờ ${isClient ? 'gia sư' : 'phụ huynh'} ký`;
  } else if (isClient) {
    badge = 'Mời bạn ký hợp đồng';
  } else {
    badge = 'Lời mời nhận lớp';
  }

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
        <div className="tch-invite__badges">
          <span className="tch-badge tch-badge--pending">{badge}</span>
          <span className={`tch-class-status tch-class-status--${classStatusTone(a.classStatus)}`}>
            {classStatusLabel(a.classStatus)}
          </span>
        </div>
      </div>
      <div className="tch-invite__actions">
        {!iSigned && (
          <button className="tch-btn tch-btn--primary" type="button" onClick={onSign}>
            {isClient ? 'Ký hợp đồng' : 'Nhận lớp & ký hợp đồng'}
          </button>
        )}
        {iSigned && (
          <button className="tch-btn tch-btn--ghost" type="button" onClick={onSign}>
            {bothSigned && isClient ? 'Thanh toán' : 'Xem hợp đồng'}
          </button>
        )}
        {!isClient && !iSigned && (
          <button className="tch-btn tch-btn--ghost" type="button" onClick={onDecline}>
            Từ chối
          </button>
        )}
      </div>
    </li>
  );
}
