import { useEffect, useMemo, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import axios from 'axios';
import { VerificationHeader } from '../../../shared/components/VerificationHeader';
import { tutorApi } from '../api/tutorApi';
import type { AttendanceStatus, ScheduleClass } from '../../center/types/centerTypes';
import '../../center/pages/CenterSchedulePage.css';

const ATT_OPTIONS: { value: AttendanceStatus; label: string }[] = [
  { value: 'PRESENT', label: 'Có mặt' },
  { value: 'ABSENT', label: 'Vắng' },
  { value: 'EXCUSED', label: 'Có phép' },
];

function todayStr(): string {
  const d = new Date();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${day}`;
}

function extractError(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.message === 'string') {
    return error.response.data.message;
  }
  return fallback;
}

export default function TutorAttendancePage() {
  const { classId } = useParams<{ classId: string }>();
  const [searchParams] = useSearchParams();
  const date = searchParams.get('date') || todayStr();

  const [data, setData] = useState<ScheduleClass | null>(null);
  const [status, setStatus] = useState<'loading' | 'success' | 'error'>('loading');
  const [loadError, setLoadError] = useState('');
  const [marks, setMarks] = useState<Record<number, AttendanceStatus>>({});
  const [confirming, setConfirming] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');

  useEffect(() => {
    if (!classId) return;
    setStatus('loading');
    tutorApi
      .getSession(Number(classId), date)
      .then((res) => {
        setData(res.data);
        const init: Record<number, AttendanceStatus> = {};
        res.data.students.forEach((s) => {
          init[s.classStudentId] = (s.status as AttendanceStatus | null) ?? 'PRESENT';
        });
        setMarks(init);
        setStatus('success');
      })
      .catch((err) => {
        setLoadError(extractError(err, 'Không tải được buổi học.'));
        setStatus('error');
      });
  }, [classId, date]);

  const setMark = (id: number, value: AttendanceStatus) => {
    setMarks((prev) => ({ ...prev, [id]: value }));
    setConfirming(false);
  };

  const counts = useMemo(() => {
    let present = 0;
    let absent = 0;
    let excused = 0;
    Object.values(marks).forEach((v) => {
      if (v === 'PRESENT') present += 1;
      else if (v === 'ABSENT') absent += 1;
      else if (v === 'EXCUSED') excused += 1;
    });
    return { present, absent, excused };
  }, [marks]);

  const doSave = async () => {
    if (!data) return;
    setSaving(true);
    setSaveError('');
    try {
      const records = data.students.map((s) => ({
        classStudentId: s.classStudentId,
        status: marks[s.classStudentId],
      }));
      const res = await tutorApi.saveAttendance(data.classId, records, date);
      setData(res.data);
      setConfirming(false);
    } catch (err) {
      setSaveError(extractError(err, 'Lưu điểm danh thất bại.'));
    } finally {
      setSaving(false);
    }
  };

  // Gia sư xác nhận khóa học hoàn thành ở buổi cuối.
  const [completing, setCompleting] = useState(false);
  const [completeMsg, setCompleteMsg] = useState('');
  const confirmCourseDone = async () => {
    if (!data) return;
    setCompleting(true);
    setSaveError('');
    try {
      const res = await tutorApi.confirmClassCompletion(data.classId);
      setCompleteMsg(res.data?.message ?? 'Đã ghi nhận xác nhận hoàn thành khóa học.');
      const s = await tutorApi.getSession(data.classId, date);
      setData(s.data);
    } catch (err) {
      setSaveError(extractError(err, 'Không xác nhận được hoàn thành khóa học.'));
    } finally {
      setCompleting(false);
    }
  };

  const hasStudents = (data?.students.length ?? 0) > 0;
  // Lớp trung tâm không giới hạn ngày điểm danh — chỉ khoá khi buổi đã được điểm danh.
  const locked = !!data?.attendanceTaken;

  return (
    <>
      <VerificationHeader />
      <div className="cs-page">
        <div className="cs-topbar">
          <Link className="cs-back" to="/tutor/schedule">
            ← Lịch dạy
          </Link>
        </div>

        {status === 'loading' && <div className="cs-state">Đang tải buổi học…</div>}
        {status === 'error' && <div className="cs-alert cs-alert--error">{loadError}</div>}

        {status === 'success' && data && (
          <>
            <header className="cs-header">
              <div>
                <h1 className="cs-title">Điểm danh — {data.title}</h1>
                <p className="cs-subtitle">
                  Ngày {date}
                  {data.slots.length > 0 &&
                    ` · ${data.slots
                      .map((s) => `${s.startTime.slice(0, 5)}–${s.endTime.slice(0, 5)}`)
                      .join(', ')}`}
                </p>
              </div>
            </header>

            {!hasStudents ? (
              <div className="cs-empty">
                <div className="cs-empty__emoji">👤</div>
                <p>Lớp chưa có học sinh ghi danh.</p>
              </div>
            ) : (
              <>
                <ul className="cs-roster cs-roster--page">
                  {data.students.map((st, i) => (
                    <li className="cs-student" key={st.classStudentId}>
                      <div className="cs-student__info">
                        <span className="cs-student__name">
                          {i + 1}. {st.studentName}
                        </span>
                        {st.studentPhone && (
                          <span className="cs-student__phone">{st.studentPhone}</span>
                        )}
                      </div>
                      <div className="cs-att">
                        {ATT_OPTIONS.map((o) => (
                          <button
                            key={o.value}
                            type="button"
                            className={`cs-att__btn cs-att__btn--${o.value.toLowerCase()}${
                              marks[st.classStudentId] === o.value ? ' is-active' : ''
                            }`}
                            disabled={locked}
                            onClick={() => setMark(st.classStudentId, o.value)}
                          >
                            {o.label}
                          </button>
                        ))}
                      </div>
                    </li>
                  ))}
                </ul>

                <div className="cs-savebar">
                  <div className="cs-summary">
                    <span className="cs-summary__ok">Có mặt {counts.present}</span>
                    <span className="cs-summary__no">Vắng {counts.absent}</span>
                    <span className="cs-summary__ex">Có phép {counts.excused}</span>
                  </div>

                  {saveError && <div className="cs-alert cs-alert--error">{saveError}</div>}

                  {data.attendanceTaken ? (
                    <div className="cs-alert cs-alert--ok">
                      ✓ Buổi này đã điểm danh — không thể điểm danh lại.
                    </div>
                  ) : confirming ? (
                    <div className="cs-confirm">
                      <span>Xác nhận lưu điểm danh cho {data.students.length} học sinh?</span>
                      <div className="cs-confirm__actions">
                        <button
                          className="cs-btn cs-btn--ghost"
                          type="button"
                          disabled={saving}
                          onClick={() => setConfirming(false)}
                        >
                          Hủy
                        </button>
                        <button
                          className="cs-btn cs-btn--primary"
                          type="button"
                          disabled={saving}
                          onClick={doSave}
                        >
                          {saving ? 'Đang lưu…' : 'Lưu'}
                        </button>
                      </div>
                    </div>
                  ) : (
                    <button
                      className="cs-btn cs-btn--primary cs-btn--lg"
                      type="button"
                      onClick={() => setConfirming(true)}
                    >
                      Xác nhận & lưu điểm danh
                    </button>
                  )}
                </div>

                {data.finalSession && (
                  <div className="cs-savebar" style={{ marginTop: 12 }}>
                    {data.classCompleted ? (
                      <div className="cs-alert cs-alert--ok">✓ Khóa học đã hoàn thành.</div>
                    ) : data.tutorCompletionConfirmed ? (
                      <div className="cs-alert cs-alert--ok">
                        ✓ Bạn đã xác nhận hoàn thành — chờ bên còn lại hoàn tất bước xác nhận.
                      </div>
                    ) : locked ? (
                      <>
                        {completeMsg && <div className="cs-alert cs-alert--ok">{completeMsg}</div>}
                        <button
                          className="cs-btn cs-btn--primary cs-btn--lg"
                          type="button"
                          disabled={completing}
                          onClick={confirmCourseDone}
                          title="Buổi học cuối — xác nhận khóa học đã hoàn thành"
                        >
                          {completing ? 'Đang gửi…' : '✅ Xác nhận khóa học hoàn thành'}
                        </button>
                      </>
                    ) : (
                      <div className="cs-alert cs-alert--ok">
                        Đây là buổi học cuối — điểm danh xong sẽ xác nhận được khóa học hoàn thành.
                      </div>
                    )}
                  </div>
                )}
              </>
            )}
          </>
        )}
      </div>
    </>
  );
}
