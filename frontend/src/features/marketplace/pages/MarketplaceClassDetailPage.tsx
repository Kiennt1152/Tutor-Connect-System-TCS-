import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import axios from 'axios';
import { VerificationHeader } from '../../../shared/components/VerificationHeader';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { ClassIssueModal } from '../../dispute/components/ClassIssueModal';
import { marketplaceApi } from '../api/marketplaceApi';
import { APP_ROUTES } from '../../../shared/constants/routes';
import { useDependentLinkStatus } from '../../profile/hooks/useDependentProfile';
import { ChatButton } from '../../messaging/components/ChatButton';
import { ClassTerminationModal } from '../components/ClassTerminationModal';
import type { LessonMode, MarketplaceClass, RecurringType } from '../types/marketplaceTypes';
import './MarketplacePage.css';

const LESSON_MODE_LABELS: Record<LessonMode, string> = {
  ONLINE: 'Trực tuyến',
  OFFLINE: 'Trực tiếp',
  HYBRID: 'Kết hợp',
};
const RECURRING_LABELS: Record<RecurringType, string> = {
  DAILY: 'Hằng ngày',
  WEEKLY: 'Hằng tuần',
  ONCE: 'Một lần',
};
const DAY_LABELS: Record<number, string> = {
  1: 'Thứ Hai',
  2: 'Thứ Ba',
  3: 'Thứ Tư',
  4: 'Thứ Năm',
  5: 'Thứ Sáu',
  6: 'Thứ Bảy',
  7: 'Chủ Nhật',
};

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

function formatCurrency(value: number): string {
  return `${new Intl.NumberFormat('vi-VN').format(value)} đ`;
}

export default function MarketplaceClassDetailPage() {
  const { classId } = useParams<{ classId: string }>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { user } = useAuth();

  const [data, setData] = useState<MarketplaceClass | null>(null);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [loadError, setLoadError] = useState('');

  const [regStatus, setRegStatus] = useState<'idle' | 'loading' | 'done' | 'error'>('idle');
  const [regMessage, setRegMessage] = useState('');
  const [terminationModalOpen, setTerminationModalOpen] = useState(false);
  const [issueModalOpen, setIssueModalOpen] = useState(false);
  const [completing, setCompleting] = useState(false);
  const [completeMsg, setCompleteMsg] = useState<{ tone: 'ok' | 'error'; text: string } | null>(null);

  const load = useCallback(() => {
    if (!classId) return;
    const assignmentId = Number(searchParams.get('assignmentId'));
    const classStudentId = Number(searchParams.get('classStudentId'));
    setStatus('loading');
    marketplaceApi
      .getClass(Number(classId), {
        assignmentId: Number.isFinite(assignmentId) && assignmentId > 0 ? assignmentId : undefined,
        classStudentId: Number.isFinite(classStudentId) && classStudentId > 0 ? classStudentId : undefined,
      })
      .then((res) => {
        setData(res.data);
        setStatus('success');
      })
      .catch((err) => {
        setLoadError(extractError(err, 'Không tải được chi tiết lớp.'));
        setStatus('error');
      });
  }, [classId, searchParams]);

  useEffect(() => {
    load();
  }, [load]);

  const register = async () => {
    if (!data) return;
    setRegStatus('loading');
    setRegMessage('');
    try {
      const res = await marketplaceApi.register(data.classId);
      setRegStatus('done');
      setRegMessage(res.data?.message ?? 'Đăng ký thành công');
      load(); // cập nhật sĩ số / trạng thái
    } catch (err) {
      setRegStatus('error');
      setRegMessage(extractError(err, 'Đăng ký thất bại.'));
    }
  };

  const submitCompletion = async () => {
    if (!data) return;
    setCompleting(true);
    setCompleteMsg(null);
    try {
      const res = await marketplaceApi.confirmCompletion(data.classId);
      setCompleteMsg({ tone: 'ok', text: res.data?.message ?? 'Đã ghi nhận.' });
      load();
    } catch (err) {
      setCompleteMsg({ tone: 'error', text: extractError(err, 'Không xác nhận được. Vui lòng thử lại.') });
    } finally {
      setCompleting(false);
    }
  };

  const role = user?.role;
  const isClient = role === 'CLIENT';
  const isTutor = role === 'TUTOR';
  const canRequestTermination = Boolean(data?.canRequestTermination);
  const completionState = data?.completionState ?? 'NONE';
  const showCompletionCard = completionState !== 'NONE';

  // Điều hướng cho học viên vị thành niên (child) — backend uỷ quyền hợp đồng/thanh toán cho phụ huynh.
  const { linkStatus: depLinkStatus } = useDependentLinkStatus(isClient);
  const needsDob = isClient && Boolean(depLinkStatus?.dateOfBirthMissing);
  const minorNeedsGuardian =
    isClient &&
    Boolean(depLinkStatus?.minorAccount) &&
    Boolean(depLinkStatus?.guardianRequired) &&
    !depLinkStatus?.guardianLinked;
  const delegatedToParent =
    isClient && Boolean(depLinkStatus?.legalProceduresDelegatedToParent);
  const legalHolderName = depLinkStatus?.legalAccountHolderName;

  const isOpen = data?.status === 'OPEN';
  const sortedSchedule = data
    ? [...data.schedule].sort((a, b) => a.dayOfWeek - b.dayOfWeek || a.startTime.localeCompare(b.startTime))
    : [];

  return (
    <>
      <VerificationHeader />
      <div className="mk-page">
        <button className="mk-back" type="button" onClick={() => navigate('/marketplace')}>
          ← Quay lại Tìm lớp
        </button>

        {status === 'loading' && <div className="mk-state">Đang tải chi tiết lớp…</div>}
        {status === 'error' && <div className="mk-state">{loadError}</div>}

        {status === 'success' && data && (
          <div className="mk-detail">
            <div className="mk-detail__main">
              <div className="mk-detail__titlebar">
                <h1 className="mk-detail__title">{data.title}</h1>
                <span className={`mk-status mk-status--${data.status.toLowerCase()}`}>
                  {isOpen ? 'Đang mở đăng ký' : data.status === 'MATCHED' ? 'Đã đủ / đã khớp' : data.status}
                </span>
              </div>

              <div className="mk-tags">
                {data.subjectName && <span className="mk-tag">{data.subjectName}</span>}
                {data.gradeName && <span className="mk-tag">{data.gradeName}</span>}
                <span className="mk-tag">{LESSON_MODE_LABELS[data.lessonMode]}</span>
                <span className="mk-tag">{RECURRING_LABELS[data.recurringType]}</span>
              </div>

              {data.description && <p className="mk-detail__desc">{data.description}</p>}

              <div className="mk-detail__section">
                <h3 className="mk-detail__h">Thông tin lớp</h3>
                <dl className="mk-detail__grid">
                  <div>
                    <dt>Thời gian</dt>
                    <dd>
                      {data.startDate} → {data.endDate}
                    </dd>
                  </div>
                  <div>
                    <dt>Số buổi</dt>
                    <dd>{data.numberOfSessions}</dd>
                  </div>
                  <div>
                    <dt>Học phí</dt>
                    <dd className="mk-fee">{formatCurrency(data.tuitionFee)}</dd>
                  </div>
                  <div>
                    <dt>Số học sinh tối đa</dt>
                    <dd>{data.maxStudents ?? '—'}</dd>
                  </div>
                  <div>
                    <dt>Người tạo</dt>
                    <dd>{data.creatorName ?? '—'}</dd>
                  </div>
                </dl>
              </div>

              <div className="mk-detail__section">
                <h3 className="mk-detail__h">Lịch học</h3>
                {sortedSchedule.length === 0 ? (
                  <p className="mk-muted">Chưa có lịch.</p>
                ) : (
                  <ul className="mk-sched">
                    {sortedSchedule.map((s) => (
                      <li className="mk-sched__item" key={s.slotId}>
                        <span className="mk-sched__day">{DAY_LABELS[s.dayOfWeek] ?? `Thứ ${s.dayOfWeek}`}</span>
                        <span className="mk-sched__time">
                          {s.startTime.slice(0, 5)} – {s.endTime.slice(0, 5)}
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>

            <aside className="mk-detail__side">
              <div className="mk-enroll">
                <div className="mk-enroll__head">
                  <span className="mk-enroll__title">Đăng ký lớp</span>
                  {data.maxStudents != null && (
                    <span className="mk-enroll__cap">Tối đa {data.maxStudents} học sinh</span>
                  )}
                </div>

                {regStatus === 'done' ? (
                  <div className="mk-alert mk-alert--ok">{regMessage}</div>
                ) : (
                  <>
                    {regStatus === 'error' && (
                      <div className="mk-alert mk-alert--error">{regMessage}</div>
                    )}
                    {isClient && needsDob ? (
                      // Backend yêu cầu ngày sinh để xác định <18; thiếu -> điều hướng cập nhật hồ sơ.
                      <div className="mk-alert mk-alert--warn" style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                        <span>
                          Vui lòng cập nhật <strong>ngày sinh</strong> trong hồ sơ trước khi đăng ký lớp.
                        </span>
                        <button
                          className="mk-btn mk-btn--primary mk-btn--block"
                          type="button"
                          onClick={() => navigate(APP_ROUTES.profile)}
                        >
                          Cập nhật hồ sơ
                        </button>
                      </div>
                    ) : isClient && minorNeedsGuardian ? (
                      // Child (vị thành niên) chưa liên kết phụ huynh -> chặn + điều hướng liên kết.
                      <div className="mk-alert mk-alert--warn" style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                        <span>
                          Tài khoản học sinh <strong>dưới 18 tuổi</strong> cần liên kết hồ sơ phụ huynh.
                          Phụ huynh sẽ là người ký hợp đồng và thanh toán sau khi bạn đăng ký.
                        </span>
                        <button
                          className="mk-btn mk-btn--primary mk-btn--block"
                          type="button"
                          onClick={() => navigate(APP_ROUTES.profileDependents)}
                        >
                          Liên kết hồ sơ phụ huynh
                        </button>
                      </div>
                    ) : isClient || isTutor ? (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                        {isClient && delegatedToParent && (
                          // Child đã liên kết: cho đăng ký, nhưng phụ huynh mới là bên ký hợp đồng.
                          <div className="mk-alert mk-alert--info">
                            Bạn dưới 18 tuổi — sau khi đăng ký, hợp đồng sẽ được gửi cho phụ huynh
                            {legalHolderName ? ` (${legalHolderName})` : ''} ký và thanh toán. Bạn chính
                            thức vào lớp sau khi phụ huynh hoàn tất.
                          </div>
                        )}
                        <button
                          className="mk-btn mk-btn--primary mk-btn--block"
                          type="button"
                          disabled={regStatus === 'loading' || !isOpen}
                          onClick={register}
                        >
                          {regStatus === 'loading'
                            ? 'Đang xử lý…'
                            : !isOpen
                              ? 'Lớp đã đóng đăng ký'
                              : isTutor
                                ? 'Ứng tuyển dạy lớp'
                                : 'Đăng ký học'}
                        </button>
                        {data.status === 'IN_PROGRESS' && (
                          <ChatButton
                            contextType="CLASS_ACTIVE"
                            contextId={data.classId}
                            label="Chat với bên liên quan"
                          />
                        )}
                      </div>
                    ) : (
                      <p className="mk-note">
                        Đăng nhập bằng tài khoản gia sư hoặc phụ huynh/học viên để đăng ký.
                      </p>
                    )}
                  </>
                )}
              </div>

              {showCompletionCard ? (
                <div className="mk-class-actions">
                  <div className="mk-enroll__head">
                    <span className="mk-enroll__title">Hoàn thành lớp</span>
                  </div>
                  {completeMsg && completionState !== 'COMPLETED' ? (
                    <div className={`mk-alert mk-alert--${completeMsg.tone === 'ok' ? 'ok' : 'error'}`}>
                      {completeMsg.text}
                    </div>
                  ) : null}

                  {completionState === 'COMPLETED' ? (
                    <div className="mk-alert mk-alert--ok">
                      Lớp đã hoàn thành. Học phí đã được giải ngân cho gia sư.
                    </div>
                  ) : completionState === 'TUTOR_CAN_CONFIRM' ? (
                    <>
                      <p className="mk-note">
                        Đánh dấu lớp đã hoàn thành và mời học viên đánh giá. Học phí escrow được giải ngân
                        sau khi học viên đánh giá gia sư.
                      </p>
                      <button
                        className="mk-btn mk-btn--primary mk-btn--block"
                        type="button"
                        disabled={completing}
                        onClick={() => void submitCompletion()}
                      >
                        {completing ? 'Đang xử lý…' : 'Hoàn thành lớp'}
                      </button>
                    </>
                  ) : completionState === 'TUTOR_WAITING' ? (
                    <p className="mk-note">
                      Đã đánh dấu hoàn thành. Đang chờ học viên đánh giá gia sư để đóng lớp và tất toán học phí.
                    </p>
                  ) : completionState === 'TUTOR_BLOCKED' ? (
                    <p className="mk-note">
                      {data.completionBlockedReason ?? 'Chưa đủ điều kiện để hoàn thành lớp.'}
                    </p>
                  ) : completionState === 'CLIENT_WAITING_TUTOR' ? (
                    <p className="mk-note">
                      Gia sư chưa hoàn thành lớp. Khi gia sư đánh dấu hoàn thành, bạn sẽ được mời đánh giá tại đây.
                    </p>
                  ) : completionState === 'CLIENT_MUST_REVIEW' ? (
                    <>
                      <p className="mk-note">
                        Gia sư đã đánh dấu lớp hoàn thành. Vui lòng đánh giá gia sư để hoàn tất lớp học —
                        học phí sẽ được giải ngân cho gia sư sau khi bạn đánh giá.
                      </p>
                      <button
                        className="mk-btn mk-btn--primary mk-btn--block"
                        type="button"
                        onClick={() => navigate(APP_ROUTES.feedback)}
                      >
                        Đánh giá gia sư
                      </button>
                    </>
                  ) : (
                    <p className="mk-note">Chưa thể hoàn thành lớp lúc này.</p>
                  )}
                </div>
              ) : null}

              {canRequestTermination ? (
                <div className="mk-class-actions">
                  <div className="mk-enroll__head">
                    <span className="mk-enroll__title">Quản lý lớp</span>
                  </div>
                  <button
                    className="mk-btn mk-btn--danger mk-btn--block"
                    type="button"
                    onClick={() => setTerminationModalOpen(true)}
                  >
                    Yêu cầu chấm dứt sớm
                  </button>
                  <button
                    className="mk-btn mk-btn--secondary mk-btn--block"
                    type="button"
                    onClick={() => setIssueModalOpen(true)}
                  >
                    Báo cáo sự cố
                  </button>
                </div>
              ) : null}
            </aside>
          </div>
        )}
      </div>
      {data ? (
        <ClassTerminationModal
          open={terminationModalOpen}
          classId={data.classId}
          assignmentId={data.terminationAssignmentId}
          classStudentId={data.terminationClassStudentId}
          classTitle={data.title}
          onClose={() => setTerminationModalOpen(false)}
        />
      ) : null}
      {data ? (
        <ClassIssueModal
          open={issueModalOpen}
          classId={data.classId}
          assignmentId={data.terminationAssignmentId}
          classStudentId={data.terminationClassStudentId}
          classTitle={data.title}
          currentUserRole={role}
          onClose={() => setIssueModalOpen(false)}
        />
      ) : null}
    </>
  );
}
